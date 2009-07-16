package fiji.pluginManager.logic;

import ij.IJ;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
 */
public class FileUploader {
	final String user = "uploads";
	final String host = "pacific.mpi-cbg.de";
	final int hostKeyType = HostKey.SSHRSA;
	final String hostKey =
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
	final String password = "fiji";

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

	private synchronized void setCommand(String command) throws Exception {
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

	//Steps to accomplish entire upload task
	public void beganUpload(SourceFile xmlSource, List<SourceFile> sources,
			SourceFile textSource) throws Exception {
		//Set db.xml.gz to read-only
		setCommand("chmod u-w /incoming/db.xml.gz");
		System.out.println("db.xml.gz set to read-only mode");

		//setCommand("chmod u+w /incoming/db.xml.gz.lock");

		//Prepare for uploading of files
		String uploadFilesCommand = "scp -p -t -r /incoming";
		setCommand(uploadFilesCommand);
		//Error check
		if(checkAck(in) != 0) {
			throw new Exception("Failed to set command " + uploadFilesCommand);
		}
		System.out.println("Acknowledgement done, prepared to upload file(s)");
		uploadXMLFileLock(xmlSource); //lock indicator, others cannot overwrite
		uploadFiles(sources);
		uploadTextFile(textSource);

		//force rename db.xml.gz.lock to db.xml.gz
		setCommand("trap - { rm /incoming/db.xml.gz.lock } && " +
				"scp -p -t /incoming/ && chmod u+w /incoming/db.xml.gz && " +
				"mv /incoming/db.xml.gz.lock /incoming/db.xml.gz");
		//setCommand("trap - { rm /var/www/update/db.xml.gz.lock } && " +
		//		"scp -p -t /var/www/update/ && chmod u+w /var/www/update/db.xml.gz && " +
		//		"mv /var/www/update/db.xml.gz.lock /var/www/update/db.xml.gz");

		//No exceptions occurred, thus inform listener of upload completion
		notifyListenersCompletionAll();
	}

	//Writes the XML file ==> Note that it is a lock version
	private synchronized void uploadXMLFileLock(SourceFile xmlSource) throws Exception {
		uploadSize = xmlSource.getFilesize();
		uploadedBytes = 0;
		//Write XML file as read-only
		uploadSingleFile(xmlSource, "C0444");
	}

	private synchronized void uploadFiles(List<SourceFile> sources) throws Exception {
		uploadSize = 0;
		uploadedBytes = 0;

		//compile filesize total
		for (SourceFile source : sources)
			uploadSize += source.getFilesize();

		//Write plugin files to server
		Map<String, List<SourceFile>> mapDirToSources = compileMapDirToFiles(sources);
		Iterator<String> directories = mapDirToSources.keySet().iterator();
		while (directories.hasNext()) {
			String directory = directories.next();
			writeFilesInsideDirectory(mapDirToSources.get(directory), directory);
		}
	}

	//Writes the text file
	private synchronized void uploadTextFile(SourceFile textSource) throws Exception {
		uploadSize = textSource.getFilesize();
		uploadedBytes = 0;
		
		//note: do not write as read-only
		uploadSingleFile(textSource, "C0664");
	}

	//creates a TreeMap of directory locations that map to corresponding files
	private synchronized Map<String, List<SourceFile>> compileMapDirToFiles(List<SourceFile> sources) {
		Map<String, List<SourceFile>> mapDirToSources = new TreeMap<String, List<SourceFile>>();
		for (SourceFile source : sources) {
			//Format the relative path to get relative directories
			String formattedPath = source.getRelativePath().replace(File.separator, "/");
			if (formattedPath.startsWith("/"))
				formattedPath = formattedPath.substring(1);
			if (formattedPath.endsWith("/"))
				formattedPath = formattedPath.substring(0, formattedPath.length()-1);
			if (formattedPath.indexOf("/") != -1) //remove the file
				formattedPath = formattedPath.substring(0, formattedPath.lastIndexOf("/"));
			else
				formattedPath = "";

			//Add the location and its file
			if (!mapDirToSources.containsKey(formattedPath)) {
				//if no mapping to files for this directory yet
				List<SourceFile> dirSources = new ArrayList<SourceFile>();
				dirSources.add(source);
				mapDirToSources.put(formattedPath, dirSources);
			} else {
				List<SourceFile> dirSources = mapDirToSources.get(formattedPath);
				dirSources.add(source);
			}
		}
		return mapDirToSources;
	}

	private synchronized void writeFilesInsideDirectory(List<SourceFile> sources,
			String directory) throws Exception {
		String[] directoryList = null;
		if (!directory.equals("")) {
			directoryList = directory.split("/");

			//Go into the directory where the files should lie
			for (String name : directoryList) {
				System.out.println("Entering " + name + "...");

				String command = "D0755 0 " + name + "\n";
				out.write(command.getBytes());
				out.flush();
				if (checkAck(in) != 0) {
					throw new Exception("Cannot enter directory.");
				}
			}
			System.out.println("Folder " + directory + " acknowledged. Upload of files will proceed.");
		}

		//Write the file, one by one
		for (SourceFile source : sources) {
			uploadSingleFile(source, "C0444");
		}

		//Exiting the directories (Go back to home) after writing the files
		if (directoryList != null) {
			for (int i = 0; i < directoryList.length; i++) {
				out.write("E\n".getBytes());
				out.flush();
				checkAckUploadError();
			}
			System.out.println("Folder " + directory + " exited.");
		}
	}

	private synchronized void uploadSingleFile(SourceFile source, String permissions)
	throws Exception {
		currentUpload = source;
		notifyListenersUpdate();

		File file = source.getFile();
		// notification that file is about to be written
		String command = permissions + " " + source.getFilesize() + " " + file.getName() + "\n";
		out.write(command.getBytes());
		out.flush();
		checkAckUploadError();

		// send contents of file
		FileInputStream input = new FileInputStream(file);
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

	private synchronized void checkAckUploadError() throws Exception {
		if (checkAck(in) != 0)
			throw new Exception("checkAck failed during uploading " +
				currentUpload.getRelativePath());
	}

	public synchronized void disconnectSession() throws IOException {
		out.close();
		channel.disconnect();
		session.disconnect();
	}

	static int checkAck(InputStream in) throws IOException {
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

	byte[] getHostKeyBytes() {
		byte[] result = new byte[hostKey.length() / 3];
		for (int i = 0; i < result.length; i++)
			result[i] = (byte)(fromHex(hostKey.charAt(i * 3 + 2))
				| (fromHex(hostKey.charAt(i * 3 + 1)) << 4));
		return result;
	}

	int fromHex(char c) {
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

	public void addListener(UploadListener listener) {
		listeners.add(listener);
	}

	public interface UploadListener {
		public void update(SourceFile source, long bytesSoFar, long bytesTotal);
		public void uploadFileComplete(SourceFile source);
		public void uploadProcessComplete();
	}

	public interface SourceFile {
		public String getRelativePath();
		public File getFile();
		public long getFilesize();
	}

	protected static class TestClass implements FileUploader.UploadListener {
		public TestClass() {
			FileUploader fileUploader;
			try {
				fileUploader = new FileUploader();
				fileUploader.addListener(this);
				List<SourceFile> files = new ArrayList<SourceFile>();
				files.add(new TestSource("C:/Users/Yap Chin Kiet/Desktop/TestFolder/SubFolder1/shoes05.jpg", "TestFolder/SubFolder1/shoes05.jpg"));
				files.add(new TestSource("C:/Users/Yap Chin Kiet/Desktop/TestFolder\\SubFolder1\\taxi05.jpg", "/TestFolder/SubFolder1/taxi05.jpg"));
				files.add(new TestSource("C:/Users/Yap Chin Kiet/Desktop/TestFolder/SubFolder2\\shoes05.jpg", "TestFolder\\SubFolder2\\shoes05.jpg"));
				files.add(new TestSource("C:/Users/Yap Chin Kiet/Desktop/TestFolder/SubFolder2/taxi05.jpg", "TestFolder/SubFolder2/taxi05.jpg"));
				files.add(new TestSource("C:/Users/Yap Chin Kiet/Desktop/TestFolder/SubFolder2\\SubsubFolder\\shoes05.jpg", "/TestFolder/SubFolder2/SubsubFolder/shoes05.jpg"));
				files.add(new TestSource("C:/Users/Yap Chin Kiet/Desktop/TestFolder/SubFolder2/SubsubFolder/taxi05.jpg", "TestFolder/SubFolder2/SubsubFolder/taxi05.jpg"));
				fileUploader.beganUpload(
						new TestSource("C:/Users/Yap Chin Kiet/Desktop/db.xml.gz.lock", "db.xml.gz.lock"),
						files,
						new TestSource("C:/Users/Yap Chin Kiet/Desktop/current.txt", "current.txt"));
				fileUploader.disconnectSession();
				System.out.println("Upload tasks complete.");
			} catch(Exception e){
				System.out.println(e.getLocalizedMessage());
			}
		}
		public void update(SourceFile source, long bytesSoFar, long bytesTotal) {
			System.out.println(source.getRelativePath() + ": " + bytesSoFar + "/" + bytesTotal);
		}

		public void uploadProcessComplete() {
			System.out.println("Complete uploads");
		}

		public class TestSource implements FileUploader.SourceFile {
			String absolutePath;
			String relativePath;

			public TestSource(String absolutePath, String relativePath) {
				this.absolutePath = absolutePath;
				this.relativePath = relativePath;
			}

			public File getFile() {
				return new File(absolutePath);
			}

			public String getRelativePath() {
				return relativePath;
			}

			public long getFilesize() {
				return new File(absolutePath).length();
			}
			
		}

		public void uploadFileComplete(SourceFile source) {
			System.out.println("Uploaded " + source.getRelativePath() + " successfully");
		}
	}

	public static void main(String args[]) {
		TestClass testclass = new TestClass(); //let it roll
	}
}
