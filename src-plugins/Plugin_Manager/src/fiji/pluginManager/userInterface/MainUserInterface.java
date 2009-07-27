package fiji.pluginManager.userInterface;
import fiji.pluginManager.logic.DependencyCompiler;
import fiji.pluginManager.logic.Installer;
import fiji.pluginManager.logic.PluginCollection;
import fiji.pluginManager.logic.PluginManager;
import fiji.pluginManager.logic.PluginObject;
import ij.IJ;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.text.BadLocationException;

/*
 * Main User Interface, where the user chooses his options...
 */
public class MainUserInterface extends JFrame implements TableModelListener {
	private PluginManager pluginManager;
	private List<PluginObject> pluginCollection;
	private List<PluginObject> viewList;

	//User Interface elements
	private boolean isDeveloper;
	private JFrame loadedFrame;
	private String[] arrViewingOptions;
	private JTextField txtSearch;
	private JComboBox comboBoxViewingOptions;
	private PluginTable table;
	private JLabel lblPluginSummary;
	private JTextPane txtPluginDetails;
	private PluginObject currentPlugin;
	private JButton btnStart;
	private JButton btnOK;

	//For developers
	private JButton btnUpload;
	private JButton btnEditDescriptions;

	public MainUserInterface(PluginManager pluginManager, boolean isDeveloper) {
		super("Plugin Manager");
		this.isDeveloper = isDeveloper;
		this.pluginManager = pluginManager;

		//Pulls required information from pluginManager
		pluginCollection = pluginManager.pluginCollection;
		viewList = pluginCollection; //initially, view all
		List<PluginObject> readOnlyList = pluginManager.readOnlyList;
		if (readOnlyList.size() > 0) {
			String namelist = "";
			for (int i = 0; i < readOnlyList.size(); i++) {
				if (i != 0 && i % 3 == 0)
					namelist += "\n";
				namelist += readOnlyList.get(i).getFilename();
				if (i < readOnlyList.size() -1)
					namelist += ", ";
			}
			IJ.showMessage("Read-Only Plugins", "WARNING: The following plugin files are set to read-only, you are advised to quit Fiji and set to writable:\n" + namelist);
		}
		setUpUserInterface();
		pack();
	}

	private void setUpUserInterface() {
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		//======== Start: LEFT PANEL ========
		//Create text search
		JLabel lblSearch1 = new JLabel("Search:", SwingConstants.LEFT);
		txtSearch = new JTextField();
		txtSearch.getDocument().addDocumentListener(new DocumentListener() {

			public void changedUpdate(DocumentEvent e) {
				changeListingListener();
			}

			public void removeUpdate(DocumentEvent e) {
				changeListingListener();
			}

			public void insertUpdate(DocumentEvent e) {
				changeListingListener();
			}

		});

		JPanel searchPanel = new JPanel();
		searchPanel.setLayout(new BoxLayout(searchPanel, BoxLayout.X_AXIS));
		searchPanel.add(lblSearch1);
		searchPanel.add(Box.createRigidArea(new Dimension(10,0)));
		searchPanel.add(txtSearch);

		//Create combo box of options
		JLabel lblSearch2 = new JLabel("View Options:");
		arrViewingOptions = new String[] {
				"View all plugins",
				"View installed plugins only",
				"View uninstalled plugins only",
				"View up-to-date plugins only",
				"View update-able plugins only",
				"View Fiji plugins only",
				"View Non-Fiji plugins only"
		};
		comboBoxViewingOptions = new JComboBox(arrViewingOptions);
		comboBoxViewingOptions.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				changeListingListener();
			}

		});
		JPanel comboBoxPanel = new JPanel();
		comboBoxPanel.setLayout(new BoxLayout(comboBoxPanel, BoxLayout.X_AXIS));
		comboBoxPanel.add(lblSearch2);
		comboBoxPanel.add(Box.createRigidArea(new Dimension(10,0)));
		comboBoxPanel.add(comboBoxViewingOptions);

		//Create labels to annotate table
		JLabel lblTable = new JLabel("Please choose what you want to install/uninstall:");
		JPanel lblTablePanel = new JPanel();
		lblTablePanel.add(lblTable);
		lblTablePanel.add(Box.createHorizontalGlue());
		lblTablePanel.setLayout(new BoxLayout(lblTablePanel, BoxLayout.X_AXIS));

		//Label text for plugin summaries
		lblPluginSummary = new JLabel();
		JPanel lblSummaryPanel = new JPanel();
		lblSummaryPanel.add(lblPluginSummary);
		lblSummaryPanel.add(Box.createHorizontalGlue());
		lblSummaryPanel.setLayout(new BoxLayout(lblSummaryPanel, BoxLayout.X_AXIS));

		//Create the plugin table and set up its scrollpane
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
		//======== End: LEFT PANEL ========

		//======== Start: RIGHT PANEL ========
		//Create textpane to hold the information and its scrollpane
		txtPluginDetails = new TextPaneDisplay();
		txtPluginDetails.setPreferredSize(new Dimension(350,315));
		JScrollPane txtScrollpane = new JScrollPane(txtPluginDetails);
		txtScrollpane.getViewport().setBackground(txtPluginDetails.getBackground());
		txtScrollpane.setPreferredSize(new Dimension(350,315));

		//Tabbed pane of plugin details to hold the textpane (w/ scrollpane)
		JTabbedPane tabbedPane = new JTabbedPane();
		JPanel panelPluginDetails = new JPanel();
		panelPluginDetails.setLayout(new BorderLayout());
		panelPluginDetails.add(txtScrollpane, BorderLayout.CENTER);
		tabbedPane.addTab("Details", null, panelPluginDetails, "Individual Plugin information");
		tabbedPane.setPreferredSize(new Dimension(350,315));

		JPanel rightPanel = new JPanel();
		rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
		rightPanel.add(Box.createVerticalGlue());

		if (isDeveloper) {
			JPanel editButtonPanel = new JPanel();
			editButtonPanel.setLayout(new BoxLayout(editButtonPanel, BoxLayout.X_AXIS));
			btnEditDescriptions = new JButton("Edit Plugin Details");
			btnEditDescriptions.setToolTipText("Edit the details of selected plugin");
			btnEditDescriptions.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					clickToEditDescriptions();
				}

			});
			btnEditDescriptions.setEnabled(true);
			editButtonPanel.add(Box.createHorizontalGlue());
			editButtonPanel.add(btnEditDescriptions);
			rightPanel.add(editButtonPanel);
		}
		rightPanel.add(tabbedPane);
		rightPanel.add(Box.createRigidArea(new Dimension(0,25)));
		//======== End: RIGHT PANEL ========

		//======== Start: TOP PANEL (LEFT + RIGHT) ========
		JPanel topPanel = new JPanel();
		topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
		topPanel.add(leftPanel);
		topPanel.add(Box.createRigidArea(new Dimension(15,0)));
		topPanel.add(rightPanel);
		topPanel.setBorder(BorderFactory.createEmptyBorder(20, 15, 5, 15));

		//Button to start actions
		btnStart = new JButton("Apply changes");
		btnStart.setToolTipText("Start installing/uninstalling specified plugins");
		btnStart.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				clickToBeginOperations();
			}

		});
		btnStart.setEnabled(false);

		//includes button to upload to server if is a Developer using
		if (isDeveloper) {
			btnUpload = new JButton("Upload to server");
			btnUpload.setToolTipText("Upload the selected plugins to server");
			btnUpload.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					clickToUploadRecords();
				}

			});
			btnUpload.setEnabled(false);
		}

		//Button to quit Plugin Manager
		btnOK = new JButton("Cancel");
		btnOK.setToolTipText("Exit Plugin Manager without applying changes");
		btnOK.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				clickToQuitPluginManager();
			}

		});
		//======== End: TOP PANEL (LEFT + RIGHT) ========

		//======== Start: BOTTOM PANEL ========
		JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));
		bottomPanel.add(btnStart);
		if (isDeveloper) {
			bottomPanel.add(Box.createRigidArea(new Dimension(15,0)));
			bottomPanel.add(btnUpload);
		}
		bottomPanel.add(Box.createHorizontalGlue());
		bottomPanel.add(btnOK);
		bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 15, 15, 15));
		//======== End: BOTTOM PANEL ========

		getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
		getContentPane().add(topPanel);
		getContentPane().add(bottomPanel);

		//initial selection
		table.changeSelection(0, 0, false, false);
	}

	//Whenever search text or ComboBox has been changed
	private void changeListingListener() {
		viewList = pluginCollection;
		if (!txtSearch.getText().trim().isEmpty())
			viewList = ((PluginCollection)pluginCollection).getList(PluginCollection.getFilterForText(txtSearch.getText().trim()));

		PluginCollection.Filter viewFilter = getCorrespondingFilter(comboBoxViewingOptions.getSelectedIndex());
		if (viewFilter != null) {
			viewList = ((PluginCollection)viewList).getList(viewFilter);
		}
		//Directly update the table for display
		table.setupTableModel(viewList);
	}

	private PluginCollection.Filter getCorrespondingFilter(int selectedIndex) {
		switch (selectedIndex) {
		case 0:
			return null; //no filter needed
		case 1:
			return PluginCollection.FILTER_STATUS_ALREADYINSTALLED;
		case 2:
			return PluginCollection.FILTER_STATUS_UNINSTALLED;
		case 3:
			return PluginCollection.FILTER_STATUS_INSTALLED;
		case 4:
			return PluginCollection.FILTER_STATUS_MAYUPDATE;
		case 5:
			return PluginCollection.FILTER_FIJI;
		case 6:
			return PluginCollection.FILTER_NOT_FIJI;
		}
		throw new Error("Viewing Option specified does not exist!");
	}

	private void clickToUploadRecords() {
		//There's no frame interface for Uploader, makes disabling pointless, thus set invisible
		Uploader uploader = new Uploader(this);
		setVisible(false);
		uploader.setUploadInformationAndStart(pluginManager);
	}

	private void clickToEditDescriptions() {
		loadedFrame = new FrameDetailsEditor(this, currentPlugin);
		loadedFrame.setVisible(true);
		setEnabled(false);
	}

	private void clickToBeginOperations() {
		loadedFrame = new FrameConfirmation(this);
		FrameConfirmation frameConfirmation = (FrameConfirmation)loadedFrame;
		frameConfirmation.displayInformation(new DependencyCompiler(pluginCollection));
		loadedFrame.setVisible(true);
		setEnabled(false);
	}

	private void clickToQuitPluginManager() {
		//if there exists plugins where actions have been specified by user
		if (((PluginCollection)pluginCollection).getList(PluginCollection.FILTER_ACTIONS_SPECIFIED).size() > 0) {
			if (JOptionPane.showConfirmDialog(this,
					"You have specified changes. Are you sure you want to quit?",
					"Quit?", JOptionPane.YES_NO_OPTION,
					JOptionPane.WARNING_MESSAGE) == JOptionPane.NO_OPTION) {
				return;
			}
		}
		dispose();
	}

	public void openDownloader() {
		backToPluginManager();
		loadedFrame = new FrameInstaller(this);
		loadedFrame.setVisible(true);
		FrameInstaller frameInstaller = (FrameInstaller)loadedFrame;
		frameInstaller.setInstaller(new Installer(pluginCollection));
		setEnabled(false);
	}

	public void backToPluginManager() {
		removeLoadedFrameIfExists();
		setEnabled(true);
		setVisible(true);
	}

	public void exitWithRestartFijiMessage() {
		removeLoadedFrameIfExists();
		IJ.showMessage("Restart Fiji", "You need to restart Fiji application for the Plugin status changes to take effect.");
		dispose();
	}

	public void exitWithRestartMessage(String title, String message) {
		IJ.showMessage(title, message);
		dispose();
	}

	private void removeLoadedFrameIfExists() {
		if (loadedFrame != null) {
			loadedFrame.setVisible(false);
			loadedFrame.dispose();
			loadedFrame = null;
		}
	}

	public void displayPluginDetails(PluginObject currentPlugin) {
		this.currentPlugin = currentPlugin;
		try {
			if (txtPluginDetails != null)
				((TextPaneDisplay)txtPluginDetails).showPluginDetails(currentPlugin);
		} catch (BadLocationException e) {
			throw new Error("Problem with printing Plugin information: " + e.getMessage());
		}
	}

	public void tableChanged(TableModelEvent e) {
		//TODO: should we count objects in the view-table or objects in the entire list?
		int size = viewList.size();
		int installCount = 0;
		int removeCount = 0;
		int updateCount = 0;
		int uploadCount = 0;

		for (PluginObject myPlugin : viewList) {
			if (myPlugin.toInstall()) {
				installCount += 1;
			} else if (myPlugin.toRemove()) {
				removeCount += 1;
			} else if (myPlugin.toUpdate()) {
				updateCount += 1;
			} else if (isDeveloper && myPlugin.toUpload()) {
				uploadCount += 1;
			}
		}
		String txtAction = "Total: " + size + ", To install: " + installCount +
		", To remove: " + removeCount + ", To update: " + updateCount;
		if (isDeveloper) txtAction += ", To upload: " + uploadCount;

		lblPluginSummary.setText(txtAction);
		enableButtonIfAnyActions(btnStart, PluginCollection.FILTER_ACTIONS_SPECIFIED_NOT_UPLOAD);
		enableButtonIfAnyActions(btnUpload, PluginCollection.FILTER_ACTIONS_UPLOAD);
	}

	private void enableButtonIfAnyActions(JButton button, PluginCollection.Filter filter) {
		if (button != null) {
			List<PluginObject> myList = ((PluginCollection)pluginCollection).getList(filter);
			if (myList.size() > 0)
				button.setEnabled(true);
			else
				button.setEnabled(false);
		}
	}

	public boolean isDeveloper() {
		return isDeveloper;
	}

}