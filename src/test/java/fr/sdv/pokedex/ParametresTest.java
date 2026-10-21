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
        assertEquals(12, parametres.getSections());
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
    void uneAncienneCleSensEstIgnoree(@TempDir Path dossier) throws IOException {
        Path fichier = dossier.resolve("parametres.properties");
        // un fichier d'une ancienne version, avec le sens des bandes
        Files.writeString(fichier, "sections=7\nsens=vertical\nquestions=5\n");

        Parametres parametres = Parametres.charger(fichier);
        assertEquals(7, parametres.getSections());
        assertEquals(5, parametres.getNombreQuestions());

        // une fois enregistré, le fichier n'a plus la clé
        parametres.enregistrer(fichier);
        String texte = Files.readString(fichier);
        assertTrue(texte.contains("sections=7"));
        assertFalse(texte.contains("sens="));

        // sans la clé sections : 12 par défaut
        Files.writeString(fichier, "sens=auto\nquestions=10\n");
        assertEquals(12, Parametres.charger(fichier).getSections());
    }
}
