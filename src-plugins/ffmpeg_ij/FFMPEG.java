package ffmpeg_ij;

/*
 * Base class to handle loading the FFMPEG libraries.
 */

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;

import ij.IJ;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.URL;

import net.sf.ffmpeg_java.AVCodecLibrary;
import net.sf.ffmpeg_java.AVFormatLibrary;
import net.sf.ffmpeg_java.AVUtilLibrary;

public class FFMPEG {

	AVUtilLibrary AVUTIL;
	AVCodecLibrary AVCODEC;
	AVFormatLibrary AVFORMAT;
	//SWScaleLibrary SWSCALE;

	public boolean loadFFMPEG() {
		return loadFFMPEG(true);
	}

	public boolean loadFFMPEG(boolean addSearchPath) {

		if (AVFORMAT != null)
			return true;

		if (addSearchPath && !addSearchPath())
			return false;

		try {
			AVUTIL = AVUtilLibrary.INSTANCE;
			AVCODEC = AVCodecLibrary.INSTANCE;
			AVFORMAT = AVFormatLibrary.INSTANCE;
		} catch (UnsatisfiedLinkError e) {
			return false;
		}
		return true;
	}

	private boolean addSearchPath() {
		String[] libs = { "/macosx/libffmpeg.dylib" };
		if (!IJ.isMacOSX()) {
			String extension = IJ.isWindows() ? "dll" : "so";
			String platform = (IJ.isWindows() ? "win" : "linux")
				+ (IJ.is64Bit() ? "64" : "");
			libs = new String[3];
			libs[0] = "/" + platform + "/libavutil." + extension;
			libs[1] = "/" + platform + "/libavcodec." + extension;
			libs[2] = "/" + platform + "/libavformat." + extension;
		}
		URL location = getClass().getResource(libs[0]);
		if (location == null) {
			String dir = IJ.getDirectory("imagej");
			if (dir == null)
				return false;
			System.setProperty("jna.library.path", dir);
			return true;
		}
		File tmp = getTempDirectory();
		if (tmp == null)
			return false;
		if (!copyTempFile(location, tmp))
			return true;
		for (int i = 1; i < libs.length; i++)
			if (!copyTempFile(getClass().getResource(libs[i]), tmp))
				return true;
		System.setProperty("jna.library.path", tmp.getAbsolutePath());
		if (IJ.isMacOSX()) {
			String[] names = { "util", "codec", "format" };
			for (int i = 0; i < names.length; i++)
				symlink("libffmpeg.dylib", tmp.getAbsolutePath()
						+ "/libav" + names[i]
						+ ".dylib");
		}
		return true;
	}

	protected static File getTempDirectory() {
		try {
			File tmp = File.createTempFile("ffmpeg", "");
			if (!tmp.delete() || !tmp.mkdirs())
				return null;
			tmp.deleteOnExit();
			return tmp;
		} catch (IOException e) {
			return null;
		}
	}

	protected static boolean copyTempFile(URL source, File directory) {
		try {
			InputStream in = source.openStream();
			String baseName = new File(source.getFile()).getName();
			File target = new File(directory, baseName);
			target.deleteOnExit();
			OutputStream out = new FileOutputStream(target);
			byte[] buffer = new byte[1<<16];
			for (;;) {
				int len = in.read(buffer);
				if (len < 0)
					break;
				out.write(buffer, 0, len);
			}
			in.close();
			out.close();
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	interface libc extends Library {
		int symlink(String source, String target);
	}

	protected static libc libc;

	public static int symlink(String source, String target) {
		if (libc == null)
			libc = (libc)Native.loadLibrary("libc", libc.class);
		return libc.symlink(source, target);
	}
}
