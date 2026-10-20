package fr.sdv.pokedex;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;

// L'image d'un Pokémon découpée en bandes horizontales, de haut en bas : chaque bande garde
// la couleur la plus fréquente à cette hauteur et la largeur du Pokémon à cet endroit.
public class Bandes {

    // en dessous de cette opacité (de 0 à 255), un pixel compte comme transparent
    private static final int OPACITE_MIN = 128;
    // en dessous de cette luminosité (de 0 à 255), un pixel est presque noir, comme les contours
    private static final int LUMINOSITE_MIN = 50;

    private int largeurImage;
    private int hauteurImage;
    private Rectangle zone;
    private int nombreSections;
    private ArrayList<Bande> liste = new ArrayList<>();

    public Bandes(BufferedImage image, int sections, boolean ignorerContoursNoirs) {
        largeurImage = image.getWidth();
        hauteurImage = image.getHeight();
        zone = zoneVisible(image);
        // pas plus de sections que de lignes de pixels
        nombreSections = Math.min(sections, zone.height);
        for (int i = 0; i < nombreSections; i++) {
            int haut = zone.y + i * zone.height / nombreSections;
            int bas = zone.y + (i + 1) * zone.height / nombreSections;
            Bande bande = creerBande(image, haut, bas, ignorerContoursNoirs);
            // une section sans aucun pixel visible n'a pas de bande
            if (bande != null) {
                liste.add(bande);
            }
        }
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

    // le nombre de sections demandé, sans dépasser la hauteur de la zone
    public int getNombreSections() {
        return nombreSections;
    }

    public ArrayList<Bande> getListe() {
        return liste;
    }

    private Bande creerBande(BufferedImage image, int haut, int bas, boolean ignorerContoursNoirs) {
        // Pour chaque groupe de couleurs proches : {nombre de pixels, total du rouge, du vert, du bleu}
        HashMap<Integer, int[]> groupes = new HashMap<>();
        HashMap<Integer, int[]> groupesClairs = new HashMap<>(); // sans les pixels presque noirs
        int gauche = image.getWidth();
        int droite = -1;
        for (int y = haut; y < bas; y++) {
            for (int x = zone.x; x < zone.x + zone.width; x++) {
                Color pixel = new Color(image.getRGB(x, y), true);
                if (pixel.getAlpha() >= OPACITE_MIN) {
                    compter(groupes, pixel);
                    if (!estSombre(pixel)) {
                        compter(groupesClairs, pixel);
                    }
                    gauche = Math.min(gauche, x);
                    droite = Math.max(droite, x);
                }
            }
        }
        if (groupes.isEmpty()) {
            return null;
        }
        // si la bande n'a que des pixels presque noirs, elle reste noire
        if (ignorerContoursNoirs && !groupesClairs.isEmpty()) {
            groupes = groupesClairs;
        }
        int[] plusFrequent = null;
        for (int[] groupe : groupes.values()) {
            if (plusFrequent == null || groupe[0] > plusFrequent[0]) {
                plusFrequent = groupe;
            }
        }
        int nombre = plusFrequent[0];
        Color couleur = new Color(plusFrequent[1] / nombre, plusFrequent[2] / nombre, plusFrequent[3] / nombre);
        return new Bande(haut, bas, gauche, droite + 1, couleur);
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
