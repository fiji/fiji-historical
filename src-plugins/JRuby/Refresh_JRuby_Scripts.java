/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package JRuby;

import common.Refresh_Scripts;
import org.jruby.*;

import java.io.*;

public class Refresh_JRuby_Scripts extends Refresh_Scripts {

	public void run(String arg) {
		setLanguageProperties("JRuby",".rb","Ruby");
		setVerbose(true);
		super.run(arg);
	}

	public void runScript(String filename) {
		PrintStream outPS=new PrintStream(System.out);
		System.out.println("Starting JRuby in runScript()...");
		Ruby rubyRuntime = Ruby.newInstance(System.in,outPS,outPS);
		System.out.println("Done.");
		rubyRuntime.evalScriptlet(JRuby_Interpreter.startupScript);

		FileInputStream fis=null;
		try {
			fis = new FileInputStream(filename);
		} catch( IOException e ) {
			throw new RuntimeException("Couldn't open the script: "+filename);
		}

		rubyRuntime.runFromMain(fis,filename);

		// Undesirably this throws an exception, so just let the 
		// JRuby runtime get finalized whenever...

		// rubyRuntime.evalScriptlet("exit");
	}
}
