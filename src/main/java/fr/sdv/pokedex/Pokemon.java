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

    // vide si le Pokémon n'a pas d'image
    public String getAdresseImage() {
        return adresseImage;
    }

    public void setAdresseImage(String adresseImage) {
        this.adresseImage = adresseImage;
    }
}
