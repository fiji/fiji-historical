package fiji.PluginManager;

import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;

/*
 * This class' role is to be in charge of how the Table should be displayed
 */
public class PluginTable extends JTable {
	static String[] arrUninstalledOptions = { "Not installed", "Install it" };
	static String[] arrInstalledOptions = { "Installed", "Remove it" };
	static String[] arrUpdateableOptions = { "Installed", "Remove it", "Update it" };
	private TableCellEditor uninstalledOptions = new DefaultCellEditor(new JComboBox(arrUninstalledOptions));
	private TableCellEditor installedOptions = new DefaultCellEditor(new JComboBox(arrInstalledOptions));
	private TableCellEditor updateableOptions = new DefaultCellEditor(new JComboBox(arrUpdateableOptions));

	//constructors remain the same

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
}

class PluginTableModel extends AbstractTableModel {
	private List<PluginObject> entries;

	public PluginTableModel() {
		super();
		entries = new ArrayList<PluginObject>();
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
					entry.setAction(PluginObject.ACTION_NONE);
				}
				//if option chosen is "Install"
				else if (newValue.equals(PluginTable.arrUninstalledOptions[1])) {
					entry.setAction(PluginObject.ACTION_REVERSE);
				}
				//otherwise...
				else {
					throw new Error("Invalid string value specified for " + entry.getFilename() + "; String object: " + newValue + ", Plugin status: " + entry.getStatus());
				}
			} else if (entry.getStatus() == PluginObject.STATUS_INSTALLED) {
				//if option chosen is "Installed"
				if (newValue.equals(PluginTable.arrInstalledOptions[0])) {
					entry.setAction(PluginObject.ACTION_NONE);
				}
				//if option chosen is "Remove" (Or uninstall it)
				else if (newValue.equals(PluginTable.arrInstalledOptions[1])) {
					entry.setAction(PluginObject.ACTION_REVERSE);
				}
				//otherwise...
				else {
					throw new Error("Invalid string value specified for " + entry.getFilename() + "; String object: " + newValue + ", Plugin status: " + entry.getStatus());
				}
			} else if (entry.getStatus() == PluginObject.STATUS_MAY_UPDATE) {
				//if option chosen is "Installed"
				if (newValue.equals(PluginTable.arrUpdateableOptions[0])) {
					entry.setAction(PluginObject.ACTION_NONE);
				}
				//if option chosen is "Remove" (Or uninstall it)
				else if (newValue.equals(PluginTable.arrUpdateableOptions[1])) {
					entry.setAction(PluginObject.ACTION_REVERSE);
				}
				//if option chosen is "Update"
				else if (newValue.equals(PluginTable.arrUpdateableOptions[2])) {
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
