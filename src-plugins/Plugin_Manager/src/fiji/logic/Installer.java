package fiji.logic;
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

import fiji.data.PluginCollection;
import fiji.data.PluginObject;

//To do: This class has the ability to track number of bytes downloaded?
//This class downloads (installs) as well as deletes files, through other classes calling
//this class' methods
public class Installer implements Runnable {
	String updateURL;
	String fijiPath;
	boolean forServer = false;
	public static final String updateDirectory = "update";
	protected final String macPrefix = "Contents/MacOS/";
	protected boolean useMacPrefix = false;
	private List<PluginObject> toInstallList;

	//Keeping track of status
	private List<PluginObject> toUninstallList;
	private List<PluginObject> downloadedList;
	private List<PluginObject> waitingList;
	private PluginObject currentlyDownloading;
	private int totalBytes = 0;
	private int downloadedBytes = 0;

	//Assume the list passed to constructor is a list of only plugins that wanted change
	public Installer(List<PluginObject> selectedList, String updateURL) {
		this.updateURL = updateURL;
		fijiPath = getDefaultFijiPath();
		toInstallList = new PluginCollection();
		waitingList = new PluginCollection();
		downloadedList = new PluginCollection();

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

	public String getDefaultFijiPath() {
		String name = "/UpdateFiji.class";
		URL url = getClass().getResource(name);
		String path = URLDecoder.decode(url.toString());
		path = path.substring(0, path.length() - name.length());
		if (path.startsWith("jar:") && path.endsWith("!"))
			path = path.substring(4, path.length() - 5);
		if (path.startsWith("file:")) {
			path = path.substring(5);
			if (File.separator.equals("\\") && path.startsWith("/"))
				path = path.substring(1);
		}
		int slash = path.lastIndexOf('/');
		if (slash > 0) {
			slash = path.lastIndexOf('/', slash - 1);
			if (slash > 0)
				path = path.substring(0, slash);
		}
		return path;
	}

	//old
	public void update(URL baseURL, String fileName, String suffix,
				String targetPath)
			throws FileNotFoundException, IOException {
		new File(targetPath).getParentFile().mkdirs();
		copyFile(new URL(baseURL, fileName + suffix).openStream(),
				new FileOutputStream(targetPath));
	}
	//alternative new method?
	public void update(HttpURLConnection myConnection, String targetPath)
	throws FileNotFoundException, IOException {
		System.out.println(currentlyDownloading.getFilename() + " began downloading...");
		new File(targetPath).getParentFile().mkdirs();
		copyFile(myConnection.getInputStream(), new FileOutputStream(targetPath));
	}

	public void copyFile(InputStream in, OutputStream out)
			throws IOException {
		byte[] buffer = new byte[65536];
		int count;
		while ((count = in.read(buffer)) >= 0) {
			out.write(buffer, 0, count);
			downloadedBytes += count;
			System.out.println("Downloaded so far: " + downloadedBytes);
		}
		in.close();
		out.close();
	}

	public String getDigest(String path, String fullPath)
	throws NoSuchAlgorithmException, FileNotFoundException,
	IOException, UnsupportedEncodingException {
		if (path.endsWith(".jar"))
			return getJarDigest(fullPath);
		MessageDigest digest = getDigest();
		digest.update(path.getBytes("ASCII"));
		updateDigest(new FileInputStream(fullPath), digest);
		return toHex(digest.digest());
	}

	public static MessageDigest getDigest()
			throws NoSuchAlgorithmException {
		return MessageDigest.getInstance("SHA-1");
	}

	public void updateDigest(InputStream input, MessageDigest digest)
			throws IOException {
		byte[] buffer = new byte[65536];
		DigestInputStream digestStream =
			new DigestInputStream(input, digest);
		while (digestStream.read(buffer) >= 0)
			; /* do nothing */
		digestStream.close();
	}

	public String getJarDigest(String path)
			throws FileNotFoundException, IOException {
		MessageDigest digest = null;
		try {
			digest = getDigest();
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}

		JarFile jar = new JarFile(path);
		List list = new ArrayList();
		Enumeration entries = jar.entries();
		while (entries.hasMoreElements())
			list.add(entries.nextElement());
		Collections.sort(list, new JarEntryComparator());

		Iterator iter = list.iterator();
		while (iter.hasNext()) {
			JarEntry entry = (JarEntry)iter.next();
			digest.update(entry.getName().getBytes("ASCII"));
			updateDigest(jar.getInputStream(entry), digest);
		}
		return toHex(digest.digest());
	}

	public static String getPlatform() {
		boolean is64bit =
			System.getProperty("os.arch", "").indexOf("64") >= 0;
		String osName = System.getProperty("os.name", "<unknown>");
		if (osName.equals("Linux"))
			return "linux" + (is64bit ? "64" : "");
		if (osName.equals("Mac OS X"))
			return "macosx";
		if (osName.startsWith("Windows"))
			return "win" + (is64bit ? "64" : "32") + ".exe";
		System.err.println("Unknown platform: " + osName);
		return osName;
	}

	public String prefix(String path) {
		return fijiPath + File.separator
			+ (forServer && path.startsWith("fiji-") ?
					"precompiled/" : "")
			+ path;
	}

	public final static char[] hex = {
		'0', '1', '2', '3', '4', '5', '6', '7',
		'8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
	};

	public static String toHex(byte[] bytes) {
		char[] buffer = new char[bytes.length * 2];
		for (int i = 0; i < bytes.length; i++) {
			buffer[i * 2] = hex[(bytes[i] & 0xf0) >> 4];
			buffer[i * 2 + 1] = hex[bytes[i] & 0xf];
		}
		return new String(buffer);
	}

	private class JarEntryComparator implements Comparator {
		public int compare(Object o1, Object o2) {
			String name1 = ((JarEntry)o1).getName();
			String name2 = ((JarEntry)o2).getName();
			return name1.compareTo(name2);
		}

		public boolean equals(Object o1, Object o2) {
			String name1 = ((JarEntry)o1).getName();
			String name2 = ((JarEntry)o2).getName();
			return name1.equals(name2);
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

			//if (hasGUI) //we assume true for now okay?
			//IJ.showStatus("Updating " + name);
			String fullPath = prefix(updateDirectory +
					File.separator + name);
			try {
				if (name.startsWith("fiji-")) {
					fullPath = prefix((useMacPrefix ? macPrefix : "") + name);
					File orig = new File(fullPath);
					orig.renameTo(new File(fullPath + ".old"));
				}

				//Download the file specified at this iteration
				System.out.println("Trying to establish connection for " + new URL(new URL(updateURL), name + "-" + date).getPath());
				HttpURLConnection myConnection = (HttpURLConnection)(new URL(new URL(updateURL), name + "-" + date)).openConnection();
				System.out.println("Connection for " + new URL(new URL(updateURL), name + "-" + date).getPath() + " established.");
				update(myConnection, fullPath);
				myConnection.disconnect();
				//update(new URL(updateURL), name, "-" + date, fullPath);
				//String digest = (String)remote.digests.get(name);
				String realDigest = getDigest(name, fullPath);
				if (!realDigest.equals(digest))
					throw new Exception("Wrong checksum: Recorded Md5 sum " + digest + " != Actual Md5 sum " + realDigest);
				if (name.startsWith("fiji-") && !getPlatform().startsWith("win"))
					Runtime.getRuntime().exec(new String[] {
							"chmod", "0755", fullPath});
				waitingList.remove(currentlyDownloading);
				downloadedList.add(currentlyDownloading);
				System.out.println(currentlyDownloading.getFilename() + " finished download.");
				currentlyDownloading = null;
			} catch(Exception e) {
				//try to delete the file
				try {
					new File(fullPath).delete();
				} catch (Exception e2) { }
				throw new Error("Could not update " + name + ": " + e.getMessage());
			}
		}
	}

}