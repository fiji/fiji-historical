package fiji.pluginManager;

import java.util.List;

public class Uploader {
	private List<PluginObject> uploadList;
	private DependencyAnalyzer dependencyAnalyzer;

	public Uploader(PluginListBuilder pluginListBuilder) {
		System.out.println("Uploader CLASS: Started up");
		PluginCollection pluginList = (PluginCollection)pluginListBuilder.getExistingPluginList();
		this.uploadList = pluginList.getList(PluginCollection.FILTER_ACTIONS_UPLOAD);
		dependencyAnalyzer = new DependencyAnalyzer(pluginListBuilder.getPluginDataProcessor());
	}

	public void generateDocuments() {
		//Generate dependencies using DependencyAnalyzer
		//Build new version of XML document (and/or current.txt)
		System.out.println("Uploader CLASS: At generateDocuments()");
		System.out.println("Uploader CLASS: At generateDocuments(), asked dependencyAnalyzer to calculate dependencies for plugins.");
		//or... dependencyAnalyzer.generateDependencies(uploadList)?
		for (PluginObject plugin : uploadList) {
			dependencyAnalyzer.generateDependencies(plugin);
		}
	}

	public void uploadToServer() {
		//upload plugins in uploadList to server
		//upload XML document (and/or current.txt) to server
		System.out.println("Uploader CLASS: At uploadToServer()");
	}
}
