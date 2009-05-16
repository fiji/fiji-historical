import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;

import javax.swing.JButton;
import javax.swing.JLabel;
import Presentation.PluginMgrUI;
import Data.PluginObject;

import java.util.ArrayList;

public class PluginManager implements PlugIn {
	private ArrayList updatesPluginList = null;
	private ArrayList existingPluginList = null;
	private ArrayList downloadNameList = null; //tentatively, array of strings from Downloader

	public void run(String arg) {
		updatesPluginList = new ArrayList();
		existingPluginList = new ArrayList();
		downloadNameList = new ArrayList();
		//installed plugins... (Assume you have extracted the data already)
		ArrayList Bdependency = new ArrayList();
		String arr[] = new String[2];
		arr[0] = "PluginA.jar";
		arr[1] = "20090429190842";
		Bdependency.add(arr);
		ArrayList Cdependency = new ArrayList();
		String arr2[] = new String[2];
		arr2[0] = "PluginA.jar";
		arr2[1] = "20090429190842";
		String arr3[] = new String[2];
		arr3[0] = "PluginB.jar";
		arr3[1] = "20090429190854";
		Cdependency.add(arr2);
		Cdependency.add(arr3);
		PluginObject pluginA = new PluginObject("PluginA.jar", "65c3ecc1bbd7564f92545ffd2521f9d96509ca64", "20090429190842", "jars", "This is a description of Plugin A", null, true, false);
		PluginObject pluginB = new PluginObject("PluginB.jar", "9624fa93cbf7720c01c7ff97c28b00747b700de3", "20090429190854", "plugins", "This is a description of Plugin B", Bdependency, true, false);
		PluginObject pluginC = new PluginObject("PluginC.jar", "1a992dbc077ef84020d44a980c7992ba6c8edf3d", "20090429190854", "misc", "This is a description of Plugin C", Cdependency, true, false);
		existingPluginList.add(pluginA);
		existingPluginList.add(pluginB);
		existingPluginList.add(pluginC);
		//IJ.showMessage("My_Plugin","Hello world!");

		//plugin frame appears after data is generated... Thus UI is ready to use them
		PluginMgrUI pluginMgrUI = PluginMgrUI.getInstance();
		pluginMgrUI.setExistingPluginList(existingPluginList);
		//pluginMgrUI.setVisible(true);
		pluginMgrUI.show();
	}
	public ArrayList getExistingPluginList() {
		return existingPluginList;
	}
	public ArrayList getUpdatesPluginList() {
		return updatesPluginList;
	}
	public ArrayList getDownloadNameList() {
		return downloadNameList;
	}
}
