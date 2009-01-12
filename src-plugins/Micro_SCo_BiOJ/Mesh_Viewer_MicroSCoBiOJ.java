/*

	La classe MeshViewer si occupa di costruire il grafo della scena , visualizzarlo e rendere possibili
	trasformazioni di rotazione, traslazione, e settaggio della trasparenza di una o due mesh.

*/


import MeshViewer.*;

import ij.measure.Calibration;
import ij.*;
import java.lang.Math;
import java.applet.Applet;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import ij.plugin.PlugIn;
import java.util.*;

import java.text.ParseException;

import javax.swing.filechooser.FileSystemView;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import javax.swing.BorderFactory.*;
import javax.imageio.ImageIO;



import javax.vecmath.*;
import javax.media.j3d.*;
import com.sun.j3d.utils.applet.MainFrame; 
import com.sun.j3d.utils.universe.*;
import com.sun.j3d.utils.geometry.*;
import com.sun.j3d.utils.behaviors.mouse.*;


public class Mesh_Viewer_MicroSCoBiOJ extends JFrame implements ActionListener, ChangeListener, ItemListener, WindowListener,  PlugIn
{
	
	int firstnt=-1;
	int firstnv=-1;
	int secondnt=-1;
	int secondnv=-1;
	
	InfoWindow iw=new InfoWindow();
	//boolean iw_active = false;
	ArrayList infos=new ArrayList();
	Color color1=null;
	Color color2=null;
	Color color3=null;
	Color color4=null;
	String info_stats= null;
	//Markers
	public static final int GAMUT = 1024;				//Numero di colori
	final static Color spectrum[] = new Color[GAMUT];	//Array dei colori
	static final double raggio= 0.01;					//Raggio delle sfere
	 static private boolean hideEn;						//Booleano che indica se in coda al file ci sono informazioni sui marker
	 static private boolean hideAx;
	JCheckBox hidemarks=null;							//CheckBox per visualizzare/rimuovere
	JCheckBox hideAxis=null;
	 static private ArrayList marks=null;				//Lista dei punti 
	 static private ArrayList col_marks=null;			//Lista dei colori
	 static public ArrayList files=null;
	static String ordine=null;
	//Controlli sulle mesh caricate
	 private JButton addMesh=null;		//Pulsante che aggiunge una mesh a quella esistente
	
	 private JButton stats= null;
	 private JButton saveAs = null;
	 private int Sl_x_old_value=-1;		// valore che indica il precedente valore dello slider x
  	 private int Sl_y_old_value=-1;		// valore che indica il precedente valore dello slider y
  	 private int Sl_z_old_value=-1;		// valore che indica il precedente valore dello slider z
	 private JSlider Sl_x=null;			//Slider per la traslazione sull'asse x
	 private JSlider Sl_y=null;			//Slider per la traslazione sull'asse y  
	 private JSlider Sl_z=null;			//Slider per la traslazione sull'asse z
	 private JButton res_transl= null;	//Pulsante per riportare la scena nella posizione iniziale	
	
  	 private int Sl_t_current_value=50;
  	 
  	 private ArrayList slider_trasp=new ArrayList();
  	 private JSlider Sl_t1=null;			//Slider della trasparenza della prima mesh caricata
	 private JSlider Sl_t2=null;			//Slider della trasparenza della seconda mesh caricata
	 private JSlider Sl_t3=null;			//Slider della trasparenza della terza mesh caricata
	 private JSlider Sl_t4=null;
	
	 private JLabel s1;					//Etichetta per la prima mesh caricata				
	 private JLabel s2;					//Etichetta per la seconda mesh caricata				
	 private JLabel s3;					//Etichetta per la terza mesh caricata
	 private JLabel s4;
	
	 private JButton bc1= null;			//Pulsante che permette di cambiare colore della prima mesh
	 private JButton bc2= null;			//Pulsante che permette di cambiare colore della seconda mesh
	 private JButton bc3= null;
	 private JButton bc4= null;
	
	 private JPanel bottom= null;		
	JRadioButton[] radioButtons=null;			//Modalità di visualizzazione (point, line, fill)
	 private JLabel bott_s0 = new JLabel();
	 private JLabel bott_s1 = new JLabel();
	 private JLabel bott_s2 = new JLabel();
	
	 private URL tmp=null;			// Per aprire il file
  	 private String filename=null;	// Nome del file .off contente la mesh
 
  	//Oggetti della scena 
  	Canvas3D canvas3D=null;
  	Appearance[] a;														// Aspetti dei marker
	private Appearance appear3=null; 
  	private Appearance appear2=null;								// Aspetto della seconda mesh (trasparenza e materiale)
	 private Appearance appear=null;								// Aspetto della prima mesh (trasparenza e materiale)
	 private Appearance opaque=null;								
	 private Transform3D initial=null;							// Trasformazione iniziale
	 private TransformGroup objTransform=null;					
	 private TransformGroup t3d=null;
  	 private TransformGroup TG=null;
  	 private BranchGroup objRoot =null;
  	 private BranchGroup scene=null;
	 private SimpleUniverse simpleU=null; 	
	 private PolygonAttributes pa = new PolygonAttributes ();	
	 private Material mat=null;
	 private Material mat2=null;
	 private Material mat3=null;
	 private Material mat4=null;
	
	TransparencyAttributes transAttr=null;	
	float valoretrasp1=-1;
	float valoretrasp2=-1;
	float valoretrasp3=-1;
	  static private int NumVerts, NumTri;
     static private float [] v;
     static private int [] t;
     static private double area;
     
     private Axis ax=null;
     private Axis ay=null;
     private Axis az=null;
     
     ArrayList meshes=new ArrayList();
     ArrayList appearances=new ArrayList();
     
  	
  	private	int numMesh=0;										// numero di mesh attualmente caricate 
  	 static private Point3d LowC, HighC;
  
    private boolean first=true; 									// booleano che serve per primo comportamento di OPEN
     private boolean changedcolor=false; 						// booleano che serve a capire se è stato cambiato il materiale della prima mesh
     private boolean changedcolor2=false;						// booleano che serve a capire se è stato cambiato il materiale della seconda mesh
     private boolean changedcolor3=false;						//booleano che serve a capire se e' stato cambiato il materiale della terza mesh
     private boolean changedcolor4=false;
     
     
     
    
  public void 	windowActivated(WindowEvent e){}
        
 public void 	windowClosed(WindowEvent e)
          {}
 public void 	windowClosing(WindowEvent e)
          {
	          
	          if (scene!=null)
	          	scene.detach();
	          	
	          iw.dispose();
	                 
	          
	          }
 public void 	windowDeactivated(WindowEvent e)
          {}
 public void 	windowDeiconified(WindowEvent e)
          {}
 public void 	windowIconified(WindowEvent e)
          {}
 public void 	windowOpened(WindowEvent e) 
  		{}
  
        
  	public void stateChanged (ChangeEvent e)
  	{
	    JSpinner tmp = (JSpinner) e.getSource ();
	    SpinnerNumberModel m = (SpinnerNumberModel) tmp.getModel ();
  	}
	
  	public void itemStateChanged(ItemEvent e) 
  	{
	  	JCheckBox tmp = (JCheckBox) e.getSource();
	  	
	  	if (tmp==hidemarks)
	  	{ 
    	if (hidemarks.isSelected())
    	{
			for (int i=0; i<marks.size(); i++)
				a[i].setTransparencyAttributes(new TransparencyAttributes(TransparencyAttributes.NICEST, 1.0f));
    	}
    	else
    	{
	     	for (int i=0; i<marks.size(); i++)
			a[i].setTransparencyAttributes(new TransparencyAttributes(TransparencyAttributes.NICEST, .0f));
		} 
		}
		else if(tmp==hideAxis)
		{
			
			if (hideAxis.isSelected())
			{
				
				ax.viewAxis(false);
				ay.viewAxis(false);
				az.viewAxis(false);	
			}
			else
			{
				
				ax.viewAxis(true);
				ay.viewAxis(true);
				az.viewAxis(true);	
			}
			
		}
		
		   
  	}	
  
  	public void actionPerformed(ActionEvent e) 
  	{
	  
    	String temp = e.getActionCommand();
    	URL u = null;
    	
   		if (temp.equals ("Points")) pa.setPolygonMode (PolygonAttributes.POLYGON_POINT);
    	if (temp.equals ("Lines")) pa.setPolygonMode (PolygonAttributes.POLYGON_LINE);
    	if (temp.equals ("Fill")) pa.setPolygonMode (PolygonAttributes.POLYGON_FILL);
    	if (temp.equals ("Open File"))
    	{
	    	
			try
			{
				 
				meshes=new ArrayList();
				appearances= new ArrayList();
				
	   			JFileChooser chooser = new JFileChooser((new File("Mesh_Viewer_MicroSCoBiOJ.class")).getPath());
	   		
	   			MyFileFilter filter = new MyFileFilter();
	   		
    			filter.addExtension("off");
    			marks=null;
    	
    			col_marks=null;
    		
    			filter.setDescription("off triangulation");
    			chooser.setFileFilter(filter);

	   			int returnVal = chooser.showOpenDialog(null);
   				if(returnVal == JFileChooser.APPROVE_OPTION) 
   				{
	   		
	   				
           			filename=chooser.getSelectedFile().getAbsolutePath();
					
          			tmp = new URL ("file", "", filename);
 	    			BufferedReader Input = new BufferedReader(new InputStreamReader(tmp.openStream()));
					
 	    			this.hideEn=false;
 	    			this.hideAx=false;
 	    			
   		    		MeshRead (Input,0);
 					
   			    	Input.close ();
 					
    				
    				files=new ArrayList();
    				files.add(chooser.getName(chooser.getSelectedFile()));
    				
     				
	    			
    				
    			    Sl_y.setEnabled(true);
    				Sl_x.setEnabled(true);
    				Sl_z.setEnabled(true);
    			   	
        			res_transl.setEnabled(true);
        			stats.setEnabled(true);
        			Sl_x.setValue(50);
    				Sl_y.setValue(50);
    				Sl_z.setValue(50);
    				
        			addMesh.setEnabled(true);
        			
        			
        			
        			slider_trasp=new ArrayList();
 					slider_trasp.add(Sl_t1);
  					slider_trasp.add(Sl_t2);
  					slider_trasp.add(Sl_t3);
  					slider_trasp.add(Sl_t4);
        			
  					
        			
        			((JSlider)(slider_trasp.get(0))).setEnabled(true);
        			
        			((JSlider)(slider_trasp.get(0))).setValue(50);
        			
 					((JSlider)(slider_trasp.get(1))).setValue(50);
 					
	   				((JSlider)(slider_trasp.get(2))).setValue(50);
	   				
	   				((JSlider)(slider_trasp.get(3))).setValue(50);
	   				
	    			bc1.setEnabled(true);
	    			
    				if (first==false)
    				{

	    		
	    				pa.setCullFace (PolygonAttributes.CULL_NONE);
			   
    					if (radioButtons[0].isSelected()) pa.setPolygonMode (PolygonAttributes.POLYGON_POINT);
    					if (radioButtons[1].isSelected()) pa.setPolygonMode (PolygonAttributes.POLYGON_LINE);
    					if (radioButtons[2].isSelected()) pa.setPolygonMode (PolygonAttributes.POLYGON_FILL);
	    				scene.detach();  
						objRoot =null;
						objTransform=null;
   						t3d=null;
   						
   						
	   					
   						
  						TG=null;
  			
  						Sl_t1.setEnabled(true);
      					/*
						if (Sl_t2.isEnabled())
						{
  							ChangeListener aux=null;
							aux=(Sl_t1.getChangeListeners())[0];
	    		
    						Sl_t1.removeChangeListener((Sl_t1.getChangeListeners())[0]);
    						Sl_t1.addChangeListener((Sl_t2.getChangeListeners())[0]);
    
    						Sl_t2.removeChangeListener((Sl_t2.getChangeListeners())[0]);
    						Sl_t2.addChangeListener(aux);
    					}*/
    					
    					
 						Sl_t2.setEnabled(false);
 						bc2.setEnabled(false);
 						//bc3.setEnabled(false);
 						Sl_t3.setEnabled(false);
 						bc3.setEnabled(false);
 						Sl_t4.setEnabled(false);
 						bc4.setEnabled(false);
 						
	 				}
     
	 			
					scene= new BranchGroup();
			
					scene.setCapability(BranchGroup.ALLOW_DETACH);  
					
			     	scene = createSceneGraph(false);
			     	
			   
     		     	simpleU.addBranchGraph(scene);
  				
     				first=false;
     				
     				setInfos();
     				if (iw.isActive())
     				{
     					iw.updateIW(infos, appearances);
     					iw.repaint();
 					}
     				
     				
					numMesh=1;
				
        		 
        
    			}
			}catch (Exception er){IJ.showMessage("Error loading mesh: " + er.getMessage());}
		}
    
    	if (temp.equals("Add Mesh"))
    	{
	    
	    	try
	    	{
	   			JFileChooser chooser = new JFileChooser((new File("meshviewer.class")).getPath());
	   			MyFileFilter filter = new MyFileFilter();
    			filter.addExtension("off");
		    	marks=null;
		    	col_marks=null;
    			filter.setDescription("off triangulation");
    			chooser.setFileFilter(filter);
    	
    			
	   			int returnVal = chooser.showOpenDialog(null);
   				if(returnVal == JFileChooser.APPROVE_OPTION) 
   				{
	   				Sl_x.setValue(50);
   					Sl_y.setValue(50);
   					Sl_z.setValue(50);
              		
   					filename=chooser.getSelectedFile().getAbsolutePath();
              		files.add(chooser.getName(chooser.getSelectedFile()));
              		tmp = new URL ("file", "", filename);
 	      	 
    				BufferedReader Input = new BufferedReader(new InputStreamReader(tmp.openStream()));
    				
    				
   				   MeshRead (Input, 1);
    			   Input.close ();
    			   
    			    if (first==false)
    				{
	    
	    				pa.setCullFace (PolygonAttributes.CULL_NONE);
			   
    					if (radioButtons[0].isSelected()) pa.setPolygonMode (PolygonAttributes.POLYGON_POINT);
    					if (radioButtons[1].isSelected()) pa.setPolygonMode (PolygonAttributes.POLYGON_LINE);
    					if (radioButtons[2].isSelected()) pa.setPolygonMode (PolygonAttributes.POLYGON_FILL);
				
    					scene.detach();  
						objRoot =null;
						objTransform=null;
   						t3d=null;
  						TG=null;
	     			}
  
				   scene= new BranchGroup();
	   			   scene.setCapability(BranchGroup.ALLOW_DETACH);   
    			   scene = createSceneGraph(true);
    				
    			   simpleU.addBranchGraph(scene);
    			   first=false;
    			   
    			   if (numMesh==1)
    			   {
    			   	bc2.setEnabled(true);
    			   	Sl_t1.setValue(50);
    			   	Sl_t2.setEnabled(true);
    			   	//Sl_t2.addChangeListener(new SL_t (1));
			   	   }
     			   
     			   if (numMesh>1)
     		       {
	     		       try{
						
		     		       	bc3.setEnabled(true);
		     		       	Sl_t1.setValue(50);
		     		       	Sl_t2.setValue(50);
     			        	//Sl_t3.addChangeListener (new SL_t (2));
     			        	
     			        	Sl_t3.setEnabled(true);
     			        	
     			        	
     			        if (numMesh==3)	   
     			        {
     			   			addMesh.setEnabled(false);
     			   			bc4.setEnabled(true);
     			   			Sl_t4.setEnabled(true);
     			   			Sl_t3.setValue(50);
 			   			}
 			   		}catch(Exception error){IJ.showMessage("Errore nel mettere la trasp delle barre "+error.getMessage());};
     			   		
 			   	   }
 			   	   setInfos();
 			   	   
 			   	   if (iw.isActive())
 			   	   {
     					iw.updateIW(infos, appearances);
     					iw.repaint();		
 					}
     				
     		       numMesh++;
    			}
			}catch (Exception er){IJ.showMessage("Error adding mesh: " + er.getMessage());}
   		}
	
	if (temp.equals ("c1"))
	{
		
		Color newColor = JColorChooser.showDialog(
                     Mesh_Viewer_MicroSCoBiOJ.this,
                     "Choose Color",
                     new Color(1.0f,.0f,.0f));
       
           if (!changedcolor)
           		changedcolor=true;
                     
    
    
    // pa.setCullFace (PolygonAttributes.CULL_NONE);
    
    //appear.setPolygonAttributes (pa);
	
    
      
    float red= (float)(((float)newColor.getRed())/( (float)255));
    float green= (float)(((float)newColor.getGreen())/( (float)255));
    float blue= (float)(((float)newColor.getBlue())/( (float)255));
   
    
    Color3f lightcolch= new Color3f(red, green, blue);
    //Color3f colch= new Color3f((float)(red-0.3), (float)(green-0.3),(float)(blue-0.3));
    Color3f colch= new Color3f((float)(red), (float)(green),(float)(blue));
    bc1.setBackground(newColor);
    s1.setForeground(newColor);
    color1=newColor;
    
    Color3f black= new Color3f(.0f,.0f,.0f);
    Color3f white= new Color3f(1.0f,1.0f,1.0f);
    mat= new Material(colch, black, colch, white, 128.0f);
    //Material mat= new Material(red, black, red, white, 128.0f);  
    ((Appearance)appearances.get(0)).setMaterial(mat);    
    
 			   	   if (iw.isActive())
 			   	   {
     					iw.updateIW(infos, appearances);
     					iw.repaint();		
 					}
	}

	if (temp.equals ("c2"))
	{
		
		Color newColor = JColorChooser.showDialog(
                     Mesh_Viewer_MicroSCoBiOJ.this,
                     "Choose Color",
                     new Color(.0f,.0f,1.0f));
                     
        if (!changedcolor2)
           		changedcolor2=true;
                     
            
     pa.setCullFace (PolygonAttributes.CULL_NONE);
    //appear.setPolygonAttributes (pa);
          
    float red= (float)(((float)newColor.getRed())/( (float)255));
    float green= (float)(((float)newColor.getGreen())/( (float)255));
    float blue= (float)(((float)newColor.getBlue())/( (float)255));
    
     bc2.setBackground(newColor);
     s2.setForeground(newColor);
     color2=newColor;
    
    Color3f lightcolch= new Color3f(red, green, blue);
    //Color3f colch= new Color3f((float)(red-0.3), (float)(green-0.3),(float)(blue-0.3));
    Color3f colch= new Color3f((float)(red), (float)(green),(float)(blue));
    
    
    Color3f black= new Color3f(.0f,.0f,.0f);
    Color3f white= new Color3f(1.0f,1.0f,1.0f);
    mat2= new Material(colch, black, colch, white, 128.0f);
    ((Appearance)appearances.get(1)).setMaterial(mat2);  
    
 			   	   if (iw.isActive())
 			   	   {
     					iw.updateIW(infos, appearances);
     					iw.repaint();		
 					}  
}
	if (temp.equals ("c3"))
	{
		
		Color newColor = JColorChooser.showDialog(
                     Mesh_Viewer_MicroSCoBiOJ.this,
                     "Choose Color",
                     new Color(.0f,1.0f,.0f));
                     IJ.showMessage("1");
        if (!changedcolor3)
           		changedcolor3=true;
                     
            
    // pa.setCullFace (PolygonAttributes.CULL_NONE);
    //appear.setPolygonAttributes (pa);
          
    float red= (float)(((float)newColor.getRed())/( (float)255));
    float green= (float)(((float)newColor.getGreen())/( (float)255));
    float blue= (float)(((float)newColor.getBlue())/( (float)255));
    
     bc3.setBackground(newColor);
     s3.setForeground(newColor);
     color3=newColor;
     
    Color3f lightcolch= new Color3f(red, green, blue);
    //Color3f colch= new Color3f((float)(red-0.3), (float)(green-0.3),(float)(blue-0.3));
    Color3f colch= new Color3f((float)(red), (float)(green),(float)(blue));
    
    
    Color3f black= new Color3f(.0f,.0f,.0f);
    Color3f white= new Color3f(1.0f,1.0f,1.0f);
   mat3= new Material(colch, black, colch, white, 128.0f);
   
   
    ((Appearance)appearances.get(2)).setMaterial(mat3);      
    
 			   	   if (iw.isActive())
 			   	   {
     					iw.updateIW(infos, appearances);
     					iw.repaint();		
 					}
	}
	if (temp.equals ("c4"))
	{
		
		Color newColor = JColorChooser.showDialog(
                     Mesh_Viewer_MicroSCoBiOJ.this,
                     "Choose Color",
                     new Color(.0f,.0f,1.0f));
                     
        if (!changedcolor4)
           		changedcolor4=true;
                     
            
    // pa.setCullFace (PolygonAttributes.CULL_NONE);
    //appear.setPolygonAttributes (pa);
          
    float red= (float)(((float)newColor.getRed())/( (float)255));
    float green= (float)(((float)newColor.getGreen())/( (float)255));
    float blue= (float)(((float)newColor.getBlue())/( (float)255));
    
     bc4.setBackground(newColor);
     s4.setForeground(newColor);
     color4=newColor;
     
    Color3f lightcolch= new Color3f(red, green, blue);
    //nella riga successiva (commentata) il -3 sarebbe per fare sembrare un po piu scure
    // le mesh che per la trasparenza adottata sarebbe meglio
    
    //Color3f colch= new Color3f((float)(red-0.3), (float)(green-0.3),(float)(blue-0.3)); 
    
    //comunque per il momento provo a lasciare il colore "secco"
    Color3f colch= new Color3f((float)(red), (float)(green),(float)(blue)); 
    
    Color3f black= new Color3f(.0f,.0f,.0f);
    Color3f white= new Color3f(1.0f,1.0f,1.0f);
   mat4= new Material(colch, black, colch, white, 128.0f);
   
   
    ((Appearance)appearances.get(3)).setMaterial(mat4);      
    
 			   	   if (iw.isActive())
 			   	   {
     					iw.updateIW(infos, appearances);
     					iw.repaint();		
 					}
	}
	if (temp.equals("Reset"))
	{
		Sl_x.setValue(50);
		Sl_y.setValue(50);
		Sl_z.setValue(50);
	}

	if (temp.equals("Stats"))
	{
		
		
		try{
			
		
	  		if (!iw.isActive())
	  			iw =new InfoWindow(infos, appearances);
	  		else
	  		{
		  		
		  		iw.updateIW(infos, appearances);
		  		iw.repaint();
		  		}
	  		//IJ.showMessage("ci sono dopo");
		// JOptionPane.showMessageDialog(this, info_stats, "Statistics",

          //   JOptionPane.INFORMATION_MESSAGE);
          } catch (Exception errorr){IJ.showMessage("Error: " + errorr.getMessage());}
	}
	if (temp.equals ("Capture"))
	{
		
	try{
	
		
		
		
		
		///////////
			JFileChooser fc = new JFileChooser((new File("MeshViewer.class")).getPath());
			MyFileFilter filter = new MyFileFilter();
    		filter.addExtension("PNG");
    		//filter.setDescription("Format image");
    		fc.setFileFilter(filter);
			int returnVal = fc.showSaveDialog(new JPanel());
            if (returnVal == JFileChooser.APPROVE_OPTION) {
               
	            String name = fc.getSelectedFile().getAbsolutePath();
	            
			
		
		
		
		//////////
		GraphicsContext3D context = canvas3D.getGraphicsContext3D();
		int height = canvas3D.getBounds().height;
		int width = canvas3D.getBounds().width;
		
		
		BufferedImage bi2 = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		
		ImageComponent2D image = new ImageComponent2D(ImageComponent2D.FORMAT_RGBA,	bi2,true,false);
		
		
		
		
		 height = canvas3D.getBounds().height;
		 width = canvas3D.getBounds().width;
		Raster raster =	new Raster(new Point3f(-1.0f, -1.0f, -1.0f),Raster.RASTER_COLOR,0,0,width,height,image,null);

		
		
		context.readRaster(raster);
		BufferedImage bi = (raster.getImage()).getImage();
		Graphics2D g = bi.createGraphics();
		Font font = new Font("Arial", Font.PLAIN, 7);
		
		try {
			File outfile = new File(name+".png");
			//String[] formatNames = ImageIO.getWriterFormatNames();
			ImageIO.write(bi, "PNG", outfile);
		} catch (Exception errore) {
			errore.printStackTrace();
		}
	
	
	
 
			image =
				new ImageComponent2D(
					ImageComponent2D.FORMAT_RGBA,
					bi2,
					true,
					false);
		
 
	
	
	}
	
	
	
}
catch(Exception errorS){IJ.showMessage("Error: " + errorS.getMessage());}
	
		
	}
	
	
	
	
    
    }
   
        void setInfos()
    	{
			int i;
			infos = new ArrayList();
			String info_stats=new String();
			
			for (i=0; i<files.size(); i++)
			{
					info_stats=new String();									
					info_stats=info_stats.concat("S" + (i+1) + ": \nFile name: " + 
										files.get(i)+"\nNumber of Vertexes: " + 
										((Mesh)meshes.get(i)).getNV() + "\n" + 
										"Number of Triangles: " + ((Mesh)meshes.get(i)).getNT());
					
					if (((Mesh)meshes.get(i)).getArea()>=0)
					{
					
						String nf= new String(Mesh_Viewer_MicroSCoBiOJ.tronca(((Mesh)meshes.get(i)).getArea(), 2));
	  					info_stats=info_stats.concat("\nMesh area: " + nf + "" + ordine + "q");
	  				}
					else 
						info_stats=info_stats + ("\nMesh area: unknown");
					
					infos.add(info_stats);
	  		}	
	  		
	  		for (int ii=i; ii<4; ii++)
	  		{
		  			//info_stats=new String();									
		  			
					info_stats=new String("S" + (ii+1) + ": \n File name: ----.---\nNumber of Vertexes: --- \nNumber of Triangles: ---\nMesh area: ---");
	  				infos.add(info_stats);
	  		}	
    
}
    
    
    
    
    
    
    
    static String tronca(double x, int prec)
    {
		String ret= "" + (x);
		String esponente = new String("");
		
		try{
			
		int point=ret.indexOf('.');
		int exp=ret.indexOf('E');
		
	    if (exp!=-1)
	    {	
		    esponente = ret.substring(exp, ret.length());
		    ret=ret.substring(0, exp);
    	}
		if ((point!=-1) && (ret.length() > point + 3))
		{
			//IJ.showMessage("il punto e' " + point);
			String intero=ret.substring(0, point+1);
			//IJ.showMessage("intero " + intero);
			String decimale=ret.substring(point+1, point + 3);
			//IJ.showMessage("il dec " +decimale);
			ret=intero + decimale + esponente;
		}  
		
	    return ret;
    	}catch (Exception xx){return null;}
	    
	    
	    
    }
    
     
  static private void MeshRead (BufferedReader in, int mode) throws IOException
  {
	  
    String readValue = null;
    
    readValue = in.readLine ();
    
    if (readValue.equals ("OFF")) readValue = in.readLine ();

    String [] tmp = readValue.split ("\\s");
    NumVerts = Integer.parseInt (tmp[0]);	 
    NumTri = Integer.parseInt (tmp[1]);
      
    v = new float [NumVerts*3];
    t = new int [NumTri*3];

    float MaxX = 0f, MaxY = 0f, MaxZ = 0f, MinX = 0f, MinY = 0f, MinZ = 0f;

    Point3d [] bounds = new Point3d [2];
    
    int k = 0;
    for (int i = 0; i < NumVerts; ++i)
    {
      readValue = in.readLine ();
     
      String [] ss = readValue.split ("\\s");
      
      float x = Float.parseFloat (ss[0]);
      float y = Float.parseFloat (ss[1]);
      float z = Float.parseFloat (ss[2]); 
      
      if (i == 0)
      {
        MaxX = MinX = x;
        MaxY = MinY = y;
        MaxZ = MinZ = z;
      } else{
        if (x > MaxX) MaxX = x;
	if (x < MinX) MinX = x;
        if (y > MaxY) MaxY = y;
	if (y < MinY) MinY = y;
        if (z > MaxZ) MaxZ = z;
	if (z < MinZ) MinZ = z;
      }
      
      v[k++] = x;
      v[k++] = y; 
      v[k++] = z; 
    }
    
    
    if (mode==0)
    {
	   
	    LowC = new Point3d (MinX, MinY, MinZ);
    	HighC = new Point3d (MaxX, MaxY, MaxZ); 
	}
	else
	{
		
	 if (LowC.x>MinX)	
	 	LowC.x=MinX;
	 if (LowC.y>MinY)	
	 	LowC.y=MinY;
	 if (LowC.z>MinZ)	
	 	LowC.z=MinZ;
	 	
	if (HighC.x<MaxX)	
	 	HighC.x=MaxX;
	 if (HighC.y<MaxY)	
	 	HighC.y=MaxY;
	 if (HighC.z<MaxZ)	
	 	HighC.z=MaxZ;
		
		
	}
      
    k = 0;
    for (int i = 0; i < NumTri; ++i)
    {
      readValue = in.readLine ();
      String [] ss = readValue.split ("\\s");
    
      t[k++] = Integer.parseInt (ss[1]);
      t[k++] = Integer.parseInt (ss[2]);
      t[k++] = Integer.parseInt (ss[3]);
    }
    readValue = in.readLine();
    
    try{
    String [] ss=readValue.split("\\s");
    area= Double.parseDouble(ss[0]);
    if (ss.length == 2)
    	ordine= ss[1];
    
    
    readValue = in.readLine();
	}catch (NullPointerException e){area= -1;}
	try{
		String[] ss=readValue.split("\\s");
		readValue = in.readLine();
		
		
		
		double nslic=Double.parseDouble(ss[0]);
		
		marks=new ArrayList();
		col_marks= new ArrayList();
		double n=0;
		double aux1=0;
		double aux2=0;
		double aux3=0;
		Integer integ= new Integer(0);
		for (int h=0; h<nslic; h++)
		{
			ss=readValue.split("\\s");
			readValue = in.readLine();
			n=Double.parseDouble(ss[0]);
			for (int s=0; s<n; s++)
			{
				ss=readValue.split("\\s");
				readValue = in.readLine();
				aux1=Double.parseDouble(ss[0]);
				
				aux2=Double.parseDouble(ss[1]);
				
				aux3=Double.parseDouble(ss[2]);
				
				
				
	 			if (LowC.x>aux1)	
	 				 LowC.x=aux1;
	 			if (LowC.y>aux2)	
	 				LowC.y=aux2;
	 			if (LowC.z>aux3)	
	 				LowC.z=aux3;
	 				
		 		if (HighC.x<aux1)	
	 				HighC.x=aux1;
	 			if (HighC.y<aux2)	
	 				HighC.y=aux2;
	 			if (HighC.z<aux3)	
	 				HighC.z=aux3;
		
				
				
				integ= new Integer(Integer.parseInt(ss[3]));
				
					marks.add(new Vector3d(aux1, aux2, aux3));
					col_marks.add(integ);
				
					
					
					
					hideEn=true;
				
				
			}
				
			
		}
			  	
	  	

	}catch(NullPointerException e){hideEn=false;}
	
	
	
	
	
  }

  // create scene graph branch group
  public BranchGroup createSceneGraph(boolean b) throws IOException
  {
	  
	  try{
	
	  if (this.hideEn)
	  {
		 
	  	  hidemarks.setEnabled(true);
	  	  
	  	  hidemarks.setSelected(false);
	  	  
  	  }
	  else
	  {
		 
		  
	  	  hidemarks.setEnabled(false);
	  	  
	  	  hidemarks.setSelected(false);
	  	  
  	  }
	  } catch(Exception e){IJ.showMessage("Error loading markers: " + e.getMessage());}
		  
	
	//bottom.add(bott_s0);
	//bott_s.repaint();
	
    objRoot = new BranchGroup();
	objRoot.setCapability(BranchGroup.ALLOW_DETACH);
	
    TG= new TransformGroup(); // mia modifica ultima
    t3d=new TransformGroup();
   
    TG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
    TG.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
    
    t3d.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
    t3d.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);

	t3d.addChild(TG);    


    objRoot.addChild(t3d);
    

    objTransform = new TransformGroup();
    objTransform.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
    objTransform.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
 
    Mesh M = new Mesh (NumVerts, NumTri, v, t, area);
    M.setCapability (M.ALLOW_APPEARANCE_READ);
    M.setCapability (M.ALLOW_APPEARANCE_WRITE);
    M.setCapability (M.ALLOW_GEOMETRY_READ);
    M.setCapability (M.ALLOW_GEOMETRY_WRITE);
    
    
       
    BoundingBox MeshBB = new BoundingBox (LowC, HighC);
    
    float max_BB = (float) (HighC.x - LowC.x);
   
    if ((HighC.y - LowC.y) > max_BB) max_BB = (float) (HighC.y - LowC.y);
    if ((HighC.z - LowC.z) > max_BB) max_BB = (float) (HighC.z - LowC.z);
    
    BoundingLeaf BL = new BoundingLeaf (MeshBB);
    objTransform.addChild (BL);
	        
     Appearance appear = M.getAppearance();
    

		simpleU.getViewer().getView().setDepthBufferFreezeTransparent(true);
		
	appear.setCapability(Appearance.ALLOW_RENDERING_ATTRIBUTES_WRITE);
     appear.setCapability(Appearance.ALLOW_TRANSPARENCY_ATTRIBUTES_READ);
	 appear.setCapability(Appearance.ALLOW_TRANSPARENCY_ATTRIBUTES_WRITE);
	 appear.setCapability(Appearance.ALLOW_MATERIAL_WRITE);
	 appear.setCapability(Appearance.ALLOW_POLYGON_ATTRIBUTES_WRITE);
	 appear.setCapability(Appearance.ALLOW_COLORING_ATTRIBUTES_WRITE);
	 
	
    pa.setCapability(PolygonAttributes.ALLOW_MODE_WRITE);
     pa.setCapability(PolygonAttributes.ALLOW_CULL_FACE_WRITE);
     pa.setCapability(PolygonAttributes.ALLOW_CULL_FACE_READ);
     pa.setCapability(Appearance.ALLOW_POLYGON_ATTRIBUTES_READ );
     pa.setCapability(Appearance.ALLOW_POLYGON_ATTRIBUTES_WRITE );
     
     
    if (radioButtons[0].isSelected()) pa.setPolygonMode (PolygonAttributes.POLYGON_POINT);
    if (radioButtons[1].isSelected()) pa.setPolygonMode (PolygonAttributes.POLYGON_LINE);
    if (radioButtons[2].isSelected()) pa.setPolygonMode (PolygonAttributes.POLYGON_FILL);

    transAttr =
			new TransparencyAttributes(
					TransparencyAttributes.BLENDED,
					.5f,
					TransparencyAttributes.BLEND_SRC_ALPHA,
					TransparencyAttributes.BLEND_ONE);
	simpleU.getViewer().getView().setTransparencySortingPolicy(View.TRANSPARENCY_SORT_GEOMETRY);			
   
  
    appear.setTransparencyAttributes(transAttr);
    pa.setCullFace (PolygonAttributes.CULL_NONE);
    appear.setPolygonAttributes (pa);
    
  
    //setto il materiale(mia modifica)
    Color3f lightred= new Color3f(.6f, .0f,.2f);
    Color3f red= new Color3f(.3f, .0f,.0f);
    Color3f black= new Color3f(.0f,.0f,.0f);
    Color3f white= new Color3f(1.0f,1.0f,1.0f);
    if (!changedcolor)
    	mat= new Material(red, black, lightred, white, 128.0f);
    
    Color3f lightblue= new Color3f(.0f, .2f,.6f);
    Color3f blue= new Color3f(.0f, .0f,.3f);
    if (!changedcolor2)
    	 mat2= new Material(blue, black, lightblue, white, 128.0f);
    	 
    Color3f lightgreen= new Color3f(.0f, .6f, .2f);
    Color3f verde= new Color3f(.0f, .3f, .0f);	 
    if (!changedcolor3)
    	mat3= new Material(verde, black, lightgreen, white, 128.0f);

   Color3f lightyellow= new Color3f(.6f, .6f, .2f);
    Color3f giano= new Color3f(.3f, .3f, .0f);	 
    if (!changedcolor4)
    	mat4= new Material(giano, black, lightyellow, white, 128.0f);
    
    ColoringAttributes shading = new ColoringAttributes();
    shading.setShadeModel (ColoringAttributes.SHADE_GOURAUD); 

    
    appear.setColoringAttributes (shading);
    
    //appearances.add(appear);
    meshes.add(M);
    
   	

    //MARKS
    //ColoringAttributes shadingflat = new ColoringAttributes();
    //shadingflat.setShadeModel (ColoringAttributes.SHADE_FLAT);     
   Vector3d centro=null;
    try{
	  
	    
	 a = new Appearance[marks.size()];    
	 TransformGroup[] moonTrans= new TransformGroup[marks.size()];   
	 Sphere[] s=new Sphere[marks.size()];   
    for (int ss=0; ss<marks.size();ss++)
    {
    
    int currcol=((Integer)(col_marks.get(ss))).intValue();
    
    	Color3f lightred1= new Color3f(spectrum[currcol]);
    	Color3f red1= new Color3f(spectrum[currcol]);
    	Color3f black1= new Color3f(spectrum[currcol]);
    	Color3f white1= new Color3f(spectrum[currcol]);
    	 
    	a[ss]= new Appearance();
    	a[ss].setCapability(Appearance.ALLOW_TRANSPARENCY_ATTRIBUTES_READ);
	 	a[ss].setCapability(Appearance.ALLOW_TRANSPARENCY_ATTRIBUTES_WRITE);
	 	//a[ss].setColoringAttributes(shadingflat);
    	a[ss].setMaterial(new Material(red1, black1, lightred1, white1, 128.0f));
    	
   	    
    	s[ss]= new Sphere((float)0.01, a[ss]);
 		Vector3d aa= (Vector3d)marks.get(ss); 
	
		centro = new Vector3d ((float) (-(HighC.x + LowC.x)/2),
    					  (float) (-(HighC.y + LowC.y)/2), 
					  (float) (-(HighC.z + LowC.z)/2));
		aa.add(centro);					  
	
		aa.x= aa.x/max_BB;
		aa.y= aa.y/max_BB;
		aa.z= aa.z/max_BB;
	
 		Transform3D trasl = new Transform3D();
  		trasl.setTranslation(aa);
  		moonTrans[ss] = new TransformGroup(trasl);

    	moonTrans[ss].setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
  		moonTrans[ss].setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
  
  		moonTrans[ss].addChild(s[ss]);
  		TG.addChild(moonTrans[ss]); 

	}
} catch (NullPointerException e){}
	
      // FINE MARKS
  	    
    Color3f cyan= new Color3f(.0f,1.0f, 1.0f);
    Color3f	green= new Color3f(.2f,1.0f, .2f);
    Color3f yellow= new Color3f(1.0f,1.0f, .0f);
    Color3f nero= new Color3f(.0f,.0f, .0f);
       Appearance acx= new Appearance();
    
    
   
     initial = new Transform3D ();
    Transform3D scale = new Transform3D ();
    scale.setScale (1d/max_BB);
    initial.setTranslation (new Vector3f ((float) (-(HighC.x + LowC.x)/2),
    					  (float) (-(HighC.y + LowC.y)/2), 
					  (float) (-(HighC.z + LowC.z)/2)));
       
    scale.mul(initial);

    objTransform.setTransform (scale);
    
    
        
    TG.addChild (objTransform);
    
    
      Mesh aux;
   
    
    //ASSI
   
    ax = new Axis ('x');
  	
    ay = new Axis ('y');
  	
    az = new Axis ('z');
   	
    hideAxis.setEnabled(true);
 	
  

    appearances=new ArrayList();

	for (int i=0; i<meshes.size();i++)
	{

		aux= new Mesh();
		aux.duplicateNode((Mesh)meshes.get(i), true);
		//meshes.remove(i);
		//meshes.add(i, aux);
		
		objTransform.addChild(aux);
		//aux2= ((Mesh)meshes.get(i)).getAppearance();
		
		appearances.add(aux.getAppearance());
		((Appearance)appearances.get(i)).setPolygonAttributes(pa);

		
		
		
	}
		
    if (meshes.size()==1)
    	((Appearance)(appearances.get(0))).setMaterial (mat);
	if (meshes.size()==2)
	{
		((Appearance)(appearances.get(0))).setColoringAttributes(shading);
	   	((Appearance)(appearances.get(0))).setMaterial (mat);
    	((Appearance)(appearances.get(1))).setMaterial (mat2);
	}
   	if (meshes.size()==3)
   	{

    	((Appearance)(appearances.get(0))).setMaterial (mat);

    	((Appearance)(appearances.get(1))).setMaterial (mat2);

    	((Appearance)(appearances.get(2))).setMaterial (mat3);
	}
	if (meshes.size()==4)
   	{

    	((Appearance)(appearances.get(0))).setMaterial (mat);

    	((Appearance)(appearances.get(1))).setMaterial (mat2);

    	((Appearance)(appearances.get(2))).setMaterial (mat3);
    	
    	((Appearance)(appearances.get(3))).setMaterial (mat4);
	}
	
    TG.addChild (ax);
    TG.addChild (ay);
    TG.addChild (az);
	
    if ((this.hideAxis).isSelected())
    {
    	ax.viewAxis(false);
		ay.viewAxis(false);
		az.viewAxis(false);	
    
    }
    
    AmbientLight lightA = new AmbientLight();
    lightA.setInfluencingBoundingLeaf (BL);
    objRoot.addChild(lightA);

    DirectionalLight lightD = new DirectionalLight();
    lightD.setInfluencingBoundingLeaf (BL);
    lightD.setDirection (0f, 0f, -1f);
    objRoot.addChild(lightD);	

    PointLight lightP = new PointLight();
    lightP.setInfluencingBoundingLeaf (BL);
    lightP.setPosition ((float)(.5*(HighC.x - LowC.x)), 
    			(float)(.5*(HighC.y - LowC.y)), 
			(float)(.5*(HighC.z - LowC.z)));
    objRoot.addChild(lightP);
    
    
    MyMouseRotate MR = new MyMouseRotate (bott_s0, bott_s1, bott_s2);
    
    bott_s0.setText((MR.getLabel(0)).getText());
    bott_s0.repaint();
    bott_s1.setText((MR.getLabel(1)).getText());
    bott_s1.repaint();
    bott_s2.setText((MR.getLabel(2)).getText());
    //bott_s2.setOpaque(true);
    //bott_s2.setForeground(new Color(153/255, 255/255, 255/255));
    bott_s2.repaint();
    
    
    
    
    
    MR.setTransformGroup (TG);
    
    
    MR.setSchedulingBounds (new BoundingSphere ());
    objRoot.addChild (MR);
	//t3d.addChild(MR);
    MouseWheelZoom MWZ = new MouseWheelZoom ();
    MWZ.setTransformGroup (TG);
    MWZ.setSchedulingBounds (new BoundingSphere ());
    MWZ.setFactor (0.10 * MWZ.getFactor ());
    
    objRoot.addChild (MWZ);

    MouseZoom MZ = new MouseZoom ();
    MZ.setTransformGroup (TG);
    MZ.setSchedulingBounds (new BoundingSphere ());
    MZ.setFactor (0.10 * MZ.getFactor ());
    objRoot.addChild (MZ);
	
    objRoot.compile();
     
    return objRoot;
    


  }
  
  
   // end of CreateSceneGraph method 
  

   
  public class SL_X_listener implements ChangeListener
  {
    public void stateChanged (ChangeEvent e)
    {
      JSlider source = (JSlider) e.getSource();
		int Sl_x_current_value= source.getValue();
      
      
      
      int tmp = ((int) source.getValue())-50;
      
      
       double valmed = (HighC.x+LowC.x)/2; 
     
                
      double traslazione=(Sl_x_current_value-Sl_x_old_value)*1.0/50;
      
      Sl_x_old_value=Sl_x_current_value;
      
 
      try{
      
    Vector3d vettore=new Vector3d(traslazione,(double).0,(double).0);
    
    Transform3D trasl=new Transform3D();
   	Transform3D objT = new Transform3D();
   	t3d.getTransform(objT); 
    trasl.setTranslation(vettore);
    objT.mul(trasl); 
    t3d.setTransform(objT); 
		}catch(Exception a){IJ.showMessage("Error: " + a.getMessage());}
    }
  }

  private class SL_Y_listener implements ChangeListener
  {
	  	
	  
     public void stateChanged (ChangeEvent e)
    {
      JSlider source = (JSlider) e.getSource();
		int Sl_y_current_value= source.getValue();
      
      
      
      int tmp = ((int) source.getValue())-50;
      
      
       
       double valmed = (HighC.x+LowC.x)/2; 
     
      
      // x : 0.5 = tmp : 50
      
           
      double traslazione=(Sl_y_current_value-Sl_y_old_value)*1.1/50;
      
      Sl_y_old_value=Sl_y_current_value;
      
 
      
      
      Vector3d vettore=new Vector3d((double).0,traslazione,(double).0);
    
     
      
      
      Transform3D trasl=new Transform3D();
      
    Transform3D objT = new Transform3D();
    
    
    t3d.getTransform(objT); 
    

      
       trasl.setTranslation(vettore);
      
      
      
       objT.mul(trasl); 
      t3d.setTransform(objT); 
      
	
    }
  }
  
  
  private class SL_Z_listener implements ChangeListener
  {
     public void stateChanged (ChangeEvent e)
    {
      JSlider source = (JSlider) e.getSource();
		int Sl_z_current_value= source.getValue();
      
      
      
      int tmp = ((int) source.getValue())-50;
      
       
       double valmed = (HighC.x+LowC.x)/2; 
    
      double traslazione=(Sl_z_current_value-Sl_z_old_value)*0.5/50;
      
      Sl_z_old_value=Sl_z_current_value;
      
 
      
      
      Vector3d vettore=new Vector3d((double).0,(double).0,traslazione);
    
 
      
      
      Transform3D trasl=new Transform3D();
      
    Transform3D objT = new Transform3D();
    
    
    t3d.getTransform(objT); 
    

      
       trasl.setTranslation(vettore);
      
      
      
       objT.mul(trasl); 
      t3d.setTransform(objT); 
      
	
    
    }
  }
  
  
  
  
  
  private class SL_t implements ChangeListener
  {
	  
	 int id;
	 SL_t(int i)
	 {
		 super();
		 this.id=i;
		 
		 
		 }
	
     public void stateChanged (ChangeEvent e)
    {
	   
      JSlider source = (JSlider) e.getSource();
  		
      
      
		 Sl_t_current_value= source.getValue();
   
		 
		 
		 if (Sl_t_current_value==0) 
		 { 
			 
			 		 
			 RenderingAttributes renderAttr = new RenderingAttributes();
			if (appearances.size()>this.id)
			{
     			((Appearance)(appearances.get(this.id))).setRenderingAttributes(renderAttr);
			
		 		((Appearance)(appearances.get(this.id))).setTransparencyAttributes(new TransparencyAttributes(TransparencyAttributes.NONE, .0f));
		 	//appear2.getMaterial(); //			19/9/2007
 			}
	 				 	
	 	 }
		 else
		 {
		 
       		valoretrasp1=(float)(((float)Sl_t_current_value)/100);
            simpleU.getViewer().getView().setDepthBufferFreezeTransparent(true);
        
      		
      		transAttr =
				new TransparencyAttributes(
					TransparencyAttributes.BLENDED,
					valoretrasp1,
					TransparencyAttributes.BLEND_SRC_ALPHA,
					TransparencyAttributes.BLEND_ONE);
    		
    		if (appearances.size()>this.id)
    		{
	    		 
				 ((Appearance)(appearances.get(this.id))).setTransparencyAttributes(transAttr);
				 
			}
					
    	
    
    
 }
    }
  }
  
  
  
  
  // Create a simple scene and attach it to the virtual universe
  private void createWindow (int narg) throws IOException
  {
    setLayout(new BorderLayout());
    GraphicsConfiguration config = SimpleUniverse.getPreferredConfiguration();
    
    canvas3D = new Canvas3D(config);
    
   
    
    
    add("Center", canvas3D);
	JLabel translat=new JLabel(" Translation"); 
    JPanel top = new JPanel (new FlowLayout (FlowLayout.LEADING));
    
    /*mia modifica da cancellare*/
    JButton open=new JButton("Open File");
   
    addMesh=new JButton("Add Mesh");
    
  
    addMesh.setEnabled(false);
    
    open.addActionListener(this);
    
    addMesh.addActionListener(this);
    
    top.add(open);
    
    top.add(addMesh);
    
     // inserimento salva come
   
   saveAs= new JButton("Capture");
   saveAs.addActionListener(this);
   top.add(saveAs);
   
   //fine salva come
    /*fine mia modifica da cancellare */
    
    top.add (new JLabel ("		Mesh visualization"));
    
    radioButtons = new JRadioButton[3];
    ButtonGroup group = new ButtonGroup();
        
    radioButtons[0] = new JRadioButton("Points");
    radioButtons[0].setActionCommand("Points");
    radioButtons[0].addActionListener (this);
    
    radioButtons[1] = new JRadioButton("Lines");
    radioButtons[1].setActionCommand("Lines");
    radioButtons[1].addActionListener (this);
    
    radioButtons[2] = new JRadioButton("Fill");
    radioButtons[2].setActionCommand("Fill");
    radioButtons[2].setSelected (true);
    radioButtons[2].addActionListener (this);

    for (int i = 0; i < 3; i++) 
    { 
      top.add(radioButtons[i]);
      group.add(radioButtons[i]);
    }

    
    float min = 0.0f;
    float max = 10.0f; 
    float step = .1f; 
    
  
    add("North", top);
	//if (area>=0)
    //	bott_s = new JLabel ("Mesh area: " + area);
    //else
    //	bott_s = new JLabel ("Mesh area: unknown");
    //bottom = new JPanel (new BorderLayout ());
    //bottom.add (bott_s);
    //add ("South", bottom);
    
    scene=null;
    
    
    int numsx = 3;
    int numsy = 3;
    int numsz = 3;
    if (narg==1)
    {
    	scene = createSceneGraph(false);
    	scene.setCapability(BranchGroup.ALLOW_DETACH);
    
     numsx = (int) ((HighC.x - LowC.x) / .1f);
     numsy = (int) ((HighC.y - LowC.y) / .1f);
     numsz = (int) ((HighC.z - LowC.z) / .1f);
}
   
 
   
/// GridBagConstraints gc =  new GridBagConstraints ();
///    gc.fill = GridBagConstraints.HORIZONTAL;
///    gc.weightx = 0.5; 
    
    Sl_x = new JSlider (JSlider.VERTICAL, 0, numsx, 0);
  
    Sl_x.setPreferredSize(new Dimension(20, 170));
     Sl_x.setMaximum(100);
     Sl_x.setMinimum(0);
     	Sl_x.setValue(50);
    Sl_x_old_value=50;
    Sl_x.setEnabled(false);
    Sl_x.addChangeListener (new SL_X_listener ());
    
    
    
    Sl_y = new JSlider (JSlider.VERTICAL, 0, numsy, 0);
    
    
    
    Sl_y.setPreferredSize(new Dimension(20, 170));
     Sl_y.setMaximum(100);
     Sl_y.setMinimum(0);
     Sl_y.setValue(50);
     Sl_y_old_value=50;
    Sl_y.setEnabled(false);
    Sl_y.addChangeListener (new SL_Y_listener ());

    
    Sl_z = new JSlider (JSlider.VERTICAL, 0, numsz, 0);
    
    Sl_z.setPreferredSize(new Dimension(20, 170));
     Sl_z.setMaximum(100);
     Sl_z.setMinimum(0);
     Sl_z.setValue(50);
     Sl_z_old_value=50;
    Sl_z.setEnabled(false);
    Sl_z.addChangeListener (new SL_Z_listener ());

    

    Sl_t1 = new JSlider (JSlider.VERTICAL, 0, numsx, 0);
  

	Sl_t1.setPreferredSize(new Dimension(17, 170));
     Sl_t1.setMaximum(100);
     Sl_t1.setMinimum(0);
     	Sl_t1.setValue(50);
   
    Sl_t1.setEnabled(false);
    Sl_t1.addChangeListener (new SL_t (0));
        
    Sl_t2 = new JSlider (JSlider.VERTICAL, 0, numsx, 0);
     	Sl_t2.setPreferredSize(new Dimension(17, 170));
     Sl_t2.setMaximum(100);
     Sl_t2.setMinimum(0);
     	Sl_t2.setValue(50);
    
    Sl_t2.setEnabled(false);
    Sl_t2.addChangeListener (new SL_t (1));
    
    Sl_t3 = new JSlider (JSlider.VERTICAL, 0, numsx, 0);
     
     Sl_t3.setMaximum(100);
     Sl_t3.setMinimum(0);
     	Sl_t3.setValue(50);
 	Sl_t3.setPreferredSize(new Dimension(17, 170));
    Sl_t3.setEnabled(false);
    Sl_t3.addChangeListener (new SL_t (2));
    
    
    
    Sl_t4 = new JSlider (JSlider.VERTICAL, 0, numsx, 0);
  


     Sl_t4.setMaximum(100);
     Sl_t4.setMinimum(0);
     	Sl_t4.setValue(50);
   	Sl_t4.setPreferredSize(new Dimension(17, 170));
    Sl_t4.setEnabled(false);
    Sl_t4.addChangeListener (new SL_t (3));
   
    
  /*slider_trasp=new ArrayList();
  slider_trasp.add(Sl_t1);
  slider_trasp.add(Sl_t2);
  slider_trasp.add(Sl_t3);*/
// METTO I VARI COMPONENTI

	//JButton button;
	
	
	

/*FABIO FROSI
east.setLayout(new GridBagLayout());
GridBagConstraints c = new GridBagConstraints();
c.fill = GridBagConstraints.HORIZONTAL;
c.weightx = 0.5;

c.gridwidth=6;
c.gridx= 0;
c.gridy= 0;
east.add (new JLabel ("Translations", JLabel.CENTER), c);

c.gridwidth=2;
c.gridx=0;
c.gridy= 1;
east.add(Sl_x, c);
c.gridx=2;
east.add(Sl_y, c);
c.gridx=4;
east.add(Sl_z, c);

c.gridwidth=2;
c.gridx=0;
c.gridy=2;
c.ipady=10;
east.add (new JLabel ("X", JLabel.CENTER), c);
c.gridx=2;
east.add (new JLabel ("Y", JLabel.CENTER), c);
c.gridx=4;
east.add (new JLabel ("Z", JLabel.CENTER), c);


c.gridwidth=6;
c.ipady=0;
c.gridx=0;
c.gridy=3;
*/
res_transl=new JButton("Reset");
res_transl.setPreferredSize(new Dimension(85,15));
res_transl.setEnabled(false);
res_transl.addActionListener(this);
/*east.add(res_transl, c);



// Trasparenza


c.gridwidth=0;
c.gridx= 0;
c.gridy= 5;
c.ipady= 20;
c.anchor=GridBagConstraints.PAGE_END;
east.add (new JLabel ("Transparency", JLabel.CENTER), c);

c.gridwidth=2;
c.ipady=-7;
c.gridx=3;
c.gridy=5;*/
s1=new JLabel("  S1", JLabel.CENTER);
s1.setForeground(Color.red);
/*east.add(s1, c);

c.gridx=5;*/
s2=new JLabel(" S2", JLabel.CENTER);
s2.setForeground(Color.blue);
/*east.add(s2, c);

c.gridx=6;*/
s3=new JLabel(" S3", JLabel.CENTER);
s3.setForeground(new Color (.0f, .5f, .0f));
/*east.add(s3, c);


c.gridx=9;*/
s4=new JLabel("S4", JLabel.CENTER);
s4.setForeground(new Color (.5f, .5f, .0f));
/*east.add(s4, c);





c.gridwidth=2;
c.ipady=0;
c.gridx=0;
c.gridy=6;
c.anchor=GridBagConstraints.PAGE_START;
JLabel piu=new JLabel ("+", JLabel.CENTER);
piu.setFont(new Font("Courier", Font.PLAIN,  18));

east.add (piu, c);
c.anchor=GridBagConstraints.PAGE_END;
JLabel meno=new JLabel ("-", JLabel.CENTER);
meno.setFont(new Font("Courier", Font.PLAIN,  18));
east.add (meno, c);

c.gridwidth=2;
c.gridx=3;
east.add(Sl_t1, c);
c.gridx=5;
east.add(Sl_t2, c);

c.gridx=7;
east.add(Sl_t3, c);

c.gridx=9;
east.add(Sl_t4, c);

c.gridy=7;
c.ipady=5;
c.gridx=3;

*/
bc1=new JButton("c1");
bc1.setPreferredSize(new Dimension(15,10));
bc1.addActionListener(this);
bc1.setForeground(Color.RED);
bc1.setBackground(Color.RED);
bc1.setEnabled(false);
///east.add(bc1, c);

///c.gridx=5;
bc2=new JButton("c2");
bc2.setPreferredSize(new Dimension(15,10));
bc2.addActionListener(this);
bc2.setForeground(Color.BLUE);
bc2.setBackground(Color.BLUE);
bc2.setEnabled(false);
///east.add(bc2, c);

///c.gridx=7;
bc3=new JButton("c3");
bc3.setPreferredSize(new Dimension(15,10));
bc3.addActionListener(this);
bc3.setForeground(new Color (.0f, .5f, .0f));
bc3.setBackground(new Color (.0f, .5f, .0f));
bc3.setEnabled(false);
///east.add(bc3, c);


///c.gridx=9;
bc4=new JButton("c4");
bc4.setPreferredSize(new Dimension(15,10));
bc4.addActionListener(this);
bc4.setForeground(new Color (.0f, .5f, .0f));
bc4.setBackground(new Color (.5f, .5f, .0f));
bc4.setEnabled(false);
///east.add(bc4, c);

hidemarks= new JCheckBox("Hide mark");
hidemarks.addItemListener(this);
///c.gridy=8;
///c.ipady= 20;
///c.gridwidth=6;
///c.gridx=0;
hidemarks.setEnabled(false);
///east.add(hidemarks, c);

hideAxis= new JCheckBox("Hide axis");
hideAxis.addItemListener(this);
///c.gridy=9;
///c.ipady= 20;
///c.gridwidth=6;
///c.gridx=0;
hideAxis.setEnabled(false);
///east.add(hideAxis, c);



///c.gridwidth=6;
///c.ipady=0;
///c.gridx=0;
///c.gridy=10;

stats=new JButton("Stats");
stats.setPreferredSize(new Dimension(85,15));
stats.setEnabled(false);
stats.addActionListener(this);
///east.add(stats, c);

JLabel piu=new JLabel ("+", JLabel.CENTER);
piu.setFont(new Font("Courier", Font.PLAIN,  10));

JLabel meno=new JLabel ("-", JLabel.CENTER);
meno.setFont(new Font("Courier", Font.PLAIN,  10));


JLabel spazio=new JLabel (" ", JLabel.CENTER);
spazio.setFont(new Font("Courier", Font.PLAIN,  18));

// pannello est

 Border b = BorderFactory.createEmptyBorder (2, 2, 2, 2);
    
    
 	JPanel contenitore= new JPanel(new FlowLayout());
 	contenitore.setBorder(b);
 	JPanel east = new JPanel (new BorderLayout ());
    //east.setBorder (b); AKTUNG
    
    	JPanel eastNord = new JPanel (new BorderLayout());
    
    			JPanel translations = new JPanel(new FlowLayout());
    			translations.add(new JLabel("Translations"));
    		
    			JPanel slidertrasl = new JPanel(new FlowLayout());
    			slidertrasl.add(Sl_x);
    			slidertrasl.add(Sl_y);
    			slidertrasl.add(Sl_z);
    		
    			
    			JPanel lab_reset = new JPanel(new BorderLayout());
    			
    				JPanel xyzlabel = new JPanel(new FlowLayout());
    				xyzlabel.add(new JLabel("  X  ",JLabel.CENTER));
    				xyzlabel.add(new JLabel(" Y  ",JLabel.CENTER));
    				xyzlabel.add(new JLabel(" Z  ",JLabel.CENTER));
    				
  
    				JPanel p_reset = new JPanel(new FlowLayout());
    				p_reset.add(res_transl);
    				
    			lab_reset.add(xyzlabel, BorderLayout.NORTH);  				
    			lab_reset.add(p_reset, BorderLayout.CENTER);
    		
    
    		eastNord.add(translations, BorderLayout.NORTH);
    		eastNord.add(slidertrasl, BorderLayout.CENTER);
    		eastNord.add(lab_reset, BorderLayout.SOUTH);
    		
    	JPanel eastCenter = new JPanel (new BorderLayout());
    	
    	
    			JPanel cappello = new JPanel(new BorderLayout());
    			   			
    				JPanel transparency = new JPanel(new FlowLayout());
    				transparency.add(new JLabel("Transparency"));
    				
    				JPanel surf_label = new JPanel(new FlowLayout());
    				surf_label.add(new JLabel(" "));
    				surf_label.add(s1);
    				surf_label.add(spazio);
    				surf_label.add(s2);
    				surf_label.add(spazio);
    				surf_label.add(s3);
    				surf_label.add(spazio);
    				surf_label.add(s4);
    			
    			cappello.add(transparency, BorderLayout.NORTH);
    			cappello.add(surf_label, BorderLayout.CENTER);
    			
    			JPanel corpo = new JPanel(new BorderLayout());
    			
    				/*JPanel surf_label = new JPanel(new FlowLayout());
    				surf_label.add(s1);
    				surf_label.add(s2);
    				surf_label.add(s3);
    				surf_label.add(s4);*/
    			
    				JPanel trasp_slider = new JPanel(new FlowLayout());
    				
    				JPanel etichette =  new JPanel(new BorderLayout());
    					
						
					etichette.setPreferredSize(new Dimension(10, 170));
					etichette.add(piu, BorderLayout.PAGE_START);
					etichette.add(meno, BorderLayout.PAGE_END);

					trasp_slider.add(etichette);
    				trasp_slider.add(Sl_t1);
    				trasp_slider.add(Sl_t2);
    				trasp_slider.add(Sl_t3);
    				trasp_slider.add(Sl_t4);
    				
    				JPanel bottoniera = new JPanel(new FlowLayout());
    				bottoniera.add(spazio);
    				bottoniera.add(bc1);
    				bottoniera.add(bc2);
    				bottoniera.add(bc3);
    				bottoniera.add(bc4);
    				
    				//corpo.add(surf_label, BorderLayout.NORTH);
    				corpo.add(trasp_slider, BorderLayout.CENTER);
    				corpo.add(bottoniera, BorderLayout.SOUTH);
    				
    			
    			    			
			eastCenter.add(corpo, BorderLayout.CENTER);	    			
    		eastCenter.add(cappello, BorderLayout.NORTH);
    		
    	JPanel eastSud=new JPanel(new BorderLayout());
    	
    		JPanel prova = new JPanel(new BorderLayout());
    	
    			JPanel pan_axis=new JPanel(new  FlowLayout());
	    			pan_axis.add(hideAxis);
    			JPanel pan_mark=new JPanel(new FlowLayout());
    				pan_mark.add(hidemarks);
    			
    			JPanel pan_stats=new JPanel(new  FlowLayout());
    				pan_stats.add(stats);
    		
    		prova.add(pan_mark, BorderLayout.NORTH);
			prova.add(pan_axis, BorderLayout.CENTER);
			prova.add(pan_stats, BorderLayout.SOUTH);
			
		eastSud.add(prova, BorderLayout.SOUTH);
		    		
    east.add(eastNord, BorderLayout.NORTH);
    east.add(eastCenter, BorderLayout.CENTER);
    east.add(eastSud, BorderLayout.SOUTH);
    
    
	contenitore.add(east); //AKTUNG










	
//add ("East", east);


//pannello in basso



    bott_s0 = new JLabel (" ");
    bott_s0.setOpaque(true);
    bott_s0.setForeground(new Color((float)(255/255), (float)(153/255), (float)(255/255)));
    bott_s0.setBackground(new Color(.0f,.0f,.0f));
    meno.setFont(new Font("Courier", Font.PLAIN,  10));
    bott_s1 = new JLabel (" ");
    bott_s1.setOpaque(true);
    bott_s1.setForeground(new Color((float)(255/255), (float)(255/255), (float)(153/255)));
    bott_s1.setBackground(new Color(.0f,.0f,.0f));
    bott_s2 = new JLabel (" ");
    bott_s2.setOpaque(true);
    bott_s2.setBackground(new Color(.0f,.0f,.0f));
    bott_s2.setForeground(new Color((float)(153/255), (float)(255/255), (float)(255/255)));
	meno.setFont(new Font("Courier", Font.PLAIN,  10));
	
    bottom = new JPanel (new FlowLayout ());
    bottom.setOpaque(true);
    bottom.setBackground(new Color (.0f,.0f,.0f));
    bottom.add (bott_s0);
    bottom.add (bott_s1);
    bottom.add (bott_s2);
    bottom.setPreferredSize(new Dimension(canvas3D.getBounds().width, 25));
    
    JPanel sud= new JPanel(new BorderLayout());
    
    JPanel pad = new JPanel(new FlowLayout());
 		/*JPanel pad_center = new JPanel(new BorderLayout());
 
    		JPanel pan_stats=new JPanel(new  FlowLayout());
    				
    	pad_center.add(pan_stats, BorderLayout.NORTH);
    pad_center.add(new JLabel("a"), BorderLayout.CENTER);
    pad_center.add(new JLabel("b"), BorderLayout.SOUTH);
    
    pad.add(pad_center);*/
    pad.setPreferredSize(new Dimension(122, 25));
    
    
    pad.setOpaque(true);
    
   
    sud.add(bottom, BorderLayout.CENTER); 
    sud.add(pad, BorderLayout.EAST);
    
    
    add ("South", sud);



	//add ("East", east);AKTUNG
	add("East", contenitore);



    simpleU=new SimpleUniverse(canvas3D);
simpleU.getViewer().getView().setDepthBufferFreezeTransparent(false);
    // This will move the ViewPlatform back a bit so the
    // objects in the scene can be viewed.
    simpleU.getViewingPlatform().setNominalViewingTransform();
	if (narg==1)
	{
    	simpleU.addBranchGraph(scene);
    	scene.setCapability(BranchGroup.ALLOW_DETACH);
	}
	
	
    
	this.pack();
	this.setVisible(true);
  }

  public void init() 
  {

  }

/*
  public MeshViewer (int narg) throws IOException
  {
    createWindow(narg);
  } 

 */
  public void run (String args) 
  {
	 
	 try{
		 
		 //this.setPreferredSize(new Dimension(650, 750));
		 this.setPreferredSize(new Dimension(650, 730));
		 this.setTitle("Mesh_Viewer_MicroSCoBiOJ");
		 this.addWindowListener(this);
		 Mesh_Viewer_MicroSCoBiOJ.setSpectrum();
	 /* int narg=args.length;
	  
	  
    if (narg==1)
    {	tmp = new URL ("file", "", args[0]);
 
    
    
    BufferedReader Input = new BufferedReader(new InputStreamReader(tmp.openStream()));
   
    MeshRead (Input);
    
    Input.close ();
}*/
   // Frame frame = new MainFrame(new MeshViewer(narg), 700, 650);
   createWindow(0);
}catch(IOException e){IJ.showMessage("Error creating interface");}

   
   
  } 


  
  
  public static void setSpectrum () {
	int k = 0;
	
			final int bound1 = GAMUT / 6;
			final int bound2 = GAMUT / 3;
			final int bound3 = GAMUT / 2;
			final int bound4 = (2 * GAMUT) / 3;
			final int bound5 = (5 * GAMUT) / 6;
			final int bound6 = GAMUT;
			final float gamutChunk1 = (float)bound1;
			final float gamutChunk2 = (float)(bound2 - bound1);
			final float gamutChunk3 = (float)(bound3 - bound2);
			final float gamutChunk4 = (float)(bound4 - bound3);
			final float gamutChunk5 = (float)(bound5 - bound4);
			final float gamutChunk6 = (float)(bound6 - bound5);
			do {
				spectrum[Mesh_Viewer_MicroSCoBiOJ.stirColor(k)] = new Color(1.0F, (float)k
					/ gamutChunk1, 0.0F);
				
			} while (++k < bound1);
			do {
				spectrum[Mesh_Viewer_MicroSCoBiOJ.stirColor(k)] = new Color(1.0F - (float)(k - bound1)
					/ gamutChunk2, 1.0F, 0.0F);
				
			} while (++k < bound2);
			do {
				spectrum[Mesh_Viewer_MicroSCoBiOJ.stirColor(k)] = new Color(0.0F, 1.0F, (float)(k - bound2)
					/ gamutChunk3);
				
			} while (++k < bound3);
			do {
				spectrum[Mesh_Viewer_MicroSCoBiOJ.stirColor(k)] = new Color(0.0F, 1.0F - (float)(k - bound3)
					/ gamutChunk4, 1.0F);
				
			} while (++k < bound4);
			do {
				spectrum[Mesh_Viewer_MicroSCoBiOJ.stirColor(k)] = new Color((float)(k - bound4)
					/ gamutChunk5, 0.0F, 1.0F);
				
			} while (++k < bound5);
			do {
				spectrum[Mesh_Viewer_MicroSCoBiOJ.stirColor(k)] = new Color(1.0F, 0.0F, 1.0F - (float)(k - bound5)
					/ gamutChunk6);
				
			} while (++k < bound6);
			
	
	
} 

private static int stirColor (
	int color
) {
	if (color < 0) {
		return(-1);
	}
	int stirredColor = 0;
	for (int k = 0; (k < (int)Math.round(Math.log((double)GAMUT) / Math.log(2.0))); k++) {
		stirredColor <<= 1;
		stirredColor |= (color & 1);
		color >>= 1;
	}
	return(stirredColor);
}
  
 
}
