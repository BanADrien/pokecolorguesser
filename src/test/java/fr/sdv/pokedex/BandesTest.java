package fr.sdv.pokedex;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BandesTest {

    // colorie un rectangle de l'image, pixel par pixel
    private void colorier(BufferedImage image, int x, int y, int largeur, int hauteur, Color couleur) {
        for (int ligne = y; ligne < y + hauteur; ligne++) {
            for (int colonne = x; colonne < x + largeur; colonne++) {
                image.setRGB(colonne, ligne, couleur.getRGB());
            }
        }
    }

    @Test
    void chaqueBandeGardeLaCouleurLaPlusFrequenteEtLaLargeur() {
        // une image transparente de 96 × 96, comme un sprite, avec un Pokémon de 40 × 64 pixels
        BufferedImage image = new BufferedImage(96, 96, BufferedImage.TYPE_INT_ARGB);
        colorier(image, 30, 20, 30, 64, Color.RED);
        colorier(image, 60, 20, 10, 64, Color.BLUE);

        Bandes bandes = new Bandes(image, 8, false);

        assertEquals(8, bandes.getListe().size());
        Bande premiere = bandes.getListe().get(0);
        assertEquals(20, premiere.getHaut());
        assertEquals(28, premiere.getBas());
        assertEquals(30, premiere.getGauche());
        assertEquals(70, premiere.getDroite());
        assertEquals(Color.RED, premiere.getCouleur());
        assertEquals(84, bandes.getListe().get(7).getBas());

        // pas plus de bandes que de lignes de pixels
        assertEquals(64, new Bandes(image, 128, false).getNombreSections());
    }

    @Test
    void lesContoursNoirsSontIgnoresSaufSiLaBandeEstTouteNoire() {
        Color contour = new Color(16, 16, 16);
        BufferedImage image = new BufferedImage(96, 96, BufferedImage.TYPE_INT_ARGB);
        // bande du haut : 18 pixels presque noirs et 12 rouges
        colorier(image, 10, 10, 6, 3, contour);
        colorier(image, 16, 10, 4, 3, Color.RED);
        // bande du bas : seulement des pixels presque noirs
        colorier(image, 10, 13, 10, 3, contour);

        Bandes avecOption = new Bandes(image, 2, true);
        assertEquals(Color.RED, avecOption.getListe().get(0).getCouleur());
        assertEquals(contour, avecOption.getListe().get(1).getCouleur());
        assertEquals(10, avecOption.getListe().get(0).getGauche());

        Bandes sansOption = new Bandes(image, 2, false);
        assertEquals(contour, sansOption.getListe().get(0).getCouleur());
    }
}
