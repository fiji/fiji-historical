package MeshViewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import javax.media.j3d.Appearance;
import javax.media.j3d.Material;
import javax.vecmath.Color3f;
import ij.*;
import javax.vecmath.Tuple3f;


public class InfoWindow extends JFrame implements WindowListener{
	private boolean iw_active;
	JPanel pannello = new JPanel(new FlowLayout());
	ArrayList labels = new ArrayList();
	ArrayList appears=new ArrayList();
	ArrayList etichetts = new ArrayList();
	
	
	public InfoWindow()
	{	iw_active = false;
		}
	
	public InfoWindow(ArrayList etichette, ArrayList appearances)	
	{
		
		updateIW(etichette, appearances);
		
		//this.setPreferredSize(new Dimension(500, 230));
	 	this.setTitle("Informations");
		this.addWindowListener(this);
		
			
		this.setBackground(new Color(1.0f,1.0f,1.0f));
		//this.add(pannello);
		this.pack();
		this.setVisible(true);
	}
	
	public boolean isActive()
	{return iw_active;}	
	
	public void updateIW(int i, Color c)
	{
		//((JTextArea)labels.get(i)).setForeground(c);
		//updateIW(etichetts
		
	}
	
	public void updateIW(ArrayList etichette, ArrayList appearances)
	{
		JTextArea l=null;
		Color3f c3f=new Color3f(.0f, .0f, .0f);
		appears = appearances;
		etichetts = etichette;
		this.remove(pannello);	
		this.repaint();
		 pannello = new JPanel(new FlowLayout());
		labels = new ArrayList();
		
		Color c=null;
		
		//for (int i=0; i < appearances.size(); i++)
		for (int i=0; i < 4; i++)
		{
			
			
			if (i<appearances.size())
		
				(((Appearance)appearances.get(i)).getMaterial()).getDiffuseColor(c3f);
		
		
			else
				c3f = new Color3f(.5f,.5f,.5f);
			
			
			if (c3f.x<.0f)
			{
				
				c3f.x=.0f;
			}
			if (c3f.y<.0f)
			{
				
				c3f.y=.0f;
			}	
			if (c3f.z<.0f)
			{
				
				c3f.z=.0f;
			}
			c = c3f.get();
			
			l = new JTextArea((String)etichette.get(i));
			
			
			l.setBackground(pannello.getBackground());
			
			l.setFont(new Font("Arial", Font.BOLD,  11));
			l.setEditable(false);
			
			
			l.setForeground(new Color(c.getRed(), c.getGreen(), c.getBlue()));
			
			labels.add(l);
				
			pannello.add((JTextArea)labels.get(i));
			
			
		}	
		
		this.add(pannello);
		pannello.repaint();
		
		
		/*for (int i=0; i < appearances.size(); i++)
		{
			((JTextArea)labels.get(i)).repaint();
			
		}	*/
		
		//this.setBackground(new Color(.0f,.0f,.0f));
		this.repaint();
		this.setVisible(false);
		this.setVisible(true);
		
		
		}
		


  public void 	windowActivated(WindowEvent e){this.repaint();}
        
 public void 	windowClosed(WindowEvent e)
          {}
 public void 	windowClosing(WindowEvent e)
          {
	     	iw_active=false;
	                 
	          
	          }
 public void 	windowDeactivated(WindowEvent e)
          {}
 public void 	windowDeiconified(WindowEvent e)
          {}
 public void 	windowIconified(WindowEvent e)
          {}
 public void 	windowOpened(WindowEvent e) 
  		{iw_active=true;}
  
		
		
		
		
		
	
	
	
	
	
	
	
	
	
	
}