package fr.sdv.pokedex;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;

// L'image d'un Pokémon découpée en bandes horizontales (de haut en bas) ou verticales (de gauche à droite).
// Chaque bande garde la couleur la plus fréquente de sa section et la place qu'y prend le Pokémon :
// sa largeur pour une bande horizontale, sa hauteur pour une bande verticale.
public class Bandes {

    // en dessous de cette opacité (de 0 à 255), un pixel compte comme transparent
    private static final int OPACITE_MIN = 128;
    // en dessous de cette luminosité (de 0 à 255), un pixel est presque noir, comme les contours
    private static final int LUMINOSITE_MIN = 50;

    private int largeurImage;
    private int hauteurImage;
    private Rectangle zone;
    private boolean vertical;
    private int nombreSections;
    private ArrayList<Bande> liste = new ArrayList<>();

    public Bandes(BufferedImage image, int sections, boolean ignorerContoursNoirs, boolean vertical) {
        largeurImage = image.getWidth();
        hauteurImage = image.getHeight();
        this.vertical = vertical;
        zone = zoneVisible(image);
        // pas plus de sections que de colonnes (ou de lignes) de pixels
        if (vertical) {
            nombreSections = Math.min(sections, zone.width);
        } else {
            nombreSections = Math.min(sections, zone.height);
        }
        for (int i = 0; i < nombreSections; i++) {
            Bande bande;
            if (vertical) {
                int gauche = zone.x + i * zone.width / nombreSections;
                int droite = zone.x + (i + 1) * zone.width / nombreSections;
                bande = creerBande(image, gauche, droite, zone.y, zone.y + zone.height, ignorerContoursNoirs);
            } else {
                int haut = zone.y + i * zone.height / nombreSections;
                int bas = zone.y + (i + 1) * zone.height / nombreSections;
                bande = creerBande(image, zone.x, zone.x + zone.width, haut, bas, ignorerContoursNoirs);
            }
            // une section sans aucun pixel visible n'a pas de bande
            if (bande != null) {
                liste.add(bande);
            }
        }
    }

    // true si les bandes verticales laissent moins de vide que les horizontales (à égalité : horizontales)
    public static boolean verticalPlusFidele(BufferedImage image, int sections, boolean ignorerContoursNoirs) {
        Bandes horizontales = new Bandes(image, sections, ignorerContoursNoirs, false);
        Bandes verticales = new Bandes(image, sections, ignorerContoursNoirs, true);
        return verticales.getVide() < horizontales.getVide();
    }

    // la taille de l'image d'origine (96 × 96) : tous les Pokémon sont dessinés à la même échelle
    public int getLargeurImage() {
        return largeurImage;
    }

    public int getHauteurImage() {
        return hauteurImage;
    }

    // le rectangle qui contient les pixels visibles du Pokémon
    public Rectangle getZone() {
        return zone;
    }

    public boolean isVertical() {
        return vertical;
    }

    // le nombre de sections demandé, sans dépasser la hauteur (ou la largeur) de la zone
    public int getNombreSections() {
        return nombreSections;
    }

    public ArrayList<Bande> getListe() {
        return liste;
    }

    // La part de vide dans les rectangles des bandes, de 0 (aucun vide) à 1 :
    // 1 - (pixels visibles recouverts / surface des rectangles). Moins il y a de vide, plus c'est fidèle.
    public double getVide() {
        int pixelsVisibles = 0;
        int surface = 0;
        for (Bande bande : liste) {
            pixelsVisibles += bande.getPixelsVisibles();
            surface += bande.getSurface();
        }
        if (surface == 0) {
            return 0; // image entièrement transparente
        }
        return 1 - (double) pixelsVisibles / surface;
    }

    // la bande d'une section (gauche, droite, haut et bas de la section ; droite et bas exclus)
    private Bande creerBande(BufferedImage image, int gaucheSection, int droiteSection, int hautSection,
                             int basSection, boolean ignorerContoursNoirs) {
        // Pour chaque groupe de couleurs proches : {nombre de pixels, total du rouge, du vert, du bleu}
        HashMap<Integer, int[]> groupes = new HashMap<>();
        HashMap<Integer, int[]> groupesClairs = new HashMap<>(); // sans les pixels presque noirs
        // le rectangle qui contient les pixels visibles de la section
        int gauche = image.getWidth();
        int haut = image.getHeight();
        int droite = -1;
        int bas = -1;
        int pixelsVisibles = 0;
        for (int y = hautSection; y < basSection; y++) {
            for (int x = gaucheSection; x < droiteSection; x++) {
                Color pixel = new Color(image.getRGB(x, y), true);
                if (pixel.getAlpha() >= OPACITE_MIN) {
                    compter(groupes, pixel);
                    if (!estSombre(pixel)) {
                        compter(groupesClairs, pixel);
                    }
                    pixelsVisibles++;
                    gauche = Math.min(gauche, x);
                    droite = Math.max(droite, x);
                    haut = Math.min(haut, y);
                    bas = Math.max(bas, y);
                }
            }
        }
        if (pixelsVisibles == 0) {
            return null;
        }
        // si la bande n'a que des pixels presque noirs, elle reste noire
        if (ignorerContoursNoirs && !groupesClairs.isEmpty()) {
            groupes = groupesClairs;
        }
        Color couleur = couleurLaPlusFrequente(groupes);
        // une bande verticale garde toute la largeur de sa section, une bande horizontale toute sa hauteur
        if (vertical) {
            return new Bande(haut, bas + 1, gaucheSection, droiteSection, couleur, pixelsVisibles);
        }
        return new Bande(hautSection, basSection, gauche, droite + 1, couleur, pixelsVisibles);
    }

    // la couleur moyenne du groupe qui a le plus de pixels
    private Color couleurLaPlusFrequente(HashMap<Integer, int[]> groupes) {
        int[] plusFrequent = null;
        for (int[] groupe : groupes.values()) {
            if (plusFrequent == null || groupe[0] > plusFrequent[0]) {
                plusFrequent = groupe;
            }
        }
        int nombre = plusFrequent[0];
        return new Color(plusFrequent[1] / nombre, plusFrequent[2] / nombre, plusFrequent[3] / nombre);
    }

    // Les couleurs très proches vont dans le même groupe (32 nuances par canal au lieu de 256),
    // sinon un dégradé compterait comme plein de couleurs différentes.
    private void compter(HashMap<Integer, int[]> groupes, Color pixel) {
        int rouge = pixel.getRed();
        int vert = pixel.getGreen();
        int bleu = pixel.getBlue();
        int cle = (rouge / 8) * 1024 + (vert / 8) * 32 + bleu / 8;
        int[] groupe = groupes.get(cle);
        if (groupe == null) {
            groupe = new int[4];
            groupes.put(cle, groupe);
        }
        groupe[0]++;
        groupe[1] += rouge;
        groupe[2] += vert;
        groupe[3] += bleu;
    }

    // la luminosité vue par l'œil : il voit mieux le vert que le rouge, et le rouge mieux que le bleu
    private boolean estSombre(Color pixel) {
        double luminosite = 0.299 * pixel.getRed() + 0.587 * pixel.getGreen() + 0.114 * pixel.getBlue();
        return luminosite < LUMINOSITE_MIN;
    }

    private Rectangle zoneVisible(BufferedImage image) {
        int gauche = image.getWidth();
        int haut = image.getHeight();
        int droite = -1;
        int bas = -1;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                Color pixel = new Color(image.getRGB(x, y), true);
                if (pixel.getAlpha() >= OPACITE_MIN) {
                    gauche = Math.min(gauche, x);
                    haut = Math.min(haut, y);
                    droite = Math.max(droite, x);
                    bas = Math.max(bas, y);
                }
            }
        }
        if (droite == -1) {
            return new Rectangle(); // image entièrement transparente
        }
        return new Rectangle(gauche, haut, droite - gauche + 1, bas - haut + 1);
    }
}
