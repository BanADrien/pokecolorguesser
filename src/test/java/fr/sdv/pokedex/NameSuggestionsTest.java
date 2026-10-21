package fr.sdv.pokedex;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests des suggestions affichées sous le champ de réponse du quiz. */
class NameSuggestionsTest {

    private static final Pokemon BULBIZARRE = pokemon(1, "Bulbizarre", "Bulbasaur", 1);
    private static final Pokemon SALAMECHE = pokemon(4, "Salamèche", "Charmander", 1);
    private static final Pokemon REPTINCEL = pokemon(5, "Reptincel", "Charmeleon", 1);
    private static final Pokemon DRACAUFEU = pokemon(6, "Dracaufeu", "Charizard", 1);
    private static final Pokemon CHENIPAN = pokemon(10, "Chenipan", "Caterpie", 1);
    private static final Pokemon CHRYSACIER = pokemon(11, "Chrysacier", "Metapod", 1);
    private static final Pokemon PIKACHU = pokemon(25, "Pikachu", "Pikachu", 1);
    private static final Pokemon NIDORAN_F = pokemon(29, "Nidoran♀", "Nidoran♀", 1);
    private static final Pokemon NIDORINA = pokemon(30, "Nidorina", "Nidorina", 1);
    private static final Pokemon NIDORAN_M = pokemon(32, "Nidoran♂", "Nidoran♂", 1);
    private static final Pokemon CHETIFLOR = pokemon(69, "Chétiflor", "Bellsprout", 1);
    private static final Pokemon CANARTICHO = pokemon(83, "Canarticho", "Farfetch’d", 1);
    private static final Pokemon M_MIME = pokemon(122, "M. Mime", "Mr. Mime", 1);
    private static final Pokemon ELECTHOR = pokemon(145, "Électhor", "Zapdos", 1);
    private static final Pokemon GERMIGNON = pokemon(152, "Germignon", "Chikorita", 2);
    private static final Pokemon PICHU = pokemon(172, "Pichu", "Pichu", 2);
    private static final Pokemon CHARTOR = pokemon(324, "Chartor", "Torkoal", 3);

    /** Tous les Pokémon de test, dans l'ordre des numéros (comme dans le Pokédex). */
    private static final List<Pokemon> ALL = List.of(BULBIZARRE, SALAMECHE, REPTINCEL, DRACAUFEU, CHENIPAN,
            CHRYSACIER, PIKACHU, NIDORAN_F, NIDORINA, NIDORAN_M, CHETIFLOR, CANARTICHO, M_MIME, ELECTHOR,
            GERMIGNON, PICHU, CHARTOR);

    /** Un Pokémon de test : pour les suggestions, seuls le numéro, les noms et la génération comptent. */
    private static Pokemon pokemon(int id, String nameFr, String nameEn, int generation) {
        return new Pokemon(id, nameFr, nameEn, generation, List.of("Normal"), 1.0, 10.0,
                50, 50, 50, 50, 50, 50, "https://example.org/" + id + ".png");
    }

    private static List<Pokemon> suggest(String typed) {
        return new NameSuggestions(ALL).suggest(typed);
    }

    @Test
    void frenchNameStartingWithTheTextIsSuggested() {
        assertEquals(List.of(SALAMECHE), suggest("sala"));
        assertEquals(List.of(DRACAUFEU), suggest("Dracau"));
        assertEquals(List.of(BULBIZARRE), suggest("bulbizarre"), "le nom en entier");
    }

    @Test
    void englishNameStartingWithTheTextIsSuggested() {
        assertEquals(List.of(DRACAUFEU), suggest("chari"));
        assertEquals(List.of(BULBIZARRE), suggest("Bulbas"));
    }

    @Test
    void onlyTheStartOfTheNameCounts() {
        assertEquals(List.of(), suggest("caufeu"), "« caufeu » est au milieu de Dracaufeu");
    }

    @Test
    void caseAccentsAndPunctuationAreIgnored() {
        assertEquals(List.of(SALAMECHE), suggest("SALAME"));
        assertEquals(List.of(ELECTHOR), suggest("elec"));
        assertEquals(List.of(ELECTHOR), suggest("ÉLEC"));
        assertEquals(List.of(DRACAUFEU), suggest("  dràcau "), "accent en trop et espaces autour");
        assertEquals(List.of(M_MIME), suggest("m. mi"));
        assertEquals(List.of(M_MIME), suggest("mmi"));
        assertEquals(List.of(M_MIME), suggest("Mr Mi"), "nom anglais sans le point");
        assertEquals(List.of(CANARTICHO), suggest("farfetch'd"), "apostrophe droite ou typographique");
    }

    @Test
    void frenchMatchesComeFirstThenEnglishOnesInFrenchAlphabeticalOrder() {
        assertEquals(List.of(
                        // nom français en « ch » : Chétiflor avant Chrysacier (é se range comme e)
                        CHARTOR, CHENIPAN, CHETIFLOR, CHRYSACIER,
                        // seulement le nom anglais : Charizard, Chikorita, Charmeleon, Charmander
                        DRACAUFEU, GERMIGNON, REPTINCEL, SALAMECHE),
                suggest("ch"));
    }

    @Test
    void pokemonMatchingInBothLanguagesIsSuggestedOnce() {
        assertEquals(List.of(PIKACHU), suggest("pika"));
        assertEquals(List.of(PICHU, PIKACHU), suggest("PI"));
    }

    @Test
    void bothNidoranAreSuggested() {
        List<Pokemon> found = suggest("nido");
        assertTrue(found.contains(NIDORAN_F), "Nidoran♀ proposé");
        assertTrue(found.contains(NIDORAN_M), "Nidoran♂ proposé");
        assertEquals(List.of(NIDORAN_F, NIDORAN_M, NIDORINA), found);
    }

    @Test
    void wrongGuessesAreNoLongerSuggested() {
        NameSuggestions suggestions = new NameSuggestions(ALL);
        List<Pokemon> wrongGuesses = List.of(CHENIPAN, DRACAUFEU);

        assertEquals(List.of(CHARTOR, CHETIFLOR, CHRYSACIER, GERMIGNON, REPTINCEL, SALAMECHE),
                suggestions.suggest("ch", wrongGuesses));
        assertEquals(List.of(), suggestions.suggest("chari", wrongGuesses), "Dracaufeu déjà proposé");
        assertEquals(List.of(NIDORAN_M, NIDORINA), suggestions.suggest("nido", List.of(NIDORAN_F)),
                "seul le Nidoran proposé disparaît");
    }

    @Test
    void noMatchGivesEmptyList() {
        assertEquals(List.of(), suggest("xyz"));
        assertEquals(List.of(), suggest("dracofeu"));
    }

    @Test
    void textWithoutLetterGivesEmptyList() {
        assertEquals(List.of(), suggest(""));
        assertEquals(List.of(), suggest("   "));
        assertEquals(List.of(), suggest(". ?"));
    }

    @Test
    void onlyPokemonOfTheChosenGenerationsAreSuggested() {
        QuizSettings settings = new QuizSettings();
        assertEquals(List.of(PICHU, PIKACHU), new NameSuggestions(settings.pool(ALL)).suggest("pi"));

        settings.setGenerationEnabled(2, false); // Pichu et Germignon sont de la 2e génération
        NameSuggestions suggestions = new NameSuggestions(settings.pool(ALL));

        assertEquals(List.of(PIKACHU), suggestions.suggest("pi"));
        assertEquals(List.of(), suggestions.suggest("germi"));
        assertEquals(List.of(), suggestions.suggest("chiko"), "pas non plus par le nom anglais");
    }
}
