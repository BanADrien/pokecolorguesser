package fr.sdv.pokedex;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Properties;

// Les réglages du quiz, enregistrés dans parametres.properties pour les retrouver au prochain lancement
public class Parametres {

    public static final int SECTIONS_MIN = 1;
    public static final int SECTIONS_MAX = 32;
    public static final int QUESTIONS_MIN = 1;
    public static final int QUESTIONS_MAX = 50;
    // le sens des bandes : choisi pour chaque Pokémon (le plus fidèle à sa forme), ou toujours le même
    public static final String SENS_AUTO = "auto";
    public static final String SENS_HORIZONTAL = "horizontal";
    public static final String SENS_VERTICAL = "vertical";

    private int sections = 8;
    private int nombreQuestions = 10;
    private String sens = SENS_AUTO;
    private boolean indiceTypes = false;
    private boolean ignorerContoursNoirs = true;
    // generations[3] vaut true si la génération 3 est cochée (la case 0 ne sert pas)
    private boolean[] generations = new boolean[Pokemon.DERNIERE_GENERATION + 1];

    public Parametres() {
        for (int generation = 1; generation <= Pokemon.DERNIERE_GENERATION; generation++) {
            generations[generation] = true;
        }
    }

    public int getSections() {
        return sections;
    }

    public void setSections(int sections) {
        if (sections < SECTIONS_MIN) {
            sections = SECTIONS_MIN;
        }
        if (sections > SECTIONS_MAX) {
            sections = SECTIONS_MAX;
        }
        this.sections = sections;
    }

    public int getNombreQuestions() {
        return nombreQuestions;
    }

    public void setNombreQuestions(int nombreQuestions) {
        if (nombreQuestions < QUESTIONS_MIN) {
            nombreQuestions = QUESTIONS_MIN;
        }
        if (nombreQuestions > QUESTIONS_MAX) {
            nombreQuestions = QUESTIONS_MAX;
        }
        this.nombreQuestions = nombreQuestions;
    }

    public String getSens() {
        return sens;
    }

    // SENS_AUTO, SENS_HORIZONTAL ou SENS_VERTICAL ; une autre valeur donne SENS_AUTO
    public void setSens(String sens) {
        if (SENS_HORIZONTAL.equals(sens) || SENS_VERTICAL.equals(sens)) {
            this.sens = sens;
        } else {
            this.sens = SENS_AUTO;
        }
    }

    // true si les bandes de cette image sont verticales : le sens choisi, ou en automatique
    // le plus fidèle à la forme du Pokémon avec le nombre de sections des réglages
    public boolean bandesVerticales(BufferedImage image) {
        if (sens.equals(SENS_AUTO)) {
            return Bandes.verticalPlusFidele(image, sections, ignorerContoursNoirs);
        }
        return sens.equals(SENS_VERTICAL);
    }

    public boolean isIndiceTypes() {
        return indiceTypes;
    }

    public void setIndiceTypes(boolean indiceTypes) {
        this.indiceTypes = indiceTypes;
    }

    // true : les contours presque noirs ne comptent pas pour la couleur des bandes
    public boolean isIgnorerContoursNoirs() {
        return ignorerContoursNoirs;
    }

    public void setIgnorerContoursNoirs(boolean ignorerContoursNoirs) {
        this.ignorerContoursNoirs = ignorerContoursNoirs;
    }

    public boolean isGenerationCochee(int generation) {
        return generations[generation];
    }

    // renvoie false si on essaie de décocher la dernière génération cochée
    public boolean setGenerationCochee(int generation, boolean cochee) {
        if (!cochee && generations[generation] && nombreDeGenerationsCochees() == 1) {
            return false;
        }
        generations[generation] = cochee;
        return true;
    }

    private int nombreDeGenerationsCochees() {
        int nombre = 0;
        for (int generation = 1; generation <= Pokemon.DERNIERE_GENERATION; generation++) {
            if (generations[generation]) {
                nombre++;
            }
        }
        return nombre;
    }

    // les Pokémon du quiz : ceux des générations cochées qui ont une image
    public ArrayList<Pokemon> pokemonsDuQuiz(ArrayList<Pokemon> pokemons) {
        ArrayList<Pokemon> resultat = new ArrayList<>();
        for (Pokemon pokemon : pokemons) {
            if (isGenerationCochee(pokemon.getGeneration()) && !pokemon.getAdresseImage().isEmpty()) {
                resultat.add(pokemon);
            }
        }
        return resultat;
    }

    // une copie : si on change les réglages pendant une partie, la partie en cours ne change pas
    public Parametres copier() {
        Parametres copie = new Parametres();
        copie.sections = sections;
        copie.nombreQuestions = nombreQuestions;
        copie.sens = sens;
        copie.indiceTypes = indiceTypes;
        copie.ignorerContoursNoirs = ignorerContoursNoirs;
        for (int generation = 1; generation <= Pokemon.DERNIERE_GENERATION; generation++) {
            copie.generations[generation] = generations[generation];
        }
        return copie;
    }

    public static Parametres charger(Path fichier) {
        Parametres parametres = new Parametres();
        if (!Files.exists(fichier)) {
            return parametres;
        }
        Properties proprietes = new Properties();
        try (Reader lecteur = Files.newBufferedReader(fichier, StandardCharsets.UTF_8)) {
            proprietes.load(lecteur);
        } catch (IOException e) {
            System.err.println("Paramètres illisibles, valeurs par défaut utilisées : " + e.getMessage());
            return parametres;
        }
        parametres.setSections(lireNombre(proprietes.getProperty("sections"), parametres.sections));
        parametres.setNombreQuestions(lireNombre(proprietes.getProperty("questions"), parametres.nombreQuestions));
        // clé absente des anciens fichiers, ou valeur inconnue : automatique
        parametres.setSens(proprietes.getProperty("sens"));
        // l'ancienne clé "mode" (QCM ou saisie libre) n'existe plus : elle est ignorée
        parametres.setIndiceTypes(Boolean.parseBoolean(proprietes.getProperty("indiceTypes")));
        // clé absente des anciens fichiers : on garde alors true
        parametres.setIgnorerContoursNoirs(Boolean.parseBoolean(proprietes.getProperty("ignorerContoursNoirs", "true")));
        parametres.lireGenerations(proprietes.getProperty("generations", ""));
        return parametres;
    }

    // "1,2,5" : les générations cochées ; si aucune n'est valable, on les garde toutes
    private void lireGenerations(String texte) {
        boolean[] lues = new boolean[Pokemon.DERNIERE_GENERATION + 1];
        boolean auMoinsUne = false;
        for (String morceau : texte.split(",")) {
            int generation = lireNombre(morceau.trim(), -1);
            if (generation >= 1 && generation <= Pokemon.DERNIERE_GENERATION) {
                lues[generation] = true;
                auMoinsUne = true;
            }
        }
        if (auMoinsUne) {
            generations = lues;
        }
    }

    public void enregistrer(Path fichier) throws IOException {
        Properties proprietes = new Properties();
        proprietes.setProperty("sections", String.valueOf(sections));
        proprietes.setProperty("questions", String.valueOf(nombreQuestions));
        proprietes.setProperty("sens", sens);
        proprietes.setProperty("indiceTypes", String.valueOf(indiceTypes));
        proprietes.setProperty("ignorerContoursNoirs", String.valueOf(ignorerContoursNoirs));
        proprietes.setProperty("generations", texteGenerations());
        try (Writer ecrivain = Files.newBufferedWriter(fichier, StandardCharsets.UTF_8)) {
            proprietes.store(ecrivain, "Paramètres du quiz Pokédex");
        }
    }

    // les générations cochées, séparées par des virgules : "1,2,5"
    private String texteGenerations() {
        String texte = "";
        for (int generation = 1; generation <= Pokemon.DERNIERE_GENERATION; generation++) {
            if (generations[generation]) {
                if (!texte.isEmpty()) {
                    texte = texte + ",";
                }
                texte = texte + generation;
            }
        }
        return texte;
    }

    private static int lireNombre(String texte, int valeurParDefaut) {
        try {
            return Integer.parseInt(texte);
        } catch (NumberFormatException e) {
            return valeurParDefaut;
        }
    }
}
