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
 * Directly responsibility: Download a file given its URL to a given destination
 */
public class Downloader implements Observable {
	private String strDestination;
	private int downloadedBytes;
	private HttpURLConnection myConnection;
	private Vector<Observer> observersList;
	private InputStream in;
	private OutputStream out;

	public Downloader(String strURL, String strDestination) throws Exception {
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

	//Todo: Removal of this method anticipated
	public void startDownload() throws FileNotFoundException, IOException {
		prepareDownload();
		copyFile();
	}

	//Todo: Removal of this method anticipated
	private void copyFile() throws IOException {
		byte[] buffer = createNewBuffer();
		int count;
		while ((count = getNextPart(buffer)) >= 0) {
			writePart(buffer, count);
		}
		endConnection();
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
		return downloadedBytes;
	}

	public void endConnection() throws IOException {
		in.close();
		out.close();
		myConnection.disconnect();
	}

	public void notifyObservers() {
		// Send notify to all Observers
		for (Observer observer : observersList) {
			observer.refreshData(this);
		}
	}

	public void register(Observer obs) {
		observersList.addElement(obs);
	}

	public void unRegister(Observer obs) {
	}

}

interface Observer {
	public void refreshData(Observable subject);
}

interface Observable {
	public void notifyObservers();

	public void register(Observer obs);

	public void unRegister(Observer obs);
}
