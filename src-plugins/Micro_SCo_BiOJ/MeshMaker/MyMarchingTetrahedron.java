package MeshMaker;

import ij.measure.Calibration;
import ij.*;
import ij.process.ImageProcessor;
import java.io.*;
import java.*;
import java.util.*;
import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Panel;
import java.awt.Button;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent; 
import java.awt.Point;

/*
	Classe che implementa l'algoritmo marching cubes creando la triangolazione dell'isosuperficie
*/





public class MyMarchingTetrahedron implements ActionListener
{
	XYZ p0, p1, p2, p3;
	int v0, v1, v2, v3;
	String ordine=null;
	ArrayList ts;	
	double area=0;
	
	
	ArrayList Points;
	ArrayList Colors;
	double volumemin=0;
	double volumemax=0;
	JProgressBar progressBar;
	
	Tetrahedron a0=null;
	Tetrahedron a1=null;
	Tetrahedron a2=null;
	Tetrahedron a3=null;
	Tetrahedron a4=null;
	Tetrahedron a5=null;
	
	int isovalue;		      // isovalore scelto per l'estrazione della superficie
	XYZ [] vertlist=null;	  // array dei punti in cui la superficie interseca il cubo
	InputStack vol;			  // oggetto contenete le informazioni dello stack
	boolean stop=true;
	int out_ntriangoli=0;
	int out_nvertici=0;   // numero di triangoli e vertici
	//XYZ [] out_vertex=null;				// vertici della triangolazione
	//int [] out_triangle=null;			// lista di triangoli della triangolazione indicizzata su out_vertex
	ArrayList out_vertex=new ArrayList();
	ArrayList out_triangle=new ArrayList();
	
	JFrame frame=null;
	
	String triangoli=new String("triangoli.txt");	// file in cui scrivo l'output (triangolazione)

	
	//parametri della mainProgram
	int is;
	ImagePlus imp;
	ImageProcessor ip;
	double dx;
	double dy;
	double dz;
	
	static final int[] edgetable={
	0x0,  0xd,  0x13, 0x1e,
	0x26, 0x2b, 0x35, 0x38,
	0x38, 0x35,	0x2b, 0x26,
	0x1e, 0x13, 0xd, 0x0			  };

// tabella dei triangoli, indica come triangolare attraverso l'indice in vertlist,
// ogni riga della matrice corrisponde ad una situazione del cubo (cioè una delle possibili configurazioni 
// indicate da edgeTable) perciò ha 256 righe
// ogni colonna si riferisce ad un indice di vertlist

/*
static final int[][] triTable =
{	{-1,-1,-1,-1,-1,-1, -1},
	{0,3,2,-1,-1,-1,-1},
	{1,4,0,-1,-1,-1,-1},
	{2,3,4,4,1,2,-1},
	{2,5,1,-1,-1,-1,-1},
	{0,5,3,0,1,5,-1},
	{2,5,4,2,4,0,-1},
	{3,5,4,-1,-1,-1,-1},
	{5,3,4,-1,-1,-1,-1},
	{5,2,4,4,2,0,-1},
	{5,0,3,1,0,5,-1},
	{5,2,1,-1,-1,-1,-1},
	{4,3,2,1,4,2,-1},
	{1,0,4,-1,-1,-1,-1},
	{3,0,2,-1,-1,-1,-1},
	{-1,-1,-1,-1,-1,-1,-1}			};
	
	*/
	
	static final int[][] triTable =
{	{-1,-1,-1,-1,-1,-1, -1},
	{0,3,2,-1,-1,-1,-1},
	{1,4,0,-1,-1,-1,-1},
	{2,4,3,4,2,1,-1},//modificata ma sembra andare bene
	{2,5,1,-1,-1,-1,-1},
	{5,0,3,1,0,5,-1}, //modificata ma sembra andare bene
	{2,5,4,2,4,0,-1},
	{3,5,4,-1,-1,-1,-1},
	{5,3,4,-1,-1,-1,-1},
	{5,2,4,4,2,0,-1},
	{5,3,0,0,1,5,-1}, //modificata ma sembra andare bene
	{5,2,1,-1,-1,-1,-1},
	{4,2,3,1,2,4,-1}, //modificata ma sembra andare bene
	{1,0,4,-1,-1,-1,-1},
	{3,0,2,-1,-1,-1,-1},
	{-1,-1,-1,-1,-1,-1,-1}			};
	

	
	public void actionPerformed(ActionEvent evt) {
		stop = false;
		
		
	}
	
	public MyMarchingTetrahedron(int is, ImagePlus imp, ImageProcessor ip, double dx, double dy, double dz, ArrayList points, ArrayList colors, String ordine)
	{
	 this.is=is;
	 this.imp=imp;
	 this.ip=ip;
	 this.dx=dx;
	 this.dy=dy;
	 this.dz=dz;
	 this.ordine=ordine;
	 //this.Points=(ArrayList)Points.clone();
	 this.Points= new ArrayList();
	
	for (int s = 0; (s < points.size()); s++) {
				Vector listPoints = (Vector)points.get(s);
				
				
				
				
				this.Points.add(listPoints);
				
				
				
				}
	this.Colors= new ArrayList();
	
	for (int s = 0; (s < colors.size()); s++) {
				Vector listColors = (Vector)colors.get(s);
			
				this.Colors.add(listColors);
				
				}
	
	 
	 boolean b = mainProgram(is,imp,ip,dx,dy,dz);
		frame.dispose();
		if (b)
		{
			
			
			new	OFFConverter(this.out_nvertici, this.out_ntriangoli, this.out_vertex, this.out_triangle, this.area, this.Points, this.Colors, this.dx, this.dy, this.dz, this.ordine);		
						
	
			
			
		}
	 
	 
	 
		
	}
	
	// Ritorna il numero di triangoli della triangolazione
	int getNtri()
	{
		return out_ntriangoli;	
	}
	
	// Ritorna il numero di vertici della triangolazione
	int getNver()
	{
		return out_nvertici;	
	}
	
	// Ritorna il vettore dei vertici della triangolazione
	List getVertex()
	{
		return out_vertex;	
	}
	
	// Ritorna il vettore dei triangoli della triangolazione indicizzata su out_vertex
	List getTriangle()
	{
		return out_triangle;
	}
	
	// Costruttore della classe che dato l'isovalore, l'immagine e l'imageprocessor associato ad essa produce
	// la triangolazione dell'isosuperficie
	boolean mainProgram(int is, ImagePlus imp, ImageProcessor ip, double dx, double dy, double dz)
	{
	
		isovalue=(int)is;
		
		
	
		
		frame = new JFrame("MicroscoBio 3D Rec");
        

        //Create and set up the content pane.
        JComponent newContentPane = new JPanel();
        
        //Create the demo's UI.
        Button stopButton = new Button("Stop");
        stopButton.setActionCommand("stop");
        stopButton.addActionListener(this);
        //startButton.setActionCommand("start");
        //startButton.addActionListener(newContentPane);

        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);

        //JTextArea taskOutput = new JTextArea(5, 20);
        //taskOutput.setMargin(new Insets(5,5,5,5));
        //taskOutput.setEditable(false);

        JPanel panel = new JPanel();
        panel.add(stopButton);
        panel.add(progressBar);

        newContentPane.add(panel, BorderLayout.PAGE_START);
        //add(new JScrollPane(taskOutput), BorderLayout.CENTER);
        //setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
           
        
        newContentPane.setOpaque(true); //content panes must be opaque
        frame.setContentPane(newContentPane);

        //Display the window.
        frame.pack();
        frame.setVisible(true);
		
		
		
		
				
		int i=0;
		int j=0;
		int k=0;
		XYZ v0=null;
		XYZ v1=null;
		XYZ v2=null;
		XYZ v3=null;
		XYZ v4=null;
		XYZ v5=null;
		XYZ v6=null;
		XYZ v7=null;
		int vv0=-1;
		int vv1=-1;
		int vv2=-1;
		int vv3=-1;
		int vv4=-1;
		int vv5=-1;
		int vv6=-1;
		int vv7=-1;
		
		
		
		vol=new InputStack(imp, ip);
	
		
		IJ.showMessage("Volume dimension for moarching tetrahedron: "+ vol.getWidth() + " x " + vol.getHeight() + " x " +vol.getDepth());

		//setto il rapporto tra il piano xy e z
		//double ratio=200/45;
		//double dimx=67;
		//double dimy=67;
		//double dimz=200;
		double dimx=dx;
		double dimy=dy;
		double dimz=dz;
		
		//nuemero di cubi che devo esaminare
		float ncubes= (float)((vol.getWidth()-1)*(vol.getHeight()-1)*(vol.getDepth()-1));		
		//IJ.showMessage("nemero cubi da esaminare "+ ncubes);
		// contatore dei cubi esaminati
		float ncubeses= 0;
		//IJ.showMessage("nemero cubi esaminati "+ ncubeses);
		//creare la griglia di cubi
		for (k=0; k < (vol.getDepth()-1) ;k++ )
		{
			for (j=0; j < (vol.getHeight()-1) ;j++ )
			{
				for (i=0; i < (vol.getWidth()-1) ;i++ )
				{
					
					 if (!stop)
					 	return false;
					 v0= new XYZ(i*dimx, j*dimy, (k+1)*dimz);
					 v1= new XYZ((i+1)*dimx, j*dimy, (k+1)*dimz);
					 v2= new XYZ((i+1)*dimx, (j+1)*dimy, (k+1)*dimz);
					 v3= new XYZ(i*dimx, (j+1)*dimy, (k+1)*dimz);
					 v4= new XYZ(i*dimx, j*dimy, k*dimz);
					 v5= new XYZ((i+1)*dimx, j*dimy, k*dimz);
					 v6= new XYZ((i+1)*dimx, (j+1)*dimy, k*dimz);
					 v7= new XYZ(i*dimx, (j+1)*dimy, k*dimz);
					
					vv0=vol.getVoxel(i,j,k+1);
					vv1=vol.getVoxel(i+1,j,k+1);
					vv2=vol.getVoxel(i+1,j+1,k+1);
					vv3=vol.getVoxel(i,j+1,k+1);
					vv4=vol.getVoxel(i,j,k);
					vv5=vol.getVoxel(i+1,j,k);
					vv6=vol.getVoxel(i+1,j+1,k);
					vv7=vol.getVoxel(i,j+1,k);
					
				
					
				
							  
			  // TASSELLAZIONE DI PROVA
			  
			  		if ((k%2)==0)
		  			{
			  			if ((((j%2)==0) && ((i%2)==0))|| (((j%2)==1) && ((i%2)==1)))
			  			{
				  		//scomposizione 0
				  		a0=new Tetrahedron(vv1, vv6, vv4, vv3, v1, v6, v4, v3);
				  		
				
						a1=new Tetrahedron(vv4, vv6, vv1, vv5, v4, v6, v1, v5);
						
				
						a2=new Tetrahedron(vv4, vv7, vv3, vv6, v4, v7, v3, v6);
						
				
						a3=new Tetrahedron(vv3, vv4, vv1, vv0, v3, v4, v1, v0);
						
				
						a4=new Tetrahedron(vv3, vv2, vv1, vv6, v3, v2, v1, v6);
					
				
				  		
				  		
				  		
				  		
			  			}
		  				else
		  				{
			  			//scomposizione 1
			  			
				  		a0=new Tetrahedron(vv2, vv0, vv7, vv5, v2, v0, v7, v5);
						a1=new Tetrahedron(vv7, vv4, vv5, vv0, v7, v4, v5, v0);
						a2=new Tetrahedron(vv2, vv5, vv7, vv6, v2, v5, v7, v6);
						a3=new Tetrahedron(vv2, vv7, vv0, vv3, v2, v7, v0, v3);
						a4=new Tetrahedron(vv2, vv1, vv0, vv5, v2, v1, v0, v5);
					
					
		  				}
			  		
			  		
			  		}
			  		else
			  		{
			  			if ((((j%2)==0) && ((i%2)==0))|| (((j%2)==1) && ((i%2)==1)))
			  			{
				  		//scomposizione 1
				  		
				  		a0=new Tetrahedron(vv2, vv0, vv7, vv5, v2, v0, v7, v5);
						a1=new Tetrahedron(vv7, vv4, vv5, vv0, v7, v4, v5, v0);
						a2=new Tetrahedron(vv2, vv5, vv7, vv6, v2, v5, v7, v6);
						a3=new Tetrahedron(vv2, vv7, vv0, vv3, v2, v7, v0, v3);
						a4=new Tetrahedron(vv2, vv1, vv0, vv5, v2, v1, v0, v5);
					
					
			  			}
		  				else
		  				{
			  			//scomposizione 0
			  		
				  		a0=new Tetrahedron(vv1, vv6, vv4, vv3, v1, v6, v4, v3);
						a1=new Tetrahedron(vv4, vv6, vv1, vv5, v4, v6, v1, v5);
						a2=new Tetrahedron(vv4, vv7, vv3, vv6, v4, v7, v3, v6);
						a3=new Tetrahedron(vv3, vv4, vv1, vv0, v3, v4, v1, v0);
						a4=new Tetrahedron(vv3, vv2, vv1, vv6, v3, v2, v1, v6);
					
					
		  				}
		  			}
			  		
			  		
			  		// FINE TASSELLAZIONE DI PROVA
			  		
			  		
			  //TASSELLAZIONE 6 tetraedri
			  /*
					a0=new Tetrahedron(vv0, vv3, vv2, vv7, v0, v3, v2, v7);
					a1=new Tetrahedron(vv0, vv2, vv6, vv7, v0, v2, v6, v7);
					a2=new Tetrahedron(vv0, vv6, vv4, vv7, v0, v6, v4, v7);
					a3=new Tetrahedron(vv0, vv1, vv6, vv2, v0, v1, v6, v2);
					a4=new Tetrahedron(vv0, vv6, vv1, vv4, v0, v6, v1, v4);
					a5=new Tetrahedron(vv5, vv1, vv6, vv4, v5, v1, v6, v4);
					
					
				*/	
					
					
					triangola(a0,isovalue,0);
					
					
					triangola(a1,isovalue,1);
					
					triangola(a2,isovalue,2);
					
					triangola(a3,isovalue,3);
					
					triangola(a4,isovalue,4);
					
				//	triangola(a5,isovalue,5);// per tassellazione 6 tetra
					
					
					// setto la progressBar
					ncubeses++;
					progressBar.setValue((int)(ncubeses*100/ncubes ));
        			progressBar.setStringPainted(true);
					
				}
			}

		}


	 volumemax=volumemax + volumemin;

	 return (true);
	 }


/*
	sia c un cubo e v un isovalore calcola la triangolazione che rappresenta la superficie di valore v passante per c 
	ritorna il numero di triangoli della triangolazione di cui sopra (minimo 0 quando il cubo è tutto fuori o dentro 
	alla superficie, massimo 5)
*/
int triangola(Tetrahedron tetra, int isolevel, int numero)
{
   int j,i;   
   int indice;  // intero che viene usato come un array binario (0 per vertici del cubo fuori dalla sup 1 se dentro)
   
   

   		indice = 0;
   
  		

		if (tetra.v0<isovalue) indice|=1;
		if (tetra.v1<isovalue) indice|=2;
		if (tetra.v2<isovalue) indice|=4;
		if (tetra.v3<isovalue) indice|=8;
		
		vertlist= new XYZ[6];
		
	  if (MyMarchingTetrahedron.edgetable[indice] == 0)
   	  {
	   
	   if (indice==0)
	   {
	
			volumemin= volumemin + dx*dy*dz;   
		   
	   }	
	   	
	   return(0);
	}
  
		
		if ((MyMarchingTetrahedron.edgetable[indice] & 1)>0)
			vertlist[0]=VertexInterp(isolevel,tetra.getPoint(0),tetra.getPoint(1),tetra.getValueVertex(0),tetra.getValueVertex(1));
		if ((MyMarchingTetrahedron.edgetable[indice] & 2)>0)
			vertlist[1]=VertexInterp(isolevel,tetra.getPoint(1),tetra.getPoint(2),tetra.getValueVertex(1),tetra.getValueVertex(2));
		if ((MyMarchingTetrahedron.edgetable[indice] & 4)>0)
			vertlist[2]=VertexInterp(isolevel,tetra.getPoint(2),tetra.getPoint(0),tetra.getValueVertex(2),tetra.getValueVertex(0));
		if ((MyMarchingTetrahedron.edgetable[indice] & 8)>0)
			vertlist[3]=VertexInterp(isolevel,tetra.getPoint(0),tetra.getPoint(3),tetra.getValueVertex(0),tetra.getValueVertex(3));
		if ((MyMarchingTetrahedron.edgetable[indice] & 16)>0)
			vertlist[4]=VertexInterp(isolevel,tetra.getPoint(1),tetra.getPoint(3),tetra.getValueVertex(1),tetra.getValueVertex(3));
		if ((MyMarchingTetrahedron.edgetable[indice] & 32)>0)
			vertlist[5]=VertexInterp(isolevel,tetra.getPoint(2),tetra.getPoint(3),tetra.getValueVertex(2),tetra.getValueVertex(3));
							
		
      
   
	
  
	
   int pos;
   
   if (MyMarchingTetrahedron.triTable[indice][0]!=-1)
   		volumemax= volumemax + dx*dy*dz;
   
   		
   		
   
   for (i=0;MyMarchingTetrahedron.triTable[indice][i]!=-1;i+=3) {
    
      tetra.add_Tri(vertlist[MyMarchingTetrahedron.triTable[indice][i  ]],vertlist[MyMarchingTetrahedron.triTable[indice][i+1]], vertlist[MyMarchingTetrahedron.triTable[indice][i+2]] );
    
    
	
	try{
	
     if (proper_triangle(vertlist[MyMarchingTetrahedron.triTable[indice][i]],vertlist[MyMarchingTetrahedron.triTable[indice][i+1]],vertlist[MyMarchingTetrahedron.triTable[indice][i+2]]))
	  {
	  
		  
		  try{
			  FileWriter fw = new FileWriter("debug1.txt", true);
			  fw.write("Tetraedro " + numero + " \n");
			  fw.write("Punto 0: " + (tetra.getPoint(0)).getX() + " " + (tetra.getPoint(0)).getY() + " " +(tetra.getPoint(0)).getZ() + "\n");
			  fw.write("Punto 1: " + (tetra.getPoint(1)).getX() + " " + (tetra.getPoint(1)).getY() + " " +(tetra.getPoint(1)).getZ() + "\n");
			  fw.write("Punto 2: " + (tetra.getPoint(2)).getX() + " " + (tetra.getPoint(2)).getY() + " " +(tetra.getPoint(2)).getZ() + "\n");
			  fw.write("Punto 3: " + (tetra.getPoint(3)).getX() + " " + (tetra.getPoint(3)).getY() + " " +(tetra.getPoint(3)).getZ() + "\n\n");
			  fw.flush();
			  /*
			  fw.write("Triangolo : \n");
			  fw.write("Punto 0: " + MyMarchingTetrahedron.triTable[indice][i+2]+ "\n");
			  fw.write("Punto 1: " + MyMarchingTetrahedron.triTable[indice][i]  +  "\n");
			  fw.write("Punto 2: " + MyMarchingTetrahedron.triTable[indice][i+1]+"\n");
			  */
			  fw.flush();
			  
			  
			  
			  
			  
			  }catch(Exception e)
			  {IJ.showMessage("Errore di debug");}
		  
		  
		  
		  
		  
		  
		  
		  
		  
		  
		  
	 	pos= MyMarchingTetrahedron.find(out_vertex, vertlist[MyMarchingTetrahedron.triTable[indice][i+2]]);
	 	if (pos==-1)
	 	{
	 		  out_vertex.add(vertlist[MyMarchingTetrahedron.triTable[indice][i+2]]);
	 		  out_triangle.add(new Integer(out_nvertici));		  
	 		  out_nvertici++;
 		}
 		else
 			out_triangle.add(new Integer(pos));	
	 	
 		pos= MyMarchingTetrahedron.find(out_vertex, vertlist[MyMarchingTetrahedron.triTable[indice][i]]);
 		
 		if (pos==-1)
	 	{
	 		  out_vertex.add(vertlist[MyMarchingTetrahedron.triTable[indice][i]]);
	 		  out_triangle.add(new Integer(out_nvertici));		  
	 		  out_nvertici++;
 		}
 		else
 			out_triangle.add(new Integer(pos));	
	 	
 		pos= MyMarchingTetrahedron.find(out_vertex, vertlist[MyMarchingTetrahedron.triTable[indice][i+1]]);
 		
 		if (pos==-1)
	 	{
		 
	 		  out_vertex.add(vertlist[MyMarchingTetrahedron.triTable[indice][i+1]]);
	 		  out_triangle.add(new Integer(out_nvertici));		  
	 		  out_nvertici++;
 		}
 		else
 			out_triangle.add(new Integer(pos));	
	 	
 	
 		out_ntriangoli++;
 	
 		area=area+MyMarchingTetrahedron.areaTriangolo(vertlist[MyMarchingTetrahedron.triTable[indice][i]], vertlist[MyMarchingTetrahedron.triTable[indice][i+1]], vertlist[MyMarchingTetrahedron.triTable[indice][i+2]]);
 	
 	
 		
 	
	  }
  }catch(Exception e ){IJ.showMessage("errore nello scrivere triangoli " + e.getMessage());}
   }
  


   return(out_ntriangoli);
}
/*
int triangola(Tetrahedron grid, int isolevel)
{
   int j,i;   
   int tetraindex;  // intero che viene usato come un array binario (0 per vertici del cubo fuori dalla sup 1 se dentro)
   
   

   		tetraindex = 0;
   
  		

		if (grid.v0<isovalue) tetraindex|=1;
		if (grid.v1<isovalue) tetraindex|=2;
		if (grid.v2<isovalue) tetraindex|=4;
		if (grid.v3<isovalue) tetraindex|=8;
		
		vertlist= new XYZ[6];
	int pos;	
	 Form the vertices of the triangles for each case 
   switch (tetraindex) {
   case 0x00:
   case 0x0F:
      break;
   case 0x0E:
   case 0x01:
      vertlist[0] = VertexInterp(isolevel,grid.getPoint(0),grid.getPoint(1),grid.getValueVertex(0),grid.getValueVertex(1));
      pos= MyMarchingTetrahedron.find(out_vertex, vertlist[0]);
 		if (pos==-1)
	 	{
		 
	 		  out_vertex.add(vertlist[0]);
	 		  out_triangle.add(new Integer(out_nvertici));		  
	 		  out_nvertici++;
 		}
 		else
 			out_triangle.add(new Integer(pos));	
	   
      
	   vertlist[1] = VertexInterp(isolevel,grid.getPoint(0),grid.getPoint(2),grid.getValueVertex(0),grid.getValueVertex(2));
      pos= MyMarchingTetrahedron.find(out_vertex, vertlist[1]);
 		if (pos==-1)
	 	{
		 
	 		  out_vertex.add(vertlist[1]);
	 		  out_triangle.add(new Integer(out_nvertici));		  
	 		  out_nvertici++;
 		}
 		else
 			out_triangle.add(new Integer(pos));	
	 	
 	
 	
      
      vertlist[2] = VertexInterp(isolevel,grid.getPoint(0),grid.getPoint(3),grid.getValueVertex(0),grid.getValueVertex(3));
      pos= MyMarchingTetrahedron.find(out_vertex, vertlist[2]);
 		if (pos==-1)
	 	{
		 
	 		  out_vertex.add(vertlist[2]);
	 		  out_triangle.add(new Integer(out_nvertici));		  
	 		  out_nvertici++;
 		}
 		else
 			out_triangle.add(new Integer(pos));	
	 	
 	
 		out_ntriangoli++;
      
      
      break;
   case 0x0D:
   case 0x02:
     vertlist[0] = VertexInterp(isolevel,grid.getPoint(1),grid.getPoint(0),grid.getValueVertex(1),grid.getValueVertex(0));
     pos= MyMarchingTetrahedron.find(out_vertex, vertlist[0]);
 		if (pos==-1)
	 	{
		 
	 		  out_vertex.add(vertlist[0]);
	 		  out_triangle.add(new Integer(out_nvertici));		  
	 		  out_nvertici++;
 		}
 		else
 			out_triangle.add(new Integer(pos));	
	 	
 	
 	
     vertlist[1] = VertexInterp(isolevel,grid.getPoint(1),grid.getPoint(3),grid.getValueVertex(1),grid.getValueVertex(3));
     pos= MyMarchingTetrahedron.find(out_vertex, vertlist[1]);
 		if (pos==-1)
	 	{
		 
	 		  out_vertex.add(vertlist[1]);
	 		  out_triangle.add(new Integer(out_nvertici));		  
	 		  out_nvertici++;
 		}
 		else
 			out_triangle.add(new Integer(pos));	
	 	
 	
 	
     vertlist[2] = VertexInterp(isolevel,grid.getPoint(1),grid.getPoint(2),grid.getValueVertex(1),grid.getValueVertex(2));
     pos= MyMarchingTetrahedron.find(out_vertex, vertlist[2]);
 		if (pos==-1)
	 	{
		 
	 		  out_vertex.add(vertlist[2]);
	 		  out_triangle.add(new Integer(out_nvertici));		  
	 		  out_nvertici++;
 		}
 		else
 			out_triangle.add(new Integer(pos));	
	 	
 	
 		out_ntriangoli++;
      
      break;
   case 0x0C:
   case 0x03:
     vertlist[0] = VertexInterp(isolevel,grid.getPoint(0),grid.getPoint(3),grid.getValueVertex(0),grid.getValueVertex(3));
     pos= MyMarchingTetrahedron.find(out_vertex, vertlist[0]);
 		if (pos==-1)
	 	{
		 
	 		  out_vertex.add(vertlist[0]);
	 		  out_triangle.add(new Integer(out_nvertici));		  
	 		  out_nvertici++;
 		}
 		else
 			out_triangle.add(new Integer(pos));	
	 	
 	
 	
 		
     vertlist[1] = VertexInterp(isolevel,grid.getPoint(0),grid.getPoint(2),grid.getValueVertex(0),grid.getValueVertex(2));
     pos= MyMarchingTetrahedron.find(out_vertex, vertlist[1]);
 		if (pos==-1)
	 	{
		 
	 		  out_vertex.add(vertlist[1]);
	 		  out_triangle.add(new Integer(out_nvertici));		  
	 		  out_nvertici++;
 		}
 		else
 			out_triangle.add(new Integer(pos));	
	 	
 	
 	
 		
     vertlist[2] = VertexInterp(isolevel,grid.getPoint(1),grid.getPoint(3),grid.getValueVertex(1),grid.getValueVertex(3));
     pos= MyMarchingTetrahedron.find(out_vertex, vertlist[2]);
 		if (pos==-1)
	 	{
		 
	 		  out_vertex.add(vertlist[2]);
	 		  out_triangle.add(new Integer(out_nvertici));		  
	 		  out_nvertici++;
 		}
 		else
 			out_triangle.add(new Integer(pos));	
	 	
 	
 		out_ntriangoli++;
      
      
     vertlist[3] = vertlist[2];
     pos= MyMarchingTetrahedron.find(out_vertex, vertlist[3]);
 		if (pos==-1)
	 	{
		 
	 		  out_vertex.add(vertlist[3]);
	 		  out_triangle.add(new Integer(out_nvertici));		  
	 		  out_nvertici++;
 		}
 		else
 			out_triangle.add(new Integer(pos));	
	 	
 	
 	
 		
    vertlist[4] = VertexInterp(isolevel,grid.getPoint(1),grid.getPoint(2),grid.getValueVertex(1),grid.getValueVertex(2));
    pos= MyMarchingTetrahedron.find(out_vertex, vertlist[4]);
 		if (pos==-1)
	 	{
		 
	 		  out_vertex.add(vertlist[4]);
	 		  out_triangle.add(new Integer(out_nvertici));		  
	 		  out_nvertici++;
 		}
 		else
 			out_triangle.add(new Integer(pos));	
	 	
 	
 	
 		
     vertlist[5] = vertlist[1];
     pos= MyMarchingTetrahedron.find(out_vertex, vertlist[5]);
 		if (pos==-1)
	 	{
		 
	 		  out_vertex.add(vertlist[5]);
	 		  out_triangle.add(new Integer(out_nvertici));		  
	 		  out_nvertici++;
 		}
 		else
 			out_triangle.add(new Integer(pos));	
	 	
 	
 		out_ntriangoli++;
 		
      
      break;
   case 0x0B:
   case 0x04:
     vertlist[0] = VertexInterp(isolevel,grid.getPoint(2),grid.getPoint(0),grid.getValueVertex(2),grid.getValueVertex(0));
     pos= MyMarchingTetrahedron.find(out_vertex, vertlist[0]);
 		if (pos==-1)
	 	{
		 
	 		  out_vertex.add(vertlist[0]);
	 		  out_triangle.add(new Integer(out_nvertici));		  
	 		  out_nvertici++;
 		}
 		else
 			out_triangle.add(new Integer(pos));	
	 	
 	
 	
 		
   vertlist[1] = VertexInterp(isolevel,grid.getPoint(2),grid.getPoint(1),grid.getValueVertex(2),grid.getValueVertex(1));
   pos= MyMarchingTetrahedron.find(out_vertex, vertlist[1]);
 		if (pos==-1)
	 	{
		 
	 		  out_vertex.add(vertlist[1]);
	 		  out_triangle.add(new Integer(out_nvertici));		  
	 		  out_nvertici++;
 		}
 		else
 			out_triangle.add(new Integer(pos));	
	 	
 	
 	
 		
    vertlist[2] = VertexInterp(isolevel,grid.getPoint(2),grid.getPoint(3),grid.getValueVertex(2),grid.getValueVertex(3));
    pos= MyMarchingTetrahedron.find(out_vertex, vertlist[2]);
 		if (pos==-1)
	 	{
		 
	 		  out_vertex.add(vertlist[2]);
	 		  out_triangle.add(new Integer(out_nvertici));		  
	 		  out_nvertici++;
 		}
 		else
 			out_triangle.add(new Integer(pos));	
	 	
 	
 		out_ntriangoli++;
 		
      
      break;
   case 0x0A:
   case 0x05:
    vertlist[0] = VertexInterp(isolevel,grid.getPoint(0),grid.getPoint(1),grid.getValueVertex(0),grid.getValueVertex(1));
    pos= MyMarchingTetrahedron.find(out_vertex, vertlist[0]);
 		if (pos==-1)
	 	{
		 
	 		  out_vertex.add(vertlist[0]);
	 		  out_triangle.add(new Integer(out_nvertici));		  
	 		  out_nvertici++;
 		}
 		else
 			out_triangle.add(new Integer(pos));	
	 	
 	
 	
 		
    vertlist[1] = VertexInterp(isolevel,grid.getPoint(2),grid.getPoint(3),grid.getValueVertex(2),grid.getValueVertex(3));
    pos= MyMarchingTetrahedron.find(out_vertex, vertlist[1]);
 		if (pos==-1)
	 	{
		 
	 		  out_vertex.add(vertlist[1]);
	 		  out_triangle.add(new Integer(out_nvertici));		  
	 		  out_nvertici++;
 		}
 		else
 			out_triangle.add(new Integer(pos));	
	 	
 	
 	
 		
     vertlist[2] = VertexInterp(isolevel,grid.getPoint(0),grid.getPoint(3),grid.getValueVertex(0),grid.getValueVertex(3));
     pos= MyMarchingTetrahedron.find(out_vertex, vertlist[2]);
 		if (pos==-1)
	 	{
		 
	 		  out_vertex.add(vertlist[2]);
	 		  out_triangle.add(new Integer(out_nvertici));		  
	 		  out_nvertici++;
 		}
 		else
 			out_triangle.add(new Integer(pos));	
	 	
 	
 		out_ntriangoli++;
 		
      
     vertlist[3] = vertlist[0];
     pos= MyMarchingTetrahedron.find(out_vertex, vertlist[3]);
 		if (pos==-1)
	 	{
		 
	 		  out_vertex.add(vertlist[3]);
	 		  out_triangle.add(new Integer(out_nvertici));		  
	 		  out_nvertici++;
 		}
 		else
 			out_triangle.add(new Integer(pos));	
	 	
 	
 	
 		
    vertlist[4] = VertexInterp(isolevel,grid.getPoint(1),grid.getPoint(2),grid.getValueVertex(1),grid.getValueVertex(2));
    pos= MyMarchingTetrahedron.find(out_vertex, vertlist[4]);
 		if (pos==-1)
	 	{
		 
	 		  out_vertex.add(vertlist[4]);
	 		  out_triangle.add(new Integer(out_nvertici));		  
	 		  out_nvertici++;
 		}
 		else
 			out_triangle.add(new Integer(pos));	
	 	
 	
 	
 		
    vertlist[5] = vertlist[1];
    pos= MyMarchingTetrahedron.find(out_vertex, vertlist[5]);
 		if (pos==-1)
	 	{
		 
	 		  out_vertex.add(vertlist[5]);
	 		  out_triangle.add(new Integer(out_nvertici));		  
	 		  out_nvertici++;
 		}
 		else
 			out_triangle.add(new Integer(pos));	
	 	
 	
 		out_ntriangoli++;
 		
      
      break;
   case 0x09:
   case 0x06:
   vertlist[0] = VertexInterp(isolevel,grid.getPoint(0),grid.getPoint(1),grid.getValueVertex(0),grid.getValueVertex(1));
   pos= MyMarchingTetrahedron.find(out_vertex, vertlist[0]);
 		if (pos==-1)
	 	{
		 
	 		  out_vertex.add(vertlist[0]);
	 		  out_triangle.add(new Integer(out_nvertici));		  
	 		  out_nvertici++;
 		}
 		else
 			out_triangle.add(new Integer(pos));	
	 	
 	
 	
 		
  vertlist[1] = VertexInterp(isolevel,grid.getPoint(1),grid.getPoint(3),grid.getValueVertex(1),grid.getValueVertex(3));
  pos= MyMarchingTetrahedron.find(out_vertex, vertlist[1]);
 		if (pos==-1)
	 	{
		 
	 		  out_vertex.add(vertlist[1]);
	 		  out_triangle.add(new Integer(out_nvertici));		  
	 		  out_nvertici++;
 		}
 		else
 			out_triangle.add(new Integer(pos));	
	 	
 	
 	
 		
   vertlist[2] = VertexInterp(isolevel,grid.getPoint(2),grid.getPoint(3),grid.getValueVertex(2),grid.getValueVertex(3));
   pos= MyMarchingTetrahedron.find(out_vertex, vertlist[2]);
 		if (pos==-1)
	 	{
		 
	 		  out_vertex.add(vertlist[2]);
	 		  out_triangle.add(new Integer(out_nvertici));		  
	 		  out_nvertici++;
 		}
 		else
 			out_triangle.add(new Integer(pos));	
	 	
 	
 		out_ntriangoli++;
 		
      
  vertlist[3] = vertlist[0];
  pos= MyMarchingTetrahedron.find(out_vertex, vertlist[3]);
 		if (pos==-1)
	 	{
		 
	 		  out_vertex.add(vertlist[3]);
	 		  out_triangle.add(new Integer(out_nvertici));		  
	 		  out_nvertici++;
 		}
 		else
 			out_triangle.add(new Integer(pos));	
	 	
 	
 	
 		
   vertlist[4] = VertexInterp(isolevel,grid.getPoint(0),grid.getPoint(2),grid.getValueVertex(0),grid.getValueVertex(2));
   pos= MyMarchingTetrahedron.find(out_vertex, vertlist[4]);
 		if (pos==-1)
	 	{
		 
	 		  out_vertex.add(vertlist[4]);
	 		  out_triangle.add(new Integer(out_nvertici));		  
	 		  out_nvertici++;
 		}
 		else
 			out_triangle.add(new Integer(pos));	
	 	
 	
 	
 		
  vertlist[5] = vertlist[2];
  pos= MyMarchingTetrahedron.find(out_vertex, vertlist[5]);
 		if (pos==-1)
	 	{
		 
	 		  out_vertex.add(vertlist[5]);
	 		  out_triangle.add(new Integer(out_nvertici));		  
	 		  out_nvertici++;
 		}
 		else
 			out_triangle.add(new Integer(pos));	
	 	
 	
 		out_ntriangoli++;
 		
      
      break;
   case 0x07:
   case 0x08:
   vertlist[0] = VertexInterp(isolevel,grid.getPoint(3),grid.getPoint(0),grid.getValueVertex(3),grid.getValueVertex(0));
   pos= MyMarchingTetrahedron.find(out_vertex, vertlist[0]);
 		if (pos==-1)
	 	{
		 
	 		  out_vertex.add(vertlist[0]);
	 		  out_triangle.add(new Integer(out_nvertici));		  
	 		  out_nvertici++;
 		}
 		else
 			out_triangle.add(new Integer(pos));	
	 	
 	
 	
 		
    vertlist[1] = VertexInterp(isolevel,grid.getPoint(3),grid.getPoint(2),grid.getValueVertex(3),grid.getValueVertex(2));
    pos= MyMarchingTetrahedron.find(out_vertex, vertlist[1]);
 		if (pos==-1)
	 	{
		 
	 		  out_vertex.add(vertlist[1]);
	 		  out_triangle.add(new Integer(out_nvertici));		  
	 		  out_nvertici++;
 		}
 		else
 			out_triangle.add(new Integer(pos));	
	 	
 	
 	
 		
    vertlist[2] = VertexInterp(isolevel,grid.getPoint(3),grid.getPoint(1),grid.getValueVertex(3),grid.getValueVertex(1));
    pos= MyMarchingTetrahedron.find(out_vertex, vertlist[2]);
 		if (pos==-1)
	 	{
		 
	 		  out_vertex.add(vertlist[2]);
	 		  out_triangle.add(new Integer(out_nvertici));		  
	 		  out_nvertici++;
 		}
 		else
 			out_triangle.add(new Integer(pos));	
	 	
 	
 		out_ntriangoli++;
     
      break;
   }
	
  
	
   
   if (MyMarchingTetrahedron.triTable[tetraindex][0]!=-1)
   		volumemax= volumemax + dx*dy*dz;
   
   		
   		
   
   
  


   return(out_ntriangoli);
}*/
/*

	Dati tre punti restituisce true se costituiscono un triangolo (tre punti defferenti)

*/

boolean proper_triangle(XYZ p1, XYZ p2, XYZ p3)
{
	if (p1.different(p2) && p1.different(p3) && p2.different(p3))
		return true;
	return false;	
		
}






/*
   Interpolazione lineare per sapere dove l'isosuperficie interseca un edge tra due vertici
*/
XYZ VertexInterp(int isolevel, XYZ p1,XYZ p2, int valp1, int valp2)
{
	
	
	//ATTENZIONE:se volglio far funzionare i valori >128 devo sistemare il valore di valp1, valp2!!!!!!!
   double mu;
   XYZ p=new XYZ();
   
   
   //int isovalue=0;
   
   		
   if (ABS(isolevel-valp1) < 0.00001)
      return(p1);
   if (ABS(isolevel-valp2) < 0.00001)
      return(p2);
   if (ABS(valp1-valp2) < 0.00001)
      return(p1);
   mu = ((double)(isolevel - valp1)) / ((double)(valp2 - valp1));
   p.setX(p1.getX() + mu * (p2.getX() - p1.getX()));
   p.setY(p1.getY() + mu * (p2.getY() - p1.getY()));
   p.setZ(p1.getZ() + mu * (p2.getZ() - p1.getZ()));
   
   return(p);
}
/*
	Valore assoluto di un double ------- GUARDA SUL MANUALE DEL C L'OPERANDO APPOSITO!!!!!
*/
double ABS(double b)
{
	double d=(double)b;
	if (d<0) d=d* (-1);
	return d;
		
	
}

static double areaTriangolo(XYZ v0, XYZ v1, XYZ v2)
{
	
	
	XYZ v= new XYZ(XYZ.appVector(v0, v1));
	XYZ w= new XYZ(XYZ.appVector(v0, v2));
	
	XYZ cross= XYZ.crossProduct(v, w);
	return (XYZ.magnitude(cross)/2); 
	
	
	
	
}



static int find(ArrayList al, XYZ point)
{
	int i;
	for (i=0; i<al.size(); i++)
	{
		if (XYZ.equal((XYZ)al.get(i), point))
			return i;	
	}
	return -1;
}

};



