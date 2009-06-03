package fiji.PluginManager;
import ij.IJ;
import ij.Menus;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
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

	private List<PluginObject> pluginList;
	private Map<String, String> digests;
	private Map<String, String> dates;
	private Map<String, String> latestDates;
	private Map<String, String> latestDigests;
	private String fijiPath;
	private boolean hasGUI;

	private PluginDataProcessor pluginDataProcessor;

	public PluginDataReader() {
		pluginList = new PluginCollection();
		if (!tempDemo) {

		dates = new TreeMap<String, String>();
		digests = new TreeMap<String, String>();
		latestDates = new TreeMap<String, String>();
		latestDigests = new TreeMap<String, String>();
		hasGUI = true;
		
		String path = stripSuffix(stripSuffix(Menus.getPlugInsPath(),
				File.separator),
				"plugins");
		pluginDataProcessor = new PluginDataProcessor(path);
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

		Dependency dependencyB1 = new Dependency("PluginA.jar", "20090429190842");
		ArrayList<Dependency> Bdependency = new ArrayList<Dependency>();
		Bdependency.add(dependencyB1);
		PluginObject pluginB = new PluginObject("PluginB.jar", "9624fa93cbf7720c01c7ff97c28b00747b700de3", "20090429190854", "This is a description of Plugin B", Bdependency, PluginObject.STATUS_UNINSTALLED, PluginObject.ACTION_NONE);

		Dependency dependencyC2 = new Dependency("PluginA.jar", "20090429190842");
		Dependency dependencyC3 = new Dependency("PluginB.jar", "20090429190854");
		ArrayList<Dependency> Cdependency = new ArrayList<Dependency>();
		Cdependency.add(dependencyC2);
		Cdependency.add(dependencyC3);
		PluginObject pluginC = new PluginObject("PluginC.jar", "1a992dbc077ef84020d44a980c7992ba6c8edf3d", "20090429190854", "This is a description of Plugin C", Cdependency, PluginObject.STATUS_UNINSTALLED, PluginObject.ACTION_NONE);

		PluginObject pluginD = new PluginObject("PluginD.jar", "61c3ecc1add7364f92545ffd2521e9d96508cb62", "20090429190842", "This is a description of Plugin D", null, PluginObject.STATUS_INSTALLED, PluginObject.ACTION_NONE);

		Dependency dependencyE4 = new Dependency("PluginA.jar", "20090429190842");
		ArrayList<Dependency> Edependency = new ArrayList<Dependency>();
		Edependency.add(dependencyE4);
		PluginObject pluginE = new PluginObject("PluginE.jar", "8114fe93cbf7720c01c7ff97c28b007b79900dc7", "20090501190854", "This is a description of Plugin E", Edependency, PluginObject.STATUS_MAY_UPDATE, PluginObject.ACTION_NONE);

		Dependency dependencyF5 = new Dependency("PluginE.jar","20090501190854");
		Dependency dependencyF6 = new Dependency("PluginB.jar","20090429190854");
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

	private String stripSuffix(String string, String suffix) {
		if (!string.endsWith(suffix))
			return string;
		return string.substring(0, string.length() - suffix.length());
	}

	public void initialize(String fijiPath) {
		initialize(fijiPath, null);
	}

	//NOTE: This method along with initialize(String fijiPath) looks redundant, will
	//look into it again
	public void initialize(String fijiPath, String[] only) {
		//is the below line even needed?
		//this.fijiPath = (fijiPath == null ? pluginDataProcessor.getDefaultFijiPath() : fijiPath);
		List<String> queue = new ArrayList<String>();

		//To get a list of plugins on the local side
		if (only == null || only.length == 0) {
			String platform = pluginDataProcessor.getPlatform();
			String macPrefix = pluginDataProcessor.getMacPrefix();
			boolean useMacPrefix = pluginDataProcessor.getUseMacPrefix();
			if (platform.equals("macosx")) {
				queue.add((useMacPrefix ? macPrefix : "") + "fiji-macosx");
				queue.add((useMacPrefix ? macPrefix : "") + "fiji-tiger");
			} else
				queue.add("fiji-" + platform);

			queue.add("ij.jar");
			queueDirectory(queue, "plugins");
			queueDirectory(queue, "jars");
			queueDirectory(queue, "retro");
			queueDirectory(queue, "misc");
		} else {
			System.out.println("very second.....");
			for (int i = 0; i < only.length; i++)
				queue.add(only[i]);
		}

		Iterator<String> iter = queue.iterator();
		int i = 0, total = queue.size();
		while (iter.hasNext()) {
			String name = (String)iter.next();
			//To do: Perhaps move GUI-related stuff over to PluginManager
			if (hasGUI)
				IJ.showStatus("Checksumming " + name + "...");

			String[] digestAndDate = pluginDataProcessor.getDigestAndDateFromFile(name);

			//index 0: path name, index 1: digest, index 2: date
			if (digestAndDate != null && digestAndDate[1] != null && digestAndDate[2] != null) {
				digests.put(digestAndDate[0], digestAndDate[1]);
				dates.put(digestAndDate[0], digestAndDate[2]);
			}

			if (hasGUI)
				IJ.showProgress(++i, total);
		}
		if (hasGUI)
			IJ.showStatus("");
	}

	//recursively looks into a directory and adds the relevant file
	public void queueDirectory(List<String> queue, String path) {
		File dir = new File(pluginDataProcessor.prefix(path));
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
	/*public void copyFile(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[65536];
		int count;
		while ((count = in.read(buffer)) >= 0) {
			out.write(buffer, 0, count);
			downloadedBytes += count;
			System.out.println("Downloaded so far: " + downloadedBytes);
		}
		in.close();
		out.close();
	}*/

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
				String platform = pluginDataProcessor.getPlatform();
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
				myPlugin.setToUpdateable(remoteDigest, remoteDate, null);
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

	public PluginDataProcessor getPluginDataProcessor() {
		return pluginDataProcessor;
	}
}
