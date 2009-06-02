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
	private List<PluginObject> pluginList; //current states of all plugins

	public Controller(List<PluginObject> pluginList) {
		this.pluginList = pluginList;
	}

	//comes up with a list of plugins needed for download to go with this selected plugin
	public void addDependency(List<PluginObject> changeToInstallList, List<PluginObject> changeToUpdateList, PluginObject selectedPlugin) {
		//if there are no dependencies for this selected plugin
		if (selectedPlugin.getDependencies() == null || selectedPlugin.getDependencies().size() == 0) {
			return;
		} else {
			List<Dependency> dependencyList = selectedPlugin.getDependencies();
			for (int i = 0; i < dependencyList.size(); i++) {

				Dependency dependency = dependencyList.get(i);
				for (int j = 0; j < pluginList.size(); j++) {
					PluginObject plugin = pluginList.get(j);

					//if Prerequisite is found,
					if (plugin.getFilename().equals(dependency.getFilename())) {
						//Which does not exist in any of the "change" lists yet
						if (!changeToInstallList.contains(plugin) &&
							!changeToUpdateList.contains(plugin)) {

							//if prerequisite already installed, but asked to be uninstalled
							if (plugin.getStatus() == PluginObject.STATUS_INSTALLED &&
									plugin.getAction() == PluginObject.ACTION_REVERSE) {
								//add to list
								changeToInstallList.add(plugin);
								addDependency(changeToInstallList, changeToUpdateList, plugin);
							}
							//if prerequisite not installed, and asked to have no action taken
							else if (plugin.getStatus() == PluginObject.STATUS_UNINSTALLED &&
									plugin.getAction() == PluginObject.ACTION_NONE) {
								//add to list
								changeToInstallList.add(plugin);
								addDependency(changeToInstallList, changeToUpdateList, plugin);
							}
							//if prerequisite is update-able, but not asked to update to latest
							else if (plugin.getStatus() == PluginObject.STATUS_MAY_UPDATE &&
									plugin.getAction() != PluginObject.ACTION_UPDATE) {
								//if current dependency's plugin is outdated
								if (plugin.getTimestamp().compareTo(dependency.getTimestamp()) < 0) {
									changeToUpdateList.add(plugin); //add to update list ("special" case)
									addDependency(changeToInstallList, changeToUpdateList, plugin);
								} else { //if current installed dependency is not outdated
									if (plugin.getAction() == PluginObject.ACTION_REVERSE) {
										changeToInstallList.add(plugin);
										addDependency(changeToInstallList, changeToUpdateList, plugin);
									} //otherwise, leave it be (It is enough unless user specifies otherwise)
								}
							}
						} else { //if prerequisite does appear in the "change" lists
							//nothing, assume its dependencies were added already as well
						}
						break;
					}
				} //end of searching through pluginList

			}
		}
	}

	public void removeDependent(List<PluginObject> changeToUninstallList, PluginObject selectedPlugin) {
		
	}

	public void setToInstall(List<PluginObject> selectedList) {
		for (int i = 0; i < selectedList.size(); i++) {
			PluginObject plugin = selectedList.get(i);
			if (plugin.getStatus() == PluginObject.STATUS_INSTALLED ||
				plugin.getStatus() == PluginObject.STATUS_MAY_UPDATE) {
				plugin.setAction(PluginObject.ACTION_NONE);
			} else if (plugin.getStatus() == PluginObject.STATUS_UNINSTALLED) {
				plugin.setAction(PluginObject.ACTION_REVERSE);
			}
		}
	}

	public void setToUpdate(List<PluginObject> selectedList) {
		for (int i = 0; i < selectedList.size(); i++) {
			PluginObject plugin = selectedList.get(i);
			if (plugin.getStatus() == PluginObject.STATUS_INSTALLED) {
				plugin.setAction(PluginObject.ACTION_NONE);
			} else if (plugin.getStatus() == PluginObject.STATUS_UNINSTALLED) {
				plugin.setAction(PluginObject.ACTION_REVERSE);
			} else if (plugin.getStatus() == PluginObject.STATUS_MAY_UPDATE) {
				plugin.setAction(PluginObject.ACTION_UPDATE);
			}
		}
	}

	public void setToRemove(List<PluginObject> selectedList) {
		for (int i = 0; i < selectedList.size(); i++) {
			PluginObject plugin = selectedList.get(i);
			if (plugin.getStatus() == PluginObject.STATUS_INSTALLED) {
				plugin.setAction(PluginObject.ACTION_REVERSE);
			} else if (plugin.getStatus() == PluginObject.STATUS_UNINSTALLED) {
				plugin.setAction(PluginObject.ACTION_NONE);
			} else if (plugin.getStatus() == PluginObject.STATUS_MAY_UPDATE) {
				plugin.setAction(PluginObject.ACTION_REVERSE);
			}
		}
	}
}

