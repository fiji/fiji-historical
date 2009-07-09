package fiji.pluginManager.logic;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import fiji.pluginManager.logic.Downloader.SourceFile;

public class XMLFileDownloader extends PluginDataObservable implements Downloader.DownloadListener {
	private List<SourceFile> sources;

	public void startDownload() {
		try {
			sources = new ArrayList<SourceFile>();
			//TODO: Replace it with XML and DTD files (2 files to download)
			downloadAndSave(PluginManager.MAIN_URL + PluginManager.TXT_FILENAME, PluginManager.TXT_FILENAME);
			//downloadAndSave(PluginManager.MAIN_URL + PluginManager.XML_FILENAME, PluginManager.XML_FILENAME);
			//downloadAndSave(PluginManager.MAIN_URL + PluginManager.DTD_FILENAME, PluginManager.DTD_FILENAME);

			Downloader downloader = new Downloader(sources.iterator());
			downloader.addListener(this);
			downloader.startDownload();
		} catch (Exception e) {
			try {
				new File(getSaveToLocation(PluginManager.XML_DIRECTORY, taskname)).delete();
			} catch (Exception e2) { }
			throw new Error("Could not download " + taskname + " successfully: " + e.getMessage());
		}
		setStatusComplete(); //indicate to observer there's no more tasks
	}

	private void downloadAndSave(String url, String filename) {
		sources.add(new InformationSource(filename, url,
				getSaveToLocation(PluginManager.XML_DIRECTORY, filename)));
	}

	private class InformationSource implements SourceFile {
		private String destination;
		private String url;
		private String filename;

		public InformationSource(String filename, String url, String destination) {
			this.filename = filename;
			this.destination = destination;
			this.url = url;
		}

		public String getDestination() {
			return destination;
		}

		public String getURL() {
			return url;
		}
		
		public String getFilename() {
			return filename;
		}
	}

	public void completion(SourceFile source) {
	}

	public void update(SourceFile source, int bytesSoFar, int bytesTotal) {
		InformationSource src = (InformationSource)source;
		changeStatus(src.getFilename(), bytesSoFar, bytesTotal);
		System.out.println("Downloaded so far: " + currentlyLoaded);
	}
}
