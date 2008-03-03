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
import ij.plugin.PlugIn;
import ij.IJ;
import ij.gui.GenericDialog;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultFocusManager;
import javax.swing.FocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.Toolkit;
import java.awt.FileDialog;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Collections;
import org.python.util.PythonInterpreter;
import org.python.core.PyObject;

/** A dynamic Jython interpreter for ImageJ.
 *	It'd be nice to have TAB expand ImageJ class names and methods.
 *
 *	Version: 2008-02-25 12:!2
 *
 *	$ PATH=/usr/local/jdk1.5.0_14/bin:$PATH javac -classpath .:../../ij.jar:../jython21/jython.jar Jython_Interpreter.java Refresh_Jython_List.java
 *	$ jar cf Jython_Interpreter.jar *class plugins.config
 */
public class Jython_Interpreter implements PlugIn {
	
	final JFrame window = new JFrame("Jython interpreter");
	final JTextArea screen = new JTextArea();
	final JTextArea prompt = new JTextArea(1, 60);//new JTextField(60);
	int active_line = 0;
	final ArrayList al_lines = new ArrayList();
	final PythonInterpreter pi = new PythonInterpreter();
	final ByteArrayOutputStream byte_out = new ByteArrayOutputStream();
	final BufferedOutputStream out = new BufferedOutputStream(byte_out);
	Thread reader;
	boolean reader_run = true;
	JPopupMenu popup_menu;
	/** Store class and def definitions to be executed as a block*/
	String multiline = "";
	String selection = null;
	String last_dir = ij.Menus.getPlugInsPath();//ij.Prefs.getString(ij.Prefs.DIR_IMAGE);
	RunOnEnter runner;

	private void p(String msg) {
		System.out.println(msg);
	}

	public void run(String arghhh) {
		//redirect stdout and stderr to the screen for the interpreter
		pi.setOut(out);
		pi.setErr(out);
		// make GUI
		makeGUI();
		// start thread to write stdout and stderr to the screen
		reader = new Thread("out_reader") {
			public void run() {
				while(reader_run) {
					String output = byte_out.toString(); // this should go with proper encoding 8859-1 or whatever is called
					if (output.length() > 0) {
						screen.append(output + "\n");
						screen.setCaretPosition(screen.getDocument().getLength());
						byte_out.reset();
					}
					try {
						sleep(500);
					} catch (InterruptedException ie) {}
				}
			}
		};
		reader.start();
		//pre-import all ImageJ java classes and TrakEM2 java classes
		String msg = importAll(pi);
		screen.append(msg);
		runner = new RunOnEnter(this);
	}

	void makeGUI() {
		//JPanel panel = new JPanel();
		//panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		//screen.setEditable(false);
		screen.setLineWrap(true);
		Font font = new Font("Courier", Font.PLAIN, 12);
		screen.setFont(font);
		popup_menu = new JPopupMenu();
		ActionListener menu_listener = new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					if (null == selection) return;
					//this is crude as it may interfere with the user data in strings.
					selection = selection.replaceAll(">>> ", "");
					selection = selection.replaceAll("\\.\\.\\. ", "");
					String command = ae.getActionCommand();
					if (command.equals("Copy")) {
						Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
						Transferable transfer = new StringSelection(selection);
						cb.setContents(transfer, (ClipboardOwner)transfer);
					} else if (command.equals("Execute")) {
						try {
							PyObject py_ob = pi.eval(selection);
							if (null != py_ob) {
								screen.append(py_ob.toString() + "\n");
							}
						} catch (Exception e) {
							pi.exec(selection);
						}
					} else if (command.equals("Save")) {
						FileDialog fd = new FileDialog(window, "Save", FileDialog.SAVE);
						fd.setDirectory(last_dir);
						fd.show();
						if (null != last_dir) last_dir = fd.getDirectory();
						String file_name = fd.getFile();
						if (null != file_name) {
							String path = last_dir + file_name;
							try {
								File file = new File(path);
								//this check is done anyway by the FileDialog, but just in case in some OSes it doesn't:
								while (file.exists()) {
									GenericDialog gd = new GenericDialog("File exists!");
									gd.addMessage("File exists! Choose another name or overwrite.");
									gd.addStringField("New file name: ", file_name);
									gd.addCheckbox("Overwrite", false);
									gd.showDialog();
									if (gd.wasCanceled()) return;
									file = new File(last_dir + gd.getNextString());
								}
								DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file), selection.length()));
								dos.writeBytes(selection);
								dos.flush();
							} catch (Exception e) {
								IJ.log("ERROR: " + e);
							}
						}
						
					}
				}
		};
		addMenuItem(popup_menu, "Copy", menu_listener);
		addMenuItem(popup_menu, "Execute", menu_listener);
		addMenuItem(popup_menu, "Save", menu_listener);
		JScrollPane scroll_prompt = new JScrollPane(prompt);
		scroll_prompt.setPreferredSize(new Dimension(440, 35));
		prompt.setFont(font);
		prompt.setLineWrap(true);
		prompt.getInputMap().put(KeyStroke.getKeyStroke("DOWN"), "down");
		prompt.getActionMap().put("down",
				new AbstractAction("down") {
					public void actionPerformed(ActionEvent ae) {
						
						//enable to scroll within lines when the prompt consists of multiple lines.
						/* //doesn't work, can't make it work
						if (0 != (ae.getModifiers() & ActionEvent.SHIFT_MASK)) {
							String text = prompt.getText();
							if (-1 != text.indexOf('\n')) {
								//do normal arrow work:
								KeyListener[] kl = prompt.getKeyListeners();
								kl[0].keyPressed(new KeyEvent(prompt, ae.getID(), ae.getWhen(), ae.getModifiers(), KeyEvent.VK_DOWN));
							}
						}
						*/
						//move forward only if it is possible
						int size = al_lines.size();
						if (active_line < size -1) {
							active_line++;
						} else if (active_line == size -1) {
							prompt.setText(""); //clear
							return;
						}
						prompt.setText((String)al_lines.get(active_line));
					}
				});
		prompt.getInputMap().put(KeyStroke.getKeyStroke("UP"), "up");
		prompt.getActionMap().put("up",
				new AbstractAction("up") {
					public void actionPerformed(ActionEvent ae) {
						int size = al_lines.size();
						if (active_line > 0) {
							if (prompt.getText().equals("") && size -1 == active_line) {
								active_line = size - 1;
							} else {
								active_line--;
							}
						}
						prompt.setText((String)al_lines.get(active_line));
					}
				});
		prompt.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "enter");
		prompt.getActionMap().put("enter",
				new AbstractAction("enter") {
					public void actionPerformed(ActionEvent ae) {
						runner.doEnter();
					}
				});
		DefaultFocusManager manager = new DefaultFocusManager() {
			public void processKeyEvent(Component focusedComponent, KeyEvent ke) {
				if (ke.getKeyCode() == KeyEvent.VK_TAB) {
					//cancelling TAB actions on focus issues
					return;
				}
				//for others call super
				super.processKeyEvent(focusedComponent, ke);
			}
		};
		FocusManager.setCurrentManager(manager);
		prompt.getInputMap().put(KeyStroke.getKeyStroke("TAB"), "tab");
		prompt.getActionMap().put("tab",
				new AbstractAction("tab") {
					public void actionPerformed(ActionEvent ae) {
						doTab(ae);
					}
				});
		screen.addMouseListener(
				new MouseAdapter() {
					public void mouseReleased(MouseEvent me) {
						selection = screen.getSelectedText();
						//show popup menu
						if (null != selection && 0 < selection.length()) {
							popup_menu.show(screen, me.getX(), me.getY());
						}
						//set focus to prompt
						prompt.requestFocus();
					}
				});
		//make scroll for the screen
		JScrollPane scroll = new JScrollPane(screen);
		scroll.setPreferredSize(new Dimension(440,400));
		//set layout
		/*
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1.0;
		c.gridwidth = GridBagConstraints.REMAINDER; //end row
		//apply layout to prompt
		gridbag.setConstraints(scroll_prompt, c);
		//make screen to fill all available space
		c.fill = GridBagConstraints.BOTH;
		c.weighty = 1.0;
		gridbag.setConstraints(scroll, c);
		
		//setup the panel
		panel.setLayout(gridbag);//(new BoxLayout(p, BoxLayout.Y_AXIS));
		panel.add(scroll);
		panel.add(scroll_prompt);
		*/

		JSplitPane panel = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scroll, scroll_prompt);
		panel.setBorder(BorderFactory.createEmptyBorder(4,4,4,4));
		//add the panel to the window
		window.getContentPane().add(panel);
		//setup window display
		window.setSize(450, 450);
		window.pack();
		//set location to bottom right corner
		Rectangle screenBounds = window.getGraphicsConfiguration().getBounds();
		int x = screenBounds.width - window.getWidth() - 35;
		int y = screenBounds.height - window.getHeight() - 35;
		window.setLocation(x, y);
		//add windowlistener
		window.addWindowListener(
				new WindowAdapter() {
					public void windowClosing(WindowEvent we) {
						runner.quit();
						reader_run = false;
						pi.setOut(System.out);
						pi.setErr(System.err);
					}
				}
		);
		//show the window
		window.setVisible(true);
		//set the focus to the input prompt
		prompt.requestFocus();
	}

	void addMenuItem(JPopupMenu menu, String label, ActionListener listener) {
		JMenuItem item = new JMenuItem(label);
		item.addActionListener(listener);
		menu.add(item);
	}

	private class RunOnEnter extends Thread {
		private Object lock = new Object();
		private boolean go = true;
		private Jython_Interpreter ji;
		RunOnEnter(Jython_Interpreter ji) {
			this.ji = ji;
			setPriority(Thread.NORM_PRIORITY);
			try { setDaemon(true); } catch (Exception e) { e.printStackTrace(); }
			start();
		}
		public void quit() {
			go = false;
			synchronized (this) { notify(); }
		}
		public void doEnter() {
			prompt.setEnabled(false);
			synchronized (this) { notify(); }
		}
		public void run() {
			while (go) {
				try {
					synchronized (this) { wait(); }
					if (!go) return;
					ji.doEnter();
				 } catch (Exception e) {
					 e.printStackTrace();
				 } finally {
					prompt.setEnabled(true);
					window.setVisible(true);
					prompt.requestFocus();
					// set caret position at the end of the prompt tabs
					String mb = prompt.getText();
					prompt.setCaretPosition(null == mb ? 0 : mb.length());
				 }
			}
		}
	}


	boolean previous_line_empty = false;
	
	void doEnter() {
		// retrieve text and test preconditions
		String text = prompt.getText();//no need, actually messes up tabs//.trim();
		int len = text.length();
		if (null == text && len <= 0) {
			return;
		}
		// store text
		if (len > 0) { //don't store null entries
			al_lines.add(text);
			active_line = al_lines.size() -1;
		}
		// store in multiline if appropriate for later execution
		int i_colon = text.lastIndexOf(':');
		if ((len > 0 && i_colon == len -1) || 0 != multiline.length()) {
			multiline +=  text + "\n";
			// adjust indentation in prompt
			int n_tabs = 0;
			for (int i=0; i<len; i++) {
				if ('\t' != text.charAt(i)) {	
					break;
				}
				n_tabs++;
			}
			// indent when sentence ends with a ':'
			if (-1 != i_colon) {
				n_tabs++;
			}
			if (1 == n_tabs) {
				prompt.setText("\t");
			} else if (n_tabs > 1) {
				char[] tabs = new char[n_tabs];
				for (int i=0; i <n_tabs; i++) {
					tabs[i] = '\t';
				}
				prompt.setText(new String(tabs));
			}
			// print to screen
			screen.append("... " + fix(text) + "\n");
			screen.setCaretPosition(screen.getDocument().getLength());
			// remove tabs from line:
			text = text.replaceAll("\\t", "");
			len = text.length(); // refresh length
			// test for text contents
			if (0 == len) {
				if (previous_line_empty) {
					text = multiline; //execute below
					multiline = "";
					previous_line_empty = false;
				} else {
					previous_line_empty = true;
				}
			} else {
				//don't eval/exec yet
				return;
			}
		} else {
			screen.append(">>> " + text + "\n");
			screen.setCaretPosition(screen.getDocument().getLength());
		}
		//try to eval, and if it fails, then it's code for exec
		try {
			PyObject py_ob = pi.eval(text);
			if (null != py_ob) {
				screen.append(py_ob.toString() + "\n");
				screen.setCaretPosition(screen.getDocument().getLength());
			}
		} catch (Exception e) {
			//execute, since eval failed
			pi.exec(fix(text));
		} finally {
			//remove tabs from prompt
			prompt.setText("");
			// reset tab expansion
			last_tab_expand = null;
		}
	}

	String fix(String text) {
		String t = text.replaceAll("\\\\n", "\n");
		t = t.replaceAll("\\\\t", "\t");
		return t;
	}

	/** Insert a tab in the prompt (in replacement for Component focus)*/
	synchronized void doTab(ActionEvent ae) {
		String prompt_text = prompt.getText();
		int len = prompt_text.length();
		boolean add_tab = true;
		for (int i=0; i<len; i++) {
			char c = prompt_text.charAt(i);
			if ('\t' != c) {
				add_tab = false;
				break;
			}
		}
		if (add_tab) {
			prompt.setText(prompt_text + "\t");
		} else {
			// attempt to expand the variable name, if possible
			expandName(prompt_text, ae);
		}
	}

	private String extractWordStub(final String prompt_text, final int caret_position) {
		final char[] c = new char[]{' ', '.', '(', ',', '['};
		final int[] cut = new int[c.length];
		for (int i=0; i<cut.length; i++) {
			cut[i] = prompt_text.lastIndexOf(c[i], caret_position);
		}
		Arrays.sort(cut);
		int ifirst = cut[cut.length-1] + 1;
		if (-1 == ifirst) return null;
		//p(ifirst + "," + caret_position + ", " + prompt_text.length());
		return prompt_text.substring(ifirst, caret_position);
	}

	private void expandName(String prompt_text, ActionEvent ae) {
		if (null != last_tab_expand) {
			last_tab_expand.cycle(ae);
			return;
		}
		if (null == prompt_text) prompt_text = prompt.getText();
		int ilast = prompt.getCaretPosition() -1;
		// check preconditions
		if (ilast <= 0) return;
		char last = prompt_text.charAt(ilast);
		if (' ' == last || '\t' == last) {
			p("last char is space or tab");
			return;
		}
		// parse last word stub
		String stub = extractWordStub(prompt_text, ilast+1);
		// check against all existing variables, which fortunately includes imported class names
		/* // WORKS, but there is a better way
		StringBuffer py = new StringBuffer("def __promptstubexpand(allvars):\n")
		.append("\t__stubexpansionresult = []\n")
		.append("\tfor k,v in allvars.iteritems():\n")
		.append("\t\tif k.startswith('").append(stub).append("'):\n")
		.append("\t\t\t__stubexpansionresult.append(k)\n")
		.append("\t\t\tprint k\n")
		.append("\treturn __stubexpansionresult\n")
		;
		pi.exec(py.toString());
		PyObject py_ob = pi.eval("__promptstubexpand(vars())\n");
		if (null == py_ob) return;
		String[] list = (String[])py_ob.__tojava__(String[].class);
		for (int i=0; i<list.length; i++) {
			System.out.println(list[i]);
		}
		*/
		/*  DOES NOT WORK, not a dictionary/hashtable, pyt a PySingleton (?)
		PyObject py_ob = pi.eval("vars()");
		if (null == py_ob) return;
		Hashtable ht = (Hashtable)py_ob.__tojava__(Hashtable.class);
		for (Iterator it = ht.keySet().iterator(); it.hasNext(); ) {
			String key = (String)it.next();
			if (key.startsWith(stub)) {
				System.out.println(key);
			}
		}
		*/
		// Easiest and safest way:
		PyObject py_vars = pi.eval("vars().keys()");
		if (null == py_vars) {
			p("No vars to search into");
			return;
		}
		String[] vars = (String[])py_vars.__tojava__(String[].class);
		ArrayList al = new ArrayList();
		for (int i=0; i<vars.length; i++) {
			if (vars[i].startsWith(stub)) {
				//System.out.println(vars[i]);
				al.add(vars[i]);
			}
		}
		Collections.sort(al, String.CASE_INSENSITIVE_ORDER);
		System.out.println("stub: '" + stub + "'");
		if (al.size() > 0) {
			last_tab_expand = new TabExpand(al, ilast - stub.length() + 1, stub);
		} else {
			last_tab_expand = null;
		}
	}

	private TabExpand last_tab_expand = null;

	private class TabExpand {
		ArrayList al = new ArrayList();
		int i = 0;
		int istart; // stub starting index
		int len_prev; // length of previously set word
		String stub;
		TabExpand(ArrayList al, int istart, String stub) {
			this.al.addAll(al);
			this.istart = istart;
			this.stub = stub;
			this.len_prev = stub.length();
			cycle(null);
		}
		void cycle(ActionEvent ae) {
			if (null == ae) {
				// first time
				set();
				return;
			}

			/*
			p("##\nlen_prev: " + len_prev);
			p("i : " + i);
			p("prompt.getText(): " + prompt.getText());
			p("prompt.getText().length(): " + prompt.getText().length());
			p("istart: " + istart + "\n##");
			*/

			int plen = prompt.getText().length();
			String stub = extractWordStub(prompt.getText(), this.istart + len_prev > plen ? plen : this.istart + len_prev); // may be null
			if (this.stub.equals(stub) || al.get(i).equals(stub)) {
				// ok
			} else {
				// can't expand, remake
				last_tab_expand = null;
				expandName(prompt.getText(), ae);
				return;
			}

			// check preconditions
			if (0 == al.size()) {
				p("No elems to expand to");
				return;
			}

			// ok set prompt to next
			i += ( 0 != (ae.getModifiers() & ActionEvent.SHIFT_MASK) ? -1 : 1);
			if (al.size() == i) i = 0;
			if (-1 == i) i = al.size() -1;
			set();
		}
		private void set() {
			String pt = prompt.getText();
			if (i > 0) p("set to " + al.get(i));
			prompt.setText(pt.substring(0, istart) + al.get(i).toString() + pt.substring(istart + len_prev));
			len_prev = ((String)al.get(i)).length();
		}
	}

	/** pre-import all ImageJ java classes and TrakEM2 java classes */
	static public String importAll(PythonInterpreter pi) {
		pi.exec("from ij import *\nfrom ij.gui import *\nfrom ij.io import *\nfrom ij.macro import *\nfrom ij.measure import *\nfrom ij.plugin import *\nfrom ij.plugin.filter import *\nfrom ij.plugin.frame import *\nfrom ij.process import *\nfrom ij.text import *\nfrom ij.util import *\n");
		String msg = "All ImageJ";
		try {
			pi.exec("from ini.trakem2 import *\nfrom ini.trakem2.persistence import *\nfrom ini.trakem2.tree import *\nfrom ini.trakem2.display import *\nfrom ini.trakem2.imaging import *\nfrom ini.trakem2.io import *\nfrom ini.trakem2.utils import *\nfrom ini.trakem2.vector import *\nfrom mpi.fruitfly.analysis import *\nfrom mpi.fruitfly.fft import *\nfrom mpi.fruitfly.general import *\nfrom mpi.fruitfly.math import *\nfrom mpi.fruitfly.math.datastructures import *\nfrom mpi.fruitfly.registration import *\n");
			msg += " and TrakEM2";
		} catch (Exception e) { /*fail silently*/ }
		return msg + " classes imported.\n";
	}
}
