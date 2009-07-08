package fiji.pluginManager.logic;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

public class PluginCollection extends ArrayList<PluginObject> {
	public interface Filter {
		boolean matchesFilter(PluginObject plugin);
	}

	private static class TextFilter implements Filter {
		String text;

		public TextFilter(String text) {
			this.text = text.trim().toLowerCase();
		}

		//determining whether search text fits description/title
		public boolean matchesFilter(PluginObject plugin) {
			String lcFilename = plugin.getFilename().trim().toLowerCase();
			boolean existsInTitle = (lcFilename.indexOf(text) >= 0);
			boolean existsInDescription = false;

			if (plugin.getDescription() != null) {
				String lcDescription = plugin.getDescription().trim().toLowerCase();
				existsInDescription = (lcDescription.indexOf(text) >= 0);
			}

			return (existsInTitle || existsInDescription);
		}
	}

	public static Filter getFilterForText(String searchText) {
		return new TextFilter(searchText);
	}

	//take in only plugins that are neither installed nor told to do so
	public static final Filter FILTER_UNLISTED_TO_INSTALL = new Filter() {
		public boolean matchesFilter(PluginObject plugin) {
			boolean currentActionUninstall = plugin.toRemove();
			boolean currentActionNone = (plugin.isInstallable() && !plugin.actionSpecified());
			return (currentActionUninstall || currentActionNone);
		}
	};

	//take in only update-able plugins that are not indicated to update
	public static final Filter FILTER_UNLISTED_TO_UPDATE = new Filter() {
		public boolean matchesFilter(PluginObject plugin) {
			return (plugin.isUpdateable() && !plugin.toUpdate());
		}
	};

	//take in only plugins that are not indicated to uninstall
	public static final Filter FILTER_UNLISTED_TO_UNINSTALL = new Filter() {
		public boolean matchesFilter(PluginObject plugin) {
			boolean currentActionNone = (plugin.isRemovableOnly() && !plugin.actionSpecified());
			boolean currentActionInstall = plugin.toInstall();
			boolean currentActionNotUninstall = (plugin.isUpdateable() && !plugin.toRemove());
			return (currentActionNone || currentActionInstall || currentActionNotUninstall);
		}
	};

	public static final Filter FILTER_ACTIONS_SPECIFIED = new Filter() {
		public boolean matchesFilter(PluginObject plugin) {
			return plugin.actionSpecified();
		}
	};

	public static final Filter FILTER_ACTIONS_SPECIFIED_NOT_UPLOAD = new Filter() {
		public boolean matchesFilter(PluginObject plugin) {
			return (plugin.actionSpecified() && !plugin.toUpload());
		}
	};

	public static final Filter FILTER_ACTIONS_UPLOAD = new Filter() {
		public boolean matchesFilter(PluginObject plugin) {
			return plugin.toUpload();
		}
	};

	public static final Filter FILTER_ACTIONS_UNINSTALL = new Filter() {
		public boolean matchesFilter(PluginObject plugin) {
			return plugin.toRemove();
		}
	};

	public static final Filter FILTER_ACTIONS_ADDORUPDATE = new Filter() {
		public boolean matchesFilter(PluginObject plugin) {
			return (plugin.toInstall() || plugin.toUpdate());
		}
	};

	public static final Filter FILTER_STATUS_ALREADYINSTALLED = new Filter() {
		public boolean matchesFilter(PluginObject plugin) {
			return plugin.isRemovable();
		}
	};

	public static final Filter FILTER_STATUS_UNINSTALLED = new Filter() {
		public boolean matchesFilter(PluginObject plugin) {
			return plugin.isInstallable();
		}
	};

	public static final Filter FILTER_STATUS_INSTALLED = new Filter() {
		public boolean matchesFilter(PluginObject plugin) {
			return plugin.isRemovableOnly();
		}
	};

	public static final Filter FILTER_STATUS_MAYUPDATE = new Filter() {
		public boolean matchesFilter(PluginObject plugin) {
			return plugin.isUpdateable();
		}
	};

	public static final Filter FILTER_FIJI = new Filter() {
		public boolean matchesFilter(PluginObject plugin) {
			return plugin.isFijiPlugin();
		}
	};

	public static final Filter FILTER_NOT_FIJI = new Filter() {
		public boolean matchesFilter(PluginObject plugin) {
			return !plugin.isFijiPlugin();
		}
	};

	public static final Filter FILTER_CHANGE_SUCCEEDED = new Filter() {
		public boolean matchesFilter(PluginObject plugin) {
			return plugin.changeSucceeded();
		}
	};

	public static final Filter FILTER_CHANGE_FAILED = new Filter() {
		public boolean matchesFilter(PluginObject plugin) {
			return plugin.changeFailed();
		}
	};

	public static final Filter FILTER_NO_CHANGE_YET = new Filter() {
		public boolean matchesFilter(PluginObject plugin) {
			return plugin.changeNotDone();
		}
	};

	public static final Filter FILTER_NO_SUCCESSFUL_CHANGE = new Filter() {
		public boolean matchesFilter(PluginObject plugin) {
			return !plugin.changeSucceeded();
		}
	};

	public static final Filter FILTER_DOWNLOAD_SUCCESS = new Filter() {
		public boolean matchesFilter(PluginObject plugin) {
			return ((plugin.toInstall() || plugin.toUpdate()) &&
					plugin.changeSucceeded());
		}
	};

	public static final Filter FILTER_DOWNLOAD_FAIL = new Filter() {
		public boolean matchesFilter(PluginObject plugin) {
			return ((plugin.toInstall() || plugin.toUpdate()) &&
					plugin.changeFailed());
		}
	};

	public static final Filter FILTER_REMOVE_SUCCESS = new Filter() {
		public boolean matchesFilter(PluginObject plugin) {
			return (plugin.toRemove() && plugin.changeSucceeded());
		}
	};

	public static final Filter FILTER_REMOVE_FAIL = new Filter() {
		public boolean matchesFilter(PluginObject plugin) {
			return (plugin.toRemove() && plugin.changeFailed());
		}
	};

	public static final Filter FILTER_UPLOAD_PLUGINFILE = new Filter() {
		public boolean matchesFilter(PluginObject plugin) {
			return (plugin.toUpload() && plugin.uploadPluginFileDone());
		}
	};

	public static final Filter FILTER_UPLOAD_MODIFIEDONLY = new Filter() {
		public boolean matchesFilter(PluginObject plugin) {
			return (plugin.toUpload() && plugin.uploadModifiedOnly());
		}
	};

	public static final Filter FILTER_UPLOAD_SUCCESS = new Filter() {
		//matching either condition: file uploaded OR details uploaded only
		public boolean matchesFilter(PluginObject plugin) {
			return (plugin.toUpload() &&
					(plugin.uploadModifiedOnly() || plugin.uploadPluginFileDone()));
		}
	};

	public static final Filter FILTER_NO_SUCCESSFUL_UPLOAD = new Filter() {
		//matching either condition: file uploaded OR details uploaded only
		public boolean matchesFilter(PluginObject plugin) {
			return (plugin.toUpload() &&
					!(plugin.uploadModifiedOnly() || plugin.uploadPluginFileDone()));
		}
	};

	public static final Filter FILTER_UPLOAD_FAIL = new Filter() {
		public boolean matchesFilter(PluginObject plugin) {
			return (plugin.toUpload() && plugin.uploadFailed());
		}
	};

	public Iterator<PluginObject> getIterator(Filter filter) {
		return getList(filter).iterator();
	}

	public List<PluginObject> getList(Filter filter) {
		List<PluginObject> myList = new PluginCollection();
		for (PluginObject plugin : this)
			if (filter.matchesFilter(plugin)) myList.add(plugin);
		return myList;
	}

	public PluginObject getPlugin(String filename) { //filename is unique identifier
		for (PluginObject plugin : this) {
			if (plugin.getFilename().equals(filename)) return plugin;
		}
		return null;
	}

	public void resetChangeAndUploadStatuses() {
		for (PluginObject plugin : this) {
			plugin.resetChangeAndUploadStatuses();
		}
	}

	//forces action for every plugin in the list to "install"
	public void setToInstall() {
		for (PluginObject plugin : this) {
			if (plugin.isRemovableOnly() || plugin.isUpdateable())
				plugin.setActionNone();
			else if (plugin.isInstallable())
				plugin.setActionToInstall();
		}
	}

	//forces action for every update-able plugin in the list to be "update"
	public void setToUpdate() {
		for (PluginObject plugin : this)
			if (plugin.isUpdateable())
				plugin.setActionToUpdate();
	}

	//forces action for every plugin in the list to be "uninstall"
	public void setToRemove() {
		for (PluginObject plugin : this) {
			if (plugin.isRemovableOnly())
				plugin.setActionToRemove();
			else if (plugin.isInstallable())
				plugin.setActionNone();
			else if (plugin.isUpdateable())
				plugin.setActionToRemove();
		}
	}
}
