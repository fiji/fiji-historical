package common;

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
import java.io.PrintWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.io.Writer;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Map;
import java.util.Iterator;
import java.util.Collections;
import java.util.regex.Pattern;
import java.util.Scanner;

public abstract class AbstractInterpreter implements PlugIn {

	final protected JFrame window = new JFrame("Interpreter");
	final protected JTextArea screen = new JTextArea();
	final protected JTextArea prompt = new JTextArea(1, 60);//new JTextField(60);
	protected int active_line = 0;
	final protected ArrayList al_lines = new ArrayList();
	final protected ArrayList<Boolean> valid_lines = new ArrayList<Boolean>();
	final protected ByteArrayOutputStream byte_out = new ByteArrayOutputStream();
	final protected BufferedOutputStream out = new BufferedOutputStream(byte_out);
    final protected PrintWriter print_out = new PrintWriter(out);
	Thread reader;
	boolean reader_run = true;
	protected JPopupMenu popup_menu;
	String last_dir = ij.Menus.getPlugInsPath();//ij.Prefs.getString(ij.Prefs.DIR_IMAGE);
	protected ExecuteCode runner;

	static final protected Hashtable<Class,AbstractInterpreter> instances = new Hashtable<Class,AbstractInterpreter>();

	static {
		// Save history of all open interpreters even in the case of a call to System.exit(0),
		// which doesn't spawn windowClosing events.
		Runtime.getRuntime().addShutdownHook(new Thread() { public void run() {
			for (Map.Entry<Class,AbstractInterpreter> e : instances.entrySet()) {
				e.getValue().closingWindow();
			}
		}});
	}

	/** Convenient System.out.prinln(text); */
	protected void p(String msg) {
		System.out.println(msg);
	}

	protected void setTitle(String title) {
		window.setTitle(title);
	}

	public void run(String arghhh) {
		AbstractInterpreter instance = instances.get(getClass());
		if (null != instance) {
			instance.window.setVisible(true);
			instance.window.toFront();
			/*
			String name = instance.getClass().getName();
			int idot = name.lastIndexOf('.');
			if (-1 != idot) name = name.substring(idot);
			IJ.showMessage("The " + name.replace('_', ' ') + " is already open!");
			*/
			return;
		}
		instances.put(getClass(), this);

		System.out.println("Open interpreters:");
		for (Map.Entry<Class,AbstractInterpreter> e : instances.entrySet()) {
			System.out.println(e.getKey() + " -> " + e.getValue());
		}

		ArrayList[] hv = readHistory();
		al_lines.addAll(hv[0]);
		valid_lines.addAll(hv[1]);
		active_line = al_lines.size();
		if (al_lines.size() != valid_lines.size()) {
			IJ.log("ERROR in parsing history!");
			al_lines.clear();
			valid_lines.clear();
			active_line = 0;
		}

		// make GUI
		makeGUI();
		// start thread to write stdout and stderr to the screen
		reader = new Thread("out_reader") {
			public void run() {
				setPriority(Thread.NORM_PRIORITY);
				while(reader_run) {
					print_out.flush();
					String output = byte_out.toString(); // this should go with proper encoding 8859_1 or whatever is called
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
		runner = new ExecuteCode();
	}

	protected void makeGUI() {
		//JPanel panel = new JPanel();
		//panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		screen.setEditable(false);
		screen.setLineWrap(true);
		Font font = new Font("Courier", Font.PLAIN, 12);
		screen.setFont(font);
		popup_menu = new JPopupMenu();
		ActionListener menu_listener = new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					String selection = screen.getSelectedText();
					if (null == selection) return;
					String sel = filterSelection();
					String command = ae.getActionCommand();
					if (command.equals("Copy")) {
						Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
						Transferable transfer = new StringSelection(sel);
						cb.setContents(transfer, (ClipboardOwner)transfer);
					} else if (command.equals("Execute")) {
						runner.execute(sel);
					} else if (command.equals("Save")) {
						FileDialog fd = new FileDialog(window, "Save", FileDialog.SAVE);
						fd.setDirectory(last_dir);
						fd.setVisible(true);
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
								DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file), sel.length()));
								dos.writeBytes(sel);
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
						//move forward only if it is possible
						int size = al_lines.size();
						if (0 == size) return;
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
						final int size = al_lines.size();
						if (0 == size) return;
						// Store current prompt content if not empty and is different that last stored line
						if (size -1 == active_line) {
							String txt = prompt.getText();
							if (null != txt && txt.trim().length() > 0 && !txt.equals((String)al_lines.get(active_line))) {
								al_lines.add(txt);
								valid_lines.add(false); // because it has never been executed yet
							}
						}

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
		prompt.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, ActionEvent.SHIFT_MASK), "shift+down");
		prompt.getActionMap().put("shift+down",
				new AbstractAction("shift+down") {
					public void actionPerformed(ActionEvent ae) {
						//enable to scroll within lines when the prompt consists of multiple lines.
						String text = prompt.getText();
						if (-1 != text.indexOf('\n')) {
							//do normal arrow work:
							// Caret can be from 0 to text.length() inclusive (i.e. after the last char)
							int cp = prompt.getCaretPosition();
							// next newline
							int next_nl = text.indexOf('\n', cp);
							if (-1 == next_nl) return; // can't scroll down
							// previous newline
							int prev_nl = text.lastIndexOf('\n', cp-1);
							if (-1 == prev_nl) prev_nl = -1; // caret at first char of first line
							// distance from prev_nl to caret
							int column = cp - prev_nl;
							// second next newline
							int next_nl_2 = text.indexOf('\n', next_nl+1);
							if (-1 == next_nl_2) next_nl_2 = text.length();
							// new caret position
							if (column > next_nl_2 - next_nl) cp = next_nl_2;
							else cp = next_nl + column;
							// check boundaries
							if (cp > text.length()) cp = text.length();
							//
							prompt.setCaretPosition(cp);
						}
					}
				});
		prompt.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, ActionEvent.SHIFT_MASK), "shift+up");
		prompt.getActionMap().put("shift+up",
				new AbstractAction("shift+up") {
					public void actionPerformed(ActionEvent ae) {
						//enable to scroll within lines when the prompt consists of multiple lines.
						String text = prompt.getText();
						if (-1 != text.indexOf('\n')) {
							//do normal arrow work:
							int cp = prompt.getCaretPosition();
							// next newline
							int next_nl = text.indexOf('\n', cp);
							if (-1 == next_nl) next_nl = text.length(); // imaginary
							// previous newline
							int prev_nl = text.lastIndexOf('\n', cp -1);
							if (-1 == prev_nl) return; // already at first row
							// distance from prev_nl to caret
							int column = cp - prev_nl;
							// second previous newline
							int prev_nl_2 = text.lastIndexOf('\n', prev_nl -1);
							// if -1 == prev_nl_2 it's ok: means we are at second row
							if (column > prev_nl - prev_nl_2) cp = prev_nl;
							else cp = prev_nl_2 + column;
							//
							prompt.setCaretPosition(cp);
						}
					}
				});
		prompt.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "enter");
		prompt.getActionMap().put("enter",
				new AbstractAction("enter") {
					public void actionPerformed(ActionEvent ae) {
						runner.executePrompt();
					}
				});
		prompt.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, ActionEvent.SHIFT_MASK), "shift+enter");
		prompt.getActionMap().put("shift+enter",
				new AbstractAction("shift+enter") {
					public void actionPerformed(ActionEvent ae) {
						// allow multiline input on shift+enter
						int cp = prompt.getCaretPosition();
						prompt.insert("\n", cp);
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
						String selection = screen.getSelectedText();
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
						closingWindow();
					}
					public void windowClosed(WindowEvent we) {
						closingWindow();
					}
				}
		);
		//show the window
		window.setVisible(true);
		//set the focus to the input prompt
		prompt.requestFocus();
	}

	private void closingWindow() {
		// Check if not closed already
		if (!instances.containsKey(getClass())) {
			return;
		}
		// Before any chance to fail, remove from hashtable of instances:
		instances.remove(getClass());
		// ... and store history
		saveHistory();
		//
		AbstractInterpreter.this.windowClosing();
		runner.quit();
		reader_run = false;
	}

	void addMenuItem(JPopupMenu menu, String label, ActionListener listener) {
		JMenuItem item = new JMenuItem(label);
		item.addActionListener(listener);
		menu.add(item);
	}

	private class ExecuteCode extends Thread {
		private Object lock = new Object();
		private boolean go = true;
		private String text = null;
		private boolean store = false; // only true when invoked from a prompt
		ExecuteCode() {
			setPriority(Thread.NORM_PRIORITY);
			try { setDaemon(true); } catch (Exception e) { e.printStackTrace(); }
			start();
		}
		public void quit() {
			go = false;
			synchronized (this) { notify(); }
		}
		public void execute(String text) {
			this.text = text;
			this.store = false;
			synchronized (this) { notify(); }
		}
		public void executePrompt() {
			prompt.setEnabled(false);
			this.text = prompt.getText();
			this.store = true;
			synchronized (this) { notify(); }
		}
		public void run() {
			AbstractInterpreter.this.threadStarting();
			while (go) {
				try {
					synchronized (this) { wait(); }
					if (!go) return;
					AbstractInterpreter.this.execute(text, store);
				 } catch (Exception e) {
					 e.printStackTrace();
				 } finally {
					if (!go) return; // this statement is reached when returning from the middle of the try/catch!
					window.setVisible(true);
					if (store) {
						prompt.setEnabled(true);
						prompt.requestFocus();
						// set caret position at the end of the prompt tabs
						String mb = prompt.getText();
						prompt.setCaretPosition(null == mb ? 0 : mb.length());
					}
					text = null;
					store = false;
				 }
			}
			AbstractInterpreter.this.threadQuitting();
		}
	}

	boolean previous_line_empty = false;

	protected void execute(String text, boolean store) {
		if (null == text) return;
		int len = text.length();
		if (len <= 0) {
			print(">>>");
			return;
		}
		// store text
		if (len > 0 && store) {
			// only if different than last line
			if (al_lines.isEmpty() || !al_lines.get(al_lines.size()-1).equals(text)) {
				al_lines.add(text);
				valid_lines.add(false);
			}
			active_line = al_lines.size() -1;
		}
		// store in multiline if appropriate for later execution
		/*
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
			print("... " + fix(text));
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
		*/
			print(">>> " + text);
		/*
		}
		*/
		try {
			Object ob = eval(text);
			if (null != ob) {
				print(ob.toString());
			}
			// if no error, mark as valid
			valid_lines.set(valid_lines.size() -1, true);
		} catch (Throwable e) {
			e.printStackTrace(print_out);
		} finally {
			//remove tabs from prompt
			prompt.setText("");
			// reset tab expansion
			last_tab_expand = null;
		}
	}

	/** Prints to screen: will append a newline char to the text, and also scroll down. */
	protected void print(String text) {
		screen.append(text + "\n");
		screen.setCaretPosition(screen.getDocument().getLength());
	}

	abstract protected Object eval(String text) throws Throwable;

	/** Expects a '#' for python and ruby, a ';' for lisp, a '//' for javascript, etc. */
	abstract protected String getLineCommentMark();

	/** Executed when the interpreter window is being closed. */
 	protected void windowClosing() {}

	/** Executed inside the executer thread before anything else. */
	protected void threadStarting() {}

	/** Executed inside the executer thread right before the thread will die. */
	protected void threadQuitting() {}

	/** Enable tab chars in the prompt. */
	protected String fix(String text) {
		String t = text.replaceAll("\\\\n", "\n");
		t = t.replaceAll("\\\\t", "\t");
		return t;
	}

	/** Insert a tab in the prompt (in replacement for Component focus)*/
	synchronized protected void doTab(ActionEvent ae) {
		String prompt_text = prompt.getText();
		int cp = prompt.getCaretPosition();
		if (cp > 0) {
			char cc = prompt_text.charAt(cp-1);
			if ('t' == cc || '\n' == cc) {
				prompt.setText(prompt_text.substring(0, cp) + "\t" + prompt_text.substring(cp));
				return;
			}
		}
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
			prompt.append("\t");
		} else {
			// attempt to expand the variable name, if possible
			expandName(prompt_text, ae);
		}
	}

	/** Optional word expansion. */
	protected ArrayList expandStub(String stub) {
		return new ArrayList(); // empty
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
		ArrayList al = expandStub(stub);
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

	private String filterSelection() {
		String sel = screen.getSelectedText().trim();

		StringBuffer sb = new StringBuffer();
		int istart = 0;
		int inl = sel.indexOf('\n');
		int len = sel.length();
		Pattern pat = Pattern.compile("^>>> .*$");

		while (true) {
			if (-1 == inl) inl = len -1;
			// process line:
			String line = sel.substring(istart, inl+1);
			if (pat.matcher(line).matches()) {
				line = line.substring(5);
			}
			sb.append(line);
			// quit if possible
			if (len -1 == inl) break;
			// prepate next
			istart = inl+1;
			inl = sel.indexOf('\n', istart);
		};

		if (0 == sb.length()) return sel;
		return sb.toString();
	}

	private void saveHistory() {
		String path = ij.Prefs.getPrefsDir() + "/" + getClass().getName() + ".log";
		File f = new File(path);
		if (!f.getParentFile().canWrite()) {
			IJ.log("Could not save history for " + getClass().getName() + "\nat path: " + path);
			return;
		}
		Writer writer = null;
		try {
			writer = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(f)), "8859_1");

			final int MAX_LINES = 2000;

			// Write all lines up to MAX_LINES
			int first = al_lines.size() - MAX_LINES;
			if (first < 0) first = 0;
			String rep = new StringBuffer().append('\n').append(getLineCommentMark()).toString();
			String separator = getLineCommentMark() + "\n";
			for (int i=first; i<al_lines.size(); i++) {
				// Separate executed code blocks with a empty comment line:
				writer.write(separator);
				String block = (String)al_lines.get(i);
				// If block threw an Exception when executed, save it as commented out:
				if (!valid_lines.get(i)) {
					block = getLineCommentMark() + block;
					block = block.replaceAll("\n", rep);
				}
				if (!block.endsWith("\n")) block += "\n";
				writer.write(block);
			}
			writer.flush();
		} catch (Throwable e) {
			IJ.log("Could NOT save history log file!");
			IJ.log(e.toString());
		} finally {
			try {
				writer.close();
			} catch (java.io.IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}

	private ArrayList[] readHistory() {
		String path = ij.Prefs.getPrefsDir() + "/" + getClass().getName() + ".log";
		File f = new File(path);
		ArrayList blocks = new ArrayList();
		ArrayList valid = new ArrayList();
		if (!f.exists()) {
			System.out.println("No history exists yet for " + getClass().getName());
			return new ArrayList[]{blocks, valid};
		}
		final String sep = getLineCommentMark() + "\n";
		Scanner scanner = null;
		try {
			scanner = new Scanner(new File(path), "8859_1").useDelimiter(sep);
			while (scanner.hasNext()) {
				String block = scanner.next();
				int inl = block.lastIndexOf('\n');
				int end = block.length() == inl + 1 ? inl : block.length();
				if (0 == block.indexOf(sep)) block = block.substring(sep.length(), end);
				else block = block.substring(0, end);
				blocks.add(block);
				valid.add(true); // all valid, even if they were not: the invalid ones are commented out
			}
		} catch (Throwable e) {
			IJ.log("Could NOT read history log file!");
			IJ.log(e.toString());
		} finally {
			scanner.close();
		}
		return new ArrayList[]{blocks, valid};
	}
}
