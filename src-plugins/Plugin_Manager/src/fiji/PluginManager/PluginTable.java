package fiji.PluginManager;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;

/*
 * This class' role is to be in charge of how the Table should be displayed
 */
public class PluginTable extends JTable {
	private Controller controller;
	private PluginTableModel pluginTableModel;
	private PluginManager pluginManager;

	static String[] arrUninstalledOptions = { "Not installed", "Install it" };
	static String[] arrInstalledOptions = { "Installed", "Remove it" };
	static String[] arrUpdateableOptions = { "Installed", "Remove it", "Update it" };
	private TableCellEditor uninstalledOptions = new DefaultCellEditor(new JComboBox(arrUninstalledOptions));
	private TableCellEditor installedOptions = new DefaultCellEditor(new JComboBox(arrInstalledOptions));
	private TableCellEditor updateableOptions = new DefaultCellEditor(new JComboBox(arrUpdateableOptions));

	//NOTE: To be created only after related display components are created
	//Namely: lblPluginSummary, txtPluginDetails
	public PluginTable(Controller controller, PluginManager pluginManager) {
		this.controller = controller;
		this.pluginManager = pluginManager;

		//default display: All plugins shown
		setupTableModel(controller.getPluginList());

		//set up the table properties and other settings
		setColumnSelectionAllowed(false);
		setRowSelectionAllowed(true);
		//this.setRowSelectionInterval(0, 1); //1st row selected
		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		getSelectionModel().addListSelectionListener(new ListSelectionListener() {

			//Called when a row is selected
			public void valueChanged(ListSelectionEvent event) {
				int viewRow = getSelectedRow();
				if (viewRow < 0) {
					//Selection got filtered away
				} else {
					int modelRow = convertRowIndexToModel(viewRow);
					PluginObject myPlugin = pluginTableModel.getEntry(modelRow);
					displayPluginDetails(myPlugin);
				}
			}

		});

		//Set appearance of table
		setShowGrid(false);
		setIntercellSpacing(new Dimension(0,0));
		setAutoResizeMode(JTableX.AUTO_RESIZE_ALL_COLUMNS);
		setRequestFocusEnabled(false);
		setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {

			// method to over-ride - returns cell renderer component
			public Component getTableCellRendererComponent(JTable table, Object value, 
					boolean isSelected, boolean hasFocus, int row, int column) {

				// let the default renderer prepare the component for us
				Component comp = super.getTableCellRendererComponent(table, value,
						isSelected, hasFocus, row, column);
				int modelRow = table.convertRowIndexToModel(row);
				PluginObject myPlugin = pluginTableModel.getEntry(modelRow);

				if (myPlugin.getAction() == PluginObject.ACTION_NONE) {
					//if there is no action
					comp.setFont(comp.getFont().deriveFont(Font.PLAIN));
				} else {
					//if an action is specified by user, bold the field
					comp.setFont(comp.getFont().deriveFont(Font.BOLD));
				}

				return comp;
			}
		});
	}

	//Set up table model, to be called each time display list is to be changed
	public void setupTableModel(List<PluginObject> myList) {
		getModel().removeTableModelListener(pluginManager);
		setModel(pluginTableModel = new PluginTableModel(controller));
		getModel().addTableModelListener(pluginManager); //listen for changes (tableChanged(TableModelEvent e))
		TableColumn col1 = getColumnModel().getColumn(0);
		TableColumn col2 = getColumnModel().getColumn(1);

		//Minimum width of 370 (250 + 120)
		col1.setPreferredWidth(250);
		col1.setMinWidth(250);
		col1.setResizable(false);
		col2.setPreferredWidth(120);
		col2.setMinWidth(120);
		col2.setResizable(false);

		pluginTableModel.update(myList);
	}

	public TableCellEditor getCellEditor(int row, int col) {
		PluginTableModel pluginTableModel = (PluginTableModel)this.getModel();
		PluginObject myPlugin = pluginTableModel.getEntry(row);

		//As we follow PluginTableModel, 1st column is filename
		if (col == 0) {
			return super.getCellEditor(row,col);
		} else if (col == 1) {
			if (myPlugin.getStatus() == PluginObject.STATUS_UNINSTALLED) //if plugin is not installed
				return uninstalledOptions;
			else if (myPlugin.getStatus() == PluginObject.STATUS_INSTALLED) //if plugin is installed
				return installedOptions;
			else if (myPlugin.getStatus() == PluginObject.STATUS_MAY_UPDATE) //if plugin is installed
				return updateableOptions;
			else
				throw new Error("Error while assigning combo-boxes to data!");
		} else
			throw new Error("Unidentified Column number for Plugin Table");
	}

	private void displayPluginDetails(PluginObject myPlugin) {
		pluginManager.displayPluginDetails(myPlugin);
	}
}

class PluginTableModel extends AbstractTableModel {
	private List<PluginObject> entries;
	private Controller controller;

	public PluginTableModel(Controller controller) {
		super();
		entries = new ArrayList<PluginObject>();
		this.controller = controller;
	}

	public int getColumnCount() {
		return 2; //Name of plugin, status
	}

	public Class getColumnClass(int columnIndex) {
		switch (columnIndex) {
			case 0: return String.class; //filename
			case 1: return String.class; //status
			default: return Object.class;
		}
	}

	public String getColumnName(int column) {
		switch (column) {
			case 0:
				return "Name";
			case 1:
				return "Status/Action";
			default:
				throw new Error("Column out of range");
		}
	}

	public PluginObject getEntry(int rowIndex) {
		return (PluginObject)entries.get(rowIndex);
	}

	public int getRowCount() {
		return entries.size();
	}

	public Object getValueAt(int rowIndex, int columnIndex) {
		PluginObject entry = (PluginObject)entries.get(rowIndex);
		switch (columnIndex) {
			case 0:
				return entry.getFilename();
			case 1:
				byte currentStatus = entry.getStatus();
				byte actionToTake = entry.getAction();
				if (currentStatus == PluginObject.STATUS_UNINSTALLED) { //if not installed 
					if (actionToTake == PluginObject.ACTION_NONE)
						return PluginTable.arrUninstalledOptions[0]; //"Not installed"
					else if (actionToTake == PluginObject.ACTION_REVERSE)
						return PluginTable.arrUninstalledOptions[1]; //"Install"
					else
						throw new Error("INVALID action value for Uninstalled Plugin");
				} else if (currentStatus == PluginObject.STATUS_INSTALLED) { //if installed
					if (actionToTake == PluginObject.ACTION_NONE)
						return PluginTable.arrInstalledOptions[0]; //"Installed"
					else if (actionToTake == PluginObject.ACTION_REVERSE)
						return PluginTable.arrInstalledOptions[1]; //"Remove"
					else
						throw new Error("INVALID action value for Installed Plugin");
				} else if (currentStatus == PluginObject.STATUS_MAY_UPDATE) { //if installed AND update-able
					if (actionToTake == PluginObject.ACTION_NONE)
						return PluginTable.arrUpdateableOptions[0]; //"Installed"
					else if (actionToTake == PluginObject.ACTION_REVERSE)
						return PluginTable.arrUpdateableOptions[1]; //"Remove"
					else if (actionToTake == PluginObject.ACTION_UPDATE)
						return PluginTable.arrUpdateableOptions[2]; //"Update"
					else
						throw new Error("INVALID action value for Update-able Plugin");
				} else {
					throw new Error("INVALID Plugin Status retrieved!");
				}
			default:
				throw new Error("Column out of range");
		}
	}

	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return columnIndex == 1;
	}

	public void setValueAt(Object value, int rowIndex, int columnIndex) {
		PluginObject entry = (PluginObject)entries.get(rowIndex);
		if(columnIndex == 1) {
			String newValue = (String)value;
			//if current status of selected plugin is "not installed"
			if (entry.getStatus() == PluginObject.STATUS_UNINSTALLED) {
				//if option chosen is "Not installed"
				if (newValue.equals(PluginTable.arrUninstalledOptions[0])) {
					List<PluginObject> toUninstallList = new ArrayList<PluginObject>();
					controller.removeDependent(toUninstallList, entry);
					entry.setAction(PluginObject.ACTION_NONE);
					
					//debug: Print out the plugin dependencies
					System.out.println("What you deselected: " + entry.getFilename());
					for (int i = 0; i < toUninstallList.size(); i++) {
						PluginObject myPlugin = toUninstallList.get(i);
						System.out.println("To set to uninstalled: " + myPlugin.getFilename() + ", " + myPlugin.getTimestamp());
					}
				}
				//if option chosen is "Install"
				else if (newValue.equals(PluginTable.arrUninstalledOptions[1])) {
					List<PluginObject> toInstallList = new ArrayList<PluginObject>();
					List<PluginObject> toUpdateList = new ArrayList<PluginObject>();
					controller.addDependency(toInstallList, toUpdateList, entry);
					entry.setAction(PluginObject.ACTION_REVERSE);

					//debug: Print out the plugin dependencies
					System.out.println("What you selected: " + entry.getFilename());
					for (int i = 0; i < toInstallList.size(); i++) {
						PluginObject myPlugin = toInstallList.get(i);
						System.out.println("To set to installed: " + myPlugin.getFilename() + ", " + myPlugin.getTimestamp());
					}
					for (int i = 0; i < toUpdateList.size(); i++) {
						PluginObject myPlugin = toUpdateList.get(i);
						System.out.println("To set to updated: " + myPlugin.getFilename() + ", " + myPlugin.getNewTimestamp());
					}
				}
				//otherwise...
				else {
					throw new Error("Invalid string value specified for " + entry.getFilename() + "; String object: " + newValue + ", Plugin status: " + entry.getStatus());
				}
			} else if (entry.getStatus() == PluginObject.STATUS_INSTALLED) {
				//if option chosen is "Installed"
				if (newValue.equals(PluginTable.arrInstalledOptions[0])) {
					List<PluginObject> toInstallList = new ArrayList<PluginObject>();
					List<PluginObject> toUpdateList = new ArrayList<PluginObject>();
					controller.addDependency(toInstallList, toUpdateList, entry);
					entry.setAction(PluginObject.ACTION_NONE);

					//debug: Print out the plugin dependencies
					System.out.println("What you selected: " + entry.getFilename());
					for (int i = 0; i < toInstallList.size(); i++) {
						PluginObject myPlugin = toInstallList.get(i);
						System.out.println("To set to installed: " + myPlugin.getFilename() + ", " + myPlugin.getTimestamp());
					}
					for (int i = 0; i < toUpdateList.size(); i++) {
						PluginObject myPlugin = toUpdateList.get(i);
						System.out.println("To set to updated: " + myPlugin.getFilename() + ", " + myPlugin.getNewTimestamp());
					}
				}
				//if option chosen is "Remove" (Or uninstall it)
				else if (newValue.equals(PluginTable.arrInstalledOptions[1])) {
					//TO IMPLEMENT: To check whether it is valid to remove w/o violating dependencies
					List<PluginObject> toUninstallList = new ArrayList<PluginObject>();
					controller.removeDependent(toUninstallList, entry);
					entry.setAction(PluginObject.ACTION_REVERSE);
					
					//debug: Print out the plugin dependencies
					System.out.println("What you deselected: " + entry.getFilename());
					for (int i = 0; i < toUninstallList.size(); i++) {
						PluginObject myPlugin = toUninstallList.get(i);
						System.out.println("To set to uninstalled: " + myPlugin.getFilename() + ", " + myPlugin.getTimestamp());
					}
				}
				//otherwise...
				else {
					throw new Error("Invalid string value specified for " + entry.getFilename() + "; String object: " + newValue + ", Plugin status: " + entry.getStatus());
				}
			} else if (entry.getStatus() == PluginObject.STATUS_MAY_UPDATE) {
				//if option chosen is "Installed"
				if (newValue.equals(PluginTable.arrUpdateableOptions[0])) {
					List<PluginObject> toInstallList = new ArrayList<PluginObject>();
					List<PluginObject> toUpdateList = new ArrayList<PluginObject>();
					controller.addDependency(toInstallList, toUpdateList, entry);
					entry.setAction(PluginObject.ACTION_NONE);

					//debug: Print out the plugin dependencies
					System.out.println("What you selected: " + entry.getFilename());
					for (int i = 0; i < toInstallList.size(); i++) {
						PluginObject myPlugin = toInstallList.get(i);
						System.out.println("To set to installed: " + myPlugin.getFilename() + ", " + myPlugin.getTimestamp());
					}
					for (int i = 0; i < toUpdateList.size(); i++) {
						PluginObject myPlugin = toUpdateList.get(i);
						System.out.println("To set to updated: " + myPlugin.getFilename() + ", " + myPlugin.getNewTimestamp());
					}
				}
				//if option chosen is "Remove" (Or uninstall it)
				else if (newValue.equals(PluginTable.arrUpdateableOptions[1])) {
					List<PluginObject> toUninstallList = new ArrayList<PluginObject>();
					controller.removeDependent(toUninstallList, entry);
					entry.setAction(PluginObject.ACTION_REVERSE);
					
					//debug: Print out the plugin dependencies
					System.out.println("What you deselected: " + entry.getFilename());
					for (int i = 0; i < toUninstallList.size(); i++) {
						PluginObject myPlugin = toUninstallList.get(i);
						System.out.println("To set to uninstalled: " + myPlugin.getFilename() + ", " + myPlugin.getTimestamp());
					}
				}
				//if option chosen is "Update"
				else if (newValue.equals(PluginTable.arrUpdateableOptions[2])) {
					List<PluginObject> toInstallList = new ArrayList<PluginObject>();
					List<PluginObject> toUpdateList = new ArrayList<PluginObject>();
					controller.addDependency(toInstallList, toUpdateList, entry);
					entry.setAction(PluginObject.ACTION_UPDATE);

					//debug: Print out the plugin dependencies
					System.out.println("What you selected: " + entry.getFilename());
					for (int i = 0; i < toInstallList.size(); i++) {
						PluginObject myPlugin = toInstallList.get(i);
						System.out.println("To set to installed: " + myPlugin.getFilename() + ", " + myPlugin.getTimestamp());
					}
					for (int i = 0; i < toUpdateList.size(); i++) {
						PluginObject myPlugin = toUpdateList.get(i);
						System.out.println("To set to updated: " + myPlugin.getFilename() + ", " + myPlugin.getNewTimestamp());
					}
				}
				//otherwise...
				else {
					throw new Error("Invalid string value specified for " + entry.getFilename() + "; String object: " + newValue + ", Plugin status: " + entry.getStatus());
				}
			} else {
				throw new Error("Invalid status specified for " + entry.getFilename() + "; String object: " + newValue + ", Plugin status: " + entry.getStatus());
			}
			fireTableChanged(new TableModelEvent(this));
		}
	}

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
	}

	public void setSortType(int type) {
	}

	public void sort(int type) {
	}
}
