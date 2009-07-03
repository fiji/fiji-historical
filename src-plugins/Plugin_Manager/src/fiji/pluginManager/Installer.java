package fiji.pluginManager;

import java.util.Iterator;
import java.util.List;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;

/*
 * This class' main role is to download selected files, as well as indicate those that
 * are marked for deletion. It is able to track the number of bytes downloaded.
 */
public class Installer extends PluginData implements Runnable, Observer {
	private List<PluginObject> pluginsWaiting;
	private volatile Thread downloadThread;

	//Each file has one try to uninstall/download
	private Iterator<PluginObject> iterUninstall;
	private Iterator<PluginObject> iterWaiting;

	//Keeping track of status
	public List<PluginObject> markedUninstallList;
	public List<PluginObject> failedUninstallList;
	public List<PluginObject> downloadedList;
	public List<PluginObject> failedDownloadsList;
	public PluginObject currentlyDownloading;
	private int totalBytes;
	private int downloadedBytes;
	private boolean isDownloading;

	//Assume the list passed to constructor is a list of only plugins that wanted change
	public Installer(List<PluginObject> pluginList) {
		super();
		downloadedList = new PluginCollection();
		failedDownloadsList = new PluginCollection();
		markedUninstallList = new PluginCollection();
		failedUninstallList = new PluginCollection();

		//divide into two groups
		PluginCollection pluginCollection = (PluginCollection)pluginList;
		iterUninstall = pluginCollection.getIterator(PluginCollection.FILTER_ACTIONS_UNINSTALL);
		pluginsWaiting = pluginCollection.getList(PluginCollection.FILTER_ACTIONS_ADDORUPDATE);
		iterWaiting = pluginsWaiting.iterator();
	}

	public int getBytesDownloaded() {
		return downloadedBytes;
	}

	public int getBytesTotal() {
		return totalBytes;
	}

	public boolean isDownloading() {
		return isDownloading;
	}

	public void beginOperations() {
		startDelete();
		startDownload();
	}

	//start processing on contents of deletionList
	private final String DELETE_FILE = "delete.txt";
	private void startDelete() {
		//TODO: Implementation of uninstallation of plugins
		//TODO: TO eliminate one of the two methods shown here once method is confirmed...
		if (iterUninstall.hasNext()) {
			String saveTxtLocation = prefix(DELETE_FILE);
			PrintStream txtPrintStream = null;
			try {
				new File(saveTxtLocation).getParentFile().mkdirs();
				txtPrintStream = new PrintStream(saveTxtLocation);
				while (iterUninstall.hasNext()) {
					PluginObject plugin = iterUninstall.next();
					String filename = plugin.getFilename();
					try {
						//checking status of existing file
						File file = new File(prefix(filename));
						if (!file.canWrite()) //if unable to override existing file
							failedUninstallList.add(plugin);
						else {
							//save line to delete.txt
							txtPrintStream.println(filename);

							//write a 0-byte file
							String pluginPath = getSaveToLocation(PluginManager.UPDATE_DIRECTORY, filename);
							new File(pluginPath).getParentFile().mkdirs();
							new File(pluginPath).createNewFile();

							markedUninstallList.add(plugin);
						}
					} catch (IOException e) {
						//this is for 0-byte file implementation
						failedUninstallList.add(plugin);
					}
				}
				txtPrintStream.close();
			} catch (IOException e) {
				//this is for delete.txt implementation
				while (iterUninstall.hasNext())
					failedUninstallList.add(iterUninstall.next());
			}
		}
	}

	//start processing on contents of updateList
	private void startDownload() {
		if (iterWaiting.hasNext()) {
			isDownloading = true;
			downloadThread = new Thread(this);
			downloadThread.start();
		}
	}

	//stop download
	public void stopDownload() {
		//thread will check if downloadThread is null, and stop action where necessary
		downloadThread = null;
	}

	//Marking files for removal assumed finished here, thus begin download tasks
	public void run() {
		Thread thisThread = Thread.currentThread();
		//This segment gets the size of the download
		for (PluginObject myPlugin : pluginsWaiting) {
			if (thisThread == downloadThread) {
				if (myPlugin.isInstallable()) {
					totalBytes += myPlugin.getFilesize();
				} else if (myPlugin.isUpdateable()) {
					totalBytes += myPlugin.getNewFilesize();
				}
				System.out.println("totalBytes so far: " + totalBytes);
			}
			else if (thisThread != downloadThread) break;
		}

		//Downloads the file(s), one by one
		while (iterWaiting.hasNext() && thisThread == downloadThread) {
			currentlyDownloading = iterWaiting.next();
			String name = currentlyDownloading.getFilename();
			String digest = null;
			String date = null;
			if (currentlyDownloading.isInstallable()) {
				digest = currentlyDownloading.getmd5Sum();
				date = currentlyDownloading.getTimestamp();
			} else if (currentlyDownloading.isUpdateable()) {
				digest = currentlyDownloading.getNewMd5Sum();
				date = currentlyDownloading.getNewTimestamp();
			}

			String saveToPath = getSaveToLocation(PluginManager.UPDATE_DIRECTORY, name);
			String downloadURL = "";
			try {
				if (name.startsWith("fiji-")) {
					File orig = new File(saveToPath);
					orig.renameTo(new File(saveToPath + ".old"));
				}

				//Establish connection to file for this iteration
				downloadURL = PluginManager.MAIN_URL + name + "-" + date;
				Downloader downloader = new Downloader(downloadURL, saveToPath);
				downloader.register(this);

				//TODO: Check for filesizes?
				//(downloader.getSize() == currentlyDownloading.getFilesize())

				//Prepare the necessary download input and output streams
				downloader.prepareDownload();
				byte[] buffer = downloader.createNewBuffer();
				int count;

				//while file is writing and download is NOT cancelled yet
				while ((count = downloader.getNextPart(buffer)) >= 0 &&
						thisThread == downloadThread) {
					downloader.writePart(buffer, count);
				}
				downloader.endConnection(); //end connection once download done

				//if download is not yet cancelled, check if downloaded has valid md5 sum
				if (thisThread == downloadThread) {
					String realDigest = getDigest(name, saveToPath);
					if (!realDigest.equals(digest))
						throw new Exception("Wrong checksum: Recorded Md5 sum " + digest + " != Actual Md5 sum " + realDigest);
					if (name.startsWith("fiji-") && !getPlatform().startsWith("win"))
						Runtime.getRuntime().exec(new String[] {
								"chmod", "0755", saveToPath});
					downloadedList.add(currentlyDownloading);
					System.out.println(currentlyDownloading.getFilename() + " finished download.");
					System.out.println((thisThread == downloadThread) ? "Thread not stopped" : "Thread stopped, but downloaded????");
					currentlyDownloading = null;
				} else {
					//if download is cancelled, delete any possible incomplete files
					deleteUnfinished();
				}

			} catch (Exception e) {
				//try to delete the file (probably this be the only catch - DRY)
				try {
					new File(saveToPath).delete();
				} catch (Exception e2) { }
				failedDownloadsList.add(currentlyDownloading);
				currentlyDownloading = null;
				System.out.println("Could not update " + name + ": " + e.getLocalizedMessage());
			}
		}
		isDownloading = false;
		System.out.println("END OF THREAD");
	}

	private void deleteUnfinished() {
		Iterator<PluginObject> iterator = pluginsWaiting.iterator();
		while (iterator.hasNext()) {
			PluginObject plugin = iterator.next();
			//if this plugin in waiting list is not fully downloaded yet
			if (!downloadedList.contains(plugin)) {
				String name = plugin.getFilename();
				String fullPath = getSaveToLocation(PluginManager.UPDATE_DIRECTORY, name);
				try {
					new File(fullPath).delete(); //delete file, if it exists
				} catch (Exception e2) { }
			}
		}
	}

	public void refreshData(Observable subject) {
		Downloader myDownloader = (Downloader)subject;
		downloadedBytes += myDownloader.getNumOfBytes();
		System.out.println("Downloaded so far: " + downloadedBytes);
	}

}