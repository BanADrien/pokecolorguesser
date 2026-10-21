package fr.sdv.pokedex;

import java.util.ArrayList;
import java.util.Collections;

// Le déroulement d'une partie de quiz (questions, vies, points), sans interface graphique
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
    private int points = 0;
    private int pointsQuestion = 0; // les points gagnés à la question en cours
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

    // le total de la partie : pour chaque Pokémon trouvé, les vies qui restaient (3, 2 ou 1 point)
    public int getPoints() {
        return points;
    }

    // tous les Pokémon trouvés du premier coup : 3 points par question
    public int getPointsMax() {
        return VIES * questions.size();
    }

    // les points de la question en cours : 0 tant que le Pokémon n'est pas trouvé, ou si la question est perdue
    public int getPointsQuestion() {
        return pointsQuestion;
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

    // 3 vies : des colonnes, 2 vies : des lignes, 1 vie : la silhouette pixelisée
    // (elle reste quand la question est perdue)
    public String getIndice() {
        if (vies >= 3) {
            return INDICE_COLONNES;
        } else if (vies == 2) {
            return INDICE_LIGNES;
        }
        return INDICE_SILHOUETTE;
    }

    // renvoie true si c'est le bon Pokémon (il rapporte les vies qui restent) ; sinon une vie est perdue
    public boolean repondre(Pokemon choix) {
        if (choix.getId() == getPokemon().getId()) {
            pointsQuestion = vies;
            points = points + vies;
            questionFinie = true;
            return true;
        }
        perdreUneVie();
        if (!mauvaisesReponses.contains(choix)) {
            mauvaisesReponses.add(choix);
        }
        return false;
    }

    // le bouton « Passer » : l'indice suivant, au prix d'une vie, comme une erreur mais sans Pokémon proposé.
    // À la dernière vie, la question est perdue.
    public void passer() {
        perdreUneVie();
    }

    // abandonne la question tout de suite, même s'il reste des vies (par exemple quand l'image manque)
    public void abandonner() {
        questionFinie = true;
    }

    private void perdreUneVie() {
        vies--;
        if (vies == 0) {
            questionFinie = true;
        }
    }

    public void questionSuivante() {
        numeroQuestion++;
        questionFinie = false;
        vies = VIES;
        pointsQuestion = 0;
        mauvaisesReponses.clear();
    }
}
