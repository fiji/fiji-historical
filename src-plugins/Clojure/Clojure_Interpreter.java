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
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PushbackReader;

import common.AbstractInterpreter;

public class Clojure_Interpreter extends AbstractInterpreter {

	static final Symbol USER = Symbol.create("user");
	static final Symbol CLOJURE = Symbol.create("clojure");
	static final Var in_ns = RT.var("clojure", "in-ns");
	static final Var refer = RT.var("clojure", "refer");
	static final Var ns = RT.var("clojure", "*ns*");
	static final Var warn_on_reflection = RT.var("clojure", "*warn-on-reflection*");

	static private boolean loaded = false;

	private LispThread thread = null;

	synchronized public void run(String arg) {
		if (loaded) {
			IJ.showMessage("You can only have one instance of the Clojure interpreter running.");
			return;
		}

		loaded = true;
		final LispThread thread = new LispThread();
		if (!thread.ready()) {
			p("Some error ocurred.");
			return;
		}
		this.thread = thread;
		// ok create window
		super.run(arg);
		super.window.setTitle("Clojure Interpreter");

		super.window.addWindowListener(
			new WindowAdapter() {
				public void windowClosing(WindowEvent we) {
					thread.quit();
				}
			}
		);
	}

	/** Evaluate clojure code. */
	protected Object eval(final String text) throws Exception {
		Object ret = thread.eval(text);
		thread.throwException();
		return ret;
	}

	/** Complicated Thread setup just to be able to initialize and cleanup within the context of the same Thread, as required by Clojure. */
	private class LispThread extends Thread {

		PipedReader reader;
		PipedWriter writer;
		PipedReader r2;
		PipedWriter w2;
		LineNumberingPushbackReader rdr;
		final Object EOF = new Object();
		PushbackReader pr;
		final Object lock = new Object();

		private boolean go = false;
		private String text = null;
		private String result = null;
		private Exception error = null;
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
				return null != rdr;
			}
		}
		void throwException() throws Exception {
			synchronized (this) {
				if (null == error) return;
				Exception e = error;
				error = null;
				throw e;
			}
		}
		private void setup() {
			synchronized (this) {
				try {
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

					// create piping system
					reader = new PipedReader();
					writer = new PipedWriter(reader);
					rdr = new LineNumberingPushbackReader(reader);
					r2 = new PipedReader();
					w2 = new PipedWriter(r2);

				} catch (Exception e) {
					e.printStackTrace();
					rdr = null;
				}
				go = true;
			}
		}
		void quit() {
			go = false;
			synchronized (this) { try { notify(); wait(); } catch (Exception e) { e.printStackTrace(); } }
		}
		String eval(String text) {
			try {
				synchronized (this) {
					this.text = text;
					notify();
				}
				synchronized (lock) {
					try { lock.wait(); } catch (InterruptedException ie) {}
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
					try {
						wait();
						if (null == text) continue;
						// EVAL
						// send the text to the pipe
						writer.write(text);
						writer.write('\n'); // despite evidence to the contrary, a '-1' does not signal end of pipe for the LineNumberingPushbackReader
						writer.flush();

						// to store the parsed output
						final StringBuffer sb = new StringBuffer();
						Object r;

						while (true) {
							if (!reader.ready()) break;
							// read the text from the pipe
							r = LispReader.read(rdr, false, EOF, false);
							if (EOF == r) {
								p("unexpected EOF");
								break;
							}
							// evaluate the tokens returned by the LispReader
							Object ret = Compiler.eval(r);
							// print the result in a lispy way
							RT.print(ret, w2);
							w2.flush();
							// read out the result for printing to the screen
							while (r2.ready()) sb.append((char)r2.read());
						};
						synchronized (lock) {
							result = sb.toString();
							text = null;
							lock.notify();
						}
					} catch (Exception e) {
						error = e;
						synchronized (lock) { text = null; lock.notify(); }
					}
					notify();
				}
			}
			// cleanup
			Var.popThreadBindings();
			loaded = false;
		}
	}
}
