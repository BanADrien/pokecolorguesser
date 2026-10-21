package fr.sdv.pokedex;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;

// Les appels à PokéAPI (https://pokeapi.co), une API gratuite et sans clé
public class ApiPokemon {

    private static final String ADRESSE_API = "https://pokeapi.co/api/v2/";
    private static final int NOMBRE_ESSAIS = 3;

    // Jackson lit le JSON
    private ObjectMapper lecteurJson = new ObjectMapper();
    private HashMap<String, String> typesEnFrancais = new HashMap<>();

    public ApiPokemon() {
        typesEnFrancais.put("normal", "Normal");
        typesEnFrancais.put("fighting", "Combat");
        typesEnFrancais.put("flying", "Vol");
        typesEnFrancais.put("poison", "Poison");
        typesEnFrancais.put("ground", "Sol");
        typesEnFrancais.put("rock", "Roche");
        typesEnFrancais.put("bug", "Insecte");
        typesEnFrancais.put("ghost", "Spectre");
        typesEnFrancais.put("steel", "Acier");
        typesEnFrancais.put("fire", "Feu");
        typesEnFrancais.put("water", "Eau");
        typesEnFrancais.put("grass", "Plante");
        typesEnFrancais.put("electric", "Électrik");
        typesEnFrancais.put("psychic", "Psy");
        typesEnFrancais.put("ice", "Glace");
        typesEnFrancais.put("dragon", "Dragon");
        typesEnFrancais.put("dark", "Ténèbres");
        typesEnFrancais.put("fairy", "Fée");
    }

    // les numéros des Pokémon apparus dans une génération
    public ArrayList<Integer> numerosDeLaGeneration(int generation) throws IOException {
        JsonNode reponse = lireJson(ADRESSE_API + "generation/" + generation + "/");
        ArrayList<Integer> numeros = new ArrayList<>();
        for (JsonNode espece : reponse.path("pokemon_species")) {
            numeros.add(numeroDansAdresse(espece.path("url").asText()));
        }
        return numeros;
    }

    public Pokemon telechargerPokemon(int numero, int generation) throws IOException {
        // l'espèce donne les noms traduits, la forme par défaut donne les types et les statistiques
        JsonNode espece = lireJson(ADRESSE_API + "pokemon-species/" + numero + "/");
        JsonNode forme = lireJson(adresseFormeParDefaut(espece));

        Pokemon pokemon = new Pokemon(espece.path("id").asInt(), nomDansLaLangue(espece, "fr"),
                nomDansLaLangue(espece, "en"), generation);
        for (JsonNode type : forme.path("types")) {
            pokemon.ajouterType(traduireType(type.path("type").path("name").asText()));
        }
        // l'API donne la taille en décimètres et le poids en hectogrammes
        pokemon.setTaille(forme.path("height").asInt() / 10.0);
        pokemon.setPoids(forme.path("weight").asInt() / 10.0);
        for (JsonNode statistique : forme.path("stats")) {
            ajouterStatistique(pokemon, statistique.path("stat").path("name").asText(),
                    statistique.path("base_stat").asInt());
        }
        // "front_default" vaut null quand le Pokémon n'a pas d'image
        JsonNode image = forme.path("sprites").path("front_default");
        if (image.isTextual()) {
            pokemon.setAdresseImage(image.asText());
        }
        return pokemon;
    }

    private void ajouterStatistique(Pokemon pokemon, String nom, int valeur) {
        switch (nom) {
            case "hp":
                pokemon.setPv(valeur);
                break;
            case "attack":
                pokemon.setAttaque(valeur);
                break;
            case "defense":
                pokemon.setDefense(valeur);
                break;
            case "special-attack":
                pokemon.setAttaqueSpeciale(valeur);
                break;
            case "special-defense":
                pokemon.setDefenseSpeciale(valeur);
                break;
            case "speed":
                pokemon.setVitesse(valeur);
                break;
        }
    }

    private String traduireType(String type) {
        String traduction = typesEnFrancais.get(type);
        if (traduction == null) {
            return type;
        }
        return traduction;
    }

    // le nom dans la langue demandée ("fr" ou "en"), sinon le nom technique ("pikachu")
    private String nomDansLaLangue(JsonNode espece, String langue) {
        for (JsonNode nom : espece.path("names")) {
            if (nom.path("language").path("name").asText().equals(langue)) {
                return nom.path("name").asText();
            }
        }
        return espece.path("name").asText();
    }

    // une espèce peut avoir plusieurs formes (les méga-évolutions par exemple) : on prend celle par défaut
    private String adresseFormeParDefaut(JsonNode espece) {
        for (JsonNode forme : espece.path("varieties")) {
            if (forme.path("is_default").asBoolean()) {
                return forme.path("pokemon").path("url").asText();
            }
        }
        return ADRESSE_API + "pokemon/" + espece.path("id").asInt() + "/";
    }

    // "https://pokeapi.co/api/v2/pokemon-species/25/" donne 25
    private int numeroDansAdresse(String adresse) {
        String[] morceaux = adresse.split("/");
        return Integer.parseInt(morceaux[morceaux.length - 1]);
    }

    private JsonNode lireJson(String adresse) throws IOException {
        return lecteurJson.readTree(telecharger(adresse));
    }

    // télécharge une adresse (JSON ou image), avec 3 essais en cas d'erreur
    public byte[] telecharger(String adresse) throws IOException {
        for (int essai = 1; essai < NOMBRE_ESSAIS; essai++) {
            try {
                return telechargerUneFois(adresse);
            } catch (IOException e) {
                // souci de réseau ou serveur surchargé : on attend un peu avant de réessayer
                attendre(1000 * essai);
            }
        }
        // dernier essai : cette fois, l'erreur est transmise
        return telechargerUneFois(adresse);
    }

    private byte[] telechargerUneFois(String adresse) throws IOException {
        HttpURLConnection connexion = (HttpURLConnection) URI.create(adresse).toURL().openConnection();
        connexion.setConnectTimeout(10000);
        connexion.setReadTimeout(30000);
        connexion.setRequestProperty("User-Agent", "pokedex-java-cours");
        // réponse compressée : environ 30 fois plus légère
        connexion.setRequestProperty("Accept-Encoding", "gzip");

        int code = connexion.getResponseCode();
        if (code != 200) {
            throw new IOException("Erreur HTTP " + code + " sur " + adresse);
        }
        byte[] octets;
        // on lit toute la réponse : la connexion peut alors resservir pour la requête suivante
        try (InputStream entree = connexion.getInputStream()) {
            octets = entree.readAllBytes();
        }
        if ("gzip".equalsIgnoreCase(connexion.getContentEncoding())) {
            try (GZIPInputStream decompression = new GZIPInputStream(new ByteArrayInputStream(octets))) {
                octets = decompression.readAllBytes();
            }
        }
        return octets;
    }

    private void attendre(int millisecondes) {
        try {
            Thread.sleep(millisecondes);
        } catch (InterruptedException e) {
            // réveillé plus tôt : on réessaie tout de suite
        }
    }
}
