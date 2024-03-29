import ij.*;
import ij.plugin.*;
import java.io.*;

// Plugin to handle file types which are not implemented
// directly in ImageJ through io.Opener
// NB: since there is no _ in the name it will not appear in Plugins menu
// -----
// Can be user modified so that your own specialised file types
// can be opened through File ... Open
// OR by drag and drop onto the ImageJ main panel
// OR by double clicking in the MacOS 9/X Finder
// -----
// Go to the point marked MODIFY HERE and modify to
// recognise and load your own file type
// -----
// Gregory Jefferis - 030629
// jefferis@stanford.edu

/**
 * Plugin to handle file types which are not implemented
 * directly in ImageJ through io.Opener.
 */
public class HandleExtraFileTypes extends ImagePlus implements PlugIn {
	static final int IMAGE_OPENED = -1;
	static final int PLUGIN_NOT_FOUND = -2;

	/** Called from io/Opener.java. */
	public void run(String path) {
		if (path.equals("")) return;
		File theFile = new File(path);
		String directory = theFile.getParent().replace('\\', '/');
		String fileName = theFile.getName();
		if (directory == null) directory = "";
		else if (!directory.endsWith("/")) directory += "/";

		// Try and recognise file type and load the file if recognised
		ImagePlus imp = openImage(directory, fileName, path);
		if (imp==null) {
			// failed to load file or plugin has opened and displayed it
			IJ.showStatus("");
			return; // failed to load file or plugin has opened and displayed it
		}
		ImageStack stack = imp.getStack();
		// set the stack of this HandleExtraFileTypes object
		// to that attached to the ImagePlus object returned by openImage()
		setStack(fileName, stack);
		// copy over the calibration info since it doesn't come with the ImageProcessor
		setCalibration(imp.getCalibration());
		// also copy the Show Info field over if it exists
		if (imp.getProperty("Info") != null)
			setProperty("Info", imp.getProperty("Info"));
		// copy over the FileInfo
		setFileInfo(imp.getOriginalFileInfo());
	}
	

	private Object tryOpen(String directory, String name, String path) {

		// set up a stream to read in 132 bytes from the file header
		// These can be checked for "magic" values which are diagnostic
		// of some image types
		InputStream is;
		byte[] buf = new byte[132];
		try {
			if (0 == path.indexOf("http://")) {
				is = new java.net.URL(path).openStream();
			} else {
				is = new FileInputStream(path);
			}
			is.read(buf, 0, 132);
			is.close();
		}
		catch (IOException e) {
			// couldn't open the file for reading
			return null;
		}
		name = name.toLowerCase();
		width = PLUGIN_NOT_FOUND;

		// Temporarily suppress "plugin not found" errors if LOCI Bio-Formats plugin is installed
		if (Menus.getCommands().get("Bio-Formats Importer")!=null && IJ.getVersion().compareTo("1.37u")>=0)
			IJ.suppressPluginNotFoundError();

		// OK now we get to the interesting bit

		// GJ: added Biorad PIC confocal file handler
		// ------------------------------------------
		// These make 12345 if you read them as the right kind of short
		// and should have this value in every Biorad PIC file
		if (buf[54]==57 && buf[55]==48) {
			return tryPlugIn("Biorad_Reader", path);
		}
		// GJ: added Gatan Digital Micrograph DM3 handler
		// ----------------------------------------------
		// check if the file ends in .DM3 or .dm3,
		// and bytes make an int value of 3 which is the DM3 version number
		if (name.endsWith(".dm3") && buf[0]==0 && buf[1]==0 && buf[2]==0 && buf[3]==3) {
			return tryPlugIn("io.DM3_Reader", path);
		}

		// IPLab file handler
		// Little-endian IPLab files start with "iiii" or "mmmm".
		if (name.endsWith(".ipl") ||
			(buf[0]==105 && buf[1]==105 && buf[2]==105 && buf[3]==105) ||
			(buf[0]==109 && buf[1]==109 && buf[2]==109 && buf[3]==109)) {
				return tryPlugIn("io.IPLab_Reader", path);
		}

		// Packard InstantImager format (.img) handler -> check HERE
		// before Analyze check below!
		// Check extension and signature bytes KAJ_
		if (name.endsWith(".img") && buf[0]==75 && buf[1]==65 && buf[2]==74 && buf[3]==0) {
			return tryPlugIn("InstantImager_Reader", path);
		}

		// Analyze format (.img/.hdr) handler
		// Note that the Analyze_Reader plugin opens and displays the
		// image and does not implement the ImagePlus class.
		if (name.endsWith(".img") || name.endsWith(".hdr")) {
			return tryPlugIn("Analyze_Reader", path);
		}

		// Image Cytometry Standard (.ics) handler
		// http://valelab.ucsf.edu/~nico/IJplugins/Ics_Opener.html
		if (name.endsWith(".ics")) {
			return tryPlugIn("Ics_Opener", path);
		}

		// Princeton Instruments SPE image file (.spe) handler
		// http://rsb.info.nih.gov/ij/plugins/spe.html
		if (name.endsWith(".spe")) {
			return tryPlugIn("OpenSPE_", path);
		}

		// Zeiss Confocal LSM 510 image file (.lsm) handler
		// http://rsb.info.nih.gov/ij/plugins/lsm-reader.html
		if (name.endsWith(".lsm")) {
			if (Menus.getCommands().get("Show LSMToolbox")!=null)
				return tryPlugIn("LSM_Toolbox", "file="+path);
			else
				// Use the version in VIB (FIXME: just temporarily, so loading works again)
				return tryPlugIn("zeiss.LSM_Reader", path);
		}

		// BM: added Bruker file handler 29.07.04
		if (name.equals("ser") || name.equals("fid") || name.equals("2rr") ||
		name.equals("2ii") || name.equals("3rrr") || name.equals("3iii") ||
		name.equals("2dseq")) {
			ij.IJ.showStatus("Opening Bruker " + name + " File");
			return tryPlugIn("BrukerOpener", name + "|" + path);
		}

		// AVI: open AVI files using AVI_Reader plugin
		if (name.endsWith(".avi")) {
			return tryPlugIn("AVI_Reader", path);
		}

		// QuickTime: open .mov and .pict files using QT_Movie_Opener plugin
		if (name.endsWith(".mov") || name.endsWith(".pict")) {
			return tryPlugIn("QT_Movie_Opener", path);
		}

		// ZVI file handler
		// Little-endian ZVI and Thumbs.db files start with d0 cf 11 e0
		// so we can only look at the extension.
		if (name.endsWith(".zvi")) {
			return tryPlugIn("ZVI_Reader", path);
		}

		// University of North Carolina (UNC) file format handler
		// 'magic' numbers are (int) offsets to data structures and
		// may change in future releases.
		if (name.endsWith(".unc") || (buf[3]==117 && buf[7]==-127 && buf[11]==36 && buf[14]==32 && buf[15]==-127)) {
			return tryPlugIn("UNC_Reader", path);
		}

		//  Leica SP confocal .lei file handler
		if (name.endsWith(".lei")) {
			int dotIndex = name.lastIndexOf(".");
			if (dotIndex>=0)
				name = name.substring(0, dotIndex);
			path = directory+name+".txt";
			File f = new File(path);
			if (!f.exists()) {
				IJ.error("Cannot find the Leica information file: "+path);
				return null;
			}
			return tryPlugIn("Leica_TIFF_sequence", path);
		} 

		// Amira file handler 
		// http://wbgn013.biozentrum.uni-wuerzburg.de/ImageJ/amira-io.html
		if (buf[0]==0x23 && buf[1]==0x20 && buf[2]==0x41
				&& buf[3]==0x6d && buf[4]==0x69 && buf[5]==0x72
				&& buf[6]==0x61 && buf[7]==0x4d && buf[8]==0x65
				&& buf[9]==0x73&&buf[10]==0x68 && buf[11]==0x20) {
			return tryPlugIn("AmiraMeshReader_", path);
		} 

		// Deltavision file handler
		// Open DV files generated on Applied Precision DeltaVision systems
		if (name.endsWith(".dv") || name.endsWith(".r3d")) {
			return tryPlugIn("Deltavision_Opener", path);
		}

		// Albert Cardona: read .mrc files (little endian). Documentation at: http://ami.scripps.edu/prtl_data/mrc_specification.htm . The parsing of the header is a bare minimum of what could be done.
		if (name.endsWith(".mrc")) {
			return tryPlugIn("io.Open_MRC_Leginon", path);
		}

		// Albert Cardona: read .dat files from the EMMENU software
		if (name.endsWith(".dat") && 1 == buf[1] && 0 == buf[2]) { // 'new format' only
			return tryPlugIn("io.Open_DAT_EMMENU", path);
		}

		// Albert Cardona: read TrakEM2 .xml files
		if (name.endsWith(".xml")) {
			if (-1 != new String(buf).toLowerCase().indexOf("trakem2")) {
				try {
					// portable way, resists absence of TrakEM2_.jar in the classpath
					final Class cla = Class.forName("ini.trakem2.Project");
					if (null != cla) {
						final java.lang.reflect.Method method = cla.getMethod("openFSProject", new Class[]{String.class});
						method.invoke(null, new Object[]{path});
					}
					// assume success in any case
					width = IMAGE_OPENED;
				} catch (Exception e) {
					e.printStackTrace();
				}
				return null;
			}
		}

		// Stephan Saalfeld: read .df3 files. Documentation at: http://www.povray.org/documentation/view/3.6.1/374/
		if (name.endsWith(".df3")) {
			return tryPlugIn("io.Open_DF3", path);
		}

		// Albert Cardona: open one or more pages of a PDF file as images
		if (name.endsWith(".pdf")) {
			return tryPlugIn("io.PDF_Viewer", path);
		}

		// Greg Jefferis: open nrrd images
		// see Nrrd_Reader code or 
		// http://teem.sourceforge.net/nrrd/
		try {
			String nrrdMagic=new String(buf,0,7,"US-ASCII");
			if(nrrdMagic.equals("NRRD000")) {
				// Ok we've identified the file type - now load it
				return tryPlugIn("io.Nrrd_Reader", path);
			}
		} catch (Exception e) {
		}

		// Johannes Schindelin: open one or more images in a .ico file
		if (name.endsWith(".ico"))
			return tryPlugIn("io.ICO_Reader", path);

		// Johannes Schindelin: open one or more images in a .icns file
		if (name.endsWith(".icns"))
			return tryPlugIn("io.Icns_Reader", path);

		// Johannes Schindelin: render an .svg image into an ImagePlus
		if (name.endsWith(".svg"))
			return tryPlugIn("io.SVG_Reader", path);

		// Johannes Schindelin: open a Cellomics DIB
		if (name.endsWith(".C01") || name.endsWith(".c01") ||
				(buf[0] == 0x28 && buf[1] == 0 &&
				 buf[2] == 0 && buf[3] == 0) ||
				(buf[0] == 0 && buf[1] == 0 &&
				 buf[2] == 0 && buf[3] == 0x10 &&
				 buf[4] == 0x78 && buf[5] == 0x01))
			return tryPlugIn("io.Cellomics_DIB_Reader", path);

		// Johannes Schindelin: handle scripts
		if (name.endsWith(".py"))
			return tryPlugIn("Jython.Refresh_Jython_Scripts", path);
		if (name.endsWith(".rb"))
			return tryPlugIn("JRuby.Refresh_JRuby_Scripts", path);
		if (name.endsWith(".js"))
			return tryPlugIn("Javascript.Refresh_Javascript_Scripts", path);
		if (name.endsWith(".clj"))
			return tryPlugIn("Clojure.Refresh_Clojure_Scripts", path);

		// ****************** MODIFY HERE ******************
		// do what ever you have to do to recognise your own file type
		// and then call appropriate plugin using the above as models
		// e.g.:
		
		/*
		// A. Dent: Added XYZ handler
		// ----------------------------------------------
		// check if the file ends in .xyz, and bytes 0 and 1 equal 42
		if (name.endsWith(".xyz") && buf[0]==42 && buf[1]==42) {
		// Ok we've identified the file type - now load it
			return tryPlugIn("XYZ_Reader", path);
		}
		*/

		return null;
	}

	private ImagePlus openImage(String directory, String name, String path) {
		Object o = tryOpen(directory, name, path);
		// if an image was returned, assume success
		if (o instanceof ImagePlus) return (ImagePlus)o;

		// if null and the file path does not exist, return null
		if (! new File(path).exists()) return null;

		// try opening the file with LOCI Bio-Formats plugin - always check this last!
		// Do not call Bio-Formats if File>Import>Image Sequence is opening this file.
		if (o==null && (IJ.getVersion().compareTo("1.38j")<0||!IJ.redirectingErrorMessages()) && width != IMAGE_OPENED) {
			Object loci = IJ.runPlugIn("loci.plugins.LociImporter", path);
			if (loci!=null) {
				// plugin exists and was launched
				try {
					// check whether plugin was successful
					Class c = loci.getClass();
					boolean success = c.getField("success").getBoolean(loci);
					boolean canceled = c.getField("canceled").getBoolean(loci);
					if (success || canceled) {
						width = IMAGE_OPENED;
						return null;
					}
				}
				catch (Exception exc) { }
			}
		}

		return null;
		
	} // openImage

	/**
	* Attempts to open the specified path with the given plugin. If the
	* plugin extends the ImagePlus class (e.g., BioRad_Reader), set
	* extendsImagePlus to true, otherwise (e.g., LSM_Reader) set it to false.
	*
	* @return A reference to the plugin, if it was successful.
	*/
	private Object tryPlugIn(String className, String path) {
		Object o = IJ.runPlugIn(className, path);
		if (o instanceof ImagePlus) {
			// plugin extends ImagePlus class
			ImagePlus imp = (ImagePlus)o;
				if (imp.getWidth()==0)
					o = null; // invalid image
				else
					width = IMAGE_OPENED; // success
		} else {
			// plugin does not extend ImagePlus; assume success
			width = IMAGE_OPENED;
		}
		return o;
	}


}
