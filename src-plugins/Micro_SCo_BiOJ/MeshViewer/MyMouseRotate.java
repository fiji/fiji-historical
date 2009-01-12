package MeshViewer;

import com.sun.j3d.utils.behaviors.mouse.*;
import javax.vecmath.Matrix3f;
import javax.media.j3d.Transform3D;
import javax.vecmath.Vector3d;


import java.lang.Math;
import javax.swing.*;
import java.awt.Color;

import ij.*;




public class MyMouseRotate extends MouseRotate
{
	public JLabel gradiX, gradiY, gradiZ;
		
	public MyMouseRotate(JLabel lab1, JLabel lab2, JLabel lab3)
	{
		super();
		gradiX= lab1;
		gradiY= lab2;
		gradiZ= lab3;
		(this.gradiX).setText("X: " + "0°"+"     ");
		(this.gradiY).setText("Y: " + "0°"+"     ");
		(this.gradiZ).setText("Z: " + "0°"+"     ");
	
	}	
	
	public void transformChanged(Transform3D t) 
	{
		
		//metodo 1
		/*
		double angolo_xy=0;
		double angolo_y=0;
		double angolo_z=0;
		double angolo_yx=0;
		double angolo_y2=0;
		double angolo_z2=0;
		
		
		int gradi_x=0;
		int gradi_y=0;
		int gradi_z=0;
		
		
		Matrix3f mat = new Matrix3f();
		t.get(mat);
		
		double coseno_x=mat.getElement(1,2);
		double coseno_y=mat.getElement(0,2);
		double coseno_z=mat.getElement(0,1);
		
		
		double coseno_x2=mat.getElement(2,1);
		double coseno_y2=mat.getElement(2,0);
		double coseno_z2=mat.getElement(1,0);
		
		angolo_xy=Math.asin((mat.getElement(1,2))*-1);
		//angolo_yx=Math.asin((mat.getElement(2,1))*-1);
		
		
		
		
		//if (angolo_x>=0)
		//{
			
		if (angolo_xy>0)
		{
			if (coseno_x2>=0)	
				gradi_x = Math.round(Math.round(Math.toDegrees(angolo_xy)));
			else
			{
				gradi_x = 180 - Math.round(Math.round(Math.toDegrees(angolo_xy)));
				
			}
		}	
		//}
		//else
		//{
		//		gradi_x = Math.round(Math.round(Math.toDegrees(angolo_x)))*-1;
			
			
		//}
		
		
		
		
		
		
		
		
		
		
		angolo_y=Math.acos(mat.getElement(0,2));
		if (angolo_y>=0)
			gradi_y = Math.round(Math.round(Math.toDegrees(angolo_y)))-90;
		else
			gradi_y = Math.round(Math.round(Math.toDegrees(angolo_y)))*-1;
		
		angolo_z=Math.acos(mat.getElement(0,1));
		if (angolo_z>=0)
			gradi_z = Math.round(Math.round(Math.toDegrees(angolo_z)))-90;
		else
			gradi_z = Math.round(Math.round(Math.toDegrees(angolo_z)))*-1;
		
		
	
		String aux = new String(" X: " +gradi_x + " coseno_x2:" + coseno_x2 + " coseno_x:" + coseno_x);
		*/
		//fine metodo 1
		int rotX, rotY, rotZ;
		Matrix3f mat = new Matrix3f();
		double coseno_x=-1;
		t.get(mat);
		rotX=MyMouseRotate.getGradX(mat, coseno_x);
		rotY=MyMouseRotate.getGradY(mat);
		rotZ=MyMouseRotate.getGradZ(mat);
		coseno_x = mat.getElement(2,0);
		
		
		gradiX.setText(" X: " + rotX+ "°     ");
		gradiX.repaint();
		
		gradiY.setText(" Y: " + rotY+ "°     ");
		gradiY.repaint();
		
		gradiZ.setText(" Z: " + rotZ+"°");
		//gradiZ.setForeground(new Color(153/255, 255/255, 255/255));
		gradiZ.repaint();
		
		
		
	}
	
	static private int getGradZ(Matrix3f mat)
	{
		Vector3d v1=new Vector3d(.0d,.0d,1.0d);
		Vector3d v2=new Vector3d((double)mat.getElement(0,2),(double)mat.getElement(1,2),(double)mat.getElement(2,2));
		
		double angle_rad = v1.angle(v2);
		double coseno_z=mat.getElement(1,2);
		int angle_grad =0;
		
		if (coseno_z>0)
			angle_grad = 360 - Math.round(Math.round(Math.toDegrees(angle_rad)));
		else
			angle_grad = Math.round(Math.round(Math.toDegrees(angle_rad)));
		return angle_grad;
	}
	
	static private int getGradY(Matrix3f mat)
	{
		Vector3d v1=new Vector3d(.0d,1.0d,.0d);
		Vector3d v2=new Vector3d((double)mat.getElement(0,1),(double)mat.getElement(1,1),(double)mat.getElement(2,1));
		
		double angle_rad = v1.angle(v2);
		double coseno_y=mat.getElement(2,1);
		int angle_grad =0;
		
		if (coseno_y>0)
			angle_grad = 360 - Math.round(Math.round(Math.toDegrees(angle_rad)));
		else
			angle_grad = Math.round(Math.round(Math.toDegrees(angle_rad)));
		return angle_grad;
	}
	
	static private int getGradX(Matrix3f mat, double aux)
	{
		Vector3d v1=new Vector3d(1.0d,.0d,.0d);
		Vector3d v2=new Vector3d((double)mat.getElement(0,0),(double)mat.getElement(1,0),(double)mat.getElement(2,0));
		
		double angle_rad = v1.angle(v2);
		double coseno_x=mat.getElement(0,2);
		int angle_grad =0;
		
		if (coseno_x>0)
			angle_grad = 360 - Math.round(Math.round(Math.toDegrees(angle_rad)));
		else
			angle_grad = Math.round(Math.round(Math.toDegrees(angle_rad)));
			
		aux=coseno_x;
		return angle_grad;
	}
	
	
	public JLabel getLabel(int i)
	{
		if (i==0)
			return this.gradiX;
		if (i==1)
			return this.gradiY;
		else
			return this.gradiZ;	
		
	}
	
	
	
}