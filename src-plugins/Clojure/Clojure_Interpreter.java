package Clojure;

/*
A dynamic Clojure interpreter plugin for ImageJ(C).
Copyright (C) 2008 Albert Cardona.
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

You may contact Albert Cardona at acardona at ini phys ethz ch.
*/
import ij.IJ;

import clojure.lang.RT;
import clojure.lang.Symbol;
import clojure.lang.Compiler;
import clojure.lang.LispReader;
import clojure.lang.Var;
import clojure.lang.LineNumberingPushbackReader;

import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;
import java.util.Collections;
import java.util.ArrayList;
import java.io.PipedWriter;
import java.io.PipedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import common.AbstractInterpreter;

public class Clojure_Interpreter extends AbstractInterpreter {

	static final Symbol USER = Symbol.create("user");
	static final Symbol CLOJURE = Symbol.create("clojure");
	static final Var in_ns = RT.var("clojure", "in-ns");
	static final Var refer = RT.var("clojure", "refer");
	static final Var ns = RT.var("clojure", "*ns*");
	static final Var warn_on_reflection = RT.var("clojure", "*warn-on-reflection*");
	static final Object EOF = new Object();

	static private boolean loaded = false;

	static private LispThread thread = null;

	public void run(String arg) {
		// synchronized with the destroy() method
		synchronized (EOF) {
			if (loaded) {
				IJ.showMessage("You can only have one instance of the Clojure interpreter running.");
				return;
			}

			loaded = true;
			super.screen.append("Starting Clojure...");
			final LispThread thread = new LispThread();
			if (!thread.ready()) {
				p("Some error ocurred.");
				return;
			}
			super.screen.append(" Ready -- have fun.\n>>>\n");
			this.thread = thread;
			// ok create window
			super.run(arg);
			super.window.setTitle("Clojure Interpreter");
		}
	}

	/** Override super. */
	protected void windowClosing() {
		thread.quit();
	}

	/** Evaluate clojure code. */ // overrides super method
	protected Object eval(final String text) throws Throwable {
		return evaluate(text);
	}

	static public Object evaluate(final String text) throws Throwable {
		if (null == thread) thread = new LispThread();
		Object ret = thread.eval(text);
		thread.throwError();
		return ret;
	}

	/** Complicated Thread setup just to be able to initialize and cleanup within the context of the same Thread, as required by Clojure. */
	static private class LispThread extends Thread {

		final Object lock = new Object();

		private boolean go = false;
		private String text = null;
		private String result = null;
		private Throwable error = null;
		private boolean working = false;
		LispThread() {
			setPriority(Thread.NORM_PRIORITY);
			try { setDaemon(true); } catch (Exception e) { e.printStackTrace(); }
			start();
			while (!go) {
				try { Thread.sleep(100); } catch (InterruptedException ie) {}
			}
		}
		boolean ready() {
			synchronized (this) {
				return isAlive();
			}
		}
		void throwError() throws Throwable {
			synchronized (this) {
				if (null == error) return;
				Throwable t = error;
				error = null;
				throw t;
			}
		}
		private void setup() {
			synchronized (this) {
				try {
					init();
				} catch (Throwable e) {
					e.printStackTrace();
				}
				// Outside try{}catch for it must always be true on start, so the thread can start, notify and die on error.
				go = true;
			}
		}
		void quit() {
			go = false;
			synchronized (this) { try { notify(); } catch (Exception e) { e.printStackTrace(); } }
		}
		String eval(String text) {
			try {
				synchronized (this) {
					this.text = text.trim();
					notify();
				}
				Thread.yield();
				working = true;
				while (working) {
					try { Thread.currentThread().sleep(100); } catch (Exception e) {}
					String res = result;
					result = null;
					return res;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}
		public void run() {
			setup();
			while (go) {
				synchronized (this) {
					StringBuffer sb = null;
					try {
						wait();
						if (null == text) {
							working = false;
							continue;
						}

						sb = parse(text);

					} catch (Throwable t) {
						error = t;
					} finally {
						// This clause gets excuted:
						//  - after a Throwable error
						//  - after calling continue and break ... inside the try { } catch, if they affect stuff outside the block
						//  - after a return call within the try { catch } block !!
						working = false;
						synchronized (lock) {
							if (null == sb) result = null;
							else {
								// remove last newline char, since it will be added again
								if (sb.length() > 0) sb.setLength(sb.length()-1);
								result = sb.toString();
							}
							text = null;
							lock.notify();
						}
						notify();
					}
				}
			}
			cleanup();
			loaded = false;
		}
	}

	static private void init() throws Throwable {
		// Copying nearly literally from the clojure.lang.Repl class by Rich Hickey
		RT.init();

		//*ns* must be thread-bound for in-ns to work
		//thread-bind *warn-on-reflection* so it can be set!
		//must have corresponding popThreadBindings in finally clause
		Var.pushThreadBindings(
				RT.map(new Object[]{ns, ns.get(),
				       warn_on_reflection, warn_on_reflection.get()}));

		//create and move into the user namespace
		in_ns.invoke(USER);
		refer.invoke(CLOJURE);
	}

	static private void cleanup() {
		Var.popThreadBindings();
		thread = null;
	}

	/** Will destroy the thread and cleanup if the interpreter is not loaded. */
	static protected void destroy() {
		// synchronized with the run(String arg) method
		synchronized (EOF) {
			if (!loaded && null != thread) thread.quit();
		}
	}

	/** Evaluates the clojure code in @param text and appends a newline char to each returned token. */
	static private StringBuffer parse(final String text) throws Throwable {
		// prepare input for parser
		final LineNumberingPushbackReader lnpr = new LineNumberingPushbackReader(new StringReader(text));
		// storage for readout
		final StringWriter sw = new StringWriter();

		while (true) {
			// read one token from the pipe
			Object r = LispReader.read(lnpr, false, EOF, false);
			if (EOF == r) {
				break;
			}
			// evaluate the tokens returned by the LispReader
			Object ret = Compiler.eval(r);
			// print the result in a lispy way
			if (null != ret) {
				RT.print(ret, sw);
				sw.write('\n');
			}
		}
		return sw.getBuffer();
	}
}
