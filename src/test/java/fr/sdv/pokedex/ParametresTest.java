package fr.sdv.pokedex;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParametresTest {

    @Test
    void sectionsEtQuestionsRestentDansLesBornes() {
        Parametres parametres = new Parametres();
        assertEquals(8, parametres.getSections());
        assertEquals(10, parametres.getNombreQuestions());

        parametres.setSections(0);
        assertEquals(1, parametres.getSections());
        parametres.setSections(99);
        assertEquals(32, parametres.getSections());
        parametres.setSections(5);
        assertEquals(5, parametres.getSections());

        parametres.setNombreQuestions(-3);
        assertEquals(1, parametres.getNombreQuestions());
        parametres.setNombreQuestions(51);
        assertEquals(50, parametres.getNombreQuestions());
    }

    @Test
    void onNePeutPasDecocherLaDerniereGeneration() {
        Parametres parametres = new Parametres();
        for (int generation = 1; generation <= 7; generation++) {
            assertTrue(parametres.setGenerationCochee(generation, false));
        }

        assertFalse(parametres.setGenerationCochee(8, false));
        assertTrue(parametres.isGenerationCochee(8));

        // une fois une autre génération cochée, la 8 peut être décochée
        assertTrue(parametres.setGenerationCochee(2, true));
        assertTrue(parametres.setGenerationCochee(8, false));
        assertFalse(parametres.isGenerationCochee(8));
    }
}
