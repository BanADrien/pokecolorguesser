package fr.sdv.pokedex;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Le déroulement d'une partie de quiz (questions, réponses, vies, score), sans aucune interface graphique.
 * Pour chaque Pokémon, on a LIVES essais : chaque erreur coûte une vie, et la question est perdue à la dernière.
 */
public final class QuizGame {

    /** Le nombre d'essais pour chaque Pokémon. */
    public static final int LIVES = 3;

    /** Ce qui se passe après une réponse. */
    public enum Result {
        /** C'est le bon Pokémon : la question est gagnée. */
        CORRECT,
        /** Mauvaise réponse, mais il reste des vies : on peut réessayer. */
        WRONG_RETRY,
        /** Mauvaise réponse avec la dernière vie : la question est perdue. */
        LOST
    }

    private final List<Pokemon> questions;
    private final List<Pokemon> wrongGuesses = new ArrayList<>(); // pour la question en cours

    private int index = 0;
    private int score = 0;
    private int livesLeft = LIVES;
    private boolean questionOver = false;

    /** pool : les Pokémon autorisés (générations choisies), questionCount : la longueur de la partie. */
    public QuizGame(List<Pokemon> pool, int questionCount, Random random) {
        if (pool.isEmpty()) {
            throw new IllegalArgumentException("aucun Pokémon à deviner");
        }
        List<Pokemon> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled, random);
        this.questions = List.copyOf(shuffled.subList(0, Math.min(questionCount, shuffled.size())));
    }

    public Pokemon current() {
        return questions.get(index);
    }

    /** Numéro de la question en cours, à partir de 1. */
    public int questionNumber() {
        return index + 1;
    }

    public int questionCount() {
        return questions.size();
    }

    /** Le nombre de Pokémon trouvés. */
    public int score() {
        return score;
    }

    /** Les vies qui restent pour la question en cours (de LIVES à 0). */
    public int livesLeft() {
        return livesLeft;
    }

    /** Le numéro de l'essai en cours (1 pour le premier), ou de celui qui a trouvé le Pokémon. */
    public int attempt() {
        return LIVES - livesLeft + 1;
    }

    /** Les Pokémon déjà proposés à tort pour la question en cours. */
    public List<Pokemon> wrongGuesses() {
        return List.copyOf(wrongGuesses);
    }

    /**
     * Le nombre de bandes à montrer : celui des réglages, doublé à chaque vie perdue (8, puis 16, puis 32).
     * ColorBands le limite ensuite à la hauteur visible du sprite.
     */
    public int sections(int baseSections) {
        return baseSections << (LIVES - livesLeft);
    }

    /** true quand la question est finie : Pokémon trouvé, vies épuisées ou question passée. */
    public boolean isQuestionOver() {
        return questionOver;
    }

    public boolean isLastQuestion() {
        return index == questions.size() - 1;
    }

    /** Réponse choisie dans la liste des suggestions. */
    public Result answer(Pokemon choice) {
        Result result = check(choice.id() == current().id());
        if (result != Result.CORRECT && !wrongGuesses.contains(choice)) {
            wrongGuesses.add(choice);
        }
        return result;
    }

    /** Réponse écrite en entier : le nom français ou anglais, sans tenir compte des majuscules ni des accents. */
    public Result answer(String typedName) {
        Pokemon pokemon = current();
        String guess = normalize(typedName);
        return check(guess.equals(normalize(pokemon.nameFr())) || guess.equals(normalize(pokemon.nameEn())));
    }

    /** Abandonne la question tout de suite : elle est perdue, quelles que soient les vies qui restent. */
    public void skip() {
        checkNotOver();
        questionOver = true;
    }

    public void next() {
        if (!questionOver || isLastQuestion()) {
            throw new IllegalStateException("pas de question suivante");
        }
        index++;
        questionOver = false;
        livesLeft = LIVES;
        wrongGuesses.clear();
    }

    private Result check(boolean correct) {
        checkNotOver();
        if (correct) {
            questionOver = true;
            score++;
            return Result.CORRECT;
        }
        livesLeft--;
        if (livesLeft == 0) {
            questionOver = true;
            return Result.LOST;
        }
        return Result.WRONG_RETRY;
    }

    private void checkNotOver() {
        if (questionOver) {
            throw new IllegalStateException("question déjà terminée");
        }
    }

    /** "  Électrik " -> "electrik", "M. Mime" -> "mmime" */
    static String normalize(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "") // enlève les accents
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
    }
}
