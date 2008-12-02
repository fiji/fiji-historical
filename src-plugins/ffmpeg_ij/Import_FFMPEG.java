package ffmpeg_ij;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import ij.ImagePlus;
import ij.ImageStack;

import ij.plugin.PlugIn;

import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

import net.sf.ffmpeg_java.AVCodecLibrary;
import net.sf.ffmpeg_java.AVFormatLibrary;
import net.sf.ffmpeg_java.AVUtilLibrary;
import net.sf.ffmpeg_java.AVCodecLibrary.AVCodec;
import net.sf.ffmpeg_java.AVCodecLibrary.AVCodecContext;
import net.sf.ffmpeg_java.AVCodecLibrary.AVFrame;
import net.sf.ffmpeg_java.AVFormatLibrary.AVFormatContext;
import net.sf.ffmpeg_java.AVFormatLibrary.AVPacket;
import net.sf.ffmpeg_java.AVFormatLibrary.AVStream;

/**
 * Based on the AVCodecSample example from ffmpeg-java by Ken Larson.
 */

public class FFMPEG_Import extends ImagePlus implements PlugIn {

	/** Takes path as argument, or asks for it and then open the image.*/
	public void run(final String arg) {
		File file = null;
		if (arg != null && arg.length() > 0)
			file = new File(arg);
		else {
			OpenDialog od =
				new OpenDialog("Choose .ico file", null);
			String directory = od.getDirectory();
			if (null == directory)
				return;
			file = new File(directory + "/" + od.getFileName());
		}

		final AVFormatLibrary AVFORMAT = AVFormatLibrary.INSTANCE;
		final AVCodecLibrary AVCODEC = AVCodecLibrary.INSTANCE;
		final AVUtilLibrary AVUTIL = AVUtilLibrary.INSTANCE;

		// not sure what the consequences of such a mismatch are,
		// but it is worth logging a warning:
		if (AVCODEC.avcodec_version() !=
				AVCodecLibrary.LIBAVCODEC_VERSION_INT)
			IJ.write("ffmpeg-java and ffmpeg versions do not match:"
					+ " avcodec_version="
					+ AVCODEC.avcodec_version()
					+ " LIBAVCODEC_VERSION_INT="
					+ AVCodecLibrary.LIBAVCODEC_VERSION_INT);

		AVFORMAT.av_register_all();


		final PointerByReference ppFormatCtx = new PointerByReference();

		// Open video file
		if (AVFORMAT.av_open_input_file(ppFormatCtx,
				file.getAbsolutePath(), null, 0, null) != 0) {
			IJ.error("Could not open " + file);
			return;
		}

		final AVFormatContext formatCtx =
			new AVFormatContext(ppFormatCtx.getValue());
//System.out.println(new String(formatCtx.filename));

		// Retrieve stream information
		if (AVFORMAT.av_find_stream_info(formatCtx) < 0) {
			IJ.error("No stream in " + file);
			return;
		}

//AVFORMAT.dump_format(formatCtx, 0, filename, 0);
		// Find the first video stream
		int videoStream = -1;
		for (int i = 0; i < formatCtx.nb_streams; i++) {
			final AVStream stream =
				new AVStream(formatCtx.getStreams()[i]);
			final AVCodecContext codecCtx =
				new AVCodecContext(stream.codec);
//System.out.println("codecCtx " + i + ": " + codecCtx);
			if (codecCtx.codec_type ==
					AVCodecLibrary.CODEC_TYPE_VIDEO) {
				videoStream=i;
				break;
			}
		}
		if (videoStream == -1) {
			IJ.error("No video stream in " + file);
			return;
		}

//System.out.println("Video stream index: " + videoStream);
		// Get a pointer to the codec context for the video stream
		final Pointer pCodecCtx =
			new AVStream(formatCtx.getStreams()[videoStream]).codec;
		final AVCodecContext codecCtx = new AVCodecContext(pCodecCtx);

//System.out.println("Codec id: " + codecCtx.codec_id);
		if (codecCtx.codec_id == 0) {
			IJ.error("Codec not available");
			return;
		}

		// Find the decoder for the video stream
		final AVCodec codec =
			AVCODEC.avcodec_find_decoder(codecCtx.codec_id);
		if (codec == null) {
			IJ.error("Codec not available");
			return;
		}

		// Open codec
		if (AVCODEC.avcodec_open(codecCtx, codec) < 0) {
			IJ.error("Codec not available");
			return;
		}

		// Allocate video frame
		final AVFrame frame = AVCODEC.avcodec_alloc_frame();
		if (frame == null) {
			IJ.error("Could not allocate frame");
			return;
		}

		// Allocate an AVFrame structure
		final AVFrame frameRGB = AVCODEC.avcodec_alloc_frame();
		if (frameRGB == null)
			throw new RuntimeException("Could not allocate frame");

		// Determine required buffer size and allocate buffer
		final int numBytes =
			AVCODEC.avpicture_get_size(AVCodecLibrary.PIX_FMT_RGB24,
					codecCtx.width, codecCtx.height);
		final Pointer buffer = AVUTIL.av_malloc(numBytes);

		// Assign appropriate parts of buffer to image planes
		// in pFrameRGB
		AVCODEC.avpicture_fill(frameRGB, buffer,
				AVCodecLibrary.PIX_FMT_RGB24,
				codecCtx.width, codecCtx.height);

		ImageStack stack = null;

		// Read frames and save first five frames to disk
		int i = 0;
		final AVPacket packet = new AVPacket();
		while (AVFORMAT.av_read_frame(formatCtx, packet) >= 0) {
			// Is this a packet from the video stream?
			if (packet.stream_index != videoStream)
				continue;

			final IntByReference frameFinished =
					new IntByReference();
			// Decode video frame
			AVCODEC.avcodec_decode_video(codecCtx, frame,
					frameFinished,
					packet.data, packet.size);

			// Did we get a video frame?
			if (frameFinished.getValue() == 0)
				break;

			// Convert the image from its native format to RGB
			AVCODEC.img_convert(frameRGB,
					AVCodecLibrary.PIX_FMT_RGB24, 
					frame, codecCtx.pix_fmt,
					codecCtx.width, 
					codecCtx.height);

			ImageProcessor ip = toSlice(frameRGB,
					codecCtx.width, codecCtx.height);
			if (stack == null) {
				setProcessor(ip);
				stack = getStack();
			} else
				stack.addSlice("", ip.getPixels());

			// Free the packet that was allocated by av_read_frame
			// AVFORMAT.av_free_packet(packet.getPointer())
			// - cannot be called because it is an inlined function.
			// so we'll just do the JNA equivalent of the inline:
			if (packet.destruct != null)
				packet.destruct.callback(packet);

		}

		// Free the RGB image
		AVUTIL.av_free(frameRGB.getPointer());

		// Free the YUV frame
		AVUTIL.av_free(frame.getPointer());

		// Close the codec
		AVCODEC.avcodec_close(codecCtx);

		// Close the video file
		AVFORMAT.av_close_input_file(formatCtx);

		System.out.println("Done");

	}

	static ColorProcessor getSlice(AVFrame frame, int width, int height) {
		int[] pixels = new int[width * height];
		final byte[] data = frame.data0.getByteArray(0, len);
		for (int j = 0; j < height; j++) {
			final int off = j * frame.linesize[0];
			for (int i = 0; i < width; i++)
				pixels[i + j * width] =
					((data[off + 3 * i]) & 0xff) << 16 |
					((data[off + 3 * i + 1]) & 0xff) << 8 |
					((data[off + 3 * i + 2]) & 0xff);
		return new ColorProcessor(width, height, pixels);
	}
}
