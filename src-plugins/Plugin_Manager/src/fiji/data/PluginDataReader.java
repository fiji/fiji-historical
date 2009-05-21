package fiji.data;
import java.util.ArrayList;
import java.util.List;

public class PluginDataReader {
	private List<PluginObject> pluginList = null;
	private List downloadNameList = null; //tentatively, array of strings from Downloader

	public PluginDataReader() {
		pluginList = new ArrayList<PluginObject>();
		//How to get "database" of information:
		//1.) build a list of installed plugins
		//2.) Add these to DB, default status "Installed"
		//3.) using _same_ method, derive an updated list from current.txt
		//4.) -New plugins on current.txt added to DB as "Uninstalled",
		//    -Plugins with updates as indicated on current.txt, change to "Update-able",
		//    -Plugins with same timestamp as that of current.txt, remain as it is.
		//5.) Get additional information (dependencies and descriptions) from
		//    "database.xml" and add to DB.
		//
		// Hmmm... For update-able plugins, should there be an extra attribute
		// "latestTimestamp" as well? (Thus need not go back to current.txt to fetch
		// timestamp info in the case of update)

		//retrieve information of installed plugins...
		PluginObject pluginA = new PluginObject("PluginA.jar", "65c3ecc1bbd7564f92545ffd2521f9d96509ca64", "jars", Long.parseLong("20090429190842"), "This is a description of Plugin A", null, PluginObject.STATUS_UNINSTALLED, PluginObject.ACTION_NONE);
		
		Dependency dependencyB1 = new Dependency("PluginA.jar", Long.parseLong("20090429190842"));
		ArrayList<Dependency> Bdependency = new ArrayList<Dependency>();
		Bdependency.add(dependencyB1);
		PluginObject pluginB = new PluginObject("PluginB.jar", "9624fa93cbf7720c01c7ff97c28b00747b700de3", "plugins", Long.parseLong("20090429190854"), "This is a description of Plugin B", Bdependency, PluginObject.STATUS_UNINSTALLED, PluginObject.ACTION_NONE);
		
		Dependency dependencyC2 = new Dependency("PluginA.jar", Long.parseLong("20090429190842"));
		Dependency dependencyC3 = new Dependency("PluginB.jar", Long.parseLong("20090429190854"));
		ArrayList<Dependency> Cdependency = new ArrayList<Dependency>();
		Cdependency.add(dependencyC2);
		Cdependency.add(dependencyC3);
		PluginObject pluginC = new PluginObject("PluginC.jar", "1a992dbc077ef84020d44a980c7992ba6c8edf3d", "misc", Long.parseLong("20090429190854"), "This is a description of Plugin C", Cdependency, PluginObject.STATUS_UNINSTALLED, PluginObject.ACTION_NONE);

		PluginObject pluginD = new PluginObject("PluginD.jar", "61c3ecc1add7364f92545ffd2521e9d96508cb62", "jars", Long.parseLong("20090429190842"), "This is a description of Plugin D", null, PluginObject.STATUS_INSTALLED, PluginObject.ACTION_NONE);

		Dependency dependencyE4 = new Dependency("PluginA.jar", Long.parseLong("20090429190842"));
		ArrayList<Dependency> Edependency = new ArrayList<Dependency>();
		Edependency.add(dependencyE4);
		PluginObject pluginE = new PluginObject("PluginE.jar", "8114fe93cbf7720c01c7ff97c28b007b79900dc7", "plugins", Long.parseLong("20090501190854"), "This is a description of Plugin E", Edependency, PluginObject.STATUS_MAY_UPDATE, PluginObject.ACTION_NONE);

		Dependency dependencyF5 = new Dependency("PluginE.jar",Long.parseLong("20090501190854"));
		Dependency dependencyF6 = new Dependency("PluginB.jar",Long.parseLong("20090429190854"));
		ArrayList<Dependency> Fdependency = new ArrayList<Dependency>();
		Fdependency.add(dependencyF5);
		Fdependency.add(dependencyF6);
		PluginObject pluginF = new PluginObject("PluginF.jar", "1b992dbca07ef84020d44a980c7902ba6c82dfee", "misc", Long.parseLong("20090509190854"), "This is a description of Plugin F", Fdependency, PluginObject.STATUS_MAY_UPDATE, PluginObject.ACTION_NONE);

		pluginList.add(pluginA);
		pluginList.add(pluginB);
		pluginList.add(pluginC);
		pluginList.add(pluginD);
		pluginList.add(pluginE);
		pluginList.add(pluginF);
	}

	public List<PluginObject> getExistingPluginList() {
		return pluginList;
	}

	public void generateUpdatesPluginList() {
	}
}
