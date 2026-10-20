package fr.sdv.pokedex;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SuggestionsTest {

    private Pokemon salameche = new Pokemon(4, "Salamèche", "Charmander", 1);
    private Pokemon reptincel = new Pokemon(5, "Reptincel", "Charmeleon", 1);
    private Pokemon dracaufeu = new Pokemon(6, "Dracaufeu", "Charizard", 1);
    private Pokemon chenipan = new Pokemon(10, "Chenipan", "Caterpie", 1);
    private Pokemon chrysacier = new Pokemon(11, "Chrysacier", "Metapod", 1);
    private Pokemon pikachu = new Pokemon(25, "Pikachu", "Pikachu", 1);
    private Pokemon chetiflor = new Pokemon(69, "Chétiflor", "Bellsprout", 1);
    private Pokemon germignon = new Pokemon(152, "Germignon", "Chikorita", 2);
    private Pokemon pichu = new Pokemon(172, "Pichu", "Pichu", 2);
    private Pokemon chartor = new Pokemon(324, "Chartor", "Torkoal", 3);

    private Suggestions creerSuggestions() {
        ArrayList<Pokemon> pokemons = new ArrayList<>();
        pokemons.add(salameche);
        pokemons.add(reptincel);
        pokemons.add(dracaufeu);
        pokemons.add(chenipan);
        pokemons.add(chrysacier);
        pokemons.add(pikachu);
        pokemons.add(chetiflor);
        pokemons.add(germignon);
        pokemons.add(pichu);
        pokemons.add(chartor);
        return new Suggestions(pokemons);
    }

    // les noms français trouvés, séparés par des espaces, pour comparer facilement
    private String noms(ArrayList<Pokemon> pokemons) {
        String texte = "";
        for (Pokemon pokemon : pokemons) {
            if (!texte.isEmpty()) {
                texte = texte + " ";
            }
            texte = texte + pokemon.getNom();
        }
        return texte;
    }

    private String chercher(String texte) {
        return noms(creerSuggestions().chercher(texte, new ArrayList<>()));
    }

    @Test
    void normaliserEnleveMajusculesAccentsEtPonctuation() {
        assertEquals("electrik", Suggestions.normaliser("  Électrik "));
        assertEquals("mmime", Suggestions.normaliser("M. Mime"));
        assertEquals("nidoran", Suggestions.normaliser("Nidoran♀"));
        assertEquals("farfetchd", Suggestions.normaliser("Farfetch’d"));
        assertEquals("porygon2", Suggestions.normaliser("Porygon2"));
    }

    @Test
    void verificationDuDebutDuNom() {
        assertTrue(Suggestions.commencePar("Dracaufeu", "DRACAU"));
        assertTrue(Suggestions.commencePar("Dracaufeu", "  dràc "));
        assertTrue(Suggestions.commencePar("M. Mime", "mmi"));
        assertTrue(Suggestions.commencePar("Mr. Mime", "Mr Mi"));
        assertFalse(Suggestions.commencePar("Dracaufeu", "caufeu"));
        assertFalse(Suggestions.commencePar("Dracaufeu", "dracofeu"));
    }

    @Test
    void suggestionsParNomFrancaisOuAnglais() {
        assertEquals("Salamèche", chercher("sala"));
        assertEquals("Dracaufeu", chercher("chari"));
        assertEquals("Pichu Pikachu", chercher("PI"));
        assertEquals("", chercher("caufeu"));
        assertEquals("", chercher(". ?"));
    }

    @Test
    void nomsFrancaisDabordPuisNomsAnglaisDansLOrdreAlphabetique() {
        assertEquals("Chartor Chenipan Chétiflor Chrysacier Dracaufeu Germignon Reptincel Salamèche",
                chercher("ch"));

        // les Pokémon déjà proposés à tort ne sont plus suggérés
        ArrayList<Pokemon> exclus = new ArrayList<>();
        exclus.add(chenipan);
        exclus.add(dracaufeu);
        assertEquals("Chartor Chétiflor Chrysacier Germignon Reptincel Salamèche",
                noms(creerSuggestions().chercher("ch", exclus)));
    }
}
