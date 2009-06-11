package fiji.pluginManager;
import ij.plugin.PlugIn;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ImageIcon;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

/*
 * Main User Interface
 */
public class PluginManager extends JFrame implements PlugIn, TableModelListener {
	private Controller controller;
	private List<PluginObject> viewList;
	private PluginDataReader pluginDataReader;
	private Installer installer;
	private String updateURL = "http://pacific.mpi-cbg.de/update/current.txt";
	private String updateLocal = "current.txt";
	private String dbURL = "...";
	private String dbLocal = "...";

	/* User Interface elements */
	private DownloadUI frameDownloader;
	private String[] arrViewingOptions = {
			"View all plugins",
			"View installed plugins only",
			"View uninstalled plugins only",
			"View up-to-date plugins only",
			"View update-able plugins only"
			};
	private JTextField txtSearch;
	private JComboBox comboBoxViewingOptions;
	private PluginTable table;
	private JLabel lblPluginSummary;
	private JTextPane txtPluginDetails;
	private JButton btnStart;
	private JButton btnOK;

	public PluginManager() {
		super("Plugin Manager");

		//Firstly, get information from local, existing plugins
		pluginDataReader = new PluginDataReader();
		if (!pluginDataReader.tempDemo) {
		//Get information from server to build on information
		pluginDataReader.buildFullPluginList(updateURL, updateLocal);
		}

		//initialize the data...
		controller = new Controller(pluginDataReader.getExistingPluginList());

		viewList = pluginDataReader.getExistingPluginList(); //initial view: All plugins
		setUpUserInterface();

		setVisible(true);
		
		pack();
	}

	private void setUpUserInterface() {
		/* Create textpane to hold the information and its scrollpane */
		txtPluginDetails = new JTextPane();
		txtPluginDetails.setEditable(false);
		txtPluginDetails.setPreferredSize(new Dimension(335,315));
		JScrollPane txtScrollpane = new JScrollPane(txtPluginDetails);
		txtScrollpane.getViewport().setBackground(txtPluginDetails.getBackground());
		txtScrollpane.setPreferredSize(new Dimension(335,315));

		/* Tabbed pane of plugin details to hold the textpane (w/ scrollpane) */
		JTabbedPane tabbedPane = new JTabbedPane();
		JPanel panelPluginDetails = new JPanel();
		panelPluginDetails.setLayout(new BorderLayout());
		panelPluginDetails.add(txtScrollpane, BorderLayout.CENTER);
		tabbedPane.addTab("Details", null, panelPluginDetails, "Individual Plugin information");
		tabbedPane.setPreferredSize(new Dimension(335,315));

		JPanel rightPanel = new JPanel();
		rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
		rightPanel.add(Box.createVerticalGlue());
		rightPanel.add(tabbedPane);
		rightPanel.add(Box.createRigidArea(new Dimension(0,25)));

		/* Create text search */
		JLabel lblSearch1 = new JLabel("Search:", SwingConstants.LEFT);
		txtSearch = new JTextField();

		JPanel searchPanel = new JPanel();
		searchPanel.setLayout(new BoxLayout(searchPanel, BoxLayout.X_AXIS));
		searchPanel.add(lblSearch1);
		searchPanel.add(Box.createRigidArea(new Dimension(10,0)));
		searchPanel.add(txtSearch);

		/* Create combo box of options */
		JLabel lblSearch2 = new JLabel("View Options:");
		comboBoxViewingOptions = new JComboBox(arrViewingOptions);
		comboBoxViewingOptions.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				comboBoxViewListener();
			}

		});
		JPanel comboBoxPanel = new JPanel();
		comboBoxPanel.setLayout(new BoxLayout(comboBoxPanel, BoxLayout.X_AXIS));
		comboBoxPanel.add(lblSearch2);
		comboBoxPanel.add(Box.createRigidArea(new Dimension(10,0)));
		comboBoxPanel.add(comboBoxViewingOptions);

		/* Create labels to annotate table */
		JLabel lblTable = new JLabel("Please choose what you want to install/uninstall:");
		JPanel lblTablePanel = new JPanel();
		lblTablePanel.add(lblTable);
		lblTablePanel.add(Box.createHorizontalGlue());
		lblTablePanel.setLayout(new BoxLayout(lblTablePanel, BoxLayout.X_AXIS));

		/* Label text for plugin summaries */
		lblPluginSummary = new JLabel();
		JPanel lblSummaryPanel = new JPanel();
		lblSummaryPanel.add(lblPluginSummary);
		lblSummaryPanel.add(Box.createHorizontalGlue());
		lblSummaryPanel.setLayout(new BoxLayout(lblSummaryPanel, BoxLayout.X_AXIS));

		/* Create the plugin table and set up its scrollpane */
		table = new PluginTable(viewList, this);
		JScrollPane pluginListScrollpane = new JScrollPane(table);
		pluginListScrollpane.getViewport().setBackground(table.getBackground());

		JPanel leftPanel = new JPanel();
		leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
		leftPanel.add(searchPanel);
		leftPanel.add(Box.createRigidArea(new Dimension(0,10)));
		leftPanel.add(comboBoxPanel);
		leftPanel.add(Box.createRigidArea(new Dimension(0,10)));
		leftPanel.add(lblTablePanel);
		leftPanel.add(Box.createRigidArea(new Dimension(0,5)));
		leftPanel.add(pluginListScrollpane);
		leftPanel.add(Box.createRigidArea(new Dimension(0,5)));
		leftPanel.add(lblSummaryPanel);

		JPanel topPanel = new JPanel();
		topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
		topPanel.add(leftPanel);
		topPanel.add(Box.createRigidArea(new Dimension(15,0)));
		topPanel.add(rightPanel);
		topPanel.setBorder(BorderFactory.createEmptyBorder(20, 15, 5, 15));

		//Buttons to start actions
		btnStart = new JButton();
		btnStart.setText("Apply changes");
		btnStart.setToolTipText("Start installing/uninstalling specified plugins");
		btnStart.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				clickToBeginOperations();
			}

		});
		btnStart.setEnabled(false);

		//Buttons to quit Plugin Manager
		btnOK = new JButton();
		btnOK.setText("Cancel");
		btnOK.setToolTipText("Exit Plugin Manager without applying changes");
		btnOK.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				clickToQuitPluginManager();
			}

		});

		JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));
		bottomPanel.add(btnStart);
		bottomPanel.add(Box.createHorizontalGlue());
		bottomPanel.add(btnOK);
		bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 15, 15, 15));

		getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
		getContentPane().add(topPanel);
		getContentPane().add(bottomPanel);
	}

	//Whenever Viewing Options in the ComboBox has been changed
	private void comboBoxViewListener() {
		int selectedIndex = comboBoxViewingOptions.getSelectedIndex();

		if (selectedIndex == 0) {

			//if "View all plugins"
			viewList = pluginDataReader.getExistingPluginList();

		} else if (selectedIndex == 1) {

			//if "View installed plugins"
			viewList = ((PluginCollection)pluginDataReader.getExistingPluginList()).getList(PluginCollection.FILTER_STATUS_ALREADYINSTALLED);

		} else if (selectedIndex == 2) {

			//if "View uninstalled plugins"
			viewList = ((PluginCollection)pluginDataReader.getExistingPluginList()).getList(PluginCollection.FILTER_STATUS_UNINSTALLED);

		} else if (selectedIndex == 3) {

			//if "View up-to-date plugins"
			viewList = ((PluginCollection)pluginDataReader.getExistingPluginList()).getList(PluginCollection.FILTER_STATUS_INSTALLED);

		} else if (selectedIndex == 4) {

			//if "View update-able plugins"
			viewList = ((PluginCollection)pluginDataReader.getExistingPluginList()).getList(PluginCollection.FILTER_STATUS_MAYUPDATE);

		} else {
			throw new Error("Viewing option specified does not exist!");
		}

		//Directly update the table for display
		table.setupTableModel(viewList);
	}

	private void clickToBeginOperations() {
		//in later implementations, this should liase with Controller
		//if status says there's a list to download...
		boolean tempDemo = pluginDataReader.tempDemo;
		if (!tempDemo) {
		frameDownloader = new DownloadUI(this);
		//Installer installer = new Installer(((PluginCollection)pluginList).getListWhereActionIsSpecified(), updateURL);
		frameDownloader.setVisible(true);
		installer = new Installer(pluginDataReader, updateURL);
		frameDownloader.setInstaller(installer);
		setEnabled(false);
		} else {
			//if just a demo
			List<PluginObject> pluginList = pluginDataReader.getExistingPluginList();
			List<PluginObject> changeList = ((PluginCollection)pluginList).getList(PluginCollection.FILTER_ACTIONS_SPECIFIED);
			List<PluginObject> change_addOrUpdateList = ((PluginCollection)changeList).getList(PluginCollection.FILTER_ACTIONS_ADDORUPDATE);
			List<PluginObject> change_removeList = ((PluginCollection)changeList).getList(PluginCollection.FILTER_ACTIONS_UNINSTALL);

			//Generates a map of plugins and their individual dependencies/dependents
			Map<PluginObject,List<PluginObject>> installDependenciesMap = new HashMap<PluginObject,List<PluginObject>>();
			Map<PluginObject,List<PluginObject>> updateDependenciesMap = new HashMap<PluginObject,List<PluginObject>>();
			Map<PluginObject,List<PluginObject>> uninstallDependentsMap = new HashMap<PluginObject,List<PluginObject>>();

			//Going through list requesting for ADD or UPDATE
			for (PluginObject myPlugin : change_addOrUpdateList) {
				//Generate lists of dependencies for each plugin
				List<PluginObject> toInstallList = new ArrayList<PluginObject>();
				List<PluginObject> toUpdateList = new ArrayList<PluginObject>();
				controller.addDependency(toInstallList, toUpdateList, myPlugin);
				installDependenciesMap.put(myPlugin, toInstallList);
				updateDependenciesMap.put(myPlugin, toUpdateList);
			}

			//Going through list requesting for REMOVE
			for (PluginObject myPlugin : change_removeList) {
				//Generate lists of dependents for each plugin
				List<PluginObject> toRemoveList = new ArrayList<PluginObject>();
				controller.removeDependent(toRemoveList, myPlugin);
				uninstallDependentsMap.put(myPlugin, toRemoveList);
			}

			//Compile a list of plugin names that conflicts with uninstalling (if any)
			List<String[]> installConflicts = new ArrayList<String[]>();
			List<String[]> updateConflicts = new ArrayList<String[]>();
			Iterator<PluginObject> iterInstall = installDependenciesMap.keySet().iterator();
			while (iterInstall.hasNext()) {
				PluginObject pluginAdd = iterInstall.next();
				List<PluginObject> toInstallList = installDependenciesMap.get(pluginAdd);
				List<PluginObject> toUpdateList = updateDependenciesMap.get(pluginAdd);
				Iterator<PluginObject> iterUninstall = uninstallDependentsMap.keySet().iterator();
				while (iterUninstall.hasNext()) {
					PluginObject pluginUninstall = iterUninstall.next();
					List<PluginObject> toUninstallList = uninstallDependentsMap.get(pluginUninstall);

					if (controller.conflicts(toInstallList, toUpdateList, toUninstallList)) {
						String installName = pluginAdd.getFilename();
						String uninstallName = pluginUninstall.getFilename();
						String[] arrNames = {installName, uninstallName};
						if (pluginAdd.isUpdateable()) {
							updateConflicts.add(arrNames);
						} else {
							installConflicts.add(arrNames);
						}
					}
				}
			}

			//Combines all the dependencies for individual plugins into one list
			List<PluginObject> toInstallList = new PluginCollection();
			List<PluginObject> toUpdateList = new PluginCollection();
			List<PluginObject> toRemoveList = new PluginCollection();
			controller.unifyInstallAndUpdateList(installDependenciesMap,
					updateDependenciesMap,
					toInstallList,
					toUpdateList);
			controller.unifyUninstallList(uninstallDependentsMap, toRemoveList);

			//Objective is to show user only information that was previously invisible
			toInstallList = ((PluginCollection)toInstallList).getList(PluginCollection.FILTER_UNLISTED_TO_INSTALL);
			toUpdateList = ((PluginCollection)toUpdateList).getList(PluginCollection.FILTER_UNLISTED_TO_UPDATE);
			toRemoveList = ((PluginCollection)toRemoveList).getList(PluginCollection.FILTER_UNLISTED_TO_UNINSTALL);

			//Actual display of information
			System.out.println("Output simulation");
			for (int i = 0; i < changeList.size(); i++) {
				//try filtering through the filtered (use "filter pattern" again)
				PluginObject myPlugin = changeList.get(i);
				String pluginName = myPlugin.getFilename();
				String pluginDescription = myPlugin.getDescription();
				String outputStr = (i+1) + ": " + pluginName + "; Description: " + pluginDescription + "; Action: ";
				if (myPlugin.isRemovableOnly()) {
					//obviously, if its in "changes" list, then action is uninstall
					outputStr += "To Uninstall";
				} else if (myPlugin.isInstallable()) {
					//obviously, if its in "changes" list, then action is install
					outputStr += "To install";
				} else if (myPlugin.isUpdateable() && myPlugin.toRemove()) {
					outputStr += "To Uninstall";
				} else if (myPlugin.toUpdate()) {
					outputStr += "To Update";
				}
				System.out.println(outputStr);
			}
			System.out.println("----------The following additional installations/removals are to be made----------");
			for (int i = 0; i < toInstallList.size(); i++) {
				PluginObject myPlugin = toInstallList.get(i);
				System.out.println("To be installed: " + myPlugin.getFilename() + ", " + myPlugin.getTimestamp());
			}
			for (int i = 0; i < toUpdateList.size(); i++) {
				PluginObject myPlugin = toUpdateList.get(i);
				System.out.println("To be updated: " + myPlugin.getFilename() + ", " + myPlugin.getNewTimestamp());
			}
			for (int i = 0; i < toRemoveList.size(); i++) {
				PluginObject myPlugin = toRemoveList.get(i);
				System.out.println("To be uninstalled: " + myPlugin.getFilename() + ", " + myPlugin.getTimestamp());
			}
			System.out.println("----------Conflicts are listed as follows:----------");
			for (int i = 0; i < installConflicts.size(); i++) {
				String[] names = installConflicts.get(i);
				System.out.println("Installing " + names[0] + " would conflict with uninstalling " + names[1]);
			}
			for (int i = 0; i < updateConflicts.size(); i++) {
				String[] names = updateConflicts.get(i);
				System.out.println("Updating " + names[0] + " would conflict with uninstalling " + names[1]);
			}
			System.out.println("End Output Simulation");
			//Supposedly, if no conflicts are found (size == 0), upon confirmation,
			//program will start download, using both "changeList" and "toInstallList" and "toUpdateList" and "toRemoveList"
		}
	}

	private void clickToQuitPluginManager() {
		//in later implementations, this should have some notifications
		dispose();
	}

	public void clickBackToPluginManager() {
		//in later implementations, this should liase with Controller
		//e.g: controller.hasDownloadEnded()... Controller checks download complete or not...
		frameDownloader.setVisible(false);
		setEnabled(true);
	}

	public void displayPluginDetails(PluginObject myPlugin) {
		txtPluginDetails.setText("");
		try {
			TextPaneFormat.insertText(txtPluginDetails, myPlugin.getFilename(), TextPaneFormat.BOLD_BLACK_TITLE);
			if (myPlugin.isUpdateable())
				TextPaneFormat.insertText(txtPluginDetails, "\n(Update is available)", TextPaneFormat.ITALIC_BLACK);
			TextPaneFormat.insertText(txtPluginDetails, "\n\nMd5 Sum", TextPaneFormat.BOLD_BLACK);
			TextPaneFormat.insertText(txtPluginDetails, "\n" + myPlugin.getmd5Sum());
			TextPaneFormat.insertText(txtPluginDetails, "\n\nLast Modified: ", TextPaneFormat.BOLD_BLACK);
			TextPaneFormat.insertText(txtPluginDetails, "" + myPlugin.getTimestamp());
			TextPaneFormat.insertText(txtPluginDetails, "\n\nDependency", TextPaneFormat.BOLD_BLACK);
			ArrayList<Dependency> myDependencies = (ArrayList<Dependency>) myPlugin.getDependencies();
			TextPaneFormat.insertDependenciesList(txtPluginDetails, myDependencies);
			TextPaneFormat.insertText(txtPluginDetails, "\n\nDescription", TextPaneFormat.BOLD_BLACK);
			String strDescription = "";
			if (myPlugin.getDescription() == null || myPlugin.getDescription().trim().equals("")) {
				strDescription = "None";
			} else
				strDescription = myPlugin.getDescription();
			TextPaneFormat.insertText(txtPluginDetails, "\n" + strDescription);
			if (myPlugin.isUpdateable()) {
				TextPaneFormat.insertText(txtPluginDetails, "\n\nUpdate Details", TextPaneFormat.BOLD_BLACK_TITLE);
				TextPaneFormat.insertText(txtPluginDetails, "\n\nNew Md5 Sum", TextPaneFormat.BOLD_BLACK);
				TextPaneFormat.insertText(txtPluginDetails, "\n" + myPlugin.getNewMd5Sum());
				TextPaneFormat.insertText(txtPluginDetails, "\n\nReleased: ", TextPaneFormat.BOLD_BLACK);
				TextPaneFormat.insertText(txtPluginDetails, "" + myPlugin.getNewTimestamp());
				TextPaneFormat.insertText(txtPluginDetails, "\n\nDependency", TextPaneFormat.BOLD_BLACK);
				ArrayList<Dependency> myNewDependencies = (ArrayList<Dependency>) myPlugin.getNewDependencies();
				TextPaneFormat.insertDependenciesList(txtPluginDetails, myNewDependencies);
			}
			//ensure first line of text is always shown (i.e.: scrolled to top)
			txtPluginDetails.setSelectionStart(0);
			txtPluginDetails.setSelectionEnd(0);
		} catch (BadLocationException e) {
			throw new Error("Problem with printing Plugin information: " + e.getMessage());
		}
	}

	public void tableChanged(TableModelEvent e) {
		//QUESTION: should we count objects in the view-table or objects in the entire list?
		int size = 0;
		int installCount = 0;
		int removeCount = 0;
		int updateCount = 0;

		size = viewList.size();
		for (PluginObject myPlugin : viewList) {
			if (myPlugin.toInstall()) {
				installCount += 1;
			} else if (myPlugin.toRemove()) {
				removeCount += 1;
			} else if (myPlugin.toUpdate()) {
				updateCount += 1;
			}
		}

		lblPluginSummary.setText("Total: " + size + ", To install: " + installCount +
				", To remove: " + removeCount + ", To update: " + updateCount);
		if (btnStart != null) {
			List<PluginObject> myList = ((PluginCollection)pluginDataReader.getExistingPluginList()).getList(PluginCollection.FILTER_ACTIONS_SPECIFIED);
			if (myList.size() > 0)
				btnStart.setEnabled(true);
			else
				btnStart.setEnabled(false);
		}
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

	@Override
	public void run(String arg0) {
		// TODO Auto-generated method stub
		
	}
}

class TextPaneFormat {
	public static SimpleAttributeSet ITALIC_BLACK = new SimpleAttributeSet();
	public static SimpleAttributeSet BOLD_BLACK = new SimpleAttributeSet();
	public static SimpleAttributeSet BLACK = new SimpleAttributeSet();
	public static SimpleAttributeSet BOLD_BLACK_TITLE = new SimpleAttributeSet();

	static {
		StyleConstants.setForeground(ITALIC_BLACK, Color.black);
		StyleConstants.setItalic(ITALIC_BLACK, true);
		StyleConstants.setFontFamily(ITALIC_BLACK, "Verdana");
		StyleConstants.setFontSize(ITALIC_BLACK, 12);

		StyleConstants.setForeground(BOLD_BLACK, Color.black);
		StyleConstants.setBold(BOLD_BLACK, true);
		StyleConstants.setFontFamily(BOLD_BLACK, "Verdana");
		StyleConstants.setFontSize(BOLD_BLACK, 12);

		StyleConstants.setForeground(BLACK, Color.black);
		StyleConstants.setFontFamily(BLACK, "Verdana");
		StyleConstants.setFontSize(BLACK, 12);

		StyleConstants.setForeground(BOLD_BLACK_TITLE, Color.black);
		//StyleConstants.setBold(BOLD_BLACK_TITLE, true);
		StyleConstants.setFontFamily(BOLD_BLACK_TITLE, "Impact");
		StyleConstants.setFontSize(BOLD_BLACK_TITLE, 18);
	}

	public static void insertText(JTextPane textPane, String text, AttributeSet set)
	throws BadLocationException {
		textPane.getDocument().insertString(textPane.getDocument().getLength(), text, set);
	}

	public static void insertText(JTextPane textPane, String text)
	throws BadLocationException {
		textPane.getDocument().insertString(textPane.getDocument().getLength(), text, TextPaneFormat.BLACK);
	}

	public static void insertDependenciesList(JTextPane textPane, List<Dependency> dependencyList)
	throws BadLocationException {
		String strDependencies = "";
		if (dependencyList != null) {
			int noOfDependencies = dependencyList.size();
			for (int i = 0; i < noOfDependencies; i++) {
				Dependency dependency = dependencyList.get(i);
				strDependencies +=  dependency.getFilename() + " (" + dependency.getTimestamp() + ")";
				if (i != noOfDependencies -1 && noOfDependencies != 1) //if last index
					strDependencies += ",\n";
			}
			if (strDependencies.equals("")) strDependencies = "None";
		} else {
			strDependencies = "None";
		}
		insertText(textPane, "\n" + strDependencies);
	}
}