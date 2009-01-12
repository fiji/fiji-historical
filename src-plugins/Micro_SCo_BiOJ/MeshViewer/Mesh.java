package MeshViewer;

import javax.media.j3d.*;
import javax.vecmath.*;
import java.io.*;
import com.sun.j3d.utils.geometry.*;

public class Mesh extends Shape3D 
{
  private   BufferedReader in = null;
  private  int NumVerts, NumTri;
  private  double area=-1;
  
  public  int getNV () { return NumVerts; }
  public  int getNT () { return NumTri; }
  public double getArea() {return area;}
  public BufferedReader getIn() {return in;}
  

  public  void setNV (int x) { NumVerts=x; }
  public  void setNT (int x) { NumTri=x; }
  public  void setArea (double a) { area=a; }    
  public  void setIn(BufferedReader i) { in=i; }
  public Mesh()
  {}
  
  public Mesh(int NumV, int NumT, float [] v, int [] t, double ar) throws IOException 
  {
    NumVerts = NumV;
    NumTri = NumT;
    area=ar;
    this.setGeometry(createGeometry(v, t));
    this.setAppearance(createAppearance());
  }
  
  private static float [] TriangleNormal (float [] p1, float [] p2, float [] p3)
  {
    float [] res = new float[3];
    
    float [] a = new float[3];
    float [] b = new float[3];
  
    a[0] = p1[0] - p2[0];  
    a[1] = p1[1] - p2[1]; 
    a[2] = p1[2] - p2[2]; 
    
    b[0] = p1[0] - p3[0]; 
    b[1] = p1[1] - p3[1]; 
    b[2] = p1[2] - p3[2]; 
    
    res[0] = a[1]*b[2] - a[2]*b[1];
    res[1] = a[2]*b[0] - a[0]*b[2];
    res[2] = a[0]*b[1] - a[1]*b[0];
    
    return res;
  }  

  public static Geometry createGeometry(float [] v, int [] t) throws IOException
  {
    GeometryInfo gi = new GeometryInfo (GeometryInfo.TRIANGLE_ARRAY);
    gi.setUseCoordIndexOnly(true);
   
    gi.setCoordinates (v);
    gi.setCoordinateIndices (t);
    
    gi.convertToIndexedTriangles();
    
    NormalGenerator ng = new NormalGenerator();
    ng.generateNormals(gi);

    return ((IndexedTriangleArray) gi.getIndexedGeometryArray());
  }

  Appearance createAppearance()
  {
    return (new Appearance());
  }
} // end of class Mesh
