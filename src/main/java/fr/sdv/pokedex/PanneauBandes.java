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
import java.util.ArrayList;

// Des cases côte à côte, chacune avec sa légende : des bandes de couleurs, la silhouette pixelisée, le vrai Pokémon
// ou un « ? ». Le quiz montre l'indice et la réponse, les paramètres les trois indices et la réponse.
// Toutes les cases utilisent la même échelle, pour que les pixels restent nets et bien alignés.
public class PanneauBandes extends JPanel {

    private static final int ESPACE = 16;         // entre deux cases
    private static final int MARGE = 8;           // à l'intérieur des cases
    private static final int ESPACE_LEGENDE = 4;  // entre une case et sa légende
    private static final Color FOND_CASE = new Color(246, 246, 246);
    private static final Color BORD_CASE = new Color(200, 200, 200);
    private static final Color GRIS_QUESTION = new Color(185, 185, 185);
    private static final Color GRIS_LEGENDE = new Color(110, 110, 110);
    private static final Color GRIS_MESSAGE = new Color(70, 70, 70);

    private BufferedImage image;      // le sprite du Pokémon
    private Rectangle zoneVisible;    // le rectangle de ses pixels visibles
    // le contenu de chaque case, de gauche à droite : des bandes, ou une image (null pour un « ? »)
    private ArrayList<Bandes> bandesDesCases = new ArrayList<>();
    private ArrayList<BufferedImage> imagesDesCases = new ArrayList<>();
    private ArrayList<String> legendes = new ArrayList<>();
    private String message;

    public PanneauBandes() {
        setPreferredSize(new Dimension(640, 320));
    }

    // enlève toutes les cases : les suivantes montreront ce Pokémon
    public void nouvellesCases(BufferedImage image) {
        this.image = image;
        zoneVisible = Bandes.zoneVisible(image);
        bandesDesCases.clear();
        imagesDesCases.clear();
        legendes.clear();
        message = null;
        repaint();
    }

    public void ajouterBandes(Bandes bandes, String legende) {
        bandesDesCases.add(bandes);
        imagesDesCases.add(null);
        legendes.add(legende);
        repaint();
    }

    // une case avec une image de la taille du sprite (le sprite ou sa silhouette pixelisée) ; null : un « ? »
    public void ajouterImage(BufferedImage dessin, String legende) {
        bandesDesCases.add(null);
        imagesDesCases.add(dessin);
        legendes.add(legende);
        repaint();
    }

    // la dernière case (la réponse) montre le vrai Pokémon
    public void montrerImage() {
        if (!imagesDesCases.isEmpty()) {
            imagesDesCases.set(imagesDesCases.size() - 1, image);
            repaint();
        }
    }

    // un texte centré à la place des cases (une ligne par \n)
    public void setMessage(String message) {
        this.message = message;
        repaint();
    }

    // "12 colonnes" ou "1 ligne", pour les légendes
    public static String decrire(Bandes bandes) {
        int sections = bandes.getNombreSections();
        String nom = "ligne";
        if (bandes.isVertical()) {
            nom = "colonne";
        }
        if (sections > 1) {
            return sections + " " + nom + "s";
        }
        return sections + " " + nom;
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
        } else if (!legendes.isEmpty()) {
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
        int nombre = legendes.size();

        // les cases sur une seule rangée ; les 4 cases de l'aperçu passent sur 2 rangées si elles y sont plus grandes
        int parRangee = nombre;
        int rangees = 1;
        int echelle = calculerEchelle(zone, nombre, 1, hauteurLegende);
        if (nombre == 4 && calculerEchelle(zone, 2, 2, hauteurLegende) > echelle) {
            parRangee = 2;
            rangees = 2;
            echelle = calculerEchelle(zone, 2, 2, hauteurLegende);
        }

        int largeurCase = image.getWidth() * echelle + 2 * MARGE;
        int hauteurCase = image.getHeight() * echelle + 2 * MARGE;
        int hauteurRangee = hauteurCase + hauteurLegende + ESPACE;
        int gauche = zone.x + (zone.width - (parRangee * largeurCase + (parRangee - 1) * ESPACE)) / 2;
        int haut = zone.y + (zone.height - (rangees * hauteurRangee - ESPACE)) / 2;

        for (int i = 0; i < nombre; i++) {
            int x = gauche + (i % parRangee) * (largeurCase + ESPACE);
            int y = haut + (i / parRangee) * hauteurRangee;
            dessinerCase(g, x, y, largeurCase, hauteurCase);
            if (bandesDesCases.get(i) != null) {
                dessinerBandes(g, bandesDesCases.get(i), x + MARGE, y + MARGE, echelle);
            } else if (imagesDesCases.get(i) != null) {
                dessinerImage(g, imagesDesCases.get(i), x + MARGE, y + MARGE, echelle);
            } else {
                dessinerPointInterrogation(g, x, y, largeurCase, hauteurCase);
            }
            String legende = legendes.get(i);
            g.setFont(getFont());
            g.setColor(GRIS_LEGENDE);
            g.drawString(legende, x + (largeurCase - mesures.stringWidth(legende)) / 2,
                    y + hauteurCase + ESPACE_LEGENDE + mesures.getAscent());
        }
    }

    // la plus grande échelle entière qui fait tenir les cases et leurs légendes (au moins 1)
    private int calculerEchelle(Rectangle zone, int parRangee, int rangees, int hauteurLegende) {
        int largeurLibre = zone.width - (parRangee - 1) * ESPACE - parRangee * 2 * MARGE;
        int hauteurLibre = zone.height - (rangees - 1) * ESPACE - rangees * (hauteurLegende + 2 * MARGE);
        int echelleX = largeurLibre / (parRangee * image.getWidth());
        int echelleY = hauteurLibre / (rangees * image.getHeight());
        return Math.max(1, Math.min(echelleX, echelleY));
    }

    private void dessinerCase(Graphics2D g, int x, int y, int largeur, int hauteur) {
        g.setColor(FOND_CASE);
        g.fillRect(x, y, largeur, hauteur);
        g.setColor(BORD_CASE);
        g.drawRect(x, y, largeur - 1, hauteur - 1);
    }

    // Le Pokémon est centré en largeur et posé en bas de la case. Le décalage est le même
    // dans toutes les cases : les bandes, la silhouette et le sprite se superposent exactement.
    private int decalageX() {
        return (image.getWidth() - zoneVisible.width) / 2 - zoneVisible.x;
    }

    private int decalageY() {
        return image.getHeight() - zoneVisible.height - zoneVisible.y;
    }

    private void dessinerBandes(Graphics2D g, Bandes bandes, int origineX, int origineY, int echelle) {
        for (Bande bande : bandes.getListe()) {
            int x = origineX + (bande.getGauche() + decalageX()) * echelle;
            int y = origineY + (bande.getHaut() + decalageY()) * echelle;
            int largeur = (bande.getDroite() - bande.getGauche()) * echelle;
            int hauteur = (bande.getBas() - bande.getHaut()) * echelle;
            g.setColor(bande.getCouleur());
            g.fillRect(x, y, largeur, hauteur);
            // sur une ligne très fine (ou une colonne très étroite), le bord cacherait la couleur
            if (largeur >= 4 && hauteur >= 4) {
                g.setColor(bande.getCouleur().darker());
                g.drawRect(x, y, largeur - 1, hauteur - 1);
            }
        }
    }

    private void dessinerImage(Graphics2D g, BufferedImage dessin, int origineX, int origineY, int echelle) {
        Rectangle zone = zoneVisible;
        int x = origineX + (zone.x + decalageX()) * echelle;
        int y = origineY + (zone.y + decalageY()) * echelle;
        // agrandissement « pixel art » : chaque pixel devient un carré net, sans flou
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(dessin,
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
