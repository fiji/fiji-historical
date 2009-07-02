package fiji.pluginManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/*
 * Determine the dependencies of the plugin through ADD and REMOVE scenarios.
 * The dependencies are determined based on the assumption that the user has already
 * selected the plugins he/she wanted to add or remove and indicated to take action.
 */
public class DependencyCompiler {
	private List<PluginObject> pluginList; //current states of all plugins

	//The different structures of the same information that the user can retrieve
	public List<PluginObject> changeList;
	public Map<PluginObject,List<PluginObject>> installDependenciesMap;
	public Map<PluginObject,List<PluginObject>> updateDependenciesMap;
	public Map<PluginObject,List<PluginObject>> uninstallDependentsMap;
	public List<PluginObject> toInstallList;
	public List<PluginObject> toUpdateList;
	public List<PluginObject> toRemoveList;

	public DependencyCompiler(List<PluginObject> pluginList) {
		this.pluginList = pluginList;
		changeList = ((PluginCollection)pluginList).getList(PluginCollection.FILTER_ACTIONS_SPECIFIED_NOT_UPLOAD);
		List<PluginObject> change_addOrUpdateList = ((PluginCollection)changeList).getList(PluginCollection.FILTER_ACTIONS_ADDORUPDATE);
		List<PluginObject> change_removeList = ((PluginCollection)changeList).getList(PluginCollection.FILTER_ACTIONS_UNINSTALL);

		//Generates a map of plugins and their individual dependencies/dependents
		installDependenciesMap = new HashMap<PluginObject,List<PluginObject>>();
		updateDependenciesMap = new HashMap<PluginObject,List<PluginObject>>();
		uninstallDependentsMap = new HashMap<PluginObject,List<PluginObject>>();

		//Going through list requesting for ADD or UPDATE
		for (PluginObject myPlugin : change_addOrUpdateList) {
			//Generate lists of dependencies for each plugin
			List<PluginObject> toInstallList = new ArrayList<PluginObject>();
			List<PluginObject> toUpdateList = new ArrayList<PluginObject>();
			addDependency(toInstallList, toUpdateList, myPlugin);
			installDependenciesMap.put(myPlugin, toInstallList);
			updateDependenciesMap.put(myPlugin, toUpdateList);
		}

		//Going through list requesting for REMOVE
		for (PluginObject myPlugin : change_removeList) {
			//Generate lists of dependents for each plugin
			List<PluginObject> toRemoveList = new ArrayList<PluginObject>();
			addDependent(toRemoveList, myPlugin);
			uninstallDependentsMap.put(myPlugin, toRemoveList);
		}

		//Combines all the dependencies for individual plugins into one list
		toInstallList = new PluginCollection();
		toUpdateList = new PluginCollection();
		toRemoveList = new PluginCollection();
		unifyInstallAndUpdateList(installDependenciesMap,
				updateDependenciesMap,
				toInstallList,
				toUpdateList);
		unifyUninstallList(uninstallDependentsMap, toRemoveList);
	}

	//comes up with a list of plugins needed for download to go with this selected plugin
	private void addDependency(List<PluginObject> changeToInstallList, List<PluginObject> changeToUpdateList, PluginObject selectedPlugin) {
		//First retrieve the dependency list
		List<Dependency> dependencyList = new ArrayList<Dependency>();
		boolean updateableState = selectedPlugin.isUpdateable();
		boolean selectedInInstallList = changeToInstallList.contains(selectedPlugin);
		boolean selectedInUpdateList = changeToUpdateList.contains(selectedPlugin);

		//Does not belong in any lists yet
		if (!selectedInInstallList && !selectedInUpdateList) {
			if (updateableState) {
				//in context of "addDependency", it can only mean "update the plugin"
				changeToUpdateList.add(selectedPlugin);
				selectedInInstallList = false;
				selectedInUpdateList = true;
			} else {
				//in context of "addDependency", it can only mean "install the plugin"
				changeToInstallList.add(selectedPlugin);
				selectedInInstallList = true;
				selectedInUpdateList = false;
			}
		} //else... (Plugin already added earlier)

		//if "Update-able" state
		if (updateableState) {
			//if in updateList, use update's dependencies, if not, go on as per normal
			if (!selectedInInstallList && selectedInUpdateList) {
				dependencyList = selectedPlugin.getNewDependencies();
			} else if (selectedInInstallList && !selectedInUpdateList) {
				//if already in the "Install" list but indicated to update
				if (selectedPlugin.toUpdate()) {
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
			for (Dependency dependency : dependencyList) {

				for (PluginObject plugin : pluginList) {

					//if Prerequisite is found,
					if (plugin.getFilename().equals(dependency.getFilename())) {
						boolean inInstallList = changeToInstallList.contains(plugin);
						boolean inUpdateList = changeToUpdateList.contains(plugin);
						//Which does not exist in any of the "change" lists yet
						if (!inInstallList && !inUpdateList) {

							//if prerequisite installed/uninstalled
							if (plugin.isRemovableOnly() || plugin.isInstallable()) {
								//add to list
								changeToInstallList.add(plugin);
								addDependency(changeToInstallList, changeToUpdateList, plugin);
							}
							//if prerequisite is update-able
							else if (plugin.isUpdateable()) {
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
						else if (inInstallList && !inUpdateList && plugin.isUpdateable()) {
							//Then check again if this current dependency's plugin is outdated
							if (plugin.getTimestamp().compareTo(dependency.getTimestamp()) < 0) {
								changeToInstallList.remove(plugin);
								changeToUpdateList.add(plugin); //add to update list ("special" case)
								addDependency(changeToInstallList, changeToUpdateList, plugin);
							}
						} //else do nothing, prerequisites added already
						break;
					}
				} //end of searching through pluginList

			}
		}
	}

	//comes up with a list of plugins needed to be removed if selected is removed
	private void addDependent(List<PluginObject> changeToUninstallList, PluginObject selectedPlugin) {
		if (!changeToUninstallList.contains(selectedPlugin)) {
			changeToUninstallList.add(selectedPlugin);
		}
		//Search through entire list
		for (PluginObject plugin : pluginList) {

			boolean inUninstallList = changeToUninstallList.contains(plugin);
			if (inUninstallList) {
				continue; //already in UninstallList ==> Its dependents are assumed to be too
			} else {
				List<Dependency> dependencyList = plugin.getDependencies();
				if (dependencyList == null || dependencyList.size() == 0) {
					//do nothing
				} else {
					for (Dependency dependency : dependencyList) {
						String dependencyFilename = dependency.getFilename();
						if (dependencyFilename.equals(selectedPlugin.getFilename())) {
							changeToUninstallList.add(plugin);
							addDependent(changeToUninstallList, plugin);
							break;
						}
					}
				}
			}

		} //end of search through pluginList
	}

	private void addToListWithNoDuplicates(List<PluginObject> existingList, List<PluginObject> additional) {
		//For every plugin in this list
		for (PluginObject plugin : additional) {
			//if existing list does not contain the plugin yet, add it
			if (!existingList.contains(plugin)) {
				existingList.add(plugin);
			}
		}
	}

	//combine the mapping of installs into one single list of "to install" plugins
	//the same goes for the mapping of updates ==> "to update" list
	private void unifyInstallAndUpdateList(Map<PluginObject,List<PluginObject>> installDependenciesMap,
			Map<PluginObject,List<PluginObject>> updateDependenciesMap,
			List<PluginObject> installList,
			List<PluginObject> updateList) {

		Iterator<List<PluginObject>> iterInstallLists = installDependenciesMap.values().iterator();
		while (iterInstallLists.hasNext())
			addToListWithNoDuplicates(installList, iterInstallLists.next());

		Iterator<List<PluginObject>> iterUpdateLists = updateDependenciesMap.values().iterator();
		while (iterUpdateLists.hasNext())
			addToListWithNoDuplicates(updateList, iterUpdateLists.next());

		Iterator<PluginObject> iterInstall = installList.iterator();
		while (iterInstall.hasNext()) {
			PluginObject plugin = iterInstall.next();
			if (updateList.contains(plugin)) {
				installList.remove(plugin); //since it already exists in "Update list"
			}
		}
	}

	//combine the mapping of uninstalls into one single list of "to uninstall" plugins
	private void unifyUninstallList(Map<PluginObject,List<PluginObject>> uninstallDependentsMap,
			List<PluginObject> uninstallList) {

		Iterator<List<PluginObject>> iterUninstallLists = uninstallDependentsMap.values().iterator();
		while (iterUninstallLists.hasNext())
			addToListWithNoDuplicates(uninstallList, iterUninstallLists.next());
	}

	private boolean conflicts(List<PluginObject> list1, List<PluginObject> list2) {
		Iterator<PluginObject> iter = list1.iterator();
		while (iter.hasNext()) {
			PluginObject thisPlugin = iter.next();
			if (list2.contains(thisPlugin)) return true;
		}
		return false;
	}

	public boolean conflicts(List<PluginObject> installList, List<PluginObject> updateList, List<PluginObject> uninstallList) {
		if (!conflicts(installList, uninstallList) && !conflicts(updateList, uninstallList))
			return false;
		else
			return true;
	}

	//forces action for every plugin in the list to "install"
	public void setToInstall(List<PluginObject> selectedList) {
		for (PluginObject plugin : selectedList) {
			if (plugin.isRemovableOnly() || plugin.isUpdateable())
				plugin.setActionNone();
			else if (plugin.isInstallable())
				plugin.setActionToInstall();
		}
	}

	//forces action for every update-able plugin in the list to be "update"
	public void setToUpdate(List<PluginObject> selectedList) {
		for (PluginObject plugin : selectedList)
			if (plugin.isUpdateable())
				plugin.setActionToUpdate();
	}

	//forces action for every plugin in the list to be "uninstall"
	public void setToRemove(List<PluginObject> selectedList) {
		for (PluginObject plugin : selectedList) {
			if (plugin.isRemovableOnly())
				plugin.setActionToRemove();
			else if (plugin.isInstallable())
				plugin.setActionNone();
			else if (plugin.isUpdateable())
				plugin.setActionToRemove();
		}
	}
}