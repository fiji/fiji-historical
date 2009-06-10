package fiji.pluginManager;
import ij.IJ;
import ij.Menus;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

/*
 * PluginDataReader's overall role is to be in charge of building of a plugin list
 * for interface usage. In other words, its instructions are to first read through
 * data of local plugins, then upon command, download remote information about
 * plugin updates, and from there build up the actual list ("PluginObject").
 */
public class PluginDataReader implements Observable, Observer {
	public boolean tempDemo = true; //if true, use artificial database...

	static byte STATUS_INACTIVE = 0; //doing nothing
	static byte STATUS_CALC = 1; //calculating Md5 sums
	static byte STATUS_DOWNLOAD = 2; //downloading important information
	private byte readerStatus = STATUS_INACTIVE; //default
	private String filename;
	private int currentlyLoaded;
	private int totalToLoad;
	private final String infoDirectory = "plugininfo";

	private LoadStatusDisplay loadStatusDisplay;
	private Vector<Observer> observersList;
	private List<PluginObject> pluginList;
	private Map<String, String> digests;
	private Map<String, String> dates;
	private Map<String, String> latestDates;
	private Map<String, String> latestDigests;

	private PluginDataProcessor pluginDataProcessor;

	public PluginDataReader() {
		observersList = new Vector<Observer>();
		loadStatusDisplay = new LoadStatusDisplay(this);
		register(loadStatusDisplay);

		pluginList = new PluginCollection();

		if (!tempDemo) {

		dates = new TreeMap<String, String>();
		digests = new TreeMap<String, String>();
		latestDates = new TreeMap<String, String>();
		latestDigests = new TreeMap<String, String>();
		
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
		Dependency dependencyA1 = new Dependency("PluginD.jar", "20090420190033");
		ArrayList<Dependency> Adependency = new ArrayList<Dependency>();
		Adependency.add(dependencyA1);
		PluginObject pluginA = new PluginObject("PluginA.jar", "65c3ecc1bbd7564f92545ffd2521f9d96509ca64", "20090429190842", "This is a description of Plugin A", Adependency, PluginObject.STATUS_UNINSTALLED, PluginObject.ACTION_NONE);

		Dependency dependencyB1 = new Dependency("PluginD.jar", "20090420190033");
		Dependency dependencyB2 = new Dependency("PluginH.jar", "20090524666220");
		Dependency dependencyB3 = new Dependency("PluginC.jar", "20081011183621");
		ArrayList<Dependency> Bdependency = new ArrayList<Dependency>();
		Bdependency.add(dependencyB1);
		Bdependency.add(dependencyB2);
		Bdependency.add(dependencyB3);
		PluginObject pluginB = new PluginObject("PluginB.jar", "9624fa93cbf7720c01c7ff97c28b00747b700de3", "20090429190854", "This is a description of Plugin B", Bdependency, PluginObject.STATUS_UNINSTALLED, PluginObject.ACTION_NONE);

		PluginObject pluginC = new PluginObject("PluginC.jar", "1a992dbc077ef84020d44a980c7992ba6c8edf3d", "20090425190854", "This is a description of Plugin C", null, PluginObject.STATUS_INSTALLED, PluginObject.ACTION_NONE);

		Dependency dependencyD1 = new Dependency("PluginF.jar", "20090420191023");
		//Dependency dependencyD2 = new Dependency("PluginL.jar", "20090220616220");
		Dependency dependencyD3 = new Dependency("PluginE.jar", "20090311213621");
		ArrayList<Dependency> Ddependency = new ArrayList<Dependency>();
		Ddependency.add(dependencyD1);
		//Ddependency.add(dependencyD2);
		Ddependency.add(dependencyD3);
		PluginObject pluginD = new PluginObject("PluginD.jar", "61c3ecc1add7364f92545ffd2521e9d96508cb62", "20090420190033", "This is a description of Plugin D", Ddependency, PluginObject.STATUS_UNINSTALLED, PluginObject.ACTION_NONE);

		Dependency dependencyE1 = new Dependency("PluginG.jar", "20090125190842");
		ArrayList<Dependency> Edependency = new ArrayList<Dependency>();
		Edependency.add(dependencyE1);
		PluginObject pluginE = new PluginObject("PluginE.jar", "8114fe93cbf7720c01c7ff97c28b007b79900dc7", "20090311213621", "This is a description of Plugin E", Edependency, PluginObject.STATUS_UNINSTALLED, PluginObject.ACTION_NONE);

		Dependency dependencyF1 = new Dependency("PluginI.jar", "20090501190854");
		ArrayList<Dependency> Fdependency = new ArrayList<Dependency>();
		Fdependency.add(dependencyF1);
		PluginObject pluginF = new PluginObject("PluginF.jar", "1b992dbca07ef84020d44a980c7902ba6c82dfee", "20090420191023", "This is a description of Plugin F", Fdependency, PluginObject.STATUS_UNINSTALLED, PluginObject.ACTION_NONE);
		
		PluginObject pluginG = new PluginObject("PluginG.jar", "1a992dbc077ef84020d44a980c7992ba6c8edf3d", "20090415160854", "This is a description of Plugin G", null, PluginObject.STATUS_MAY_UPDATE, PluginObject.ACTION_NONE);
		pluginG.setUpdateDetails("2c992db3327ef8402bd44b980c7992da6c8eefd9", "20090502130854", null);
		
		Dependency dependencyH1 = new Dependency("PluginD.jar", "20090420190033");
		ArrayList<Dependency> HnewDependency = new ArrayList<Dependency>();
		HnewDependency.add(dependencyH1);
		PluginObject pluginH = new PluginObject("PluginH.jar", "33c88dc1fbd7564f92587ffdc521f9de6507ca65", "20081224666220", "This is a description of Plugin H", null, PluginObject.STATUS_MAY_UPDATE, PluginObject.ACTION_NONE);
		pluginH.setUpdateDetails("23d88dc1fbd7564f92087ffdc529acce6500ef60", "20090524666220", HnewDependency);
		
		Dependency dependencyI1 = new Dependency("PluginF.jar", "20090420191023");
		Dependency dependencyI2 = new Dependency("PluginK.jar", "20081221866291");
		ArrayList<Dependency> Idependency = new ArrayList<Dependency>();
		Idependency.add(dependencyI1);
		Idependency.add(dependencyI2);
		PluginObject pluginI = new PluginObject("PluginI.jar", "9624fa93cbf7720c01c7ff97c28b00747b700de3", "20090429190854", "This is a description of Plugin I", Idependency, PluginObject.STATUS_UNINSTALLED, PluginObject.ACTION_NONE);

		Dependency dependencyJ1 = new Dependency("PluginI.jar", "20090404090854");
		ArrayList<Dependency> Jdependency = new ArrayList<Dependency>();
		Jdependency.add(dependencyJ1);
		PluginObject pluginJ = new PluginObject("PluginJ.jar", "9624fa93cbf7720c01c7ff97c28b00747b700de3", "20090521181954", "This is a description of Plugin J", Jdependency, PluginObject.STATUS_UNINSTALLED, PluginObject.ACTION_NONE);
		
		Dependency dependencyK1 = new Dependency("PluginJ.jar", "20090404090854");
		ArrayList<Dependency> Kdependency = new ArrayList<Dependency>();
		Kdependency.add(dependencyK1);
		PluginObject pluginK = new PluginObject("PluginK.jar", "9624fa93cbf7720c01c7ff97c28b00747b700de3", "20081221866291", "This is a description of Plugin K", Kdependency, PluginObject.STATUS_UNINSTALLED, PluginObject.ACTION_NONE);

		pluginList.add(pluginA);
		pluginList.add(pluginB);
		pluginList.add(pluginC);
		pluginList.add(pluginD);
		pluginList.add(pluginE);
		pluginList.add(pluginF);
		pluginList.add(pluginG);
		pluginList.add(pluginH);
		pluginList.add(pluginI);
		pluginList.add(pluginJ);
		pluginList.add(pluginK);
		}
	}

	private String stripSuffix(String string, String suffix) {
		if (!string.endsWith(suffix))
			return string;
		return string.substring(0, string.length() - suffix.length());
	}

	//Generate required information from the local plugins
	private void initialize(String fijiPath) {
		List<String> queue = new ArrayList<String>();

		//To get a list of plugins on the local side
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

		//To calculate the Md5 sums on the local side
		Iterator<String> iter = queue.iterator();
		readerStatus = PluginDataReader.STATUS_CALC;
		currentlyLoaded = 0;
		totalToLoad = queue.size();
		while (iter.hasNext()) {
			filename = (String)iter.next();
			String[] digestAndDate = pluginDataProcessor.getDigestAndDateFromFile(filename);

			//index 0: path name, index 1: digest, index 2: date
			if (digestAndDate != null && digestAndDate[1] != null && digestAndDate[2] != null) {
				digests.put(digestAndDate[0], digestAndDate[1]);
				dates.put(digestAndDate[0], digestAndDate[2]);
			}

			++currentlyLoaded;
			notifyObservers();
		}
	}

	public byte getReaderStatus() {
		return readerStatus;
	}

	public String getFilename() {
		return filename;
	}

	public int getCurrentlyLoaded() {
		return currentlyLoaded;
	}

	public int getTotalToLoad() {
		return totalToLoad;
	}

	public List<PluginObject> getExistingPluginList() {
		return pluginList;
	}

	public PluginDataProcessor getPluginDataProcessor() {
		return pluginDataProcessor;
	}

	//recursively looks into a directory and adds the relevant file
	private void queueDirectory(List<String> queue, String path) {
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

	private void readUpdateFile(String fileLocation) throws Exception {
		FileInputStream fstream = new FileInputStream(fileLocation);
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String line;
		while ((line = br.readLine()) != null) {
			int space = line.indexOf(' ');
			if (space < 0)
				continue;
			String path = line.substring(0, space);
			int space2 = line.indexOf(' ', space + 1);
			if (space2 < 0)
				continue;
			String date = line.substring(space + 1, space2);
			String digest = line.substring(space2 + 1);
			//Note, you can check date and digest for validity before adding to the treemaps
			latestDates.put(path, date);
			latestDigests.put(path, digest);
		}
		br.close();
	}

	private void readDatabaseFile() {
		//placeholder for perhaps XML parsing of the DB file
		
		//Role is to determine the timestamp (i.e.: version) of the plugins on local side
	}

	/* Called after local plugin files have been processed */
	public void buildFullPluginList(String updateURL, String updateFile) {
		//First file to download: updateFile from updateURL
		currentlyLoaded = 0;
		totalToLoad = 0;
		readerStatus = PluginDataReader.STATUS_DOWNLOAD;
		String updateFileLocation = pluginDataProcessor.prefix(infoDirectory +
				File.separator + updateFile);
		filename = updateFile;
		notifyObservers();

		//save content downloaded to a local folder
		try {
			//Establishes connection
			Downloader downloader = new Downloader(updateURL, updateFileLocation);
			downloader.register(this);
			totalToLoad += downloader.getSize();

			//Prepare the necessary download input and output streams
			downloader.prepareDownload();
			byte[] buffer = downloader.createNewBuffer();
			int count;

			//Start actual downloading and writing to file
			while ((count = downloader.getNextPart(buffer)) >= 0) {
				downloader.writePart(buffer, count);
			}
			downloader.endConnection(); //end connection once download done

		} catch(Exception e) {
			//try to delete the file (probably this be the only catch - DRY)
			try {
				new File(updateFileLocation).delete();
			} catch (Exception e2) { }
			throw new Error("Could not download " + updateFile + " successfully: " + e.getMessage());
		}

		//reads the file downloaded
		try {
			readUpdateFile(updateFileLocation);
			readDatabaseFile();
		} catch (Exception e) {
			throw new Error("Failed to read the downloaded file(s). Error Message: " + e.getMessage());
		}

		//Converts data gathered into lists of PluginObject, ready for UI classes usage
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
				myPlugin = new PluginObject(name, digest, date, PluginObject.STATUS_INSTALLED);
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
				myPlugin = new PluginObject(name, remoteDigest, remoteDate, PluginObject.STATUS_UNINSTALLED);
				pluginList.add(myPlugin);
			} else { //if its to be updated
				myPlugin = new PluginObject(name, digest, date, PluginObject.STATUS_MAY_UPDATE);
				myPlugin.setUpdateDetails(remoteDigest, remoteDate, null);
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
				PluginObject myPlugin = new PluginObject(name, digest, date, PluginObject.STATUS_INSTALLED);
				pluginList.add(myPlugin);
			}
		}

		readerStatus = PluginDataReader.STATUS_INACTIVE;
		notifyObservers();
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

	//Being observed, PluginDataReader notifies LoadStatusDisplay
	public void notifyObservers() {
		// Send notify to all Observers
		for (int i = 0; i < observersList.size(); i++) {
			Observer observer = (Observer) observersList.elementAt(i);
			observer.refreshData(this);
		}
	}

	//Being observed, PluginDataReader adds observers
	public void register(Observer obs) {
		observersList.addElement(obs);
	}

	public void unRegister(Observer obs) {}

	//As Observer of Downloaders, PluginDataReader gathers download information
	public void refreshData(Observable subject) {
		Downloader myDownloader = (Downloader)subject;
		currentlyLoaded += myDownloader.getNumOfBytes();
		System.out.println("Downloaded so far: " + currentlyLoaded);
		notifyObservers(); //Notify since data is observed by LoadStatusDisplay
	}

}

class LoadStatusDisplay implements Observer {
	private PluginDataReader pluginDataReader;

	public LoadStatusDisplay(PluginDataReader pluginDataReader) {
		this.pluginDataReader = pluginDataReader;
		IJ.showStatus("Starting up Plugin Manager");
	}

	public void refreshData(Observable subject) {
		if (subject == pluginDataReader) { //if pluginDataReader is sending data directly
			if (pluginDataReader.getReaderStatus() == PluginDataReader.STATUS_CALC) {
				IJ.showStatus("Checksumming " + pluginDataReader.getFilename() + "...");
				IJ.showProgress(pluginDataReader.getCurrentlyLoaded(), pluginDataReader.getTotalToLoad());
			} else if (pluginDataReader.getReaderStatus() == PluginDataReader.STATUS_DOWNLOAD) {
				IJ.showStatus("Downloading " + pluginDataReader.getFilename() + "...");
				int percentage = 0;
				if (pluginDataReader.getTotalToLoad() > 0) {
					percentage = pluginDataReader.getCurrentlyLoaded() * 100 / pluginDataReader.getTotalToLoad();
				}
				IJ.showProgress(percentage, 100);
			} else {
				IJ.showStatus("");
			}
		}
	}

}
