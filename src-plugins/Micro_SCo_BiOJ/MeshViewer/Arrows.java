package MeshViewer;

import javax.media.j3d.*;
import javax.vecmath.*;

public class Arrows extends Shape3D
{
  public Arrows ()
  {
    this.setGeometry(createGeometry());
    this.setAppearance(createAppearance());
  }
  
  private Geometry createGeometry ()
  {
    IndexedLineArray Lines = new IndexedLineArray (6, GeometryArray.COORDINATES |
    						      GeometryArray.COLOR_3, 6);

    Lines.setCoordinate (0, new Point3f (-.9f, 0f, 0f));
    Lines.setCoordinate (1, new Point3f (.9f, 0f, 0f));
    Lines.setCoordinate (2, new Point3f (0f, -.9f, 0f));
    Lines.setCoordinate (3, new Point3f (0f, .9f, 0f));
    Lines.setCoordinate (4, new Point3f (0f, 0f, -.9f));
    Lines.setCoordinate (5, new Point3f (0f, 0f, .9f));

    Color3f blue = new Color3f(0, 0, 1);
    for (int i = 0; i < 6; ++i) Lines.setColor (i, blue);

    Lines.setCoordinateIndex (0, 0);
    Lines.setCoordinateIndex (1, 1);
    Lines.setCoordinateIndex (2, 2);
    Lines.setCoordinateIndex (3, 3);
    Lines.setCoordinateIndex (4, 4);
    Lines.setCoordinateIndex (5, 5);
    
    return Lines;
  }
  
  private Appearance createAppearance ()
  {
    Appearance boxAppear = new Appearance();
    PolygonAttributes polyAttrib = new PolygonAttributes();

    polyAttrib.setPolygonMode (PolygonAttributes.POLYGON_LINE);
    boxAppear.setPolygonAttributes(polyAttrib);

    return boxAppear;
  }
}
