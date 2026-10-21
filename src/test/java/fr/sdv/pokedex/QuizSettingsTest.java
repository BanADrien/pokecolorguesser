package fr.sdv.pokedex;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests des réglages du quiz et de leur fichier .properties (écrit dans un dossier temporaire). */
class QuizSettingsTest {

    private static final Set<Integer> ALL_GENERATIONS = Set.of(1, 2, 3, 4, 5, 6, 7, 8);

    /** Un Pokémon de test : ici, seuls le numéro, la génération et l'image comptent. */
    private static Pokemon pokemon(int id, String name, int generation, String spriteUrl) {
        return new Pokemon(id, name, name, generation, List.of("Normal"), 1.0, 10.0,
                50, 50, 50, 50, 50, 50, spriteUrl);
    }

    @Test
    void defaultValues() {
        QuizSettings settings = new QuizSettings();
        assertEquals(8, settings.getSections());
        assertEquals(10, settings.getQuestionCount());
        assertFalse(settings.isShowTypes());
        assertTrue(settings.isIgnoreDarkOutline(), "contours noirs ignorés par défaut");
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
    void poolKeepsCheckedGenerationsWithAnImage() {
        Pokemon bulbizarre = pokemon(1, "Bulbizarre", 1, "https://example.org/1.png");
        Pokemon sansImage = pokemon(2, "Herbizarre", 1, "");
        Pokemon germignon = pokemon(152, "Germignon", 2, "https://example.org/152.png");
        Pokemon arcko = pokemon(252, "Arcko", 3, "https://example.org/252.png");
        QuizSettings settings = new QuizSettings();
        settings.setGenerationEnabled(2, false);

        assertEquals(List.of(bulbizarre, arcko), settings.pool(List.of(bulbizarre, sansImage, germignon, arcko)));
    }

    @Test
    void copyIsIndependent() {
        QuizSettings settings = new QuizSettings();
        settings.setSections(5);
        settings.setGenerationEnabled(3, false);

        QuizSettings copy = settings.copy();
        settings.setSections(9);
        settings.setQuestionCount(20);
        settings.setShowTypes(true);
        settings.setIgnoreDarkOutline(false);
        settings.setGenerationEnabled(3, true);
        settings.setGenerationEnabled(4, false);

        assertEquals(5, copy.getSections());
        assertEquals(10, copy.getQuestionCount());
        assertFalse(copy.isShowTypes());
        assertTrue(copy.isIgnoreDarkOutline());
        assertFalse(copy.isGenerationEnabled(3));
        assertTrue(copy.isGenerationEnabled(4));
    }

    @Test
    void saveThenLoadGivesSameSettings(@TempDir Path folder) throws IOException {
        QuizSettings settings = new QuizSettings();
        settings.setSections(5);
        settings.setQuestionCount(3);
        settings.setShowTypes(true);
        settings.setIgnoreDarkOutline(false);
        for (int generation : new int[]{1, 3, 4, 5, 6, 7}) {
            settings.setGenerationEnabled(generation, false);
        }
        Path file = folder.resolve("parametres.properties");

        settings.save(file);
        QuizSettings loaded = QuizSettings.load(file);

        assertEquals(5, loaded.getSections());
        assertEquals(3, loaded.getQuestionCount());
        assertTrue(loaded.isShowTypes());
        assertFalse(loaded.isIgnoreDarkOutline());
        assertEquals(Set.of(2, 8), loaded.getGenerations());
    }

    @Test
    void missingFileGivesDefaultValues(@TempDir Path folder) {
        QuizSettings loaded = QuizSettings.load(folder.resolve("absent.properties"));
        assertEquals(8, loaded.getSections());
        assertEquals(10, loaded.getQuestionCount());
        assertFalse(loaded.isShowTypes());
        assertEquals(ALL_GENERATIONS, loaded.getGenerations());
    }

    @Test
    void invalidValuesAreReplaced(@TempDir Path folder) throws IOException {
        Path file = folder.resolve("parametres.properties");
        Files.writeString(file, """
                sections=beaucoup
                questions=500
                generations=0, 3,9,x
                """);

        QuizSettings loaded = QuizSettings.load(file);

        assertEquals(8, loaded.getSections(), "valeur illisible : valeur par défaut");
        assertEquals(QuizSettings.MAX_QUESTIONS, loaded.getQuestionCount(), "valeur trop grande : plafonnée");
        assertEquals(Set.of(3), loaded.getGenerations(), "seules les générations de 1 à 8 sont gardées");
    }

    @Test
    void oldFileWithAnswerModeIsStillLoaded(@TempDir Path folder) throws IOException {
        // fichier écrit par une ancienne version : on y choisissait entre QCM et saisie libre,
        // et le réglage des contours noirs n'existait pas encore
        Path file = folder.resolve("parametres.properties");
        Files.writeString(file, """
                generations=1,2
                indiceTypes=true
                mode=qcm
                questions=7
                sections=12
                """);

        QuizSettings loaded = QuizSettings.load(file);

        assertEquals(12, loaded.getSections());
        assertEquals(7, loaded.getQuestionCount());
        assertTrue(loaded.isShowTypes());
        assertTrue(loaded.isIgnoreDarkOutline(), "clé absente : valeur par défaut");
        assertEquals(Set.of(1, 2), loaded.getGenerations());

        loaded.save(file);
        assertFalse(Files.readString(file).contains("mode"), "la clé mode n'est plus enregistrée");
    }
}
