package fiji.pluginManager.logic;
import java.util.List;

public class PluginObject {
	private String strFilename; //Main identifier
	private String md5Sum; //Used for comparison: Determine if update needed
	private String timestamp; //Version of plugin file ("Unique within each filename")
	private String newMd5Sum; //if any
	private String newTimestamp; //if any
	private String description;
	private int filesize;
	private List<Dependency> dependency; //Dependency object: filename and timestamp
	private boolean isFiji;

	//Current physical state and action-to-take of plugin as the User sees it
	public static final byte STATUS_UNINSTALLED = 0; //Meaning current status is not installed
	public static final byte STATUS_INSTALLED = 1; //Meaning current status is installed
	public static final byte STATUS_MAY_UPDATE = 2; //Meaning installed AND update-able
	private byte status = 0; //default
	public static final byte ACTION_NONE = 0; //No action; Remain as it is
	public static final byte ACTION_REVERSE = 1; //Install if not installed, Uninstall if installed
	public static final byte ACTION_UPDATE = 2; //Only possibly valid for (status == 2)
	public static final byte ACTION_UPLOAD = 3; //Only possible for Developers
	private byte action = ACTION_NONE; //default

	//State to indicate whether Plugin removed/downloaded successfully
	public static enum ChangeStatus { NONE, SUCCESS, FAIL };
	private ChangeStatus changedStatus = ChangeStatus.NONE; //default

	public PluginObject(String strFilename, String md5Sum, String timestamp, byte status, boolean isFiji) {
		this.strFilename = strFilename;
		this.md5Sum = md5Sum;
		this.timestamp = timestamp;
		this.status = status;
		this.isFiji = isFiji;
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

	public void setUpdateDetails(String newMd5Sum, String newTimestamp) {
		setStatus(PluginObject.STATUS_MAY_UPDATE); //set status, if not done so already
		this.newMd5Sum = newMd5Sum;
		this.newTimestamp = newTimestamp;
	}

	private void setStatus(byte status) {
		this.status = status;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setDependency(List<Dependency> dependency) {
		this.dependency = dependency;
	}

	public void setFilesize(int filesize) {
		this.filesize = filesize;
	}

	public void setActionToInstall() {
		if (isInstallable())
			setAction(PluginObject.ACTION_REVERSE);
		else
			throw new Error("Plugin " + strFilename + " cannot install as its current state is not UNINSTALLED.");
	}

	public void setActionToRemove() {
		if (isRemovable())
			setAction(PluginObject.ACTION_REVERSE);
		else
			throw new Error("Plugin " + strFilename + " cannot remove as its current state was never installed.");
	}

	public void setActionToUpdate() {
		if (isUpdateable())
			setAction(PluginObject.ACTION_UPDATE);
		else
			throw new Error("Plugin " + strFilename + " cannot update as its current state is not UPDATEABLE.");
	}

	public void setActionToUpload() {
		setAction(PluginObject.ACTION_UPLOAD);
	}

	public void setActionNone() {
		setAction(PluginObject.ACTION_NONE);
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

	private void setAction(byte action) {
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

	public String getDescription() {
		return description;
	}

	public int getFilesize() {
		return filesize;
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

	public boolean isInstallable() {
		return (status == PluginObject.STATUS_UNINSTALLED);
	}

	public boolean isUpdateable() {
		return (status == PluginObject.STATUS_MAY_UPDATE);
	}

	public boolean isRemovableOnly() {
		return (status == PluginObject.STATUS_INSTALLED);
	}

	public boolean isRemovable() {
		return (status == PluginObject.STATUS_INSTALLED ||
				status == PluginObject.STATUS_MAY_UPDATE);
	}

	public boolean actionSpecified() {
		return (action != PluginObject.ACTION_NONE);
	}

	public boolean toUpdate() {
		return (isUpdateable() && action == PluginObject.ACTION_UPDATE);
	}

	public boolean toRemove() {
		return (isRemovable() && action == PluginObject.ACTION_REVERSE);
	}

	public boolean toInstall() {
		return (isInstallable() && action == PluginObject.ACTION_REVERSE);
	}

	public boolean toUpload() {
		return (action == PluginObject.ACTION_UPLOAD);
	}

	public boolean isFijiPlugin() {
		return isFiji;
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
