package fiji.logic;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import fiji.data.PluginObject;
import fiji.data.PluginDataReader;

public class Controller {
	private List<PluginObject> pluginList = null;
	private List downloadNameList = null; //tentatively, array of strings from Downloader
	private PluginDataReader pluginDataReader = null;

	public Controller(String updateURL) {
		//Firstly, build a list from local, existing plugins
		pluginDataReader = new PluginDataReader();
		//Get information from server to build on information
		try {
			pluginDataReader.buildFullPluginList(new URL(updateURL));
		} catch (MalformedURLException e) {
			throw new Error(updateURL + " specifies an unknown protocol.");
		}

		pluginList = new ArrayList<PluginObject>();
		downloadNameList = new ArrayList();

		//this is where we extract the data...
		pluginList = pluginDataReader.getExistingPluginList();

	}

	public List<PluginObject> getPluginList() {
		return pluginList;
	}

	public List getDownloadNameList() {
		return downloadNameList;
	}
}

