/*
* Auteur : Équipe 1
* Date   : juin 2023
* */


/*      Paquetage      */
package cinketera.ihm;




/*       Imports       */
import javax.swing.ImageIcon;
import javax.swing.JPanel;

import cinketera.Controleur;
import cinketera.metier.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel affichant la map (iles, chemin, etc...)
 */
public class PanelIles extends JPanel
{

	/* Attributs de Classe */
	/*      Constants      */
	public  static final String NOM_CHEMIN = "./resources/iles/";
	private static final Color  BACK_COLOR = new Color( 35,137,218); // 182, 211, 255




	/*      Attributs      */
	private int id;

	private boolean estNouvelleManche;	
	private Ile ile1;
	private Ile ile2;

	private Controleur ctrl;

	private int maxX;
	private int maxY;
	private double coef;
	
	private List<ImageIcon> lstImgIles;

	private List<Polygon> polygons;
	private Rectangle hitboxCarteBonus;
	private Ellipse2D historique;




	/**
	 * Constructeur.
	 */
	public PanelIles(Controleur ctrl, int id) 
	{
		this.ctrl = ctrl;
		this.id   = id;
		this.estNouvelleManche = true;
		this.declarerImage();

		//Paramètre de base
		this.setBackground( PanelIles.BACK_COLOR );

		//Action
		GereSouris gereSouris = new GereSouris();
		this.addMouseListener(gereSouris);
		this.addMouseMotionListener(gereSouris);
	}




	/*      Méthodes       */
	private void declarerImage()
	{
		this.lstImgIles = new ArrayList<>();
		for (Ile i : this.ctrl.getIles(this.id)) 
			this.lstImgIles.add( new ImageIcon(PanelIles.NOM_CHEMIN + i.getNom() + ".png"));

		//On regarde le max X et Y
		List<Ile> lstIles = this.ctrl.getIles(this.id);
		for (Ile i : lstIles) 
		{
			int x = this.lstImgIles.get(lstIles.indexOf(i)).getIconHeight() + i.getXImages();
			int y = this.lstImgIles.get(lstIles.indexOf(i)).getIconWidth () + i.getYImages();

			maxX = Math.max(x, maxX);
			maxY = Math.max(y, maxY);
		}
	}

	public void newManche () 
	{ 
		this.estNouvelleManche = true;
		this.ile2 = null; 
	}



	
	/*     Paint/Draw      */
	//"Main"
	public void paintComponent(Graphics g) 
	{
		super.paintComponent(g);

		List<Ile>     lstIles    = this.ctrl.getIles(this.id);
		List<Region>  lstRegion  = this.ctrl.getRegions( this.id );
		List<Polygon> lstPolygon = new ArrayList<>();
		Graphics2D    g2         = (Graphics2D) g;

		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		Font boldFont   = new Font("BoldFont"  , Font.BOLD , 12);
		Font dialogFont = new Font("DialogFont", Font.PLAIN, 12);


		//Calcul du coef
		int hauteur    = this.ctrl.getHauteur(this.id) - 40 ;
		int largeur    = this.ctrl.getLargeur(this.id) - 20 ;
		
		double coef1   = hauteur * 1.0 / (this.maxY);
		double coef2   = largeur * 1.0 / (this.maxX);
		double oldCoef = this.coef;
		
		this.coef = Math.min(coef1, coef2);

		if (this.coef < 0.3) this.coef = 0.3;

		if ( oldCoef != this.coef ) this.updateShape();


		//Afficher l'ile de début en surligné pour savoir ou on commence
		if (this.estNouvelleManche)
		{
			this.ile1 = this.ctrl.getIleDebut(this.id);
		}
		this.estNouvelleManche = false;


		//Représenter les régions
		for (Region r : lstRegion)
		{
			Polygon     poly   = new Polygon    ();
			List<Point> points = new ArrayList<>();

			for (Ile i : r.getIles())
			{
				int       x   = i.getXImages();
				int       y   = i.getYImages();

				ImageIcon img = this.lstImgIles.get( lstIles.indexOf(i) );

				int       w   = img.getIconWidth();
				int       h   = img.getIconHeight();

				points.add( new Point( x    , y    ) );
				points.add( new Point( x + w, y    ) );
				points.add( new Point( x    , y + h) );
				points.add( new Point( x + w, y + h) );
			}

			this.trierPointsPolygone(points, poly);
			this.dessinerFondRegion(g2,poly);

			lstPolygon.add(poly);
		}


		//Déssiner bordure îles
		for (Ile i : this.ctrl.getIles(this.id))
		{
			Polygon p = this.trouverPolygon(i);
			Polygon pResize = new Polygon(p.xpoints, p.ypoints, p.npoints);

			pResize = PanelIles.resizePolygon(pResize, 1.2);
			g2.setColor(new Color(116,204,244));
			g2.fill(pResize);

		}


		// Afficher les nom des régions
		for (Region r : lstRegion)
		{
			Polygon poly = lstPolygon.get( lstRegion.indexOf(r) );
			double x     = poly.getBounds().getMinX();
			double y     = poly.getBounds().getMinY();

			g2.setColor(Color.BLACK);
			g2.setFont( boldFont );

			g2.drawString( r.getNom(), (float) x, (float) y);
			g2.drawLine((int) x, (int) y + 2, (int) x + getFontMetrics(boldFont).stringWidth(r.getNom()), (int) y + 2);

			g2.setFont( dialogFont );
		}


		//Afficher les routes
		for (Chemin c : this.ctrl.getChemins(this.id)) 
		{
			// Point
			Ile i1 = c.getIleA();
			Ile i2 = c.getIleB();

			// Coords
			int x1 = (int)(i1.getXPoint() * this.coef);
			int y1 = (int)(i1.getYPoint() * this.coef);

			int x2 = (int) (i2.getXPoint() * this.coef);
			int y2 = (int) (i2.getYPoint() * this.coef);

			// Ligne
			if (c.getCouleurPrim() == null)
			{
				Stroke  stroke;
				float[] pointille;
				if(c.getBonus() == 0)
				{
					pointille = new float[]{2f, 2f};
					stroke = new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f, pointille, 2.0f);
					g2.setColor(new Color (88, 41, 0));
				}
				else
				{
					pointille = new float[]{5f, 2f};
					stroke    = new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f, pointille, 0.0f);
					g2.setColor(new Color (52,12,0));
				}

				g2.setStroke(stroke);
				g2.drawLine(x1, y1, x2, y2);
			}
			else
			{
				g2.setColor(c.getCouleurPrim());
				g2.setStroke(new BasicStroke(5f));
				g2.drawLine(x1, y1, x2, y2);

				if (c.getCouleurSec() != null)
				{
					float[] pointille = {2f, 2f};
					g2.setColor(c.getCouleurSec());
					g2.setStroke(new BasicStroke(5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f, pointille , 2.0f));
					g2.drawLine(x1, y1, x2, y2);
				}

			}
		}


		Color colFeutre = this.ctrl.getColFeutre(this.id);


		// Afficher les iles
		if (this.ile1 != null)
		{
			for (Chemin c : this.ile1.getCheminAutour())
				if (this.ctrl.estColoriable(c, this.id))
				{
					this.drawPolygonePossible(c.getIleA(), g2);
					this.drawPolygonePossible(c.getIleB(), g2);
				}
				
		}

		for (Ile i : this.ctrl.getIles(this.id)) 
		{

			//Contour si selectionné 
			if (i == this.ile1 || i == this.ile2)
			{
				Polygon p = this.trouverPolygon(i);

				g2.setColor(colFeutre.brighter());
				g2.fill(p);
				g2.setStroke(new BasicStroke(5f));
				g2.drawPolygon(p);
			}

			// Images100
			ImageIcon img      = this.lstImgIles.get(lstIles.indexOf(i));
			PanelIles.redimensionnerIcon(img, this.coef).paintIcon(this, g, (int) (i.getXImages() * this.coef), (int) (i.getYImages()*this.coef));

			// Noms des iles
			g2.setColor(Color.BLACK);
			g2.drawString( i.getNom(), (int) (i.getXNom() * this.coef) , (int) (i.getYNom() * this.coef));
		}


		//Afficher le paneau du score
		ImageIcon imgPanneau = PanelIles.redimensionnerIcon(new ImageIcon("./resources/images/panneau_score.png"), this.coef/2);

		int xPanneau = largeur - imgPanneau.getIconWidth();
		int yPanneau = 0;

		imgPanneau.paintIcon(this, g2, xPanneau, yPanneau);


		//Afficher le score
		g2.setColor(Color.BLACK);
		g2.setFont( boldFont );

		String[] ensS = PanelIles.this.ctrl.getScore(this.id).split(";");
		g2.drawString( "Total : "+ensS[0], xPanneau+30, yPanneau + (int)(imgPanneau.getIconHeight()/2) );

		g2.setColor(new Color(61,26,0));
		g2.drawString( " -"      +ensS[1], xPanneau+30, yPanneau + (int)(imgPanneau.getIconHeight()/2+15)    );
		g2.drawString( " -"      +ensS[2], xPanneau+30, yPanneau + (int)(imgPanneau.getIconHeight()/2+30)    );


		//Dessiner le logo de l'historique en bas à droite
		ImageIcon logo = PanelIles.redimensionnerIcon(new ImageIcon("./resources/images/Historique.png"), 5 * this.coef / 50);

		int x = largeur - logo.getIconWidth();
		int y = hauteur - logo.getIconHeight() - 10;

		logo.paintIcon(this, g2, x, y);

		double larg = logo.getIconWidth() - 20;
		
		this.historique = new Ellipse2D.Double(x + 10, y + 10, larg, larg );


		//Dessiner la carte bonus
		if ( this.ctrl.getOptionActive() )
		{
			ImageIcon imgCarteBonus = PanelIles.redimensionnerIcon(new ImageIcon(this.ctrl.getImageBonus(this.id)), 25*this.coef / 50);

			x = largeur - imgCarteBonus.getIconWidth();
			y = hauteur - imgCarteBonus.getIconHeight() - this.ctrl.getHauteur(1)/9;

			this.hitboxCarteBonus = new Rectangle(x, y, imgCarteBonus.getIconWidth(), imgCarteBonus.getIconHeight());

			imgCarteBonus.paintIcon(this, g2, x, y);
		}
	}

	//Dessiner le dégrader des régions
	private void dessinerFondRegion (Graphics2D g2, Polygon p)
	{
		double[] echelle = { 1.2, 0.8};
		Color [] fondCol = {new Color( 28,163,236), new Color( 90,188,216)};

		//Poly 1
		for (int i = 0; i < echelle.length; i++)
		{
			Polygon poly = PanelIles.resizePolygon(p, echelle[i]);

			g2.setColor(fondCol[i]);
			g2.fill(poly);
		}
	}

	//Permet de redimensionner les polygons avec un coef donné
	public static Polygon resizePolygon(Polygon polygon, double coef) 
	{
		Point center = getPolygonCenter(polygon);

		for (int i = 0; i < polygon.npoints; i++) 
		{
			int deltaX = polygon.xpoints[i] - center.x;
			int deltaY = polygon.ypoints[i] - center.y;

			int newX = center.x + (int) (deltaX * coef);
			int newY = center.y + (int) (deltaY * coef);

			polygon.xpoints[i] = newX;
			polygon.ypoints[i] = newY;
		}

		return polygon;
	}

	//Recupère le centre du polygon
	public static Point getPolygonCenter(Polygon polygon) 
	{
		int totalX = 0;
		int totalY = 0;

		for (int i = 0; i < polygon.npoints; i++) 
		{
			totalX += polygon.xpoints[i]; //On regarde la largeur  x
			totalY += polygon.ypoints[i]; //On regarde la longueur y
		}

		int centerX = totalX / polygon.npoints;
		int centerY = totalY / polygon.npoints;

		return new Point(centerX, centerY);
	}

	//Trie les points pour garder les points exterieur
	private void trierPointsPolygone(List<Point> points, Polygon poly)
	{
		int n = points.size();
		if (n < 3)
		{
			for ( Point p : points ) poly.addPoint( (int) (p.getX() * this.coef), (int) (p.getY() * this.coef) );

			return;
		}

		List<Point> polygoneTri = new ArrayList<>();

		// Trouver le point le plus à gauche
		int pointGaucheIndex = 0;
		for (int i = 1; i < n; i++)
		{
			if ( points.get(i).getX() < points.get(pointGaucheIndex).getX() )
			{
				pointGaucheIndex = i;
			}
		}

		int pointCourantIndex = pointGaucheIndex;
		int pointSuivantIndex;

		do
		{
			polygoneTri.add( points.get(pointCourantIndex) );
			pointSuivantIndex = (pointCourantIndex + 1) % n;

			for ( int i = 0; i < n; i++ )
			{
				// Vérifier si le point i est plus à gauche que le pointSuivant
				if ( orientation(points.get(pointCourantIndex), points.get(i), points.get(pointSuivantIndex)) == 2 )
				{
					pointSuivantIndex = i;
				}
			}

			pointCourantIndex = pointSuivantIndex;

		}
		while ( pointCourantIndex != pointGaucheIndex );

		for ( Point p : polygoneTri ) poly.addPoint( (int) (p.getX() * this.coef), (int) (p.getY() * this.coef) );
	}

	//Savoir dans quel orientation vas le trait
	private static int orientation(Point p, Point q, Point r)
	{
		int val = (int) ( (q.getY() - p.getY()) * (r.getX() - q.getX()) - (q.getX() - p.getX()) * (r.getY() - q.getY()) );

		if (val == 0) return 0;  // Les points sont colinéaires

		return (val > 0) ? 1 : 2; // 1 pour sens horaire, 2 pour sens anti-horaire
	}

	//Redimensionne les images
	public static ImageIcon redimensionnerIcon(ImageIcon img, double coef)
	{
		int       larg     = img.getIconWidth();
		int       lon      = img.getIconHeight();
		Image     reImage  = img.getImage().getScaledInstance((int) (larg*coef), (int) (lon*coef), Image.SCALE_DEFAULT);

		return new ImageIcon(reImage);
	}

	//Déssiner le contours des îles si ont peut les colorier
	private void drawPolygonePossible(Ile i, Graphics2D g2)
	{
		Polygon p = this.trouverPolygon(i);

		switch (i.getCoul())
		{
			case "Rouge" -> g2.setColor(new Color(204,  80, 124));
			case "Vert"  -> g2.setColor(new Color( 20, 186,  67));
			case "Jaune" -> g2.setColor(new Color(217, 215,  41));
			case "Brun"  -> g2.setColor(new Color(189, 187, 140));
			default      -> g2.setColor(new Color(255, 255, 200)); //par défauts
		}

		g2.fill(p);
		g2.setStroke(new BasicStroke(5f));
		g2.drawPolygon(p); 
	} 


	//Remet à jour le contour des îles
	public void updateShape()
	{
		this.polygons = new ArrayList<>();

		int cpt = 0;

		for (ImageIcon i : this.lstImgIles)
		{
			
			Ile ile = this.ctrl.getIles(this.id).get(cpt++);

			// Reset du polygone de l'image
			Polygon p = new Polygon();

			// Création d'une BufferedImage
			BufferedImage img = new BufferedImage(i.getImage().getWidth(null), i.getImage().getHeight(null), BufferedImage.TYPE_INT_ARGB);
			Graphics2D    bGr = img.createGraphics();

			bGr.drawImage(i.getImage(), 0, 0, null);
			bGr.dispose();

			// On note tout les points sur les bords droit puis gauche
			for (int y = 0; y < img.getHeight(); y++)
			{
				for (int x = img.getWidth() - 1; x >= 0; x--)
				{
					if ( img.getRGB(x, y) != 0 )
					{
						p.addPoint( (int) ( (x + ile.getXImages()) * this.coef ), (int) ( (y + ile.getYImages()) * this.coef ) );
						break;
					}
				}
			}

			for (int y = img.getHeight() - 1; y >= 0; y--)
			{
				for (int x = 0; x < img.getWidth(); x++)
				{
					if ( img.getRGB(x, y) != 0 )
					{
						p.addPoint( (int) ( (x + ile.getXImages()) * this.coef ), (int) ( (y + ile.getYImages()) * this.coef ) );
						break;
					}
				}
			}

			this.polygons.add(p);
		}
	}

	//Retrouver l'île correspondant au polygon
	public Polygon trouverPolygon(Ile i)
	{
		return this.polygons.get( this.ctrl.getIles(this.id).indexOf( i ) );
	}




	/**
	* Classe Privée : 
	*     - Gestion de la souris
	*/
	private class GereSouris extends MouseAdapter
	{


		/*    Constructeur     */
		public GereSouris()
		{
			super();

			PanelIles.this.updateShape();
			PanelIles.this.polygons = polygons;
		}


		/*     Activation      */
		public void mouseMoved(MouseEvent e)
		{
			int posX = e.getX();
			int posY = e.getY();

			//changer curseur sur la carte bonus
			if ( PanelIles.this.ctrl.getOptionActive() && !PanelIles.this.ctrl.bonusAEteActive( PanelIles.this.id ) )
			{
				if (PanelIles.this.hitboxCarteBonus.contains(posX, posY))
				{
					PanelIles.this.setCursor( new Cursor( Cursor.HAND_CURSOR ));
					return;
				}
			}

			//changer curseur sur le logo de l'historique
			if (PanelIles.this.historique.contains(posX, posY))
			{
				PanelIles.this.setCursor( new Cursor( Cursor.HAND_CURSOR ));
				return;
			}

			//changer curseur sur les iles selectionnees
			for( Polygon polygon : PanelIles.this.polygons )
			{
				if( polygon.contains( posX, posY ) )
				{
					PanelIles.this.setCursor( new Cursor( Cursor.HAND_CURSOR ));
					return;
				}
			}

			// sinon on reinitialise le curseur
			PanelIles.this.setCursor( new Cursor( Cursor.DEFAULT_CURSOR   ));	
		}

		public void mousePressed(MouseEvent e)
		{

			int posX = e.getX();
			int posY = e.getY();

			//PanelIles.this.ctrl.test();
			if (PanelIles.this.historique.contains(posX, posY))
			{
				PanelIles.this.ctrl.showHistorique(PanelIles.this.id);
				return;
			}

			if ( PanelIles.this.ctrl.getOptionActive() )
			{
				if (PanelIles.this.hitboxCarteBonus.contains(posX, posY))
				{
					PanelIles.this.ctrl.activerCarteBonus(PanelIles.this.id);
					PanelIles.this.repaint();
					return;
				}
			}

			Ile i = trouverIle( posX, posY );
			Controleur ctrl =  PanelIles.this.ctrl; // raccourci inhumain de l'argumentation

			if ( i == null )
			{
				PanelIles.this.ile1 = PanelIles.this.ile2 = null;
			} 
			else
			{
				PanelIles.this.ile2 = PanelIles.this.ile1;
				PanelIles.this.ile1 = i;
			}

			if ( PanelIles.this.ile1 != null && PanelIles.this.ile2 != null  && PanelIles.this.ile1 != PanelIles.this.ile2)
			{
				Chemin c = ctrl.trouverChemin(PanelIles.this.ile1, PanelIles.this.ile2, PanelIles.this.id);
				
				if ( !ctrl.colorier(c, PanelIles.this.id) )
				{
					PanelIles.this.ile2 = null;
					PanelIles.this.ile1 = i;
				}
			}

			if (i == null)
				PanelIles.this.ctrl.hideHistorique(PanelIles.this.id);

			PanelIles.this.repaint();
		}

		//Cherche l'ile clique.
		private Ile trouverIle(int x, int y)
		{
			for (Polygon p : PanelIles.this.polygons)
				if ( p != null  && p.contains(x, y) )
					return PanelIles.this.ctrl.getIles(PanelIles.this.id).get( PanelIles.this.polygons.indexOf(p) );

			return null;
		}
	}
}
