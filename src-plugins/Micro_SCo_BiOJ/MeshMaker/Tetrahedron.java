package MeshMaker;


import ij.measure.Calibration;
import ij.*;
import ij.process.ImageProcessor;
import java.util.*;

/*
	Classe che rappresenta i cubi sui quali applicare l'algoritmo marching cube
*/
	
public class Tetrahedron
{
	XYZ p0, p1, p2, p3;
	int v0, v1, v2, v3;
	XYZ[] vertlist;

	int ntri;						// Numero di triangoli della triangolazione del cubo	
	LinkedList tr_list=null;
	
	Tetrahedron( int val0, int val1, int val2, int val3, XYZ ver0, XYZ ver1, XYZ ver2, XYZ ver3)
	{
		ntri=0;
		p0=ver0;
		p1=ver1;
		p2=ver2;
		p3=ver3;
			
		v0=val0;
		v1=val1;
		v2=val2;
		v3=val3;
			
		
	}


	
	// Costruttore: acquisisce i vertici del cubo e i loro valori
	

	// Ritorna il valore del vertice dato in input
	int getValueVertex(int index)
	{
		if (index==0)
		{
			return v0;
		}
		
		if (index==1)
		{
			return v1;
		}
		
		if (index==2)
		{
			return v2;
		}
		
		if (index==3)
		{
			return v3;
		}
		return -1;
	}
	
	// Ritorna le coordinate cartesiane del vertice dato in input
	XYZ getPoint(int index)
	{
		if (index==0)
		{
			return p0;
		}
		
		if (index==1)
		{
			return p1;
		}
		
		if (index==2)
		{
			return p2;
		}
		
		if (index==3)
		{
			return p3;
		}
		return null;
	}
	
	// Aggiunge un triangolo alla triangolazione del cubo
	void add_Tri(XYZ p0, XYZ p1, XYZ p2)
	{
		tr_list=new LinkedList();
		Triangle t=new Triangle (p0, p1, p2);
		tr_list.add(t);	
		ntri++;
	}
	
	
};


