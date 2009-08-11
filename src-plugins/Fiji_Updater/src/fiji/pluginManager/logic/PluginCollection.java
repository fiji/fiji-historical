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
			if (lcFilename.indexOf(text) >= 0)
				return true;
			else if (plugin.getPluginDetails().matchesDetails(text))
				return true;
			else
				return false;
		}
	}

	public static Filter getFilterForText(String searchText) {
		return new TextFilter(searchText);
	}

	//take in only plugins that are neither installed nor told to do so
	public static final Filter FILTER_UNLISTED_TO_INSTALL = new Filter() {
		public boolean matchesFilter(PluginObject plugin) {
			boolean actionNone = (plugin.isInstallable() && !plugin.actionSpecified());
			return (plugin.toRemove() || actionNone || plugin.toUpload());
		}
	};

	//take in only update-able plugins not instructed to update
	public static final Filter FILTER_UNLISTED_TO_UPDATE = new Filter() {
		public boolean matchesFilter(PluginObject plugin) {
			boolean actionNoUpdate = plugin.isUpdateable() && !plugin.toUpdate();
			return (actionNoUpdate || plugin.toUpload());
		}
	};

	//take in only plugins that are not instructed to uninstall
	public static final Filter FILTER_UNLISTED_TO_UNINSTALL = new Filter() {
		public boolean matchesFilter(PluginObject plugin) {
			boolean actionNotRemove = plugin.isRemovable() && !plugin.toRemove();
			return (actionNotRemove || plugin.toInstall() || plugin.toUpload());
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

	public static final Filter FILTER_ACTIONS_UPDATE = new Filter() {
		public boolean matchesFilter(PluginObject plugin) {
			return plugin.toUpdate();
		}
	};

	public static final Filter FILTER_ACTIONS_INSTALL = new Filter() {
		public boolean matchesFilter(PluginObject plugin) {
			return plugin.toInstall();
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

	public static final Filter FILTER_READONLY = new Filter() {
		public boolean matchesFilter(PluginObject plugin) {
			return plugin.isReadOnly();
		}
	};

	public Iterator<PluginObject> getIterator(Filter filter) {
		return getList(filter).iterator();
	}

	public PluginCollection getList(Filter filter) {
		PluginCollection myList = new PluginCollection();
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

	public PluginObject getPluginFromTimestamp(String filename, String timestamp) {
		for (PluginObject plugin : this) {
			if (plugin.getFilename().equals(filename) &&
					plugin.getTimestamp().equals(timestamp)) {
				return plugin;
			}
		}
		return null;
	}

	public PluginObject getPluginFromDigest(String filename, String digest) {
		for (PluginObject plugin : this) {
			if (plugin.getFilename().equals(filename) &&
					plugin.getmd5Sum().equals(digest)) {
				return plugin;
			}
		}
		return null;
	}

	//this method assumes list of plugins are of the same filename (i.e.: different versions)
	public PluginObject getLatestPlugin() {
		PluginObject latest = null;
		for (PluginObject plugin : this)
			if (latest == null || plugin.getTimestamp().compareTo(latest.getTimestamp()) > 0)
				latest = plugin;
		return latest;
	}

	public void resetChangeStatuses() {
		for (PluginObject plugin : this) {
			plugin.resetChangeStatuses();
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
