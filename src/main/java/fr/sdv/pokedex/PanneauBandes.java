package fr.sdv.pokedex;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

// Deux cases côte à côte : à gauche les bandes de couleurs (l'indice, en bandes horizontales ou verticales),
// à droite un « ? » ou le vrai Pokémon.
// Les deux cases utilisent la même échelle, pour que les pixels restent nets et bien alignés.
public class PanneauBandes extends JPanel {

    private static final int ESPACE = 24;         // entre les deux cases
    private static final int MARGE = 8;           // à l'intérieur des cases
    private static final int ESPACE_LEGENDE = 4;  // entre une case et sa légende
    private static final Color FOND_CASE = new Color(246, 246, 246);
    private static final Color BORD_CASE = new Color(200, 200, 200);
    private static final Color GRIS_QUESTION = new Color(185, 185, 185);
    private static final Color GRIS_LEGENDE = new Color(110, 110, 110);
    private static final Color GRIS_MESSAGE = new Color(70, 70, 70);

    private Bandes bandes;
    private BufferedImage image;
    private boolean imageVisible;
    private String message;

    public PanneauBandes() {
        setPreferredSize(new Dimension(640, 320));
    }

    // les bandes d'un Pokémon ; l'image n'est dessinée que si imageVisible vaut true
    public void afficher(Bandes bandes, BufferedImage image, boolean imageVisible) {
        this.bandes = bandes;
        this.image = image;
        this.imageVisible = imageVisible;
        this.message = null;
        repaint();
    }

    public void montrerImage() {
        imageVisible = true;
        repaint();
    }

    // un texte centré à la place des cases (une ligne par \n)
    public void setMessage(String message) {
        this.message = message;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics;
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Insets bords = getInsets();
        Rectangle zone = new Rectangle(bords.left, bords.top,
                getWidth() - bords.left - bords.right, getHeight() - bords.top - bords.bottom);
        if (message != null) {
            dessinerMessage(g, zone);
        } else if (bandes != null) {
            dessinerCases(g, zone);
        }
    }

    private void dessinerMessage(Graphics2D g, Rectangle zone) {
        g.setFont(getFont().deriveFont(getFont().getSize2D() + 4));
        g.setColor(GRIS_MESSAGE);
        FontMetrics mesures = g.getFontMetrics();
        String[] lignes = message.split("\n");
        int hauteurLigne = mesures.getHeight() + 4;
        int y = zone.y + (zone.height - lignes.length * hauteurLigne) / 2 + mesures.getAscent();
        for (String ligne : lignes) {
            g.drawString(ligne, zone.x + (zone.width - mesures.stringWidth(ligne)) / 2, y);
            y += hauteurLigne;
        }
    }

    private void dessinerCases(Graphics2D g, Rectangle zone) {
        FontMetrics mesures = g.getFontMetrics(getFont());
        int hauteurLegende = ESPACE_LEGENDE + mesures.getHeight();

        // la plus grande échelle entière qui fait tenir les deux cases et leurs légendes
        int largeurImage = bandes.getLargeurImage();
        int hauteurImage = bandes.getHauteurImage();
        int echelleX = (zone.width - ESPACE - 4 * MARGE) / (2 * largeurImage);
        int echelleY = (zone.height - hauteurLegende - 2 * MARGE) / hauteurImage;
        int echelle = Math.max(1, Math.min(echelleX, echelleY));

        int largeurCase = largeurImage * echelle + 2 * MARGE;
        int hauteurCase = hauteurImage * echelle + 2 * MARGE;
        int gauche = zone.x + (zone.width - (2 * largeurCase + ESPACE)) / 2;
        int haut = zone.y + (zone.height - (hauteurCase + hauteurLegende)) / 2;
        int droite = gauche + largeurCase + ESPACE;

        dessinerCase(g, gauche, haut, largeurCase, hauteurCase);
        dessinerBandes(g, gauche + MARGE, haut + MARGE, echelle);

        dessinerCase(g, droite, haut, largeurCase, hauteurCase);
        if (imageVisible && image != null) {
            dessinerImage(g, droite + MARGE, haut + MARGE, echelle);
        } else {
            dessinerPointInterrogation(g, droite, haut, largeurCase, hauteurCase);
        }

        g.setFont(getFont());
        g.setColor(GRIS_LEGENDE);
        int yLegende = haut + hauteurCase + ESPACE_LEGENDE + mesures.getAscent();
        String indice = legendeIndice();
        g.drawString(indice, gauche + (largeurCase - mesures.stringWidth(indice)) / 2, yLegende);
        g.drawString("Réponse", droite + (largeurCase - mesures.stringWidth("Réponse")) / 2, yLegende);
    }

    // "Indice · 16 bandes verticales" : dans le quiz, le nombre de bandes change quand on perd une vie
    private String legendeIndice() {
        int sections = bandes.getNombreSections();
        String sens = "horizontale";
        if (bandes.isVertical()) {
            sens = "verticale";
        }
        if (sections == 0) {
            return "Indice";
        } else if (sections == 1) {
            return "Indice · 1 bande " + sens;
        }
        return "Indice · " + sections + " bandes " + sens + "s";
    }

    private void dessinerCase(Graphics2D g, int x, int y, int largeur, int hauteur) {
        g.setColor(FOND_CASE);
        g.fillRect(x, y, largeur, hauteur);
        g.setColor(BORD_CASE);
        g.drawRect(x, y, largeur - 1, hauteur - 1);
    }

    // Le Pokémon est centré en largeur et posé en bas de la case. Le décalage est le même
    // pour les bandes et pour l'image : les deux dessins se superposent exactement.
    private int decalageX() {
        Rectangle zone = bandes.getZone();
        return (bandes.getLargeurImage() - zone.width) / 2 - zone.x;
    }

    private int decalageY() {
        Rectangle zone = bandes.getZone();
        return bandes.getHauteurImage() - zone.height - zone.y;
    }

    private void dessinerBandes(Graphics2D g, int origineX, int origineY, int echelle) {
        for (Bande bande : bandes.getListe()) {
            int x = origineX + (bande.getGauche() + decalageX()) * echelle;
            int y = origineY + (bande.getHaut() + decalageY()) * echelle;
            int largeur = (bande.getDroite() - bande.getGauche()) * echelle;
            int hauteur = (bande.getBas() - bande.getHaut()) * echelle;
            g.setColor(bande.getCouleur());
            g.fillRect(x, y, largeur, hauteur);
            // sur une bande très fine (ou une colonne très étroite), le bord cacherait la couleur
            if (largeur >= 4 && hauteur >= 4) {
                g.setColor(bande.getCouleur().darker());
                g.drawRect(x, y, largeur - 1, hauteur - 1);
            }
        }
    }

    private void dessinerImage(Graphics2D g, int origineX, int origineY, int echelle) {
        Rectangle zone = bandes.getZone();
        int x = origineX + (zone.x + decalageX()) * echelle;
        int y = origineY + (zone.y + decalageY()) * echelle;
        // agrandissement « pixel art » : chaque pixel devient un carré net, sans flou
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(image,
                x, y, x + zone.width * echelle, y + zone.height * echelle,
                zone.x, zone.y, zone.x + zone.width, zone.y + zone.height,
                null);
    }

    private void dessinerPointInterrogation(Graphics2D g, int x, int y, int largeur, int hauteur) {
        Font police = getFont().deriveFont(Font.BOLD, hauteur * 0.55f);
        // on centre le dessin du « ? » lui-même, plus précis que les mesures de la police
        Rectangle2D forme = police.createGlyphVector(g.getFontRenderContext(), "?").getVisualBounds();
        g.setFont(police);
        g.setColor(GRIS_QUESTION);
        g.drawString("?",
                (float) (x + (largeur - forme.getWidth()) / 2 - forme.getX()),
                (float) (y + (hauteur - forme.getHeight()) / 2 - forme.getY()));
    }
}
