/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/**
A modified version of Albert Cardona's Refresh_Jython_List plugin,
for subclassing to do the same for arbitrary languages and directories.

------------------------------------------------------------------------

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

package common;

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

/**
 * 	1 - looks for python script files under the ImageJ/plugins/jython folder
 * 	2 - updates the Plugins / Jython submenu, a MenuItem for each script
 * 	3 - listens to that submenu MenuItem items and launches the python scripts when called
 *
 * 	To create a shortcut to a Python plugin a macro can be done to pass appropriate arguments to the Launch_Python_Script class, or tweak ImageJ, or a thousand not-so-straighforward ways.
 *
 */
abstract public class Refresh_Scripts implements PlugIn, ActionListener {

	protected String subMenu;
	protected String scriptExtension;
	protected String languageName;

	public void setLanguageProperties( String subMenu, String scriptExtension, String languageName ) {
		this.subMenu = subMenu;
		this.scriptExtension = scriptExtension;
		this.languageName = languageName;
	}

	boolean verbose = false;

	protected void setVerbose(boolean v) { verbose = v; }

	File script_dir;

	public void run(String arg) {
		script_dir = new File(Menus.getPlugInsPath() + subMenu);

		if( subMenu == null || scriptExtension == null || languageName == null ) {
			IJ.error("BUG: setLanguageProperties must have been called (with non-null arguments)");
			return;
		}

		// Find files with the correct extension
		if (!script_dir.exists()) {
			return;
		}

		String[] files = script_dir.list();
		ArrayList all_script_files = new ArrayList();
		for (int i=0; i<files.length; i++) {
			if (files[i].endsWith(scriptExtension)) {
				all_script_files.add(files[i]);
			}
		}
		String[] script_files = new String[0];//empty, so it doesn't fail ever below when there are no python scripts in the Jython folder //was: null;
		if (0 != all_script_files.size()) {
			script_files = new String[all_script_files.size()];
			all_script_files.toArray(script_files);
		}
		// grab the right subMenu under the Plugins menu
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
		Menu script_menu = null;
		for (int i=0; i<n; i++) {
			MenuItem item = plugins_menu.getItem(i);
			if (item.getLabel().equals(subMenu) && item instanceof Menu) {
				script_menu = (Menu)item;
				break;
			}
		}
		// Remove all python scripts from the Jython menu
		n = script_menu.getItemCount();
		MenuItem[] items = new MenuItem[n];
		for (int i=0; i<n; i++) {
			items[i] = script_menu.getItem(i);
		}
		for (int i=0; i<n; i++) {
			String command = items[i].getActionCommand();
			if (!command.endsWith(scriptExtension) && !command.equals("-")) { // separators are removed too, since they contain a single '-'
				continue;
			}
			script_menu.remove(items[i]);
		}
		// Add a separator
		script_menu.addSeparator();
		// Add the scripts as MenuItem if they arent' there already
		for (int i=0; i<script_files.length; i++) {
			if( verbose )
				System.out.println("Found a "+languageName+" script: "+script_files[i]);
			MenuItem item = new MenuItem(strip(script_files[i]));
			item.addActionListener(this);
			item.setActionCommand(script_files[i]); // storing the name of the python script file as the action command. The label is stripped!
			script_menu.add(item);
		}
		// Notify the user
		IJ.showStatus(languageName + " script list refreshed (" + script_files.length  + " scripts)");
	}

	/** Converts 'My_python_script.py' to 'My python script'*/
	private String strip(String file_name) {
		StringBuffer name = new StringBuffer(file_name);
		int i_extension = file_name.indexOf(scriptExtension);
		if (-1 != i_extension && ((file_name.length()-scriptExtension.length()) == i_extension)) { //don't cut the extension if the .py is some internal part of the name
			//cut extension
			name.setLength(i_extension);
		}
		String result = name.toString();
		return result.replace('_',' ');
	}

	abstract protected void runScript(String filename);

	/** Listens to the MenuItem objects holding the name of the python script.*/
	public void actionPerformed(ActionEvent ae) {
		Object source = ae.getSource();
		if (source instanceof MenuItem) {
			MenuItem item = (MenuItem)source;
			final String file_name = item.getActionCommand();
			if (file_name.endsWith(scriptExtension)) {
				// fork to avoid running on the EventDispatchThread
				new Thread() {
					public void run() {
						setPriority(Thread.NORM_PRIORITY);
						try {
							String scriptFilename = script_dir.getCanonicalPath() + File.separator + file_name;
							runScript(scriptFilename);
							/* Move this into the refactored Refresh_Python_Scripts:
							   PythonInterpreter PI = new PythonInterpreter();
							   Jython_Interpreter.importAll(PI);
							   PI.execfile(script_dir.getCanonicalPath() + File.separator + file_name);
							*/
						} catch (Exception e) {
							e.printStackTrace();
							IJ.log(e.toString());
						}
					}
				}.start();
			}
		}
	}
}
