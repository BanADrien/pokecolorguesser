package fr.sdv.pokedex;

import fr.sdv.pokedex.QuizGame.Result;
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

/** Tests de la logique du quiz : comparaison des noms, vies, déroulement de la partie et score. */
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
        return new QuizGame(List.of(pokemon), 1, new Random(0)).answer(typed) == Result.CORRECT;
    }

    /** Un Pokémon du pool qui n'est pas la bonne réponse (le numéro donné, ou le suivant). */
    private static Pokemon wrong(QuizGame game, List<Pokemon> pool, int id) {
        Pokemon pokemon = pool.get(id - 1);
        return pokemon.id() != game.current().id() ? pokemon : pool.get(id % pool.size());
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
        // limite connue : le symbole est enlevé, donc ♀ et ♂ ne sont pas distingués dans un nom écrit
        assertTrue(accepts(NIDORAN_M, "Nidoran♀"));
    }

    @Test
    void pokemonChosenInTheListIsComparedByNumber() {
        // choisi dans la liste des suggestions, chaque Nidoran est bien distingué de l'autre
        assertEquals(Result.WRONG_RETRY, new QuizGame(List.of(NIDORAN_M), 1, new Random(0)).answer(NIDORAN_F));
        assertEquals(Result.CORRECT, new QuizGame(List.of(NIDORAN_M), 1, new Random(0)).answer(NIDORAN_M));
    }

    @Test
    void newGameStartsAtFirstQuestionWithScoreZeroAndAllLives() {
        QuizGame game = new QuizGame(pool(20), 10, new Random(42));
        assertEquals(10, game.questionCount());
        assertEquals(1, game.questionNumber());
        assertEquals(0, game.score());
        assertEquals(QuizGame.LIVES, game.livesLeft());
        assertEquals(1, game.attempt());
        assertEquals(List.of(), game.wrongGuesses());
        assertFalse(game.isQuestionOver());
        assertFalse(game.isLastQuestion());
    }

    @Test
    void eachPokemonIsAskedOnlyOnce() {
        QuizGame game = new QuizGame(pool(20), 10, new Random(42));
        Set<Integer> asked = new HashSet<>();
        for (int question = 1; question <= game.questionCount(); question++) {
            assertTrue(asked.add(game.current().id()), "le même Pokémon ne revient pas deux fois");
            game.skip();
            if (!game.isLastQuestion()) {
                game.next();
            }
        }
        assertEquals(10, asked.size());
    }

    @Test
    void wrongAnswerCostsALifeWithoutEndingTheQuestion() {
        List<Pokemon> pool = pool(10);
        QuizGame game = new QuizGame(pool, 2, new Random(3));
        Pokemon wrong = wrong(game, pool, 1);

        assertEquals(Result.WRONG_RETRY, game.answer(wrong));

        assertEquals(QuizGame.LIVES - 1, game.livesLeft());
        assertEquals(2, game.attempt(), "deuxième essai");
        assertFalse(game.isQuestionOver(), "on peut réessayer");
        assertEquals(List.of(wrong), game.wrongGuesses());
        assertEquals(0, game.score());
        assertThrows(IllegalStateException.class, game::next, "la question n'est pas finie");
    }

    @Test
    void thirdWrongAnswerLosesTheQuestion() {
        List<Pokemon> pool = pool(10);
        QuizGame game = new QuizGame(pool, 2, new Random(3));

        assertEquals(Result.WRONG_RETRY, game.answer(wrong(game, pool, 1)));
        assertEquals(Result.WRONG_RETRY, game.answer("pas un Pokémon"), "une réponse écrite compte aussi");
        assertEquals(Result.LOST, game.answer(wrong(game, pool, 3)));

        assertEquals(0, game.livesLeft());
        assertTrue(game.isQuestionOver());
        assertEquals(0, game.score());
        assertEquals(2, game.wrongGuesses().size(), "seuls les Pokémon choisis dans la liste sont retenus");
    }

    @Test
    void pokemonFoundAfterErrorsCountsInTheScore() {
        List<Pokemon> pool = pool(10);
        QuizGame game = new QuizGame(pool, 2, new Random(3));

        game.answer(wrong(game, pool, 1));
        game.answer(wrong(game, pool, 2));
        assertEquals(Result.CORRECT, game.answer(game.current()), "trouvé avec la dernière vie");

        assertEquals(1, game.score());
        assertEquals(3, game.attempt(), "trouvé au troisième essai");
        assertEquals(1, game.livesLeft());
        assertTrue(game.isQuestionOver());
    }

    @Test
    void scoreCountsOnlyPokemonFound() {
        List<Pokemon> pool = pool(20);
        QuizGame game = new QuizGame(pool, 3, new Random(7));

        assertEquals(Result.CORRECT, game.answer(game.current()), "bon Pokémon choisi dans la liste");
        assertEquals(1, game.score());
        game.next();

        game.skip();
        assertEquals(1, game.score(), "question passée");
        game.next();

        assertEquals(Result.CORRECT, game.answer(game.current().nameEn().toUpperCase()), "nom anglais écrit");
        assertEquals(2, game.score());
        assertTrue(game.isLastQuestion());
    }

    @Test
    void skipEndsTheQuestionAtOnce() {
        List<Pokemon> pool = pool(5);
        QuizGame game = new QuizGame(pool, 2, new Random(1));
        game.answer(wrong(game, pool, 1));

        game.skip();

        assertTrue(game.isQuestionOver(), "abandon tout de suite, même avec des vies");
        assertEquals(QuizGame.LIVES - 1, game.livesLeft(), "passer ne coûte pas de vie en plus");
        assertEquals(0, game.score());
    }

    @Test
    void noAnswerAfterTheEndOfTheQuestion() {
        List<Pokemon> pool = pool(5);
        QuizGame found = new QuizGame(pool, 2, new Random(1));
        found.answer(found.current());
        assertThrows(IllegalStateException.class, () -> found.answer(found.current()));
        assertThrows(IllegalStateException.class, () -> found.answer("Pokémon 1"));
        assertThrows(IllegalStateException.class, found::skip);
        assertEquals(1, found.score(), "la deuxième réponse ne compte pas");

        QuizGame lost = new QuizGame(pool, 2, new Random(1));
        for (int i = 0; i < QuizGame.LIVES; i++) {
            lost.answer("faux");
        }
        assertThrows(IllegalStateException.class, () -> lost.answer(lost.current()), "plus de vie");
        assertThrows(IllegalStateException.class, lost::skip);
        assertEquals(0, lost.score());
    }

    @Test
    void nextQuestionGivesBackAllLives() {
        List<Pokemon> pool = pool(10);
        QuizGame game = new QuizGame(pool, 2, new Random(3));
        game.answer(wrong(game, pool, 1));
        game.skip();

        game.next();

        assertEquals(QuizGame.LIVES, game.livesLeft());
        assertEquals(List.of(), game.wrongGuesses(), "les erreurs de la question précédente sont oubliées");
        assertFalse(game.isQuestionOver());
    }

    @Test
    void sectionsDoubleWithEachLostLife() {
        List<Pokemon> pool = pool(10);
        QuizGame game = new QuizGame(pool, 1, new Random(3));
        assertEquals(8, game.sections(8));
        assertEquals(32, game.sections(32));

        game.answer(wrong(game, pool, 1));
        assertEquals(16, game.sections(8));
        assertEquals(64, game.sections(32), "on peut dépasser le maximum des réglages");

        game.answer(wrong(game, pool, 2));
        assertEquals(32, game.sections(8));
    }

    @Test
    void smallPoolLimitsQuestions() {
        QuizGame game = new QuizGame(pool(3), 10, new Random(1));
        assertEquals(3, game.questionCount());
    }

    @Test
    void emptyPoolIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new QuizGame(List.of(), 10, new Random(1)));
    }

    @Test
    void nextNeedsTheEndOfTheQuestionAndAnotherQuestion() {
        QuizGame game = new QuizGame(pool(5), 2, new Random(1));
        assertThrows(IllegalStateException.class, game::next, "il faut d'abord finir la question");

        game.skip();
        game.next();
        assertEquals(2, game.questionNumber());
        assertFalse(game.isQuestionOver());

        game.skip();
        assertTrue(game.isLastQuestion());
        assertThrows(IllegalStateException.class, game::next, "pas de question après la dernière");
    }
}
