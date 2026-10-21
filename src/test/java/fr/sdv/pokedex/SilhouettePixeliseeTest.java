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
    void uneCaseEstNoireSiAuMoinsLaMoitieEstVisible() {
        // une image de 12 × 4, découpée en 3 cases de 4 × 4 (16 pixels chacune)
        BufferedImage image = new BufferedImage(12, 4, BufferedImage.TYPE_INT_ARGB);
        colorier(image, 0, 0, 4, 4, Color.RED);     // case de gauche : 16 pixels visibles
        colorier(image, 4, 0, 2, 4, Color.GREEN);   // case du milieu : 8 pixels, juste la moitié
        colorier(image, 11, 0, 1, 4, Color.BLUE);   // case de droite : 7 pixels...
        colorier(image, 10, 0, 1, 3, Color.BLUE);
        image.setRGB(9, 0, new Color(0, 0, 255, 50).getRGB()); // ... plus un presque transparent, qui ne compte pas

        SilhouettePixelisee silhouette = new SilhouettePixelisee(image, 3);

        assertEquals(4, silhouette.getCote());
        assertEquals(3, silhouette.getNombreColonnes());
        assertEquals(1, silhouette.getNombreLignes());
        assertTrue(silhouette.isNoire(0, 0));
        assertTrue(silhouette.isNoire(0, 1));
        assertFalse(silhouette.isNoire(0, 2));

        // dans l'image, une case noire l'est en entier, une case vide reste transparente
        BufferedImage dessin = silhouette.creerImage();
        assertEquals(12, dessin.getWidth());
        assertEquals(4, dessin.getHeight());
        assertEquals(Color.BLACK.getRGB(), dessin.getRGB(7, 3));
        assertEquals(0, dessin.getRGB(11, 0));
    }

    @Test
    void lesCasesSontCarreesEtCouvrentLaZoneVisible() {
        // un Pokémon de 30 pixels de large et 20 de haut, dans un sprite de 96 × 96
        BufferedImage image = new BufferedImage(96, 96, BufferedImage.TYPE_INT_ARGB);
        colorier(image, 40, 60, 30, 20, Color.MAGENTA);

        SilhouettePixelisee silhouette = new SilhouettePixelisee(image, 12);

        // 30 / 12 = 2,5 : des cases de 3 pixels, 10 sur la largeur et 7 sur la hauteur
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

        SilhouettePixelisee douze = new SilhouettePixelisee(image, 12);
        assertEquals(4, douze.getCote());
        assertEquals(10, douze.getNombreLignes());
        assertEquals(7, douze.getNombreColonnes());

        SilhouettePixelisee trenteDeux = new SilhouettePixelisee(image, 32);
        assertEquals(2, trenteDeux.getCote());
        assertEquals(19, trenteDeux.getNombreLignes());
        assertEquals(13, trenteDeux.getNombreColonnes());

        // une seule section : une seule grande case, noire car le Pokémon la remplit à 70 %
        SilhouettePixelisee une = new SilhouettePixelisee(image, 1);
        assertEquals(1, une.getNombreLignes());
        assertEquals(1, une.getNombreColonnes());
        assertTrue(une.isNoire(0, 0));

        // une image toute transparente n'a aucune case
        SilhouettePixelisee vide = new SilhouettePixelisee(new BufferedImage(96, 96, BufferedImage.TYPE_INT_ARGB), 12);
        assertEquals(0, vide.getNombreLignes());
        assertEquals(0, vide.getNombreColonnes());
    }
}
