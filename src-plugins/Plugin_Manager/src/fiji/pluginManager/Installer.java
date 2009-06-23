package fiji.pluginManager;

import ij.IJ;

import java.util.Iterator;
import java.util.List;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/*
 * This class' main role is to download selected files, as well as indicate those that
 * are marked for deletion. It is able to track the number of bytes downloaded.
*/
public class Installer implements Runnable, Observer {
	private PluginDataProcessor pluginDataProcessor;
	private String updateURL;
	private final String updateDirectory = "update";
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
	public Installer(PluginDataReader pluginDataReader, String updateURL) {
		this.updateURL = updateURL;
		this.pluginDataProcessor = pluginDataReader.getPluginDataProcessor();

		downloadedList = new PluginCollection();
		failedDownloadsList = new PluginCollection();

		//divide into two groups
		PluginCollection pluginCollection = (PluginCollection)pluginDataReader.getExistingPluginList();
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
		//This segment gets the size of the file
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
			PluginObject myPlugin = iterWaiting.next();
			currentlyDownloading = myPlugin;
			String name = myPlugin.getFilename();
			String digest = null;
			String date = null;
			if (myPlugin.isInstallable()) {
				digest = myPlugin.getmd5Sum();
				date = myPlugin.getTimestamp();
			} else if (myPlugin.isUpdateable()) {
				digest = myPlugin.getNewMd5Sum();
				date = myPlugin.getNewTimestamp();
			}

			String savePath = pluginDataProcessor.getSavePath(updateDirectory, name);
			String downloadURL = "";
			try {
				if (name.startsWith("fiji-")) {
					File orig = new File(savePath);
					orig.renameTo(new File(savePath + ".old"));
				}

				//Establish connection to file for this iteration
				downloadURL = new URL(new URL(updateURL), name + "-" + date).toString();
				Downloader downloader = new Downloader(downloadURL, savePath);
				downloader.register(this);

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
					String realDigest = pluginDataProcessor.getDigest(name, savePath);
					if (!realDigest.equals(digest))
						throw new Exception("Wrong checksum: Recorded Md5 sum " + digest + " != Actual Md5 sum " + realDigest);
					if (name.startsWith("fiji-") && !pluginDataProcessor.getPlatform().startsWith("win"))
						Runtime.getRuntime().exec(new String[] {
								"chmod", "0755", savePath});
					downloadedList.add(currentlyDownloading);
					System.out.println(currentlyDownloading.getFilename() + " finished download.");
					System.out.println((thisThread == downloadThread) ? "Thread not stopped" : "Thread stopped, but downloaded????");
					currentlyDownloading = null;
				} else {
					//if download is cancelled, delete any possible incomplete files
					deleteUnfinished();
				}

			} catch(Exception e) {
				//try to delete the file (probably this be the only catch - DRY)
				try {
					new File(savePath).delete();
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
				String fullPath = pluginDataProcessor.getSavePath(updateDirectory, name);
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