package fiji.pluginManager.logic;

public class Dependency {
	private String filename;
	private String timestamp;
	private String relation;
	public static final String RELATION_AT_LEAST = "at-least";
	public static final String RELATION_AT_MOST = "at-most";
	public static final String RELATION_EXACT = "exact";

	public Dependency(String filename, String timestamp, String relation) {
		this.filename = filename;
		this.timestamp = timestamp;
		this.relation = relation;
	}

	public String getFilename() {
		return filename;
	}

	public String getTimestamp() {
		return timestamp;
	}

	public String getRelation() {
		return relation;
	}
}