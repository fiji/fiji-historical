package fiji.PluginManager;

public class Dependency {
	private String filename;
	private String timestamp;

	public Dependency(String filename, String timestamp) {
		this.filename = filename;
		this.timestamp = timestamp;
	}

	public String getFilename() {
		return filename;
	}

	public String getTimestamp() {
		return timestamp;
	}
}
