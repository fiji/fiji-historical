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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import java.util.jar.Manifest;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
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
		fijiHome = discoverFijiHome();
	}

	protected static String fijiHome;

	protected String discoverFijiHome() {
		URL url = getClass().getResource("Fake.class");
		String fijiHome = URLDecoder.decode(url.toString());
		if (getPlatform().startsWith("win"))
			fijiHome = fijiHome.replace('\\', '/');
		if (!fijiHome.endsWith("/Fake.class"))
			throw new RuntimeException("unexpected URL: " + url);
		fijiHome = fijiHome.substring(0, fijiHome.length() - 10);
		int slash = fijiHome.lastIndexOf('/', fijiHome.length() - 2);
		if (fijiHome.startsWith("jar:file:") &&
				fijiHome.endsWith(".jar!/"))
			fijiHome = fijiHome.substring(9, slash + 1);
		else if (fijiHome.startsWith("file:/"))
			fijiHome = fijiHome.substring(5, slash + 1);
		if (getPlatform().startsWith("win") && fijiHome.startsWith("/"))
			fijiHome = fijiHome.substring(1);
		if (fijiHome.endsWith("precompiled/"))
			fijiHome = fijiHome.substring(0,
					fijiHome.length() - 12);

		return fijiHome;
	}

	protected static void discoverJython() throws IOException {
		String pythonHome = fijiHome + "jars/jython2.2.1";
		System.setProperty("python.home", pythonHome);
		System.setProperty("python.cachedir.skip", "false");
		getClassLoader(pythonHome + "/jython.jar");
	}

	protected static void discoverJavac() throws IOException {
		String path = fijiHome + "jars/javac.jar";
		if (!new File(path).exists())
			path = fijiHome + "precompiled/javac.jar";
		getClassLoader(path);
	}

	protected static List discoverJars() throws FakeException {
		List jars = new ArrayList();
		if (new File(fijiHome + "ij.jar").exists())
			jars.add(fijiHome + "ij.jar");

		File cwd = new File(".");
		/*
		 * Since View5D contains an ImageCanvas (d'oh!) which would
		 * be picked up instead of ImageJ's, we cannot blindly
		 * include all plugin's jars...
		 */
		// expandGlob(fijiHome + "plugins/**/*.jar", jars, cwd);
		expandGlob(fijiHome + "misc/**/*.jar", jars, cwd);
		expandGlob(fijiHome + "jars/**/*.jar", jars, cwd);

		return jars;
	}

	protected static String discoverClassPath() throws FakeException {
		Iterator iter = discoverJars().iterator();
		String classPath = "";
		while (iter.hasNext())
			classPath += (classPath.equals("") ?
					"" : File.pathSeparator)
				+ iter.next();
		return classPath;
	}

	public void make(String[] args) {
		try {
			Parser parser = new Parser();

			parser.setVariable("FIJIHOME", fijiHome);

			// filter out variable definitions
			int firstArg = 0;
			while (firstArg < args.length &&
					args[firstArg].indexOf('=') >= 0)
				firstArg++;

			List list = null;
			if (args.length > firstArg) {
				list = new ArrayList();
				for (int i = firstArg; i < args.length; i++)
					list.add(args[i]);
			}
			Parser.Rule all = parser.parseRules(list);

			for (int i = 0; i < firstArg; i++) {
				int equal = args[i].indexOf('=');
				parser.setVariable(args[i].substring(0, equal),
						args[i].substring(equal + 1));
			}

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
		protected Rule allRule;

		public Parser() throws FakeException {
			this(null);
		}

		public Parser(String path) throws FakeException {
			if (path == null || path.equals(""))
				path = Parser.path;
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
				Iterator iter = allPlatforms.iterator();
				while (iter.hasNext()) {
					String platform = (String)iter.next();
					setVariable("platform(" + platform
						+ ")", platform);
				}
			}

			setVariable("platform", getPlatform());

			addSpecialRule(new Special("show-rules") {
				void action() { showMap(allRules, false); }
			});

			addSpecialRule(new Special("show-vars") {
				void action() { showMap(variables, true); }
			});

			addSpecialRule(new Special("clean") {
				void action() { cleanAll(false); }
			});

			addSpecialRule(new Special("clean-dry-run") {
				void action() { cleanAll(true); }
			});

			addSpecialRule(new Special("check") {
				void action() { check(); }
			});
		}

		protected void showMap(Map map, boolean showKeys) {
			List list = new ArrayList(map.keySet());
			Collections.sort(list);
			Iterator iter = list.iterator();
			while (iter.hasNext()) {
				Object key = iter.next();
				System.out.println((showKeys ?
						key.toString() + " = " : "")
					+ map.get(key));
			}
		}

		protected void cleanAll(boolean dry_run) {
			Iterator iter = allRules.keySet().iterator();
			while (iter.hasNext()) {
				Rule rule = (Rule)allRules.get(iter.next());
				rule.clean(dry_run);
			}
		}

		protected void check() {
			List list = new ArrayList(allRules.keySet());
			Collections.sort(list);
			Iterator iter = list.iterator();
			while (iter.hasNext()) {
				Rule rule = (Rule)allRules.get(iter.next());
				if (rule instanceof All)
					continue;
				if (rule instanceof Special)
					continue;
				if (rule instanceof SubFake) {
					System.out.println("Subfake '"
						+ rule.getLastPrerequisite()
						+ "' would make '"
						+ rule.target + "'");
					continue;
				}
				if (rule.upToDate())
					continue;
				if (rule instanceof ExecuteProgram) {
					String program =
						((ExecuteProgram)rule).program;
					if (program.equals(""))
						continue;
					System.out.println("Program '" + program
						+ "' would maybe make '"
						+ rule.target + "'");
					continue;
				}
				System.out.println("'" + rule.target
						+ "' is not up-to-date");
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
						lineNumber++;
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
			Rule rule = null;

			if (target.indexOf('*') >= 0) {
				int bracket = target.indexOf('[');
				String program = "";
				if (bracket >= 0) {
					program = target.substring(bracket);
					target = target.substring(0, bracket);
				}
				GlobFilter filter = new GlobFilter(target);
				Iterator iter = new ArrayList(allPrerequisites)
					.iterator();
				while (iter.hasNext()) {
					target = (String)iter.next();
					if (allRules.containsKey(target))
						continue;
					if (!filter.accept(null, target))
						continue;
					rule = addRule(target
						+ filter.replace(program),
						filter.replace(prerequisites));
				}
				return rule;
			}

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

			String lastPrereq = list.size() == 0 ? null :
				(String)list.get(list.size() - 1);

			if (allRule == null)
				rule = allRule = new All(target, list);
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
			else if (lastPrereq != null &&
					new File(lastPrereq).isDirectory())
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
			if (rule == null)
				throw new FakeException("Unrecognized rule");

			allRules.put(target, rule);

			Iterator iter = list.iterator();
			while (iter.hasNext())
				allPrerequisites.add(iter.next());

			return rule;
		}

		protected void addSpecialRule(Special rule) {
			allRules.put(rule.target, rule);
		}

		protected void error(String message) throws FakeException {
			if (lineNumber < 0)
				throw new FakeException(path + ":" + message);
			throw new FakeException(path + ":" + lineNumber + ": "
					+ message + "\n\t" + line);
		}

		// the variables

		protected Map variables = new HashMap();

		public int getVariableNameEnd(String value, int offset) {
			int end = offset;
			while (end < value.length() &&
					isVarChar(value.charAt(end)))
				end++;
			return end;
		}

		/*
		 * This handles
		 *
		 * 	$SOMETHING(*) = $SOME $THINK $ELSE
		 *
		 * by searching for all available subkeys of $SOME, $THING
		 * and $ELSE, and setting $SOMETHING(subkey) for all of them.
		 */
		public void setVariableWildcard(String key, String value)
				throws FakeException {
			/* get all variable names */
			int offset = 0;
			Set variableNames = new HashSet();
			for (;;) {
				int dollar = value.indexOf('$', offset);
				if (dollar < 0)
					break;
				offset = getVariableNameEnd(value, dollar + 1);
				variableNames.add(value.substring(dollar + 1,
							offset).toUpperCase());
			}
			key = key.toUpperCase();
			Set subkeys = new HashSet();
			Iterator iter = variables.keySet().iterator();
			while (iter.hasNext()) {
				String var = (String)iter.next();
				int paren = var.indexOf('(');
				if (paren < 0)
					continue;
				String name = var.substring(0, paren);
				if (!variableNames.contains(name))
					continue;
				subkeys.add(var.substring(paren));
			}
			/* 3rd loop to avoid concurrent modification */
			iter = subkeys.iterator();
			while (iter.hasNext())
				setVariable(key + iter.next(), value);
		}

		public void setVariable(String key, String value)
				throws FakeException {
			int paren = key.indexOf('(');
			String name = paren < 0 ? key : key.substring(0, paren);

			if (key.charAt(paren + 1) == '*') {
				setVariableWildcard(name, value);
				return;
			}

			value = expandVariables(value, paren < 0 ? null :
				key.substring(paren + 1, key.length() - 1));

			if (value.indexOf('*') >= 0 ||
					value.indexOf('?') >= 0) {
				String separator = key.equals("CLASSPATH") ||
					key.startsWith("CLASSPATH(") ?
					":" : " ";
				List files = new ArrayList();
				StringTokenizer tokenizer = new
					StringTokenizer(value.replace('\t',
							' '), separator);
				while (tokenizer.hasMoreTokens()) {
					String token = tokenizer.nextToken();
					if (expandGlob(token, files, cwd) < 1)
						System.err.println("Warning: "
							+ "no match for "
							+ token);
				}
				value = "";
				if (separator.equals(":"))
					separator = File.separator;
				Iterator iter = files.iterator();
				while (iter.hasNext())
					value += separator +
						quoteArg((String)iter.next());
			}

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
			int offset = 0;
			for (;;) {
				int dollar = value.indexOf('$', offset);
				if (dollar < 0)
					return value;

				int end = getVariableNameEnd(value, dollar + 1);
				String name = value.substring(dollar + 1, end);
				int paren = name.indexOf('(');
				String substitute;
				if (paren < 0)
					substitute =
						getVariable(name.toUpperCase(),
						subkey, subkey2);
				else
					substitute = (String)variables.get(
					name.substring(0, paren).toUpperCase()
					+ name.substring(paren));
				if (substitute == null)
					substitute = "";
				value = value.substring(0, dollar)
					+ substitute
					+ (end < value.length() ?
						value.substring(end) : "");
				offset = dollar + substitute.length();
			}
		}

		public boolean isVarChar(char c) {
			return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
				|| (c >= '0' && c <= '9') || c == '_'
				|| c == '(' || c == ')';
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
			key = key.toUpperCase();
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
			boolean wasAlreadyInvoked;
			boolean wasAlreadyChecked;

			Rule(String target, List prerequisites) {
				this.target = target;
				this.prerequisites = prerequisites;
				try {
					String name = "TARGET(" + target + ")";
					if (!variables.containsKey(name))
						setVariable(name, target);
					name = "PRE(" + target + ")";
					if (!variables.containsKey(name))
						setVariable(name,
							join(prerequisites));
				} catch (FakeException e) { /* ignore */ }
			}

			abstract void action() throws FakeException;

			boolean upToDate() {
				// this implements the mtime check
				File file = new File(makePath(cwd, target));
				if (!file.exists()) {
					nonUpToDates = prerequisites;
					return false;
				}
				long targetModifiedTime = file.lastModified();

				nonUpToDates = new ArrayList();
				Iterator iter = prerequisites.iterator();
				while (iter.hasNext()) {
					String prereq = (String)iter.next();
					String path = makePath(cwd, prereq);
					if (new File(path).lastModified()
							> targetModifiedTime)
						nonUpToDates.add(prereq);
				}

				return nonUpToDates.size() == 0;
			}

			boolean upToDate(String path) {
				if (path == null)
					return true;
				long targetModified =
					new File(cwd, target).lastModified();
				long pathModified =
					new File(path).lastModified();
				return targetModified > pathModified;
			}

			void make() throws FakeException {
				if (wasAlreadyChecked)
					return;
				wasAlreadyChecked = true;
				if (wasAlreadyInvoked)
					error("Dependency cycle detected!");
				wasAlreadyInvoked = true;
				try {
					if (getVarBool("DEBUG"))
						System.err.println("Checking "
							+ this);
					makePrerequisites();

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
				wasAlreadyInvoked = false;
			}

			protected void clean(boolean dry_run) {
				clean(target, dry_run);
			}

			protected void clean(String path, boolean dry_run) {
				File file = new File(path);
				if (file.exists() && !file.isDirectory()) {
					if (dry_run)
						System.out.println("rm "
							+ path);
					else
						file.delete();
				}
			}

			protected void error(String message)
					throws FakeException {
				throw new FakeException(message
						+ "\n\tin rule " + this);
			}

			public void makePrerequisites() throws FakeException {
				Iterator iter = prerequisites.iterator();
				while (iter.hasNext()) {
					String prereq = (String)iter.next();

					if (!allRules.containsKey(prereq)) {
						if (this instanceof All)
							error("Unknown target: "
								+ prereq);
						else
							continue;
					}

					Rule rule = (Rule)allRules.get(prereq);
					rule.make();
				}
			}

			public String getLastPrerequisite() {
				int index = prerequisites.size() - 1;
				return (String)prerequisites.get(index);
			}

			public String toString() {
				return toString(getVarBool("VERBOSE") ? 0 : 60);
			}

			public String toString(int maxCharacters) {
				String result = "";
				if (getVarBool("VERBOSE") &&
						!(this instanceof Special)) {
					String type = getClass().getName();
					int dollar = type.lastIndexOf('$');
					if (dollar >= 0)
						type = type.substring(dollar + 1);
					result += "(" + type + ") ";
				}
				if (getVarBool("DEBUG") && (this
						instanceof ExecuteProgram))
					target += "[" +
						((ExecuteProgram)this).program
						+ "]";
				result += target + " <-";
				Iterator iter = prerequisites.iterator();
				while (iter.hasNext())
					result += " " + iter.next();
				if (maxCharacters > 0 && result.length()
						> maxCharacters)
					result = result.substring(0,
						Math.max(0, maxCharacters - 3))
						+ "...";
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

			List compileJavas(List javas) throws FakeException {
				toolsPath = getVar("TOOLSPATH");
				return Fake.compileJavas(javas, cwd,
					getVar("JAVAVERSION"),
					getVarBool("VERBOSE"),
					getVarBool("SHOWDEPRECATION"),
					getVar("CLASSPATH"));
			}

			String getPluginsConfig() {
				String path = getVar("pluginsConfigDirectory");
				if (path == null || path.equals(""))
					return null;
				String key = target;
				if (key.endsWith(".jar"))
					key = key.substring(0,
							key.length() - 4);
				int slash = key.lastIndexOf('/');
				if (slash >= 0)
					key = key.substring(slash + 1);
				path += "/" + key + ".config";
				if (!new File(path).exists())
					return null;
				return path;
			}

			void copyJar(String source, String target, File cwd,
					String configPath)
					throws FakeException {
				if (configPath == null)
					copyFile(source, target, cwd);
				else try {
					copyJarWithPluginsConfig(source, target,
						cwd, configPath);
				} catch (Exception e) {
					e.printStackTrace();
					throw new FakeException("Couldn't copy "
						+ source + " to " + target
						+ ": " + e);
				}
			}

			void copyJarWithPluginsConfig(String source,
					String target, File cwd,
					String configPath) throws Exception {
				File file = new File(cwd, source);
				InputStream input = new FileInputStream(file);
				JarInputStream in = new JarInputStream(input);

				Manifest manifest = in.getManifest();
				file = new File(cwd, target);
				OutputStream output =
					new FileOutputStream(file);
				JarOutputStream out = manifest == null ?
					new JarOutputStream(output) :
					new JarOutputStream(output, manifest);

				addPluginsConfigToJar(out, configPath);

				JarEntry entry;
				while ((entry = in.getNextJarEntry()) != null) {
					String name = entry.getName();
					if (name.equals("plugins.config") &&
							configPath != null) {
						in.closeEntry();
						continue;
					}
					byte[] buf = readStream(in);
					in.closeEntry();
					out.putNextEntry(entry);
					out.write(buf);
					out.closeEntry();
				}
				in.close();
				out.close();
			}
		}

		class All extends Rule {
			All(String target, List prerequisites) {
				super(target, prerequisites);
			}

			public void action() throws FakeException {
			}

			public boolean upToDate() {
				return false;
			}
		}

		abstract class Special extends Rule {
			Special(String target) {
				super(target, new ArrayList());
			}

			boolean upToDate() {
				return false;
			}
		}

		class SubFake extends Rule {
			String source;
			String configPath;

			SubFake(String target, List prerequisites) {
				super(target, prerequisites);
				source = getLastPrerequisite()
					+ new File(target).getName();
				configPath = getPluginsConfig();
			}

			boolean upToDate() {
				return false;
			}

			void action() throws FakeException {
				Iterator iter = prerequisites.iterator();
				while (iter.hasNext())
					action((String)iter.next());

				File file = new File(makePath(cwd, source));
				if (getVarBool("IGNOREMISSINGFAKEFILES") &&
						!file.exists()) {
					String precompiled =
						getVar("PRECOMPILEDDIRECTORY");
					if (precompiled == null)
						return;
					source = precompiled + file.getName();
					if (!new File(makePath(cwd,
							source)).exists())
						return;
				}

				if (target.indexOf('.') >= 0)
					copyJar(source, target, cwd,
							configPath);
			}

			void action(String directory) throws FakeException {
				fakeOrMake(cwd, directory,
					getVarBool("VERBOSE", directory),
					getVarBool("IGNOREMISSINGFAKEFILES",
						directory),
					getVarPath("TOOLSPATH", directory),
					getVarPath("CLASSPATH", directory));
			}

			String getVarPath(String variable, String subkey) {
				String value = getVar(variable, subkey);
				if (value == null)
					return null;

				String result = "";
				StringTokenizer tokenizer =
					new StringTokenizer(value,
							File.pathSeparator);
				while (tokenizer.hasMoreElements()) {
					if (!result.equals(""))
						result += File.pathSeparator;
					File file =
						new File(tokenizer.nextToken());
					result += file.getAbsolutePath();
				}
				return result;
			}
		}

		class CopyJar extends Rule {
			String source, configPath;
			CopyJar(String target, List prerequisites) {
				super(target, prerequisites);
				source = getLastPrerequisite();
				configPath = getPluginsConfig();
			}

			void action() throws FakeException {
				copyJar(source, target, cwd, configPath);
			}

			boolean upToDate() {
				if (super.upToDate() && upToDate(configPath))
					return true;

				return jarUpToDate(source, target);
			}
		}

		class CompileJar extends Rule {
			String configPath;

			CompileJar(String target, List prerequisites) {
				super(target, uniq(prerequisites));
				configPath = getPluginsConfig();
			}

			void action() throws FakeException {
				compileJavas(prerequisites);
				List files =
					java2classFiles(prerequisites, cwd);
				makeJar(target, getMainClass(), files, cwd,
					configPath, getVarBool("VERBOSE"));
			}

			boolean upToDate() {
				return super.upToDate() && upToDate(configPath);
			}

			String getMainClass() {
				return getVariable("MAINCLASS", target);
			}

			protected void clean(boolean dry_run) {
				super.clean(dry_run);
				List javas = new ArrayList();
				Iterator iter = prerequisites.iterator();
				while (iter.hasNext()) {
					String path = (String)iter.next();
					if (path.endsWith(".java"))
						javas.add(path);
				}

				try {
					iter = java2classFiles(javas,
							cwd).iterator();
				} catch (FakeException e) {
					System.err.println("Warning: could not "
						+ "find required .class files: "
						+ this);
					return;
				}
				while (iter.hasNext())
					clean((String)iter.next(), dry_run);
			}
		}

		class CompileClass extends Rule {
			CompileClass(String target, List prerequisites) {
				super(target, prerequisites);
			}

			void action() throws FakeException {
				compileJavas(prerequisites);

				// copy class files, if necessary
				int slash = target.lastIndexOf('/') + 1;
				String destPrefix = target.substring(0, slash);

				String prefix = getLastPrerequisite();
				slash = prefix.lastIndexOf('/') + 1;
				prefix = prefix.substring(0, slash);

				Iterator iter = java2classFiles(prerequisites,
						cwd).iterator();
				while (iter.hasNext()) {
					String source = (String)iter.next();
					if (!source.startsWith(prefix))
						continue;
					int slash2 = source.lastIndexOf('/');
					copyFile(source, destPrefix +
						source.substring(slash2), cwd);
				}
			}
		}

		class CompileCProgram extends Rule {
			boolean linkCPlusPlus = false;

			CompileCProgram(String target, List prerequisites) {
				super(target, prerequisites);
				if (getPlatform().startsWith("win"))
					this.target += ".exe";
			}

			void action() throws FakeException {
				try {
					action(prerequisites.iterator());
				} catch (IOException e) {
					fallBackToPrecompiled(e.getMessage());
				}
			}

			void action(Iterator iter)
					throws IOException, FakeException {
				List out = new ArrayList();
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

			String compileCXX(String path)
					throws IOException, FakeException {
				linkCPlusPlus = true;
				return compile(path, "g++", "CXXFLAGS");
			}

			String compileC(String path)
					throws IOException, FakeException {
				return compile(path, "gcc", "CFLAGS");
			}

			String compile(String path, String compiler,
					String flags)
					throws IOException, FakeException {
				List arguments = new ArrayList();
				arguments.add(compiler);
				if (getVarBool("DEBUG"))
					arguments.add("-g");
				arguments.add("-c");
				addFlags(flags, path, arguments);
				arguments.add(path);
				try {
					execute(arguments, path,
						getVarBool("VERBOSE", path));
					return path.substring(0,
						path.length() - 4) + ".o";
				} catch(FakeException e) {
					return error("compile", path, e);
				}
			}

			void link(String target, List objects)
					throws FakeException {
				File file = new File(target);
				moveFileOutOfTheWay(file);
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

			void fallBackToPrecompiled(String reason)
					throws FakeException {
				String precompiled =
					getVar("PRECOMPILEDDIRECTORY");
				if (precompiled == null)
					error(reason);
				System.err.println("Falling back to copying "
					+ target + " from " + precompiled);
				File file = new File(makePath(cwd, target));
				if (!precompiled.endsWith("/"))
					precompiled += "/";
				String source =
					precompiled + file.getName();
				if (!new File(source).exists())
					source += "-" + getPlatform();
				moveFileOutOfTheWay(makePath(cwd, target));
				copyFile(source, target, cwd);
				if (!getPlatform().startsWith("win")) try {
					/* avoid Java6-ism */
					Runtime.getRuntime().exec(new String[]
						{ "chmod", "0755", target});
				} catch (Exception e) { /* ignore */ }
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
				if (program.equals(""))
					return;
				try {
					String expanded =
						expandVariables(program,
							target);
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
		String glob;
		String lastMatch;

		GlobFilter(String glob) {
			this(glob, 0);
		}

		GlobFilter(String glob, long newerThan) {
			this.glob = glob;
			String regex = "^"
				+ glob.replace(".", "\\.")
				.replace("^", "\\^")
				.replace("$", "\\$")
				.replace("?", "[^/]")
				.replace("*", "[^/]*")
				.replace("[^/]*[^/]*", ".*")
				+ "$";
			pattern = Pattern.compile(regex);
			this.newerThan = newerThan;
		}

		public boolean accept(File dir, String name) {
			if (newerThan > 0 && newerThan > new File(dir, name)
					.lastModified())
				return false;
			if (pattern.matcher(name).matches()) {
				lastMatch = name;
				return true;
			}
			lastMatch = null;
			return false;
		}

		boolean wildcardContainsStarStar;
		int firstWildcardIndex = -1, suffixLength;
		String wildcardPattern;

		private void initReplace() throws FakeException {
			if (firstWildcardIndex >= 0)
				return;
			wildcardContainsStarStar = glob.indexOf("**") >= 0;
			int first = glob.indexOf('*');
			int first2 = glob.indexOf('?');
			if (first < 0 && first2 < 0)
				throw new FakeException("Expected glob: "
					+ glob);
			int last = glob.lastIndexOf('*');
			int last2 = glob.lastIndexOf('?');
			firstWildcardIndex = first < 0 ||
				(first2 >= 0 && first > first2) ?
				first2 : first;
			int lastWildcardIndex = last < 0 || last < last2 ?
				last2 : last;
			wildcardPattern = glob.substring(firstWildcardIndex,
				lastWildcardIndex + 1);
			suffixLength = glob.length() - lastWildcardIndex - 1;
		}

		public String replace(String name) throws FakeException {
			initReplace();
			int index = name.indexOf(wildcardPattern);
			while (!wildcardContainsStarStar && index >= 0 &&
					name.substring(index).startsWith("**"))
				index = name.indexOf(wildcardPattern,
					name.substring(index).startsWith("**/*")
					? index + 4 : index + 2);
			if (index < 0)
				return name;
			return replace(name.substring(0, index)
				+ lastMatch.substring(firstWildcardIndex,
					lastMatch.length() - suffixLength)
				+ name.substring(index
					+ wildcardPattern.length()));
		}

		public List replace(List names) throws FakeException {
			List result = new ArrayList();
			Iterator iter = names.iterator();
			while (iter.hasNext())
				result.add(replace((String)iter.next()));
			return result;
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
			pattern = "*";
		}

		String[] names = parentDirectory.list(new GlobFilter(pattern,
					newerThan));

		for (int i = 0; i < names.length; i++) {
			String path = parentPath + names[i];
			if (starstar && names[i].startsWith("."))
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

	/*
	 * This function inspects a .class file for a given .java file,
	 * infers the package name and all used classes, and adds to "all"
	 * the class file names of those classes used that have been found
	 * in the same class path.
	 */
	protected static void java2classFiles(String java, File cwd, Set all) {
		if (java.endsWith(".java"))
			java = java.substring(0, java.length() - 5) + ".class";
		else if (!java.endsWith(".class")) {
			all.add(java);
			return;
		}
		byte[] buffer = readFile(makePath(cwd, java));
		if (buffer == null) {
			System.err.println("Warning: " + java
					+ " does not exist.  Skipping...");
			return;
		}
		ByteCodeAnalyzer analyzer = new ByteCodeAnalyzer(buffer);
		String fullClass = analyzer.getPathForClass() + ".class";
		if (!java.endsWith(fullClass))
			throw new RuntimeException("Huh? " + fullClass
					+ " is not a suffix of " + java);
		java = java.substring(0, java.length() - fullClass.length());
		Iterator iter = analyzer.getClassNames();
		while (iter.hasNext()) {
			String className = (String)iter.next();
			String path = java + className + ".class";
			if (new File(makePath(cwd, path)).exists() &&
					!all.contains(path)) {
				all.add(path);
				java2classFiles(path, cwd, all);
			}
		}
	}

	/* discovers all the .class files for a given set of .java files */
	protected static List java2classFiles(List javas, File cwd)
			throws FakeException {
		Set all = new HashSet();
		Iterator iter = javas.iterator();
		while (iter.hasNext())
			java2classFiles((String)iter.next(), cwd, all);
		return new ArrayList(all);
	}

	// this function handles the javac singleton
	protected static synchronized void callJavac(String[] arguments,
			boolean verbose) throws FakeException {
		try {
			if (javac == null) {
				discoverJavac();
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
			return;
		} catch (FakeException e) {
			/* was compile error */
			throw e;
		} catch (Exception e) {
			System.err.println("Could not find javac " + e
				+ " (tools path = " + toolsPath + "), "
				+ "falling back to system javac");
		}

		// fall back to calling javac
		String[] newArguments = new String[arguments.length + 1];
		newArguments[0] = "javac";
		System.arraycopy(arguments, 0, newArguments, 1,
				arguments.length);
		try {
			execute(newArguments, new File("."), verbose);
		} catch (Exception e) {
			throw new FakeException("Could not even fall back "
				+ " to javac in the PATH");
		}
	}

	// returns all .java files in the list, and returns a list where
	// all the .java files have been replaced by their .class files.
	protected static List compileJavas(List javas, File cwd,
			String javaVersion, boolean verbose,
			boolean showDeprecation, String extraClassPath)
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
		String classPath = discoverClassPath();
		if (extraClassPath != null && !extraClassPath.equals("")) {
			StringTokenizer tokenizer =
				new StringTokenizer(extraClassPath, ":");
			while (tokenizer.hasMoreElements())
				classPath += File.pathSeparator
					+ makePath(cwd, tokenizer.nextToken());
		}
		if (classPath != null && !classPath.equals("")) {
			arguments.add("-classpath");
			arguments.add(classPath);
		}

		int optionCount = arguments.size();
		Iterator iter = javas.iterator();
		while (iter.hasNext()) {
			String path = (String)iter.next();

			if (path.endsWith(".java"))
				arguments.add(makePath(cwd, path));
		}

		/* Do nothing if there is nothing to do ;-) */
		if (optionCount == arguments.size())
			return javas;

		String[] args = (String[])arguments.toArray(new
				String[arguments.size()]);

		if (verbose) {
			String output = "Compiling .java files: javac";
			for (int i = 0; i < args.length; i++)
				output += " " + args[i];
			System.err.println(output);
		}

		try {
			callJavac(args, verbose);
		} catch (FakeException e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			throw new FakeException("Compile error: " + e);
		}

		List result = java2classFiles(javas, cwd);
		return result;
	}

	protected static void addPluginsConfigToJar(JarOutputStream jar,
			String configPath) throws IOException {
		if (configPath == null)
			return;

		JarEntry entry = new JarEntry("plugins.config");
		jar.putNextEntry(entry);
		byte[] buffer = readFile(configPath);
		jar.write(buffer, 0, buffer.length);
		jar.closeEntry();
	}

	protected static void makeJar(String path, String mainClass, List files,
			File cwd, String configPath, boolean verbose)
			throws FakeException {
		path = makePath(cwd, path);
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
			String origPath = null;
			if (moveFileOutOfTheWay(path)) {
				origPath = path;
				path += ".new";
			}

			OutputStream out = new FileOutputStream(path);
			JarOutputStream jar = manifest == null ?
				new JarOutputStream(out) :
				new JarOutputStream(out, manifest);

			addPluginsConfigToJar(jar, configPath);
			String lastBase = null;
			Iterator iter = files.iterator();
			while (iter.hasNext()) {
				String realName = (String)iter.next();
				String name = realName;
				byte[] buffer = readFile(makePath(cwd,
								realName));
				if (buffer == null)
					throw new FakeException("File "
						+ realName + " does not exist,"
						+ " could not make " + path);
				if (realName.endsWith(".class")) {
					ByteCodeAnalyzer analyzer =
						new ByteCodeAnalyzer(buffer);
					name = analyzer.getPathForClass()
						+ ".class";
					if (realName.endsWith(name))
						lastBase = realName.substring(0,
							realName.length()
							- name.length());
					else
						lastBase = null;
				}
				else if (lastBase != null &&
						realName.startsWith(lastBase))
					name = realName
						.substring(lastBase.length());

				JarEntry entry = new JarEntry(name);
				jar.putNextEntry(entry);
				jar.write(buffer, 0, buffer.length);
				jar.closeEntry();
			}

			jar.close();
			if (origPath != null)
				throw new FakeException("Could not remove "
					+ origPath
					+ " before building it anew\n"
					+ "Stored it as " + path
					+ " instead.");
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
		if (target.equals(source))
			return;
		try {
			if (!target.startsWith("/"))
				target = cwd + "/" + target;
			if (!source.startsWith("/"))
				source = cwd + "/" + source;
			File parent = new File(target).getParentFile();
			if (!parent.exists())
				parent.mkdirs();
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
			boolean verbose) throws IOException, FakeException {
		execute(arguments, new File(file).getParentFile(), verbose);
	}

	protected static void execute(String[] args, String file,
			boolean verbose) throws IOException, FakeException {
		execute(args, new File(file).getParentFile(), verbose);
	}

	protected static void execute(List arguments, File dir, boolean verbose)
			throws IOException, FakeException {
		String[] args = new String[arguments.size()];
		arguments.toArray(args);
		execute(args, dir, verbose);
	}

	protected static void execute(String[] args, File dir, boolean verbose)
			throws IOException, FakeException {
		if (verbose) {
			String output = "Executing:";
			for (int i = 0; i < args.length; i++)
				output += " '" + args[i] + "'";
			System.err.println(output);
		}

		if (args[0].endsWith(".py")) {
			String args0orig = args[0];
			args[0] = makePath(dir, args[0]);
			if (executePython(args))
				return;
			if (verbose)
				System.err.println("Falling back to Python ("
					+ "Jython was not found in classpath)");
			args[0] = args0orig;
		}

		/* stupid, stupid Windows... */
		if (getPlatform().startsWith("win"))
			for (int i = 0; i < args.length; i++)
				args[i] = quoteArg(args[i]);

		Process proc = Runtime.getRuntime().exec(args, null, dir);
		new StreamDumper(proc.getErrorStream(), System.err).start();
		new StreamDumper(proc.getInputStream(), System.out).start();
		try {
			proc.waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
			throw new FakeException(e.getMessage());
		}
		int exitValue = proc.exitValue();
		if (exitValue != 0)
			throw new FakeException("Failed: " + exitValue);
	}

	private static String quotables = " \"\'";
	public static String quoteArg(String arg) {
		return quoteArg(arg, quotables);
	}

	public static String quoteArg(String arg, String quotables) {
		for (int j = 0; j < arg.length(); j++) {
			char c = arg.charAt(j);
			if (quotables.indexOf(c) >= 0) {
				String replacement;
				if (c == '"') {
					if (System.getenv("MSYSTEM") != null)
						replacement = "\\" + c;
					else
						replacement = "'" + c + "'";
				}
				else
					replacement = "\"" + c + "\"";
				arg = arg.substring(0, j)
					+ replacement
					+ arg.substring(j + 1);
				j += replacement.length() - 1;
			}
		}
		return arg;
	}


	protected static Constructor jythonCreate;
	protected static Method jythonExec, jythonExecfile;

	protected static boolean executePython(String[] args)
			throws FakeException {
		if (jythonExecfile == null) try {
			discoverJython();
			ClassLoader loader = getClassLoader();
			String className = "org.python.util.PythonInterpreter";
			Class main = loader.loadClass(className);
			Class[] argsType = new Class[] { };
			jythonCreate = main.getConstructor(argsType);
			argsType = new Class[] { args[0].getClass() };
			jythonExec = main.getMethod("exec", argsType);
			argsType = new Class[] { args[0].getClass() };
			jythonExecfile = main.getMethod("execfile", argsType);
		} catch (Exception e) {
			return false;
		}

		try {
			Object instance =
				jythonCreate.newInstance(new Object[] { });
			String init = "import sys\n" +
				"sys.argv = [";
			for (int i = 0; i < args.length; i++)
				init += (i > 0 ? ", " : "")
					+ "\"" + quoteArg(args[i], "\"") + "\"";
			init += "]\n";
			jythonExec.invoke(instance, new Object[] { init });
			String sysPath = "sys.path.insert(0, '"
				+ new File(args[0]).getParent() + "')";
			jythonExec.invoke(instance, new Object[] { sysPath });
			jythonExecfile.invoke(instance,
					new Object[] { args[0] });
		} catch (InvocationTargetException e) {
			e.getTargetException().printStackTrace();
			throw new FakeException("Jython failed");
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	protected void fakeOrMake(File cwd, String directory, boolean verbose,
			boolean ignoreMissingFakefiles, String toolsPath,
			String classPath) throws FakeException {
		String fakeFile = directory + '/' + Parser.path;
		boolean tryFake = new File(fakeFile).exists();
		if (ignoreMissingFakefiles && !tryFake &&
				!(new File(directory + "/Makefile").exists())) {
			if (verbose)
				System.err.println("Ignore " + directory);
			return;
		}
		System.err.println((tryFake ? "F" : "M") + "aking in "
			+ directory + (directory.endsWith("/") ? "" : "/"));

		try {
			if (tryFake) {
				// Try "Fake"
				Parser parser = new Parser(fakeFile);
				if (toolsPath != null)
					parser.setVariable("TOOLSPATH",
							toolsPath);
				if (classPath != null)
					parser.setVariable("CLASSPATH",
							classPath);
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

		class ClassNameIterator implements Iterator {
			int index;

			ClassNameIterator() {
				index = -1;
				findNext();
			}

			void findNext() {
				while (++index < poolOffsets.length)
					if (getU1(poolOffsets[index]) == 7)
						break;
			}

			public boolean hasNext() {
				return index < poolOffsets.length;
			}

			public Object next() {
				int offset = poolOffsets[index];
				findNext();
				return getString(dereferenceOffset(offset + 1));
			}

			public void remove()
					throws UnsupportedOperationException {
				throw new UnsupportedOperationException();
			}
		}

		public Iterator getClassNames() {
			return new ClassNameIterator();
		}

		public String toString() {
			String result = "";
			for (int i = 0; i < poolOffsets.length; i++) {
				int offset = poolOffsets[i];
				result += "index #" + i + ": "
					+ format(offset) + "\n";
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
			InputStream input = getResourceAsStream(path, !true);
			if (input == null)
				throw new ClassNotFoundException(name);
			try {
				byte[] buffer = readStream(input);
				input.close();
				result = defineClass(name,
						buffer, 0, buffer.length);
				cache.put(name, result);
				return result;
			} catch (IOException e) { return null; }
		}
	}

	static byte[] readStream(InputStream input) throws IOException {
		byte[] buffer = new byte[1024];
		int offset = 0, len = 0;
		for (;;) {
			if (offset == buffer.length)
				buffer = realloc(buffer,
						2 * buffer.length);
			len = input.read(buffer, offset,
					buffer.length - offset);
			if (len < 0)
				return realloc(buffer, offset);
			offset += len;
		}
	}

	static byte[] realloc(byte[] buffer, int newLength) {
		if (newLength == buffer.length)
			return buffer;
		byte[] newBuffer = new byte[newLength];
		System.arraycopy(buffer, 0, newBuffer, 0,
				Math.min(newLength, buffer.length));
		return newBuffer;
	}

	public static List uniq(List list) {
		Set set = new HashSet();
		List result = new ArrayList();
		Iterator iter = list.iterator();
		while (iter.hasNext()) {
			Object key = iter.next();
			if (set.contains(key))
				continue;
			result.add(key);
			set.add(key);
		}
		return result;
	}

	public static String join(List list) {
		return join(list, " ");
	}

	public static String join(List list, String separator) {
		Iterator iter = list.iterator();
		String result = iter.hasNext() ? iter.next().toString() : "";
		while (iter.hasNext())
			result += separator + iter.next();
		return result;
	}

	static boolean moveFileOutOfTheWay(String file) throws FakeException {
		return moveFileOutOfTheWay(new File(file));
	}

	static boolean moveFileOutOfTheWay(File file) throws FakeException {
		if (!file.exists())
			return false;
		if (file.delete())
			return false;
		if (file.renameTo(new File(file.getPath() + ".old")))
			return true;
		throw new FakeException("Could not move " + file
				+ " out of the way");
	}


	// our very own exception

	static class FakeException extends Exception {
		public static final long serialVersionUID = 1;
		public FakeException(String message) {
			super(message);
		}

		public String toString() {
			return getMessage();
		}
	}
}
