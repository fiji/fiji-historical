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
		setStatusComplete(); //indicate to observer there's no more tasks
	}

	private void downloadAndSave(String url, String filename) throws IOException {
		saveFileLocation = getSaveToLocation(PluginManager.XML_DIRECTORY, filename);

		//progress starts out at 0 for download of a single file
		changeStatus(filename, 0, 0);

		Downloader downloader = new Downloader(url, saveFileLocation);
		totalToLoad += downloader.getSize();
		downloader.startDownloadAndObserve(this);
	}

	//As Observer of Downloaders, XMLFileDownloader gathers how much downloaded so far
	public void refreshData(Observable subject) {
		Downloader myDownloader = (Downloader)subject;
		currentlyLoaded = myDownloader.getBytesSoFar();
		System.out.println("Downloaded so far: " + currentlyLoaded);
		changeStatus(taskname, currentlyLoaded, totalToLoad);
	}
}
