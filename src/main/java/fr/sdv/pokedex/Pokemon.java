package fr.sdv.pokedex;

import java.util.ArrayList;

public class Pokemon {

    // le Pokédex va de la génération 1 à la génération 8 (Galar)
    public static final int DERNIERE_GENERATION = 8;

    private int id;
    private String nom;
    private String nomAnglais;
    private int generation;
    private ArrayList<String> types = new ArrayList<>();
    private double taille;
    private double poids;
    private int pv;
    private int attaque;
    private int defense;
    private int attaqueSpeciale;
    private int defenseSpeciale;
    private int vitesse;
    private String adresseImage = "";

    public Pokemon(int id, String nom, String nomAnglais, int generation) {
        this.id = id;
        this.nom = nom;
        this.nomAnglais = nomAnglais;
        this.generation = generation;
    }

    public int getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public String getNomAnglais() {
        return nomAnglais;
    }

    public int getGeneration() {
        return generation;
    }

    public ArrayList<String> getTypes() {
        return types;
    }

    public void ajouterType(String type) {
        types.add(type);
    }

    // "Plante / Poison"
    public String getTypesTexte() {
        return String.join(" / ", types);
    }

    public double getTaille() {
        return taille;
    }

    public void setTaille(double taille) {
        this.taille = taille;
    }

    public double getPoids() {
        return poids;
    }

    public void setPoids(double poids) {
        this.poids = poids;
    }

    public int getPv() {
        return pv;
    }

    public void setPv(int pv) {
        this.pv = pv;
    }

    public int getAttaque() {
        return attaque;
    }

    public void setAttaque(int attaque) {
        this.attaque = attaque;
    }

    public int getDefense() {
        return defense;
    }

    public void setDefense(int defense) {
        this.defense = defense;
    }

    public int getAttaqueSpeciale() {
        return attaqueSpeciale;
    }

    public void setAttaqueSpeciale(int attaqueSpeciale) {
        this.attaqueSpeciale = attaqueSpeciale;
    }

    public int getDefenseSpeciale() {
        return defenseSpeciale;
    }

    public void setDefenseSpeciale(int defenseSpeciale) {
        this.defenseSpeciale = defenseSpeciale;
    }

    public int getVitesse() {
        return vitesse;
    }

    public void setVitesse(int vitesse) {
        this.vitesse = vitesse;
    }

    public int getTotal() {
        return pv + attaque + defense + attaqueSpeciale + defenseSpeciale + vitesse;
    }

    // vide si le Pokémon n'a pas d'image
    public String getAdresseImage() {
        return adresseImage;
    }

    public void setAdresseImage(String adresseImage) {
        this.adresseImage = adresseImage;
    }
}
