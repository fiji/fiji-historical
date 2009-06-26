package fiji.pluginManager;

import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import ij.IJ;

public class LoadStatusDisplay implements Observer {
	private XMLFileDownloader xmlFileDownloader;
	private PluginListBuilder pluginListBuilder;

	public LoadStatusDisplay() {
		IJ.showStatus("Starting up Plugin Manager");
		xmlFileDownloader = new XMLFileDownloader(this);
		xmlFileDownloader.startDownload();
	}

	public void refreshData(Observable subject) {
		try {
			if (subject == xmlFileDownloader) {
				IJ.showStatus("Downloading " + xmlFileDownloader.getTaskname() + "...");
				IJ.showProgress(xmlFileDownloader.getCurrentlyLoaded(), xmlFileDownloader.getTotalToLoad());
				if (xmlFileDownloader.allTasksComplete()) {
					pluginListBuilder = new PluginListBuilder(this);
					pluginListBuilder.buildFullPluginList();
				}
			} else if (subject == pluginListBuilder) {
				IJ.showStatus("Downloading " + pluginListBuilder.getTaskname() + "...");
				IJ.showProgress(pluginListBuilder.getCurrentlyLoaded(), pluginListBuilder.getTotalToLoad());
				if (pluginListBuilder.allTasksComplete()) {
					IJ.showStatus("");
				}
			}
		} catch (ParserConfigurationException e1) {
			throw new Error(e1.getLocalizedMessage());
		} catch (IOException e2) {
			throw new Error(e2.getLocalizedMessage());
		} catch (SAXException e3) {
			throw new Error(e3.getLocalizedMessage());
		}
	}

	public List<PluginObject> getExistingPluginList() {
		System.out.println("mysize " + pluginListBuilder.extractFullPluginList().size());
		return pluginListBuilder.extractFullPluginList();
	}
}