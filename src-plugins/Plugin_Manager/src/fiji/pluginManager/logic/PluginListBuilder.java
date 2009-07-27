package fiji.pluginManager.logic;
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
 * 1st step: Get information of local plugins (Md5 sums and version)
 * 2nd step: Given XML file, get information of latest Fiji plugins (Md5 sums and version)
 * 3rd step: Build up list of "PluginObject" using both local and updates
 * 
 * digests and dates hold Md5 sums and versions of local plugins respectively
 * latestDigests and latestDates hold Md5 sums and versions of latest Fiji plugins
 */
public class PluginListBuilder extends PluginDataObservable {
	private final String[] pluginDirectories = {"plugins", "jars", "retro", "misc"};
	public List<PluginObject> pluginCollection; //info available after list is built
	public List<PluginObject> readOnlyList; //info available after list is built
	private Map<String, String> digests;
	private Map<String, String> dates;
	private Map<String, String> latestDates;
	private Map<String, String> latestDigests;
	private XMLFileReader xmlFileReader;

	public PluginListBuilder(XMLFileReader xmlFileReader) {
		if (xmlFileReader == null) throw new Error("XMLFileReader object is null");
		this.xmlFileReader = xmlFileReader;

		//initialize storage
		dates = new TreeMap<String, String>();
		digests = new TreeMap<String, String>();
		latestDates = new TreeMap<String, String>();
		latestDigests = new TreeMap<String, String>();
		pluginCollection = new PluginCollection();
		readOnlyList = new PluginCollection();
	}

	public void buildFullPluginList() throws ParserConfigurationException, IOException, SAXException {
		//Generates information of plugins on local side (digests, dates)
		buildLocalPluginData();
		//Generates information of latest plugins on remote side
		xmlFileReader.getLatestDigestsAndDates(latestDigests, latestDates);
		//Builds up a list of PluginObjects, of both local and remote
		generatePluginList();
	}

	//To get data of plugins on the local side
	private void buildLocalPluginData() throws ParserConfigurationException, SAXException, IOException {
		List<String> queue = generatePluginNamelist();

		//To calculate the Md5 sums on the local side
		Iterator<String> iter = queue.iterator();
		currentlyLoaded = 0;
		totalToLoad = queue.size();
		while (iter.hasNext()) {
			String outputFilename = initializeFilename(iter.next());
			String outputDigest = getDigestFromFile(outputFilename); //TODO: forServer flag
			digests.put(outputFilename, outputDigest);

			//null indicate XML records does not have such plugin filename and md5 sums
			String outputDate = xmlFileReader.getTimestampFromRecords(outputFilename,
					outputDigest);
			if (outputDate == null) {
				//use local plugin's last modified timestamp instead
				outputDate = getTimestampFromFile(outputFilename);
			}
			dates.put(outputFilename, outputDate);

			changeStatus(outputFilename, ++currentlyLoaded, totalToLoad);
		}
	}

	private List<String> generatePluginNamelist() {
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

		for (String directory : pluginDirectories) {
			File dir = new File(prefix(directory));
			if (!dir.isDirectory())
				throw new Error("Plugin Directory " + directory + " does not exist!");
			else
				queueDirectory(queue, directory);
		}

		return queue;
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
				myPlugin.setUpdateDetails(remoteDigest, remoteDate);
			}
			//Plugin shall only contains the latest version's details
			myPlugin.setPluginDetails(xmlFileReader.getPluginDetailsFrom(pluginName));
			myPlugin.setDependency(xmlFileReader.getDependenciesFrom(pluginName));
			myPlugin.setFilesize(xmlFileReader.getFilesizeFrom(pluginName));

			pluginCollection.add(myPlugin);
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
				pluginCollection.add(myPlugin);
			}
		}

		for (PluginObject plugin : pluginCollection) {
			File file = new File(prefix(plugin.getFilename()));
			if (!file.exists() || file.canWrite())
				continue;
			readOnlyList.add(plugin);
			IJ.log(plugin.getFilename() + " is read-only file.");
		} //Still remains in pluginCollection for dependency reference purposes

		setStatusComplete(); //indicate to observer there's no more tasks
	}
}