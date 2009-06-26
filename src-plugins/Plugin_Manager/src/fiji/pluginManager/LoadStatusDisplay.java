package fiji.pluginManager;

import java.util.List;
import ij.IJ;

public class LoadStatusDisplay implements Observer {
	private XMLFileDownloader xmlFileDownloader;
	private PluginListBuilder pluginListBuilder;
	private String fileURL = "http://pacific.mpi-cbg.de/update/current.txt";//should be XML file actually
	private String saveFile = "current.txt";//should be XML file actually

	public LoadStatusDisplay() {
		IJ.showStatus("Starting up Plugin Manager");
		xmlFileDownloader = new XMLFileDownloader(this);
		xmlFileDownloader.startDownload(fileURL, saveFile);
	}

	public void refreshData(Observable subject) {
		if (subject == xmlFileDownloader) {
			IJ.showStatus("Downloading " + xmlFileDownloader.getTaskname() + "...");
			IJ.showProgress(xmlFileDownloader.getCurrentlyLoaded(), xmlFileDownloader.getTotalToLoad());
			if (xmlFileDownloader.allTasksComplete()) {
				pluginListBuilder = new PluginListBuilder(this);
				pluginListBuilder.buildFullPluginList(xmlFileDownloader.getSaveFileLocation());
			}
		} else if (subject == pluginListBuilder) {
			IJ.showStatus("Downloading " + pluginListBuilder.getTaskname() + "...");
			IJ.showProgress(pluginListBuilder.getCurrentlyLoaded(), pluginListBuilder.getTotalToLoad());
			if (pluginListBuilder.allTasksComplete()) {
				IJ.showStatus("");
			}
		}
	}

	public List<PluginObject> getExistingPluginList() {
		System.out.println("mysize " + pluginListBuilder.extractFullPluginList().size());
		return pluginListBuilder.extractFullPluginList();
	}
}