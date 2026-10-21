package fr.sdv.pokedex;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

// Le dernier indice du quiz : la silhouette du Pokémon en gros pixels. Sa zone visible est découpée
// en cases carrées, deux fois plus fines que les bandes (24 × 24 environ pour 12 sections),
// et une case est noire si le Pokémon en remplit au moins 40 %.
public class SilhouettePixelisee {

    // la part de pixels visibles qu'il faut dans une case pour qu'elle soit noire
    public static final double REMPLISSAGE_MIN = 0.4;

    private int largeurImage;
    private int hauteurImage;
    private Rectangle zone;       // les pixels visibles du sprite
    private int tailleGrille;     // le nombre de cases au plus sur le plus grand côté
    private int cote;             // le côté d'une case, en pixels du sprite
    private int colonnes;
    private int lignes;
    private int gaucheGrille;     // le coin en haut à gauche de la grille
    private int hautGrille;
    private boolean[][] noires;   // noires[ligne][colonne]

    public SilhouettePixelisee(BufferedImage image, int sections) {
        largeurImage = image.getWidth();
        hauteurImage = image.getHeight();
        zone = Bandes.zoneVisible(image);

        // deux fois plus de cases que de sections sur le plus grand côté, mais pas plus que de pixels
        int plusGrandCote = Math.max(zone.width, zone.height);
        tailleGrille = Math.min(2 * sections, plusGrandCote);
        cote = 1; // image entièrement transparente
        if (tailleGrille > 0) {
            cote = (int) Math.ceil((double) plusGrandCote / tailleGrille);
        }
        colonnes = (int) Math.ceil((double) zone.width / cote);
        lignes = (int) Math.ceil((double) zone.height / cote);

        // La grille couvre toute la zone, et peut la dépasser d'un peu moins d'une case.
        // Elle est centrée en largeur et posée en bas, comme le Pokémon dans les cases du quiz.
        gaucheGrille = zone.x - (colonnes * cote - zone.width) / 2;
        hautGrille = zone.y + zone.height - lignes * cote;

        noires = new boolean[lignes][colonnes];
        for (int ligne = 0; ligne < lignes; ligne++) {
            for (int colonne = 0; colonne < colonnes; colonne++) {
                // les pixels de la case hors de la zone sont transparents : ils comptent comme vides
                int visibles = compterPixelsVisibles(image, getCase(ligne, colonne));
                noires[ligne][colonne] = visibles >= REMPLISSAGE_MIN * cote * cote;
            }
        }
    }

    // 24 pour une silhouette de 24 × 24 cases au plus : le double du nombre de sections,
    // sans dépasser le plus grand côté visible (en pixels)
    public int getTailleGrille() {
        return tailleGrille;
    }

    public int getCote() {
        return cote;
    }

    public int getNombreColonnes() {
        return colonnes;
    }

    public int getNombreLignes() {
        return lignes;
    }

    public boolean isNoire(int ligne, int colonne) {
        return noires[ligne][colonne];
    }

    // la place d'une case dans le sprite, coupée au bord de la zone visible
    public Rectangle getCase(int ligne, int colonne) {
        Rectangle carre = new Rectangle(gaucheGrille + colonne * cote, hautGrille + ligne * cote, cote, cote);
        return carre.intersection(zone);
    }

    // une image de la taille du sprite : les cases noires, et du transparent autour
    public BufferedImage creerImage() {
        BufferedImage dessin = new BufferedImage(largeurImage, hauteurImage, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dessin.createGraphics();
        g.setColor(Color.BLACK);
        for (int ligne = 0; ligne < lignes; ligne++) {
            for (int colonne = 0; colonne < colonnes; colonne++) {
                if (noires[ligne][colonne]) {
                    Rectangle uneCase = getCase(ligne, colonne);
                    g.fillRect(uneCase.x, uneCase.y, uneCase.width, uneCase.height);
                }
            }
        }
        g.dispose();
        return dessin;
    }

    private int compterPixelsVisibles(BufferedImage image, Rectangle partie) {
        int visibles = 0;
        for (int y = partie.y; y < partie.y + partie.height; y++) {
            for (int x = partie.x; x < partie.x + partie.width; x++) {
                if (Bandes.estVisible(new Color(image.getRGB(x, y), true))) {
                    visibles++;
                }
            }
        }
        return visibles;
    }
}
