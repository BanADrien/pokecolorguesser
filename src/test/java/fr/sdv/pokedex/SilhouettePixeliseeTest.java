package fr.sdv.pokedex;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SilhouettePixeliseeTest {

    // colorie un rectangle de l'image, pixel par pixel
    private void colorier(BufferedImage image, int x, int y, int largeur, int hauteur, Color couleur) {
        for (int ligne = y; ligne < y + hauteur; ligne++) {
            for (int colonne = x; colonne < x + largeur; colonne++) {
                image.setRGB(colonne, ligne, couleur.getRGB());
            }
        }
    }

    @Test
    void uneCaseEstNoireSiLePokemonEnRemplitAuMoins40PourCent() {
        // une image de 16 × 4 : avec 2 sections, 4 cases de 4 × 4 (16 pixels chacune)
        BufferedImage image = new BufferedImage(16, 4, BufferedImage.TYPE_INT_ARGB);
        colorier(image, 0, 0, 4, 4, Color.RED);     // 1re case : 16 pixels visibles
        colorier(image, 4, 0, 2, 3, Color.GREEN);   // 2e case : 6 pixels...
        image.setRGB(7, 0, Color.GREEN.getRGB());    // ... plus 1 : 7 pixels, plus de 40 % (6,4 pixels)
        colorier(image, 8, 0, 2, 3, Color.BLUE);    // 3e case : 6 pixels, moins de 40 %...
        image.setRGB(11, 0, new Color(0, 0, 255, 50).getRGB()); // ... plus un presque transparent, qui ne compte pas
        colorier(image, 15, 0, 1, 4, Color.BLUE);   // 4e case : 4 pixels

        SilhouettePixelisee silhouette = new SilhouettePixelisee(image, 2);

        assertEquals(4, silhouette.getTailleGrille());
        assertEquals(4, silhouette.getCote());
        assertEquals(4, silhouette.getNombreColonnes());
        assertEquals(1, silhouette.getNombreLignes());
        assertTrue(silhouette.isNoire(0, 0));
        assertTrue(silhouette.isNoire(0, 1));
        assertFalse(silhouette.isNoire(0, 2));
        assertFalse(silhouette.isNoire(0, 3));

        // dans l'image, une case noire l'est en entier, une case vide reste transparente
        BufferedImage dessin = silhouette.creerImage();
        assertEquals(16, dessin.getWidth());
        assertEquals(4, dessin.getHeight());
        assertEquals(Color.BLACK.getRGB(), dessin.getRGB(7, 3));
        assertEquals(0, dessin.getRGB(8, 0));
        assertEquals(0, dessin.getRGB(15, 0));
    }

    @Test
    void lesCasesSontCarreesEtCouvrentLaZoneVisible() {
        // un Pokémon de 30 pixels de large et 20 de haut, dans un sprite de 96 × 96
        BufferedImage image = new BufferedImage(96, 96, BufferedImage.TYPE_INT_ARGB);
        colorier(image, 40, 60, 30, 20, Color.MAGENTA);

        SilhouettePixelisee silhouette = new SilhouettePixelisee(image, 6);

        // 6 sections : 12 cases au plus sur la largeur ; 30 / 12 = 2,5 : des cases de 3 pixels,
        // 10 sur la largeur et 7 sur la hauteur
        assertEquals(3, silhouette.getCote());
        assertEquals(10, silhouette.getNombreColonnes());
        assertEquals(7, silhouette.getNombreLignes());
        // la grille est posée en bas : la case en bas à gauche est un carré entier
        Rectangle enBas = silhouette.getCase(6, 0);
        assertEquals(40, enBas.x);
        assertEquals(77, enBas.y);
        assertEquals(3, enBas.width);
        assertEquals(3, enBas.height);
        // la rangée du haut est coupée par le bord de la zone visible
        Rectangle enHaut = silhouette.getCase(0, 9);
        assertEquals(67, enHaut.x);
        assertEquals(60, enHaut.y);
        assertEquals(3, enHaut.width);
        assertEquals(2, enHaut.height);

        // toutes les cases sont remplies : le dessin recouvre exactement le Pokémon
        BufferedImage dessin = silhouette.creerImage();
        assertEquals(Color.BLACK.getRGB(), dessin.getRGB(40, 60));
        assertEquals(Color.BLACK.getRGB(), dessin.getRGB(69, 79));
        assertEquals(0, dessin.getRGB(39, 79));
        assertEquals(0, dessin.getRGB(70, 79));
        assertEquals(0, dessin.getRGB(40, 59));
    }

    @Test
    void leNombreDeCasesSuitLeNombreDeSections() {
        // un Pokémon de 26 pixels de large et 37 de haut : c'est la hauteur qui compte
        BufferedImage image = new BufferedImage(96, 96, BufferedImage.TYPE_INT_ARGB);
        colorier(image, 35, 59, 26, 37, Color.YELLOW);

        // 6 sections : 12 cases au plus, de 4 pixels (37 / 12 = 3,1)
        SilhouettePixelisee six = new SilhouettePixelisee(image, 6);
        assertEquals(4, six.getCote());
        assertEquals(10, six.getNombreLignes());
        assertEquals(7, six.getNombreColonnes());

        // 12 sections : 24 cases au plus, de 2 pixels (37 / 24 = 1,5)
        SilhouettePixelisee douze = new SilhouettePixelisee(image, 12);
        assertEquals(2, douze.getCote());
        assertEquals(19, douze.getNombreLignes());
        assertEquals(13, douze.getNombreColonnes());

        // une seule section : 2 cases au plus, toutes noires car le Pokémon est un rectangle plein
        SilhouettePixelisee une = new SilhouettePixelisee(image, 1);
        assertEquals(2, une.getNombreLignes());
        assertEquals(2, une.getNombreColonnes());
        assertTrue(une.isNoire(0, 0));
        assertTrue(une.isNoire(1, 1));

        // une image toute transparente n'a aucune case
        SilhouettePixelisee vide = new SilhouettePixelisee(new BufferedImage(96, 96, BufferedImage.TYPE_INT_ARGB), 12);
        assertEquals(0, vide.getTailleGrille());
        assertEquals(0, vide.getNombreLignes());
        assertEquals(0, vide.getNombreColonnes());
    }

    @Test
    void laSilhouetteADeuxFoisPlusDeCasesQueDeSectionsSansDepasserLesPixels() {
        // un Pokémon de 48 pixels de large et 96 de haut
        BufferedImage grand = new BufferedImage(96, 96, BufferedImage.TYPE_INT_ARGB);
        colorier(grand, 24, 0, 48, 96, Color.ORANGE);

        // 12 sections : 12 lignes pour l'indice en lignes, mais une silhouette de 24 × 24 cases au plus
        assertEquals(12, new Bandes(grand, 12, true, false).getNombreSections());
        SilhouettePixelisee douze = new SilhouettePixelisee(grand, 12);
        assertEquals(24, douze.getTailleGrille());
        assertEquals(4, douze.getCote());
        assertEquals(24, douze.getNombreLignes());
        assertEquals(12, douze.getNombreColonnes());

        // 6 sections : 12 × 12 cases au plus
        SilhouettePixelisee six = new SilhouettePixelisee(grand, 6);
        assertEquals(12, six.getTailleGrille());
        assertEquals(8, six.getCote());
        assertEquals(12, six.getNombreLignes());
        assertEquals(6, six.getNombreColonnes());

        // un petit Pokémon de 10 × 20 pixels : pas plus de cases que de pixels, donc des cases de 1 pixel
        BufferedImage petit = new BufferedImage(96, 96, BufferedImage.TYPE_INT_ARGB);
        colorier(petit, 40, 70, 10, 20, Color.ORANGE);
        SilhouettePixelisee plafond = new SilhouettePixelisee(petit, 12);
        assertEquals(20, plafond.getTailleGrille());
        assertEquals(1, plafond.getCote());
        assertEquals(20, plafond.getNombreLignes());
        assertEquals(10, plafond.getNombreColonnes());
        assertEquals(20, new SilhouettePixelisee(petit, 32).getTailleGrille());
    }
}
