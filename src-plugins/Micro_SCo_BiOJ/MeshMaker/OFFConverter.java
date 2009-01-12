package MeshMaker;

import java.io.*;
import java.*;
import java.util.*;
import javax.swing.*;
import ij.*;
import java.awt.*;

public class OFFConverter {
	
	OFFConverter (int nver, int ntri, ArrayList lv, ArrayList lt, double area, ArrayList points, ArrayList colors, double dx, double dy, double dz, String ordine)
	{
		try{
			int i,j;
			JFileChooser fc = new JFileChooser((new File("OFFConverter.class")).getPath());
			MyFileFilter filter = new MyFileFilter();
    		filter.addExtension("off");
    		filter.setDescription("off triangulation");
    		fc.setFileFilter(filter);
			int returnVal = fc.showSaveDialog(new JPanel());
            if (returnVal == JFileChooser.APPROVE_OPTION) {
               
	            String name = fc.getSelectedFile().getAbsolutePath();
	            
				FileWriter fw=new FileWriter(name+".off", false);
				
				fw.write("OFF\n");
				fw.write(nver + " " + (ntri) + " 0\n");
				for(i=0;i<nver;i++)
			{
				fw.write(String.valueOf(((XYZ)lv.get(i)).getX()) + " ");
				fw.write(String.valueOf(((XYZ)lv.get(i)).getY()) + " ");
				fw.write(String.valueOf(((XYZ)lv.get(i)).getZ()) + "\n");
			}
			fw.flush();
			j=0;
			
			
			while (j<lt.size())
			{
				
				fw.write("3 " +((Integer)lt.get(j)).toString()+ " " + ((Integer)lt.get(j+1)).toString() + " " + ((Integer)lt.get(j+2)).toString() + "\n");
				j=j+3;
				fw.flush();
			}
			fw.write(String.valueOf(area+" "));
			fw.write(ordine+"\n");
			fw.flush();
			// scrivo i marker
			Point p;
			String n;
			String x;
			String y;
			String z;
			String c;
			Vector v1;
			Vector v2;
			int k, s;
			fw.write(""+points.size()+"\n");
			fw.flush();
			for (k=0; k<points.size(); k++)
			{
				v1= (Vector)points.get(k);
				v2= (Vector)colors.get(k);
				fw.write(""+v1.size()+"\n");
				for (s=0;s<v1.size(); s++)
				{
					
					
					x=String.valueOf((double)((Point)v1.get(s)).getX()*dx);
					y=String.valueOf((double)((Point)v1.get(s)).getY()*dy);
					z=String.valueOf((double)k*dz);
					c=String.valueOf(((Integer)v2.get(s)).intValue());
					
					fw.write(x + " " + y + " " + z + " "  + c +" \n");
					fw.flush();
					
				}
				
			}
			
			//fine scrittura marker
			
			fw.close();
			
		}}
		catch(Exception e){System.out.println(e.getMessage());}
	
	}
};
