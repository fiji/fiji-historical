package fiji.pluginManager;

import ij.IJ;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/*
 * PluginListBuilder's overall role is to be in charge of building of a plugin list
 * for interface usage.
 * 
 * 1st step: Parses an XML file, thus readying it for local/remote information retrieval
 * 2nd step: Get information of local plugins (Md5 sums and version)
 * 3rd step: Get information of latest Fiji plugins (Md5 sums and version)
 * 4th step: Build up list of "PluginObject" using both local and updates
 * 
 */
public class PluginListBuilder extends PluginDataObservable {
	private List<PluginObject> pluginList;
	private List<PluginObject> readOnlyList;
	private Map<String, String> digests;
	private Map<String, String> dates;
	private Map<String, String> latestDates;
	private Map<String, String> latestDigests;
	private XMLFileReader xmlFileReader;

	public PluginListBuilder(Observer observer) {
		super(observer);
		//initialize storage
		dates = new TreeMap<String, String>();
		digests = new TreeMap<String, String>();
		latestDates = new TreeMap<String, String>();
		latestDigests = new TreeMap<String, String>();
		pluginList = new PluginCollection();
		readOnlyList = new PluginCollection();
	}

	public List<PluginObject> extractFullPluginList() {
		return pluginList;
	}

	public List<PluginObject> getReadOnlyPlugins() {
		return readOnlyList; //for error notification purposes
	}

	//recursively looks into a directory and adds the relevant file
	private void queueDirectory(List<String> queue, String path) {
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

	public void buildFullPluginList() throws ParserConfigurationException, IOException, SAXException {
		//Parses XML document; contents is needed for both local and remote plugins
		xmlFileReader = new XMLFileReader(PluginManager.XML_DIRECTORY + File.separator + PluginManager.XML_FILENAME);
		//Generates information of plugins on local side
		buildLocalPluginList();
		//Generates information of plugins on remote side
		xmlFileReader.getLatestDigestsAndDates(latestDigests, latestDates);
		//Builds up a list of PluginObjects, of both local and remote
		generatePluginList();
	}

	private void buildLocalPluginList() throws ParserConfigurationException, SAXException, IOException {
		//To get a list of plugins on the local side
		List<String> queue = new ArrayList<String>();

		//Gather filenames of all local plugins
		//TODO (Done?): Check if these launchers exist before adding them
		if (getPlatform().equals("macosx")) {
			String macosx = (getUseMacPrefix() ? getMacPrefix() : "") + "fiji-macosx";
			String tiger = (getUseMacPrefix() ? getMacPrefix() : "") + "fiji-tiger";
			if (fileExists(macosx)) queue.add(macosx);
			if (fileExists(tiger)) queue.add(tiger);
		} else {
			String fijiapp = "fiji-" + getPlatform();
			if (fileExists(fijiapp)) queue.add("fiji-" + getPlatform());
		}
		if (fileExists("ij.jar")) queue.add("ij.jar");

		//Directories assumed to exist
		queueDirectory(queue, "plugins");
		queueDirectory(queue, "jars");
		queueDirectory(queue, "retro");
		queueDirectory(queue, "misc");

		//To calculate the Md5 sums on the local side
		Iterator<String> iter = queue.iterator();
		currentlyLoaded = 0;
		totalToLoad = queue.size();
		while (iter.hasNext()) {
			String outputFilename = taskname = initializeFilename(iter.next());
			String outputDigest = getDigestFromFile(outputFilename); //TODO: forServer flag
			String outputDate;
			digests.put(outputFilename, outputDigest);

			//if XML file does not contain plugin filename or digest does not exist
			if (!xmlFileReader.matchesFilenameAndDigest(outputFilename, outputDigest)) {
				//use the local plugin's last modified timestamp instead
				outputDate = getTimestampFromFile(outputFilename);
			} else {
				//if it does exist, then use the associated timestamp as recorded
				outputDate = xmlFileReader.getTimestamp(outputFilename, outputDigest);
			}
			dates.put(outputFilename, outputDate);

			++currentlyLoaded;
			notifyObservers();
		}
	}

	private void generatePluginList() {
		//Converts data gathered into lists of PluginObject, ready for UI classes usage
		Iterator<String> iterLatest = latestDigests.keySet().iterator();
		while (iterLatest.hasNext()) {
			String pluginName = iterLatest.next();
			// launcher is platform-specific
			if (pluginName.startsWith("fiji-")) {
				if (!pluginName.equals("fiji-" + getPlatform()) &&
						(!getPlatform().equals("macosx") || !pluginName.startsWith("fiji-tiger"))) {
					continue;
				}
			}
			String digest = digests.get(pluginName);
			String remoteDigest = latestDigests.get(pluginName);
			String date = dates.get(pluginName);
			String remoteDate = latestDates.get(pluginName);
			PluginObject myPlugin = null;

			if (digest != null && remoteDigest.equals(digest)) { //if latest version installed
				myPlugin = new PluginObject(pluginName, digest, date, PluginObject.STATUS_INSTALLED, true);
			} else if (digest == null) { //if new file (Not installed yet)
				myPlugin = new PluginObject(pluginName, remoteDigest, remoteDate, PluginObject.STATUS_UNINSTALLED, true);
			} else { //if its installed but can be updated
				myPlugin = new PluginObject(pluginName, digest, date, PluginObject.STATUS_MAY_UPDATE, true);
				//set latest update details
				String updatedDescription = xmlFileReader.getDescriptionFrom(pluginName, remoteDate);
				List<Dependency> updatedDependencies = xmlFileReader.getDependenciesFrom(pluginName, remoteDate);
				int updatedFilesize = xmlFileReader.getFilesizeFrom(pluginName, remoteDate);
				myPlugin.setUpdateDetails(remoteDigest,
						remoteDate,
						updatedDescription,
						updatedDependencies,
						updatedFilesize);
			}

			String pluginDate = myPlugin.getTimestamp();
			String pluginDigest = myPlugin.getmd5Sum();
			//if md5 sum exists in XML records, then timestamp exists as well
			if (xmlFileReader.matchesFilenameAndDigest(pluginName, pluginDigest)) {
				//Use filename and timestamp to get associated description & dependencies
				myPlugin.setDescription(xmlFileReader.getDescriptionFrom(pluginName, pluginDate));
				myPlugin.setDependency(xmlFileReader.getDependenciesFrom(pluginName, pluginDate));
				myPlugin.setFilesize(xmlFileReader.getFilesizeFrom(pluginName, pluginDate));
			} else { //if digest of this plugin does not exist in the records
				myPlugin.setFilesize(getFilesizeFromFile(myPlugin.getFilename()));
			}
			pluginList.add(myPlugin);
		}

		//To capture non-Fiji plugins
		Iterator<String> iterCurrent = digests.keySet().iterator();
		while (iterCurrent.hasNext()) {
			String name = iterCurrent.next();
			//If it is not a Fiji plugin (Not found in list of up-to-date versions)
			if (!latestDigests.containsKey(name)) {
				String digest = digests.get(name);
				String date = dates.get(name);
				//implies third-party plugin, no description nor dependency information available
				PluginObject myPlugin = new PluginObject(name, digest, date, PluginObject.STATUS_INSTALLED, false);
				myPlugin.setFilesize(getFilesizeFromFile(myPlugin.getFilename()));
				pluginList.add(myPlugin);
			}
		}

		for (PluginObject plugin : pluginList) {
			File file = new File(plugin.getFilename());
			if (!file.exists() || file.canWrite())
				continue;
			readOnlyList.add(plugin);
			IJ.log(plugin.getFilename() + " is read-only file.");
		}
		//for (PluginObject plugin : readOnlyList) pluginList.remove(plugin); //cannot view

		allTasksComplete = true;
		notifyObservers();
	}
}