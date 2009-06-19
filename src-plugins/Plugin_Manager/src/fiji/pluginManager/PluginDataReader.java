package fiji.pluginManager;
import ij.IJ;
import ij.Menus;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/*
 * PluginDataReader's overall role is to be in charge of building of a plugin list
 * for interface usage.
 * 
 * 1st step: Download information about Fiji plugins.
 * 2nd step: Get information of local plugins (Md5 sums and version)
 * 3rd step: Get information of latest Fiji plugins (Md5 sums and version)
 * 4th step: Build up list of "PluginObject" using both local and updates
 * 
 * Note that 3rd and 4th step are combined into a single method.
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

	private LoadStatusDisplay loadStatusDisplay;
	private Vector<Observer> observersList;
	private List<PluginObject> pluginList;
	private Map<String, String> digests;
	private Map<String, String> dates;
	private Map<String, String> latestDates;
	private Map<String, String> latestDigests;

	private final String infoDirectory = "plugininfo";
	private String path; //location of local plugins
	private PluginDataProcessor pluginDataProcessor;
	private XMLFileReader xmlFileReader;
	private String saveFileLocation;
	private String saveFile;
	private String fileURL;

	public PluginDataReader(String fileURL, String saveFile) {
		this.saveFile = saveFile;
		this.fileURL = fileURL;

		//set up observers
		observersList = new Vector<Observer>();
		loadStatusDisplay = new LoadStatusDisplay(this);
		register(loadStatusDisplay);

		//initialize storage
		dates = new TreeMap<String, String>();
		digests = new TreeMap<String, String>();
		latestDates = new TreeMap<String, String>();
		latestDigests = new TreeMap<String, String>();
		pluginList = new PluginCollection();

		//initialize location of downloads and local plugins
		path = stripSuffix(stripSuffix(Menus.getPlugInsPath(),
				File.separator),
				"plugins");
		pluginDataProcessor = new PluginDataProcessor(path);
		saveFileLocation = pluginDataProcessor.prefix(infoDirectory +
				File.separator + saveFile);
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

	public void downloadXMLFile() {
		//progress starts out at 0 for download of a single file
		currentlyLoaded = 0 ;
		totalToLoad = 0;
		readerStatus = PluginDataReader.STATUS_DOWNLOAD;
		filename = saveFile;
		notifyObservers();

		try {

			//Establishes connection
			Downloader downloader = new Downloader(fileURL, saveFileLocation);
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

		} catch (Exception e) {
			try {
				new File(saveFileLocation).delete();
			} catch (Exception e2) { }
			throw new Error("Could not download " + saveFile + " successfully: " + e.getMessage());
		}
	}

	public void buildLocalPluginInformation() {
		try {
		if (!tempDemo)
			xmlFileReader = new XMLFileReader(saveFileLocation);
		else
			xmlFileReader = new XMLFileReader(infoDirectory +
				File.separator + "pluginRecords.xml"); //temporary hardcode

			//To get a list of plugins on the local side
			List<String> queue = new ArrayList<String>();
			String platform = pluginDataProcessor.getPlatform();
			String macPrefix = pluginDataProcessor.getMacPrefix();
			boolean useMacPrefix = pluginDataProcessor.getUseMacPrefix();

		if (!tempDemo) {

			//Gather filenames of all local plugins
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

			queue.add("PluginE.jar");
			queue.add("PluginG.jar");
			queue.add("PluginH.jar");
			queue.add("PluginL.jar");
			digests.put("PluginE.jar", "8114fe93cbf7720c01c7ff97c28b007b79900dc7");
			//digests.put("PluginE.jar", "cb19fe73cbf562c011e7fa87c28b007b7c990dc3"); //test non-existent digest
			digests.put("PluginG.jar", "1a992dbc077ef84020d44a980c7992ba6c8edf3d");
			//digests.put("PluginH.jar", "33c88dc1fbd7564f92587ffdc521f9de6507ca65");
			digests.put("PluginH.jar", "43e68dc9fbd7964ecd587ffdc621f9de6050ba69"); //test non-existent digest
			digests.put("PluginL.jar", "69ba8dc9fbd8945ec5e43fdfc612f9ec6150e644"); //test non-Fiji plugin
		}

			//To calculate the Md5 sums on the local side
			Iterator<String> iter = queue.iterator();
			readerStatus = PluginDataReader.STATUS_CALC;
			currentlyLoaded = 0;
			totalToLoad = queue.size();
			while (iter.hasNext()) {
				filename = (String)iter.next();
				String outputFilename = filename;
				String outputDigest;
				String outputDate;

				if (useMacPrefix && outputFilename.startsWith(macPrefix))
					outputFilename = outputFilename.substring(macPrefix.length());
				if (File.separator.equals("\\"))
					outputFilename = outputFilename.replace("\\", "/");

			if (!tempDemo) {
				outputDigest = pluginDataProcessor.getDigestFromFile(filename);
				digests.put(outputFilename, outputDigest);
			} else {
				//temporary line of code
				outputDigest = digests.get(outputFilename);
			}

				//if XML file does not contain plugin filename or digest does not exist
				if (!xmlFileReader.existsFilename(outputFilename) ||
					!xmlFileReader.existsDigest(outputFilename, outputDigest)) {
					//use the local plugin's last modified timestamp instead
					if (!tempDemo) {
					outputDate = pluginDataProcessor.getTimestampFromFile(filename);
					} else {
					outputDate = "20090619999666"; //assume latest... always
					}
				} else {
					//if it does exist, then use the associated timestamp as recorded
					//NOTE: if it is a temporary demo, then it SHOULD always end up here
					outputDate = xmlFileReader.getTimestamp(outputFilename, outputDigest);
				}
				dates.put(outputFilename, outputDate);

				++currentlyLoaded;
				notifyObservers();
			}

		} catch (ParserConfigurationException e1) {
			throw new Error(e1.getLocalizedMessage());
		} catch (IOException e2) {
			throw new Error(e2.getLocalizedMessage());
		} catch (SAXException e3) {
			throw new Error(e3.getLocalizedMessage());
		} catch (XPathExpressionException e) {
			throw new Error(e.getLocalizedMessage());
		}
	}

	private String stripSuffix(String string, String suffix) {
		if (!string.endsWith(suffix))
			return string;
		return string.substring(0, string.length() - suffix.length());
	}

	//Called after local plugin files have been processed
	public void buildFullPluginList() {
		try {
			xmlFileReader.getLatestDigestsAndDates(latestDigests, latestDates);

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

				//if latest version installed
				if (digest != null && remoteDigest.equals(digest)) {
					myPlugin = new PluginObject(name, digest, date, PluginObject.STATUS_INSTALLED, true);
				}
				//if new file (Not installed yet)
				else if (digest == null) {
					myPlugin = new PluginObject(name, remoteDigest, remoteDate, PluginObject.STATUS_UNINSTALLED, true);
				}
				//if its installed but can be updated
				else {
					myPlugin = new PluginObject(name, digest, date, PluginObject.STATUS_MAY_UPDATE, true);
					//set latest update details
					String updatedDescription = xmlFileReader.getDescriptionFrom(name, remoteDate);
					List<Dependency> updatedDependencies = xmlFileReader.getDependenciesFrom(name, remoteDate);
					int updatedFilesize = xmlFileReader.getFilesizeFrom(name, remoteDate);
					myPlugin.setUpdateDetails(remoteDigest,
							remoteDate,
							updatedDescription,
							updatedDependencies,
							updatedFilesize);
				}

				String pluginDate = myPlugin.getTimestamp();
				String pluginDigest = myPlugin.getmd5Sum();
				//if md5 sum exists in XML records, then timestamp exists as well
				if (xmlFileReader.existsDigest(name, pluginDigest)) {
					//Use filename and timestamp to get associated description & dependencies
					myPlugin.setDescription(xmlFileReader.getDescriptionFrom(name, pluginDate));
					myPlugin.setDependency(xmlFileReader.getDependenciesFrom(name, pluginDate));
					myPlugin.setFilesize(xmlFileReader.getFilesizeFrom(name, pluginDate));
				} else { //if digest of this plugin does not exist in the records
					//TODO: Placeholder code for calculating filesizes
					//and perhaps dependency (Using DependencyAnalyzer) from file itself
				}
				pluginList.add(myPlugin);
			}

			Iterator<String> iterCurrent = digests.keySet().iterator();
			while (iterCurrent.hasNext()) {
				String name = iterCurrent.next();

				// if it is not a Fiji plugin (Not found in list of up-to-date versions)
				if (!latestDigests.containsKey(name)) {
					String digest = digests.get(name);
					String date = dates.get(name);
					//implies third-party plugin
					//no extra information available (i.e: description & dependencies)
					PluginObject myPlugin = new PluginObject(name, digest, date, PluginObject.STATUS_INSTALLED, false);
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
		} catch (XPathExpressionException e) {
			throw new Error(e.getLocalizedMessage());
		}
	}

	/*private void readUpdateFile(String fileLocation) throws Exception {
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
	}*/

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
