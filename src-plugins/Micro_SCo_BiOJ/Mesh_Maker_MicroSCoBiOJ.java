import MeshMaker.*;


import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.awt.event.WindowListener;
import java.io.*;
import java.util.*;
import javax.swing.*;


import ij.*;
import ij.plugin.*;
import ij.process.*;
import ij.gui.*;
import ij.measure.*;
import ij.plugin.frame.Recorder;
import ij.plugin.frame.PlugInFrame;
import ij.plugin.frame.PasteController;

public class Mesh_Maker_MicroSCoBiOJ extends PlugInFrame implements PlugIn, Measurements, WindowListener ,
	Runnable, ActionListener, AdjustmentListener, ItemListener 
	{
	Checkbox markers=new Checkbox("Show markers bar");
	//Button addMark;
	ArrayList Points;
	ArrayList Colors;
	int [] histogram=new int[256];;
	Button delMark;
	PointPicker pp=null;
	ImageProcessor ip;
	ImagePlus imp;
	static final int RED=0, BLACK_AND_WHITE=1, OVER_UNDER=2;
	static final String[] modes = {"Red","Black & White", "Over/Under"};
	static final double defaultMinThreshold = 85; 
	static final double defaultMaxThreshold = 255;
	static boolean fill1 = true;
	static boolean fill2 = true;
	static boolean useBW = true;
	static Frame instance; 
	static int mode = RED;	
	ThresholdPlot plot = new ThresholdPlot();
	Thread thread;
	
	int minValue = -1;
	int maxValue = -1;
	int sliderRange = 256;
	boolean doAutoAdjust,doReset,doApplyLut,doStateChange,doQuit;
	
	Panel panel;
	Button autoB, resetB, applyB, quitB;
	int previousImageID;
	int previousImageType;
	double previousMin, previousMax;
	ImageJ ij;
	double minThreshold, maxThreshold;  // 0-255
	Scrollbar minSlider;
	Label label1;
	TextField tf_dimX;
	TextField tf_dimY;
	TextField tf_dimZ;
	double dimX;
	double dimY;
	double dimZ;
	
	boolean done;
	boolean invertedLut;
	int lutColor;	

	static Choice algorithm;
	static Choice oom;
	
	String ordine= new String("m");

	String algoMC=new String("Marching Cubes");
	String algoMT=new String("Marching Tetrahedron");
	int  algo=0;
	public Mesh_Maker_MicroSCoBiOJ() {
		
		super("Mesh_Maker_MicroSCoBiOJ");
		
		imp = WindowManager.getCurrentImage();
		
		
		if (imp == null  || !(imp.getStackSize() > 1)) {
			IJ.showMessage("Please select a stack");
			return;
		}
		
		if(imp.getType()==ImagePlus.COLOR_RGB || (imp.getBitDepth())!= 8) {
	    	IJ.showMessage("Stack format not supported"); 
	    	return; 
		}
		
		ip=imp.getProcessor();
		
		
		
		imp.addImageListener(new MyImageListener(this));
		
		if (instance!=null) {
			instance.toFront();
			instance.setVisible(true);
			return;
		}
		
		
		  this.setBackground(new Color(222,223,222));
		  this.setResizable(false);

	
  
		WindowManager.addWindow(this);
		instance = this;
		this.setLayout(new BorderLayout());
		//setLutColor(mode);
		lutColor = ImageProcessor.RED_LUT;
		IJ.register(PasteController.class);

		ij = IJ.getInstance();
		
		 instance.addWindowListener(this);
		Panel nord=new Panel();
		add(nord, BorderLayout.NORTH);
		Font font = new Font("SansSerif", Font.PLAIN, 10);
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		nord.setLayout(gridbag);
		
		
		// plot
		int y = 0;
		
		Panel panelChoise= new Panel();
		GridBagLayout gb = new GridBagLayout();
		GridBagConstraints cc = new GridBagConstraints();
		panelChoise.setLayout(gb);
		 //panelChoise.setLayout(new GridLayout(1, 2));
 		//cc.gridx=0;
 		//cc.gridy=0;
 		//cc.fill = GridBagConstraints.BOTH;
		//cc.anchor = GridBagConstraints.CENTER;
		//cc.insets = new Insets(10, 10, 0, 10);
		// addMark=new Button("Add Marker");
		// delMark=new Button("Del Marker(s)");
		// addMark.addActionListener(this);
		// delMark.addActionListener(this);
 		//panelChoise.add(addMark,cc);
 		//cc.gridx = 1;
 		//cc.fill = GridBagConstraints.BOTH;
		//cc.anchor = GridBagConstraints.CENTER;
		//cc.insets = new Insets(10, 10, 0, 10);
		//panelChoise.add(delMark,cc);
 		
		
		
		
		c.gridx=0;
		c.gridy=y++;
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.CENTER;
		c.insets = new Insets(10, 10, 0, 10);
		nord.add(panelChoise, c);
		
		c.gridx = 0;
		c.gridy = y++;
		c.gridwidth = 2;
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.CENTER;
		c.insets = new Insets(10, 10, 0, 10);
		nord.add(plot, c);
		
		
		
		// minThreshold slider
		minSlider = new Scrollbar(Scrollbar.HORIZONTAL, sliderRange/3, 1, 0, sliderRange);
		c.gridx = 0;
		c.gridy = y++;
		c.gridwidth = 1;
		c.weightx = IJ.isMacintosh()?90:100;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(5, 10, 0, 0);
		nord.add(minSlider, c);
		minSlider.addAdjustmentListener(this);
		minSlider.setUnitIncrement(1);
		
		// minThreshold slider label
		c.gridx = 1;
		c.gridwidth = 1;
		c.weightx = IJ.isMacintosh()?10:0;
		c.insets = new Insets(5, 0, 0, 10);
		label1 = new Label("       ", Label.RIGHT);
    	label1.setFont(font);
		nord.add(label1, c);
		
		

		// buttons
		int trim = IJ.isMacOSX()?15:0;
		panel = new Panel();
		panel.setBackground(new Color(222,223,222));
		autoB = new TrimmedButton("Auto",trim);
		autoB.addActionListener(this);
		autoB.addKeyListener(ij);
		panel.add(autoB);
		resetB = new TrimmedButton("Reset",trim);
		resetB.addActionListener(this);
		resetB.addKeyListener(ij);
		panel.add(resetB);
		applyB = new TrimmedButton("Start",trim);
		applyB.addActionListener(this);          
		applyB.addKeyListener(ij);               
		panel.add(applyB);                       
		quitB = new TrimmedButton("Quit",trim);
		quitB.addActionListener(this);
		quitB.addKeyListener(ij);
		panel.add(quitB);
		c.gridx = 0;
		c.gridy = y++;
		c.gridwidth = 2;
		c.insets = new Insets(0, 5, 10, 5);

		
		add(panel, BorderLayout.SOUTH);
		
		
		
		
		
		
		Panel sud= new Panel();	
		add(sud, BorderLayout.CENTER);                                       
		
		sud.setLayout(new GridBagLayout());
		GridBagConstraints cg = new GridBagConstraints();
		cg.fill = GridBagConstraints.HORIZONTAL;        
		
		
		cg.gridwidth=20;
		cg.gridx= 0;
		cg.gridy= 0;
		
		sud.add (new Label ("", Label.LEFT), cg);
	
				
		
		cg.gridwidth=20;
		cg.gridx= 0;
		cg.gridy= 1;
		sud.add (new Label ("Voxel dimension:", Label.LEFT), cg);

		cg.gridwidth=3;
		cg.gridy=2;
		sud.add(new Label( "X:", Label.LEFT), cg);
		cg.gridx=3;
		cg.gridwidth=15;			
		tf_dimX = new TextField("1.0",15);
		tf_dimX.setEditable(true);
		sud.add(tf_dimX, cg);
		
		
		
		cg.gridwidth=3;
		cg.gridy=3;
		cg.gridx=0;
		sud.add(new Label( "Y:", Label.LEFT), cg);
		cg.gridx=3;
		cg.gridwidth=15;			
		tf_dimY = new TextField("1.0",15);
		tf_dimY.setEditable(true);
		sud.add(tf_dimY, cg);
		
		
		cg.gridwidth=3;
		cg.gridy=4;
		cg.gridx=0;
		sud.add(new Label( "Z:", Label.LEFT), cg);
		
		cg.gridx=3;
		cg.gridwidth=15;			
		tf_dimZ = new TextField("1.0",15);
		tf_dimZ.setEditable(true);
		sud.add(tf_dimZ, cg);
		
		
		cg.gridwidth=20;
		cg.gridx= 0;
		cg.gridy= 5;
		
		sud.add( new Label("Order of magnitude: ", Label.LEFT), cg);
		
		
		cg.gridwidth=20;
		cg.gridx= 0;
		cg.gridy= 6;
		
		oom= new Choice();
		
		oom.addItem(new String("Kilometers"));
		oom.addItem(new String("Hectometers"));
		oom.addItem(new String("Decameters"));
		oom.addItem(new String("Meters"));
		oom.addItem(new String("Decimeters"));
		oom.addItem(new String("Centimeters"));
		oom.addItem(new String("Millimeters"));
		oom.addItem(new String("Micrometers"));
		oom.addItem(new String("Nanometers"));
		oom.addItem(new String("Picometers"));
		
		oom.select(3);
		oom.addItemListener(this);
		
		sud.add( oom, cg);
		
		
		
		cg.gridwidth=20;
		cg.gridx= 0;
		cg.gridy= 7;
		
		sud.add (new Label ("", Label.LEFT), cg);
	
		
		cg.gridwidth=20;
		cg.gridx= 0;
		cg.gridy= 8;
		
		sud.add (new Label ("Algorithm:", Label.LEFT), cg);

		
		
		algorithm = new Choice();
		algorithm.addItem(algoMC);
		algorithm.addItem(algoMT);
		algorithm.select(0);
		algorithm.addItemListener(this);
		cg.gridx = 2;
		cg.gridy = 9;
		cg.gridwidth = 2;
		cg.insets = new Insets(5, 5, 0, 5);
		cg.anchor = GridBagConstraints.CENTER;
		cg.fill = GridBagConstraints.NONE;
		sud.add(algorithm, cg);
		
		
		cg.gridwidth=20;
		cg.gridx= 0;
		cg.gridy= 10;
		sud.add (new Label ("", Label.LEFT), cg);
		
		
		cg.gridwidth=20;
		cg.gridx= 0;
		cg.gridy= 11;
		markers.addItemListener(this);
		sud.add (markers, cg);
		
	
		
		cg.gridwidth=20;
		cg.gridx= 0;
		cg.gridy= 12;
		sud.add (new Label ("", Label.LEFT), cg);
		
	
		
		
		
 		addKeyListener(ij);  // ImageJ handles keyboard shortcuts
		pack();
		GUI.center(this);
		show();

		thread = new Thread(this, "Mesh_Maker_MicroSCoBiOJ");
		
		thread.start();
		
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp!=null)
			setup(imp);
	}
	
	public synchronized void adjustmentValueChanged(AdjustmentEvent e) {
		if (e.getSource()==minSlider)
			minValue = minSlider.getValue();
	//	else
	//		maxValue = maxSlider.getValue();
		notify();
	}

	public synchronized  void actionPerformed(ActionEvent e) {
		Button b = (Button)e.getSource();
		
		
		
		
		if (b==null) return;
		
		/*
		if (b==delMark)
		
		{
			if (pp!=null)
			{
			for (int s = 0; (s < pp.ph.length); s++) {
					pp.ph[s].removePoints();
				}
				
			}
		}
		
		if (b==addMark)
	{
		pp=new PointPicker();
	}*/
	else{
		
		 if (b==resetB)
			doReset = true;
		else if (b==autoB)
			doAutoAdjust = true;
		else if (b==applyB)
			doApplyLut = true;
		else if (b==quitB)
			{
				
			doQuit = true;
		}
		
		notify();
	}
	}
	
	public synchronized void itemStateChanged(ItemEvent e) {
		
	
		if (e.getItemSelectable() == markers)
		
			if (markers.getState())
				pp=new PointPicker();
		
			else
			{
			pp.tb.restorePreviousToolbar();
			Toolbar.getInstance().repaint();	
			pointPickerTerminate terminateDialog
				= new pointPickerTerminate(IJ.getInstance());
		
				for (int s = 0; (s < pp.ph.length); s++) {
					pp.ph[s].removePoints();
				}
				pp.tb.cleanUpListeners();	
				pp=null;
				
			}	
		
		else
		{
		
			if (e.getItemSelectable() == algorithm)
				algo = algorithm.getSelectedIndex();
			else
			{
				
					
		
				//IJ.showMessage("selezionato " + oom.getItem(oom.getSelectedIndex()));
		if ((oom.getItem(oom.getSelectedIndex())).equals("Kilometers"))ordine = "Km"; 
		if ((oom.getItem(oom.getSelectedIndex())).equals("Hectometers"))ordine = "Hm";
		if ((oom.getItem(oom.getSelectedIndex())).equals("Decameters"))ordine = "Dm";
		if ((oom.getItem(oom.getSelectedIndex())).equals("Meters"))ordine = "m";
		if ((oom.getItem(oom.getSelectedIndex())).equals("Decimeters"))ordine = "dm";
		if ((oom.getItem(oom.getSelectedIndex())).equals("Centimeters"))ordine = "cm";
		if ((oom.getItem(oom.getSelectedIndex())).equals("Millimeters"))ordine = "mm";
		if ((oom.getItem(oom.getSelectedIndex())).equals("Micrometers"))ordine = "micron";
		if ((oom.getItem(oom.getSelectedIndex())).equals("Nanometers"))ordine = "nm";
		if ((oom.getItem(oom.getSelectedIndex())).equals("Picometers"))ordine = "pm";
		
			//IJ.showMessage("entro qui " + ordine );	
			}	
				
		}
	}

	ImageProcessor setup(ImagePlus imp) {
		ImageProcessor ip;
		int type = imp.getType();
		if (type==ImagePlus.COLOR_RGB)
			return null;
		ip = imp.getProcessor();
		boolean minMaxChange = false;
		if (type==ImagePlus.GRAY16 || type==ImagePlus.GRAY32) {
			if (ip.getMin()!=previousMin || ip.getMax()!=previousMax)
				minMaxChange = true;
	 		previousMin = ip.getMin();
	 		previousMax = ip.getMax();
		}
		int id = imp.getID();
		if (minMaxChange || id!=previousImageID || type!=previousImageType) {
			invertedLut = imp.isInvertedLut();
			minThreshold = ip.getMinThreshold();
			maxThreshold = ip.getMaxThreshold();
			if (minThreshold==ip.NO_THRESHOLD) {
				minThreshold = defaultMinThreshold;
				maxThreshold = defaultMaxThreshold;
			} else {
				minThreshold = scaleDown(ip, minThreshold);
				maxThreshold = scaleDown(ip, maxThreshold);
			}
			plot.setHistogram(imp);
			scaleUpAndSet(ip, minThreshold, maxThreshold);
			updateLabels(imp, ip);
			updatePlot();
			updateScrollBars();
			imp.updateAndDraw();
		}
	 	previousImageID = id;
	 	previousImageType = type;
	 	return ip;
	}
	
	/** Scales threshold levels in the range 0-255 to the actual levels. */
	
	void scaleUpAndSet(ImageProcessor ip, double minThreshold, double maxThreshold) {
		if (!(ip instanceof ByteProcessor) && minThreshold!=ImageProcessor.NO_THRESHOLD) {
			double min = ip.getMin();
			double max = ip.getMax();
			if (max>min) {
				minThreshold = min + (minThreshold/255.0)*(max-min);
				maxThreshold = min + (maxThreshold/255.0)*(max-min);
			} else
				minThreshold = ImageProcessor.NO_THRESHOLD;
		}
		ip.setThreshold(minThreshold, maxThreshold, lutColor);
	}

	/** Scales a threshold level to the range 0-255. */
	
	double scaleDown(ImageProcessor ip, double threshold) {
		double min = ip.getMin();
		double max = ip.getMax();
		if (max>min)
			return ((threshold-min)/(max-min))*255.0;
		else
			return ImageProcessor.NO_THRESHOLD;
	}
	
	/** Scales a threshold level in the range 0-255 to the actual level. */
	
	double scaleUp(ImageProcessor ip, double threshold) {
		double min = ip.getMin();
		double max = ip.getMax();
		if (max>min)
			return min + (threshold/255.0)*(max-min);
		else
			return ImageProcessor.NO_THRESHOLD;
	}

	void updatePlot() {
		plot.minThreshold = minThreshold;
		plot.maxThreshold = maxThreshold;
		plot.mode = mode;
		plot.repaint();
	}
	
	void updateLabels(ImagePlus imp, ImageProcessor ip) {
		double min = ip.getMinThreshold();
		double max = ip.getMaxThreshold();
		if (min==ImageProcessor.NO_THRESHOLD) {
			label1.setText("");
//			label2.setText("");
		} else {
			Calibration cal = imp.getCalibration();
			if (cal.calibrated()) {
				min = cal.getCValue((int)min);
				max = cal.getCValue((int)max);
			}
			if (((int)min==min && (int)max==max) || (ip instanceof ShortProcessor)) {
				label1.setText(""+(int)min);
				//label2.setText(""+(int)max);
			} else {
				label1.setText(""+IJ.d2s(min,2));
				//label2.setText(""+IJ.d2s(max,2));
			}
		}
	}

	void updateScrollBars() {
		minSlider.setValue((int)minThreshold);
		//maxSlider.setValue((int)maxThreshold);
	}
	
	/** Restore image outside non-rectangular roi. */
	
  	void doMasking(ImagePlus imp, ImageProcessor ip) {
		ImageProcessor mask = imp.getMask();
		if (mask!=null)
			ip.reset(mask);
	}

	void adjustMinThreshold(ImagePlus imp, ImageProcessor ip, double value) {
		if (IJ.altKeyDown()) {
			double width = maxThreshold-minThreshold;
			if (width<1.0) width = 1.0;
			minThreshold = value;
			//maxThreshold = minThreshold+width;
			maxThreshold = 255;
			if ((minThreshold+width)>255) {
				minThreshold = 255-width;
				//maxThreshold = minThreshold+width;
				maxThreshold = 255;
				minSlider.setValue((int)minThreshold);
			}
			//maxSlider.setValue((int)maxThreshold);
			scaleUpAndSet(ip, minThreshold, maxThreshold);
			return;
		}
		minThreshold = value;
		if (maxThreshold<minThreshold) {
			maxThreshold = minThreshold;
			//maxSlider.setValue((int)maxThreshold);
		}
		scaleUpAndSet(ip, minThreshold, maxThreshold);
	}

	void adjustMaxThreshold(ImagePlus imp, ImageProcessor ip, int cvalue) {
		//maxThreshold = cvalue;
		maxThreshold= 255;
		if (minThreshold>maxThreshold) {
			minThreshold = maxThreshold;
			minSlider.setValue((int)minThreshold);
		}
		scaleUpAndSet(ip, minThreshold, maxThreshold);
	}

	void reset(ImagePlus imp, ImageProcessor ip) {
		
		plot.setHistogram(imp);
		ip.resetThreshold();
		updateScrollBars();
		
		if (Recorder.record)
			Recorder.record("resetThreshold");
		
	}

	void doQuit(ImagePlus imp) {
		try{
		if (imp!=null)
		{	
		if (pp!=null)
		{
			pp.tb.restorePreviousToolbar();
			Toolbar.getInstance().repaint();	
			pointPickerTerminate terminateDialog
				= new pointPickerTerminate(IJ.getInstance());
		
				for (int s = 0; (s < pp.ph.length); s++) {
					pp.ph[s].removePoints();
				}
				pp.tb.cleanUpListeners();
	 	}
	 	ip=imp.getProcessor();
	 	reset(imp, ip); 
	    WindowManager.repaintImageWindows();	
	} 	
	
		this.close(); 
		
	
	}catch (Exception e){this.close();}
	    
		
		
		
	}

	void changeState(ImagePlus imp, ImageProcessor ip) {
		scaleUpAndSet(ip, minThreshold, maxThreshold);
		updateScrollBars();
	}

	void autoThreshold(ImagePlus imp, ImageProcessor ip) {
		if (!(ip instanceof ByteProcessor || ip instanceof ShortProcessor))
			return;
		ImageStatistics stats = ImageStatistics.getStatistics(ip, MIN_MAX+MODE, null);
		
		
		int threshold = this.getHistogramStack();
		
		threshold =  ip.getAutoThreshold(histogram);
		
		
		double lower, upper;
		
		lower=threshold;
		upper=255;
		
		ip.resetMinAndMax();
		ip.setThreshold(lower, upper, lutColor);
		minThreshold = scaleDown(ip, lower);
		//maxThreshold = scaleDown(ip, upper);
		maxThreshold = 255;
		//IJ.log(lower+" "+upper+" "+minThreshold+" "+maxThreshold+" "+ip.getMin()+" "+ip.getMax());
		updateScrollBars();
		imp.updateAndDraw();
		if (Recorder.record)
			Recorder.record("setThreshold", (int)ip.getMinThreshold(), (int)ip.getMaxThreshold());
 	}
 	
 	void apply() {
	 	boolean err=false;
 		try{
	 		
	 		dimX= Double.parseDouble(tf_dimX.getText());
	 		
	 	}catch (NumberFormatException e){IJ.showMessage("Dimension voxel X must be a number");err=true;}
	 	try{
	 		
	 		dimY= Double.parseDouble(tf_dimY.getText());
	 		
	 	}catch (NumberFormatException e){IJ.showMessage("Dimension voxel Y must be a number");err=true;}
	 	try{
	 		
	 		dimZ= Double.parseDouble(tf_dimZ.getText());
	 		
	 	}catch (NumberFormatException e){IJ.showMessage("Dimension voxel Z must be a number");err=true;}
	 	
	 	
	 	if (!err)
	 	{
	 		try {
		 		
		 		
			 Points= new ArrayList();
			Colors= new ArrayList();
			
			if (pp!=null)
			{
			for (int s = 0; (s < pp.ph.length); s++) {
				Vector listPoints = pp.ph[s].getPoints();
				
				Vector listColors = pp.ph[s].getColors();
				
				Points.add(listPoints);
				Colors.add(listColors);
				
				}
			
			
			pp.tb.restorePreviousToolbar();
			Toolbar.getInstance().repaint();
		}
		MyMarchingCubes mc=null;
		MyMarchingTetrahedron mt=null;
		
		imp=WindowManager.getCurrentImage();
		ip=imp.getProcessor();
		
		
		
		
		
		markers.setState(false);
			if (algorithm.getSelectedIndex()==0)
				mc= new MyMarchingCubes((int)minThreshold, imp, ip, dimX, dimY, dimZ, Points, Colors, ordine);
			else
				mt= new MyMarchingTetrahedron((int)minThreshold, imp, ip, dimX, dimY, dimZ, Points, Colors, ordine);
		if (pp!=null)
		{	
			pointPickerTerminate terminateDialog
				= new pointPickerTerminate(IJ.getInstance());
		/*
				for (int s = 0; (s < pp.ph.length); s++) {
					pp.ph[s].removePoints();
				}
				pp.tb.cleanUpListeners();*/
	 	}	
 			}
 			catch (Exception e) {IJ.showMessage("Error " + e.getMessage());}
		}
 	}
	
	static final int RESET=0, AUTO=1, HIST=2, APPLY=3, STATE_CHANGE=4, MIN_THRESHOLD=5, MAX_THRESHOLD=6, QUIT=7;

	
	public void run() {
		while (!done) {
			synchronized(this) {
				try {wait();}
				catch(InterruptedException e) {}
			}
			doUpdate();
		}
	}

	void doUpdate() {
		ImagePlus imp;
		ImageProcessor ip;
		int action;
		int min = minValue;
		int max = maxValue;
		if (doReset) action = RESET;
		else if (doAutoAdjust) action = AUTO;
		else if (doApplyLut) action = APPLY;
		else if (doStateChange) action = STATE_CHANGE;
		else if (doQuit) action = QUIT;
		else if (minValue>=0) action = MIN_THRESHOLD;
		else if (maxValue>=0) action = MAX_THRESHOLD;
		else return;
		minValue = -1;
		maxValue = -1;
		doReset = false;
		doAutoAdjust = false;
		doApplyLut = false;
		doStateChange = false;
		doQuit = false;
		imp = WindowManager.getCurrentImage();
		if (action==QUIT) {
			doQuit(imp); return;}
		if (imp==null) {
			IJ.beep();
			IJ.showStatus("No image");
			return;
		}
		
		
		
		ip = setup(imp);
		if (ip==null) {
			imp.unlock();
			IJ.beep();
			IJ.showStatus("RGB images cannot be thresolded");
			return;
		}
		//IJ.write("setup: "+(imp==null?"null":imp.getTitle()));
		switch (action) {
			case RESET: reset(imp, ip); break;
			case AUTO:{
		imp=WindowManager.getCurrentImage();
		ip=imp.getProcessor();
		 autoThreshold(imp, ip); break;}
			case APPLY: apply(); break;
			case STATE_CHANGE: changeState(imp, ip); break;
			case MIN_THRESHOLD: adjustMinThreshold(imp, ip, min); break;
			case MAX_THRESHOLD: adjustMaxThreshold(imp, ip, max); break;
		}
		updatePlot();
		updateLabels(imp, ip);
		ip.setLutAnimation(true);
		imp.updateAndDraw();
	}

    public void windowClosing(WindowEvent e) {
	    ImagePlus imp = WindowManager.getCurrentImage();
	   	if (imp!=null)
	   	{
	    ip=imp.getProcessor();
	    reset(imp, ip); 
	    WindowManager.repaintImageWindows();
  		}
		super.windowClosing(e);
		
		instance = null;
		done = true;
		synchronized(this) {
			notify();
		}
		
		
	}

    public void windowActivated(WindowEvent e) {
	    
    	super.windowActivated(e);
		//ImagePlus imp = WindowManager.getCurrentImage();
		imp=WindowManager.getCurrentImage();
		if (imp!=null) {
			previousImageID = 0;
			setup(imp);
		
		}
	}
                                      	                                                                                     
		public int getHistogramStack() {
		int level;
		int[] histogramTemp = new int[256];
		ImageProcessor iptemp=null;
		ImageStack stack = imp.getStack();
		for (int mmm=0; mmm<256; mmm++)
			histogram[mmm]=0;
		
		//IJ.showMessage("figura " + imp.getShortTitle() + " numero slice " + imp.getImageStackSize());
		for (int j=0; j<imp.getImageStackSize(); j++)
		{
			
			iptemp= stack.getProcessor(j+1);
			
			histogramTemp=iptemp.getHistogram();
			
		
			for (int k=0; k<256; k++)
				histogram[k]= histogram[k] + histogramTemp[k];
						
		
		}

		
		int maxValue = histogram.length - 1;
		double result,tempSum1,tempSum2,tempSum3,tempSum4;

		
		//for (int i=0; i<10; i++) //FROSI da decommentare
		//	histogram[i] = 0; //FROSI da decommentare
		
		histogram[maxValue] = 0;
		int min = 0;
		while ((histogram[min]==0) && (min<maxValue))
			min++;
		int max = maxValue;
		while ((histogram[max]==0) && (max>0))
			max--;
		if (min>=max) {
			level = histogram.length/2;
			return level;
		}

		int movingIndex = min;
		int inc = max/40;
		if (inc<1) inc = 1;
		do {
			tempSum1=tempSum2=tempSum3=tempSum4=0.0;
			for (int i=min; i<=movingIndex; i++) {
				tempSum1 += i*histogram[i];
				tempSum2 += histogram[i];
			}
			for (int i=(movingIndex+1); i<=max; i++) {
				tempSum3 += i *histogram[i];
				tempSum4 += histogram[i];
			}
			
			result = (tempSum1/tempSum2/2.0) + (tempSum3/tempSum4/2.0);
			movingIndex++;
			//if (max>255 && (movingIndex%inc)==0)
			//	ip.showProgress((double)(movingIndex)/max);
			} while ((movingIndex+1)<=result && movingIndex<=(max-1));
		
		//ip.showProgress(1.0);
		level = (int)Math.round(result);
		return level;
	}

                                        	                                                                                     
	
	
	
} // Mesh_Maker_MicroSCoBiOJ class


class ThresholdPlot extends Canvas implements Measurements, MouseListener {
	
	static final int WIDTH = 256, HEIGHT=48;
	double minThreshold = 85;
	double maxThreshold = 255;
	int[] histogram;
	Color[] hColors;
	int hmax;
	Image os;
	Graphics osg;
	int mode;
	
	public ThresholdPlot() {
		addMouseListener(this);
		setSize(WIDTH+1, HEIGHT+1);
	}

	void setHistogram(ImagePlus imp) {
		ImageProcessor ip = imp.getProcessor();
		if (!(ip instanceof ByteProcessor)) {
			double min = ip.getMin();
			double max = ip.getMax();
			ip.setMinAndMax(min, max);
			Rectangle r = ip.getRoi();
			ip = new ByteProcessor(ip.createImage());
			ip.setRoi(r);
		}
		ip.setMask(imp.getMask());
		ImageStatistics stats = ImageStatistics.getStatistics(ip, AREA+MODE, null);
		int maxCount2 = 0;
		histogram = stats.histogram;
		for (int i = 0; i < stats.nBins; i++)
		if ((histogram[i] > maxCount2) && (i != stats.mode))
			maxCount2 = histogram[i];
		hmax = stats.maxCount;
		if ((hmax>(maxCount2 * 2)) && (maxCount2 != 0)) {
			hmax = (int)(maxCount2 * 1.5);
			histogram[stats.mode] = hmax;
        	}
		os = null;

   		ColorModel cm = ip.getColorModel();
		if (!(cm instanceof IndexColorModel))
			return;
		IndexColorModel icm = (IndexColorModel)cm;
		int mapSize = icm.getMapSize();
		if (mapSize!=256)
			return;
		byte[] r = new byte[256];
		byte[] g = new byte[256];
		byte[] b = new byte[256];
		icm.getReds(r); 
		icm.getGreens(g); 
		icm.getBlues(b);
		hColors = new Color[256];
		for (int i=0; i<256; i++)
			hColors[i] = new Color(r[i]&255, g[i]&255, b[i]&255);
	}

	public void update(Graphics g) {
		paint(g);
	}

	public void paint(Graphics g) {
		if (histogram!=null) {
			if (os==null) {
				os = createImage(WIDTH,HEIGHT);
				osg = os.getGraphics();
				osg.setColor(Color.white);
				osg.fillRect(0, 0, WIDTH, HEIGHT);
				osg.setColor(Color.gray);
				for (int i = 0; i < WIDTH; i++) {
					if (hColors!=null) osg.setColor(hColors[i]);
					osg.drawLine(i, HEIGHT, i, HEIGHT - ((int)(HEIGHT * histogram[i])/hmax));
				}
				osg.dispose();
			}
			g.drawImage(os, 0, 0, this);
		} else {
			g.setColor(Color.white);
			g.fillRect(0, 0, WIDTH, HEIGHT);
		}
		g.setColor(Color.black);
 		g.drawRect(0, 0, WIDTH, HEIGHT);
 		if (mode==Mesh_Maker_MicroSCoBiOJ.RED)
			g.setColor(Color.red);
		else if (mode==Mesh_Maker_MicroSCoBiOJ.OVER_UNDER) {
			g.setColor(Color.blue);
 			g.drawRect(1, 1, (int)minThreshold-2, HEIGHT);
 			g.drawRect(1, 0, (int)minThreshold-2, 0);
 			g.setColor(Color.green);
 			g.drawRect((int)maxThreshold+1, 1, WIDTH-(int)maxThreshold, HEIGHT);
 			g.drawRect((int)maxThreshold+1, 0, WIDTH-(int)maxThreshold, 0);
			return;
		}
 		g.drawRect((int)minThreshold, 1, (int)(maxThreshold-minThreshold), HEIGHT);
 		g.drawLine((int)minThreshold, 0, (int)maxThreshold, 0);
     }

	public void mousePressed(MouseEvent e) {}
	public void mouseReleased(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mouseClicked(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	
	
	
	

}; // ThresholdPlot class


class MyImageListener implements ImageListener{
	
	Mesh_Maker_MicroSCoBiOJ mta;
	int currentS;
	MyImageListener(Mesh_Maker_MicroSCoBiOJ m){mta=m;currentS=mta.imp.getCurrentSlice()-1;}
           
	public void imageClosed(ImagePlus imp){}
	public void imageOpened(ImagePlus imp){}
    public void imageUpdated(ImagePlus imp){}
			

};



                           

                          

                                               

                          

                          

                          





                       