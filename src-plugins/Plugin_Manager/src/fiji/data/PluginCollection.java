package fiji.data;

import java.util.List;
import java.util.ArrayList;

public class PluginCollection extends ArrayList<PluginObject> {
	public PluginCollection() {
	}

	public List<PluginObject> getListWhereStatus(int myStatus) {
		List<PluginObject> newCollection = new PluginCollection();
		if (myStatus >= 0 && myStatus <= 2) {
			for (int i = 0; i < size(); i++) {
				PluginObject myPlugin = get(i);
				if (myPlugin.getStatus() == myStatus) {
					newCollection.add(myPlugin);
				}
			}
		} else throw new Error("Specified status of value " + myStatus + " does not exist");
		return newCollection;
	}

	public List<PluginObject> getListWhereAction(int myAction) {
		List<PluginObject> newCollection = new PluginCollection();
		if (myAction >= 0 && myAction <= 2) {
			for (int i = 0; i < size(); i++) {
				PluginObject myPlugin = get(i);
				if (myPlugin.getStatus() == myAction) {
					newCollection.add(myPlugin);
				}
			}
		} else throw new Error("Specified action of value " + myAction + " does not exist");
		return newCollection;
	}

	public List<PluginObject> getListWhereActionUninstall() {
		List<PluginObject> newCollection = new PluginCollection();
		for (int i = 0; i < size(); i++) {
			PluginObject myPlugin = get(i);
			if ((myPlugin.getStatus() == PluginObject.STATUS_INSTALLED ||
				myPlugin.getStatus() == PluginObject.STATUS_MAY_UPDATE) &&
				myPlugin.getAction() == PluginObject.ACTION_REVERSE) {
				newCollection.add(myPlugin);
			}
		}
		return newCollection;
	}

	public List<PluginObject> getListWhereActionIsSpecified() {
		List<PluginObject> newCollection = new PluginCollection();
		for (int i = 0; i < size(); i++) {
			PluginObject myPlugin = get(i);
			if (myPlugin.getAction() != PluginObject.ACTION_NONE) {
				newCollection.add(myPlugin);
			}
		}
		return newCollection;
	}

	//if installed, that is either up-to-date or update-able
	public List<PluginObject> getAlreadyInstalledList() {
		List<PluginObject> newCollection = new PluginCollection();
		for (int i = 0; i < size(); i++) {
			PluginObject myPlugin = get(i);
			if (myPlugin.getStatus() == PluginObject.STATUS_INSTALLED ||
				myPlugin.getStatus() == PluginObject.STATUS_MAY_UPDATE) {
				newCollection.add(myPlugin);
			}
		}
		return newCollection;
	}
}
