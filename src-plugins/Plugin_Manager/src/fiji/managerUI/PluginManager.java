package fiji.managerUI;
import ij.plugin.frame.PlugInFrame;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultCellEditor;
import javax.swing.JTextField;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ImageIcon;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import extendedSwing.JTableX;
import extendedSwing.RowEditorModel;
import fiji.data.Dependency;
import fiji.data.PluginObject;
import fiji.logic.Controller;

public class PluginManager extends PlugInFrame implements ActionListener, TableModelListener {
	private Controller controller = null;

	/* User Interface elements */
	private DownloadUI frameDownloader = null;
	private String[] arrViewingOptions = {
			"View all plugins",
			"View installed plugins only",
			"View uninstalled plugins only",
			"View up-to-date plugins only",
			"View update-able plugins only"
			};
	private JTextField txtSearch = null;
	private JComboBox comboBoxViewingOptions = null;
	private PluginTableModel pluginTableModel = null;
	private JTableX table = null;
	private JLabel lblPluginSummary = null;
	private JTextPane txtPluginDetails = null;
	private JButton btnStart = null;
	private JButton btnOK = null;
	static String[] arrUninstalledOptions = { "Not installed", "Install it" };
	static String[] arrInstalledOptions = { "Installed", "Remove it" };
	static String[] arrUpdateableOptions = { "Installed", "Remove it", "Update it" };
	private DefaultCellEditor uninstalledOptions = new DefaultCellEditor(new JComboBox(arrUninstalledOptions));
	private DefaultCellEditor installedOptions = new DefaultCellEditor(new JComboBox(arrInstalledOptions));
	private DefaultCellEditor updateableOptions = new DefaultCellEditor(new JComboBox(arrUpdateableOptions));
	private RowEditorModel rowEditorModel = null;

	private String updateURL = "http://pacific.mpi-cbg.de/update/current.txt";
	//private String ... //current.txt and database.txt address tentatively......

	public PluginManager() {
		super("Plugin Manager");
		this.setSize(750,540);
		this.setLayout(null);
		//Container content = this.getContentPane();

		//initialize the data...
		controller = new Controller(updateURL);

		//if status says there's a list to download...
		frameDownloader = new DownloadUI(this); //but don't show it yet...

		setUpUserInterface();

		//Retrieves the data
		pluginTableModel.update(controller.getPluginList());

		//after displaying UI and data ready for display, allow editable ComboBoxes
		setupPluginComboBoxes();

		//this.setVisible(true);
		this.show();
	}

	private void setUpUserInterface() {
		/* Create labels to annotate search options */
		JLabel lblSearch1 = new JLabel("Search:");
		lblSearch1.setBounds(30, 45, 120, 25);
		lblSearch1.setAlignmentX(TOP_ALIGNMENT);
		lblSearch1.setAlignmentY(LEFT_ALIGNMENT);
		JLabel lblSearch2 = new JLabel("View Options:");
		lblSearch2.setBounds(30, 85, 120, 25);
		lblSearch2.setAlignmentX(TOP_ALIGNMENT);
		lblSearch2.setAlignmentY(LEFT_ALIGNMENT);

		/* Create text search */
		txtSearch = new JTextField();
		txtSearch.setBounds(150, 45, 230, 25);

		/* Create combo box of options */
		comboBoxViewingOptions = new JComboBox(arrViewingOptions);
		comboBoxViewingOptions.setBounds(150, 85, 230, 25);
		comboBoxViewingOptions.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				comboBoxViewListener();
			}

		});
		
		/* Create labels to annotate table */
		JLabel lblTable = new JLabel("Please choose what you want to install/uninstall:");
		lblTable.setBounds(30, 120, 350, 30);
		lblTable.setAlignmentX(TOP_ALIGNMENT);
		lblTable.setAlignmentY(LEFT_ALIGNMENT);

		/* Create the plugin table */
		table = new JTableX(pluginTableModel = new PluginTableModel());
		table.setColumnSelectionAllowed(false);
		table.setRowSelectionAllowed(true);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

			public void valueChanged(ListSelectionEvent event) {
				int viewRow = table.getSelectedRow();
				if (viewRow < 0) {
					//Selection got filtered away
				} else {
					int modelRow = table.convertRowIndexToModel(viewRow);
					PluginObject myPlugin = pluginTableModel.getEntry(modelRow);
					txtPluginDetails.setText("");
					TextPaneFormat.insertText(txtPluginDetails, myPlugin.getFilename(), TextPaneFormat.BOLD_BLACK_TITLE);
					if (myPlugin.getStatus() == PluginObject.STATUS_MAY_UPDATE)
						TextPaneFormat.insertText(txtPluginDetails, "\n(Update is available)", TextPaneFormat.BOLD_BLACK);
					TextPaneFormat.insertText(txtPluginDetails, "\n\nMd5 Sum", TextPaneFormat.BOLD_BLACK);
					TextPaneFormat.insertText(txtPluginDetails, "\n" + myPlugin.getmd5Sum(), TextPaneFormat.BLACK);
					TextPaneFormat.insertText(txtPluginDetails, "\n\nReleased: ", TextPaneFormat.BOLD_BLACK);
					TextPaneFormat.insertText(txtPluginDetails, "" + myPlugin.getTimestamp(), TextPaneFormat.BLACK);
					TextPaneFormat.insertText(txtPluginDetails, "\n\nDependency", TextPaneFormat.BOLD_BLACK);
					ArrayList<Dependency> myDependencies = (ArrayList<Dependency>) myPlugin.getDependencies();
					String strDependencies = "";
					if (myDependencies != null) {
						int noOfDependencies = myDependencies.size();
						for (int i = 0; i < noOfDependencies; i++) {
							Dependency dependency = myDependencies.get(i);
							strDependencies +=  dependency.getFilename() + " (" + dependency.getTimestamp() + ")";
							if (i != noOfDependencies -1 && noOfDependencies != 1) //if last index
								strDependencies += ",\n";
						}
						if (strDependencies.equals("")) strDependencies = "None";
					} else {
						strDependencies = "None";
					}
					TextPaneFormat.insertText(txtPluginDetails, "\n" + strDependencies, TextPaneFormat.BLACK);
					TextPaneFormat.insertText(txtPluginDetails, "\n\nDescription", TextPaneFormat.BOLD_BLACK);
					String strDescription = "";
					if (myPlugin.getDescription() == null || myPlugin.getDescription().trim().equals("")) {
						strDescription = "None";
					} else
						strDescription = myPlugin.getDescription();
					TextPaneFormat.insertText(txtPluginDetails, "\n" + strDescription, TextPaneFormat.BLACK);
					if (myPlugin.getStatus() == PluginObject.STATUS_MAY_UPDATE) {
						TextPaneFormat.insertText(txtPluginDetails, "\n\nUpdate Details", TextPaneFormat.BOLD_BLACK_TITLE);
						TextPaneFormat.insertText(txtPluginDetails, "\n\nNew Md5 Sum", TextPaneFormat.BOLD_BLACK);
						TextPaneFormat.insertText(txtPluginDetails, "\n" + myPlugin.getmd5Sum(), TextPaneFormat.BLACK);
						TextPaneFormat.insertText(txtPluginDetails, "\n\nReleased: ", TextPaneFormat.BOLD_BLACK);
						TextPaneFormat.insertText(txtPluginDetails, "" + myPlugin.getTimestamp(), TextPaneFormat.BLACK);
					}
				}
			}

		});
		table.getModel().addTableModelListener(this); //listen for changes (tableChanged(TableModelEvent e))

		//Set appearance of table
		table.setShowGrid(false);
		table.setIntercellSpacing(new Dimension(0,0));
		table.setRowHeight(table.getRowHeight() + 2);
		table.setPreferredScrollableViewportSize(new Dimension(370,310));
		table.setBounds(0, 0, 370, 310);
		table.setRequestFocusEnabled(false);

		TableColumn col1 = table.getColumnModel().getColumn(0);
		TableColumn col2 = table.getColumnModel().getColumn(1);

		col1.setPreferredWidth(250);
		col1.setMinWidth(250);
		col1.setMaxWidth(250);
		col1.setResizable(false);
		col2.setPreferredWidth(120);
		col2.setMinWidth(120);
		col2.setMaxWidth(120);
		col2.setResizable(false);
		
		/* create the scrollpane that holds the table */
		JScrollPane pluginListScrollpane = new JScrollPane(table);
		pluginListScrollpane.getViewport().setBackground(table.getBackground());
		pluginListScrollpane.setBounds(30, 150, 370, 310);

		/* Label text for plugin summaries */
		lblPluginSummary = new JLabel();
		lblPluginSummary.setBounds(30, 460, 350, 30);
		lblPluginSummary.setAlignmentX(TOP_ALIGNMENT);
		lblPluginSummary.setAlignmentY(LEFT_ALIGNMENT);

		/* Create textpane to hold the information */
		txtPluginDetails = new JTextPane();
		txtPluginDetails.setEditable(false);
		txtPluginDetails.setText("");
		txtPluginDetails.setBounds(0, 0, 290, 275);

		/* Create scrollpane to hold the textpane */
		JScrollPane txtScrollpane = new JScrollPane(txtPluginDetails);
		txtScrollpane.getViewport().setBackground(txtPluginDetails.getBackground());
		txtScrollpane.setBounds(5, 5, 290, 275);

		/* Tabbed pane of plugin details to hold the textpane (w/ scrollpane) */
		JTabbedPane tabbedPane = new JTabbedPane();
		JPanel panelPluginDetails = new JPanel();
		panelPluginDetails.setLayout(null);
		panelPluginDetails.setBounds(0, 0, 305, 315);
		panelPluginDetails.add(txtScrollpane);
		tabbedPane.addTab("Details", null, panelPluginDetails, "Individual Plugin information");
		tabbedPane.setBounds(420, 145, 305, 315);

		//Buttons to start actions
		btnStart = new JButton();
		btnStart.setBounds(30, 490, 110, 30);
		btnStart.setText("Start");
		btnStart.setToolTipText("Start installing/uninstalling specified plugins");
		btnStart.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				clickToBeginOperations();
			}

		});

		//Buttons to quit Plugin Manager
		btnOK = new JButton();
		btnOK.setBounds(610, 490, 110, 30);
		btnOK.setText("OK");
		btnOK.setToolTipText("Exit Plugin Manager");
		btnOK.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				clickToQuitPluginManager();
			}

		});

		this.add(lblSearch1);
		this.add(lblSearch2);
		this.add(txtSearch);
		this.add(comboBoxViewingOptions);
		this.add(lblTable);
		this.add(pluginListScrollpane);
		this.add(lblPluginSummary);
		this.add(tabbedPane);
		this.add(btnStart);
		this.add(btnOK);
	}

	//Assuming user interface setup, with all relevant plugin data already in table...
	private void setupPluginComboBoxes() {
    	rowEditorModel = new RowEditorModel();
    	table.setRowEditorModel(rowEditorModel);
    	int size = table.getRowCount();
        for (int i = 0; i < size; i++) {
        	PluginObject myPlugin = pluginTableModel.getEntry(i);
        	if (myPlugin.getStatus() == PluginObject.STATUS_UNINSTALLED) //if plugin is not installed
        		rowEditorModel.addEditorForRow(i, uninstalledOptions);
        	else if (myPlugin.getStatus() == PluginObject.STATUS_INSTALLED) //if plugin is installed
        		rowEditorModel.addEditorForRow(i, installedOptions);
        	else if (myPlugin.getStatus() == PluginObject.STATUS_MAY_UPDATE) //if plugin is installed
        		rowEditorModel.addEditorForRow(i, updateableOptions);
        	else
        		throw new Error("Error while assigning combo-boxes to data!");
        }
	}

	//Whenever Viewing Options in the ComboBox has been changed
	private void comboBoxViewListener() {
		String strOption = (String)comboBoxViewingOptions.getSelectedItem();

		//Remove the old pluginList display
		table.getModel().removeTableModelListener(this);
		//Preparing the new pluginList display
		table.setModel(pluginTableModel = new PluginTableModel());
		table.getModel().addTableModelListener(this);

		if (strOption.equals(arrViewingOptions[0])) {

			//if "View all plugins"
			pluginTableModel.update(controller.getPluginList());
			//When data is ready for display, allow editable ComboBoxes
			setupPluginComboBoxes();

		} else if (strOption.equals(arrViewingOptions[1])) {

			//if "View installed plugins"
			List<PluginObject> viewList = new ArrayList<PluginObject>();
			for (int i = 0; i < controller.getPluginList().size(); i++) {
				PluginObject myPlugin = controller.getPluginList().get(i);
				if (myPlugin.getStatus() == PluginObject.STATUS_INSTALLED ||
					myPlugin.getStatus() == PluginObject.STATUS_MAY_UPDATE) {
					viewList.add(myPlugin);
				}
			}
			pluginTableModel.update(viewList);
			//When data is ready for display, allow editable ComboBoxes
			setupPluginComboBoxes();

		} else if (strOption.equals(arrViewingOptions[2])) {

			//if "View uninstalled plugins"
			List<PluginObject> viewList = new ArrayList<PluginObject>();
			for (int i = 0; i < controller.getPluginList().size(); i++) {
				PluginObject myPlugin = controller.getPluginList().get(i);
				if (myPlugin.getStatus() == PluginObject.STATUS_UNINSTALLED) {
					viewList.add(myPlugin);
				}
			}
			pluginTableModel.update(viewList);
			//When data is ready for display, allow editable ComboBoxes
			setupPluginComboBoxes();

		} else if (strOption.equals(arrViewingOptions[3])) {

			//if "View up-to-date plugins"
			List<PluginObject> viewList = new ArrayList<PluginObject>();
			for (int i = 0; i < controller.getPluginList().size(); i++) {
				PluginObject myPlugin = controller.getPluginList().get(i);
				if (myPlugin.getStatus() == PluginObject.STATUS_INSTALLED) {
					viewList.add(myPlugin);
				}
			}
			pluginTableModel.update(viewList);
			//When data is ready for display, allow editable ComboBoxes
			setupPluginComboBoxes();

		} else if (strOption.equals(arrViewingOptions[4])) {

			//if "View update-able plugins"
			List<PluginObject> viewList = new ArrayList<PluginObject>();
			for (int i = 0; i < controller.getPluginList().size(); i++) {
				PluginObject myPlugin = controller.getPluginList().get(i);
				if (myPlugin.getStatus() == PluginObject.STATUS_MAY_UPDATE) {
					viewList.add(myPlugin);
				}
			}
			pluginTableModel.update(viewList);
			//When data is ready for display, allow editable ComboBoxes
			setupPluginComboBoxes();

		} else {
			throw new Error("Viewing option specified does not exist!");
		}
	}
	private void clickToBeginOperations() {
		//in later implementations, this should liase with Controller
		//e.g: controller.isReadyToBegin()... Controller checks pluginList...
		frameDownloader.setVisible(true);
		this.setEnabled(false);
	}
	
	private void clickToQuitPluginManager() {
		//in later implementations, this should have some notifications
		this.dispose();
	}

	public void clickBackToPluginManager() {
		//in later implementations, this should liase with Controller
		//e.g: controller.hasDownloadEnded()... Controller checks download complete or not...
		frameDownloader.setVisible(false);
		this.setEnabled(true);
	}

	public void actionPerformed(ActionEvent e) {
		
	}

	//When a value in the table has been modified by the user
	public void tableChanged(TableModelEvent e) {
		//QUESTION: should we count objects in the view-table or objects in the entire list?
		PluginTableModel model = (PluginTableModel)e.getSource();
		int size = model.getRowCount();
		int installCount = 0;
		int removeCount = 0;
		int updateCount = 0;
		for (int i = 0; i < size; i++) {
			PluginObject myPlugin = model.getEntry(i);
			if (myPlugin.getStatus() == PluginObject.STATUS_UNINSTALLED &&
				myPlugin.getAction() == PluginObject.ACTION_REVERSE) {
				installCount += 1;
			}
			else if ((myPlugin.getStatus() == PluginObject.STATUS_INSTALLED ||
				myPlugin.getStatus() == PluginObject.STATUS_MAY_UPDATE) &&
				myPlugin.getAction() == PluginObject.ACTION_REVERSE) {
				removeCount += 1;
			} else if (myPlugin.getStatus() == PluginObject.STATUS_MAY_UPDATE &&
						myPlugin.getAction() == PluginObject.ACTION_UPDATE) {
				updateCount += 1;
			}
		}
		lblPluginSummary.setText("Total: " + size + ", To install: " + installCount +
				", To remove: " + removeCount + ", To update: " + updateCount);
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

//{{{ PluginTableModel class
class PluginTableModel extends AbstractTableModel {
	private List<PluginObject> entries;

	//{{{ Constructor
	public PluginTableModel() {
		super();
		entries = new ArrayList<PluginObject>();
		//update(); //get the entries?
	} //}}}

	//{{{ getColumnCount() method
	public int getColumnCount() {
		return 2; //Name of plugin, status
	} //}}}

	//{{{ getColumnClass() method
	public Class getColumnClass(int columnIndex) {
		switch (columnIndex)
		{
			case 0: return String.class; //filename
			case 1: return String.class; //status
			default: return Object.class;
		}
	} //}}}

	//{{{ getColumnName() method
	public String getColumnName(int column) {
		switch (column)
		{
			case 0:
				return "Name";
			case 1:
				return "Status/Action";
			/*case 1:
				return jEdit.getProperty("manage-plugins.info.name");
			case 2:
				return jEdit.getProperty("manage-plugins.info.version");
			case 3:
				return jEdit.getProperty("manage-plugins.info.status");*/
			default:
				throw new Error("Column out of range");
		}
	} //}}}

	//{{{ getEntry() method
	public PluginObject getEntry(int rowIndex) {
		return (PluginObject)entries.get(rowIndex);
	} //}}}

	//{{{ getRowCount() method
	public int getRowCount() {
		return entries.size();
	} //}}}

	//{{{ getValueAt() method
	public Object getValueAt(int rowIndex, int columnIndex) {
		PluginObject entry = (PluginObject)entries.get(rowIndex);
		switch (columnIndex)
		{
			case 0:
				if (entry.getAction() != PluginObject.ACTION_NONE)
					return entry.getFilename() + " *"; //"*" indicates action needed
				else
					return entry.getFilename();
			case 1:
				byte currentStatus = entry.getStatus();
				byte actionToTake = entry.getAction();
				if (currentStatus == PluginObject.STATUS_UNINSTALLED) { //if not installed 
					if (actionToTake == PluginObject.ACTION_NONE)
						return PluginManager.arrUninstalledOptions[0]; //"Not installed"
					else if (actionToTake == PluginObject.ACTION_REVERSE)
						return PluginManager.arrUninstalledOptions[1]; //"Install"
					else
						throw new Error("INVALID action value for Uninstalled Plugin");
				} else if (currentStatus == PluginObject.STATUS_INSTALLED) { //if installed
					if (actionToTake == PluginObject.ACTION_NONE)
						return PluginManager.arrInstalledOptions[0]; //"Installed"
					else if (actionToTake == PluginObject.ACTION_REVERSE)
						return PluginManager.arrInstalledOptions[1]; //"Remove"
					else
						throw new Error("INVALID action value for Installed Plugin");
				} else if (currentStatus == PluginObject.STATUS_MAY_UPDATE) { //if installed AND update-able
					if (actionToTake == PluginObject.ACTION_NONE)
						return PluginManager.arrUpdateableOptions[0]; //"Installed"
					else if (actionToTake == PluginObject.ACTION_REVERSE)
						return PluginManager.arrUpdateableOptions[1]; //"Remove"
					else if (actionToTake == PluginObject.ACTION_UPDATE)
						return PluginManager.arrUpdateableOptions[2]; //"Update"
					else
						throw new Error("INVALID action value for Update-able Plugin");
				} else {
					throw new Error("INVALID Plugin Status retrieved!");
				}
			/*case 1:
				if(entry.name == null)
				{
					return MiscUtilities.getFileName(entry.jar);
				}
				else
					return entry.name;
			case 2:
				return entry.version;
			case 3:
				return jEdit.getProperty("plugin-manager.status."
					+ entry.status);*/
			default:
				throw new Error("Column out of range");
		}
	} //}}}

	//{{{ isCellEditable() method
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return columnIndex == 1;
	} //}}}

	//{{{ setValueAt() method
	public void setValueAt(Object value, int rowIndex, int columnIndex) {
		//Entry entry = (Entry)entries.get(rowIndex);
		PluginObject entry = (PluginObject)entries.get(rowIndex);
		if(columnIndex == 1) {
			String newValue = (String)value;
			//if current status of selected plugin is "not installed"
			if (entry.getStatus() == PluginObject.STATUS_UNINSTALLED) {
				//if option chosen is "Not installed"
				if (newValue.equals(PluginManager.arrUninstalledOptions[0])) {
					entry.setAction(PluginObject.ACTION_NONE);
				}
				//if option chosen is "Install"
				else if (newValue.equals(PluginManager.arrUninstalledOptions[1])) {
					entry.setAction(PluginObject.ACTION_REVERSE);
				}
				//otherwise...
				else {
					throw new Error("Invalid string value specified for " + entry.getFilename() + "; String object: " + newValue + ", Plugin status: " + entry.getStatus());
				}
			} else if (entry.getStatus() == PluginObject.STATUS_INSTALLED) {
				//if option chosen is "Installed"
				if (newValue.equals(PluginManager.arrInstalledOptions[0])) {
					entry.setAction(PluginObject.ACTION_NONE);
				}
				//if option chosen is "Remove" (Or uninstall it)
				else if (newValue.equals(PluginManager.arrInstalledOptions[1])) {
					entry.setAction(PluginObject.ACTION_REVERSE);
				}
				//otherwise...
				else {
					throw new Error("Invalid string value specified for " + entry.getFilename() + "; String object: " + newValue + ", Plugin status: " + entry.getStatus());
				}
			} else if (entry.getStatus() == PluginObject.STATUS_MAY_UPDATE) {
				//if option chosen is "Installed"
				if (newValue.equals(PluginManager.arrUpdateableOptions[0])) {
					entry.setAction(PluginObject.ACTION_NONE);
				}
				//if option chosen is "Remove" (Or uninstall it)
				else if (newValue.equals(PluginManager.arrUpdateableOptions[1])) {
					entry.setAction(PluginObject.ACTION_REVERSE);
				}
				//if option chosen is "Update"
				else if (newValue.equals(PluginManager.arrUpdateableOptions[2])) {
					entry.setAction(PluginObject.ACTION_UPDATE);
				}
				//otherwise...
				else {
					throw new Error("Invalid string value specified for " + entry.getFilename() + "; String object: " + newValue + ", Plugin status: " + entry.getStatus());
				}
			} else {
				throw new Error("Invalid status specified for " + entry.getFilename() + "; String object: " + newValue + ", Plugin status: " + entry.getStatus());
			}
			fireTableChanged(new TableModelEvent(this));
			/*PluginJAR jar = jEdit.getPluginJAR(entry.jar);
			if(jar == null)
			{
				if(value.equals(Boolean.FALSE))
					return;

				loadPluginJAR(entry.jar);
			}
			else
			{
				if(value.equals(Boolean.TRUE))
					return;

				unloadPluginJARWithDialog(jar);
			}*/
		}

		//update();
	} //}}}

	//{{{ update() method
	public void update(List<PluginObject> myArr) {
		entries = myArr;
		fireTableChanged(new TableModelEvent(this));

		/*String systemJarDir = MiscUtilities.constructPath(
			jEdit.getJEditHome(),"jars");
		String userJarDir;
		if(jEdit.getSettingsDirectory() == null)
			userJarDir = null;
		else
		{
			userJarDir = MiscUtilities.constructPath(
				jEdit.getSettingsDirectory(),"jars");
		}

		PluginJAR[] plugins = jEdit.getPluginJARs();
		for(int i = 0; i < plugins.length; i++)
		{
			String path = plugins[i].getPath();
			if(path.startsWith(systemJarDir)
				|| (userJarDir != null
				&& path.startsWith(userJarDir)))
			{
				Entry e = new Entry(plugins[i]);
				if(!hideLibraries.isSelected()
					|| e.clazz != null)
				{
					entries.add(e);
				}
			}
		}

		String[] newPlugins = jEdit.getNotLoadedPluginJARs();
		for(int i = 0; i < newPlugins.length; i++)
		{
			Entry e = new Entry(newPlugins[i]);
			entries.add(e);
		}

		sort(sortType);*/
	} //}}}

	//{{{ setSortType() method
	public void setSortType(int type) {
	} //}}}

	//{{{ sort() method
	public void sort(int type) {
	}
	//}}}


} //}}}
