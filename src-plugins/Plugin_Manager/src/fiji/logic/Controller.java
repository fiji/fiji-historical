package fiji.logic;
import java.util.ArrayList;
import java.util.List;

import fiji.data.PluginObject;
import fiji.data.PluginDataReader;

public class Controller {
	private List<PluginObject> updatesPluginList = null;
	private List<PluginObject> existingPluginList = null;
	private List downloadNameList = null; //tentatively, array of strings from Downloader
	private PluginDataReader pluginDataReader = null;

	public Controller() {
		pluginDataReader = new PluginDataReader();
		updatesPluginList = new ArrayList<PluginObject>();
		existingPluginList = new ArrayList<PluginObject>();
		downloadNameList = new ArrayList();

		//this is where we extract the data...
		existingPluginList = pluginDataReader.getExistingPluginList();

	}

	public List<PluginObject> getUpdatesPluginList() {
		return updatesPluginList;
	}

	public List<PluginObject> getExistingPluginList() {
		return existingPluginList;
	}

	public List getDownloadNameList() {
		return downloadNameList;
	}

	public void generateUpdatesPluginList() {
		pluginDataReader.generateUpdatesPluginList();
	}
}

