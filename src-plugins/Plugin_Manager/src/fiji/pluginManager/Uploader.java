package fiji.pluginManager;

import ij.IJ;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import org.xml.sax.SAXException;

public class Uploader implements Observer {
	private UpdatesWriter updatesWriter;
	private List<PluginObject> uploadChangesList;
	private DependencyAnalyzer dependencyAnalyzer;
	private XMLFileReader xmlFileReader;
	private Map<String, List<PluginObject>> newPluginRecords;
	private List<PluginObject> filesUploadList; //list of plugins whose files has to be uploaded

	public Uploader(List<PluginObject> pluginList) throws ParserConfigurationException, IOException, SAXException {
		System.out.println("Uploader CLASS: Started up");
		PluginCollection pluginCollection = (PluginCollection)pluginList;
		uploadChangesList = pluginCollection.getList(PluginCollection.FILTER_ACTIONS_UPLOAD);
		dependencyAnalyzer = new DependencyAnalyzer(pluginCollection);
		xmlFileReader = new XMLFileReader(PluginManager.XML_DIRECTORY +
				File.separator + PluginManager.XML_FILENAME); //generates XML records from file
	}

	public void generateNewPluginRecords() throws IOException {
		//Checking list for Fiji plugins - Either new versions or changes to existing ones
		filesUploadList = new PluginCollection();
		newPluginRecords = xmlFileReader.getXMLRecords();
		Iterator<String> pluginNamelist = newPluginRecords.keySet().iterator();
		while (pluginNamelist.hasNext()) {
			String name = pluginNamelist.next();
			List<PluginObject> versionList = newPluginRecords.get(name);
			for (PluginObject pluginToUpload : uploadChangesList) {
				if (pluginToUpload.getFilename().equals(name)) {
					PluginObject version = getPluginMatchingDigest(pluginToUpload.getmd5Sum(), versionList);
					if (version != null) {
						//edit the existing version's details, but no new file uploaded
						version.setDescription(pluginToUpload.getDescription());
					} else {
						//this version does not appear in existing records, therefore add it
						addPluginToVersionList(pluginToUpload, versionList);
					}
					break;
				}
			}
		}

		//Checking list for non-Fiji plugins to add to new records
		for (PluginObject pluginToUpload : uploadChangesList) {
			String name = pluginToUpload.getFilename();
			List<PluginObject> versionList = newPluginRecords.get(name);
			if (versionList == null) { //non-Fiji plugin, therefore add it
				versionList = new PluginCollection();
				addPluginToVersionList(pluginToUpload, versionList);
				newPluginRecords.put(name, versionList);
			}
		}
	}

	private void addPluginToVersionList(PluginObject pluginToUpload, List<PluginObject> versionList) throws IOException {
		pluginToUpload.setDependency(dependencyAnalyzer.getDependencyListFromFile(pluginToUpload.getFilename()));
		versionList.add(pluginToUpload);
		filesUploadList.add(pluginToUpload); //indicates plugin file itself has to be uploaded
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
		updatesWriter = new UpdatesWriter(this);
		updatesWriter.uploadFilesToServer(newPluginRecords, filesUploadList);
	}

	public List<PluginObject> getSuccessfulUploads() {
		return updatesWriter.successList;
	}

	public List<PluginObject> getFailedUploads() {
		return updatesWriter.failList;
	}

	public void resetActionsOfChangeList() {
		for (PluginObject plugin : uploadChangesList)
			plugin.setActionNone();
	}

	//Updating the interface
	public void refreshData(Observable subject) {
		if (subject == updatesWriter) {
			if (updatesWriter.allTasksComplete()) {
				IJ.showStatus("");
			} else {
				//continue displaying statuses
				IJ.showStatus("Uploading " + updatesWriter.getTaskname() + "...");
				IJ.showProgress(updatesWriter.getCurrentlyLoaded(), updatesWriter.getTotalToLoad());
			}
		}
	}

}
