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
    void uneErreurCouteUneVieEtDoubleLesBandes() {
        PartieQuiz partie = partieAvecPikachu();
        assertEquals(1, partie.getNombreQuestions());
        assertEquals(3, partie.getVies());
        assertEquals(8, partie.nombreDeBandes(8));

        assertFalse(partie.repondre(salameche));

        assertEquals(2, partie.getVies());
        assertEquals(2, partie.getEssai());
        assertEquals(16, partie.nombreDeBandes(8));
        assertFalse(partie.isQuestionFinie());
        assertTrue(partie.getMauvaisesReponses().contains(salameche));
    }

    @Test
    void laTroisiemeErreurPerdLaQuestion() {
        PartieQuiz partie = partieAvecPikachu();
        partie.repondre(salameche);
        partie.repondre(carapuce);
        assertEquals(1, partie.getVies());
        assertEquals(32, partie.nombreDeBandes(8));
        assertFalse(partie.isQuestionFinie());

        assertFalse(partie.repondre(bulbizarre));

        assertEquals(0, partie.getVies());
        assertTrue(partie.isQuestionFinie());
        assertEquals(0, partie.getScore());
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

        // la question suivante redonne toutes les vies
        partie.questionSuivante();
        assertEquals(2, partie.getNumeroQuestion());
        assertEquals(3, partie.getVies());
        assertTrue(partie.getMauvaisesReponses().isEmpty());
        assertTrue(partie.isDerniereQuestion());
        assertFalse(partie.isQuestionFinie());
    }
}
