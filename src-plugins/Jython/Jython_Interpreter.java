package Jython;

/*
A dynamic Jython interpreter plugin for ImageJ(C).
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

You may contact Albert Cardona at albert at pensament net, at http://www.pensament.net/java/
*/
import ij.IJ;
import ij.gui.GenericDialog;
import java.util.ArrayList;
import org.python.util.PythonInterpreter;
import org.python.core.PyObject;
import org.python.core.PyDictionary;
import org.python.core.PySystemState;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;
import java.util.Collections;

import common.AbstractInterpreter;
import common.RefreshScripts;

/** A dynamic Jython interpreter for ImageJ.
 *	It'd be nice to have TAB expand ImageJ class names and methods.
 *
 *	Version: 2008-02-25 12:!2
 *
 *	$ PATH=/usr/local/jdk1.5.0_14/bin:$PATH javac -classpath .:../../ij.jar:../jython21/jython.jar Jython_Interpreter.java Refresh_Jython_List.java
 *	$ jar cf Jython_Interpreter.jar *class plugins.config
 */
public class Jython_Interpreter extends AbstractInterpreter {

	PythonInterpreter pi;

	public void run(String arg) {
		super.run(arg);
		super.window.setTitle("Jython Interpreter");
		super.prompt.setEnabled(false);
		print_out.print("Starting Jython ...");
		// Create a python interpreter that can load classes from plugin jar files.
		PySystemState.initialize(System.getProperties(), System.getProperties(), new String[] { }, IJ.getClassLoader());
		PySystemState pystate = new PySystemState();
		pystate.setClassLoader(IJ.getClassLoader());
		pystate.add_extdir(ij.Menus.getPlugInsPath()); // without this line, the class loader doesn't find plugin jars.
		pi = new PythonInterpreter(new PyDictionary(), pystate);
		//redirect stdout and stderr to the screen for the interpreter
		pi.setOut(out);
		pi.setErr(out);
		//pre-import all ImageJ java classes and TrakEM2 java classes
		String msg = importAll(pi);
		super.screen.append(msg);
		// fix back on closing
		super.window.addWindowListener(
			new WindowAdapter() {
				public void windowClosing(WindowEvent we) {
					pi.setOut(System.out);
					pi.setErr(System.err);
				}
			}
		);
		super.prompt.setEnabled(true);
		super.prompt.requestFocus();
		print_out.println("... done.");
	}

	/** Evaluate python code. */
	protected Object eval(String text) {
		pi.exec(fix(text));
		return null;
	}

	/** Returns an ArrayList of String, each entry a possible word expansion. */
	protected ArrayList expandStub(String stub) {
		final ArrayList al = new ArrayList();
		PyObject py_vars = pi.eval("vars().keys()");
		if (null == py_vars) {
			p("No vars to search into");
			return al;
		}
		String[] vars = (String[])py_vars.__tojava__(String[].class);
		for (int i=0; i<vars.length; i++) {
			if (vars[i].startsWith(stub)) {
				//System.out.println(vars[i]);
				al.add(vars[i]);
			}
		}
		Collections.sort(al, String.CASE_INSENSITIVE_ORDER);
		System.out.println("stub: '" + stub + "'");
		return al;
	}

	/** pre-import all ImageJ java classes and TrakEM2 java classes */
	static public String importAll(PythonInterpreter pi) {
		try {
			pi.exec("from ij import *\nfrom ij.gui import *\nfrom ij.io import *\nfrom ij.macro import *\nfrom ij.measure import *\nfrom ij.plugin import *\nfrom ij.plugin.filter import *\nfrom ij.plugin.frame import *\nfrom ij.process import *\nfrom ij.text import *\nfrom ij.util import *\nfrom java.lang import *\n");
		} catch (Exception e) {
			RefreshScripts.printError(e);
			return "";
		}
		String msg = "All ImageJ and java.lang";
		try {
			pi.exec("from ini.trakem2 import *\nfrom ini.trakem2.persistence import *\nfrom ini.trakem2.tree import *\nfrom ini.trakem2.display import *\nfrom ini.trakem2.imaging import *\nfrom ini.trakem2.io import *\nfrom ini.trakem2.utils import *\nfrom ini.trakem2.vector import *\nfrom mpi.fruitfly.analysis import *\nfrom mpi.fruitfly.fft import *\nfrom mpi.fruitfly.general import *\nfrom mpi.fruitfly.math import *\nfrom mpi.fruitfly.math.datastructures import *\nfrom mpi.fruitfly.registration import *\n");
			msg += " and TrakEM2";
		} catch (Exception e) { /*fail silently*/ }
		return msg + " classes imported.\n";
	}

	protected String getLineCommentMark() {
		return "#";
	}
}
