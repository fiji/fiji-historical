package fiji.logic;
import java.util.ArrayList;
import java.util.List;

import fiji.data.PluginObject;
import fiji.data.Dependency;

public class Controller {
	private List<PluginObject> updatesPluginList = null;
	private List<PluginObject> existingPluginList = null;
	private List downloadNameList = null; //tentatively, array of strings from Downloader

	public Controller() {
		updatesPluginList = new ArrayList<PluginObject>();
		existingPluginList = new ArrayList<PluginObject>();
		downloadNameList = new ArrayList();

		//this is where we extract the data...

		//installed plugins... (Assume you have extracted the data already)
		Dependency dependency1 = new Dependency("PluginA.jar", Long.parseLong("20090429190842"));
		ArrayList<Dependency> Bdependency = new ArrayList<Dependency>();
		Bdependency.add(dependency1);
		Dependency dependency2 = new Dependency("PluginA.jar", Long.parseLong("20090429190842"));
		Dependency dependency3 = new Dependency("PluginB.jar", Long.parseLong("20090429190854"));
		ArrayList<Dependency> Cdependency = new ArrayList<Dependency>();
		Cdependency.add(dependency2);
		Cdependency.add(dependency3);
		PluginObject pluginA = new PluginObject("PluginA.jar", "65c3ecc1bbd7564f92545ffd2521f9d96509ca64", "20090429190842", "jars", "This is a description of Plugin A", null, true, false);
		PluginObject pluginB = new PluginObject("PluginB.jar", "9624fa93cbf7720c01c7ff97c28b00747b700de3", "20090429190854", "plugins", "This is a description of Plugin B", Bdependency, true, false);
		PluginObject pluginC = new PluginObject("PluginC.jar", "1a992dbc077ef84020d44a980c7992ba6c8edf3d", "20090429190854", "misc", "This is a description of Plugin C", Cdependency, true, false);
		existingPluginList.add(pluginA);
		existingPluginList.add(pluginB);
		existingPluginList.add(pluginC);
	}

	public List<PluginObject> getUpdatesPluginList() {
		return updatesPluginList;
	}

	public List<PluginObject> getExistingPluginList() {
		return existingPluginList;
	}

	public List getDownloadNameList() {
		return downloadNameList;
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
		PluginObject pluginD = new PluginObject("PluginD.jar", "61c3ecc1add7364f92545ffd2521e9d96508cb62", "20090429190842", "jars", "This is a description of Plugin D", null, false, false);
		PluginObject pluginE = new PluginObject("PluginE.jar", "8114fe93cbf7720c01c7ff97c28b007b79900dc7", "20090501190854", "plugins", "This is a description of Plugin E", Edependency, false, false);
		PluginObject pluginF = new PluginObject("PluginF.jar", "1b992dbca07ef84020d44a980c7902ba6c82dfee", "20090509190854", "misc", "This is a description of Plugin F", Fdependency, false, false);
		updatesPluginList.add(pluginD);
		updatesPluginList.add(pluginE);
		updatesPluginList.add(pluginF);
	}
}

