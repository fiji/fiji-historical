package fiji.pluginManager.logic;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.io.File;
import java.io.IOException;

import fiji.pluginManager.utilities.Downloader;
import fiji.pluginManager.utilities.PluginData;
import fiji.pluginManager.utilities.Downloader.FileDownload;

/*
 * UpdateTracker.java is for normal users in managing their plugins.
 * 
 * This class' main role is to download selected files, as well as indicate those that
 * are marked for deletion. It is able to track the number of bytes downloaded.
 */
public class UpdateTracker extends PluginData implements Runnable, Downloader.DownloadListener {
	private volatile Thread downloadThread;
	private volatile Downloader downloader;
	private List<FileDownload> downloaderList;

	//Keeping track of status
	public PluginCollection changeList; //list of plugins specified to uninstall/download
	public PluginObject currentlyDownloading;
	private int totalBytes;
	private int completedBytesTotal; //bytes downloaded so far of all completed files
	private int currentBytesSoFar; //bytes downloaded so far of current file
	private boolean isDownloading;

	public UpdateTracker(List<PluginObject> plugins) {
		super();
		PluginCollection pluginList = (PluginCollection)plugins;
		changeList = (PluginCollection)pluginList.getList(
				PluginCollection.FILTER_ACTIONS_SPECIFIED_NOT_UPLOAD);
		changeList.resetChangeStatuses();
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

	//start processing on contents of Delete List (Mark them for deletion)
	public void markToDelete() {
		for (PluginObject plugin : changeList.getList(PluginCollection.FILTER_ACTIONS_UNINSTALL)) {
			String filename = plugin.getFilename();
			try {
				//checking status of existing file
				File file = new File(prefix(filename));
				if (!file.canWrite()) //if unable to override existing file
					plugin.setChangeStatusToFail();
				else {
					//write a 0-byte file
					String pluginPath = getSaveToLocation(PluginManager.UPDATE_DIRECTORY, filename);
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
		downloaderList = new ArrayList<FileDownload>();
		for (PluginObject plugin : changeList.getList(PluginCollection.FILTER_ACTIONS_ADDORUPDATE)) {
			//For each selected plugin, get target path to save to
			String name = plugin.getFilename();
			String saveToPath = getSaveToLocation(PluginManager.UPDATE_DIRECTORY, name);

			//For each selected plugin, get download URL
			String date = null;
			if (plugin.isInstallable()) {
				date = plugin.getTimestamp();
			} else if (plugin.isUpdateable()) {
				date = plugin.getNewTimestamp();
			}
			//TODO: fix!!!
			String downloadURL = PluginManager.TEMP_DOWNLOADURL + name + "-" + date;
			//String downloadURL = PluginManager.MAIN_URL + name + "-" + date;
			PluginDownload src = new PluginDownload(plugin, downloadURL, saveToPath);
			downloaderList.add(src);

			//Gets the total size of the downloads
			totalBytes += src.getRecordedFileSize();
		}

		downloader = new Downloader(downloaderList.iterator());
		downloader.addListener(this);
		downloader.startDownload(); //nothing happens if downloaderList is empty

		if (thisThread != downloadThread) {
			//if cancelled, remove any unfinished downloads
			for (PluginObject plugin : changeList.getList(PluginCollection.FILTER_NO_SUCCESSFUL_CHANGE)) {
				String fullPath = getSaveToLocation(PluginManager.UPDATE_DIRECTORY, plugin.getFilename());
				try {
					new File(fullPath).delete(); //delete file, if it exists
				} catch (Exception e2) { }
			}
		}
		isDownloading = false;
	}

	private void resolveDownloadError(PluginDownload src, Exception e) {
		//try to delete the file
		try {
			new File(src.getDestination()).delete();
		} catch (Exception e1) { }
		src.getPlugin().setChangeStatusToFail();
		System.out.println("Could not update " + src.getPlugin().getFilename() +
				": " + e.getLocalizedMessage());
	}

	public Iterator<PluginObject> iterDownloaded() {
		return changeList.getIterator(PluginCollection.FILTER_DOWNLOAD_SUCCESS);
	}

	public int getNumberOfSuccessfulDownloads() {
		return changeList.getList(PluginCollection.FILTER_DOWNLOAD_SUCCESS).size();
	}

	public Iterator<PluginObject> iterFailedDownloads() {
		return changeList.getIterator(PluginCollection.FILTER_DOWNLOAD_FAIL);
	}

	public int getNumberOfFailedDownloads() {
		return changeList.getList(PluginCollection.FILTER_DOWNLOAD_SUCCESS).size();
	}

	public Iterator<PluginObject> iterMarkedUninstall() {
		return changeList.getIterator(PluginCollection.FILTER_REMOVE_SUCCESS);
	}

	public int getNumberOfMarkedUninstalls() {
		return changeList.getList(PluginCollection.FILTER_REMOVE_SUCCESS).size();
	}

	public Iterator<PluginObject> iterFailedUninstalls() {
		return changeList.getIterator(PluginCollection.FILTER_REMOVE_FAIL);
	}

	public int getNumberOfFailedUninstalls() {
		return changeList.getList(PluginCollection.FILTER_REMOVE_FAIL).size();
	}

	public boolean successfulChangesMade() {
		Iterator<PluginObject> iterator = changeList.getIterator(PluginCollection.FILTER_CHANGE_SUCCEEDED);
		return iterator.hasNext();
	}

	//Listener receives notification that download for file has finished
	public void fileComplete(FileDownload source) {
		PluginDownload src = (PluginDownload)source;
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

			currentlyDownloading.setChangeStatusToSuccess();
			System.out.println(currentlyDownloading.getFilename() + " finished download.");
			currentlyDownloading = null;

		} catch (Exception e) {
			resolveDownloadError(src, e);
			currentlyDownloading = null;
		}
		completedBytesTotal += currentBytesSoFar;
		currentBytesSoFar = 0;
	}

	public void update(FileDownload source, int bytesSoFar, int bytesTotal) {
		PluginDownload src = (PluginDownload)source;
		currentlyDownloading = src.getPlugin();
		currentBytesSoFar = bytesSoFar;
	}

	public void fileFailed(FileDownload source, Exception e) {
		resolveDownloadError((PluginDownload)source, e);
	}
}