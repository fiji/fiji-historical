import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import java.lang.reflect.Method;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import java.util.jar.Manifest;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.JarOutputStream;

import java.util.regex.Pattern;

public class Fake {
	protected static boolean debug = true;
	protected static Method javac;

	public static void main(String[] args) {
		new Fake().make(args);
	}

	public void make(String[] args) {
		try {
			Parser parser = new Parser(args);
			Rule all = parser.parseRules();
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
		String path = "Fakefile";
		BufferedReader reader;
		String line;
		int lineNumber;

		public Parser(String[] args) throws FakeException {
			if (args != null && args.length > 0)
				path = args[0];

			try {
				InputStream stream = new FileInputStream(path);
				InputStreamReader input =
					new InputStreamReader(stream);
				reader = new BufferedReader(input);
			} catch (IOException e) {
				error("Could not read file: " + path);
			}

			lineNumber = 0;
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
				if (arrow < 0)
					error("Invalid line (missing colon)");

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

		protected void error(String message) throws FakeException {
			throw new FakeException(path + ":" + lineNumber + ": "
					+ message + "\n\t" + line);
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
		if (debug)
			System.err.println("expandGlob " + glob + (newerThan > 0
				?  " > " + new java.util.Date(newerThan) : ""));

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
	protected static List compileJavas(List javas) throws FakeException {
		List arguments = new ArrayList();
		arguments.add("-source");
		arguments.add("1.5");
		arguments.add("-target");
		arguments.add("1.5");

		Iterator iter = javas.iterator();
		while (iter.hasNext()) {
			String path = (String)iter.next();
			if (path.endsWith(".java"))
				arguments.add(path);
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


	// the rule pool

	protected static Map allRules = new HashMap();

	public static void addRule(Rule rule) {
		allRules.put(rule.target, rule);
	}

	public static Rule addRule(String target, String prerequisites)
			throws FakeException {
		List list = new ArrayList();
		StringTokenizer tokenizer = new StringTokenizer(prerequisites);

		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			if (expandGlob(token, list) == 0)
				throw new FakeException("Glob did not match any"
					+ " file: '" + token + "'");
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
		else if (target.endsWith(")")) {
			int paren = target.indexOf('(');

			if (paren < 0)
				throw new FakeException("Invalid rule");

			String program = target.substring(paren + 1,
				target.length() - 1);
			target = target.substring(0, paren - 1).trim();

			rule = new ExecuteProgram(target, list, program);
		}

		if (rule == null)
			throw new FakeException("Unrecognized rule");

		addRule(rule);

		return rule;
	}



	// the different rule types

	abstract static class Rule {
		protected String target;
		protected List prerequisites, nonUpToDates;

		Rule(String target, List prerequisites) {
			this.target = target;
			this.prerequisites = prerequisites;
		}

		abstract void action() throws FakeException;

		boolean upToDate() {
			// this implements the mtime check
			File file = new File(target);
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
				action();
			} catch (Exception e) {
				error(e.getMessage());
			}
		}

		protected void error(String message) throws FakeException {
			throw new FakeException(message
					+ "\n\tin rule " + this);
		}

		public String toString() {
			String result = "";
			if (debug) {
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

	static class All extends Rule {
		All(String target, List prerequisites) {
			super(target, prerequisites);
		}

		public void action() throws FakeException {
			Iterator iter = prerequisites.iterator();
			while (iter.hasNext()) {
				String prereq = (String)iter.next();

				if (!allRules.containsKey(prereq))
					error("Unknown target: " + prereq);

				Rule rule = (Rule)allRules.get(prereq);
				rule.make();
			}
		}
	}

	static class SubFake extends Rule {
		SubFake(String target, List prerequisites) {
			super(target, prerequisites);
		}

		void action() throws FakeException {
			error("Not yet implemented");
		}
	}

	static class CopyJar extends Rule {
		CopyJar(String target, List prerequisites) {
			super(target, prerequisites);
		}

		void action() throws FakeException {
			error("Not yet implemented");
		}
	}

	static class CompileJar extends Rule {
		CompileJar(String target, List prerequisites) {
			super(target, prerequisites);
		}

		void action() throws FakeException {
			List files = compileJavas(nonUpToDates);
			makeJar(target, null, files);
		}
	}

	static class CompileClass extends Rule {
		CompileClass(String target, List prerequisites) {
			super(target, prerequisites);
		}

		void action() throws FakeException {
			error("Not yet implemented");
		}
	}

	static class ExecuteProgram extends Rule {
		String program;
		ExecuteProgram(String target, List prerequisites,
				String program) {
			super(target, prerequisites);
			this.program = program;
		}

		void action() throws FakeException {
			error("Not yet implemented");
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
