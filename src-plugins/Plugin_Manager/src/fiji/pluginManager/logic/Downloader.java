package fiji.pluginManager.logic;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/* 
 * Direct responsibility: Download a list of files given their respective URLs to their
 * respective destinations. Updates its download status to its Observer as well.
 */
public class Downloader {
	private int downloadedBytes;
	private int downloadSize;
	private HttpURLConnection connection;
	private List<DownloadListener> listeners;
	private InputStream in;
	private OutputStream out;
	private Iterator<SourceFile> sourceFiles;
	private SourceFile currentSource;
	private boolean cancelled; //stop download entirely

	public Downloader(Iterator<SourceFile> sourceFiles) {
		this.sourceFiles = sourceFiles;
		listeners = new ArrayList<DownloadListener>();
	}

	public void cancelDownload() {
		cancelled = true;
	}

	public void startDownload() throws IOException {
		while (sourceFiles.hasNext() && !cancelled) {
			currentSource = sourceFiles.next();
			//Start connection
			connection = (HttpURLConnection)(new URL(currentSource.getURL())).openConnection();
			downloadedBytes = 0; //start with nothing downloaded
			downloadSize = connection.getContentLength();
			if (downloadSize < 0)
				throw new Error("Content Length is not known");
			notifyListenersUpdate(); //first notification starts from 0

			System.out.println("Trying to connect to " + connection.getURL().toString() + "...");
			new File(currentSource.getDestination()).getParentFile().mkdirs();
			in = connection.getInputStream();
			out = new FileOutputStream(currentSource.getDestination());

			//Start actual downloading and writing to file
			byte[] buffer = new byte[65536];
			int count;
			while ((count = in.read(buffer)) >= 0 && !cancelled) {
				out.write(buffer, 0, count);
				downloadedBytes += count;
				notifyListenersUpdate();
			}

			//end connection once download done
			notifyListenersCompletion();
			in.close();
			out.close();
			connection.disconnect();
		}
	}

	public void notifyListenersUpdate() {
		for (DownloadListener listener : listeners) {
			listener.update(currentSource, downloadedBytes, downloadSize);
		}
	}

	public void notifyListenersCompletion() {
		for (DownloadListener listener : listeners) {
			listener.completion(currentSource);
		}
	}

	public void addListener(DownloadListener listener) {
		listeners.add(listener);
	}

	public interface DownloadListener {
		public void update(SourceFile source, int bytesSoFar, int bytesTotal);
		public void completion(SourceFile source);
	}

	public interface SourceFile {
		public String getDestination();
		public String getURL();
	}
}