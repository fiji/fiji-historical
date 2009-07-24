package fiji.pluginManager.logic;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import fiji.pluginManager.utilities.Downloader;
import fiji.pluginManager.utilities.CompressionUtility;
import fiji.pluginManager.utilities.Downloader.FileDownload;

/*
 * Directly in charge of downloading and saving start-up files (i.e.: XML file and related).
 */
public class XMLFileDownloader extends PluginDataObservable implements Downloader.DownloadListener {
	private List<FileDownload> sources;
	private long xmlLastModified;

	public void startDownload() {
		sources = new ArrayList<FileDownload>();
		String xml_url = PluginManager.MAIN_URL + PluginManager.XML_COMPRESSED_FILENAME;
		addToDownload(xml_url, PluginManager.XML_COMPRESSED_FILENAME);
		addToDownload(PluginManager.MAIN_URL + PluginManager.DTD_FILENAME,
				PluginManager.DTD_FILENAME);

		//Record last modified date of XML, used for uploading purposes (Lock conflict)
		try {
			xmlLastModified = new URL(xml_url).openConnection().getLastModified();
		} catch (Exception ex) {
			throw new Error("Failed to get last modified date of XML document");
		}

		//Start downloading the required files
		Downloader downloader = new Downloader(sources.iterator());
		downloader.addListener(this);
		downloader.startDownload();

		//Uncompress the XML file
		try {
			String compressedFileLocation = prefix(PluginManager.XML_COMPRESSED_FILENAME);
			String xmlFileLocation = prefix(PluginManager.XML_FILENAME);
			byte[] data = CompressionUtility.getDecompressedData(
					new FileInputStream(compressedFileLocation));
			FileOutputStream saveFile = new FileOutputStream(xmlFileLocation); //if needed...
			saveFile.write(data);
			saveFile.close();
		} catch (Exception e) {
			throw new Error("Failed to decompress XML file.");
		}

		setStatusComplete(); //indicate to observer there's no more tasks
	}

	public long getXMLLastModified() {
		return xmlLastModified;
	}

	private void addToDownload(String url, String filename) {
		sources.add(new InformationSource(filename, url, prefix(filename)));
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
	}

	public void fileFailed(FileDownload source, Exception e) {
		try {
			new File(source.getDestination()).delete();
		} catch (Exception e2) { }
		throw new Error("Failed to save from " + source.getURL() + ", " +
				e.getLocalizedMessage());
	}
}
