package fiji.managerUI;
import ij.plugin.frame.PlugInFrame;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JTabbedPane;
import javax.swing.ImageIcon;

import fiji.logic.Controller;

public class PluginManager extends PlugInFrame implements ActionListener {
	private DownloadUI frameDownloader = null;
	private TreeViewUI treeViewUI = null;
	private ListViewUI listViewUI = null;
	private UpdateUI updateUI = null;
	private Controller controller = null;
	//private String ... //current.txt and database.txt address tentatively......

	public PluginManager() {
		super("Plugin Manager");
		this.setSize(750,525);
		this.setLayout(null);
		//Container content = this.getContentPane();

		//initialize the data...
		controller = new Controller();

		//Tabbed pane of contents
		JTabbedPane tabbedPane = new JTabbedPane();
		treeViewUI = new TreeViewUI(this);
		//treeViewUI.setExistingPluginList...
		listViewUI = new ListViewUI(this);
		listViewUI.setExistingPluginList(controller.getExistingPluginList());
		updateUI = new UpdateUI(this);
		//updateUI.setUpdatesPluginList(controller.getUpdatesPluginList());
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
		
		this.show();
	}

	public void clickToDownloadUpdates() {
		//in later implementations, this should liase with Controller
		frameDownloader.setVisible(true);
		this.setEnabled(false);
	}

	public void clickBackToPluginManager() {
		//in later implementations, this should liase with Controller
		frameDownloader.setVisible(false);
		this.setEnabled(true);
	}
	
	public void clickToGenerateUpdates() {
		//show the download window
		frameDownloader.setVisible(true);
		this.setEnabled(false);
		//began download
		controller.generateUpdatesPluginList();
		updateUI.setUpdatesPluginList(controller.getUpdatesPluginList());
	}

	public void actionPerformed(ActionEvent e) {
		
	}

	/* Returns an ImageIcon, or null if the path was invalid. */
	protected ImageIcon createImageIcon(String path) {
		java.net.URL imgURL = PluginManager.class.getResource(path);
		if (imgURL != null) {
			return new ImageIcon(imgURL);
		} else {
			System.err.println("Couldn't find file: " + path);
			return null;
		}
	}
}
