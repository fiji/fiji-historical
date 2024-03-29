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
import org.python.util.PythonInterpreter;
import common.RefreshScripts;

/**
 * 	1 - looks for python script files under the ImageJ/plugins/jython folder
 * 	2 - updates the Plugins / Jython submenu, a MenuItem for each script
 * 	3 - listens to that submenu MenuItem items and launches the python scripts when called
 *
 * 	To create a shortcut to a Python plugin a macro can be done to pass appropriate arguments to the Launch_Python_Script class, or tweak ImageJ, or a thousand not-so-straighforward ways.
 *
 */
public class Refresh_Jython_Scripts extends RefreshScripts {

	public void run(String arg) {
		setLanguageProperties(".py", "Jython");
		setVerbose(false);
		super.run(arg);
	}

	/** Run a jython script in its own separate interpreter and namespace. */
	public void runScript(String path) {
		try {
			PythonInterpreter PI = new PythonInterpreter();
			Jython_Interpreter.importAll(PI);
			PI.execfile(path);
		} catch (Throwable t) {
			printError(t);
		}
	}
}
