package fiji.data;
import java.util.ArrayList;
import java.util.List;

public class PluginDataReader {
	private List<PluginObject> updatesPluginList = null;
	private List<PluginObject> existingPluginList = null;
	private List downloadNameList = null; //tentatively, array of strings from Downloader

	public PluginDataReader() {
		existingPluginList = new ArrayList<PluginObject>();

		//retrieve information of installed plugins...
		Dependency dependency1 = new Dependency("PluginA.jar", Long.parseLong("20090429190842"));
		ArrayList<Dependency> Bdependency = new ArrayList<Dependency>();
		Bdependency.add(dependency1);
		Dependency dependency2 = new Dependency("PluginA.jar", Long.parseLong("20090429190842"));
		Dependency dependency3 = new Dependency("PluginB.jar", Long.parseLong("20090429190854"));
		ArrayList<Dependency> Cdependency = new ArrayList<Dependency>();
		Cdependency.add(dependency2);
		Cdependency.add(dependency3);
		PluginObject pluginA = new PluginObject("PluginA.jar", "65c3ecc1bbd7564f92545ffd2521f9d96509ca64", "20090429190842", "jars", "This is a description of Plugin A", null, true, 0);
		PluginObject pluginB = new PluginObject("PluginB.jar", "9624fa93cbf7720c01c7ff97c28b00747b700de3", "20090429190854", "plugins", "This is a description of Plugin B", Bdependency, false, 0);
		PluginObject pluginC = new PluginObject("PluginC.jar", "1a992dbc077ef84020d44a980c7992ba6c8edf3d", "20090429190854", "misc", "This is a description of Plugin C", Cdependency, true, 0);
		existingPluginList.add(pluginA);
		existingPluginList.add(pluginB);
		existingPluginList.add(pluginC);
	}

	public List<PluginObject> getExistingPluginList() {
		return existingPluginList;
	}

	public List<PluginObject> getUpdatesPluginList() {
		return updatesPluginList;
	}

	public void generateUpdatesPluginList() {
		updatesPluginList = new ArrayList<PluginObject>();
		//to-update plugins... (Assume you have extracted the data already)
		ArrayList<Dependency> Edependency = new ArrayList<Dependency>();
		Dependency dependency4 = new Dependency("PluginA.jar", Long.parseLong("20090429190842"));
		Edependency.add(dependency4);
		ArrayList<Dependency> Fdependency = new ArrayList<Dependency>();
		Dependency dependency5 = new Dependency("PluginE.jar",Long.parseLong("20090501190854"));
		Dependency dependency6 = new Dependency("PluginB.jar",Long.parseLong("20090429190854"));
		Fdependency.add(dependency5);
		Fdependency.add(dependency6);
		PluginObject pluginD = new PluginObject("PluginD.jar", "61c3ecc1add7364f92545ffd2521e9d96508cb62", "20090429190842", "jars", "This is a description of Plugin D", null, false, 0);
		PluginObject pluginE = new PluginObject("PluginE.jar", "8114fe93cbf7720c01c7ff97c28b007b79900dc7", "20090501190854", "plugins", "This is a description of Plugin E", Edependency, false, 0);
		PluginObject pluginF = new PluginObject("PluginF.jar", "1b992dbca07ef84020d44a980c7902ba6c82dfee", "20090509190854", "misc", "This is a description of Plugin F", Fdependency, false, 0);
		updatesPluginList.add(pluginD);
		updatesPluginList.add(pluginE);
		updatesPluginList.add(pluginF);
	}
}
