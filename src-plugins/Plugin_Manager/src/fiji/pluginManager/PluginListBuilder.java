package fiji.pluginManager;

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
	public boolean tempDemo = false; //if true, use artificial database...
	private List<PluginObject> pluginList;
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
	}

	public List<PluginObject> extractFullPluginList() {
		return pluginList;
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
		if (!tempDemo) {
			//xmlFileReader = new XMLFileReader(getSaveToLocation(PluginManager.XML_DIRECTORY, PluginManager.XML_FILENAME));
			xmlFileReader = new XMLFileReader("plugininfo" +
					File.separator + "pluginRecords.xml"); //temporary hardcode
		}
		else
			xmlFileReader = new XMLFileReader("plugininfo" +
					File.separator + "pluginRecords.xml"); //temporary hardcode
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
		if (!tempDemo) {

			//Gather filenames of all local plugins
			if (getPlatform().equals("macosx")) {
				queue.add((getUseMacPrefix() ? getMacPrefix() : "") + "fiji-macosx");
				queue.add((getUseMacPrefix() ? getMacPrefix() : "") + "fiji-tiger");
			} else
				queue.add("fiji-" + getPlatform());

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
			currentlyLoaded = 0;
			totalToLoad = queue.size();
			while (iter.hasNext()) {
				taskname = (String)iter.next();
				String outputFilename = taskname;
				String outputDigest;
				String outputDate;

				if (getUseMacPrefix() && outputFilename.startsWith(getMacPrefix()))
					outputFilename = outputFilename.substring(getMacPrefix().length());
				if (File.separator.equals("\\"))
					outputFilename = outputFilename.replace("\\", "/");

			if (!tempDemo) {
				outputDigest = getDigestFromFile(outputFilename);
				digests.put(outputFilename, outputDigest);
			} else {
				//temporary line of code
				outputDigest = digests.get(outputFilename);
			}

				//if XML file does not contain plugin filename or digest does not exist
				if (!xmlFileReader.matchesFilenameAndDigest(outputFilename, outputDigest)) {
					//use the local plugin's last modified timestamp instead
					if (!tempDemo) {
					outputDate = getTimestampFromFile(outputFilename);
					} else {
					outputDate = "20090622999666"; //assume latest... always
					}
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
						(!getPlatform().equals("macosx") ||
								!pluginName.startsWith("fiji-tiger")))
					continue;
			}
			String digest = digests.get(pluginName);
			String remoteDigest = latestDigests.get(pluginName);
			String date = dates.get(pluginName);
			String remoteDate = latestDates.get(pluginName);
			PluginObject myPlugin = null;

			System.out.println(pluginName + ", digest: " + digest + ", timestamp: " + date);

			//if latest version installed
			if (digest != null && remoteDigest.equals(digest)) {
				myPlugin = new PluginObject(pluginName, digest, date, PluginObject.STATUS_INSTALLED, true);
			}
			//if new file (Not installed yet)
			else if (digest == null) {
				myPlugin = new PluginObject(pluginName, remoteDigest, remoteDate, PluginObject.STATUS_UNINSTALLED, true);
			}
			//if its installed but can be updated
			else {
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
				//TODO: Placeholder code for calculating perhaps dependency
				//(Using DependencyAnalyzer) from file itself
				if (!tempDemo) {
					myPlugin.setFilesize(getFilesizeFromFile(myPlugin.getFilename()));
				} else {
					myPlugin.setFilesize(4500);
				}
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
				if (!tempDemo) {
					myPlugin.setFilesize(getFilesizeFromFile(myPlugin.getFilename()));
				} else {
					myPlugin.setFilesize(4500);
				}
				pluginList.add(myPlugin);
			}
		}

		allTasksComplete = true;
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
}