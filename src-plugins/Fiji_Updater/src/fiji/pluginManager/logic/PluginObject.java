package fiji.pluginManager.logic;
import java.util.ArrayList;
import java.util.List;

public class PluginObject {
	private String strFilename; //Main identifier
	private String md5Sum; //Used for comparison: Determine if update needed
	private String timestamp; //Version of plugin file ("Unique within each filename")
	private String newMd5Sum; //if any
	private String newTimestamp; //if any
	private PluginDetails pluginDetails;
	private long filesize;
	private List<Dependency> dependency; //Dependency object: filename and timestamp

	//Status of its record in database
	private boolean fiji; //name in records or not
	private boolean recorded; //its md5 sum in records or not
	private boolean readOnly; //physical file (local side) read-only?

	//Current physical state of plugin
	public static enum CurrentStatus { UNINSTALLED, INSTALLED, UPDATEABLE };
	private CurrentStatus status = CurrentStatus.UNINSTALLED;

	//Action that user indicates to take
	//Note here, "REVERSE" means install if not installed; uninstall if installed
	public static enum Action { NONE, REVERSE, UPDATE, UPLOAD };
	private Action action = Action.NONE;

	//State to indicate whether Plugin removed/downloaded successfully
	public static enum ChangeStatus { NONE, SUCCESS, FAIL };
	private ChangeStatus changedStatus = ChangeStatus.NONE; //default

	public PluginObject(String strFilename, String md5Sum, String timestamp, CurrentStatus status,
			boolean fiji, boolean recorded) {
		this.strFilename = strFilename;
		this.md5Sum = md5Sum;
		this.timestamp = timestamp;
		this.status = status;
		this.fiji = fiji;
		this.recorded = recorded;
		pluginDetails = new PluginDetails(); //default: no information, empty
	}

	public void setUpdateDetails(String newMd5Sum, String newTimestamp) {
		setStatus(CurrentStatus.UPDATEABLE); //set status, if not done so already
		this.newMd5Sum = newMd5Sum;
		this.newTimestamp = newTimestamp;
	}

	private void setStatus(CurrentStatus status) {
		this.status = status;
	}

	//set description details
	public void setPluginDetails(PluginDetails pluginDetails) {
		if (pluginDetails == null)
			throw new Error("Plugin " + strFilename + " cannot have null PluginDetails object.");
		this.pluginDetails = pluginDetails;
	}

	public void setDependency(List<Dependency> dependency) {
		this.dependency = dependency;
	}

	public void setDependency(Iterable<String> dependencies, PluginCollection allPlugins) {
		dependency = new ArrayList<Dependency>();
		if (dependencies == null)
			return;
		for (String file : dependencies) {
			//Only add if JAR file is in Fiji records
			PluginObject other = allPlugins.getPlugin(file);
			if (other != null)
				dependency.add(new Dependency(file, other.getTimestamp(), "at-least"));
		}
	}

	public void setFilesize(long filesize) {
		this.filesize = filesize;
	}

	public void setActionToInstall() {
		if (isInstallable())
			setAction(Action.REVERSE);
		else
			throw new Error("Plugin " + strFilename + " cannot install as its current state is not UNINSTALLED.");
	}

	public void setActionToRemove() {
		if (isRemovable())
			setAction(Action.REVERSE);
		else
			throw new Error("Plugin " + strFilename + " cannot remove as its current state was never installed.");
	}

	public void setActionToUpdate() {
		if (isUpdateable())
			setAction(Action.UPDATE);
		else
			throw new Error("Plugin " + strFilename + " cannot update as its current state is not UPDATEABLE.");
	}

	public void setActionToUpload() {
		setAction(Action.UPLOAD);
	}

	public void setActionNone() {
		setAction(Action.NONE);
	}

	public void resetChangeStatuses() {
		this.changedStatus = ChangeStatus.NONE;
	}

	public void setChangeStatusToSuccess() { //Indicates successful download/uninstall
		this.changedStatus = ChangeStatus.SUCCESS;
	}

	public void setChangeStatusToFail() {
		this.changedStatus = ChangeStatus.FAIL;
	}

	private void setAction(Action action) {
		this.action = action;
	}
	
	public void setIsReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
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

	public PluginDetails getPluginDetails() {
		return pluginDetails;
	}

	public long getFilesize() {
		return filesize;
	}

	public List<Dependency> getDependencies() {
		return dependency;
	}

	public CurrentStatus getStatus() {
		return status;
	}

	public Action getAction() {
		return action;
	}

	public boolean isInstallable() {
		return (status == CurrentStatus.UNINSTALLED);
	}

	public boolean isUpdateable() {
		return (status == CurrentStatus.UPDATEABLE);
	}

	public boolean isRemovableOnly() {
		return (status == CurrentStatus.INSTALLED);
	}

	public boolean isRemovable() {
		return (status == CurrentStatus.INSTALLED || status == CurrentStatus.UPDATEABLE);
	}

	public boolean actionSpecified() {
		return (action != Action.NONE);
	}

	public boolean toUpdate() {
		return (isUpdateable() && action == Action.UPDATE);
	}

	public boolean toRemove() {
		return (isRemovable() && action == Action.REVERSE);
	}

	public boolean toInstall() {
		return (isInstallable() && action == Action.REVERSE);
	}

	public boolean toUpload() {
		return (action == Action.UPLOAD);
	}

	public boolean isFijiPlugin() {
		return fiji;
	}

	public boolean isInRecords() {
		return recorded;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public boolean changeSucceeded() {
		return (changedStatus == ChangeStatus.SUCCESS); //managed to download/uninstall
	}

	public boolean changeFailed() {
		return (changedStatus == ChangeStatus.FAIL);
	}

	public boolean changeNotDone() {
		return (changedStatus == ChangeStatus.NONE);
	}

}
