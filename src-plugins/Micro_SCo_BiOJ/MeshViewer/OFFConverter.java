package MeshViewer;

import ij.IJ;

import java.io.*;
import java.*;
import java.util.*;


public class OFFConverter {

	public static void main(String[] args)
	{
		LinkedList lv =new LinkedList();
		
		int ntri=0;
		int nver=0;
		
		String file_ver=new String(args[0]);
		String file_tri=new String(args[1]);
		
		try{
		
		FileReader fr=new FileReader(file_ver);
		BufferedReader br=new BufferedReader(fr);
		
		String dato=null;
		
		
		while((dato=br.readLine())!=null)
		{
			lv.add(dato);
			nver++;
		}
		
		fr.close();
		}catch (Exception e){IJ.showMessage("Error saving file: " + e.getMessage());}
		
		
		LinkedList lt=new LinkedList();
		try{
		
		FileReader fr=new FileReader(file_tri);
		BufferedReader br=new BufferedReader(fr);
		
		String dato=null;
		
		while((dato=br.readLine())!=null)
		{
			lt.add(dato);
			ntri++;
		}
		
		
		
		fr.close();
		
		}catch (Exception e){IJ.showMessage("Error saving file: " + e.getMessage());}
		
		
		OFFConverter.scrivifileoff(nver, ntri, lv, lt);
		
		
	
	}

	static void scrivifileoff(int nver, int ntri, LinkedList lv, LinkedList lt)
	{
		try{
			int i,j;
			j=0;
			FileWriter fw=new FileWriter("risposta.off", true);
			
			fw.write("OFF\n");
			fw.write(nver + " " + (ntri/3) + " 0\n");
			for(i=0;i<nver;i++)
				fw.write((String)(lv.get(i))+"\n");
			fw.flush();
			while (j<ntri)
			{
				
				fw.write("3 " +(String)(lt.get(j))+ " " + (String)(lt.get(j+1)) + " " + (String)(lt.get(j+2)) + "\n");
				j=j+3;
				fw.flush();
			}
			fw.close();
		}
		catch(Exception e){IJ.showMessage("Error saving file: " + e.getMessage());}
		
		
		
		
	
	}
	
	

};
