package fiji.pluginManager;

import java.util.Iterator;
import java.util.List;
import java.io.File;
import java.io.IOException;

/*
 * This class' main role is to download selected files, as well as indicate those that
 * are marked for deletion. It is able to track the number of bytes downloaded.
 */
public class Installer extends PluginData implements Runnable, Observer {
	private volatile Thread downloadThread;
	private volatile Downloader downloader;

	//Keeping track of status
	public List<PluginObject> changeList; //list of plugins specified to uninstall/download
	public PluginObject currentlyDownloading;
	private int totalBytes;
	private int completedBytesTotal; //bytes downloaded so far of all completed files
	private int currentBytesSoFar; //bytes downloaded so far of current file
	private boolean isDownloading;

	//Assume the list passed to constructor is a list of only plugins that wanted change
	public Installer(List<PluginObject> pluginList) {
		super();

		//divide into two groups
		PluginCollection pluginCollection = (PluginCollection)pluginList;
		changeList = pluginCollection.getList(PluginCollection.FILTER_ACTIONS_SPECIFIED_NOT_UPLOAD);
		((PluginCollection)changeList).resetChangeAndUploadStatuses();
	}

	public int getBytesDownloaded() {
		return (completedBytesTotal + currentBytesSoFar); //return progress
	}

	public int getBytesTotal() {
		return totalBytes;
	}

	public boolean isDownloading() {
		return isDownloading;
	}

	//Convenience method to run both tasks
	public void beginOperations() {
		startDelete();
		startDownload();
	}

	//start processing on contents of Delete List (Mark them for deletion)
	public void startDelete() {
		Iterator<PluginObject> iterToUninstall = ((PluginCollection)changeList).getIterator(
				PluginCollection.FILTER_ACTIONS_UNINSTALL);
		while (iterToUninstall.hasNext()) {
			PluginObject plugin = iterToUninstall.next();
			String filename = plugin.getFilename();
			try {
				//checking status of existing file
				File file = new File(prefix(filename));
				if (!file.canWrite()) //if unable to override existing file
					plugin.setChangeStatusToFail();
				else {
					//write a 0-byte file
					String pluginPath = getSaveToLocation(PluginManager.UPDATE_DIRECTORY,
							filename);
					new File(pluginPath).getParentFile().mkdirs();
					new File(pluginPath).createNewFile();
					plugin.setChangeStatusToSuccess();
				}
			} catch (IOException e) {
				plugin.setChangeStatusToFail();
			}
		}
	}

	//start processing on contents of updateList
	public void startDownload() {
		Iterator<PluginObject> iterToDownload = ((PluginCollection)changeList).getIterator(
				PluginCollection.FILTER_ACTIONS_ADDORUPDATE);
		if (iterToDownload.hasNext()) {
			isDownloading = true;
			downloadThread = new Thread(this);
			downloadThread.start();
		}
	}

	//stop download
	public void stopDownload() {
		//thread will check if downloadThread is null, and stop action where necessary
		downloadThread = null;
		downloader.setDownloadThread(downloadThread);
	}

	//Marking files for removal assumed finished here, thus begin download tasks
	public void run() {
		Thread thisThread = Thread.currentThread();

		//This segment gets the size of the download
		Iterator<PluginObject> iterToDownload = ((PluginCollection)changeList).getIterator(
				PluginCollection.FILTER_ACTIONS_ADDORUPDATE);
		while (iterToDownload.hasNext()) {
			PluginObject myPlugin = iterToDownload.next();
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
		iterToDownload = ((PluginCollection)changeList).getIterator(
				PluginCollection.FILTER_ACTIONS_ADDORUPDATE);
		while (iterToDownload.hasNext() && thisThread == downloadThread) {
			currentlyDownloading = iterToDownload.next();
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
				if (thisThread == downloadThread) {
					downloader = new Downloader(downloadURL, saveToPath);
					//Checking if actual filesize consistent with records
					int recordedSize = 0;
					if (currentlyDownloading.isInstallable())
						recordedSize = currentlyDownloading.getFilesize();
					else if (currentlyDownloading.isUpdateable())
						recordedSize = currentlyDownloading.getNewFilesize();
					if (recordedSize != downloader.getSize())
						throw new Exception("Recorded filesize of " + name + " is " +
							recordedSize + ". It is not equal to actual content length of " +
							downloader.getSize() + ". Download will not proceed.");
					//Configure settings and begin download if no other problems
					downloader.setDownloadThread(downloadThread);
					downloader.startDownloadAndObserve(this);
				}
				completedBytesTotal += currentBytesSoFar;
				currentBytesSoFar = 0;

				//if download is not yet cancelled, check if downloaded has valid md5 sum
				if (thisThread == downloadThread) {
					String realDigest = getDigest(name, saveToPath);
					if (!realDigest.equals(digest))
						throw new Exception("Wrong checksum for " + name + ": Recorded Md5 sum "
								+ digest + " != Actual Md5 sum " + realDigest);
					if (name.startsWith("fiji-") && !getPlatform().startsWith("win"))
						Runtime.getRuntime().exec(new String[] {
								"chmod", "0755", saveToPath});
					currentlyDownloading.setChangeStatusToSuccess();
					System.out.println(currentlyDownloading.getFilename() + " finished download.");
					System.out.println((thisThread == downloadThread) ? "Thread not stopped" : "Thread stopped, but downloaded????");
					currentlyDownloading = null;
				} else {
					//if download is cancelled, delete any possible incomplete files
					deleteUnfinished();
				}

			} catch (Exception e) {
				//try to delete the file
				try {
					new File(saveToPath).delete();
				} catch (Exception e2) { }
				currentlyDownloading.setChangeStatusToFail();
				currentlyDownloading = null;
				System.out.println("Could not update " + name + ": " + e.getLocalizedMessage());
			}
		}
		isDownloading = false;
		System.out.println("END OF THREAD");
	}

	private void deleteUnfinished() {
		Iterator<PluginObject> iterator = ((PluginCollection)changeList).getIterator(
				PluginCollection.FILTER_NO_SUCCESSFUL_CHANGE);
		while (iterator.hasNext()) {
			PluginObject plugin = iterator.next();
			String fullPath = getSaveToLocation(PluginManager.UPDATE_DIRECTORY, plugin.getFilename());
			try {
				new File(fullPath).delete(); //delete file, if it exists
			} catch (Exception e2) { }
		}
	}

	public Iterator<PluginObject> iterDownloaded() {
		return ((PluginCollection)changeList).getIterator(
				PluginCollection.FILTER_DOWNLOAD_SUCCESS);
	}

	public int getNumberOfSuccessfulDownloads() {
		return ((PluginCollection)changeList).getList(
				PluginCollection.FILTER_DOWNLOAD_SUCCESS).size();
	}

	public Iterator<PluginObject> iterFailedDownloads() {
		return ((PluginCollection)changeList).getIterator(
				PluginCollection.FILTER_DOWNLOAD_FAIL);
	}

	public int getNumberOfFailedDownloads() {
		return ((PluginCollection)changeList).getList(
				PluginCollection.FILTER_DOWNLOAD_SUCCESS).size();
	}

	public Iterator<PluginObject> iterMarkedUninstall() {
		return ((PluginCollection)changeList).getIterator(
				PluginCollection.FILTER_REMOVE_SUCCESS);
	}

	public int getNumberOfMarkedUninstalls() {
		return ((PluginCollection)changeList).getList(
				PluginCollection.FILTER_REMOVE_SUCCESS).size();
	}

	public Iterator<PluginObject> iterFailedUninstalls() {
		return ((PluginCollection)changeList).getIterator(
				PluginCollection.FILTER_REMOVE_FAIL);
	}

	public int getNumberOfFailedUninstalls() {
		return ((PluginCollection)changeList).getList(
				PluginCollection.FILTER_REMOVE_FAIL).size();
	}

	public boolean successfulChangesMade() {
		Iterator<PluginObject> iterator = ((PluginCollection)changeList).getIterator(
				PluginCollection.FILTER_CHANGE_SUCCEEDED);
		return iterator.hasNext();
	}

	public void refreshData(Observable subject) {
		Downloader myDownloader = (Downloader)subject;
		currentBytesSoFar = myDownloader.getBytesSoFar();
		System.out.println("Downloaded so far: " + (completedBytesTotal + currentBytesSoFar));
	}

}