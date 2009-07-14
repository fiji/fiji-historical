package fiji.utilities;

import ij.IJ;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class FileUploader {
	private String host;
	private int hostKeyType;
	private String hostKey;
	private JSch jsch;
	private Session session;
	private Channel channel;
	private List<UploadListener> listeners;
	private File currentUpload;
	private int uploadedBytes;
	private int uploadSize;

	public FileUploader(String host, String hostKey, int hostKeyType) {
		this.host = host;
		this.hostKey = hostKey;
		this.hostKeyType = hostKeyType;
		listeners = new ArrayList<UploadListener>();
	}

	public void connectSession(String user, String password) throws JSchException {
		jsch = new JSch();
		HostKey hostKey = new HostKey(host, hostKeyType,
			getHostKeyBytes());
		jsch.getHostKeyRepository().add(hostKey, null);

		session = jsch.getSession(user, host, 22);
		session.setPassword(password);
		session.connect();
	}

	//Standard to upload a file
	public void uploadFile(File file) {
		currentUpload = file;
		try {
			// exec 'scp -t file' remotely
			String path = file.getName().replace(' ', '_');
			String command = "scp -p -t incoming/" + path;
			Channel channel = session.openChannel("exec");
			((ChannelExec)channel).setCommand(command);

			// get I/O streams for remote scp
			OutputStream out = channel.getOutputStream();
			InputStream in = channel.getInputStream();

			channel.connect();

			if(checkAck(in) != 0)
				return;

			// send "C0644 filesize filename"
			uploadedBytes = 0;
			uploadSize = new Long(file.length()).intValue();
			command = "C0444 " + uploadSize + " " + path + "\n";
			out.write(command.getBytes());
			out.flush();
			if(checkAck(in) != 0)
				return;

			// send a content of file
			IJ.showStatus("Uploading " + file.getName());
			FileInputStream input = new FileInputStream(file);
			byte[] buf = new byte[16384];
			//long total = 0;
			for (;;) {
				int len = input.read(buf, 0, buf.length);
				if (len <= 0)
					break;
				out.write(buf, 0, len);
				uploadedBytes += len;
				notifyListenersUpdate();
			}
			notifyListenersCompletion();
			input.close();

			// send '\0'
			buf[0] = 0;
			out.write(buf, 0, 1);
			out.flush();
			if (checkAck(in) != 0)
				return;
			out.close();

			channel.disconnect();

		} catch (JSchException e1) {
			notifyListenersError(e1);
		} catch (IOException e2) {
			notifyListenersError(e2);
		} catch (Exception e3) {
			notifyListenersError(new Exception("Unidentified Exception when uploading " +
					currentUpload.getPath()));
		}
	}

	public void disconnectSession() {
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
			listener.update(currentUpload, uploadedBytes, uploadSize);
		}
	}

	public void notifyListenersCompletion() {
		for (UploadListener listener : listeners) {
			listener.uploadComplete(currentUpload);
		}
	}

	public void notifyListenersError(Exception e) {
		for (UploadListener listener : listeners) {
			listener.uploadFailed(currentUpload, e);
		}
	}

	public void addListener(UploadListener listener) {
		listeners.add(listener);
	}

	public interface UploadListener {
		public void update(File source, int bytesSoFar, int bytesTotal);
		public void uploadComplete(File source);
		public void uploadFailed(File source, Exception e);
	}
}
