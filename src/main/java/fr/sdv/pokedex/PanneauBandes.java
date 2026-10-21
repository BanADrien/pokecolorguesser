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
// Dans le quiz, les indices déjà vus restent en petit (des miniatures) en haut de la première case.
public class PanneauBandes extends JPanel {

    private static final int ESPACE = 16;         // entre deux cases
    private static final int MARGE = 8;           // à l'intérieur des cases
    private static final int ESPACE_LEGENDE = 4;  // entre une case et sa légende
    private static final int REDUCTION_MINIATURE = 4;  // une miniature fait le quart de l'indice en grand
    private static final int ESPACE_MINIATURE = 8;     // entre deux miniatures, et sous les miniatures
    private static final int ESPACE_LEGENDE_MINIATURE = 2;
    private static final int MARGE_MINIATURE = 3;      // à l'intérieur du cadre d'une miniature
    private static final Color FOND_CASE = new Color(246, 246, 246);
    private static final Color BORD_CASE = new Color(200, 200, 200);
    private static final Color FOND_MINIATURE = new Color(230, 230, 230); // pas blanc : il y a des bandes blanches
    private static final Color BORD_MINIATURE = new Color(165, 165, 165);
    private static final Color GRIS_QUESTION = new Color(185, 185, 185);
    private static final Color GRIS_LEGENDE = new Color(110, 110, 110);
    private static final Color GRIS_MESSAGE = new Color(70, 70, 70);

    private BufferedImage image;      // le sprite du Pokémon
    private Rectangle zoneVisible;    // le rectangle de ses pixels visibles
    // le contenu de chaque case, de gauche à droite : des bandes, ou une image (null pour un « ? »)
    private ArrayList<Bandes> bandesDesCases = new ArrayList<>();
    private ArrayList<BufferedImage> imagesDesCases = new ArrayList<>();
    private ArrayList<String> legendes = new ArrayList<>();
    private ArrayList<Bandes> miniatures = new ArrayList<>();  // les indices déjà vus, dans l'ordre
    private boolean avecMiniatures;   // true : les cases gardent de la place pour les miniatures
    private String message;

    public PanneauBandes() {
        setPreferredSize(new Dimension(640, 320));
    }

    // Le quiz : les cases gardent toujours de la place en haut pour les miniatures, même avant la première,
    // pour que rien ne bouge pendant la question.
    public void setAvecMiniatures(boolean avecMiniatures) {
        this.avecMiniatures = avecMiniatures;
        repaint();
    }

    // enlève toutes les cases et les miniatures : les suivantes montreront ce Pokémon
    public void nouvellesCases(BufferedImage image) {
        this.image = image;
        zoneVisible = Bandes.zoneVisible(image);
        bandesDesCases.clear();
        imagesDesCases.clear();
        legendes.clear();
        miniatures.clear();
        message = null;
        repaint();
    }

    // un indice déjà vu, en petit en haut de la première case (à droite des miniatures précédentes)
    public void ajouterMiniature(Bandes bandes) {
        miniatures.add(bandes);
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

    // "silhouette 24 × 24", pour les légendes
    public static String decrire(SilhouettePixelisee silhouette) {
        int taille = silhouette.getTailleGrille();
        return "silhouette " + taille + " × " + taille;
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

        // le sprite agrandi, et au-dessus la place gardée pour les miniatures (la même dans toutes les cases)
        int place = placeMiniatures(echelle);
        int largeurCase = image.getWidth() * echelle + 2 * MARGE;
        int hauteurCase = place + image.getHeight() * echelle + 2 * MARGE;
        int hauteurRangee = hauteurCase + hauteurLegende + ESPACE;
        int gauche = zone.x + (zone.width - (parRangee * largeurCase + (parRangee - 1) * ESPACE)) / 2;
        int haut = zone.y + (zone.height - (rangees * hauteurRangee - ESPACE)) / 2;

        for (int i = 0; i < nombre; i++) {
            int x = gauche + (i % parRangee) * (largeurCase + ESPACE);
            int y = haut + (i / parRangee) * hauteurRangee;
            // Le Pokémon est centré en largeur et posé en bas de la case. C'est la même place dans
            // toutes les cases : les bandes, la silhouette et le sprite se superposent exactement.
            int xPokemon = x + MARGE + ((image.getWidth() - zoneVisible.width) / 2) * echelle;
            int yPokemon = y + MARGE + place + (image.getHeight() - zoneVisible.height) * echelle;
            dessinerCase(g, x, y, largeurCase, hauteurCase);
            if (bandesDesCases.get(i) != null) {
                dessinerBandes(g, bandesDesCases.get(i), xPokemon, yPokemon, echelle, 1);
            } else if (imagesDesCases.get(i) != null) {
                dessinerImage(g, imagesDesCases.get(i), xPokemon, yPokemon, echelle);
            } else {
                dessinerPointInterrogation(g, x, y, largeurCase, hauteurCase);
            }
            if (i == 0) {
                dessinerMiniatures(g, x + MARGE, y + MARGE, echelle);
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
        int echelle = Math.min(echelleX, echelleY);
        // la place des miniatures grandit avec l'échelle : on réduit l'échelle tant qu'elle ne tient pas
        while (echelle > 1 && rangees * (image.getHeight() * echelle + placeMiniatures(echelle)) > hauteurLibre) {
            echelle--;
        }
        return Math.max(1, echelle);
    }

    // La hauteur à ajouter en haut des cases pour que les miniatures ne touchent jamais l'indice en grand.
    // Le Pokémon est posé en bas de la case : il suffit d'ajouter ce qui manque au-dessus de lui.
    private int placeMiniatures(int echelle) {
        if (!avecMiniatures) {
            return 0;
        }
        int hauteurMiniatures = hauteurBlocMiniature(echelle) + ESPACE_MINIATURE;
        int videAuDessus = (image.getHeight() - zoneVisible.height) * echelle;
        return Math.max(0, hauteurMiniatures - videAuDessus);
    }

    // une miniature avec son cadre et sa légende
    private int hauteurBlocMiniature(int echelle) {
        return coteMiniature(echelle) + 2 + ESPACE_LEGENDE_MINIATURE + getFontMetrics(policeMiniatures()).getHeight();
    }

    // le côté d'une miniature, sans son cadre : le quart du sprite agrandi (les sprites sont carrés)
    private int coteMiniature(int echelle) {
        return image.getWidth() * echelle / REDUCTION_MINIATURE;
    }

    private Font policeMiniatures() {
        return getFont().deriveFont(getFont().getSize2D() - 1);
    }

    // Les indices déjà vus, côte à côte en haut de la case, chacun dans un petit cadre avec sa légende.
    // Une miniature montre les mêmes bandes que l'indice en grand, réduites pour bien remplir le cadre.
    private void dessinerMiniatures(Graphics2D g, int x, int y, int echelle) {
        int cote = coteMiniature(echelle);
        // un pixel du sprite devient multiplicateur / diviseur pixels : le plus grand côté du Pokémon
        // occupe le cadre, moins une petite marge de chaque côté
        int multiplicateur = cote - 2 * MARGE_MINIATURE;
        int diviseur = Math.max(1, Math.max(zoneVisible.width, zoneVisible.height));
        int largeurPokemon = zoneVisible.width * multiplicateur / diviseur;
        int hauteurPokemon = zoneVisible.height * multiplicateur / diviseur;
        Font police = policeMiniatures();
        FontMetrics mesures = g.getFontMetrics(police);
        for (Bandes bandes : miniatures) {
            String legende = decrire(bandes);
            // la place de la miniature et de sa légende, la plus large des deux
            int largeurPlace = Math.max(cote + 2, mesures.stringWidth(legende));
            int gaucheCadre = x + (largeurPlace - cote - 2) / 2;
            g.setColor(FOND_MINIATURE);
            g.fillRect(gaucheCadre + 1, y + 1, cote, cote);
            // le Pokémon au milieu du cadre
            dessinerBandes(g, bandes, gaucheCadre + 1 + (cote - largeurPokemon) / 2,
                    y + 1 + (cote - hauteurPokemon) / 2, multiplicateur, diviseur);
            g.setColor(BORD_MINIATURE);
            g.drawRect(gaucheCadre, y, cote + 1, cote + 1);
            g.setFont(police);
            g.setColor(GRIS_LEGENDE);
            g.drawString(legende, x + (largeurPlace - mesures.stringWidth(legende)) / 2,
                    y + cote + 2 + ESPACE_LEGENDE_MINIATURE + mesures.getAscent());
            x = x + largeurPlace + ESPACE_MINIATURE;
        }
    }

    private void dessinerCase(Graphics2D g, int x, int y, int largeur, int hauteur) {
        g.setColor(FOND_CASE);
        g.fillRect(x, y, largeur, hauteur);
        g.setColor(BORD_CASE);
        g.drawRect(x, y, largeur - 1, hauteur - 1);
    }

    // Les bandes, avec le coin en haut à gauche de la zone visible du sprite placé en (x, y).
    // Un pixel du sprite devient multiplicateur / diviseur pixels à l'écran : l'échelle pour l'indice
    // en grand (diviseur 1), une fraction pour une miniature.
    private void dessinerBandes(Graphics2D g, Bandes bandes, int x, int y, int multiplicateur, int diviseur) {
        for (Bande bande : bandes.getListe()) {
            int gauche = x + (bande.getGauche() - zoneVisible.x) * multiplicateur / diviseur;
            int droite = x + (bande.getDroite() - zoneVisible.x) * multiplicateur / diviseur;
            int haut = y + (bande.getHaut() - zoneVisible.y) * multiplicateur / diviseur;
            int bas = y + (bande.getBas() - zoneVisible.y) * multiplicateur / diviseur;
            int largeur = droite - gauche;
            int hauteur = bas - haut;
            g.setColor(bande.getCouleur());
            g.fillRect(gauche, haut, largeur, hauteur);
            // sur une ligne très fine (ou une colonne très étroite), le bord cacherait la couleur
            if (largeur >= 4 && hauteur >= 4) {
                g.setColor(bande.getCouleur().darker());
                g.drawRect(gauche, haut, largeur - 1, hauteur - 1);
            }
        }
    }

    // la zone visible d'une image de la taille du sprite (la silhouette ou le sprite), placée en (x, y)
    private void dessinerImage(Graphics2D g, BufferedImage dessin, int x, int y, int echelle) {
        Rectangle zone = zoneVisible;
        // agrandissement « pixel art » : chaque pixel devient un carré net, sans flou
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(dessin,
                x, y, x + zone.width * echelle, y + zone.height * echelle,
                zone.x, zone.y, zone.x + zone.width, zone.y + zone.height,
                null);
    }

    private void dessinerPointInterrogation(Graphics2D g, int x, int y, int largeur, int hauteur) {
        Font police = getFont().deriveFont(Font.BOLD, Math.min(largeur, hauteur) * 0.55f);
        // on centre le dessin du « ? » lui-même, plus précis que les mesures de la police
        Rectangle2D forme = police.createGlyphVector(g.getFontRenderContext(), "?").getVisualBounds();
        g.setFont(police);
        g.setColor(GRIS_QUESTION);
        g.drawString("?",
                (float) (x + (largeur - forme.getWidth()) / 2 - forme.getX()),
                (float) (y + (hauteur - forme.getHeight()) / 2 - forme.getY()));
    }
}
