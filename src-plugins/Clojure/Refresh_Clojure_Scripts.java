package Clojure;

import common.RefreshScripts;

public class Refresh_Clojure_Scripts extends RefreshScripts {

	public void run(String arg) {
		setLanguageProperties(".clj","Clojure");
		setVerbose(false);
		super.run(arg);
	}

	public void runScript(String path) {
		try {
			Clojure_Interpreter.init();
			Clojure_Interpreter.parse("(load-file \"" + path + "\")");
			Clojure_Interpreter.cleanup();
		} catch (Throwable error) {
			printError(error);
		}
	}
}
