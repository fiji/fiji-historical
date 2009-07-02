package fiji.pluginManager;

import java.util.Vector;

public class PluginDataObservable extends PluginData implements Observable {
	private Vector<Observer> observersList;
	protected String taskname;
	protected int currentlyLoaded;
	protected int totalToLoad;
	protected boolean allTasksComplete;

	public PluginDataObservable(Observer observer, boolean forServer) {
		super(forServer);
		initializePluginDataObservable(observer);
	}

	public PluginDataObservable(Observer observer) {
		initializePluginDataObservable(observer);
	}

	private void initializePluginDataObservable(Observer observer) {
		observersList = new Vector<Observer>();
		register(observer);
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