package fiji.logic;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import fiji.data.PluginCollection;
import fiji.data.PluginObject;
import fiji.data.PluginDataReader;

public class Controller {
	private String updateURL = null;
	private List<PluginObject> pluginList = null;
	private PluginDataReader pluginDataReader = null;
	private Installer installer = null;

	public Controller(String updateURL) {
		this.updateURL = updateURL;

		//Firstly, get information from local, existing plugins
		pluginDataReader = new PluginDataReader();
		//Get information from server to build on information
		try {
			pluginDataReader.buildFullPluginList(new URL(updateURL));
		} catch (MalformedURLException e) {
			throw new Error(updateURL + " specifies an unknown protocol.");
		}

		pluginList = new ArrayList<PluginObject>();

		//this is where we extract the data...
		pluginList = pluginDataReader.getExistingPluginList();

	}

	public List<PluginObject> getPluginList() {
		return pluginList;
	}

	public void createInstaller() {
		installer = new Installer(((PluginCollection)pluginList).getListWhereActionIsSpecified(), updateURL);
	}

	public Installer getInstaller() {
		return installer;
	}
}

