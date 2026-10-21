package fr.sdv.pokedex;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;

// L'image d'un Pokémon découpée en colonnes verticales (de gauche à droite) ou en lignes horizontales
// (de haut en bas). Chaque bande garde la couleur la plus fréquente de sa section et la place qu'y prend
// le Pokémon : sa hauteur pour une colonne, sa largeur pour une ligne.
public class Bandes {

    // en dessous de cette opacité (de 0 à 255), un pixel compte comme transparent
    private static final int OPACITE_MIN = 128;
    // en dessous de cette luminosité (de 0 à 255), un pixel est presque noir, comme les contours
    private static final int LUMINOSITE_MIN = 50;

    private boolean vertical;
    private int nombreSections;
    private ArrayList<Bande> liste = new ArrayList<>();

    public Bandes(BufferedImage image, int sections, boolean ignorerContoursNoirs, boolean vertical) {
        this.vertical = vertical;
        Rectangle zone = zoneVisible(image);
        // pas plus de colonnes que la largeur visible, ni de lignes que la hauteur visible
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

    // true pour des colonnes verticales, false pour des lignes horizontales
    public boolean isVertical() {
        return vertical;
    }

    // le nombre de sections demandé, sans dépasser la largeur (ou la hauteur) visible
    public int getNombreSections() {
        return nombreSections;
    }

    public ArrayList<Bande> getListe() {
        return liste;
    }

    // le plus petit rectangle qui contient tous les pixels visibles du Pokémon
    public static Rectangle zoneVisible(BufferedImage image) {
        int gauche = image.getWidth();
        int haut = image.getHeight();
        int droite = -1;
        int bas = -1;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (estVisible(new Color(image.getRGB(x, y), true))) {
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

    // un pixel assez opaque fait partie du Pokémon (sert aussi pour la silhouette pixelisée)
    public static boolean estVisible(Color pixel) {
        return pixel.getAlpha() >= OPACITE_MIN;
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
        for (int y = hautSection; y < basSection; y++) {
            for (int x = gaucheSection; x < droiteSection; x++) {
                Color pixel = new Color(image.getRGB(x, y), true);
                if (estVisible(pixel)) {
                    compter(groupes, pixel);
                    if (!estSombre(pixel)) {
                        compter(groupesClairs, pixel);
                    }
                    gauche = Math.min(gauche, x);
                    droite = Math.max(droite, x);
                    haut = Math.min(haut, y);
                    bas = Math.max(bas, y);
                }
            }
        }
        if (droite == -1) {
            return null; // aucun pixel visible dans la section
        }
        // si la bande n'a que des pixels presque noirs, elle reste noire
        if (ignorerContoursNoirs && !groupesClairs.isEmpty()) {
            groupes = groupesClairs;
        }
        Color couleur = couleurLaPlusFrequente(groupes);
        // une colonne garde toute la largeur de sa section, une ligne toute sa hauteur
        if (vertical) {
            return new Bande(haut, bas + 1, gaucheSection, droiteSection, couleur);
        }
        return new Bande(hautSection, basSection, gauche, droite + 1, couleur);
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
}
