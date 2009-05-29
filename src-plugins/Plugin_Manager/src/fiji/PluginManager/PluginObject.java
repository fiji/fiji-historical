package fiji.PluginManager;
import java.util.List;


public class PluginObject {
	private String strFilename; //Identifier
	private String md5Sum; //Used for comparison: Determine if update needed
	private String timestamp; //Version of plugin file ("Co-Identifier")
	private String newMd5Sum; //if any
	private String newTimestamp; //if any
	private String directory; //Where should the plugin file (Loaded) be located
	private String description;
	public static final byte STATUS_UNINSTALLED = 0; //Meaning current status is not installed
	public static final byte STATUS_INSTALLED = 1; //Meaning current status is installed
	public static final byte STATUS_MAY_UPDATE = 2; //Meaning installed AND update-able
	private byte status = 0; //default
	public static final byte ACTION_NONE = 0; //No action; Remain as it is
	public static final byte ACTION_REVERSE = 1; //Install if not installed, Uninstall if installed
	public static final byte ACTION_UPDATE = 2; //Only possibly valid for (status == 2)
	private byte action = ACTION_NONE; //default
	private List<Dependency> dependency; //Dependency object: filename and timestamp

	public PluginObject(String strFilename, String md5Sum, String timestamp) {
		this.strFilename = strFilename;
		this.md5Sum = md5Sum;
		this.timestamp = timestamp;
	}

	public PluginObject(String strFilename, String md5Sum, String timestamp, String description, List<Dependency> dependency, byte status, byte action) {
		this.strFilename = strFilename;
		this.md5Sum = md5Sum;
		this.timestamp = timestamp;
		this.description = description;
		this.dependency = dependency;
		this.status = status;
		this.action = action;
	}

	public void setToUpdateable(String newMd5Sum, String newTimestamp) {
		setStatus(PluginObject.STATUS_MAY_UPDATE); //set status, if not done so already
		this.newMd5Sum = newMd5Sum;
		this.newTimestamp = newTimestamp;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setDependency(List<Dependency> dependency) {
		this.dependency = dependency;
	}

	public void setStatus(byte status) {
		this.status = status;
	}

	public void setAction(byte action) {
		this.action = action;
	}

	public String getFilename() {
		return strFilename;
	}

	public String getmd5Sum() {
		return md5Sum;
	}
	
	public String getNewMd5Sum() {
		return newMd5Sum;
	}

	public String getTimestamp() {
		return timestamp;
	}
	
	public String getNewTimestamp() {
		return newTimestamp;
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

	public byte getStatus() {
		return status;
	}

	public byte getAction() {
		return action;
	}
}
