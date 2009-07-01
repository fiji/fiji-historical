package fiji.pluginManager;

import java.util.Iterator;
import java.util.List;
import java.io.File;
import java.net.URL;

/*
 * This class' main role is to download selected files, as well as indicate those that
 * are marked for deletion. It is able to track the number of bytes downloaded.
*/
public class Installer extends PluginData implements Runnable, Observer {
	private List<PluginObject> pluginsWaiting;
	private volatile Thread downloadThread;

	//Keeping track of status
	private Iterator<PluginObject> iterUninstall;
	private List<PluginObject> downloadedList;
	private Iterator<PluginObject> iterWaiting;
	private List<PluginObject> failedDownloadsList;
	private PluginObject currentlyDownloading;
	private int totalBytes;
	private int downloadedBytes;
	private boolean isDownloading;

	//Assume the list passed to constructor is a list of only plugins that wanted change
	public Installer(List<PluginObject> pluginList) {
		super();
		downloadedList = new PluginCollection();
		failedDownloadsList = new PluginCollection();

		//divide into two groups
		PluginCollection pluginCollection = (PluginCollection)pluginList;
		iterUninstall = pluginCollection.getIterator(PluginCollection.FILTER_ACTIONS_UNINSTALL);
		pluginsWaiting = pluginCollection.getList(PluginCollection.FILTER_ACTIONS_ADDORUPDATE);
		iterWaiting = pluginsWaiting.iterator();
	}

	//start processing on contents of deletionList
	public void startDelete() {
		while (iterUninstall.hasNext()) {
			//PluginObject plugin = iterUninstall.next();
			//do deleting
		}
	}

	public int getBytesDownloaded() {
		return downloadedBytes;
	}

	public int getBytesTotal() {
		return totalBytes;
	}

	public List<PluginObject> getListOfDownloaded() {
		return downloadedList;
	}

	public boolean stillDownloading() {
		return isDownloading;
	}

	public List<PluginObject> getListOfFailedDownloads() {
		return failedDownloadsList;
	}

	public PluginObject getCurrentDownload() {
		return currentlyDownloading;
	}

	//start processing on contents of updateList
	public void startDownload() {
		downloadThread = new Thread(this);
		downloadThread.start();
	}

	//stop download
	public void stopDownload() {
		//thread will check if downloadThread is null, and stop action where necessary
		downloadThread = null;
	}

	public void run() {
		Thread thisThread = Thread.currentThread();
		isDownloading = true;
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