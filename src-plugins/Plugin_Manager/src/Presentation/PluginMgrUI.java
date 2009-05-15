package Presentation;
import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.frame.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JFrame;
import java.awt.Container;
import javax.swing.JButton;
import javax.swing.JTabbedPane;
import javax.swing.JComponent;
import javax.swing.ImageIcon;

public class PluginMgrUI extends PlugInFrame implements ActionListener {
	private static PluginMgrUI pluginMgrUI = null;

	private PluginMgrUI() {
		super("Plugin Manager");
		this.setSize(750,525);
		this.setLayout(null);
		//Container content = this.getContentPane();

		//Tabbed pane of contents
		JTabbedPane tabbedPane = new JTabbedPane();
		TreeViewUI treeViewUI = new TreeViewUI();
		ListViewUI listViewUI = new ListViewUI();
		UpdateUI updateUI = new UpdateUI();
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

		/*
		content.add(tabbedPane);
		content.add(btnOK);
		content.add(btnCancel);
		*/
		this.add(tabbedPane);
		this.add(btnOK);
		this.add(btnCancel);
		//JComponent downloadUI = new DownloadUI(); //attached to another JFrame
	}
	public static PluginMgrUI getInstance() {
		if (pluginMgrUI == null) {
			pluginMgrUI = new PluginMgrUI();
		}
		return pluginMgrUI;
	}
	public static JFrame getDownloadManager(JComponent downloadUI) {
		return null;
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
