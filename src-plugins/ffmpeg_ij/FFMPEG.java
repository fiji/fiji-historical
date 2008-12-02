package ffmpeg_ij;

/*
 * Base class to handle loading the FFMPEG libraries.
 */

import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;

import ij.IJ;

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

	public boolean loadFFMPEG(boolean addSearchPaths) {

		if (AVFORMAT != null)
			return true;

		if (addSearchPaths && !addSearchPaths())
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

	private boolean addSearchPaths() {
		String dir = IJ.getDirectory("imagej");
		if (dir == null)
			return false;
		String[] libs = { "avformat", "avcodec", "avutil" };
		for (int i = 0; i < libs.length; i++)
			NativeLibrary.addSearchPath(libs[i], dir);
		return true;
	}
}
