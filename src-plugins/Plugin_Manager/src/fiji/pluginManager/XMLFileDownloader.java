package fiji.pluginManager;
import java.io.File;
import java.io.IOException;

public class XMLFileDownloader extends PluginDataObservable implements Observer {
	private String saveFileLocation;

	public XMLFileDownloader(Observer observer) {
		super(observer);
	}

	public void startDownload() {
		try {
			downloadAndSave(PluginManager.MAIN_URL + PluginManager.TXT_FILENAME, PluginManager.TXT_FILENAME);
			downloadAndSave(PluginManager.MAIN_URL + PluginManager.TXT_FILENAME, PluginManager.TXT_FILENAME);
			//downloadAndSave(PluginManager.MAIN_URL + PluginManager.XML_FILENAME, PluginManager.XML_FILENAME);
			//downloadAndSave(PluginManager.MAIN_URL + PluginManager.DTD_FILENAME, PluginManager.DTD_FILENAME);
		} catch (Exception e) {
			try {
				new File(saveFileLocation).delete();
			} catch (Exception e2) { }
			throw new Error("Could not download " + taskname + " successfully: " + e.getMessage());
		}
		allTasksComplete = true;
		notifyObservers();
	}

	private void downloadAndSave(String url, String filename) throws IOException {
		saveFileLocation = getSaveToLocation(PluginManager.XML_DIRECTORY, filename);

		//progress starts out at 0 for download of a single file
		taskname = filename;
		currentlyLoaded = 0 ;
		totalToLoad = 0;
		notifyObservers();
		
		//Establishes connection
		Downloader downloader = new Downloader(url, saveFileLocation);
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
	}

	//As Observer of Downloaders, XMLFileDownloader gathers how much downloaded so far
	public void refreshData(Observable subject) {
		Downloader myDownloader = (Downloader)subject;
		currentlyLoaded += myDownloader.getNumOfBytes();
		System.out.println("Downloaded so far: " + currentlyLoaded);
		notifyObservers();
	}
}
