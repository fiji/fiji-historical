package MeshMaker;


import ij.measure.Calibration;
import ij.*;
import ij.process.ImageProcessor;
import java.util.*;

/*
	Classe che rappresenta i cubi sui quali applicare l'algoritmo marching cube
*/

class Cube
{
	int[] v=null;					// Array dei valori dei vertici del cubo
	XYZ[] p=null;					// Lista dei vertici del cubo
	//Triangle[] tr_list=null;
	LinkedList tr_list=null;		// Lista linkata che rappresenta la triangolazione del cubo
	int ntri;						// Numero di triangoli della triangolazione del cubo
	
	// Costruttore: acquisisce i vertici del cubo e i loro valori
	Cube(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, XYZ v0, XYZ v1, XYZ v2, XYZ v3, XYZ v4,
		XYZ v5, XYZ v6, XYZ v7) 
	{
		ntri=0;
		
		p=new XYZ[8];
		p[0]=v0;
		p[1]=v1;
		p[2]=v2;
		p[3]=v3;
		p[4]=v4;
		p[5]=v5;
		p[6]=v6;
		p[7]=v7;
		
		v=new int[8];
		v[0]=p0;
		v[1]=p1;
		v[2]=p2;
		v[3]=p3;
		v[4]=p4;
		v[5]=p5;
		v[6]=p6;
		v[7]=p7;
	}

	// Ritorna il valore del vertice dato in input
	int getValueVertex(int index)
	{
		return v[index];
	}
	
	// Ritorna le coordinate cartesiane del vertice dato in input
	XYZ getPoint(int index)
	{
		return p[index];	
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


