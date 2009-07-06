package fiji.pluginManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Vector;

/*
 * Direct responsibility: Download a file given its URL to a given destination.
 * Updates its download status to its Observer as well.
 */
public class Downloader implements Observable {
	private volatile Thread downloadThread;
	private String strDestination;
	private int downloadedBytes;
	private int downloadSize;
	private HttpURLConnection connection;
	private Vector<Observer> observersList;
	private InputStream in;
	private OutputStream out;

	public Downloader(String strURL, String strDestination) throws IOException {
		if (strURL == null || strDestination == null)
			throw new Error("Downloader constructor parameters cannot be null");
		this.strDestination = strDestination;
		observersList = new Vector<Observer>();
		connection = (HttpURLConnection)(new URL(strURL)).openConnection();
		downloadedBytes = 0; //start with nothing downloaded
		downloadSize = connection.getContentLength();
		if (downloadSize < 0)
			throw new Error("Content Length is not known");
	}

	public int getSize() {
		return downloadSize;
	}

	public void prepareDownload() throws FileNotFoundException, IOException {
		System.out.println("Trying to connect to " + connection.getURL().toString() + "...");
		new File(strDestination).getParentFile().mkdirs();
		in = connection.getInputStream();
		out = new FileOutputStream(strDestination);
	}

	public byte[] createNewBuffer() {
		return new byte[65536];
	}

	public int getNextPart(byte[] buffer) throws IOException {
		return in.read(buffer);
	}

	public void writePart(byte[] buffer, int count) throws IOException {
		out.write(buffer, 0, count);
		downloadedBytes += count;
		notifyObservers();
	}

	public int getBytesSoFar() {
		return downloadedBytes;
	}

	public void endConnection() throws IOException {
		in.close();
		out.close();
		connection.disconnect();
	}

	//convenience method
	public void startDownloadAndObserve(Observer observer) throws IOException {
		//if cancelling downloads are needed, use threads to track
		boolean useThread = (downloadThread == null ? false : true);
		if (observer != null)
			register(observer);
		Thread thisThread = Thread.currentThread();

		//Prepare the necessary download input and output streams
		prepareDownload();
		byte[] buffer = createNewBuffer();
		int count;

		//Start actual downloading and writing to file
		while ((count = getNextPart(buffer)) >= 0 &&
				(!useThread || (useThread && thisThread == downloadThread))) {
			writePart(buffer, count);
		}
		endConnection(); //end connection once download done
	}

	public void setDownloadThread(Thread downloadThread) {
		this.downloadThread = downloadThread;
	}

	public void notifyObservers() {
		for (Observer observer : observersList) {
			observer.refreshData(this);
		}
	}

	public void register(Observer obs) {
		observersList.addElement(obs);
	}

	public void unRegister(Observer obs) { }

}