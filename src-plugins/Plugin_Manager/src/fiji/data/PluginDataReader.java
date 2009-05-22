package fiji.data;
import fiji.logic.Downloader;
import ij.IJ;
import ij.Menus;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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

		pluginList = new ArrayList<PluginObject>();
		latestDates = new TreeMap<String, String>();
		latestDigests = new TreeMap<String, String>();
		hasGUI = true;

		//currentDate = timestamp(Calendar.getInstance());
		
		String path = stripSuffix(stripSuffix(Menus.getPlugInsPath(),
				File.separator), "plugins");
		initialize(path);

		/*String url = gd.getNextString();
		try {
			update(new URL(url));
		} catch (MalformedURLException e) {
			IJ.write("Invalid URL: " + url);
		}*/

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
		PluginObject pluginA = new PluginObject("PluginA.jar", "65c3ecc1bbd7564f92545ffd2521f9d96509ca64", Long.parseLong("20090429190842"), "This is a description of Plugin A", null, PluginObject.STATUS_UNINSTALLED, PluginObject.ACTION_NONE);

		Dependency dependencyB1 = new Dependency("PluginA.jar", Long.parseLong("20090429190842"));
		ArrayList<Dependency> Bdependency = new ArrayList<Dependency>();
		Bdependency.add(dependencyB1);
		PluginObject pluginB = new PluginObject("PluginB.jar", "9624fa93cbf7720c01c7ff97c28b00747b700de3", Long.parseLong("20090429190854"), "This is a description of Plugin B", Bdependency, PluginObject.STATUS_UNINSTALLED, PluginObject.ACTION_NONE);

		Dependency dependencyC2 = new Dependency("PluginA.jar", Long.parseLong("20090429190842"));
		Dependency dependencyC3 = new Dependency("PluginB.jar", Long.parseLong("20090429190854"));
		ArrayList<Dependency> Cdependency = new ArrayList<Dependency>();
		Cdependency.add(dependencyC2);
		Cdependency.add(dependencyC3);
		PluginObject pluginC = new PluginObject("PluginC.jar", "1a992dbc077ef84020d44a980c7992ba6c8edf3d", Long.parseLong("20090429190854"), "This is a description of Plugin C", Cdependency, PluginObject.STATUS_UNINSTALLED, PluginObject.ACTION_NONE);

		PluginObject pluginD = new PluginObject("PluginD.jar", "61c3ecc1add7364f92545ffd2521e9d96508cb62", Long.parseLong("20090429190842"), "This is a description of Plugin D", null, PluginObject.STATUS_INSTALLED, PluginObject.ACTION_NONE);

		Dependency dependencyE4 = new Dependency("PluginA.jar", Long.parseLong("20090429190842"));
		ArrayList<Dependency> Edependency = new ArrayList<Dependency>();
		Edependency.add(dependencyE4);
		PluginObject pluginE = new PluginObject("PluginE.jar", "8114fe93cbf7720c01c7ff97c28b007b79900dc7", Long.parseLong("20090501190854"), "This is a description of Plugin E", Edependency, PluginObject.STATUS_MAY_UPDATE, PluginObject.ACTION_NONE);

		Dependency dependencyF5 = new Dependency("PluginE.jar",Long.parseLong("20090501190854"));
		Dependency dependencyF6 = new Dependency("PluginB.jar",Long.parseLong("20090429190854"));
		ArrayList<Dependency> Fdependency = new ArrayList<Dependency>();
		Fdependency.add(dependencyF5);
		Fdependency.add(dependencyF6);
		PluginObject pluginF = new PluginObject("PluginF.jar", "1b992dbca07ef84020d44a980c7902ba6c82dfee", Long.parseLong("20090509190854"), "This is a description of Plugin F", Fdependency, PluginObject.STATUS_MAY_UPDATE, PluginObject.ACTION_NONE);

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
			//dates.put(path, timestamp(modified));
			if (File.separator.equals("\\"))
				path = path.replace("\\", "/");
			//digests.put(path, digest);
			PluginObject myPlugin = new PluginObject(path, digest, Long.parseLong(timestamp(modified)));
			myPlugin.setStatus(PluginObject.STATUS_INSTALLED);
			pluginList.add(myPlugin);
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

	private void fetchUpdateInformation(String textFileURL) throws IOException {
		Downloader reader = new Downloader(textFileURL);
		String line = null;
		while ((line = reader.getNextLineFromTextFile()) != null) {
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
		
	}

	/* Called after PluginList is built from local plugin files */
	public void combineUpdatesWithPluginList(String strURL) {
		try {
			fetchUpdateInformation(strURL);
		} catch (FileNotFoundException e) {
			IJ.showMessage("No updates found");
			return; /* nothing to do, please move along */
		} catch (Exception e) {
			IJ.error("Error getting current versions: " + e);
			return;
		}

		for (int i = 0; i < pluginList.size(); i++) {
			PluginObject myPlugin = pluginList.get(i);
			String myPluginName = myPlugin.getFilename();
			String myPluginMd5Sum = myPlugin.getmd5Sum();
			long myPluginTimestamp = myPlugin.getTimestamp();

			//if current plugin is a Fiji plugin
			if (latestDigests.containsKey(myPluginName)) {
				/* launcher is platform-specific */
				if (myPluginName.startsWith("fiji-")) {
					String platform = getPlatform();
					if (!myPluginName.equals("fiji-" + platform) &&
						(!platform.equals("macosx") ||
						 !myPluginName.startsWith("fiji-tiger")))
						continue;
				}

				//Remove digests, dates checked for, remaining ones indicate NEW plugins
				String latestMd5Sum = latestDigests.remove(myPluginName);
				long latestTimestamp = Long.parseLong(latestDates.remove(myPluginName));

				//if same md5 sums
				if (myPluginMd5Sum != null && latestMd5Sum.equals(myPluginMd5Sum))
					continue;

				//if different md5 sums (implies need for update), but local date is newer
				if (latestTimestamp < myPluginTimestamp)
					continue; //Assume local modification, need not update

				//since different md5 sums and is local date is older
				myPlugin.setToUpdateable(latestMd5Sum, latestTimestamp);
				myPlugin.setStatus(PluginObject.STATUS_MAY_UPDATE);
			} else {
				//latest digests do not have file ==> Not considered Fiji plugin
				//do nothing...
			}
		}

		//As for the remaining updated-plugin information ==> Uninstalled Plugins
		Iterator<String> iter = latestDigests.keySet().iterator();
		while (iter.hasNext()) {
			String name = (String)iter.next();
			/* launcher is platform-specific */
			if (name.startsWith("fiji-")) {
				String platform = getPlatform();
				if (!name.equals("fiji-" + platform) &&
						(!platform.equals("macosx") ||
						!name.startsWith("fiji-tiger")))
					continue;
			}
			String md5Sum = latestDigests.get(name);
			long timestamp = Long.parseLong(latestDates.get(name));

			PluginObject newPlugin = new PluginObject(name, md5Sum, timestamp);
			newPlugin.setStatus(PluginObject.STATUS_UNINSTALLED);
			pluginList.add(newPlugin);
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
