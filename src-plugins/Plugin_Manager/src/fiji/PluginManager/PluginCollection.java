package fiji.pluginManager;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

public class PluginCollection extends ArrayList<PluginObject> {
	public interface Filter {
		boolean matchesFilter(PluginObject plugin);
	}

	public static final Filter FILTER_ACTIONSUNINSTALL = new Filter() {
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

	public static final Filter FILTER_STATUSALREADYINSTALLED = new Filter() {
		public boolean matchesFilter(PluginObject plugin) {
			return (plugin.getStatus() == PluginObject.STATUS_INSTALLED ||
					plugin.getStatus() == PluginObject.STATUS_MAY_UPDATE);
		}
	};

	public static final Filter FILTER_STATUSUNINSTALLED = new Filter() {
		public boolean matchesFilter(PluginObject plugin) {
			return (plugin.getStatus() == PluginObject.STATUS_UNINSTALLED);
		}
	};

	public static final Filter FILTER_STATUSINSTALLED = new Filter() {
		public boolean matchesFilter(PluginObject plugin) {
			return (plugin.getStatus() == PluginObject.STATUS_INSTALLED);
		}
	};

	public static final Filter FILTER_STATUSMAYUPDATE = new Filter() {
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
