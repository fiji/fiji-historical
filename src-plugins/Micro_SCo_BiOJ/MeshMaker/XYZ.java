package MeshMaker;

/*
	Classe relativa ad oggetti rappresentanti punti cartesiani nello spazio
*/

import java.*;

class XYZ
{
	double x, y, z;		// Coordinate del punto
	
	XYZ(XYZ p)
	{
		x=p.getX();
		y=p.getY();
		z=p.getZ();
	}
	
	
	
	// Costruttore: crea il punto corrispondente alla terna fornita in input	
	
	
	
	XYZ(double a, double b, double c)
	{
		x=a;
		y=b;
		z=c;
	}
	
	// Costruttore vuoto
	XYZ(){x=0;y=0; z=0;}
	
	// Restituisce la coordinata x del punto
	double getX() {	return this.x; }
	
	// Restituisce la coordinata y del punto
	double getY() {	return this.y; }
	
	// Restituisce la coordinata z del punto
	double getZ() { return this.z; }
	
	// Setta la coordinata x del punto al valore specificato
	void setX(double v)	{ this.x=v;	}
	
	// Setta la coordinata y del punto al valore specificato
	void setY(double v)	{ this.y=v;	}
	
	// Setta la coordinata z del punto al valore specificato
	void setZ(double v)	{ this.z=v;	}
	
	// restituisce true se i due punti in input sono differenti altrimenti restituisce false
	boolean different(XYZ p2)
	{
		if (this.getX()!=p2.getX() || this.getY()!=p2.getY() || this.getZ()!=p2.getZ())
			return true;
		return false;
		
		
		
	}
	
	static boolean equal(XYZ p1, XYZ p2)
	{
		if ((p1.getX()==p2.getX()) && (p1.getY()==p2.getY()) && (p1.getZ()==p2.getZ()))
			return true;
		else
			return false; 
		
	}
	
	
	static XYZ appVector(XYZ p1, XYZ p2)
	{
		
		return new XYZ(p2.getX()-p1.getX(), p2.getY()-p1.getY(), p2.getZ()-p1.getZ());
	}
	
	static XYZ crossProduct(XYZ v, XYZ w){
		double compX= (v.getY()*w.getZ())-(v.getZ()*w.getY());
		double compY= (v.getZ()*w.getX())-(v.getX()*w.getZ());
		double compZ= (v.getX()*w.getY())-(v.getY()*w.getX());
		
		return new XYZ(compX, compY, compZ);		
	}
	
	static double magnitude(XYZ v){
		
		return Math.sqrt(v.getX()*v.getX() + v.getY()*v.getY() + v.getZ()*v.getZ());
		
	}
	
		
	static XYZ midPoint(XYZ p1, XYZ p2)
	{
		double xx=(p1.getX()+p2.getX())/2;
		double yy=(p1.getY()+p2.getY())/2;
		double zz=(p1.getZ()+p2.getZ())/2;
		return new XYZ(xx, yy, zz);
		

		
	}
};