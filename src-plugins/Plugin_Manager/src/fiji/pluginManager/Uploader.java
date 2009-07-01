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
	private Map<String, List<PluginObject>> newPluginRecords;

	public Uploader(List<PluginObject> pluginList) throws ParserConfigurationException, IOException, SAXException {
		System.out.println("Uploader CLASS: Started up");
		PluginCollection pluginCollection = (PluginCollection)pluginList;
		this.uploadList = pluginCollection.getList(PluginCollection.FILTER_ACTIONS_UPLOAD);
		dependencyAnalyzer = new DependencyAnalyzer(pluginCollection);
		//xmlFileReader = new XMLFileReader(getSaveToLocation(PluginManager.XML_DIRECTORY, PluginManager.XML_FILENAME));
		xmlFileReader = new XMLFileReader(PluginManager.XML_DIRECTORY +
				File.separator + PluginManager.XML_FILENAME);
	}

	public Iterator<PluginObject> iteratorUploads() {
		return uploadList.iterator();
	}

	public void generateNewPluginRecords() throws IOException {
		//Checking list for Fiji plugins - Either new versions or changes to existing ones
		newPluginRecords = xmlFileReader.getXMLRecords();
		Iterator<String> pluginNamelist = newPluginRecords.keySet().iterator();
		while (pluginNamelist.hasNext()) {
			String name = pluginNamelist.next();
			List<PluginObject> versionList = newPluginRecords.get(name);
			for (PluginObject pluginToUpload : uploadList) {
				if (pluginToUpload.getFilename().equals(name)) {
					PluginObject version = getPluginMatchingDigest(pluginToUpload.getmd5Sum(), versionList);
					if (version != null) {
						//edit the existing version's details, but no new version uploaded
						version.setDescription(pluginToUpload.getDescription());
					} else {
						//this version does not appear in existing records, therefore add it
						pluginToUpload.setDependency(dependencyAnalyzer.getDependencyListFromFile(pluginToUpload.getFilename()));
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
				pluginToUpload.setDependency(dependencyAnalyzer.getDependencyListFromFile(pluginToUpload.getFilename()));
				versionList = new PluginCollection();
				versionList.add(pluginToUpload);
				newPluginRecords.put(name, versionList);
			}
		}
	}

	private PluginObject getPluginMatchingDigest(String digest, List<PluginObject> pluginList) {
		for (PluginObject plugin : pluginList) {
			if (digest.equals(plugin.getmd5Sum())) {
				return plugin;
			}
		}
		return null;
	}

	public void uploadToServer() throws IOException, TransformerConfigurationException, SAXException {
		//upload XML document (and/or current.txt) to server
		System.out.println("Uploader CLASS: At uploadToServer()");
		UpdatesWriter updatesWriter = new UpdatesWriter();
		updatesWriter.uploadFilesToServer(newPluginRecords);
	}

}
