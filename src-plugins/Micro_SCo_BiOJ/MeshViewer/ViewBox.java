package MeshViewer;


import javax.media.j3d.*;
import javax.vecmath.*;
import java.lang.*;

class ViewBox extends Shape3D
{
  public float size;
  public Point3f LowC;
  
  public ViewBox (Point3f lc, float s)
  {
    this.size = s;
    this.LowC = lc;
    this.setGeometry(createGeometry(lc, s));
  }
  
  public Geometry createGeometry (Point3f c, float s)
  {
    IndexedLineArray Lines = new IndexedLineArray (8, GeometryArray.COORDINATES |
    						      GeometryArray.COLOR_3, 24);

    Lines.setCoordinate (0, c);
    Lines.setCoordinate (1, new Point3f (c.x, c.y, c.z + s));
    Lines.setCoordinate (2, new Point3f (c.x + s, c.y, c.z + s));
    Lines.setCoordinate (3, new Point3f (c.x + s, c.y, c.z));
    Lines.setCoordinate (4, new Point3f (c.x, c.y + s, c.z));
    Lines.setCoordinate (5, new Point3f (c.x, c.y + s, c.z + s));
    Lines.setCoordinate (6, new Point3f (c.x + s, c.y + s, c.z + s));
    Lines.setCoordinate (7, new Point3f (c.x + s, c.y + s, c.z));
    
    Color3f green = new Color3f(0, 1, 0);
    for (int i = 0; i < 8; ++i) Lines.setColor (i, green);
    
    Lines.setCoordinateIndex (0, 0);
    Lines.setCoordinateIndex (1, 1);
    Lines.setCoordinateIndex (2, 1);
    Lines.setCoordinateIndex (3, 2);
    Lines.setCoordinateIndex (4, 2);
    Lines.setCoordinateIndex (5, 3);
    Lines.setCoordinateIndex (6, 3);
    Lines.setCoordinateIndex (7, 0);
    Lines.setCoordinateIndex (8, 0);
    Lines.setCoordinateIndex (9, 4);
    Lines.setCoordinateIndex (10, 1);
    Lines.setCoordinateIndex (11, 5);
    Lines.setCoordinateIndex (12, 2);
    Lines.setCoordinateIndex (13, 6);
    Lines.setCoordinateIndex (14, 3);
    Lines.setCoordinateIndex (15,7);
    Lines.setCoordinateIndex (16, 4);
    Lines.setCoordinateIndex (17, 5);
    Lines.setCoordinateIndex (18, 5);
    Lines.setCoordinateIndex (19, 6);
    Lines.setCoordinateIndex (20, 6);
    Lines.setCoordinateIndex (21, 7);
    Lines.setCoordinateIndex (22, 7);
    Lines.setCoordinateIndex (23, 4) ;
    
    return Lines;
  }
}
