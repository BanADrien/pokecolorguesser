package fr.sdv.pokedex;

import java.util.ArrayList;
import java.util.Collections;

// Le déroulement d'une partie de quiz (questions, vies, score), sans interface graphique
public class PartieQuiz {

    // le nombre d'essais pour chaque Pokémon
    public static final int VIES = 3;
    // l'indice montré pendant une question, selon les vies qui restent
    public static final String INDICE_COLONNES = "colonnes";
    public static final String INDICE_LIGNES = "lignes";
    public static final String INDICE_SILHOUETTE = "silhouette";

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

    // 3 vies : des colonnes, 2 vies : des lignes, 1 vie : la silhouette (elle reste quand la question est perdue)
    public String getIndice() {
        if (vies >= 3) {
            return INDICE_COLONNES;
        } else if (vies == 2) {
            return INDICE_LIGNES;
        }
        return INDICE_SILHOUETTE;
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
