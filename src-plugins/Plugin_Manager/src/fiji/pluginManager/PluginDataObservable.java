package fiji.pluginManager;

import java.util.Vector;

/*
 * Class functionality:
 * A specialized version of PluginData.
 * 
 * Allows a user interface to observe it. Class is designed for performing multiple
 * tasks, allowing the user to decide when to inform the interface of its status.
 */
public class PluginDataObservable extends PluginData implements Observable {
	private Vector<Observer> observersList;
	protected String taskname; //Generic title of the current task, namely a filename
	protected int currentlyLoaded;
	protected int totalToLoad;
	protected boolean allTasksComplete;

	public PluginDataObservable(boolean forServer) {
		super(forServer);
		observersList = new Vector<Observer>();
	}

	public PluginDataObservable() {
		super(false);
		observersList = new Vector<Observer>();
	}

	public String getTaskname() {
		return taskname;
	}

	public int getCurrentlyLoaded() {
		return currentlyLoaded;
	}

	public int getTotalToLoad() {
		return totalToLoad;
	}

	public boolean allTasksComplete() {
		return allTasksComplete;
	}

	//PluginDataObservable notifies its observers
	public void notifyObservers() {
		// Send notify to all Observers
		for (int i = 0; i < observersList.size(); i++) {
			Observer observer = (Observer) observersList.elementAt(i);
			observer.refreshData(this);
		}
	}

	//PluginDataObservable adds observers to inform them of any changes
	public void register(Observer obs) {
		if (obs != null)
			observersList.addElement(obs);
	}

	public void unRegister(Observer obs) {}

	protected void changeStatus(String taskname, int currentlyLoaded, int totalToLoad) {
		this.taskname = taskname;
		this.currentlyLoaded = currentlyLoaded;
		this.totalToLoad = totalToLoad;
		notifyObservers();
	}

	protected void setStatusComplete() {
		allTasksComplete = true;
		notifyObservers();
	}

}