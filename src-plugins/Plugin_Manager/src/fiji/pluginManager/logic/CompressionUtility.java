package fiji.pluginManager.logic;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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

	//Testing
	public static void main(String args[]) throws IOException {
		CompressionUtility utility = new CompressionUtility();
		String compressedFileLocation = PluginManager.READ_DIRECTORY + "/" +
				PluginManager.XML_COMPRESSED_FILENAME;
		String xmlFileLocation = PluginManager.READ_DIRECTORY + "/" +
				PluginManager.XML_FILENAME;
		byte[] data = utility.getDecompressedData(
				new FileInputStream(compressedFileLocation));
		FileOutputStream saveFile = new FileOutputStream(xmlFileLocation); //if needed...
		saveFile.write(data);
		saveFile.close();
	}
}