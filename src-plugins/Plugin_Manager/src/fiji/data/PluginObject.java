package fiji.data;
import java.util.List;

public class PluginObject {
	private String strFilename = null; //Identifier
	private String md5Sum = null; //Used for comparison: Determine if update needed
	private String timestamp = null; //Version of plugin file ("Co-Identifier")
	private String directory = null; //Where should the plugin file (Loaded) be located
	private String description = null;
	private boolean status = false; //True: Installed, False: Not Installed
	private int action = 0; //0: Remain as it is, 1: Install, 2: Uninstall, 3: Update
	private List<Dependency> dependency = null; //2-element arrays of ==> 0: filename, 1: timestamp

	public PluginObject(String strFilename, String md5Sum, String timestamp, String directory) {
		this.strFilename = strFilename;
		this.md5Sum = md5Sum;
		this.timestamp = timestamp;
		this.directory = directory;
	}

	public PluginObject(String strFilename, String md5Sum, String timestamp, String directory, String description, List<Dependency> dependency, boolean status, int action) {
		this.strFilename = strFilename;
		this.md5Sum = md5Sum;
		this.timestamp = timestamp;
		this.directory = directory;
		this.description = description;
		this.dependency = dependency;
		this.status = status;
		this.action = action;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setDependency(List<Dependency> dependency) {
		this.dependency = dependency;
	}

	public void setStatusLoaded(boolean status) {
		this.status = status;
	}

	public void setAction(int action) {
		this.action = action;
	}

	public String getFilename() {
		return strFilename;
	}

	public String getmd5Sum() {
		return md5Sum;
	}

	public String getTimestamp() {
		return timestamp;
	}

	public String getDirectory() {
		return directory;
	}

	public String getDescription() {
		return description;
	}

	public List<Dependency> getDependencies() {
		return dependency;
	}

	public Dependency getDependency(int index) {
		return dependency.get(index);
	}

	public boolean getStatus() {
		return status;
	}

	public int getAction() {
		return action;
	}
}
