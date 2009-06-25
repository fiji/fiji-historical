package fiji.pluginManager;

import java.io.IOException;
import java.util.List;

public class Uploader {
	private List<PluginObject> uploadList;
	private DependencyAnalyzer dependencyAnalyzer;

	public Uploader(List<PluginObject> pluginList) {
		System.out.println("Uploader CLASS: Started up");
		PluginCollection pluginCollection = (PluginCollection)pluginList;
		this.uploadList = pluginCollection.getList(PluginCollection.FILTER_ACTIONS_UPLOAD);
		dependencyAnalyzer = new DependencyAnalyzer();
	}

	public void generateDocuments() throws IOException {
		//Generate dependencies using DependencyAnalyzer
		//Build new version of XML document (and/or current.txt)
		System.out.println("Uploader CLASS: At generateDocuments()");
		System.out.println("Uploader CLASS: At generateDocuments(), asked dependencyAnalyzer to calculate dependencies for plugins.");
		//or... dependencyAnalyzer.generateDependencies(uploadList)?
		for (PluginObject plugin : uploadList) {
			List<Dependency> dependencies = dependencyAnalyzer.getDependencyListFromFile(plugin.getFilename());
			System.out.println("========Results of dependencyAnalyzer of " + plugin.getFilename() + "========");
			for (int i = 0; i < dependencies.size(); i++) {
				Dependency dependency = dependencies.get(i);
				System.out.println((i+1) + ".) " + dependency.getFilename() + ", " + dependency.getTimestamp());
			}
			System.out.println("========End of results of " + plugin.getFilename() + "========");
		}
	}

	public void uploadToServer() {
		//upload plugins in uploadList to server
		//upload XML document (and/or current.txt) to server
		System.out.println("Uploader CLASS: At uploadToServer()");
	}
}
