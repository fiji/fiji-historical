package fiji.PluginManager;

public class Dependency {
	private String filename = "";
	private long timestamp = 0;
	public Dependency(String filename, long timestamp) {
		this.filename = filename;
		this.timestamp = timestamp;
	}
	public String getFilename() {
		return filename;
	}
	public long getTimestamp() {
		return timestamp;
	}
}
