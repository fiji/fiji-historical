package MeshViewer;

import javax.media.j3d.*;
import javax.vecmath.*;
import ij.*;

public class Axis extends Shape3D
{
	char dimension;
	Appearance boxAppear;
  public Axis (char dimension)
  {
	this.dimension=dimension;
    this.setGeometry(createGeometry());
    this.setAppearance(createAppearance());
    
  }
  
  private Geometry createGeometry ()
  {
    IndexedLineArray Lines = new IndexedLineArray (2, GeometryArray.COORDINATES |
    						      GeometryArray.COLOR_3, 2);

    
    Color3f colore=null;
    if (dimension=='x')
    {
	    
	    Lines.setCoordinate (0, new Point3f (-.9f, 0f, 0f));
	    
	    Lines.setCoordinate (1, new Point3f (.9f, 0f, 0f));
	    

    	colore = new Color3f(255/255, 153/255, 255/255);
    	
	}
    else if (dimension=='y')
    	 {
	    	 Lines.setCoordinate (0, new Point3f (0f, -.9f, 0f));
    		 Lines.setCoordinate (1, new Point3f (0f, .9f, 0f));
    		 
    		 colore = new Color3f(255/255, 255/255, 153/255);
	 	 }
    	 else if (dimension=='z')
    	 {
	    	 

		    Lines.setCoordinate (0, new Point3f (0f, 0f, -.9f));
    		Lines.setCoordinate (1, new Point3f (0f, 0f, .9f));

    	 	colore = new Color3f(153/255, 255/255, 255/255);
	 	 }
	 	 
    	
    	
    for (int i = 0; i < 2; ++i) Lines.setColor (i, colore);

    Lines.setCoordinateIndex (0, 0);
    Lines.setCoordinateIndex (1, 1);
   
    
    return Lines;
  }
  
  private Appearance createAppearance ()
  {
    boxAppear = new Appearance();
    boxAppear.setCapability(Appearance.ALLOW_TRANSPARENCY_ATTRIBUTES_READ);
	boxAppear.setCapability(Appearance.ALLOW_TRANSPARENCY_ATTRIBUTES_WRITE);
    PolygonAttributes polyAttrib = new PolygonAttributes();
    

   // polyAttrib.setPolygonMode (PolygonAttributes.POLYGON_LINE);
    boxAppear.setPolygonAttributes(polyAttrib);

    return boxAppear;
  }
  public void viewAxis(boolean b)
  {
	  	try{
		if (!b)
			(this.boxAppear).setTransparencyAttributes(new TransparencyAttributes(TransparencyAttributes.NICEST, 1.0f));
    	else
	     	(this.boxAppear).setTransparencyAttributes(new TransparencyAttributes(TransparencyAttributes.NICEST, .0f));
     	}
	    catch(Exception e){IJ.showMessage("Errore nel hideaxis " + e.getMessage());}
		    	  
	  
  }
}
