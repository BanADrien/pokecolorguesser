package fr.sdv.pokedex;

import java.util.ArrayList;
import java.util.Collections;

// Le déroulement d'une partie de quiz (questions, vies, score), sans interface graphique
public class PartieQuiz {

    // le nombre d'essais pour chaque Pokémon
    public static final int VIES = 3;

    private ArrayList<Pokemon> questions = new ArrayList<>();
    private ArrayList<Pokemon> mauvaisesReponses = new ArrayList<>(); // pour la question en cours
    private int numeroQuestion = 1;
    private int score = 0;
    private int vies = VIES;
    private boolean questionFinie = false;

    public PartieQuiz(ArrayList<Pokemon> pokemons, int nombreQuestions) {
        ArrayList<Pokemon> melange = new ArrayList<>(pokemons);
        Collections.shuffle(melange);
        for (int i = 0; i < nombreQuestions && i < melange.size(); i++) {
            questions.add(melange.get(i));
        }
    }

    // le Pokémon à deviner
    public Pokemon getPokemon() {
        return questions.get(numeroQuestion - 1);
    }

    public int getNumeroQuestion() {
        return numeroQuestion;
    }

    public int getNombreQuestions() {
        return questions.size();
    }

    public int getScore() {
        return score;
    }

    public int getVies() {
        return vies;
    }

    // le numéro de l'essai en cours (1 pour le premier), ou de celui qui a trouvé le Pokémon
    public int getEssai() {
        return VIES - vies + 1;
    }

    public ArrayList<Pokemon> getMauvaisesReponses() {
        return mauvaisesReponses;
    }

    public boolean isQuestionFinie() {
        return questionFinie;
    }

    public boolean isDerniereQuestion() {
        return numeroQuestion == questions.size();
    }

    // le nombre de bandes à montrer : celui des réglages, doublé à chaque vie perdue (8, puis 16, puis 32)
    public int nombreDeBandes(int bandesDeDepart) {
        int bandes = bandesDeDepart;
        for (int i = vies; i < VIES; i++) {
            bandes = bandes * 2;
        }
        return bandes;
    }

    // renvoie true si c'est le bon Pokémon ; sinon une vie est perdue
    public boolean repondre(Pokemon choix) {
        if (choix.getId() == getPokemon().getId()) {
            score++;
            questionFinie = true;
            return true;
        }
        vies--;
        if (vies == 0) {
            questionFinie = true;
        }
        if (!mauvaisesReponses.contains(choix)) {
            mauvaisesReponses.add(choix);
        }
        return false;
    }

    // abandonne la question tout de suite, même s'il reste des vies
    public void passer() {
        questionFinie = true;
    }

    public void questionSuivante() {
        numeroQuestion++;
        questionFinie = false;
        vies = VIES;
        mauvaisesReponses.clear();
    }
}
