package fr.sdv.pokedex;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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

    @Test
    void leSensDesBandesEstEnregistrePuisRelu(@TempDir Path dossier) throws IOException {
        Path fichier = dossier.resolve("parametres.properties");
        Parametres parametres = new Parametres();
        assertEquals("auto", parametres.getSens());

        parametres.setSens(Parametres.SENS_VERTICAL);
        parametres.enregistrer(fichier);
        assertTrue(Files.readString(fichier).contains("sens=vertical"));
        assertEquals("vertical", Parametres.charger(fichier).getSens());

        // une valeur inconnue, ou un ancien fichier sans la clé : automatique
        Files.writeString(fichier, "sections=8\nsens=diagonal\n");
        assertEquals("auto", Parametres.charger(fichier).getSens());
        Files.writeString(fichier, "sections=8\nquestions=10\n");
        assertEquals("auto", Parametres.charger(fichier).getSens());
    }
}
