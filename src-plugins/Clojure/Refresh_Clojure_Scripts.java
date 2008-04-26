package Clojure;

import common.RefreshScripts;
import java.io.File;
import ij.IJ;

public class Refresh_Clojure_Scripts extends RefreshScripts {

	public void run(String arg) {
		setLanguageProperties(".clj","Clojure");
		setVerbose(false);
		super.run(arg);
	}

	/** Runs the script at path */
	public void runScript(String path) {
		try {
			if (!path.endsWith(".clj") || !new File(path).exists()) {
				IJ.log("Not a clojure script or not found: " + path);
				return;
			}
			Clojure_Interpreter.evaluate("(load-file \"" + path + "\")");
			Clojure_Interpreter.destroy();
		} catch (Throwable error) {
			printError(error);
		}
	}
}
