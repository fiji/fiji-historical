package fiji.pluginManager;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;

import org.xml.sax.SAXException;

public class Uploader {
	private List<PluginObject> uploadList;
	private DependencyAnalyzer dependencyAnalyzer;
	private XMLFileReader xmlFileReader;

	public Uploader(List<PluginObject> pluginList) throws ParserConfigurationException, IOException, SAXException {
		System.out.println("Uploader CLASS: Started up");
		PluginCollection pluginCollection = (PluginCollection)pluginList;
		this.uploadList = pluginCollection.getList(PluginCollection.FILTER_ACTIONS_UPLOAD);
		dependencyAnalyzer = new DependencyAnalyzer(pluginCollection);
		//xmlFileReader = new XMLFileReader(getSaveToLocation(PluginManager.XML_DIRECTORY, PluginManager.XML_FILENAME));
		xmlFileReader = new XMLFileReader("plugininfo" +
				File.separator + "pluginRecords.xml"); //temporary hardcode
	}

	public void generateDocuments() throws IOException, TransformerConfigurationException, SAXException {
		//Generate dependencies using DependencyAnalyzer
		//Build new version of XML document (and/or current.txt)
		System.out.println("Uploader CLASS: At generateDocuments()");
		System.out.println("Uploader CLASS: At generateDocuments(), asked dependencyAnalyzer to calculate dependencies for plugins.");
		//or... dependencyAnalyzer.generateDependencies(uploadList)?

		for (PluginObject plugin : uploadList) {
			boolean calcDependencies = false;
			if (plugin.isFijiPlugin()) {
				if (plugin.isRemovableOnly() || plugin.isInstallable()) {
					calcDependencies = false;
				} else if (plugin.isUpdateable()) {
					//update-able being defined having different digest from latest records
					if (!xmlFileReader.matchesFilenameAndDigest(plugin.getFilename(), plugin.getmd5Sum())) {
						calcDependencies = true; //does not exist in records
					} else {
						calcDependencies = false;
					}
				}
			} else {
				calcDependencies = true; //not fiji plugin ==> does not exist in records
			}

			//only calculate dependencies if digest does not exist in records
			if (calcDependencies) {
				/*System.out.println("========Results of dependencyAnalyzer of " + plugin.getFilename() + "========");
				for (int i = 0; i < dependencies.size(); i++) {
					Dependency dependency = dependencies.get(i);
					System.out.println((i+1) + ".) " + dependency.getFilename() + ", " + dependency.getTimestamp());
				}
				System.out.println("========End of results of " + plugin.getFilename() + "========");*/
				plugin.setDependency(dependencyAnalyzer.getDependencyListFromFile(plugin.getFilename()));
			}
		}

		//Checking list for Fiji plugins - Either new versions or changes to existing ones
		Map<String, List<PluginObject>> newPluginRecords = xmlFileReader.getXMLRecords();
		Iterator<String> pluginNamelist = newPluginRecords.keySet().iterator();
		while (pluginNamelist.hasNext()) {
			String name = pluginNamelist.next();
			List<PluginObject> versionList = newPluginRecords.get(name);
			for (PluginObject pluginToUpload : uploadList) {
				if (pluginToUpload.getFilename().equals(name)) {
					PluginObject version = getPluginMatchingDigest(pluginToUpload.getmd5Sum(), versionList);
					if (version != null) {
						//update the existing version
						version.setDescription(pluginToUpload.getDescription());
					} else {
						//this version does not appear in existing records, therefore add it
						versionList.add(pluginToUpload);
					}
					break;
				}
			}
		}

		//Checking list for non-Fiji plugins to add to new records
		for (PluginObject pluginToUpload : uploadList) {
			String name = pluginToUpload.getFilename();
			List<PluginObject> versionList = newPluginRecords.get(name);
			if (versionList == null) { //non-Fiji plugin, therefore add it
				versionList = new PluginCollection();
				versionList.add(pluginToUpload);
				newPluginRecords.put(name, versionList);
			}
		}

		//UpdatesWriter updatesWriter = new UpdatesWriter(getSaveToLocation(PluginManager.XML_DIRECTORY, PluginManager.XML_FILENAME), null);
		UpdatesWriter updatesWriter = new UpdatesWriter();
		updatesWriter.writeXMLFile(newPluginRecords);
	}

	private PluginObject getPluginMatchingDigest(String digest, List<PluginObject> pluginList) {
		for (PluginObject plugin : pluginList) {
			if (digest.equals(plugin.getmd5Sum())) {
				return plugin;
			}
		}
		return null;
	}

	public void uploadToServer() {
		//upload plugins in uploadList to server
		//upload XML document (and/or current.txt) to server
		System.out.println("Uploader CLASS: At uploadToServer()");
	}
}
