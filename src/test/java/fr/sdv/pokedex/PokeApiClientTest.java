package fr.sdv.pokedex;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests de la lecture des réponses de PokéAPI, sans réseau. Les fichiers de src/test/resources/pokeapi/
 * sont de vraies réponses (generation/1, pokemon-species/25, pokemon/25) réduites aux champs utilisés.
 */
class PokeApiClientTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Lit un fichier JSON de src/test/resources/pokeapi/ (Maven le place dans le classpath des tests). */
    private static JsonNode resource(String name) throws IOException {
        try (InputStream in = PokeApiClientTest.class.getResourceAsStream("/pokeapi/" + name)) {
            assertNotNull(in, "fichier de test introuvable : " + name);
            return MAPPER.readTree(in);
        }
    }

    @Test
    void firstGenerationHas151Species() throws IOException {
        List<Integer> ids = PokeApiClient.speciesIds(resource("generation-1.json"));

        assertEquals(151, ids.size());
        assertEquals(IntStream.rangeClosed(1, 151).boxed().toList(), ids.stream().sorted().toList());
    }

    @Test
    void pikachuIsBuiltFromSpeciesAndPokemon() throws IOException {
        Pokemon pikachu = PokeApiClient.toPokemon(resource("pokemon-species-25.json"), resource("pokemon-25.json"), 1);

        Pokemon expected = new Pokemon(25, "Pikachu", "Pikachu", 1, List.of("Électrik"),
                0.4, 6.0, // 4 décimètres, 60 hectogrammes
                35, 55, 40, 50, 50, 90,
                "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/25.png");
        assertEquals(expected, pikachu);
    }

    @Test
    void pikachuDefaultVarietyIsChosen() throws IOException {
        assertEquals("https://pokeapi.co/api/v2/pokemon/25/",
                PokeApiClient.defaultVarietyUrl(resource("pokemon-species-25.json")));
    }

    @Test
    void defaultVarietyIsFoundEvenWhenNotFirst() throws IOException {
        JsonNode species = MAPPER.readTree("""
                {"id": 6, "varieties": [
                  {"is_default": false, "pokemon": {"url": "https://pokeapi.co/api/v2/pokemon/10034/"}},
                  {"is_default": true, "pokemon": {"url": "https://pokeapi.co/api/v2/pokemon/6/"}}
                ]}
                """);
        assertEquals("https://pokeapi.co/api/v2/pokemon/6/", PokeApiClient.defaultVarietyUrl(species));
    }

    @Test
    void withoutVarietiesTheUrlIsBuiltFromTheId() throws IOException {
        JsonNode species = MAPPER.readTree("""
                {"id": 6}
                """);
        assertEquals("https://pokeapi.co/api/v2/pokemon/6/", PokeApiClient.defaultVarietyUrl(species));
    }

    @Test
    void namesAreTranslatedAndTypesConverted() throws IOException {
        JsonNode species = MAPPER.readTree("""
                {"id": 6, "name": "charizard", "names": [
                  {"language": {"name": "ja"}, "name": "リザードン"},
                  {"language": {"name": "en"}, "name": "Charizard"},
                  {"language": {"name": "fr"}, "name": "Dracaufeu"}
                ]}
                """);
        JsonNode pokemon = MAPPER.readTree("""
                {"height": 17, "weight": 905,
                 "types": [{"type": {"name": "fire"}}, {"type": {"name": "flying"}}],
                 "stats": [{"base_stat": 100, "stat": {"name": "speed"}}],
                 "sprites": {"front_default": null}}
                """);

        Pokemon dracaufeu = PokeApiClient.toPokemon(species, pokemon, 1);

        assertEquals(6, dracaufeu.id());
        assertEquals("Dracaufeu", dracaufeu.nameFr());
        assertEquals("Charizard", dracaufeu.nameEn());
        assertEquals(List.of("Feu", "Vol"), dracaufeu.types());
        assertEquals(1.7, dracaufeu.heightM());
        assertEquals(90.5, dracaufeu.weightKg());
        assertEquals(100, dracaufeu.speed());
        assertEquals(0, dracaufeu.hp(), "statistique absente : 0");
        assertEquals("", dracaufeu.spriteUrl(), "front_default vaut null : pas d'image");
    }

    @Test
    void missingTranslationFallsBackToSpeciesName() throws IOException {
        JsonNode species = MAPPER.readTree("""
                {"id": 1, "name": "bulbasaur", "names": []}
                """);
        JsonNode pokemon = MAPPER.readTree("""
                {"types": [{"type": {"name": "inconnu"}}]}
                """);

        Pokemon bulbizarre = PokeApiClient.toPokemon(species, pokemon, 1);

        assertEquals("bulbasaur", bulbizarre.nameFr());
        assertEquals("bulbasaur", bulbizarre.nameEn());
        assertEquals(List.of("inconnu"), bulbizarre.types(), "type sans traduction : gardé tel quel");
    }
}
