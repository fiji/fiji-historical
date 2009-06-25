package fiji.pluginManager;

import java.io.File;
import java.util.Vector;

public class XMLFileDownloader extends PluginData implements Observable, Observer  {
	private Vector<Observer> observersList;
	private final String infoDirectory = "plugininfo";
	private String filename;
	private int currentlyLoaded;
	private int totalToLoad;
	private String saveFileLocation;
	private boolean downloadComplete;

	public XMLFileDownloader(Observer observer) {
		super();
		observersList = new Vector<Observer>();
		register(observer);
	}

	public void startDownload(String fileURL, String saveFile) {
		saveFileLocation = getSaveToLocation(infoDirectory, saveFile);

		//progress starts out at 0 for download of a single file
		filename = saveFile;
		currentlyLoaded = 0 ;
		totalToLoad = 0;
		notifyObservers();

		try {
			//Establishes connection
			Downloader downloader = new Downloader(fileURL, saveFileLocation);
			downloader.register(this);
			totalToLoad += downloader.getSize();

			//Prepare the necessary download input and output streams
			downloader.prepareDownload();
			byte[] buffer = downloader.createNewBuffer();
			int count;

			//Start actual downloading and writing to file
			while ((count = downloader.getNextPart(buffer)) >= 0) {
				downloader.writePart(buffer, count);
			}
			downloader.endConnection(); //end connection once download done

			downloadComplete = true;
			notifyObservers();

		} catch (Exception e) {
			try {
				new File(saveFileLocation).delete();
			} catch (Exception e2) { }
			throw new Error("Could not download " + filename + " successfully: " + e.getMessage());
		}
	}

	public String getSaveFileLocation() {
		return saveFileLocation;
	}

	public String getFilename() {
		return filename;
	}

	public int getCurrentlyLoaded() {
		return currentlyLoaded;
	}

	public int getTotalToLoad() {
		return totalToLoad;
	}

	public boolean downloadComplete() {
		return downloadComplete;
	}

	//Being observed, PluginDataReader notifies LoadStatusDisplay
	public void notifyObservers() {
		// Send notify to all Observers
		for (int i = 0; i < observersList.size(); i++) {
			Observer observer = (Observer) observersList.elementAt(i);
			observer.refreshData(this);
		}
	}

	//Being observed, PluginDataReader adds observers
	public void register(Observer obs) {
		observersList.addElement(obs);
	}

	public void unRegister(Observer obs) {}

	//As Observer of Downloaders, PluginDataReader gathers download information
	public void refreshData(Observable subject) {
		Downloader myDownloader = (Downloader)subject;
		currentlyLoaded += myDownloader.getNumOfBytes();
		System.out.println("Downloaded so far: " + currentlyLoaded);
		notifyObservers(); //Notify since data is observed by LoadStatusDisplay
	}
}
