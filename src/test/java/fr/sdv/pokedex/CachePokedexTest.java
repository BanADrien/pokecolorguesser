package fr.sdv.pokedex;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CachePokedexTest {

    // @TempDir : JUnit crée un dossier vide pour le test, puis le supprime
    @Test
    void leFichierEstEcritPuisReluAuNouveauFormat(@TempDir Path dossier) throws IOException {
        Pokemon dracaufeu = new Pokemon(6, "Dracaufeu", "Charizard", 1);
        dracaufeu.ajouterType("Feu");
        dracaufeu.ajouterType("Vol");
        dracaufeu.setAdresseImage("https://exemple.fr/6.png");
        Pokemon sansImage = new Pokemon(808, "Meltan", "Meltan", 7);
        sansImage.ajouterType("Acier");
        ArrayList<Pokemon> pokemons = new ArrayList<>();
        pokemons.add(dracaufeu);
        pokemons.add(sansImage);

        Path fichier = dossier.resolve("cache").resolve("pokedex.csv");
        CachePokedex cache = new CachePokedex(fichier);
        cache.ecrire(pokemons);

        List<String> lignes = Files.readAllLines(fichier, StandardCharsets.UTF_8);
        assertEquals("id;nom;nom_anglais;generation;types;image", lignes.get(0));
        assertEquals("6;Dracaufeu;Charizard;1;Feu/Vol;https://exemple.fr/6.png", lignes.get(1));
        assertEquals("808;Meltan;Meltan;7;Acier;", lignes.get(2));

        ArrayList<Pokemon> relus = cache.lire();
        assertEquals(2, relus.size());
        Pokemon relu = relus.get(0);
        assertEquals(6, relu.getId());
        assertEquals("Dracaufeu", relu.getNom());
        assertEquals("Charizard", relu.getNomAnglais());
        assertEquals(1, relu.getGeneration());
        assertEquals("Feu / Vol", relu.getTypesTexte());
        assertEquals("https://exemple.fr/6.png", relu.getAdresseImage());
        assertEquals("", relus.get(1).getAdresseImage());
    }

    @Test
    void unFichierALAncienFormatEstRefuse(@TempDir Path dossier) throws IOException {
        // l'en-tête des premières versions, avec les statistiques : le fichier doit être re-téléchargé
        ArrayList<String> lignes = new ArrayList<>();
        lignes.add("id;nom;nom_anglais;generation;types;taille_m;poids_kg;pv;attaque;defense;atq_spe;def_spe;"
                + "vitesse;image");
        lignes.add("25;Pikachu;Pikachu;1;Électrik;0.4;6.0;35;55;40;50;50;90;https://exemple.fr/25.png");
        Path fichier = dossier.resolve("pokedex.csv");
        Files.write(fichier, lignes, StandardCharsets.UTF_8);

        // null : ChargementPokedex télécharge alors tout à nouveau
        assertNull(new CachePokedex(fichier).lire());
        // pareil quand le fichier n'existe pas encore
        assertNull(new CachePokedex(dossier.resolve("absent.csv")).lire());
    }
}
