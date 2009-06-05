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

	public void startDownload() throws FileNotFoundException, IOException {
		System.out.println("Trying to connect to " + myConnection.getURL().toString() + "...");
		new File(strDestination).getParentFile().mkdirs();
		copyFile(myConnection.getInputStream(),
				new FileOutputStream(strDestination));
		myConnection.disconnect();
	}

	private void copyFile(InputStream in, OutputStream out)
	throws IOException {
		byte[] buffer = new byte[65536];
		int count;
		while ((count = in.read(buffer)) >= 0) {
			out.write(buffer, 0, count);
			downloadedBytes = count;
			notifyObservers();
		}
		in.close();
		out.close();
	}

	public int getNumOfBytes() {
		return downloadedBytes;
	}

	public void notifyObservers() {
		// Send notify to all Observers
		for (int i = 0; i < observersList.size(); i++) {
			Observer observer = (Observer) observersList.elementAt(i);
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
