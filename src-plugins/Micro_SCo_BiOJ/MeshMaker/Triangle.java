package MeshMaker;

import ij.measure.Calibration;
import ij.*;
import ij.process.ImageProcessor;

/*
	Classe che implementa l'oggetto triangolo
*/

class Triangle
{
	XYZ[] p=null;	// Array che contiene le coordinate dei tre vertici che formano il triangolo
	
	// Costruttore: crea il triangolo relativo ai tre punti dati in input
	Triangle(XYZ p0, XYZ p1, XYZ p2)
	{
		p=new XYZ[3];
		p[0]=p0;
		p[1]=p1;
		p[2]=p2;
	}


};