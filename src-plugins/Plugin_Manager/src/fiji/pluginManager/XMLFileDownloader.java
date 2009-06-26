package fiji.pluginManager;
import java.io.File;

public class XMLFileDownloader extends PluginDataObservable implements Observer {
	private String saveFileLocation;

	public XMLFileDownloader(Observer observer) {
		super(observer);
	}

	public void startDownload() {
		saveFileLocation = getSaveToLocation(PluginManager.XML_DIRECTORY, PluginManager.XML_FILENAME);

		//progress starts out at 0 for download of a single file
		taskname = PluginManager.XML_FILENAME;
		currentlyLoaded = 0 ;
		totalToLoad = 0;
		notifyObservers();

		try {
			//Establishes connection
			Downloader downloader = new Downloader(PluginManager.XML_FILE_URL, saveFileLocation);
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

			allTasksComplete = true;
			notifyObservers();

		} catch (Exception e) {
			try {
				new File(saveFileLocation).delete();
			} catch (Exception e2) { }
			throw new Error("Could not download " + taskname + " successfully: " + e.getMessage());
		}
	}

	//As Observer of Downloaders, XMLFileDownloader gathers how much downloaded so far
	public void refreshData(Observable subject) {
		Downloader myDownloader = (Downloader)subject;
		currentlyLoaded += myDownloader.getNumOfBytes();
		System.out.println("Downloaded so far: " + currentlyLoaded);
		notifyObservers();
	}
}
