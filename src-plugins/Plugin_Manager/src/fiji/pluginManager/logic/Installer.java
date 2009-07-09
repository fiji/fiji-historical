package fiji.pluginManager.logic;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.io.File;
import java.io.IOException;
import fiji.pluginManager.logic.Downloader.SourceFile;

/*
 * This class' main role is to download selected files, as well as indicate those that
 * are marked for deletion. It is able to track the number of bytes downloaded.
 */
public class Installer extends PluginData implements Runnable, Downloader.DownloadListener {
	private volatile Thread downloadThread;
	private volatile Downloader downloader;
	private List<SourceFile> downloaderList;

	//Keeping track of status
	public List<PluginObject> changeList; //list of plugins specified to uninstall/download
	public PluginObject currentlyDownloading;
	private int totalBytes;
	private int completedBytesTotal; //bytes downloaded so far of all completed files
	private int currentBytesSoFar; //bytes downloaded so far of current file
	private boolean isDownloading;

	public Installer(List<PluginObject> pluginList) {
		super();
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
		isDownloading = true;
		downloadThread = new Thread(this);
		downloadThread.start();
	}

	//stop download
	public void stopDownload() {
		//thread will check if downloadThread is null, and stop action where necessary
		downloadThread = null;
		downloader.cancelDownload();
	}

	//Marking files for removal assumed finished here, thus begin download tasks
	public void run() {
		Thread thisThread = Thread.currentThread();

		Iterator<PluginObject> iterDownload = ((PluginCollection)changeList).getIterator(
				PluginCollection.FILTER_ACTIONS_ADDORUPDATE);
		downloaderList = new ArrayList<SourceFile>();
		while (iterDownload.hasNext()) {
			PluginObject plugin = iterDownload.next();
			//For each selected plugin, get target path to save to
			String name = plugin.getFilename();
			String saveToPath = getSaveToLocation(PluginManager.UPDATE_DIRECTORY, name);
			if (name.startsWith("fiji-")) {
				File orig = new File(saveToPath);
				orig.renameTo(new File(saveToPath + ".old"));
			}

			//For each selected plugin, get download URL
			String date = null;
			if (currentlyDownloading.isInstallable()) {
				date = currentlyDownloading.getTimestamp();
			} else if (currentlyDownloading.isUpdateable()) {
				date = currentlyDownloading.getNewTimestamp();
			}
			String downloadURL = PluginManager.MAIN_URL + name + "-" + date;
			PluginSource src = new PluginSource(plugin, downloadURL, saveToPath);
			downloaderList.add(src);

			//Gets the total size of the downloads
			totalBytes += src.getRecordedFileSize();
			System.out.println("totalBytes so far: " + totalBytes);
		}

		try {
			downloader = new Downloader(downloaderList.iterator());
			downloader.addListener(this);
			downloader.startDownload(); //nothing happens if downloaderList is empty
		} catch (Exception e) {
			clearDownloadError(currentlyDownloading, e);
		}

		if (thisThread != downloadThread) {
			//if cancelled, remove any unfinished downloads
			Iterator<PluginObject> iterator = ((PluginCollection)changeList).getIterator(
					PluginCollection.FILTER_NO_SUCCESSFUL_CHANGE);
			while (iterator.hasNext()) {
				PluginObject plugin = iterator.next();
				String fullPath = getSaveToLocation(PluginManager.UPDATE_DIRECTORY,
						plugin.getFilename());
				try {
					new File(fullPath).delete(); //delete file, if it exists
				} catch (Exception e2) { }
			}
		}

		isDownloading = false;
		System.out.println("END OF THREAD");		
	}

	private void clearDownloadError(PluginObject plugin, Exception e) {
		String destination = getSaveToLocation(PluginManager.UPDATE_DIRECTORY,
				plugin.getFilename());
		//try to delete the file if inconsistency is found
		try {
			new File(destination).delete();
		} catch (Exception e2) { }
		currentlyDownloading.setChangeStatusToFail();
		System.out.println("Could not update " + currentlyDownloading.getFilename() +
				": " + e.getLocalizedMessage());
		currentlyDownloading = null;
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

	//Listener receives notification that download for file has stopped
	public void completion(SourceFile source) {
		PluginSource src = (PluginSource)source;
		currentlyDownloading = src.getPlugin();
		String filename = currentlyDownloading.getFilename();

		try {
			//Check filesize
			int recordedSize = src.getRecordedFileSize();
			int actualFilesize = getFilesizeFromFile(src.getDestination());
			if (recordedSize != actualFilesize)
				throw new Exception("Recorded filesize of " + filename + " is " +
						recordedSize + ". It is not equal to actual filesize of " +
						actualFilesize + ".");

			//Check Md5 sum
			String recordedDigest = src.getRecordedDigest();
			String actualDigest = getDigest(filename, src.getDestination());
			if (!recordedDigest.equals(actualDigest))
				throw new Exception("Wrong checksum for " + filename +
						": Recorded Md5 sum " + recordedDigest + " != Actual Md5 sum " +
						actualDigest);

			if (filename.startsWith("fiji-") && !getPlatform().startsWith("win"))
				Runtime.getRuntime().exec(new String[] {
						"chmod", "0755", src.getDestination()});
			currentlyDownloading.setChangeStatusToSuccess();
			System.out.println(currentlyDownloading.getFilename() + " finished download.");
			currentlyDownloading = null;

		} catch (Exception e) {
			clearDownloadError(currentlyDownloading, e);
		}
		completedBytesTotal += currentBytesSoFar;
		currentBytesSoFar = 0;
	}

	public void update(SourceFile source, int bytesSoFar, int bytesTotal) {
		PluginSource src = (PluginSource)source;
		currentlyDownloading = src.getPlugin();
		currentBytesSoFar = bytesSoFar;
		System.out.println("Downloaded so far: " + (completedBytesTotal + currentBytesSoFar));
	}
}