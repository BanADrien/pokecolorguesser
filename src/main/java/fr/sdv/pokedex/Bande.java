package fr.sdv.pokedex;

import java.awt.Color;

// Une bande du quiz (horizontale ou verticale) : sa place dans l'image (bas et droite exclus),
// sa couleur la plus fréquente et le nombre de pixels visibles qu'elle recouvre
public class Bande {

    private int haut;
    private int bas;
    private int gauche;
    private int droite;
    private Color couleur;
    private int pixelsVisibles;

    public Bande(int haut, int bas, int gauche, int droite, Color couleur, int pixelsVisibles) {
        this.haut = haut;
        this.bas = bas;
        this.gauche = gauche;
        this.droite = droite;
        this.couleur = couleur;
        this.pixelsVisibles = pixelsVisibles;
    }

    public int getHaut() {
        return haut;
    }

    public int getBas() {
        return bas;
    }

    public int getGauche() {
        return gauche;
    }

    public int getDroite() {
        return droite;
    }

    public Color getCouleur() {
        return couleur;
    }

    public int getPixelsVisibles() {
        return pixelsVisibles;
    }

    // le nombre de pixels du rectangle, visibles ou non
    public int getSurface() {
        return (droite - gauche) * (bas - haut);
    }
}
