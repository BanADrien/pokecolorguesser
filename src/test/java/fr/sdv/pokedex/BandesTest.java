package fr.sdv.pokedex;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BandesTest {

    // colorie un rectangle de l'image, pixel par pixel
    private void colorier(BufferedImage image, int x, int y, int largeur, int hauteur, Color couleur) {
        for (int ligne = y; ligne < y + hauteur; ligne++) {
            for (int colonne = x; colonne < x + largeur; colonne++) {
                image.setRGB(colonne, ligne, couleur.getRGB());
            }
        }
    }

    // un corps de 56 × 20 pixels posé sur deux jambes écartées : un gros trou entre les jambes
    private BufferedImage deuxJambes() {
        BufferedImage image = new BufferedImage(96, 96, BufferedImage.TYPE_INT_ARGB);
        colorier(image, 20, 20, 56, 20, Color.ORANGE);
        colorier(image, 20, 40, 12, 44, Color.ORANGE);
        colorier(image, 64, 40, 12, 44, Color.ORANGE);
        return image;
    }

    @Test
    void chaqueBandeGardeLaCouleurLaPlusFrequenteEtLaLargeur() {
        // une image transparente de 96 × 96, comme un sprite, avec un Pokémon de 40 × 64 pixels
        BufferedImage image = new BufferedImage(96, 96, BufferedImage.TYPE_INT_ARGB);
        colorier(image, 30, 20, 30, 64, Color.RED);
        colorier(image, 60, 20, 10, 64, Color.BLUE);

        Bandes bandes = new Bandes(image, 8, false, false);

        assertEquals(8, bandes.getListe().size());
        Bande premiere = bandes.getListe().get(0);
        assertEquals(20, premiere.getHaut());
        assertEquals(28, premiere.getBas());
        assertEquals(30, premiere.getGauche());
        assertEquals(70, premiere.getDroite());
        assertEquals(Color.RED, premiere.getCouleur());
        assertEquals(84, bandes.getListe().get(7).getBas());

        // pas plus de bandes que de lignes de pixels
        assertEquals(64, new Bandes(image, 128, false, false).getNombreSections());
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

        Bandes avecOption = new Bandes(image, 2, true, false);
        assertEquals(Color.RED, avecOption.getListe().get(0).getCouleur());
        assertEquals(contour, avecOption.getListe().get(1).getCouleur());
        assertEquals(10, avecOption.getListe().get(0).getGauche());

        Bandes sansOption = new Bandes(image, 2, false, false);
        assertEquals(contour, sansOption.getListe().get(0).getCouleur());
    }

    @Test
    void uneColonneVerticaleGardeSaCouleurEtSaHauteur() {
        Color contour = new Color(16, 16, 16);
        BufferedImage image = new BufferedImage(96, 96, BufferedImage.TYPE_INT_ARGB);
        // colonne de gauche, haute de 60 pixels : 6 pixels de contour presque noir et 4 de vert par ligne
        colorier(image, 20, 24, 6, 60, contour);
        colorier(image, 26, 24, 4, 60, Color.GREEN);
        // colonne de droite, haute de 20 pixels, toute rouge
        colorier(image, 30, 64, 10, 20, Color.RED);

        Bandes bandes = new Bandes(image, 2, true, true);

        assertTrue(bandes.isVertical());
        assertEquals(2, bandes.getListe().size());
        Bande gauche = bandes.getListe().get(0);
        assertEquals(20, gauche.getGauche());
        assertEquals(30, gauche.getDroite());
        assertEquals(24, gauche.getHaut());
        assertEquals(84, gauche.getBas());
        assertEquals(Color.GREEN, gauche.getCouleur());
        Bande droite = bandes.getListe().get(1);
        assertEquals(30, droite.getGauche());
        assertEquals(40, droite.getDroite());
        assertEquals(64, droite.getHaut());
        assertEquals(84, droite.getBas());
        assertEquals(Color.RED, droite.getCouleur());

        // sans l'option, le contour l'emporte ; et pas plus de colonnes que la largeur visible
        assertEquals(contour, new Bandes(image, 2, false, true).getListe().get(0).getCouleur());
        assertEquals(20, new Bandes(image, 32, true, true).getNombreSections());
    }

    @Test
    void leVideEstLaPartDesRectanglesSansPixelVisible() {
        // deux carrés de 10 × 10 sur la même ligne, séparés par un trou de 10 pixels
        BufferedImage image = new BufferedImage(96, 96, BufferedImage.TYPE_INT_ARGB);
        colorier(image, 10, 10, 10, 10, Color.GREEN);
        colorier(image, 30, 10, 10, 10, Color.GREEN);

        // une seule bande horizontale bouche le trou : 200 pixels visibles sur 300
        assertEquals(1.0 / 3, new Bandes(image, 1, true, false).getVide(), 0.0001);
        // trois colonnes : celle du milieu est vide, elle n'a pas de bande
        Bandes colonnes = new Bandes(image, 3, true, true);
        assertEquals(2, colonnes.getListe().size());
        assertEquals(0, colonnes.getVide(), 0.0001);
    }

    @Test
    void leModeAutomatiqueChoisitLeVerticalPourDeuxJambesSeparees() {
        BufferedImage image = deuxJambes();
        Bandes horizontales = new Bandes(image, 8, true, false);
        Bandes verticales = new Bandes(image, 8, true, true);
        assertTrue(verticales.getVide() < horizontales.getVide());
        assertTrue(Bandes.verticalPlusFidele(image, 8, true));

        // les paramètres : automatique par défaut, sinon le sens choisi
        Parametres parametres = new Parametres();
        assertTrue(parametres.bandesVerticales(image));
        parametres.setSens(Parametres.SENS_HORIZONTAL);
        assertFalse(parametres.bandesVerticales(image));
    }

    @Test
    void leModeAutomatiqueGardeLHorizontalQuandCestPlusFidele() {
        // un « T » : une barre de 56 × 16 pixels sur un pied de 12 pixels de large
        BufferedImage image = new BufferedImage(96, 96, BufferedImage.TYPE_INT_ARGB);
        colorier(image, 20, 20, 56, 16, Color.CYAN);
        colorier(image, 42, 36, 12, 48, Color.CYAN);
        assertFalse(Bandes.verticalPlusFidele(image, 8, true));

        // un rectangle plein : aucun vide des deux côtés, l'égalité garde l'horizontal
        BufferedImage rectangle = new BufferedImage(96, 96, BufferedImage.TYPE_INT_ARGB);
        colorier(rectangle, 30, 30, 40, 40, Color.PINK);
        assertFalse(Bandes.verticalPlusFidele(rectangle, 8, true));

        // le sens peut aussi être imposé dans les paramètres
        Parametres parametres = new Parametres();
        parametres.setSens(Parametres.SENS_VERTICAL);
        assertTrue(parametres.bandesVerticales(image));
    }
}
