import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import java.util.regex.Pattern;

public class Fake {
	protected static boolean debug = true;

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

	protected static Map allRules = new HashMap();

	public static void addRule(Rule rule) {
		allRules.put(rule.target, rule);
	}

	static class GlobFilter implements FilenameFilter {
		Pattern pattern;

		GlobFilter(String glob) {
			String regex = "^" + glob.replace(".", "\\.")
				.replace("^", "\\^").replace("$", "\\$")
				.replace("?", ".").replace("*", ".*") + "$";
			pattern = Pattern.compile(regex);
		}

		public boolean accept(File dir, String name) {
			return pattern.matcher(name).matches();
		}
	}

	protected static int expandGlob(String glob, List list)
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

		String[] names = parentDirectory.list(new GlobFilter(pattern));

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
						list);
				count += expandGlob(parentPath + names[i]
						+ remainder, list);
			}

		return count;
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

	abstract static class Rule {
		protected String target;
		protected List prerequisites;

		Rule(String target, List prerequisites) {
			this.target = target;
			this.prerequisites = prerequisites;
		}

		abstract void action() throws FakeException;

		boolean upToDate() {
			// this implements the mtime check
			File file = new File(target);
			if (!file.exists())
				return false;
			long targetModifiedTime = file.lastModified();

			Iterator iter = prerequisites.iterator();
			while (iter.hasNext()) {
				String prereq = (String)iter.next();
				if (new File(prereq).lastModified()
						> targetModifiedTime)
					return false;
			}

			return true;
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
			error("Not yet implemented");
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

	static class FakeException extends Exception {
		public FakeException(String message) {
			super(message);
		}

		public String toString() {
			return getMessage();
		}
	}
}
