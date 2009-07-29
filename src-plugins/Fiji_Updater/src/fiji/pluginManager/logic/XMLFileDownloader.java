package fiji.pluginManager.logic;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import fiji.pluginManager.utilities.Downloader;
import fiji.pluginManager.utilities.Compressor;
import fiji.pluginManager.utilities.Downloader.FileDownload;

/*
 * Directly in charge of downloading and saving start-up files (i.e.: XML file and related).
 */
public class XMLFileDownloader extends PluginDataObservable implements Downloader.DownloadListener {
	private List<FileDownload> sources;
	private long xmlLastModified;
	private final String dateRecords = "dates.dat";

	public void startDownload() throws IOException {
		sources = new ArrayList<FileDownload>();

		//Read the file dates.dat, get recorded download date of XML
		long dateRecorded = 0;
		try {
			DataInputStream datesFile = new DataInputStream(new
					FileInputStream(prefix(dateRecords)));
			dateRecorded = datesFile.readLong();
			datesFile.close();
		} catch (IOException e) {
			dateRecorded = 0;
		}

		//Record last modified date of XML for uploading purposes (Lock conflict)
		String xml_url = PluginManager.MAIN_URL + PluginManager.XML_COMPRESSED;
		try {
			URLConnection myConnection = new URL(xml_url).openConnection();
			myConnection.setUseCaches(false);
			xmlLastModified = myConnection.getLastModified();
		} catch (Exception ex) {
			throw new Error("Failed to get last modified date of XML document");
		}

		//if values are different, download XML and record new last modified date
		if (dateRecorded != xmlLastModified ||
				!new File(prefix(PluginManager.XML_COMPRESSED)).exists()) {
			try {
				DataOutputStream datesWriter = new DataOutputStream(new
					FileOutputStream(prefix(dateRecords)));
				datesWriter.writeLong(xmlLastModified);
				datesWriter.close();
			} catch (IOException e) {
				System.out.println("Warning: Unable to record XML last modified date.");
			}
			//In other words, only download XML when it has been modified
			addToDownload(xml_url, PluginManager.XML_COMPRESSED);
		}
		addToDownload(PluginManager.MAIN_URL + PluginManager.DTD_FILENAME,
				PluginManager.DTD_FILENAME);

		//Start downloading the required files
		Downloader downloader = new Downloader(sources.iterator());
		downloader.addListener(this);
		downloader.startDownload();

		//Uncompress the XML file
		String compressedFileLocation = prefix(PluginManager.XML_COMPRESSED);
		String xmlFileLocation = prefix(PluginManager.XML_FILENAME);
		byte[] data = Compressor.getDecompressedData(
				new FileInputStream(compressedFileLocation));
		FileOutputStream saveFile = new FileOutputStream(xmlFileLocation); //if needed...
		saveFile.write(data);
		saveFile.close();

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
