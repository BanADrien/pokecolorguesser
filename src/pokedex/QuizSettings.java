package pokedex;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/** Les réglages du quiz, enregistrés dans un fichier .properties pour les retrouver au prochain lancement. */
public class QuizSettings {

    public static final int MIN_SECTIONS = 1;
    public static final int MAX_SECTIONS = 32;
    public static final int MIN_QUESTIONS = 1;
    public static final int MAX_QUESTIONS = 50;

    private int sections = 8;
    private int questionCount = 10;
    private boolean multipleChoice = true;
    private boolean showTypes = false;
    private final Set<Integer> generations = new TreeSet<>();

    public QuizSettings() {
        for (int gen = PokemonRepository.FIRST_GENERATION; gen <= PokemonRepository.LAST_GENERATION; gen++) {
            generations.add(gen);
        }
    }

    public int getSections() {
        return sections;
    }

    public void setSections(int sections) {
        this.sections = Math.clamp(sections, MIN_SECTIONS, MAX_SECTIONS);
    }

    public int getQuestionCount() {
        return questionCount;
    }

    public void setQuestionCount(int questionCount) {
        this.questionCount = Math.clamp(questionCount, MIN_QUESTIONS, MAX_QUESTIONS);
    }

    /** true : QCM à 4 propositions, false : il faut écrire le nom. */
    public boolean isMultipleChoice() {
        return multipleChoice;
    }

    public void setMultipleChoice(boolean multipleChoice) {
        this.multipleChoice = multipleChoice;
    }

    public boolean isShowTypes() {
        return showTypes;
    }

    public void setShowTypes(boolean showTypes) {
        this.showTypes = showTypes;
    }

    public Set<Integer> getGenerations() {
        return Collections.unmodifiableSet(generations);
    }

    public boolean isGenerationEnabled(int generation) {
        return generations.contains(generation);
    }

    /** Coche ou décoche une génération. Renvoie false si on tente de décocher la dernière. */
    public boolean setGenerationEnabled(int generation, boolean enabled) {
        if (enabled) {
            generations.add(generation);
            return true;
        }
        if (generations.size() == 1 && generations.contains(generation)) {
            return false;
        }
        generations.remove(generation);
        return true;
    }

    /** Une copie indépendante : une partie en cours n'est pas modifiée si on change les réglages. */
    public QuizSettings copy() {
        QuizSettings copy = new QuizSettings();
        copy.sections = sections;
        copy.questionCount = questionCount;
        copy.multipleChoice = multipleChoice;
        copy.showTypes = showTypes;
        copy.generations.clear();
        copy.generations.addAll(generations);
        return copy;
    }

    public static QuizSettings load(Path file) {
        QuizSettings settings = new QuizSettings();
        if (!Files.exists(file)) {
            return settings;
        }
        Properties properties = new Properties();
        try (Reader in = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            properties.load(in);
        } catch (IOException e) {
            System.err.println("Paramètres illisibles, valeurs par défaut utilisées : " + e.getMessage());
            return settings;
        }
        settings.setSections(intOrDefault(properties.getProperty("sections"), settings.sections));
        settings.setQuestionCount(intOrDefault(properties.getProperty("questions"), settings.questionCount));
        settings.setMultipleChoice(!"saisie".equals(properties.getProperty("mode")));
        settings.setShowTypes(Boolean.parseBoolean(properties.getProperty("indiceTypes")));

        Set<Integer> saved = new TreeSet<>();
        for (String gen : properties.getProperty("generations", "").split(",")) {
            int value = intOrDefault(gen.trim(), -1);
            if (value >= PokemonRepository.FIRST_GENERATION && value <= PokemonRepository.LAST_GENERATION) {
                saved.add(value);
            }
        }
        if (!saved.isEmpty()) {
            settings.generations.clear();
            settings.generations.addAll(saved);
        }
        return settings;
    }

    public void save(Path file) throws IOException {
        Properties properties = new Properties();
        properties.setProperty("sections", String.valueOf(sections));
        properties.setProperty("questions", String.valueOf(questionCount));
        properties.setProperty("mode", multipleChoice ? "qcm" : "saisie");
        properties.setProperty("indiceTypes", String.valueOf(showTypes));
        properties.setProperty("generations",
                generations.stream().map(String::valueOf).collect(Collectors.joining(",")));
        try (Writer out = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            properties.store(out, "Paramètres du quiz Pokédex");
        }
    }

    private static int intOrDefault(String text, int defaultValue) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
