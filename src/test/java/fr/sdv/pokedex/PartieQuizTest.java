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

    // un Pokémon de la liste qui n'est pas celui à deviner
    private Pokemon autrePokemon(ArrayList<Pokemon> pokemons, Pokemon bon) {
        if (pokemons.get(0) == bon) {
            return pokemons.get(1);
        }
        return pokemons.get(0);
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
        assertEquals(0, partie.getPoints());
        // la silhouette reste affichée à côté du Pokémon dévoilé
        assertEquals(PartieQuiz.INDICE_SILHOUETTE, partie.getIndice());
    }

    @Test
    void lesPointsSontLesViesQuiRestent() {
        // du premier coup : 3 points
        PartieQuiz partie = partieAvecPikachu();
        assertEquals(0, partie.getPointsQuestion());
        assertTrue(partie.repondre(pikachu));
        assertEquals(3, partie.getPointsQuestion());
        assertEquals(3, partie.getPoints());

        // au 2e essai : 2 points
        partie = partieAvecPikachu();
        partie.repondre(salameche);
        partie.repondre(pikachu);
        assertEquals(2, partie.getPoints());

        // au 3e essai : 1 point
        partie = partieAvecPikachu();
        partie.repondre(salameche);
        partie.repondre(carapuce);
        partie.repondre(pikachu);
        assertEquals(1, partie.getPoints());

        // trois erreurs : 0 point
        partie = partieAvecPikachu();
        partie.repondre(salameche);
        partie.repondre(carapuce);
        partie.repondre(bulbizarre);
        assertEquals(0, partie.getPointsQuestion());
        assertEquals(0, partie.getPoints());
    }

    @Test
    void passerFaitPerdreUneVieEtChangeLIndice() {
        PartieQuiz partie = partieAvecPikachu();

        // 3 vies (colonnes) : on passe aux lignes
        partie.passer();
        assertEquals(2, partie.getVies());
        assertEquals(PartieQuiz.INDICE_LIGNES, partie.getIndice());
        assertFalse(partie.isQuestionFinie());

        // 2 vies (lignes) : on passe à la silhouette pixelisée
        partie.passer();
        assertEquals(1, partie.getVies());
        assertEquals(PartieQuiz.INDICE_SILHOUETTE, partie.getIndice());
        assertFalse(partie.isQuestionFinie());
    }

    @Test
    void passerALaDerniereViePerdLaQuestion() {
        PartieQuiz partie = partieAvecPikachu();
        partie.repondre(salameche);
        partie.repondre(carapuce);

        partie.passer();

        assertEquals(0, partie.getVies());
        assertTrue(partie.isQuestionFinie());
        assertEquals(0, partie.getPointsQuestion());
        assertEquals(0, partie.getPoints());
    }

    @Test
    void passerPuisTrouverDonneDeuxPoints() {
        PartieQuiz partie = partieAvecPikachu();
        partie.passer();

        assertTrue(partie.repondre(pikachu));

        assertEquals(2, partie.getPointsQuestion());
        assertEquals(2, partie.getPoints());
    }

    @Test
    void passerNAjoutePasDeMauvaisEssai() {
        PartieQuiz partie = partieAvecPikachu();
        partie.passer();
        assertTrue(partie.getMauvaisesReponses().isEmpty());

        // une vraie erreur, elle, est gardée
        partie.repondre(salameche);
        partie.passer();
        assertEquals(1, partie.getMauvaisesReponses().size());
        assertTrue(partie.getMauvaisesReponses().contains(salameche));
    }

    @Test
    void abandonnerFinitLaQuestionToutDeSuite() {
        // utilisé quand l'image du Pokémon manque : la question est finie sans perdre de vie
        PartieQuiz partie = partieAvecPikachu();
        partie.abandonner();

        assertTrue(partie.isQuestionFinie());
        assertEquals(3, partie.getVies());
        assertEquals(0, partie.getPoints());
    }

    @Test
    void leMaximumEstDeTroisPointsParQuestionEtLesPointsSAdditionnent() {
        ArrayList<Pokemon> pokemons = new ArrayList<>();
        for (int numero = 1; numero <= 20; numero++) {
            pokemons.add(new Pokemon(numero, "Pokémon " + numero, "Pokemon " + numero, 1));
        }
        assertEquals(30, new PartieQuiz(pokemons, 10).getPointsMax());

        // une petite partie de 3 questions : 9 points au maximum
        PartieQuiz partie = new PartieQuiz(pokemons, 3);
        assertEquals(9, partie.getPointsMax());
        // trouvé du premier coup : +3
        partie.repondre(partie.getPokemon());
        partie.questionSuivante();
        assertEquals(0, partie.getPointsQuestion());
        // une erreur, puis trouvé : +2
        partie.repondre(autrePokemon(pokemons, partie.getPokemon()));
        partie.repondre(partie.getPokemon());
        assertEquals(2, partie.getPointsQuestion());
        partie.questionSuivante();
        // passé trois fois : la question est perdue, +0
        partie.passer();
        partie.passer();
        partie.passer();
        assertTrue(partie.isQuestionFinie());

        assertEquals(5, partie.getPoints());
        assertEquals(9, partie.getPointsMax());

        // s'il y a moins de Pokémon que de questions, le maximum suit le vrai nombre de questions
        assertEquals(3, partieAvecPikachu().getPointsMax());
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
        assertEquals(2, partie.getPoints());
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
