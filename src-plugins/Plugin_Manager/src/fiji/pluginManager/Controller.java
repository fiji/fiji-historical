package fiji.pluginManager;
import ij.Menus;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/*
 * Determine the dependencies of the plugin through ADD and REMOVE scenarios.
 * The dependencies are determined based on the assumption that the user has already
 * selected the plugins he/she wanted to add or remove and indicated to take action.
 */
public class Controller {
	private List<PluginObject> pluginList; //current states of all plugins

	public Controller(List<PluginObject> pluginList) {
		this.pluginList = pluginList;
	}

	//comes up with a list of plugins needed for download to go with this selected plugin
	public void addDependency(List<PluginObject> changeToInstallList, List<PluginObject> changeToUpdateList, PluginObject selectedPlugin) {
		//First retrieve the dependency list
		List<Dependency> dependencyList = new ArrayList<Dependency>();
		boolean updateableState = (selectedPlugin.getStatus() == PluginObject.STATUS_MAY_UPDATE);
		boolean selectedInInstallList = changeToInstallList.contains(selectedPlugin);
		boolean selectedInUpdateList = changeToUpdateList.contains(selectedPlugin);

		//Does not belong in any lists yet
		if (!selectedInInstallList && !selectedInUpdateList) {
			if (updateableState) {
				//in context of "addDependency", it can only mean "update the plugin"
				changeToUpdateList.add(selectedPlugin);
			} else {
				//in context of "addDependency", it can only mean "install the plugin"
				changeToInstallList.add(selectedPlugin);
			}
		} //else... (Plugin already added earlier)

		//if "Update-able" state
		if (updateableState) {
			//if in updateList, use update's dependencies, if not, go on as per normal
			if (!selectedInInstallList && selectedInUpdateList) {
				dependencyList = selectedPlugin.getNewDependencies();
			} else if (selectedInInstallList && !selectedInUpdateList) {
				//if already in the "Install" list but indicated to update
				if (selectedPlugin.getAction() == PluginObject.ACTION_UPDATE) {
					changeToInstallList.remove(selectedPlugin);
					changeToUpdateList.add(selectedPlugin);
					dependencyList = selectedPlugin.getNewDependencies();
				} else {
					dependencyList = selectedPlugin.getDependencies();
				}
			}
		}
		//Otherwise, it only means "installable", no updates
		else {
			//if it is not an "Update-able" state, then go on as per normal
			dependencyList = selectedPlugin.getDependencies();
		}

		//if there are no dependencies for this selected plugin
		if (dependencyList == null || dependencyList.size() == 0) {
			return;
		} else {
			//if there are dependencies, check for prerequisites
			for (int i = 0; i < dependencyList.size(); i++) {

				Dependency dependency = dependencyList.get(i);
				for (int j = 0; j < pluginList.size(); j++) {
					PluginObject plugin = pluginList.get(j);

					//if Prerequisite is found,
					if (plugin.getFilename().equals(dependency.getFilename())) {
						boolean inInstallList = changeToInstallList.contains(plugin);
						boolean inUpdateList = changeToUpdateList.contains(plugin);
						//Which does not exist in any of the "change" lists yet
						if (!inInstallList && !inUpdateList) {

							//if prerequisite installed/uninstalled
							if (plugin.getStatus() == PluginObject.STATUS_INSTALLED ||
								plugin.getStatus() == PluginObject.STATUS_UNINSTALLED) {
								//add to list
								changeToInstallList.add(plugin);
								addDependency(changeToInstallList, changeToUpdateList, plugin);
							}
							//if prerequisite is update-able
							else if (plugin.getStatus() == PluginObject.STATUS_MAY_UPDATE) {
								//if current dependency's plugin is outdated
								if (plugin.getTimestamp().compareTo(dependency.getTimestamp()) < 0) {
									changeToUpdateList.add(plugin); //add to update list ("special" case)
								} else { //if not, just installing is the minimum
									changeToInstallList.add(plugin);
								}
								addDependency(changeToInstallList, changeToUpdateList, plugin);
							}
						}
						//if previous "update-able" prerequisite only requires an install
						else if (inInstallList && !inUpdateList &&
								plugin.getStatus() == PluginObject.STATUS_MAY_UPDATE) {
							//Then check again if this current dependency's plugin is outdated
							if (plugin.getTimestamp().compareTo(dependency.getTimestamp()) < 0) {
								changeToInstallList.remove(plugin);
								changeToUpdateList.add(plugin); //add to update list ("special" case)
								addDependency(changeToInstallList, changeToUpdateList, plugin);
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

	//comes up with a list of plugins needed to be removed if selected is removed
	public void removeDependent(List<PluginObject> changeToUninstallList, PluginObject selectedPlugin) {
		if (!changeToUninstallList.contains(selectedPlugin)) {
			changeToUninstallList.add(selectedPlugin);
		}
		//Search through entire list
		for (int i = 0; i < pluginList.size(); i++) {
			PluginObject plugin = pluginList.get(i);
			boolean inUninstallList = changeToUninstallList.contains(plugin);
			if (inUninstallList) {
				continue; //already in UninstallList ==> Its dependents are assumed to be too
			} else {
				List<Dependency> dependencyList = plugin.getDependencies();
				if (dependencyList == null || dependencyList.size() == 0) {
					//do nothing
				} else {
					for (int j = 0; j < dependencyList.size(); j++) {
						Dependency dependency = dependencyList.get(j);
						String dependencyFilename = dependency.getFilename();
						if (dependencyFilename.equals(selectedPlugin.getFilename())) {
							changeToUninstallList.add(plugin);
							removeDependent(changeToUninstallList, plugin);
							break;
						}
					}
				}
			}
		}
	}

	//Using a compiled list of dependencies for all plugins selected by user
	//Objective is to show user only information that was previously invisible
	public List<PluginObject> getUnlistedInstalls(List<PluginObject> toInstallList) {
		List<PluginObject> unlistedInstalls = new ArrayList<PluginObject>();
		for (int i = 0; i < toInstallList.size(); i++) {
			PluginObject plugin = toInstallList.get(i);
			if ((plugin.getStatus() == PluginObject.STATUS_INSTALLED ||
				plugin.getStatus() == PluginObject.STATUS_MAY_UPDATE) &&
				plugin.getAction() == PluginObject.ACTION_REVERSE) {
				unlistedInstalls.add(plugin);
			} else if (plugin.getStatus() == PluginObject.STATUS_UNINSTALLED &&
					plugin.getAction() == PluginObject.ACTION_NONE) {
				unlistedInstalls.add(plugin);
			}
		}
		return unlistedInstalls;
	}

	//Using a compiled list of dependencies of all plugins selected by user
	//Objective is to show user only information that was previously invisible
	public List<PluginObject> getUnlistedUpdates(List<PluginObject> toUpdateList) {
		List<PluginObject> unlistedUpdates = new ArrayList<PluginObject>();
		for (int i = 0; i < toUpdateList.size(); i++) {
			PluginObject plugin = toUpdateList.get(i);
			if (plugin.getStatus() == PluginObject.STATUS_MAY_UPDATE &&
				plugin.getAction() != PluginObject.ACTION_UPDATE) {
				unlistedUpdates.add(plugin);
			}
		}
		return unlistedUpdates;
	}

	//Using a compiled list of dependencies of all plugins deselected by user
	//Objective is to show user only information that was previously invisible
	public List<PluginObject> getUnlistedRemoves(List<PluginObject> toUninstallList) {
		List<PluginObject> unlistedRemoves = new ArrayList<PluginObject>();
		for (int i = 0; i < toUninstallList.size(); i++) {
			PluginObject plugin = toUninstallList.get(i);
			if (plugin.getStatus() == PluginObject.STATUS_INSTALLED &&
				plugin.getAction() == PluginObject.ACTION_NONE) {
				unlistedRemoves.add(plugin);
			} else if (plugin.getStatus() == PluginObject.STATUS_UNINSTALLED &&
					plugin.getAction() == PluginObject.ACTION_REVERSE) {
				unlistedRemoves.add(plugin);
			} else if (plugin.getStatus() == PluginObject.STATUS_MAY_UPDATE &&
					plugin.getAction() != PluginObject.ACTION_REVERSE) {
				unlistedRemoves.add(plugin);
			}
		}
		return unlistedRemoves;
	}

	//forces action for every plugin in the list to "install"
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

	//forces action for every update-able plugin in the list to be "update"
	public void setToUpdate(List<PluginObject> selectedList) {
		for (int i = 0; i < selectedList.size(); i++) {
			PluginObject plugin = selectedList.get(i);
			if (plugin.getStatus() == PluginObject.STATUS_MAY_UPDATE) {
				plugin.setAction(PluginObject.ACTION_UPDATE);
			}
		}
	}

	//forces action for every plugin in the list to be "uninstall"
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

	public List<PluginObject> getPluginList() {
		return pluginList;
	}
}

