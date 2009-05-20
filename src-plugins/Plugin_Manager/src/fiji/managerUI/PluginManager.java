package fiji.managerUI;
import ij.plugin.frame.PlugInFrame;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultCellEditor;
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
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

import extendedSwing.JTableX;
import extendedSwing.RowEditorModel;
import fiji.data.Dependency;
import fiji.data.PluginObject;
import fiji.logic.Controller;

public class PluginManager extends PlugInFrame implements ActionListener {
	private Controller controller = null;
	private List<PluginObject> pluginList = null;

	/* User Interface elements */
	private DownloadUI frameDownloader = null;
	private PluginTableModel pluginModel = null;
	private JTableX table = null;
	private JTextPane txtPluginDetails = null;
	private JButton btnStart = null;
	private JButton btnOK = null;
	private String[] arrUninstalledOptions = { "Uninstalled", "Install it" };
	private String[] arrInstalledOptions1 = { "Installed", "Remove it" };
	private String[] arrInstalledOptions2 = { "Installed", "Remove it", "Update it" };
	private DefaultCellEditor uninstalledOptions = new DefaultCellEditor(new JComboBox(arrUninstalledOptions));
	private DefaultCellEditor installedOptions1 = new DefaultCellEditor(new JComboBox(arrInstalledOptions1));
	private DefaultCellEditor installedOptions2 = new DefaultCellEditor(new JComboBox(arrInstalledOptions2));
	private RowEditorModel rowEditorModel = null;
	//private String ... //current.txt and database.txt address tentatively......

	public PluginManager() {
		super("Plugin Manager");
		this.setSize(750,540);
		this.setLayout(null);
		//Container content = this.getContentPane();

		//initialize the data...
		controller = new Controller();

        //if status says there's a list to download...
    	frameDownloader = new DownloadUI(this);
    	//but don't show it yet...

    	setUpUserInterface();
    	pluginList = controller.getExistingPluginList();
    	pluginModel.update(pluginList);
    	//after displaying UI, allow editable comboboxes
    	setupPluginComboBoxes();

		this.setVisible(true);
	}

	private void setUpUserInterface() {
		/* Create labels to annotate */
		JLabel tableLabel = new JLabel("Please choose what you want to install/uninstall:");
		tableLabel.setBounds(30, 120, 350, 30);
		tableLabel.setAlignmentX(TOP_ALIGNMENT);
		tableLabel.setAlignmentY(LEFT_ALIGNMENT);

		/* Create the plugin table */
		table = new JTableX(pluginModel = new PluginTableModel());
		table.setColumnSelectionAllowed(false);
		table.setRowSelectionAllowed(true);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.getSelectionModel().addListSelectionListener(

		        new ListSelectionListener() {
		            public void valueChanged(ListSelectionEvent event) {
		                int viewRow = table.getSelectedRow();
		                if (viewRow < 0) {
		                    //Selection got filtered away
		                } else {
		                    int modelRow = table.convertRowIndexToModel(viewRow);
		                    PluginObject myPlugin = (PluginObject)pluginList.get(modelRow);
		                    txtPluginDetails.setText("");
		                    TextPaneFormat.insertText(txtPluginDetails, "Name: ", TextPaneFormat.BOLD_BLACK);
		                    TextPaneFormat.insertText(txtPluginDetails, myPlugin.getFilename(), TextPaneFormat.BLACK);
		                    TextPaneFormat.insertText(txtPluginDetails, "\n\nMd5 Sum", TextPaneFormat.BOLD_BLACK);
		                    TextPaneFormat.insertText(txtPluginDetails, "\n" + myPlugin.getmd5Sum(), TextPaneFormat.BLACK);
		                    TextPaneFormat.insertText(txtPluginDetails, "\n\nReleased:", TextPaneFormat.BOLD_BLACK);
		                    TextPaneFormat.insertText(txtPluginDetails, myPlugin.getTimestamp(), TextPaneFormat.BLACK);
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
		                    TextPaneFormat.insertText(txtPluginDetails, "\n" + myPlugin.getDescription(), TextPaneFormat.BLACK);
		                }
		            }
		        }

		);

		//Set appearance of table
		table.setShowGrid(false);
		table.setIntercellSpacing(new Dimension(0,0));
		table.setRowHeight(table.getRowHeight() + 2);
		table.setPreferredScrollableViewportSize(new Dimension(370,310));
		table.setBounds(0, 0, 370, 310);
		table.setRequestFocusEnabled(false);

		TableColumn col1 = table.getColumnModel().getColumn(0);
		TableColumn col2 = table.getColumnModel().getColumn(1);

		col1.setPreferredWidth(190);
		col1.setMinWidth(190);
		col1.setMaxWidth(190);
		col1.setResizable(false);
		col2.setPreferredWidth(180);
		col2.setMinWidth(180);
		col2.setMaxWidth(180);
		col2.setResizable(false);

		/* Create scrollpane to hold the table */
		JScrollPane scrollpane = new JScrollPane(table);
		scrollpane.getViewport().setBackground(table.getBackground());
		scrollpane.setBounds(30, 150, 370, 310);

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
		btnStart.setBounds(30, 470, 110, 30);
		btnStart.setText("Start");
		btnStart.setToolTipText("Start installing/uninstalling specified plugins");

		//Buttons to quit Plugin Manager
		btnOK = new JButton();
		btnOK.setBounds(610, 490, 110, 30);
		btnOK.setText("OK");
		btnOK.setToolTipText("Exit Plugin Manager");

		this.add(tableLabel);
		this.add(scrollpane);
		this.add(tabbedPane);
		this.add(btnStart);
		this.add(btnOK);
	}

	//Assuming user interface has been setup along with values available on table...
	private void setupPluginComboBoxes() {
    	rowEditorModel = new RowEditorModel();
    	table.setRowEditorModel(rowEditorModel);
        for (int i = 0; i < pluginList.size(); i++) {
        	PluginObject myPlugin = (PluginObject)pluginList.get(i);
        	if (myPlugin.getStatus() == false) //if plugin is not installed
        		rowEditorModel.addEditorForRow(i, uninstalledOptions);
        	else //if plugin is installed
        		//todo: Check whether plugin has updates or not (ref. discovered later versions)
        		rowEditorModel.addEditorForRow(i, installedOptions1);
        }
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
		//controller.generateUpdatesPluginList();
		//updateUI.setUpdatesPluginList(controller.getUpdatesPluginList());
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
				return entry.getFilename();
			case 1:
				boolean installed = entry.getStatus();
				int actionToTake = entry.getAction();
				if (installed == false) {
					if (actionToTake == 0)
						return "Not installed";
					else if (actionToTake == 1)
						return "Install it";
					else
						throw new Error("INVALID action value for Uninstalled Plugin");
				} else { //if it is installed
					if (actionToTake == 0)
						return "Installed";
					else if (actionToTake == 2)
						return "Remove it";
					else if (actionToTake == 3)
						return "Update it";
					else
						throw new Error("INVALID action value for Installed Plugin");
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
			if (newValue.equals("Not installed") && entry.getStatus() == false)
				entry.setAction(0);
			else if (newValue.equals("Install it") && entry.getStatus() == false)
				entry.setAction(1);
			else if (newValue.equals("Installed") && entry.getStatus() == true)
				entry.setAction(0);
			else if (newValue.equals("Remove it") && entry.getStatus() == true)
				entry.setAction(2);
			else if (newValue.equals("Update it") && entry.getStatus() == true)
				entry.setAction(3);
			else throw new Error("INVALID action value specified for selected Plugin");
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
		entries.clear();
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
