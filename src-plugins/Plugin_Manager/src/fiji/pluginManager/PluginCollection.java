package fiji.pluginManager;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

public class PluginCollection extends ArrayList<PluginObject> {
	public interface Filter {
		boolean matchesFilter(PluginObject plugin);
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

	public PluginCollection() {}

	public Iterator<PluginObject> getIterator(Filter filter) {
		return getList(filter).iterator();
	}

	public List<PluginObject> getList(Filter filter) {
		List<PluginObject> myList = new PluginCollection();
		for (PluginObject plugin : this)
			if (filter.matchesFilter(plugin)) myList.add(plugin);
		return myList;
	}
}
