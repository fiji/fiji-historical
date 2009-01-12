package MeshViewer;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.colorchooser.*;


public class MyColorChooser extends JPanel  {
	JColorChooser tcc=null;
	JPanel preview=null;
    public MyColorChooser() {
        super(new BorderLayout());
        preview=new JPanel();
        
        preview.setBackground(Color.red);
        preview.add(new JButton("ci sono"));
        JLabel banner = new JLabel("Welcome to the Tutorial Zone!",
                            JLabel.CENTER);
        banner.setForeground(Color.yellow);
       
        tcc = new JColorChooser(banner.getForeground());
       	tcc.setPreviewPanel(preview);
       	
       	
        add(tcc, BorderLayout.PAGE_START);
        add(preview, BorderLayout.PAGE_END);
        JFrame frame = new JFrame("ColorChooser");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.add(this);
		
        //Display the window.
        frame.pack();
        frame.setVisible(true);
        
   } 
      public void stateChanged(ChangeEvent e) {
        Color newColor = tcc.getColor();
        preview.setBackground(newColor);
    }  
    }