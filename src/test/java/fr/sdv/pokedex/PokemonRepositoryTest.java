package fr.sdv.pokedex;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/** Tests de la lecture du fichier cache (aucun appel à PokéAPI). */
class PokemonRepositoryTest {

    private static final String SPRITES = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/";

    @Test
    void validCacheIsReadWithoutDownloading(@TempDir Path folder) throws Exception {
        Path cacheFile = folder.resolve("pokedex.csv");
        Files.write(cacheFile, List.of(
                PokemonRepository.HEADER,
                "1;Bulbizarre;Bulbasaur;1;Plante/Poison;0.7;6.9;45;49;49;65;65;45;" + SPRITES + "1.png",
                "122;M. Mime;Mr. Mime;1;Psy/Fée;1.3;54.5;40;45;65;100;120;90;" + SPRITES + "122.png",
                "25;Pikachu;Pikachu;1;Électrik;0.4;6.0;35;55;40;50;50;90;"), // sans image
                StandardCharsets.UTF_8);

        // le suivi du téléchargement n'est appelé que si l'on télécharge depuis PokéAPI
        List<Pokemon> pokemons = new PokemonRepository(cacheFile)
                .loadAll((done, total) -> fail("le cache est valide : rien ne doit être téléchargé"));

        assertEquals(3, pokemons.size());
        Pokemon bulbizarre = new Pokemon(1, "Bulbizarre", "Bulbasaur", 1, List.of("Plante", "Poison"), 0.7, 6.9,
                45, 49, 49, 65, 65, 45, SPRITES + "1.png");
        assertEquals(bulbizarre, pokemons.get(0));
        assertEquals(318, pokemons.get(0).total());
        assertEquals("M. Mime", pokemons.get(1).nameFr());
        assertEquals(List.of("Psy", "Fée"), pokemons.get(1).types());
        assertEquals(List.of("Électrik"), pokemons.get(2).types());
        assertEquals("", pokemons.get(2).spriteUrl());
    }
}
