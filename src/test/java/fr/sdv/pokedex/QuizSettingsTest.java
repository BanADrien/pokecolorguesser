package fr.sdv.pokedex;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests des réglages du quiz et de leur fichier .properties (écrit dans un dossier temporaire). */
class QuizSettingsTest {

    private static final Set<Integer> ALL_GENERATIONS = Set.of(1, 2, 3, 4, 5, 6, 7, 8);

    @Test
    void defaultValues() {
        QuizSettings settings = new QuizSettings();
        assertEquals(8, settings.getSections());
        assertEquals(10, settings.getQuestionCount());
        assertTrue(settings.isMultipleChoice());
        assertFalse(settings.isShowTypes());
        assertEquals(ALL_GENERATIONS, settings.getGenerations());
    }

    @Test
    void sectionsAndQuestionsStayWithinBounds() {
        QuizSettings settings = new QuizSettings();

        settings.setSections(0);
        assertEquals(QuizSettings.MIN_SECTIONS, settings.getSections());
        settings.setSections(99);
        assertEquals(QuizSettings.MAX_SECTIONS, settings.getSections());
        settings.setSections(5);
        assertEquals(5, settings.getSections());

        settings.setQuestionCount(-3);
        assertEquals(QuizSettings.MIN_QUESTIONS, settings.getQuestionCount());
        settings.setQuestionCount(51);
        assertEquals(QuizSettings.MAX_QUESTIONS, settings.getQuestionCount());
    }

    @Test
    void lastGenerationCannotBeUnchecked() {
        QuizSettings settings = new QuizSettings();
        for (int generation = 1; generation <= 7; generation++) {
            assertTrue(settings.setGenerationEnabled(generation, false));
        }

        assertFalse(settings.setGenerationEnabled(8, false), "il faut garder au moins une génération");
        assertTrue(settings.isGenerationEnabled(8));
        assertEquals(Set.of(8), settings.getGenerations());

        // une fois une autre génération cochée, la 8 peut être décochée
        assertTrue(settings.setGenerationEnabled(2, true));
        assertTrue(settings.setGenerationEnabled(8, false));
        assertEquals(Set.of(2), settings.getGenerations());
    }

    @Test
    void generationsCannotBeChangedFromOutside() {
        QuizSettings settings = new QuizSettings();
        assertThrows(UnsupportedOperationException.class, () -> settings.getGenerations().clear());
    }

    @Test
    void copyIsIndependent() {
        QuizSettings settings = new QuizSettings();
        settings.setSections(5);
        settings.setGenerationEnabled(3, false);

        QuizSettings copy = settings.copy();
        settings.setSections(9);
        settings.setQuestionCount(20);
        settings.setMultipleChoice(false);
        settings.setShowTypes(true);
        settings.setGenerationEnabled(3, true);
        settings.setGenerationEnabled(4, false);

        assertEquals(5, copy.getSections());
        assertEquals(10, copy.getQuestionCount());
        assertTrue(copy.isMultipleChoice());
        assertFalse(copy.isShowTypes());
        assertFalse(copy.isGenerationEnabled(3));
        assertTrue(copy.isGenerationEnabled(4));
    }

    @Test
    void saveThenLoadGivesSameSettings(@TempDir Path folder) throws IOException {
        QuizSettings settings = new QuizSettings();
        settings.setSections(5);
        settings.setQuestionCount(3);
        settings.setMultipleChoice(false);
        settings.setShowTypes(true);
        for (int generation : new int[]{1, 3, 4, 5, 6, 7}) {
            settings.setGenerationEnabled(generation, false);
        }
        Path file = folder.resolve("parametres.properties");

        settings.save(file);
        QuizSettings loaded = QuizSettings.load(file);

        assertEquals(5, loaded.getSections());
        assertEquals(3, loaded.getQuestionCount());
        assertFalse(loaded.isMultipleChoice());
        assertTrue(loaded.isShowTypes());
        assertEquals(Set.of(2, 8), loaded.getGenerations());
    }

    @Test
    void missingFileGivesDefaultValues(@TempDir Path folder) {
        QuizSettings loaded = QuizSettings.load(folder.resolve("absent.properties"));
        assertEquals(8, loaded.getSections());
        assertEquals(10, loaded.getQuestionCount());
        assertTrue(loaded.isMultipleChoice());
        assertEquals(ALL_GENERATIONS, loaded.getGenerations());
    }

    @Test
    void invalidValuesAreReplaced(@TempDir Path folder) throws IOException {
        Path file = folder.resolve("parametres.properties");
        Files.writeString(file, """
                sections=beaucoup
                questions=500
                mode=saisie
                generations=0, 3,9,x
                """);

        QuizSettings loaded = QuizSettings.load(file);

        assertEquals(8, loaded.getSections(), "valeur illisible : valeur par défaut");
        assertEquals(QuizSettings.MAX_QUESTIONS, loaded.getQuestionCount(), "valeur trop grande : plafonnée");
        assertFalse(loaded.isMultipleChoice());
        assertEquals(Set.of(3), loaded.getGenerations(), "seules les générations de 1 à 8 sont gardées");
    }
}
