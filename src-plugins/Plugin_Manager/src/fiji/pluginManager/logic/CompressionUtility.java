package fiji.pluginManager.logic;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public class CompressionUtility {
	//Decompress a file
	public byte[] getDecompressedData(InputStream in) throws IOException {
		InflaterInputStream inflaterInputStream = new InflaterInputStream(in);
		ByteArrayOutputStream bout = new ByteArrayOutputStream(65536);
		int data;
		while ((data = inflaterInputStream.read()) != -1) {
			bout.write(data);
		}
		inflaterInputStream.close();
		bout.close();
		return bout.toByteArray();
	}

	//Takes in file's data, compress, and save to destination
	public void compressAndSave(byte[] data, OutputStream out) throws IOException {
		Deflater deflater = new Deflater();
		DeflaterOutputStream dout = new DeflaterOutputStream(out, deflater);
		dout.write(data);
		dout.close();
	}

	//Gets the bytes data of a file
	public byte[] readStream(InputStream input) throws IOException {
		byte[] buffer = new byte[1024];
		int offset = 0, len = 0;
		for (;;) {
			if (offset == buffer.length)
				buffer = realloc(buffer,
						2 * buffer.length);
			len = input.read(buffer, offset,
					buffer.length - offset);
			if (len < 0)
				return realloc(buffer, offset);
			offset += len;
		}
	}

	private byte[] realloc(byte[] buffer, int newLength) {
		if (newLength == buffer.length)
			return buffer;
		byte[] newBuffer = new byte[newLength];
		System.arraycopy(buffer, 0, newBuffer, 0,
				Math.min(newLength, buffer.length));
		return newBuffer;
	}
}