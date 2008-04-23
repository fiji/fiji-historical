/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package JRuby;

import ij.plugin.PlugIn;
import org.jruby.*;
import java.io.PrintStream;
import common.AbstractInterpreter;

public class JRuby_Interpreter extends AbstractInterpreter {

	Ruby rubyRuntime;

	protected Object eval(String text) {
		return rubyRuntime.evalScriptlet(text);
	}

	public void run( String ignored ) {
		super.run(ignored);
		setTitle("JRuby Interpreter");
		rubyRuntime = Ruby.newInstance(System.in,print_out,print_out);
		// This will need much more careful thought to import a sensible
		// set of classes:
		String startupScript = "" +
			"require 'java'\n" +
			"include_class 'ij.IJ'\n";
		rubyRuntime.evalScriptlet(startupScript);
	}
}
