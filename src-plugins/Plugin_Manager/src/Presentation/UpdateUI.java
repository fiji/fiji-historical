package Presentation;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JTabbedPane;
import javax.swing.JButton;
import javax.swing.JCheckBox;
//import javax.swing.JLabel;
import javax.swing.table.*;
import java.awt.Dimension;
import java.util.ArrayList;
import Data.PluginObject;

class UpdateUI extends JPanel {
	public UpdateUI() {
		super();
		this.setLayout(null);

		/* Create the plugin table */
		PluginTableModel pluginModel = null;
		JTable table = new JTable(pluginModel = new PluginTableModel());
		table.setShowGrid(false);
		table.setIntercellSpacing(new Dimension(0,0));
		table.setRowHeight(table.getRowHeight() + 2);
		table.setPreferredScrollableViewportSize(new Dimension(370,320));
		table.setBounds(0, 0, 370, 320);
		table.setRequestFocusEnabled(false);

		TableColumn col1 = table.getColumnModel().getColumn(0);
		TableColumn col2 = table.getColumnModel().getColumn(1);
		TableColumn col3 = table.getColumnModel().getColumn(2);

		col1.setPreferredWidth(30);
		col1.setMinWidth(30);
		col1.setMaxWidth(30);
		col1.setResizable(false);

		col2.setPreferredWidth(220);
		col3.setPreferredWidth(120);

		/* Create scrollpane to hold the table */
		JScrollPane scrollpane = new JScrollPane(table);
		scrollpane.getViewport().setBackground(table.getBackground());
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

		JButton btnCheckUpdate = new JButton();
		btnCheckUpdate.setBounds(395, 350, 145, 30);
		btnCheckUpdate.setText("Check for Updates");
		btnCheckUpdate.setToolTipText("Retain settings and exit Plugin Manager");

		JButton btnDownload = new JButton();
		btnDownload.setBounds(555, 350, 145, 30);
		btnDownload.setText("Download Updates");
		btnDownload.setToolTipText("Retain settings and exit Plugin Manager");

		JCheckBox chkSelectAll = new JCheckBox();
		chkSelectAll.setBounds(10, 350, 120, 30);
		chkSelectAll.setText("Select All");

		this.add(scrollpane);
		this.add(tabbedPane);
		this.add(btnCheckUpdate);
		this.add(btnDownload);
		this.add(chkSelectAll);
	}
	public static void main(String args[]) {
	}
}