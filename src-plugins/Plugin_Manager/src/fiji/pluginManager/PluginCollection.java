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
			boolean currentActionUninstall =
				((plugin.getStatus() == PluginObject.STATUS_INSTALLED ||
					plugin.getStatus() == PluginObject.STATUS_MAY_UPDATE) &&
					plugin.getAction() == PluginObject.ACTION_REVERSE);
			boolean currentActionNone =
				(plugin.getStatus() == PluginObject.STATUS_UNINSTALLED &&
					plugin.getAction() == PluginObject.ACTION_NONE);
			return (currentActionUninstall || currentActionNone);
		}
	};

	//take in only update-able plugins that are not indicated to update
	public static final Filter FILTER_UNLISTED_TO_UPDATE = new Filter() {
		public boolean matchesFilter(PluginObject plugin) {
			return (plugin.getStatus() == PluginObject.STATUS_MAY_UPDATE &&
					plugin.getAction() != PluginObject.ACTION_UPDATE);
		}
	};

	//take in only plugins that are not indicated to uninstall
	public static final Filter FILTER_UNLISTED_TO_UNINSTALL = new Filter() {
		public boolean matchesFilter(PluginObject plugin) {
			boolean currentActionNone =
				(plugin.getStatus() == PluginObject.STATUS_INSTALLED &&
						plugin.getAction() == PluginObject.ACTION_NONE);
			boolean currentActionInstall =
				(plugin.getStatus() == PluginObject.STATUS_UNINSTALLED &&
						plugin.getAction() == PluginObject.ACTION_REVERSE);
			boolean currentActionNotUninstall =
				(plugin.getStatus() == PluginObject.STATUS_MAY_UPDATE &&
						plugin.getAction() != PluginObject.ACTION_REVERSE);
			return (currentActionNone || currentActionInstall || currentActionNotUninstall);
		}
	};

	public static final Filter FILTER_ACTIONS_SPECIFIED = new Filter() {
		public boolean matchesFilter(PluginObject plugin) {
			return (plugin.getAction() != PluginObject.ACTION_NONE);
		}
	};

	public static final Filter FILTER_ACTIONS_UNINSTALL = new Filter() {
		public boolean matchesFilter(PluginObject plugin) {
			return ((plugin.getStatus() == PluginObject.STATUS_INSTALLED ||
					plugin.getStatus() == PluginObject.STATUS_MAY_UPDATE) &&
					plugin.getAction() == PluginObject.ACTION_REVERSE);
		}
	};

	public static final Filter FILTER_ACTIONS_ADDORUPDATE = new Filter() {
		public boolean matchesFilter(PluginObject plugin) {
			return ((plugin.getStatus() == PluginObject.STATUS_UNINSTALLED &&
					plugin.getAction() == PluginObject.ACTION_REVERSE) ||
					(plugin.getStatus() == PluginObject.STATUS_MAY_UPDATE &&
					plugin.getAction() == PluginObject.ACTION_UPDATE));
		}
	};

	public static final Filter FILTER_STATUS_ALREADYINSTALLED = new Filter() {
		public boolean matchesFilter(PluginObject plugin) {
			return (plugin.getStatus() == PluginObject.STATUS_INSTALLED ||
					plugin.getStatus() == PluginObject.STATUS_MAY_UPDATE);
		}
	};

	public static final Filter FILTER_STATUS_UNINSTALLED = new Filter() {
		public boolean matchesFilter(PluginObject plugin) {
			return (plugin.getStatus() == PluginObject.STATUS_UNINSTALLED);
		}
	};

	public static final Filter FILTER_STATUS_INSTALLED = new Filter() {
		public boolean matchesFilter(PluginObject plugin) {
			return (plugin.getStatus() == PluginObject.STATUS_INSTALLED);
		}
	};

	public static final Filter FILTER_STATUS_MAYUPDATE = new Filter() {
		public boolean matchesFilter(PluginObject plugin) {
			return (plugin.getStatus() == PluginObject.STATUS_MAY_UPDATE);
		}
	};

	public PluginCollection() {}

	public Iterator<PluginObject> getIterator(Filter filter) {
		return getList(filter).iterator();
	}

	public List<PluginObject> getList(Filter filter) {
		Iterator<PluginObject> iter = iterator();
		List<PluginObject> myList = new PluginCollection();
		while (iter.hasNext()) {
			PluginObject plugin = iter.next();
			if (filter.matchesFilter(plugin)) myList.add(plugin);
		}
		return myList;
	}
}
