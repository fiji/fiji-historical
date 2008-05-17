/** Albert Cardona 2008. Released under General Public License. */
package Javascript;

import ij.IJ;
import ij.plugin.PlugIn;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ImporterTopLevel;
import java.io.PrintStream;
import common.AbstractInterpreter;
import ij.Menus;
import java.io.File;

public class Javascript_Interpreter extends AbstractInterpreter {

	private Scriptable scope = null;
	private Context cx = null;

	protected void threadStarting() {
		try {
			// Initialize inside the executer thread in parent class
			print_out.print("Starting Javascript ...");
			cx = Context.enter();
			//scope = cx.initStandardObjects(null); // reuse
			// the above, with a null arg, enables reuse of the scope in subsequent calls.
			// But this one is better: includes importClass and importPackage js functions
			scope = new ImporterTopLevel(cx);
			print_out.println(" done.");
			if (null != imports(cx, scope)) print_out.println("All ImageJ and java.lang.* classes imported.");
		} catch (Throwable t) {
			t.printStackTrace(print_out);
		}
	}

	protected Object eval(String text) throws Throwable {
		if (null == scope) return null;
		Object result = cx.evaluateString(scope, text, "<cmd>", 1, null);
		return cx.toString(result);
	}

	public void run( String ignored ) {
		super.run(ignored);
		setTitle("Javascript Interpreter");
	}

	protected void windowClosing() {}

	protected void threadQuitting() {
		Context.exit();
	}

	/** Import all ImageJ and java.lang classes. */
	static protected String imports(Context cx, Scriptable scope) {
		String[] pkgs = {"ij", "ij.gui", "ij.io", "ij.macro", "ij.measure", "ij.plugin", "ij.plugin.filter", "ij.plugin.frame", "ij.process", "ij.text", "ij.util", "java.lang"};
		StringBuffer sb = new StringBuffer();
		for (int i=0; i<pkgs.length; i++) {
			sb.append("importPackage(Packages.").append(pkgs[i]).append(");");
		}
		try {
			return cx.toString(cx.evaluateString(scope, sb.toString(), "<cmd>", 1, null));
		} catch (Throwable t) {
			t.printStackTrace();
		}
		return null;
	}
}
