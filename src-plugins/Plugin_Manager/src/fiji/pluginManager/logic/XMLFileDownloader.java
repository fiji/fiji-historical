package fiji.pluginManager.logic;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import fiji.pluginManager.logic.Downloader.FileDownload;

public class XMLFileDownloader extends PluginDataObservable implements Downloader.DownloadListener {
	private List<FileDownload> sources;

	public void startDownload(long xmlModifiedSince) {
		sources = new ArrayList<FileDownload>();
		//TODO: Replace it with XML and DTD files (2 files to download)
		addToDownload(PluginManager.MAIN_URL + PluginManager.TXT_FILENAME, PluginManager.TXT_FILENAME);
		//addToDownload(PluginManager.MAIN_URL + PluginManager.XML_COMPRESSED_FILENAME,
		//		PluginManager.XML_COMPRESSED_FILENAME);
		//addToDownload(PluginManager.MAIN_URL + PluginManager.DTD_FILENAME,
		//		PluginManager.DTD_FILENAME);

		Downloader downloader = new Downloader(sources.iterator());
		downloader.addListener(this);
		downloader.startDownload();

		//Uncompress the XML file
		try {
			FileUtility fileUtility = new FileUtility();
			String compressedFileLocation = getSaveToLocation(PluginManager.READ_DIRECTORY,
					PluginManager.XML_COMPRESSED_FILENAME);
			String xmlFileLocation = getSaveToLocation(PluginManager.READ_DIRECTORY,
					PluginManager.XML_FILENAME);
			byte[] data = fileUtility.getDecompressedData(
					new FileInputStream(compressedFileLocation));
			FileOutputStream saveFile = new FileOutputStream(xmlFileLocation); //if needed...
			saveFile.write(data);
			saveFile.close();
		} catch (Exception e) {
			throw new Error("Failed to decompress XML file.");
		}

		setStatusComplete(); //indicate to observer there's no more tasks
	}

	private void addToDownload(String url, String filename) {
		sources.add(new InformationSource(filename, url,
				getSaveToLocation(PluginManager.READ_DIRECTORY, filename)));
	}

	private class InformationSource implements FileDownload {
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

	public void fileComplete(FileDownload source) {}

	public void update(FileDownload source, int bytesSoFar, int bytesTotal) {
		InformationSource src = (InformationSource)source;
		changeStatus(src.getFilename(), bytesSoFar, bytesTotal);
		System.out.println("Downloaded so far: " + currentlyLoaded);
	}

	public void fileFailed(FileDownload source, Exception e) {
		try {
			new File(source.getDestination()).delete();
		} catch (Exception e2) { }
		throw new Error("Could not download " + taskname + " successfully: " + e.getLocalizedMessage());
	}
}
