package fr.sdv.pokedex;

import java.awt.Color;

// Une bande du quiz (une ligne horizontale ou une colonne verticale) :
// sa place dans l'image (bas et droite exclus) et sa couleur la plus fréquente
public class Bande {

    private int haut;
    private int bas;
    private int gauche;
    private int droite;
    private Color couleur;

    public Bande(int haut, int bas, int gauche, int droite, Color couleur) {
        this.haut = haut;
        this.bas = bas;
        this.gauche = gauche;
        this.droite = droite;
        this.couleur = couleur;
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
}
