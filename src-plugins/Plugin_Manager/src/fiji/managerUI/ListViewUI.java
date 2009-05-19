package fiji.managerUI;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
//import javax.swing.JCheckBox;
//import javax.swing.JLabel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableColumn;
import javax.swing.table.AbstractTableModel;

import fiji.data.PluginObject;
import fiji.data.Dependency;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

class ListViewUI extends JPanel {
	private PluginManager pluginManager = null;
	private List<PluginObject> existingPluginList = null;
	private PluginTableModel pluginModel = null;
	private JTable table = null;
	private JTextPane txtPluginDetails = null;

	public ListViewUI(PluginManager pluginManager) {
		super();
		this.setLayout(null);
		this.pluginManager = pluginManager;

		/* Create the plugin table */
		table = new JTable(pluginModel = new PluginTableModel());
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
		                    PluginObject myPlugin = (PluginObject)existingPluginList.get(modelRow);
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
		txtPluginDetails = new JTextPane();
		txtPluginDetails.setEditable(false);
		txtPluginDetails.setText("");
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

	public void setExistingPluginList(List<PluginObject> arr) {
		existingPluginList = arr;
		pluginModel.update(existingPluginList);
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
		return 3; //4 values to show for Plugin object
	} //}}}

	//{{{ getColumnClass() method
	public Class getColumnClass(int columnIndex) {
		switch (columnIndex)
		{
			case 0: return Boolean.class;
			case 1: return String.class; //to be replaced...
			case 2: return String.class; //to be replaced...
			default: return Object.class;
		}
	} //}}}

	//{{{ getColumnName() method
	public String getColumnName(int column) {
		switch (column)
		{
			case 0:
				return " ";
			case 1:
				return "Name";
			case 2:
				return "Status";
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
				return new Boolean(entry.getStatusLoaded());
			case 1:
				return entry.getFilename();
			case 2:
				if (entry.getStatusLoaded() == false) {
					return "Unloaded";
				} else {
					return "Loaded";
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
		return columnIndex == 0;
	} //}}}

	//{{{ setValueAt() method
	public void setValueAt(Object value, int rowIndex, int columnIndex) {
		//Entry entry = (Entry)entries.get(rowIndex);
		PluginObject entry = (PluginObject)entries.get(rowIndex);
		if(columnIndex == 0)
		{
			entry.setStatusLoaded(((Boolean)value).booleanValue());
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
