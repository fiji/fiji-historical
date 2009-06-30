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
 * Directly responsibility: Download a file given its URL to a given destination.
 * Updates its download status to its Observer as well.
 */
public class Downloader implements Observable {
	private String strDestination;
	private int downloadedBytes;
	private HttpURLConnection myConnection;
	private Vector<Observer> observersList;
	private InputStream in;
	private OutputStream out;

	public Downloader(String strURL, String strDestination) throws IOException {
		if (strURL == null || strDestination == null)
			throw new Error("Downloader constructor parameters cannot be null");
		this.strDestination = strDestination;
		downloadedBytes = 0; //start with nothing downloaded
		observersList = new Vector<Observer>();
		myConnection = (HttpURLConnection)(new URL(strURL)).openConnection();
	}

	public int getSize() {
		return myConnection.getContentLength();
	}

	public void prepareDownload() throws FileNotFoundException, IOException {
		System.out.println("Trying to connect to " + myConnection.getURL().toString() + "...");
		new File(strDestination).getParentFile().mkdirs();
		in = myConnection.getInputStream();
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
		downloadedBytes = count;
		notifyObservers();
	}

	public int getNumOfBytes() {
		//number of bytes retrieved at THIS interval, NOT "bytes downloaded so far"
		return downloadedBytes;
	}

	public void endConnection() throws IOException {
		in.close();
		out.close();
		myConnection.disconnect();
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