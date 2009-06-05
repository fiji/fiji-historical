package fiji.PluginManager;
import ij.IJ;

import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;

/*
 * This class' main role is to download selected files, as well as indicate those that
 * are marked for deletion. It is able to track the number of bytes downloaded.
*/
public class Installer implements Runnable, Observer {
	private PluginDataProcessor pluginDataProcessor;
	private String updateURL;
	private final String updateDirectory = "update";
	private List<PluginObject> toInstallList;

	//Keeping track of status
	private List<PluginObject> toUninstallList;
	private List<PluginObject> downloadedList;
	private List<PluginObject> waitingList;
	private List<PluginObject> failedDownloadsList;
	private PluginObject currentlyDownloading;
	private int totalBytes;
	private int downloadedBytes;

	//Assume the list passed to constructor is a list of only plugins that wanted change
	public Installer(PluginDataProcessor pluginDataProcessor, List<PluginObject> selectedList, String updateURL) {
		this.updateURL = updateURL;
		this.pluginDataProcessor = pluginDataProcessor;

		toInstallList = new PluginCollection();
		waitingList = new PluginCollection();
		downloadedList = new PluginCollection();
		failedDownloadsList = new PluginCollection();

		//divide into two groups
		toUninstallList = ((PluginCollection)selectedList).getListWhereActionUninstall();
		for (int i = 0; i < selectedList.size(); i++) {
			PluginObject myPlugin = selectedList.get(i);
			if (!toUninstallList.contains(myPlugin)) {
				waitingList.add(myPlugin);
				toInstallList.add(myPlugin);
			}
		}
	}

	//start processing on contents of deletionList
	public void startDelete() {
		for (int i = 0; i < toUninstallList.size(); i++) {
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

	public List<PluginObject> getListOfWaiting() {
		return waitingList;
	}

	public List<PluginObject> getListOfFailedDownloads() {
		return failedDownloadsList;
	}
	public PluginObject getCurrentDownload() {
		return currentlyDownloading;
	}

	//start processing on contents of updateList
	public void startDownload() {
		Thread myThread = new Thread(this);
		myThread.start();
	}

	public void run() {
		//Temporary arrangement - This segment gets the size of the file
		for (int i = 0; i < toInstallList.size(); i++) {
			PluginObject myPlugin = toInstallList.get(i);
			String name = myPlugin.getFilename();
			String digest = null;
			String date = null;
			if (myPlugin.getStatus() == PluginObject.STATUS_UNINSTALLED) {
				digest = myPlugin.getmd5Sum();
				date = myPlugin.getTimestamp();
			} else if (myPlugin.getStatus() == PluginObject.STATUS_MAY_UPDATE) {
				digest = myPlugin.getNewMd5Sum();
				date = myPlugin.getNewTimestamp();
			}

			try {
				System.out.println("Trying to establish connection for " + new URL(new URL(updateURL), name + "-" + date).getPath());
				URL myURL = new URL(updateURL);
				HttpURLConnection myConn = (HttpURLConnection)(new URL(myURL, name + "-" + date)).openConnection();
				System.out.println("Connection for " + new URL(new URL(updateURL), name + "-" + date).getPath() + " established.");
				totalBytes += myConn.getContentLength();
				System.out.println("total bytes so far: " + totalBytes);
				myConn.disconnect();
			} catch (MalformedURLException e) {
				throw new Error(updateURL + " has unknown protocol.");
			} catch (IOException e) {
				throw new Error("I/O Exception while opening connection to " + updateURL);
			}
		}

		//Downloads the file(s), one by one
		Iterator<PluginObject> iterInstallList = toInstallList.listIterator();
		while (iterInstallList.hasNext()) {
			PluginObject myPlugin = iterInstallList.next();
			currentlyDownloading = myPlugin;
			String name = myPlugin.getFilename();
			String digest = null;
			String date = null;
			if (myPlugin.getStatus() == PluginObject.STATUS_UNINSTALLED) {
				digest = myPlugin.getmd5Sum();
				date = myPlugin.getTimestamp();
			} else if (myPlugin.getStatus() == PluginObject.STATUS_MAY_UPDATE) {
				digest = myPlugin.getNewMd5Sum();
				date = myPlugin.getNewTimestamp();
			}

			String savePath = pluginDataProcessor.prefix(updateDirectory +
					File.separator + name);
			String downloadURL = "";
			try {
				if (name.startsWith("fiji-")) {
					boolean useMacPrefix = pluginDataProcessor.getUseMacPrefix();
					String macPrefix = pluginDataProcessor.getMacPrefix();
					savePath = pluginDataProcessor.prefix((useMacPrefix ? macPrefix : "") + name);
					File orig = new File(savePath);
					orig.renameTo(new File(savePath + ".old"));
				}

				//Download the file specified at this iteration
				downloadURL = new URL(new URL(updateURL), name + "-" + date).toString();
				Downloader downloader = new Downloader(downloadURL, savePath);
				downloader.register(this);
				downloader.startDownload(); //download (after which will close connection)

				String realDigest = pluginDataProcessor.getDigest(name, savePath);
				if (!realDigest.equals(digest))
					throw new Exception("Wrong checksum: Recorded Md5 sum " + digest + " != Actual Md5 sum " + realDigest);
				if (name.startsWith("fiji-") && !pluginDataProcessor.getPlatform().startsWith("win"))
					Runtime.getRuntime().exec(new String[] {
							"chmod", "0755", savePath});
				waitingList.remove(currentlyDownloading);
				downloadedList.add(currentlyDownloading);
				System.out.println(currentlyDownloading.getFilename() + " finished download.");
				currentlyDownloading = null;

			} catch (MalformedURLException e) {
				waitingList.remove(currentlyDownloading);
				failedDownloadsList.add(currentlyDownloading);
				currentlyDownloading = null;
				System.out.println("URL: " + downloadURL + " has unknown protocol.");
			} catch (IOException e) {
				waitingList.remove(currentlyDownloading);
				failedDownloadsList.add(currentlyDownloading);
				currentlyDownloading = null;
				System.out.println("I/O Exception while opening connection to " + downloadURL);
			} catch(Exception e) {
				//try to delete the file (probably this be the only catch - DRY)
				try {
					new File(savePath).delete();
				} catch (Exception e2) { }
				waitingList.remove(currentlyDownloading);
				failedDownloadsList.add(currentlyDownloading);
				currentlyDownloading = null;
				System.out.println("Could not update " + name + ": " + e.getMessage());
			}
		}
	}

	public void refreshData(Observable subject) {
		Downloader myDownloader = (Downloader)subject;
		downloadedBytes += myDownloader.getNumOfBytes();
		System.out.println("Downloaded so far: " + downloadedBytes);
	}

}