package fiji.data;
import ij.IJ;
import ij.Menus;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import java.net.URL;
import java.net.URLDecoder;

import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.text.DecimalFormat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class PluginDataReader {
	private boolean tempDemo = true; //if true, use UpdateFiji.java's code...

	private List downloadNameList = null; //tentatively, array of strings from Downloader
	private List<PluginObject> pluginList = null;

	private Map<String, String> digests = null;
	private Map<String, String> dates = null;
	private Map<String, String> latestDates = null;
	private Map<String, String> latestDigests = null;
	private String fijiPath;
	//private String currentDate;
	private boolean hasGUI = false;
	private boolean forServer = false;
	private final String macPrefix = "Contents/MacOS/";
	private boolean useMacPrefix = false;

	public PluginDataReader() {
		if (tempDemo) {

		pluginList = new PluginCollection();
		dates = new TreeMap<String, String>();
		digests = new TreeMap<String, String>();
		latestDates = new TreeMap<String, String>();
		latestDigests = new TreeMap<String, String>();
		hasGUI = true;
		
		String path = stripSuffix(stripSuffix(Menus.getPlugInsPath(),
				File.separator),
				"plugins");
		initialize(path);

		} else {


		//How to get "database" of information:
		//1.) build a list of installed plugins
		//2.) Add these to DB, default status "Installed"
		//3.) using _same_ method, derive an updated list from current.txt
		//4.) -New plugins on current.txt added to DB as "Uninstalled",
		//    -Plugins with updates as indicated on current.txt, change to "Update-able",
		//    -Plugins with same timestamp as that of current.txt, remain as it is.
		//5.) Get additional information (dependencies and descriptions) from
		//    "database.xml" and add to DB.
		//
		// Hmmm... For update-able plugins, should there be an extra attribute
		// "latestTimestamp" as well? (Thus need not go back to current.txt to fetch
		// timestamp info in the case of update)

		//retrieve information of installed plugins...
		PluginObject pluginA = new PluginObject("PluginA.jar", "65c3ecc1bbd7564f92545ffd2521f9d96509ca64", "20090429190842", "This is a description of Plugin A", null, PluginObject.STATUS_UNINSTALLED, PluginObject.ACTION_NONE);

		Dependency dependencyB1 = new Dependency("PluginA.jar", Long.parseLong("20090429190842"));
		ArrayList<Dependency> Bdependency = new ArrayList<Dependency>();
		Bdependency.add(dependencyB1);
		PluginObject pluginB = new PluginObject("PluginB.jar", "9624fa93cbf7720c01c7ff97c28b00747b700de3", "20090429190854", "This is a description of Plugin B", Bdependency, PluginObject.STATUS_UNINSTALLED, PluginObject.ACTION_NONE);

		Dependency dependencyC2 = new Dependency("PluginA.jar", Long.parseLong("20090429190842"));
		Dependency dependencyC3 = new Dependency("PluginB.jar", Long.parseLong("20090429190854"));
		ArrayList<Dependency> Cdependency = new ArrayList<Dependency>();
		Cdependency.add(dependencyC2);
		Cdependency.add(dependencyC3);
		PluginObject pluginC = new PluginObject("PluginC.jar", "1a992dbc077ef84020d44a980c7992ba6c8edf3d", "20090429190854", "This is a description of Plugin C", Cdependency, PluginObject.STATUS_UNINSTALLED, PluginObject.ACTION_NONE);

		PluginObject pluginD = new PluginObject("PluginD.jar", "61c3ecc1add7364f92545ffd2521e9d96508cb62", "20090429190842", "This is a description of Plugin D", null, PluginObject.STATUS_INSTALLED, PluginObject.ACTION_NONE);

		Dependency dependencyE4 = new Dependency("PluginA.jar", Long.parseLong("20090429190842"));
		ArrayList<Dependency> Edependency = new ArrayList<Dependency>();
		Edependency.add(dependencyE4);
		PluginObject pluginE = new PluginObject("PluginE.jar", "8114fe93cbf7720c01c7ff97c28b007b79900dc7", "20090501190854", "This is a description of Plugin E", Edependency, PluginObject.STATUS_MAY_UPDATE, PluginObject.ACTION_NONE);

		Dependency dependencyF5 = new Dependency("PluginE.jar",Long.parseLong("20090501190854"));
		Dependency dependencyF6 = new Dependency("PluginB.jar",Long.parseLong("20090429190854"));
		ArrayList<Dependency> Fdependency = new ArrayList<Dependency>();
		Fdependency.add(dependencyF5);
		Fdependency.add(dependencyF6);
		PluginObject pluginF = new PluginObject("PluginF.jar", "1b992dbca07ef84020d44a980c7902ba6c82dfee", "20090509190854", "This is a description of Plugin F", Fdependency, PluginObject.STATUS_MAY_UPDATE, PluginObject.ACTION_NONE);

		pluginList.add(pluginA);
		pluginList.add(pluginB);
		pluginList.add(pluginC);
		pluginList.add(pluginD);
		pluginList.add(pluginE);
		pluginList.add(pluginF);

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

	public void initialize(String fijiPath) {
		initialize(fijiPath, null);
	}

	public void initialize(String fijiPath, String[] only) {
		this.fijiPath = (fijiPath == null ? getDefaultFijiPath() : fijiPath);

		List<String> queue = new ArrayList<String>();

		if (only == null || only.length == 0) {
			String platform = getPlatform();
			if (platform.equals("macosx")) {
				String macLauncher = macPrefix + "fiji-macosx";
				if (new File(prefix(macLauncher)).exists())
					useMacPrefix = true;
				queue.add((useMacPrefix ? macPrefix : "") + "fiji-macosx");
				queue.add((useMacPrefix ? macPrefix : "") + "fiji-tiger");
			} else
				queue.add("fiji-" + platform);

			queue.add("ij.jar");
			queueDirectory(queue, "plugins");
			queueDirectory(queue, "jars");
			queueDirectory(queue, "retro");
			queueDirectory(queue, "misc");
		} else
			for (int i = 0; i < only.length; i++)
				queue.add(only[i]);

		Iterator<String> iter = queue.iterator();
		int i = 0, total = queue.size();
		while (iter.hasNext()) {
			String name = (String)iter.next();
			//To do: Perhaps move GUI-related stuff over to PluginManager
			if (hasGUI)
				IJ.showStatus("Checksumming " + name + "...");
			initializeFile(name);
			if (hasGUI)
				IJ.showProgress(++i, total);
		}
		if (hasGUI)
			IJ.showStatus("");
	}

	public void queueDirectory(List<String> queue, String path) {
		File dir = new File(prefix(path));
		if (!dir.isDirectory())
			return;
		String[] list = dir.list();
		for (int i = 0; i < list.length; i++)
			if (list[i].equals(".") || list[i].equals(".."))
				continue;
			else if (list[i].endsWith(".jar"))
				queue.add(path + File.separator + list[i]);
			else
				queueDirectory(queue,
					path + File.separator + list[i]);
	}

	public void initializeFile(String path) {
		try {
			String fullPath = prefix(path);
			String digest = getDigest(path, fullPath);
			long modified = new File(fullPath).lastModified();
			if (useMacPrefix && path.startsWith(macPrefix))
				path = path.substring(macPrefix.length());
			if (File.separator.equals("\\"))
				path = path.replace("\\", "/");
			digests.put(path, digest);
			dates.put(path, timestamp(modified));
		} catch (Exception e) {
			if (e instanceof FileNotFoundException &&
					path.startsWith("fiji-"))
				return;
			System.err.println("Could not get digest: "
					+ prefix(path) + " (" + e + ")");
			e.printStackTrace();
		}
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

	public String getJarDigest(String path) throws FileNotFoundException, IOException {
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

	public String prefix(String path) {
		return fijiPath + File.separator
			+ (forServer && path.startsWith("fiji-") ?
					"precompiled/" : "")
			+ path;
	}

	public static String stripSuffix(String string, String suffix) {
		if (!string.endsWith(suffix))
			return string;
		return string.substring(0, string.length() - suffix.length());
	}

	public static String timestamp(Calendar date) {
		DecimalFormat format = new DecimalFormat("00");
		int month = date.get(Calendar.MONTH) + 1;
		int day = date.get(Calendar.DAY_OF_MONTH);
		int hour = date.get(Calendar.HOUR_OF_DAY);
		int minute = date.get(Calendar.MINUTE);
		int second = date.get(Calendar.SECOND);
		return "" + date.get(Calendar.YEAR) +
			format.format(month) + format.format(day) +
			format.format(hour) + format.format(minute) +
			format.format(second);
	}

	public static String timestamp(long millis) {
		Calendar date = Calendar.getInstance();
		date.setTimeInMillis(millis);
		return timestamp(date);
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

	public List<PluginObject> getExistingPluginList() {
		return pluginList;
	}

	public void initializeFromList(InputStream input) throws IOException {
		BufferedReader in =
			new BufferedReader(new InputStreamReader(input));
		String line;
		while ((line = in.readLine()) != null) {
			int space = line.indexOf(' ');
			if (space < 0)
				continue;
			String path = line.substring(0, space);
			int space2 = line.indexOf(' ', space + 1);
			if (space2 < 0)
				continue;
			String date = line.substring(space + 1, space2);
			String digest = line.substring(space2 + 1);
			latestDates.put(path, date);
			latestDigests.put(path, digest);
		}
		in.close();
	}

	/* Called after local plugin files have been processed */
	public void buildFullPluginList(URL listFile) {
	//public void combineUpdatesWithPluginList(String strURL) {
		try {
			initializeFromList(listFile.openStream());
			//fetchUpdateInformation(strURL);
		} catch (FileNotFoundException e) {
			IJ.showMessage("No updates found");
			return; /* nothing to do, please move along */
		} catch (Exception e) {
			IJ.error("Error getting current versions: " + e);
			return;
		}

		Iterator<String> iterLatest = latestDigests.keySet().iterator();
		while (iterLatest.hasNext()) {
			String name = iterLatest.next();

			// launcher is platform-specific
			if (name.startsWith("fiji-")) {
				String platform = getPlatform();
				if (!name.equals("fiji-" + platform) &&
						(!platform.equals("macosx") ||
						!name.startsWith("fiji-tiger")))
					continue;
			}

			String digest = digests.get(name);
			String remoteDigest = latestDigests.get(name);
			String date = dates.get(name);
			String remoteDate = latestDates.get(name);
			PluginObject myPlugin = null;

			System.out.println(name + ", digest: " + digest + ", timestamp: " + date);

			if (digest != null && remoteDigest.equals(digest)) {
				myPlugin = new PluginObject(name, digest, date);
				myPlugin.setStatus(PluginObject.STATUS_INSTALLED);
				pluginList.add(myPlugin);
				continue;
			}
			/*if (date != null && date.compareTo(remoteDate) > 0) {
				myPlugin = new PluginObject(name, digest, date);
				myPlugin.setStatus(PluginObject.STATUS_INSTALLED);
				pluginList.add(myPlugin);
				continue; // local modification
			}*/
			//if new file
			if (digest == null) {
				myPlugin = new PluginObject(name, remoteDigest, remoteDate);
				myPlugin.setStatus(PluginObject.STATUS_UNINSTALLED);
				pluginList.add(myPlugin);
			} else { //if its to be updated
				myPlugin = new PluginObject(name, digest, date);
				myPlugin.setToUpdateable(remoteDigest, remoteDate);
				myPlugin.setStatus(PluginObject.STATUS_MAY_UPDATE);
				pluginList.add(myPlugin);
			}
		}

		Iterator<String> iterCurrent = digests.keySet().iterator();
		while (iterCurrent.hasNext()) {
			String name = iterCurrent.next();

			// if it is not a Fiji plugin (Not found in list of up-to-date versions)
			if (!latestDigests.containsKey(name)) {
				String digest = digests.get(name);
				String date = dates.get(name);
				//implies third-party plugin
				PluginObject myPlugin = new PluginObject(name, digest, date);
				myPlugin.setStatus(PluginObject.STATUS_INSTALLED);
				//add it anyway?
				pluginList.add(myPlugin);
			}
		}

		/*
		boolean someAreNotWritable = false;
		for (int i = 0; i < list.size(); i++) {
			File file = new File((String)list.get(i));
			if (!file.exists() || file.canWrite())
				continue;
			IJ.log("Read-only file: " + list.get(i));
			someAreNotWritable = true;
			list.remove(i--);
		}

		if (someAreNotWritable) {
			String msg = " of the updateable files are writable.";
			if (list.size() == 0) {
				IJ.error("None" + msg);
				return;
			}
			IJ.showMessage("Some" + msg);
		}*/


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
}
