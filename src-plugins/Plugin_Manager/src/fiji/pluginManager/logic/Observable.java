package fiji.pluginManager.logic;

public interface Observable {
	public void notifyObservers();

	public void register(Observer obs);

}