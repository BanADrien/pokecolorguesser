package fr.sdv.pokedex;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/** Le déroulement d'une partie de quiz (questions, réponses, score), sans aucune interface graphique. */
public final class QuizGame {

    private static final int CHOICE_COUNT = 4;

    private final List<Pokemon> pool;
    private final List<Pokemon> questions;
    private final Random random;

    private int index = 0;
    private int score = 0;
    private boolean answered = false;
    private List<Pokemon> choices;

    /** pool : les Pokémon autorisés (générations choisies), questionCount : la longueur de la partie. */
    public QuizGame(List<Pokemon> pool, int questionCount, Random random) {
        if (pool.isEmpty()) {
            throw new IllegalArgumentException("aucun Pokémon à deviner");
        }
        this.pool = List.copyOf(pool);
        this.random = random;
        List<Pokemon> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled, random);
        this.questions = List.copyOf(shuffled.subList(0, Math.min(questionCount, shuffled.size())));
        this.choices = drawChoices();
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

    public int score() {
        return score;
    }

    public boolean isAnswered() {
        return answered;
    }

    public boolean isLastQuestion() {
        return index == questions.size() - 1;
    }

    /** Les propositions du QCM : la bonne réponse mélangée avec des Pokémon pris au hasard. */
    public List<Pokemon> choices() {
        return choices;
    }

    /** Réponse en mode QCM. Renvoie true si c'est le bon Pokémon. */
    public boolean answer(Pokemon choice) {
        return check(choice.id() == current().id());
    }

    /** Réponse en mode saisie : le nom français ou anglais, sans tenir compte des majuscules ni des accents. */
    public boolean answer(String typedName) {
        Pokemon pokemon = current();
        String guess = normalize(typedName);
        return check(guess.equals(normalize(pokemon.nameFr())) || guess.equals(normalize(pokemon.nameEn())));
    }

    public void skip() {
        check(false);
    }

    public void next() {
        if (!answered || isLastQuestion()) {
            throw new IllegalStateException("pas de question suivante");
        }
        index++;
        answered = false;
        choices = drawChoices();
    }

    private boolean check(boolean correct) {
        if (answered) {
            throw new IllegalStateException("question déjà répondue");
        }
        answered = true;
        if (correct) {
            score++;
        }
        return correct;
    }

    private List<Pokemon> drawChoices() {
        List<Pokemon> others = new ArrayList<>(pool);
        others.removeIf(p -> p.id() == current().id());
        Collections.shuffle(others, random);
        List<Pokemon> result = new ArrayList<>(others.subList(0, Math.min(CHOICE_COUNT - 1, others.size())));
        result.add(current());
        Collections.shuffle(result, random);
        return List.copyOf(result);
    }

    /** "  Électrik " -> "electrik", "M. Mime" -> "mmime" */
    static String normalize(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "") // enlève les accents
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
    }
}
