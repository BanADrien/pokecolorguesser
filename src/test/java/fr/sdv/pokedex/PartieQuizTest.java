package fr.sdv.pokedex;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PartieQuizTest {

    private Pokemon pikachu = new Pokemon(25, "Pikachu", "Pikachu", 1);
    private Pokemon salameche = new Pokemon(4, "Salamèche", "Charmander", 1);
    private Pokemon carapuce = new Pokemon(7, "Carapuce", "Squirtle", 1);
    private Pokemon bulbizarre = new Pokemon(1, "Bulbizarre", "Bulbasaur", 1);

    // une partie avec un seul Pokémon possible : il faut trouver Pikachu
    private PartieQuiz partieAvecPikachu() {
        ArrayList<Pokemon> pokemons = new ArrayList<>();
        pokemons.add(pikachu);
        return new PartieQuiz(pokemons, 10);
    }

    @Test
    void uneErreurCouteUneVie() {
        PartieQuiz partie = partieAvecPikachu();
        assertEquals(1, partie.getNombreQuestions());
        assertEquals(3, partie.getVies());

        assertFalse(partie.repondre(salameche));

        assertEquals(2, partie.getVies());
        assertEquals(2, partie.getEssai());
        assertFalse(partie.isQuestionFinie());
        assertTrue(partie.getMauvaisesReponses().contains(salameche));
    }

    @Test
    void lIndiceDependDesViesRestantes() {
        PartieQuiz partie = partieAvecPikachu();
        // 3 vies : des colonnes
        assertEquals("colonnes", partie.getIndice());
        assertEquals(PartieQuiz.INDICE_COLONNES, partie.getIndice());

        // 2 vies : des lignes
        partie.repondre(salameche);
        assertEquals(PartieQuiz.INDICE_LIGNES, partie.getIndice());

        // 1 vie : la silhouette
        partie.repondre(carapuce);
        assertEquals(1, partie.getVies());
        assertEquals(PartieQuiz.INDICE_SILHOUETTE, partie.getIndice());
    }

    @Test
    void laTroisiemeErreurPerdLaQuestion() {
        PartieQuiz partie = partieAvecPikachu();
        partie.repondre(salameche);
        partie.repondre(carapuce);
        assertEquals(1, partie.getVies());
        assertFalse(partie.isQuestionFinie());

        assertFalse(partie.repondre(bulbizarre));

        assertEquals(0, partie.getVies());
        assertTrue(partie.isQuestionFinie());
        assertEquals(0, partie.getScore());
        // la silhouette reste affichée à côté du Pokémon dévoilé
        assertEquals(PartieQuiz.INDICE_SILHOUETTE, partie.getIndice());
    }

    @Test
    void bonneReponseApresUneErreurPuisQuestionSuivante() {
        ArrayList<Pokemon> pokemons = new ArrayList<>();
        pokemons.add(pikachu);
        pokemons.add(salameche);
        PartieQuiz partie = new PartieQuiz(pokemons, 2);
        Pokemon bon = partie.getPokemon();
        Pokemon mauvais = pikachu;
        if (bon == pikachu) {
            mauvais = salameche;
        }

        partie.repondre(mauvais);
        assertTrue(partie.repondre(bon));
        assertEquals(1, partie.getScore());
        assertEquals(2, partie.getEssai());
        assertTrue(partie.isQuestionFinie());

        // la question suivante redonne toutes les vies, et l'indice repart des colonnes
        partie.questionSuivante();
        assertEquals(2, partie.getNumeroQuestion());
        assertEquals(3, partie.getVies());
        assertEquals(PartieQuiz.INDICE_COLONNES, partie.getIndice());
        assertTrue(partie.getMauvaisesReponses().isEmpty());
        assertTrue(partie.isDerniereQuestion());
        assertFalse(partie.isQuestionFinie());
    }
}
