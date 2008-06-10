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
import java.io.ByteArrayInputStream;

import java.lang.reflect.Method;

import java.net.MalformedURLException;
import java.net.URL;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.JarOutputStream;

import java.util.regex.Pattern;

public class Fake {
	protected static Method javac;
	protected static String toolsPath;

	public static void main(String[] args) {
		new Fake().make(args);
	}

	final static Set variableNames = new HashSet();

	public Fake() {
		variableNames.add("DEBUG");
		variableNames.add("JAVAVERSION");
		variableNames.add("SHOWDEPRECATION");
		variableNames.add("VERBOSE");
		variableNames.add("IGNOREMISSINGFAKEFILES");
		variableNames.add("CFLAGS");
		variableNames.add("CXXFLAGS");
		variableNames.add("LDFLAGS");
		variableNames.add("MAINCLASS");
	}

	public void make(String[] args) {
		try {
			Parser parser = new Parser();

			List list = null;
			if (args.length > 0) {
				list = new ArrayList();
				for (int i = 0; i < args.length; i++)
					list.add(args[i]);
			}
			Parser.Rule all = parser.parseRules(list);
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
		protected Set allPrerequisites = new HashSet();
		protected Set allPlatforms;

		public Parser() throws FakeException {
			this(null);
		}

		public Parser(String path) throws FakeException {
			if (path == null || path.equals(""))
				path = this.path;
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

			if (allPlatforms == null) {
				allPlatforms = new HashSet();
				allPlatforms.add("linux");
				allPlatforms.add("linux64");
				allPlatforms.add("win32");
				allPlatforms.add("win64");
				allPlatforms.add("macosx");
			}
		}

		public Rule parseRules(List targets) throws FakeException {
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

				while (line.endsWith("\\"))
					try {
						String next = reader.readLine();
						line = line.substring(0,
							line.length() - 1)
							+ next;
					} catch (IOException e) {
						error("Error reading file");
					}

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

			lineNumber = -1;

			if (result == null)
				error("Could not find default rule");

			checkVariableNames();

			if (targets != null)
				return new All("", targets);

			return result;
		}

		public Rule addRule(String target, String prerequisites)
				throws FakeException {
			List list = new ArrayList();
			StringTokenizer tokenizer = new
				StringTokenizer(expandVariables(prerequisites,
							target), " \t\n");

			while (tokenizer.hasMoreTokens()) {
				String token = tokenizer.nextToken();
				if (expandGlob(token, list, cwd) == 0)
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
			else if (target.endsWith("]")) {
				int paren = target.indexOf('[');

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

			Iterator iter = list.iterator();
			while (iter.hasNext())
				allPrerequisites.add(iter.next());

			return rule;
		}

		protected void error(String message) throws FakeException {
			if (lineNumber < 0)
				throw new FakeException(path + ":" + message);
			throw new FakeException(path + ":" + lineNumber + ": "
					+ message + "\n\t" + line);
		}

		// the variables

		protected Map variables = new HashMap();

		public void setVariable(String key, String value)
				throws FakeException {
			int paren = key.indexOf('(');
			String name = paren < 0 ? key : key.substring(0, paren);

			value = expandVariables(value, paren < 0 ? null :
				key.substring(paren + 1, key.length() - 1));

			name = name.toUpperCase() + (paren < 0 ?
				"" : key.substring(paren));
			variables.put(name, value);
		}

		public String expandVariables(String value) {
			return expandVariables(value, null, null);
		}

		public String expandVariables(String value, String subkey) {
			return expandVariables(value, subkey, null);
		}

		public String expandVariables(String value,
				String subkey, String subkey2) {
			for (;;) {
				int dollar = value.indexOf('$');
				if (dollar < 0)
					return value;

				int end = dollar + 1;
				while (end < value.length() &&
						isAlnum(value.charAt(end)))
					end++;
				String name = value.substring(dollar + 1, end);
				String substitute =
					getVariable(name.toUpperCase(),
						subkey, subkey2);
				value = value.substring(0, dollar)
					+ (substitute == null ? "" : substitute)
					+ (end < value.length() ?
						value.substring(end) : "");
			}
		}

		public boolean isAlnum(char c) {
			return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
				|| (c >= '0' && c <= '9') || c == '_';
		}

		public void checkVariableNames() throws FakeException {
			Iterator iter = variables.keySet().iterator();
			while (iter.hasNext()) {
				String key = (String)iter.next();
				int paren = key.indexOf('(');
				if (paren < 0 || !key.endsWith(")"))
					continue;
				String name = key.substring(paren + 1,
						key.length() - 1);
				if (!allPrerequisites.contains(name) &&
						!allRules.containsKey(name) &&
						!allPlatforms.contains(name))
					throw new FakeException("Invalid target"
						+ " for variable " + key);
			}
		}

		public String getVariable(String key) {
			return getVariable(key, null, null);
		}

		public String getVariable(String key, String subkey) {
			return getVariable(key, subkey, null);
		}

		public String getVariable(String key,
				String subkey, String subkey2) {
			String res = null;
			if (subkey != null)
				res = (String)variables.get(key
						+ "(" + subkey + ")");
			if (subkey2 != null && res == null)
				res = (String)variables.get(key
						+ "(" + subkey2 + ")");
			if (res == null)
				res = (String)variables.get(key
						+ "(" + getPlatform() + ")");
			if (res == null)
				res = (String)variables.get(key);
			return res;
		}

		public void dumpVariables() {
			System.err.println("Variable dump:");
			Iterator iter = variables.keySet().iterator();
			while (iter.hasNext()) {
				String key = (String)iter.next();
				System.err.println(key + " = "
						+ variables.get(key));
			}
		}

		public boolean getBool(String string) {
			return string != null &&
				(string.equalsIgnoreCase("true") ||
				 string.equals("1"));
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
					if (getVarBool("DEBUG"))
						System.err.println("Checking "
							+ this);
					if (upToDate())
						return;
					System.err.println("Faking " + this);
					action();
				} catch (Exception e) {
					if (!(e instanceof FakeException))
						e.printStackTrace();
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
				if (getVarBool("VERBOSE")) {
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

			String getVar(String key) {
				return getVariable(key, target);
			}

			String getVar(String key, String subkey) {
				return getVariable(key, subkey, target);
			}

			boolean getVarBool(String key) {
				return getBool(getVariable(key, target));
			}

			boolean getVarBool(String key, String subkey) {
				return getBool(getVariable(key,
							subkey, target));
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

			public boolean upToDate() {
				return false;
			}
		}

		class SubFake extends Rule {
			String source;

			SubFake(String target, List prerequisites) {
				super(target, prerequisites);
				source = getLastPrerequisite()
					+ new File(target).getName();
			}

			boolean upToDate() {
				return false;
			}

			void action() throws FakeException {
				Iterator iter = prerequisites.iterator();
				while (iter.hasNext())
					action((String)iter.next());

				if (getVarBool("IGNOREMISSINGFAKEFILES") &&
						!new File(makePath(cwd,
							source)).exists())
					return;

				if (target.indexOf('.') >= 0)
					copyFile(source, target, cwd);
			}

			void action(String directory) throws FakeException {
				fakeOrMake(cwd, directory,
					getVarBool("VERBOSE", directory),
					getVarBool("IGNOREMISSINGFAKEFILES",
						directory));
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

				return jarUpToDate(source, target);
			}
		}

		class CompileJar extends Rule {
			CompileJar(String target, List prerequisites) {
				super(target, prerequisites);
			}

			void action() throws FakeException {
				toolsPath = getVar("TOOLSPATH");
				List files = compileJavas(nonUpToDates,
					cwd, getVar("JAVAVERSION"),
					getVarBool("VERBOSE"),
					getVarBool("SHOWDEPRECATION"));
				makeJar(target, getMainClass(), files,
					getVarBool("VERBOSE"));
			}

			String getMainClass() {
				return getVariable("MAINCLASS", target);
			}
		}

		class CompileClass extends Rule {
			CompileClass(String target, List prerequisites) {
				super(target, prerequisites);
			}

			void action() throws FakeException {
				toolsPath = getVar("TOOLSPATH");
				compileJavas(prerequisites,
					cwd, getVar("JAVAVERSION"),
					getVarBool("VERBOSE"),
					getVarBool("SHOWDEPRECATION"));
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

			void addFlags(String variable, String path,
					List arguments) throws FakeException {
				String value = getVariable(variable,
						path, target);
				arguments.addAll(splitCommandLine(value));
			}

			String compileCXX(String path) throws FakeException {
				linkCPlusPlus = true;
				List arguments = new ArrayList();
				arguments.add("g++");
				arguments.add("-c");
				addFlags("CXXFLAGS", path, arguments);
				arguments.add(path);
				try {
					execute(arguments, path,
						getVarBool("VERBOSE", path));
					return path.substring(0,
						path.length() - 4) + ".o";
				} catch(Exception e) {
					return error("compile", path, e);
				}
			}

			String compileC(String path) throws FakeException {
				List arguments = new ArrayList();
				arguments.add("gcc");
				arguments.add("-c");
				addFlags("CFLAGS", path, arguments);
				arguments.add(path);
				try {
					execute(arguments, path,
						getVarBool("VERBOSE", path));
					return path.substring(0,
						path.length() - 2) + ".o";
				} catch(Exception e) {
					return error("compile", path, e);
				}
			}

			void link(String target, List objects)
					throws FakeException {
				List arguments = new ArrayList();
				arguments.add(linkCPlusPlus ? "g++" : "gcc");
				arguments.add("-o");
				arguments.add(target);
				addFlags("LDFLAGS", target, arguments);
				arguments.addAll(objects);
				addFlags("LIBS", target, arguments);
				try {
					execute(arguments, target,
						getVarBool("VERBOSE", path));
				} catch(Exception e) {
					error("link", target, e);
				}
			}

			String error(String action, String file, Exception e)
					throws FakeException {
				throw new FakeException("Could not " + action
					+ " " + file + ": " + e);
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
					String expanded =
						expandVariables(program);
					execute(splitCommandLine(expanded), cwd,
						getVarBool("VERBOSE", program));
				} catch (Exception e) {
					if (!(e instanceof FakeException))
						e.printStackTrace();
					throw new FakeException("Program failed: '"
						+ program + "'\n" + e);
				}
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

	protected static int expandGlob(String glob, List list, File cwd)
			throws FakeException {
		return expandGlob(glob, list, cwd, 0);
	}

	protected static int expandGlob(String glob, List list, File cwd,
			long newerThan) throws FakeException {
		// find first wildcard
		int star = glob.indexOf('*'), qmark = glob.indexOf('?');

		// no wildcard?
		if (star < 0 && qmark < 0) {
			list.add(glob);
			return 1;
		}

		if (qmark >= 0 && qmark < star)
			star = qmark;
		boolean starstar = glob.substring(star).startsWith("**/");

		int prevSlash = glob.lastIndexOf('/', star);
		int nextSlash = glob.indexOf('/', star);

		String parentPath =
			prevSlash < 0 ? "" : glob.substring(0, prevSlash + 1);
		File parentDirectory = new File(makePath(cwd, parentPath));
		if (!parentDirectory.exists())
			throw new FakeException("Directory '" + parentDirectory
				+ "' not found");

		String pattern = nextSlash < 0 ?
			glob.substring(prevSlash + 1) :
			glob.substring(prevSlash + 1, nextSlash);

		String remainder = nextSlash < 0 ?
			null : glob.substring(nextSlash + 1);

		int count = 0;

		if (starstar) {
			count += expandGlob(parentPath + remainder, list,
						cwd, newerThan);
			remainder = "**/" + remainder;
		}

		String[] names = parentDirectory.list(new GlobFilter(pattern,
					newerThan));

		for (int i = 0; i < names.length; i++) {
			String path = parentPath + names[i];
			if (starstar && path.startsWith("."))
				continue;
			if (nextSlash < 0) {
				list.add(path);
				count++;
			}
			else if (new File(makePath(cwd, path)).isDirectory())
				count += expandGlob(path + "/" + remainder,
						list, cwd, newerThan);
		}

		return count;
	}

	// adds the .class files for a certain .java file
	protected static void java2classFiles(String path, List result,
			File cwd, long newerThan) throws FakeException {
		if (!path.endsWith(".java")) {
			result.add(path);
			return;
		}

		String stem = path.substring(0, path.length() - 5);
		if (expandGlob(stem + ".class", result, cwd, newerThan) == 0)
			throw new FakeException("No class file compiled for '"
				+ path + "'");
		expandGlob(stem + "$*.class", result, cwd, newerThan);
	}

	// this function handles the javac singleton
	protected static synchronized void callJavac(String[] arguments)
			throws FakeException {
		try {
			if (javac == null) {
				ClassLoader loader = getClassLoader(toolsPath);
				String className = "com.sun.tools.javac.Main";
				Class main = loader.loadClass(className);
				Class[] argsType = new Class[] {
					arguments.getClass()
				};
				javac = main.getMethod("compile", argsType);
			}

			Object result = javac.invoke(null,
					new Object[] { arguments });
			if (!result.equals(new Integer(0)))
				throw new FakeException("Compile error");
		} catch (Exception e) {
			e.printStackTrace();
			throw new FakeException("Could not find javac " + e
				+ " (tools path = " + toolsPath + ")");
		}
	}

	// returns all .java files in the list, and returns a list where
	// all the .java files have been replaced by their .class files.
	protected List compileJavas(List javas, File cwd, String javaVersion,
			boolean verbose, boolean showDeprecation)
			throws FakeException {
		List arguments = new ArrayList();
		if (javaVersion != null && !javaVersion.equals("")) {
			arguments.add("-source");
			arguments.add(javaVersion);
			arguments.add("-target");
			arguments.add(javaVersion);
		}
		if (showDeprecation) {
			arguments.add("-deprecation");
			arguments.add("-Xlint:unchecked");
		}
		String classPath = System.getProperty("java.class.path");
		if (classPath != null && !classPath.equals("")) {
			arguments.add("-classpath");
			arguments.add(classPath);
		}

		Iterator iter = javas.iterator();
		while (iter.hasNext()) {
			String path = (String)iter.next();

			if (path.endsWith(".java"))
				arguments.add(makePath(cwd, path));
		}

		String[] args = (String[])arguments.toArray(new
				String[arguments.size()]);
		long now = System.currentTimeMillis();

		if (verbose) {
			String output = "Compiling .java files: javac";
			for (int i = 0; i < args.length; i++)
				output += " " + args[i];
			System.err.println(output);
		}

		try {
			callJavac(args);
		} catch (FakeException e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			throw new FakeException("Compile error: " + e);
		}

		List result = new ArrayList();
		iter = javas.iterator();
		while (iter.hasNext())
			java2classFiles((String)iter.next(), result,
					cwd, now);
		return result;
	}

	protected static void makeJar(String path, String mainClass, List files,
			boolean verbose) throws FakeException {
		if (verbose) {
			String output = "Making " + path;
			if (mainClass != null)
				output += " with main-class " + mainClass;
			output += " from";
			Iterator iter = files.iterator();
			while (iter.hasNext())
				output += " " + iter.next();
			System.err.println(output);
		}
		Manifest manifest = null;
		if (mainClass != null) {
			String text = "Manifest-Version: 1.0\nMain-Class: "
				+ mainClass + "\n";
			InputStream input =
				new ByteArrayInputStream(text.getBytes());
			try {
				manifest = new Manifest(input);
			} catch(Exception e) { }
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
			JarOutputStream jar = manifest == null ?
				new JarOutputStream(out) :
				new JarOutputStream(out, manifest);

			Iterator iter = files.iterator();
			while (iter.hasNext()) {
				String realName = (String)iter.next();
				byte[] buffer = readFile(realName);
				ByteCodeAnalyzer analyzer =
					new ByteCodeAnalyzer(buffer);
				String name = analyzer.getPathForClass();

				JarEntry entry = new JarEntry(name + ".class");
				jar.putNextEntry(entry);
				jar.write(buffer, 0, buffer.length);
				jar.closeEntry();
			}

			jar.close();
		} catch (Exception e) {
			e.printStackTrace();
			throw new FakeException("Error writing "
				+ path + ": " + e);
		}
	}

	static byte[] readFile(String fileName) {
		try {
			File file = new File(fileName);
			if (!file.exists())
				return null;
			InputStream in = new FileInputStream(file);
			byte[] buffer = new byte[(int)file.length()];
			in.read(buffer);
			in.close();
			return buffer;
		} catch (Exception e) { return null; }
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
	protected static void execute(List arguments, String file,
			boolean verbose) throws Exception {
		execute(arguments, new File(file).getParentFile(), verbose);
	}

	protected static void execute(String[] args, String file,
			boolean verbose) throws Exception {
		execute(args, new File(file).getParentFile(), verbose);
	}

	protected static void execute(List arguments, File dir, boolean verbose)
			throws Exception {
		String[] args = new String[arguments.size()];
		arguments.toArray(args);
		execute(args, dir, verbose);
	}

	protected static void execute(String[] args, File dir, boolean verbose)
			throws Exception {
		if (verbose) {
			String output = "Executing:";
			for (int i = 0; i < args.length; i++)
				output += " '" + args[i] + "'";
			System.err.println(output);
		}
		Process proc = Runtime.getRuntime().exec(args, null, dir);
		new StreamDumper(proc.getErrorStream(), System.err).start();
		new StreamDumper(proc.getInputStream(), System.out).start();
		proc.waitFor();
		int exitValue = proc.exitValue();
		if (exitValue != 0)
			throw new FakeException("Failed: " + exitValue);
	}

	protected void fakeOrMake(File cwd, String directory, boolean verbose,
			boolean ignoreMissingFakefiles) throws FakeException {
		String fakeFile = directory + '/' + Parser.path;
		boolean tryFake = new File(fakeFile).exists();
		if (ignoreMissingFakefiles && !tryFake &&
				!(new File(directory + "/Makefile").exists())) {
			System.err.println("Ignore " + directory);
			return;
		}
		System.err.println((tryFake ? "F" : "M") + "aking in "
			+ directory + (directory.endsWith("/") ? "" : "/"));

		try {
			if (tryFake) {
				// Try "Fake"
				Parser parser = new Parser(fakeFile);
				parser.cwd = new File(cwd, directory);
				Parser.Rule all = parser.parseRules(null);
				all.make();
			} else
				// Try "make"
				execute(new String[] { "make" },
					new File(directory), verbose);
		} catch (Exception e) {
			if (!(e instanceof FakeException))
				e.printStackTrace();
			throw new FakeException((tryFake ?  "Fake" : "make")
				+ " failed: " + e);
		}
		System.err.println("Leaving " + directory);
	}

	protected static boolean jarUpToDate(String source, String target) {
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
			JarEntry entry = (JarEntry)iter.nextElement();
			JarEntry other =
				(JarEntry)targetJar.getEntry(entry.getName());
			if (other == null)
				return false;
			if (entry.hashCode() != other.hashCode())
				return false;
		}
		try {
			targetJar.close();
			sourceJar.close();
		} catch(IOException e) { }

		return true;
	}

	protected static List splitCommandLine(String program)
			throws FakeException {
		List result = new ArrayList();
		if (program == null)
			return result;
		int len = program.length();
		String current = "";

		for (int i = 0; i < len; i++) {
			char c = program.charAt(i);
			if (isQuote(c)) {
				int i2 = findClosingQuote(program,
						c, i + 1, len);
				current += program.substring(i + 1, i2);
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

	protected static int findClosingQuote(String s, char quote,
			int index, int len) throws FakeException {
		for (int i = index; i < len; i++) {
			char c = s.charAt(i);
			if (c == quote)
				return i;
			if (isQuote(c))
				i = findClosingQuote(s, c, i + 1, len);
		}
		String spaces = "               ";
		for (int i = 0; i < index; i++)
			spaces += " ";
		throw new FakeException("Unclosed quote: "
			+ s + "\n" + spaces + "^");
	}

	protected static boolean isQuote(char c) {
		return c == '"' || c == '\'';
	}

	public static String getPlatform() {
		boolean is64bit =
			System.getProperty("os.arch", "").indexOf("64") >= 0;
		String osName = System.getProperty("os.name", "<unknown>");
		if (osName.equals("Linux"))
			return "linux" + (is64bit ? "64" : "");
		if (osName.equals("Mac OS X"))
			return "macosx";
		if (osName.startsWith("Windows"))
			return "win" + (is64bit ? "64" : "32");
		System.err.println("Unknown platform: " + osName);
		return osName;
	}

	public static boolean isAbsolutePath(String path) {
		boolean isWindows = getPlatform().startsWith("win");
		return (isWindows && path.length() > 1 && path.charAt(1) == ':')
			|| (!isWindows && path.startsWith("/"));
	}

	public static String makePath(File cwd, String path) {
		if (isAbsolutePath(path))
			return path;
		if (path.equals("."))
			return cwd.toString();
		if (cwd.toString().equals("."))
			return path.equals("") ? "." : path;
		return new File(cwd, path).toString();
	}

	static class ByteCodeAnalyzer {
		byte[] buffer;
		int[] poolOffsets;
		int endOffset;

		public ByteCodeAnalyzer(byte[] buffer) {
			this.buffer = buffer;
			if ((int)getU4(0) != 0xcafebabe)
				throw new RuntimeException("No class");
			getConstantPoolOffsets();
		}

		public String getPathForClass() {
			int thisOffset = dereferenceOffset(endOffset + 2);
			if (getU1(thisOffset) != 7)
				throw new RuntimeException("Parse error");
			return getString(dereferenceOffset(thisOffset + 1));
		}

		int dereferenceOffset(int offset) {
			int index = getU2(offset);
			return poolOffsets[index - 1];
		}

		void getConstantPoolOffsets() {
			int poolCount = getU2(8) - 1;
			poolOffsets = new int[poolCount];
			int offset = 10;
			for (int i = 0; i < poolCount; i++) {
				poolOffsets[i] = offset;
				int tag = getU1(offset);
				if (tag == 7 || tag == 8)
					offset += 3;
				else if (tag == 9 || tag == 10 || tag == 11 ||
						tag == 3 || tag == 4 ||
						tag == 12)
					offset += 5;
				else if (tag == 5 || tag == 6) {
					poolOffsets[++i] = offset;
					offset += 9;
				}
				else if (tag == 1)
					offset += 3 + getU2(offset + 1);
				else
					throw new RuntimeException("Unknown tag"
						+ " " + tag);
			}
			endOffset = offset;
		}

		public String toString() {
			String result = "";
			for (int i = 0; i < poolOffsets.length - 1; i++) {
				int offset = poolOffsets[i];
				result += "index #" + i + ": " + format(offset);
				int tag = getU1(offset);
				if (tag == 5 || tag == 6)
					i++;
			}
			return result;
		}

		int getU1(int offset) {
			return buffer[offset] & 0xff;
		}

		int getU2(int offset) {
			return getU1(offset) << 8 | getU1(offset + 1);
		}

		long getU4(int offset) {
			return ((long)getU2(offset)) << 16 | getU2(offset + 2);
		}

		String getString(int offset) {
			try {
				return new String(buffer, offset + 3,
						getU2(offset + 1), "UTF-8");
			} catch (Exception e) { return ""; }
		}

		String format(int offset) {
			int tag = getU1(offset);
			int u2 = getU2(offset + 1);
			String result = "offset: " + offset + "(" + tag + "), ";
			if (tag == 7)
				return result + "class " + u2;
			if (tag == 9)
				return result + "field " + u2 + ", "
					+ getU2(offset + 3);
			if (tag == 10)
				return result + "method " + u2 + ", "
					+ getU2(offset + 3);
			if (tag == 11)
				return result + "interface method " + u2 + ", "
					+ getU2(offset + 3);
			if (tag == 8)
				return result + "string #" + u2;
			if (tag == 3)
				return result + "integer " + getU4(offset + 1);
			if (tag == 4)
				return result + "float " + getU4(offset + 1);
			if (tag == 12)
				return result + "name and type " + u2 + ", "
					+ getU2(offset + 3);
			if (tag == 5)
				return result + "long "
					+ getU4(offset + 1) + ", "
					+ getU4(offset + 5);
			if (tag == 6)
				return result + "double "
					+ getU4(offset + 1) + ", "
					+ getU4(offset + 5);
			if (tag == 1)
				return result + "utf8 " + u2
					+ " " + getString(offset);
			return result + "unknown";
		}
	}

	private static JarClassLoader classLoader;

	public static ClassLoader getClassLoader() throws IOException {
		return getClassLoader(null);
	}

	protected static ClassLoader getClassLoader(String jarFile)
			throws IOException {
		if (classLoader == null)
			classLoader = new JarClassLoader();
		if (jarFile != null &&
				! classLoader.jarFiles.containsKey(jarFile))
			classLoader.jarFiles.put(jarFile, new JarFile(jarFile));
		return classLoader;
	}

	private static class JarClassLoader extends ClassLoader {
		Map jarFiles;
		Map cache;

		JarClassLoader() {
			super(Thread.currentThread().getContextClassLoader());
			jarFiles = new HashMap();
			cache = new HashMap();
		}

		public URL getResource(String name) {
			Iterator iter = jarFiles.keySet().iterator();
			while (iter.hasNext()) {
				String file = (String)iter.next();
				JarFile jar = (JarFile)jarFiles.get(file);
				if (jar.getEntry(name) == null)
					continue;
				String url = "file:///"
					+ file.replace('\\', '/')
					+ "!/" + name;
				try {
					return new URL("jar", "", url);
				} catch (MalformedURLException e) { }
			}
			return getSystemResource(name);
		}

		public InputStream getResourceAsStream(String name) {
			return getResourceAsStream(name, false);
		}

		public InputStream getResourceAsStream(String name,
				boolean nonSystemOnly) {
			Iterator iter = jarFiles.values().iterator();
			while (iter.hasNext()) {
				JarFile jar = (JarFile)iter.next();
				JarEntry entry = jar.getJarEntry(name);
				if (entry == null)
					continue;
				try {
					return jar.getInputStream(entry);
				} catch (IOException e) { }
			}
			if (nonSystemOnly)
				return null;
			return super.getResourceAsStream(name);
		}

		public Class loadClass(String name)
				throws ClassNotFoundException {
			return loadClass(name, true);
		}

		public synchronized Class loadClass(String name,
				boolean resolve) throws ClassNotFoundException {
			Object cached = cache.get(name);
			if (cached != null)
				return (Class)cached;
			Class result;
			try {
				result = super.loadClass(name, resolve);
				if (result != null)
					return result;
			} catch (Exception e) { }
			String path = name.replace('.', '/') + ".class";
			InputStream input = getResourceAsStream(path, true);
			try {
				byte[] buffer = readStream(input);
				result = defineClass(name,
						buffer, 0, buffer.length);
				cache.put(name, result);
				return result;
			} catch (IOException e) { return null; }
		}

		byte[] readStream(InputStream input) throws IOException {
			byte[] buffer = new byte[1024];
			int offset = 0, len = 0;
			for (;;) {
				if (offset == buffer.length)
					buffer = realloc(buffer,
							2 * buffer.length);
				len = input.read(buffer, offset,
						buffer.length - offset);
				if (len < 0) {
					input.close();
					return realloc(buffer, offset);
				}
				offset += len;
			}
		}

		byte[] realloc(byte[] buffer, int newLength) {
			if (newLength == buffer.length)
				return buffer;
			byte[] newBuffer = new byte[newLength];
			System.arraycopy(buffer, 0, newBuffer, 0,
				Math.min(newLength, buffer.length));
			return newBuffer;
		}
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
