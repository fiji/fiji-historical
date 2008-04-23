package Jython;
/** 
A Jython utility plugin for ImageJ(C).
Copyright (C) 2005 Albert Cardona.
This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation (http://www.gnu.org/licenses/gpl.txt )

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA. 

You may contact Albert Cardona at albert at pensament dot net, at http://www.pensament.net/java/
*/
import ij.IJ;
import ij.Menus;
import ij.plugin.PlugIn;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.MenuBar;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.io.File;
import org.python.util.PythonInterpreter;

/**
 * 	1 - looks for python script files under the ImageJ/plugins/jython folder
 * 	2 - updates the Plugins / Jython submenu, a MenuItem for each script
 * 	3 - listens to that submenu MenuItem items and launches the python scripts when called
 *
 * 	To create a shortcut to a Python plugin a macro can be done to pass appropriate arguments to the Launch_Python_Script class, or tweak ImageJ, or a thousand not-so-straighforward ways.
 *
 */
public class Refresh_Jython_List implements PlugIn, ActionListener {

	final File jython_dir = new File(Menus.getPlugInsPath() + "Jython");

	public void run(String arg) {
		// list .py files under plugins/Jython
		// Check that the directory exists, or return
		if (!jython_dir.exists()) {
			return;
		}
		// list .py files the easy way
		String[] files = jython_dir.list();
		ArrayList al_py_files = new ArrayList();
		for (int i=0; i<files.length; i++) {
			if (files[i].indexOf(".py") == files[i].length() - 3) {
				al_py_files.add(files[i]);
			}
		}
		String[] py_files = new String[0];//empty, so it doesn't fail ever below when there are no python scripts in the Jython folder //was: null;
		if (0 != al_py_files.size()) {
			py_files = new String[al_py_files.size()];
			al_py_files.toArray(py_files);
		}
		// grab the Menu "Jython" under the Plugins menu
		MenuBar menu_bar = Menus.getMenuBar();
		Menu plugins_menu = null;
		int n = menu_bar.getMenuCount();
		for (int i=0; i<n; i++) {
			Menu menu = menu_bar.getMenu(i);
			if (menu.getLabel().equals("Plugins")) {
				plugins_menu = menu;
				break;
			}
		}
		n = plugins_menu.getItemCount();
		Menu jython_menu = null;
		for (int i=0; i<n; i++) {
			MenuItem item = plugins_menu.getItem(i);
			if (item.getLabel().equals("Jython") && item instanceof Menu) {
				jython_menu = (Menu)item;
				break;
			}
		}
		// Remove all python scripts from the Jython menu
		n = jython_menu.getItemCount();
		MenuItem[] items = new MenuItem[n];
		for (int i=0; i<n; i++) {
			items[i] = jython_menu.getItem(i);
		}
		for (int i=0; i<n; i++) {
			String command = items[i].getActionCommand();
			if (command.indexOf(".py") != command.length() -3 && !command.equals("-")) { // separators are removed too, since they contain a single '-'
				continue;
			}
			jython_menu.remove(items[i]);
		}
		// Add a separator
		jython_menu.addSeparator();
		// Add the scripts as MenuItem if they arent' there already
		for (int i=0; i<py_files.length; i++) {
			MenuItem item = new MenuItem(strip(py_files[i]));
			item.addActionListener(this);
			item.setActionCommand(py_files[i]); // storing the name of the python script file as the action command. The label is stripped!
			jython_menu.add(item);
		}
		// Notify the user
		IJ.showStatus("Python script list refreshed (" + py_files.length  + " scripts)");
	}

	/** Converts 'My_python_script.py' to 'My python script'*/
	private String strip(String file_name) {
		StringBuffer name = new StringBuffer(file_name);
		int i_extension = file_name.indexOf(".py");
		if (-1 != i_extension && (file_name.length() -3) == i_extension) { //don't cute the extension if the .py is some internal part of the name
			//cut extension
			name.setLength(i_extension);
		}
		int i_ = file_name.indexOf('_');
		while (-1 != i_) {
			name.setCharAt(i_, ' ');
			i_ = file_name.indexOf('_', i_ +1);
		}
		return name.toString();
	}

	/** Listens to the MenuItem objects holding the name of the python script.*/
	public void actionPerformed(ActionEvent ae) {
		Object source = ae.getSource();
		if (source instanceof MenuItem) {
			MenuItem item = (MenuItem)source;
			final String file_name = item.getActionCommand();
			if (file_name.indexOf(".py") == file_name.length() -3) {
				// fork to avoid running on the EventDispatchThread
				new Thread() {
					public void run() {
						setPriority(Thread.NORM_PRIORITY);
						try {
							PythonInterpreter PI = new PythonInterpreter();
							Jython_Interpreter.importAll(PI);
							PI.execfile(jython_dir.getCanonicalPath() + File.separator + file_name);
						} catch (Exception e) {
							IJ.log(e.toString());
						}
					}
				}.start();
			}
		}
	}
}
