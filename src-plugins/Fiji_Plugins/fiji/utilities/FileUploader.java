package fiji.utilities;

import ij.IJ;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

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
	private File currentUpload;
	private int uploadedBytes;
	private int uploadSize;
	private OutputStream out;
	private InputStream in;

	public FileUploader(String directory) throws JSchException, IOException {
		if (directory == null)
			directory = "";
		listeners = new ArrayList<UploadListener>();

		JSch jsch = new JSch();
		HostKey hostKeyObject = new HostKey(host, hostKeyType,
			getHostKeyBytes());
		jsch.getHostKeyRepository().add(hostKeyObject, null);

		session = jsch.getSession(user, host, 22);
		session.setPassword(password);
		session.connect();

		//Open up a channel to upload files (With possibility of multiple files)
		channel = session.openChannel("exec");
		String command = "scp -p -t -r " + directory;
		((ChannelExec)channel).setCommand(command);

		// get I/O streams for remote scp
		out = channel.getOutputStream();
		in = channel.getInputStream();
		channel.connect();

		//Acknowledgement, going to upload file(s)
		if(checkAck(in) != 0) {
			return;
		}
		System.out.println("Acknowledgement done, prepared to upload file(s)");
	}

	public synchronized void uploadMultipleFiles(Iterator<File> files) {
		uploadSize = 0;
		uploadedBytes = 0;
		while (files.hasNext()) {
			File file = files.next();
			uploadSize += new Long(file.length()).intValue();
			actualUpload(file);
		}
		notifyListenersCompletion();
	}

	public synchronized void uploadFile(File file) {
		List<File> singleFileList = new ArrayList<File>();
		singleFileList.add(file);
		uploadMultipleFiles(singleFileList.iterator());
	}

	private synchronized void actualUpload(File file) {
		currentUpload = file;
		try {
			System.out.println("Going to upload " + file.getName());
			String path = file.getName().replace(' ', '_');

			// send "C0644 filesize filename"
			String command = "C0444 " + new Long(file.length()).intValue() + " " + path + "\n";
			out.write(command.getBytes());
			out.flush();

			//Acknowledgement, going to upload this particular file
			if (checkAck(in) != 0) {
				return;
			}
			System.out.println("Acknowledged. Upload of " + file.getName() + " will proceed.");

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
			if (checkAck(in) != 0)
				return;
			System.out.println("Acknowledged that file uploaded.");

		} catch (IOException e2) {
			notifyListenersError(e2);
		} catch (Exception e3) {
			notifyListenersError(new Exception("Unidentified Exception when uploading " +
					currentUpload.getPath()));
		}
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
			}
			while (c != '\n');
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

	public void notifyListenersUpdate() {
		for (UploadListener listener : listeners) {
			listener.update(currentUpload.getName(), uploadedBytes, uploadSize);
		}
	}

	public void notifyListenersCompletion() {
		for (UploadListener listener : listeners) {
			listener.uploadComplete(currentUpload.getName());
		}
	}

	public void notifyListenersError(Exception e) {
		for (UploadListener listener : listeners) {
			listener.uploadFailed(currentUpload.getName(), e);
		}
	}

	public void addListener(UploadListener listener) {
		listeners.add(listener);
	}

	public interface UploadListener {
		public void update(String filename, int bytesSoFar, int bytesTotal);
		public void uploadComplete(String filename);
		public void uploadFailed(String filename, Exception e);
	}
}
