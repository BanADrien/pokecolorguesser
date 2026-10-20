package fr.sdv.pokedex;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests de la logique du quiz : comparaison des noms, score, propositions du QCM. */
class QuizGameTest {

    private static final Pokemon DRACAUFEU = pokemon(6, "Dracaufeu", "Charizard");
    private static final Pokemon NIDORAN_F = pokemon(29, "Nidoran♀", "Nidoran♀");
    private static final Pokemon NIDORAN_M = pokemon(32, "Nidoran♂", "Nidoran♂");
    private static final Pokemon CANARTICHO = pokemon(83, "Canarticho", "Farfetch’d");
    private static final Pokemon M_MIME = pokemon(122, "M. Mime", "Mr. Mime");

    /** Un Pokémon de test : pour le quiz, seuls le numéro et les noms comptent. */
    private static Pokemon pokemon(int id, String nameFr, String nameEn) {
        return new Pokemon(id, nameFr, nameEn, 1, List.of("Normal"), 1.0, 10.0,
                50, 50, 50, 50, 50, 50, "https://example.org/" + id + ".png");
    }

    /** Des Pokémon numérotés de 1 à size. */
    private static List<Pokemon> pool(int size) {
        List<Pokemon> pool = new ArrayList<>();
        for (int id = 1; id <= size; id++) {
            pool.add(pokemon(id, "Pokémon " + id, "Pokemon " + id));
        }
        return pool;
    }

    /** Une partie d'une seule question sur ce Pokémon, avec une réponse écrite. */
    private static boolean accepts(Pokemon pokemon, String typed) {
        return new QuizGame(List.of(pokemon), 1, new Random(0)).answer(typed);
    }

    @Test
    void normalizeIgnoresCaseAccentsSpacesAndPunctuation() {
        assertEquals("mmime", QuizGame.normalize("m. mime"));
        assertEquals("mmime", QuizGame.normalize("M. Mime"));
        assertEquals("dracaufeu", QuizGame.normalize("DRACAUFEU"));
        assertEquals("electrik", QuizGame.normalize("  Électrik "));
        assertEquals("nidoran", QuizGame.normalize("Nidoran♀"));
    }

    @Test
    void typedNameAcceptsFrenchOrEnglishName() {
        assertTrue(accepts(DRACAUFEU, "DRACAUFEU"));
        assertTrue(accepts(DRACAUFEU, "charizard"));
        assertTrue(accepts(DRACAUFEU, "  dràcaufeu  "), "accent en trop et espaces autour");
        assertTrue(accepts(M_MIME, "m. mime"));
        assertTrue(accepts(M_MIME, "mr mime"));
        assertTrue(accepts(CANARTICHO, "canarticho"));
        assertTrue(accepts(CANARTICHO, "farfetchd"), "l'apostrophe est ignorée");
    }

    @Test
    void typedNameRefusesOtherNames() {
        assertFalse(accepts(DRACAUFEU, "dracofeu"));
        assertFalse(accepts(DRACAUFEU, "salameche"));
        assertFalse(accepts(DRACAUFEU, ""));
    }

    @Test
    void nidoranSymbolIsIgnored() {
        assertTrue(accepts(NIDORAN_F, "Nidoran♀"));
        assertTrue(accepts(NIDORAN_F, "nidoran"));
        // limite connue : le symbole est enlevé, donc ♀ et ♂ ne sont pas distingués
        assertTrue(accepts(NIDORAN_M, "Nidoran♀"));
    }

    @Test
    void newGameStartsAtFirstQuestionWithScoreZero() {
        QuizGame game = new QuizGame(pool(20), 10, new Random(42));
        assertEquals(10, game.questionCount());
        assertEquals(1, game.questionNumber());
        assertEquals(0, game.score());
        assertFalse(game.isAnswered());
        assertFalse(game.isLastQuestion());
    }

    @Test
    void eachQuestionHasFourDistinctChoicesIncludingTheAnswer() {
        QuizGame game = new QuizGame(pool(20), 10, new Random(42));
        Set<Integer> asked = new HashSet<>();
        for (int question = 1; question <= game.questionCount(); question++) {
            List<Pokemon> choices = game.choices();
            assertEquals(4, choices.size());
            assertEquals(4, choices.stream().map(Pokemon::id).distinct().count(), "propositions distinctes");
            assertTrue(choices.contains(game.current()), "la bonne réponse fait partie des propositions");
            assertTrue(asked.add(game.current().id()), "le même Pokémon ne revient pas deux fois");
            game.skip();
            if (!game.isLastQuestion()) {
                game.next();
            }
        }
        assertEquals(10, asked.size());
    }

    @Test
    void scoreCountsOnlyCorrectAnswers() {
        QuizGame game = new QuizGame(pool(20), 3, new Random(7));

        assertTrue(game.answer(game.current()), "bonne réponse en QCM");
        assertEquals(1, game.score());
        game.next();

        Pokemon wrong = game.choices().stream()
                .filter(p -> p.id() != game.current().id())
                .findFirst()
                .orElseThrow();
        assertFalse(game.answer(wrong), "mauvaise réponse en QCM");
        assertEquals(1, game.score());
        game.next();

        assertTrue(game.answer(game.current().nameEn().toUpperCase()), "bonne réponse écrite, en anglais");
        assertEquals(2, game.score());
        assertTrue(game.isLastQuestion());
    }

    @Test
    void skipCountsAsWrongAnswer() {
        QuizGame game = new QuizGame(pool(5), 2, new Random(1));
        game.skip();
        assertTrue(game.isAnswered());
        assertEquals(0, game.score());
    }

    @Test
    void smallPoolLimitsQuestionsAndChoices() {
        QuizGame game = new QuizGame(pool(3), 10, new Random(1));
        assertEquals(3, game.questionCount());
        assertEquals(3, game.choices().size());
    }

    @Test
    void emptyPoolIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new QuizGame(List.of(), 10, new Random(1)));
    }

    @Test
    void answeringTwiceIsRefused() {
        QuizGame game = new QuizGame(pool(5), 2, new Random(1));
        game.answer(game.current());
        assertThrows(IllegalStateException.class, () -> game.answer(game.current()));
        assertThrows(IllegalStateException.class, () -> game.answer("Pokémon 1"));
        assertThrows(IllegalStateException.class, game::skip);
        assertEquals(1, game.score(), "la deuxième réponse ne compte pas");
    }

    @Test
    void nextNeedsAnAnswerAndAnotherQuestion() {
        QuizGame game = new QuizGame(pool(5), 2, new Random(1));
        assertThrows(IllegalStateException.class, game::next, "il faut d'abord répondre");

        game.skip();
        game.next();
        assertEquals(2, game.questionNumber());
        assertFalse(game.isAnswered());

        game.skip();
        assertTrue(game.isLastQuestion());
        assertThrows(IllegalStateException.class, game::next, "pas de question après la dernière");
    }
}
