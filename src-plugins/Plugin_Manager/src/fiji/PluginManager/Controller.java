package fiji.PluginManager;
import ij.Menus;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/*
 * Determine the dependencies of the plugin.
 */
public class Controller {
	private List<PluginObject> pluginList;

	public Controller(List<PluginObject> pluginList) {
		this.pluginList = pluginList;
	}

}

