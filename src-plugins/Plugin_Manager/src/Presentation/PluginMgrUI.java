package Presentation;
import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.frame.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Container;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JComponent;
import javax.swing.ImageIcon;

public class PluginMgrUI extends PlugInFrame implements ActionListener {
	private static PluginMgrUI pluginMgrUI = null;
	private DownloadUI frameDownloader = null;
	private TreeViewUI treeViewUI = null;
	private ListViewUI listViewUI = null;
	private UpdateUI updateUI = null;

	private PluginMgrUI() {
		super("Plugin Manager");
		this.setSize(750,525);
		this.setLayout(null);
		//Container content = this.getContentPane();

		//Tabbed pane of contents
		JTabbedPane tabbedPane = new JTabbedPane();
		treeViewUI = new TreeViewUI(this);
		listViewUI = new ListViewUI(this);
		updateUI = new UpdateUI(this);
		//JComponent downloadUI = new DownloadUI(); //attached to another JFrame
		tabbedPane.addTab("Tree", null, treeViewUI, "Tree view of plugins");
		tabbedPane.addTab("List", null, listViewUI, "List view of plugins");
		tabbedPane.addTab("Update", null, updateUI, "Get a list of updates");
		tabbedPane.setBounds(20, 35, 710, 425);

		//Buttons for OK and Cancel
		JButton btnOK = new JButton();
		btnOK.setBounds(490, 475, 110, 30);
		btnOK.setText("OK");
		btnOK.setToolTipText("Retain settings and exit Plugin Manager");
		JButton btnCancel = new JButton();
		btnCancel.setBounds(620, 475, 110, 30);
		btnCancel.setText("Cancel");
		btnCancel.setToolTipText("Exit Plugin Manager");

        //if status says there's a list to download...
    	frameDownloader = new DownloadUI(this);
    	//but don't show it yet...

		/*
		content.add(tabbedPane);
		content.add(btnOK);
		content.add(btnCancel);
		*/
		this.add(tabbedPane);
		this.add(btnOK);
		this.add(btnCancel);
	}
	public static PluginMgrUI getInstance() {
		if (pluginMgrUI == null) {
			pluginMgrUI = new PluginMgrUI();
		}
		return pluginMgrUI;
	}
	public void setExistingPluginList(ArrayList arr) {
		listViewUI.setExistingPluginList(arr);
	}
	public void setUpdatesPluginList(ArrayList arr) {
	}
	public void setDownloadNameList(ArrayList arr) {
	}
	public void showDownloadManager() {
		//in later implementations, this should feedback to Plugin Manager, which will
		//liase with SystemController
		frameDownloader.setVisible(true);
		this.setEnabled(false);
	}
	public void backToPluginManager() {
		frameDownloader.setVisible(false);
		this.setEnabled(true);
	}
	public void actionPerformed(ActionEvent e) {
		
	}
    /* Returns an ImageIcon, or null if the path was invalid. */
    protected ImageIcon createImageIcon(String path) {
        java.net.URL imgURL = PluginMgrUI.class.getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL);
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
    }

}
