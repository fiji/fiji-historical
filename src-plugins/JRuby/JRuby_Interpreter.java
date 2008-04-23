/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package JRuby;

import ij.IJ;
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
		// Strangely, this seems to always return null even if
		// there's an instance already running...
		if( null != Ruby.getCurrentInstance() ) {
			IJ.error("There is already an instance of "+
				 "the JRuby interpreter");
			return;
		}
		super.run(ignored);
		setTitle("JRuby Interpreter");
		print_out.println("Starting JRuby ...");
		rubyRuntime = Ruby.newInstance(System.in,print_out,print_out);
		print_out.println("Done.");
		// This sets up method_missing to find the right class
		// for anything beginning ij in the ij package.  (We could
		// change this to add other package hierarchies too, e.g.
		// those in VIB.)
		String startupScript = "" +
			"require 'java'\n" +
			"module Kernel\n" +
			"  def ij\n" +
			"    JavaUtilities.get_package_module_dot_format('ij')" +
			"  end\n" +
			"end\n";
		rubyRuntime.evalScriptlet(startupScript);
	}
}
