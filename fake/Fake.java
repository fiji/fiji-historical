import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;

import java.lang.reflect.Method;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import java.util.jar.Manifest;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.JarOutputStream;

import java.util.regex.Pattern;

public class Fake {
	protected static Method javac;

	public static void main(String[] args) {
		new Fake().make(args);
	}

	public void make(String[] args) {
		try {
			Parser parser = new Parser(args);
			Parser.Rule all = parser.parseRules();
			all.make();
		}
		catch (FakeException e) {
			System.err.println(e);
			System.exit(1);
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}


	// the parser

	class Parser {
		public final static String path = "Fakefile";
		BufferedReader reader;
		String line;
		int lineNumber;
		File cwd;
		protected Map allRules = new HashMap();

		public Parser(String[] args) throws FakeException {
			this(args != null && args.length > 0 ? args[0] : path,
					args);
		}

		public Parser(String path, String[] args) throws FakeException {
			try {
				InputStream stream = new FileInputStream(path);
				InputStreamReader input =
					new InputStreamReader(stream);
				reader = new BufferedReader(input);
			} catch (IOException e) {
				error("Could not read file: " + path);
			}

			lineNumber = 0;
			cwd = new File(".");
		}

		public Rule parseRules() throws FakeException {
			Rule result = null;

			for (;;) {
				try {
					line = reader.readLine();
				} catch (IOException e) {
					error("Error reading file");
				}

				if (line == null)
					break;

				lineNumber++;
				line = line.trim();

				if (line.length() == 0 || line.startsWith("#"))
					continue;

				int arrow = line.indexOf("<-");
				if (arrow < 0) {
					int equal = line.indexOf('=');
					if (equal < 0)
						error("Invalid line");
					String key = line.substring(0, equal);
					String val = line.substring(equal + 1);
					setVariable(key.trim(), val.trim());
					continue;
				}

				String target = line.substring(0, arrow).trim();
				String list = line.substring(arrow + 2).trim();
				try {
					Rule rule = addRule(target, list);

					if (result == null)
						result = rule;
				} catch (Exception e) {
					error(e.getMessage());
				}
			}

			if (result == null)
				error("Could not find default rule");

			return result;
		}

		public Rule addRule(String target, String prerequisites)
				throws FakeException {
			List list = new ArrayList();
			StringTokenizer tokenizer =
				new StringTokenizer(prerequisites);

			while (tokenizer.hasMoreTokens()) {
				String token = tokenizer.nextToken();
				if (expandGlob(token, list) == 0)
					throw new FakeException("Glob did not "
						+ "match any file: '"
						+ token + "'");
			}

			Rule rule = null;

			if (allRules.isEmpty())
				rule = new All(target, list);
			else if (new File(prerequisites).isDirectory())
				rule = new SubFake(target, list);
			else if (target.endsWith(".jar")) {
				if (prerequisites.endsWith(".jar"))
					rule = new CopyJar(target, list);
				else
					rule = new CompileJar(target, list);
			}
			else if (prerequisites.endsWith(".c") ||
					prerequisites.endsWith(".cxx"))
				rule = new CompileCProgram(target, list);
			else if (target.endsWith(".class"))
				rule = new CompileClass(target, list);
			else if (target.endsWith(")")) {
				int paren = target.indexOf('(');

				if (paren < 0)
					throw new FakeException("Invalid rule");

				String program = target.substring(paren + 1,
					target.length() - 1);
				target = target.substring(0, paren).trim();

				rule = new ExecuteProgram(target, list,
					program);
			}

			if (rule == null)
				throw new FakeException("Unrecognized rule");

			allRules.put(rule.target, rule);

			return rule;
		}

		protected void error(String message) throws FakeException {
			throw new FakeException(path + ":" + lineNumber + ": "
					+ message + "\n\t" + line);
		}

		// the different rule types

		abstract class Rule {
			protected String target;
			protected List prerequisites, nonUpToDates;

			Rule(String target, List prerequisites) {
				this.target = target;
				this.prerequisites = prerequisites;
			}

			abstract void action() throws FakeException;

			boolean upToDate() {
				// this implements the mtime check
				File file = new File(cwd, target);
				if (!file.exists()) {
					nonUpToDates = prerequisites;
					return false;
				}
				long targetModifiedTime = file.lastModified();

				nonUpToDates = new ArrayList();
				Iterator iter = prerequisites.iterator();
				while (iter.hasNext()) {
					String prereq = (String)iter.next();
					if (new File(prereq).lastModified()
							> targetModifiedTime)
						nonUpToDates.add(prereq);
				}

				return nonUpToDates.size() == 0;
			}

			void make() throws FakeException {
				try {
					if (upToDate())
						return;
					System.err.println("Faking " + this);
					action();
				} catch (Exception e) {
					new File(target).delete();
					error(e.getMessage());
				}
			}

			protected void error(String message)
					throws FakeException {
				throw new FakeException(message
						+ "\n\tin rule " + this);
			}

			public String getLastPrerequisite() {
				int index = prerequisites.size() - 1;
				return (String)prerequisites.get(index);
			}

			public String toString() {
				String result = "";
				if (verbose) {
					String type = getClass().getName();
					int dollar = type.lastIndexOf('$');
					if (dollar >= 0)
						type = type.substring(dollar + 1);
					result += "(" + type + ") ";
				}
				result += target + " <-";
				Iterator iter = prerequisites.iterator();
				while (iter.hasNext())
					result += " " + iter.next();
				return result;
			}
		}

		class All extends Rule {
			All(String target, List prerequisites) {
				super(target, prerequisites);
			}

			public void action() throws FakeException {
				Iterator iter = prerequisites.iterator();
				while (iter.hasNext()) {
					String prereq = (String)iter.next();

					if (!allRules.containsKey(prereq))
						error("Unknown target: "
							+ prereq);

					Rule rule = (Rule)allRules.get(prereq);
					rule.make();
				}
			}
		}

		class SubFake extends Rule {
			String source;

			SubFake(String target, List prerequisites) {
				super(target, prerequisites);
				source = getLastPrerequisite()
					+ new File(target).getName();
				if (target.endsWith("/"))
					target = target.substring(0,
							target.length() - 1);
			}

			boolean upToDate() {
				return false;
			}

			void action() throws FakeException {
				Iterator iter = prerequisites.iterator();
				while (iter.hasNext())
					action((String)iter.next());
				if (target.indexOf('.') >= 0)
					copyFile(source, target, cwd);
			}

			void action(String directory) throws FakeException {
				String fakeFile = directory + '/' + Parser.path;
				boolean tryFake = new File(fakeFile).exists();
				System.err.println((tryFake ? "F" : "M")
					+ "aking in " + directory + "/");

				try {
					if (tryFake) {
						// Try "Fake"
						Parser parser =
							new Parser(fakeFile,
								null);
						parser.cwd = new File(cwd,
								directory);
						Rule all = parser.parseRules();
						all.make();
					} else
						// Try "make"
						execute(new String[] { "make" },
							new File(directory));
				} catch (Exception e) {
					if (!(e instanceof FakeException))
						e.printStackTrace();
					throw new FakeException((tryFake ?
						"Fake" : "make")
						+ " failed: " + e);
				}
				System.err.println("Leaving " + directory);
			}
		}

		class CopyJar extends Rule {
			String source;
			CopyJar(String target, List prerequisites) {
				super(target, prerequisites);
				source = getLastPrerequisite();
			}

			void action() throws FakeException {
				copyFile(source, target, cwd);
			}

			boolean upToDate() {
				if (super.upToDate())
					return true;

				JarFile targetJar, sourceJar;

				try {
					targetJar = new JarFile(target);
				} catch(IOException e) {
					return false;
				}
				try {
					sourceJar = new JarFile(source);
				} catch(IOException e) {
					return true;
				}

				Enumeration iter = sourceJar.entries();
				while (iter.hasMoreElements()) {
					JarEntry entry =
						(JarEntry)iter.nextElement();
					JarEntry other = (JarEntry)
						targetJar.getEntry(
							entry.getName());
					if (other == null)
						return false;
					if (entry.hashCode() !=
							other.hashCode())
						return false;
				}
				try {
					targetJar.close();
					sourceJar.close();
				} catch(IOException e) { }
				return true;
			}
		}

		class CompileJar extends Rule {
			CompileJar(String target, List prerequisites) {
				super(target, prerequisites);
			}

			void action() throws FakeException {
				List files = compileJavas(nonUpToDates, cwd);
				makeJar(target, null, files);
			}
		}

		class CompileClass extends Rule {
			CompileClass(String target, List prerequisites) {
				super(target, prerequisites);
			}

			void action() throws FakeException {
				compileJavas(prerequisites, cwd);
			}
		}

		class CompileCProgram extends Rule {
			boolean linkCPlusPlus = false;

			CompileCProgram(String target, List prerequisites) {
				super(target, prerequisites);
			}

			void action() throws FakeException {
				List out = new ArrayList();

				Iterator iter = prerequisites.iterator();
				while (iter.hasNext()) {
					String path = (String)iter.next();
					if (path.endsWith(".c")) {
						out.add(compileC(path));
					}
					else if (path.endsWith(".cxx"))
						out.add(compileCXX(path));
					else
						throw new FakeException("Cannot"
							+ " compile " + path);
				}
				link(target, out);
			}

			void add(String variable, String path, List arguments) {
				// TODO
			}

			String compileCXX(String path) throws FakeException {
				linkCPlusPlus = true;
				List arguments = new ArrayList();
				arguments.add("g++");
				arguments.add("-c");
				add("CXXFLAGS", path, arguments);
				arguments.add(path);
				try {
					execute(arguments, path);
					return path.substring(0, path.length() - 4)
						+ ".o";
				} catch(Exception e) {
					throw new FakeException("Could not "
						+ "compile " + path + ": " + e);
				}
			}

			String compileC(String path) throws FakeException {
				List arguments = new ArrayList();
				arguments.add("gcc");
				arguments.add("-c");
				add("CFLAGS", path, arguments);
				arguments.add(path);
				try {
					execute(arguments, path);
					return path.substring(0,
						path.length() - 2) + ".o";
				} catch(Exception e) {
					throw new FakeException("Could not "
						+ "compile " + path + ": " + e);
				}
			}

			void link(String target, List objects)
					throws FakeException {
				List arguments = new ArrayList();
				arguments.add(linkCPlusPlus ? "g++" : "gcc");
				arguments.add("-o");
				arguments.add(target);
				add("LDFLAGS", target, arguments);
				arguments.addAll(objects);
				add("LIBS", target, arguments);
				try {
					execute(arguments, target);
				} catch(Exception e) {
					e.printStackTrace();
					throw new FakeException("Could not link "
						+ target + ": " + e);
				}
			}
		}

		class ExecuteProgram extends Rule {
			String program;

			ExecuteProgram(String target, List prerequisites,
					String program) {
				super(target, prerequisites);
				this.program = program;
			}

			void action() throws FakeException {
				try {
					execute(splitCommandLine(program), cwd);
				} catch (Exception e) {
					if (!(e instanceof FakeException))
						e.printStackTrace();
					throw new FakeException("Program failed: '"
						+ program + "'\n" + e);
				}
			}

			List splitCommandLine(String program)
					throws FakeException {
				List result = new ArrayList();
				int len = program.length();
				String current = "";

				for (int i = 0; i < len; i++) {
					char c = program.charAt(i);
					if (isQuote(c)) {
						int i2 = findClosingQuote(
							program, c, i + 1, len);
						current += program.substring(i
								+ 1, i2);
						i = i2;
						continue;
					}
					if (c == ' ' || c == '\t') {
						if (current.equals(""))
							continue;
						result.add(current);
						current = "";
					} else
						current += c;
				}
				if (!current.equals(""))
					result.add(current);
				return result;
			}

			int findClosingQuote(String s, char quote, int index,
					int len)
					throws FakeException {
				for (int i = index; i < len; i++) {
					char c = s.charAt(i);
					if (c == quote)
						return i;
					if (isQuote(c))
						i = findClosingQuote(s, c,
								i + 1, len);
				}
				String spaces = "               ";
				for (int i = 0; i < index; i++)
					spaces += " ";
				throw new FakeException("Unclosed quote: "
					+ program + "\n" + spaces + "^");
			}

			boolean isQuote(char c) {
				return c == '"' || c == '\'';
			}
		}
	}


	// several utility functions

	static class GlobFilter implements FilenameFilter {
		Pattern pattern;
		long newerThan;

		GlobFilter(String glob, long newerThan) {
			String regex = "^" + glob.replace(".", "\\.")
				.replace("^", "\\^").replace("$", "\\$")
				.replace("?", ".").replace("*", ".*") + "$";
			pattern = Pattern.compile(regex);
			this.newerThan = newerThan;
		}

		public boolean accept(File dir, String name) {
			if (newerThan > 0 && newerThan > new File(dir, name)
					.lastModified())
				return false;
			return pattern.matcher(name).matches();
		}
	}

	protected static int expandGlob(String glob, List list)
			throws FakeException {
		return expandGlob(glob, list, 0);
	}

	protected static int expandGlob(String glob, List list, long newerThan)
			throws FakeException {
		// find first wildcard
		int star = glob.indexOf('*'), qmark = glob.indexOf('?');

		// no wildcard?
		if (star < 0 && qmark < 0) {
			list.add(glob);
			return 1;
		}

		if (qmark >= 0 && qmark < star)
			star = qmark;
		boolean starstar = glob.substring(star).startsWith("**");

		int prevSlash = glob.lastIndexOf('/', star);
		int nextSlash = glob.indexOf('/', star);

		String parentPath = prevSlash < 0 ?
			new File("").getAbsolutePath() :
			glob.substring(0, prevSlash);
		File parentDirectory = new File(parentPath);
		if (!parentDirectory.exists())
			throw new FakeException("Directory '" + parentDirectory
				+ "' not found");

		String pattern = glob.substring(prevSlash + 1, nextSlash < 0 ?
			glob.length() : nextSlash);

		String remainder = nextSlash < 0 ?
			null : glob.substring(nextSlash);

		String[] names = parentDirectory.list(new GlobFilter(pattern,
					newerThan));

		parentPath = prevSlash < 0 ? "" : parentPath + "/";
		int count = nextSlash < 0 ? names.length : 0;
		for (int i = 0; i < names.length; i++)
			if (nextSlash < 0)
				list.add(parentPath + names[i]);
			else if (new File(parentPath + names[i])
					.isDirectory()) {
				if (starstar)
					count += expandGlob(parentPath
						+ names[i] + "/**" + remainder,
						list, newerThan);
				count += expandGlob(parentPath + names[i]
						+ remainder, list, newerThan);
			}

		return count;
	}

	// adds the .class files for a certain .java file
	protected static void java2classFiles(String path, List result,
			long newerThan) throws FakeException {
		if (!path.endsWith(".java")) {
			result.add(path);
			return;
		}

		String stem = path.substring(0, path.length() - 5);
		if (expandGlob(stem + ".class", result, newerThan) == 0)
			throw new FakeException("No class file compiled for '"
				+ path + "'");
		expandGlob(stem + "$*.class", result, newerThan);
	}

	// this function handles the javac singleton
	protected static synchronized void callJavac(String[] arguments)
			throws Exception {
		if (javac == null) {
			Class main = Class.forName("com.sun.tools.javac.Main");
			javac = main.getMethod("compile",
					new Class[] { arguments.getClass() });
		}

		Object result = javac.invoke(null, new Object[] { arguments });
		if (!result.equals(new Integer(0)))
			throw new FakeException("Compile error");
	}

	// returns all .java files in the list, and returns a list where
	// all the .java files have been replaced by their .class files.
	protected List compileJavas(List javas, File cwd)
			throws FakeException {
		List arguments = new ArrayList();
		arguments.add("-source");
		arguments.add(javaVersion);
		arguments.add("-target");
		arguments.add(javaVersion);
		if (showDeprecation) {
			arguments.add("-deprecation");
			arguments.add("-Xlint:unchecked");
		}

		Iterator iter = javas.iterator();
		while (iter.hasNext()) {
			String path = (String)iter.next();
			if (path.endsWith(".java"))
				arguments.add(cwd + "/" + path);
		}

		String[] args = new String[arguments.size()];
		long now = System.currentTimeMillis();

		try {
			callJavac((String[])arguments.toArray(args));
		} catch (FakeException e) {
			throw e;
		} catch (Exception e) {
			throw new FakeException("Compile error: " + e);
		}

		List result = new ArrayList();
		iter = javas.iterator();
		while (iter.hasNext())
			java2classFiles((String)iter.next(), result, now);
		return result;
	}

	protected static void makeJar(String path, String mainClass, List files)
			throws FakeException {
		Manifest manifest = null;
		if (mainClass != null) {
			manifest = new Manifest();
			manifest.getMainAttributes().put("Main-Class",
					mainClass);
		}

		try {
			/*
			 * Avoid SIGBUS when writing fake.jar: it may be
			 * in use (mmap()ed), and overwriting that typically
			 * results in a crash.
			 */
			if (new File(path).exists() &&
					!new File(path).delete() &&
					!new File(path).renameTo(new File(path
							+ ".old")))
				throw new FakeException("Could not remove "
					+ path + " before building it anew");

			OutputStream out = new FileOutputStream(path);
			JarOutputStream jar = mainClass == null ?
				new JarOutputStream(out) :
				new JarOutputStream(out, manifest);

			Iterator iter = files.iterator();
			while (iter.hasNext()) {
				String name = (String)iter.next();

				JarEntry entry = new JarEntry(name);
				jar.putNextEntry(entry);

				InputStream file = new FileInputStream(name);
				byte[] buffer = new byte[1<<16];
				for (;;) {
					int len = file.read(buffer);
					if (len < 0)
						break;
					jar.write(buffer, 0, len);
				}
				file.close();
				jar.closeEntry();
			}

			jar.close();
		} catch (Exception e) {
			throw new FakeException("Error writing "
				+ path + ": " + e);
		}
	}

	public static void copyFile(String source, String target, File cwd)
			throws FakeException {
		try {
			if (!target.startsWith("/"))
				target = cwd + "/" + target;
			if (!source.startsWith("/"))
				source = cwd + "/" + source;
			OutputStream out = new FileOutputStream(target);
			InputStream in = new FileInputStream(source);
			byte[] buffer = new byte[1<<16];
			for (;;) {
				int len = in.read(buffer);
				if (len < 0)
					break;
				out.write(buffer, 0, len);
			}
			in.close();
			out.close();
		} catch (IOException e) {
			throw new FakeException("Could not copy "
				+ source + " to " + target + ": " + e);
		}
	}

	protected static class StreamDumper extends Thread {
		BufferedReader in;
		PrintStream out;

		StreamDumper(InputStream in, PrintStream out) {
			this.in = new BufferedReader(new InputStreamReader(in));
			this.out = out;
		}

		public void run() {
			for (;;) {
				try {
					String line = in.readLine();
					if (line == null)
						break;
					out.println(line);
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	// the parameter "file" is only used to set the cwd
	protected static void execute(List arguments, String file)
			throws Exception {
		execute(arguments, new File(file).getParentFile());
	}

	protected static void execute(String[] args, String file)
			throws Exception {
		execute(args, new File(file).getParentFile());
	}

	protected static void execute(List arguments, File dir)
			throws Exception {
		String[] args = new String[arguments.size()];
		arguments.toArray(args);
		execute(args, dir);
	}

	protected static void execute(String[] args, File dir)
			throws Exception {
		Process proc = Runtime.getRuntime().exec(args, null, dir);
		new StreamDumper(proc.getErrorStream(), System.err).start();
		new StreamDumper(proc.getInputStream(), System.out).start();
		proc.waitFor();
		int exitValue = proc.exitValue();
		if (exitValue != 0)
			throw new FakeException("Failed: " + exitValue);
	}


	// the variables

	protected boolean debug = false;
	protected boolean verbose = false;
	protected boolean showDeprecation = true;
	protected String javaVersion = "1.5";

	public void setVariable(String key, String value)
			throws FakeException {
		if (key.equalsIgnoreCase("javaVersion"))
			javaVersion = value;
		else if (key.equalsIgnoreCase("debug"))
			debug = value.equalsIgnoreCase("true");
		else if (key.equalsIgnoreCase("verbose"))
			verbose = value.equalsIgnoreCase("true");
		else if (key.equals("showDeprecation"))
			showDeprecation = value.equalsIgnoreCase("true");
		else
			throw new FakeException("Unknown variable: " + key);
	}

	// our very own exception

	static class FakeException extends Exception {
		public FakeException(String message) {
			super(message);
		}

		public String toString() {
			return getMessage();
		}
	}
}
