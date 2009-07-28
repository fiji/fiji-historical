package fiji.pluginManager.logic;
import ij.IJ;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/*
 * This FileUploader is highly specialized to upload plugins and XML information over to
 * Pacific. There is a series of steps to follow. Any exception means entire upload process
 * is considered invalid.
 * 
 * 1.) Set db.xml.gz to read-only
 * 2.) Verify db.xml.gz has not been modified, if not, upload process cancelled
 * 3.) Upload db.xml.gz.lock (Lock file, prevent others from writing it ATM)
 * 4.) Upload plugin files and current.txt
 * 5.) If all goes well, force rename db.xml.gz.lock to db.xml.gz
 * 
 */
public class FileUploader {
	private final String user = "uploads";
	private final String host = "pacific.mpi-cbg.de";
	private final int hostKeyType = HostKey.SSHRSA;
	private final String hostKey =
		" 00 00 00 07 73 73 68 2d 72 73 61 00 00 00 01 23" +
		" 00 00 01 01 00 c5 8b 21 2f f2 59 8e d1 b9 de 7f" +
		" 57 e7 3c c9 d8 d8 0b d7 d2 f7 0e 67 62 3e f6 95" +
		" 79 09 ec d9 5a 17 3c 9f 31 1c 2a 33 75 d7 a2 1f" +
		" c2 15 74 ba b7 53 ef f4 94 e3 d9 5c 03 d8 7b bf" +
		" 23 43 0f 0e f7 87 14 e4 67 0f 64 04 91 f0 9b 24" +
		" 2a 31 59 7f 86 7f 50 77 6c 35 24 5a 78 9c a9 9a" +
		" fd a6 39 26 6f bf b0 8d 09 f9 0d fa 64 74 ec f5" +
		" dc 29 0d 07 e8 7b c5 ac 41 55 27 1c ba b1 d9 8b" +
		" 2a 56 a6 f7 d8 ad ce 44 7c fd ee d6 91 00 1f 8c" +
		" a3 ea 0c 68 39 1f c5 65 2f 95 b9 40 28 38 cd bf" +
		" 01 bf d1 ad e6 c6 34 d7 95 56 ae 2f f1 17 29 e9" +
		" a5 4e 4c 93 b2 6f e7 7f b2 5d 5c 9b b6 09 27 83" +
		" aa 87 33 aa 2b de 2a a0 c2 7a 9d 96 6c 0e 32 b3" +
		" 15 12 f2 8f 3f 9c 03 6f 9a 3b f5 8d 57 c0 9a 17" +
		" 0a 46 44 72 c4 83 5d 4d 23 1b d9 92 7b 02 98 e4" +
		" 9a 55 db 33 82 a0 c7 96 86 78 bf 31 fd b4 6c 62" +
		" bf 42 3a 05 63";
	private final String password = "fiji";

	private final String uploadDir = "/incoming/"; //TODO: Change to the LIVE version
	private Session session;
	private Channel channel;
	private List<UploadListener> listeners;
	private SourceFile currentUpload;
	private long uploadedBytes;
	private long uploadSize;
	private OutputStream out;
	private InputStream in;

	public FileUploader() throws JSchException, IOException {
		listeners = new ArrayList<UploadListener>();

		JSch jsch = new JSch();
		HostKey hostKeyObject = new HostKey(host, hostKeyType,
			getHostKeyBytes());
		jsch.getHostKeyRepository().add(hostKeyObject, null);

		session = jsch.getSession(user, host, 22);
		session.setPassword(password);
		session.connect();
	}

	//Steps to accomplish entire upload task
	//Note: For information list, index 0 is XML lock file, 1 is text file
	public synchronized void beganUpload(long xmlLastModified, List<SourceFile> information,
			List<SourceFile> sources) throws Exception {
		//Set db.xml.gz to read-only
		setCommand("chmod u-w " + uploadDir + PluginManager.XML_COMPRESSED);
		System.out.println("db.xml.gz set to read-only mode");

		//Prepare for uploading of files
		String uploadFilesCommand = "scp -p -t -r " + uploadDir;
		setCommand(uploadFilesCommand);
		if(checkAck(in) != 0) { //Error check
			throw new Exception("Failed to set command " + uploadFilesCommand);
		}
		System.out.println("Acknowledgement done, prepared to upload file(s)");

		//Verify that XML file did not change since last downloaded
		System.out.println("Checking if XML file has been modified since last download...");
		if (!verifyXMLFileDidNotChange(xmlLastModified)) {
			throw new Exception("Conflict: XML file has been modified since it was last downloaded.");
		}
		System.out.println("XML file was not modified since last download, clear!");

		//Start actual upload
		uploadSingleFile(information.get(0)); //XML lock file
		uploadFiles(sources);
		uploadSingleFile(information.get(1)); //current.txt file

		//Unlock process
		String cmd1 = "chmod u+w " + uploadDir + PluginManager.XML_COMPRESSED;
		String cmd2 = "mv " + uploadDir + PluginManager.XML_LOCK + " " +
		uploadDir + PluginManager.XML_COMPRESSED;
		String cmd3 = "rm -f " + uploadDir + PluginManager.XML_LOCK;

		setCommand(cmd1);
		System.out.println("Command ran: " + cmd1);

		setCommand(cmd2);
		System.out.println("Command ran: " + cmd2);

		setCommand(cmd3);
		System.out.println("Command ran: " + cmd3);

		//setCommand("sh -c \"" + cmd1 + " && " + cmd2 + " && " + cmd3 + "\"");
		//System.out.println("Command ran: sh -c " + cmd1 + "," + cmd2 + "," + cmd3);

		//Force rename db.xml.gz.lock to db.xml.gz
		/*setCommand("sh -c \"trap 'rm -f " + uploadDir + PluginManager.XML_COMPRESSED_LOCK + "' EXIT && " +
				"scp -p -t " + uploadDir + " && " +
				"chmod u+w " + uploadDir + PluginManager.XML_COMPRESSED_FILENAME + " && " +
				"mv " + uploadDir + PluginManager.XML_COMPRESSED_LOCK + " " +
				uploadDir + PluginManager.XML_COMPRESSED_FILENAME + "\"");
		System.out.println("db.xml.gz.lock renamed back to db.xml.gz");*/

		//No exceptions occurred, thus inform listener of upload completion
		out.close();
		channel.disconnect();
		notifyListenersCompletionAll();
	}

	private void setCommand(String command) throws Exception {
		if (out != null) {
			out.close();
			channel.disconnect();
		}
		channel = session.openChannel("exec");
		((ChannelExec)channel).setCommand(command);

		// get I/O streams for remote scp
		out = channel.getOutputStream();
		in = channel.getInputStream();
		channel.connect();
	}

	private boolean verifyXMLFileDidNotChange(long xmlModifiedSince) throws MalformedURLException, IOException {
		//Use isModifiedSince header field to identify
		URL xmlURL = new URL(PluginManager.MAIN_URL + PluginManager.XML_COMPRESSED);
		HttpURLConnection uc = (HttpURLConnection)xmlURL.openConnection();
		/*uc.setIfModifiedSince(xmlModifiedSince);
		System.out.println("xmlModifiedSince... " + xmlModifiedSince);
		return (uc.getInputStream().read() == -1);*/
		if (xmlModifiedSince != uc.getLastModified()) return false;
		else return true;
	}

	//Upload and tracks the status of this single file
	private void uploadSingleFile(SourceFile source) throws Exception {
		List<SourceFile> singleSource = new ArrayList<SourceFile>();
		singleSource.add(source);
		uploadFiles(singleSource);
	}

	private void uploadFiles(List<SourceFile> sources) throws Exception {
		uploadSize = 0;
		uploadedBytes = 0;

		//compile filesize total
		for (SourceFile source : sources)
			uploadSize += source.getFilesize();

		//Write files to server
		Map<String, List<SourceFile>> mapDirToSources = compileMapDirToFiles(sources);
		Iterator<String> directories = mapDirToSources.keySet().iterator();
		while (directories.hasNext()) {
			String directory = directories.next();
			writeFilesInsideDirectory(mapDirToSources.get(directory), directory);
		}
	}

	//creates a TreeMap of directory locations that map to corresponding files
	private Map<String, List<SourceFile>> compileMapDirToFiles(List<SourceFile> sources) {
		Map<String, List<SourceFile>> mapDirToSources = new TreeMap<String, List<SourceFile>>();
		for (SourceFile source : sources) {
			String directory = source.getDirectory();

			//Add the location and its file
			if (!mapDirToSources.containsKey(directory)) {
				//if no mapping to files for this directory yet
				List<SourceFile> dirSources = new ArrayList<SourceFile>();
				dirSources.add(source);
				mapDirToSources.put(directory, dirSources);
			} else {
				List<SourceFile> dirSources = mapDirToSources.get(directory);
				dirSources.add(source);
			}
		}
		return mapDirToSources;
	}

	private void writeFilesInsideDirectory(List<SourceFile> sources,
			String directory) throws Exception {
		String[] directoryList = null;
		if (!directory.equals("")) {
			directoryList = directory.split("/");

			//Go into the directory where the files should lie
			for (String name : directoryList) {
				String command = "D0755 0 " + name + "\n";
				out.write(command.getBytes());
				out.flush();
				if (checkAck(in) != 0) {
					throw new Exception("Cannot enter directory " + name);
				}
			}
		}

		//Write the file, one by one
		for (SourceFile source : sources) {
			currentUpload = source;
			notifyListenersUpdate();

			// notification that file is about to be written
			String command = source.getPermissions() + " " + source.getFilesize() + " " +
				source.getFilenameToWrite() + "\n";
			out.write(command.getBytes());
			out.flush();
			checkAckUploadError();

			// send contents of file
			FileInputStream input = new FileInputStream(source.getAbsolutePath());
			byte[] buf = new byte[16384];
			for (;;) {
				int len = input.read(buf, 0, buf.length);
				if (len <= 0)
					break;
				out.write(buf, 0, len);
				uploadedBytes += len;
				notifyListenersUpdate(); //update listeners every data upload
			}
			input.close();

			// send '\0'
			buf[0] = 0;
			out.write(buf, 0, 1);
			out.flush();
			checkAckUploadError();
			notifyListenersFileComplete();
		}

		//Exiting the directories (Go back to home) after writing the files
		if (directoryList != null) {
			for (int i = 0; i < directoryList.length; i++) {
				out.write("E\n".getBytes());
				out.flush();
				checkAckUploadError();
			}
		}
	}

	private void checkAckUploadError() throws Exception {
		if (checkAck(in) != 0)
			throw new Exception("checkAck failed during uploading " +
				currentUpload.getDirectory() + "/" + currentUpload.getFilenameToWrite());
	}

	public void disconnectSession() throws IOException {
		out.close();
		channel.disconnect();
		session.disconnect();
	}

	private int checkAck(InputStream in) throws IOException {
		int b = in.read();
		// b may be 0 for success,
		//          1 for error,
		//          2 for fatal error,
		//          -1
		if (b == 0)
			return b;
		if (b == -1)
			return b;

		if (b == 1 || b == 2) {
			StringBuffer sb = new StringBuffer();
			int c;
			do {
				c = in.read();
				sb.append((char)c);
			} while (c != '\n');
			IJ.error(sb.toString());
		}
		return b;
	}

	private byte[] getHostKeyBytes() {
		byte[] result = new byte[hostKey.length() / 3];
		for (int i = 0; i < result.length; i++)
			result[i] = (byte)(fromHex(hostKey.charAt(i * 3 + 2))
				| (fromHex(hostKey.charAt(i * 3 + 1)) << 4));
		return result;
	}

	private int fromHex(char c) {
		if (c >= '0' && c <= '9')
			return c - '0';
		if (c >= 'a' && c <= 'f')
			return c - 'a' + 10;
		if (c >= 'A' && c <= 'F')
			return c - 'A' + 10;
		throw new RuntimeException("Illegal hex character: " + c);
	}

	private void notifyListenersUpdate() {
		for (UploadListener listener : listeners) {
			listener.update(currentUpload, uploadedBytes, uploadSize);
		}
	}

	private void notifyListenersCompletionAll() {
		for (UploadListener listener : listeners) {
			listener.uploadProcessComplete();
		}
	}

	private void notifyListenersFileComplete() {
		for (UploadListener listener : listeners) {
			listener.uploadFileComplete(currentUpload);
		}
	}

	public synchronized void addListener(UploadListener listener) {
		listeners.add(listener);
	}

	public interface UploadListener {
		public void update(SourceFile source, long bytesSoFar, long bytesTotal);
		public void uploadFileComplete(SourceFile source);
		public void uploadProcessComplete();
	}

	public interface SourceFile {
		public String getAbsolutePath();
		public String getDirectory();
		public String getFilenameToWrite();
		public String getPermissions();
		public long getFilesize();
	}
}
