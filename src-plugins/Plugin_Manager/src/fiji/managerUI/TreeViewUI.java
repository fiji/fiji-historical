package fiji.managerUI;

import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.*;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JTabbedPane;

import fiji.data.PluginObject;
//import javax.swing.JCheckBox;
//import javax.swing.JLabel;
import java.util.ArrayList;

class TreeViewUI extends JPanel {
	private PluginManager pluginManager = null;
	public TreeViewUI(PluginManager pluginManager) {
		super();
		this.setLayout(null);
		this.pluginManager = pluginManager;

		/* Create the tree */
		DefaultMutableTreeNode top =
            new DefaultMutableTreeNode("Fiji");

		//Create a tree that allows one selection at a time.
        JTree tree = new JTree(top);

        tree.getSelectionModel().setSelectionMode
        (TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setBounds(0, 0, 370, 320);

		/* Create scrollpane to hold the tree */
		JScrollPane scrollpane = new JScrollPane(tree);
		scrollpane.getViewport().setBackground(tree.getBackground());
		scrollpane.setBounds(10, 20, 370, 320);

		/* Create textpane to hold the information */
		JTextPane txtPluginDetails = new JTextPane();
		txtPluginDetails.setText("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\nhahaha!");
		txtPluginDetails.setBounds(0, 0, 290, 285);
		
		/* Create scrollpane to hold the textpane */
		JScrollPane txtScrollpane = new JScrollPane(txtPluginDetails);
		txtScrollpane.getViewport().setBackground(txtPluginDetails.getBackground());
		txtScrollpane.setBounds(5, 5, 290, 285);

		/* Tabbed pane of plugin details to hold the textpane (w/ scrollpane) */
		JTabbedPane tabbedPane = new JTabbedPane();
		JPanel panelPluginDetails = new JPanel();
		panelPluginDetails.setLayout(null);
		panelPluginDetails.setBounds(0, 0, 305, 325);
		panelPluginDetails.add(txtScrollpane);
		tabbedPane.addTab("Details", null, panelPluginDetails, "Individual Plugin information");
		tabbedPane.setBounds(395, 15, 305, 325);

		/* Dependency-checking Checkbox and label */
		/*JCheckBox checkboxInformUser = new JCheckBox();*/

		this.add(scrollpane);
		this.add(tabbedPane);
	}
}