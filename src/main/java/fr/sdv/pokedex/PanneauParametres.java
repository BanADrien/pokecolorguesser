package fr.sdv.pokedex;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Random;

// L'onglet Paramètres : les réglages du quiz, enregistrés à chaque changement, et un aperçu des indices
public class PanneauParametres extends JPanel {

    private static final int NUMERO_PIKACHU = 25;
    // la région de chaque génération (case 0 : génération 1)
    private static final String[] REGIONS = {"Kanto", "Johto", "Hoenn", "Sinnoh", "Unys", "Kalos", "Alola", "Galar"};

    private Parametres parametres;
    private Path fichier;
    private CacheImages images;
    private Random hasard = new Random();

    private JSpinner choixSections;
    private JSpinner choixQuestions;
    private JCheckBox caseIndiceTypes = new JCheckBox("Afficher les types en indice");
    private JCheckBox caseContours = new JCheckBox("Ignorer les contours noirs");
    private ArrayList<JCheckBox> casesGenerations = new ArrayList<>(); // case 0 : génération 1
    private PanneauBandes apercu = new PanneauBandes();
    private JLabel labelExemple = new JLabel(" ");
    private JButton boutonAutreExemple = new JButton("Autre exemple");

    private ArrayList<Pokemon> pokemons = new ArrayList<>();
    private Pokemon exemple;
    private BufferedImage imageExemple; // null tant que l'image de l'exemple n'est pas chargée
    private int numeroChargement;       // pour ignorer un chargement devenu inutile

    public PanneauParametres(Parametres parametres, Path fichier, CacheImages images) {
        super(new BorderLayout(16, 0));
        this.parametres = parametres;
        this.fichier = fichier;
        this.images = images;
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        add(creerFormulaire(), BorderLayout.WEST);
        add(creerApercu(), BorderLayout.CENTER);

        apercu.setMessage("Chargement des Pokémon…");
        boutonAutreExemple.setEnabled(false);
        boutonAutreExemple.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                montrerAutreExemple();
            }
        });
    }

    private JPanel creerFormulaire() {
        JPanel formulaire = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 12, 0);
        formulaire.add(creerCadreQuiz(), c);
        formulaire.add(creerCadreGenerations(), c);
        JLabel note = new JLabel("<html>Les changements sont enregistrés automatiquement<br>"
                + "et s'appliquent à la prochaine partie.</html>");
        note.setForeground(new Color(100, 100, 100));
        c.insets = new Insets(0, 4, 0, 0);
        formulaire.add(note, c);
        c.weighty = 1; // pousse le formulaire vers le haut
        formulaire.add(Box.createVerticalGlue(), c);
        return formulaire;
    }

    private JPanel creerCadreQuiz() {
        choixSections = creerChoixNombre(parametres.getSections(), Parametres.SECTIONS_MIN, Parametres.SECTIONS_MAX);
        choixSections.setToolTipText("Nombre de colonnes (3 vies), puis de lignes (2 vies) de l'indice, "
                + "et de cases sur le plus grand côté de la silhouette pixelisée (1 vie)");
        choixSections.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                parametres.setSections((Integer) choixSections.getValue());
                enregistrer();
                mettreAJourApercu();
            }
        });
        choixQuestions = creerChoixNombre(parametres.getNombreQuestions(), Parametres.QUESTIONS_MIN,
                Parametres.QUESTIONS_MAX);
        choixQuestions.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                parametres.setNombreQuestions((Integer) choixQuestions.getValue());
                enregistrer();
            }
        });

        caseContours.setSelected(parametres.isIgnorerContoursNoirs());
        caseContours.setToolTipText("La couleur d'une bande ne tient pas compte des pixels presque noirs "
                + "(sauf si la bande n'a que ça)");
        caseContours.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                parametres.setIgnorerContoursNoirs(caseContours.isSelected());
                enregistrer();
                mettreAJourApercu();
            }
        });
        caseIndiceTypes.setSelected(parametres.isIndiceTypes());
        caseIndiceTypes.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                parametres.setIndiceTypes(caseIndiceTypes.isSelected());
                enregistrer();
            }
        });

        JPanel cadre = new JPanel(new GridBagLayout());
        cadre.setBorder(creerBordure("Quiz"));
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 0, 3, 12);
        c.gridy = 0;
        cadre.add(new JLabel("Nombre de sections :"), c);
        c.weightx = 1; // la place en trop va à droite : le contenu reste calé à gauche
        cadre.add(choixSections, c);
        c.weightx = 0;
        c.gridy = 1;
        cadre.add(new JLabel("Questions par partie :"), c);
        cadre.add(choixQuestions, c);
        c.gridy = 2;
        c.gridwidth = 2;
        c.insets = new Insets(8, 0, 0, 0);
        cadre.add(caseContours, c);
        c.gridy = 3;
        c.insets = new Insets(2, 0, 0, 0);
        cadre.add(caseIndiceTypes, c);
        return cadre;
    }

    private JPanel creerCadreGenerations() {
        for (int i = 0; i < Pokemon.DERNIERE_GENERATION; i++) {
            int generation = i + 1;
            JCheckBox caseGeneration = new JCheckBox(generation + " – " + REGIONS[i],
                    parametres.isGenerationCochee(generation));
            caseGeneration.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    cocherGeneration(generation, caseGeneration);
                }
            });
            casesGenerations.add(caseGeneration);
        }

        // deux colonnes remplies de haut en bas : 1 à 4 à gauche, 5 à 8 à droite
        JPanel grille = new JPanel(new GridLayout(4, 2, 24, 0));
        for (int ligne = 0; ligne < 4; ligne++) {
            grille.add(casesGenerations.get(ligne));
            grille.add(casesGenerations.get(ligne + 4));
        }

        JButton boutonToutCocher = new JButton("Tout cocher");
        boutonToutCocher.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toutCocher();
            }
        });
        JPanel boutons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        boutons.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        boutons.add(boutonToutCocher);

        JPanel cadre = new JPanel(new BorderLayout());
        cadre.setBorder(creerBordure("Générations"));
        cadre.add(grille, BorderLayout.CENTER);
        cadre.add(boutons, BorderLayout.SOUTH);
        return cadre;
    }

    private JPanel creerApercu() {
        JLabel explication = new JLabel("L'indice du quiz avec ces réglages, selon les vies qui restent, "
                + "puis la réponse :");
        labelExemple.setFont(labelExemple.getFont().deriveFont(Font.BOLD));
        JPanel bas = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        bas.add(labelExemple);
        bas.add(boutonAutreExemple);

        JPanel cadre = new JPanel(new BorderLayout(0, 8));
        cadre.setBorder(creerBordure("Aperçu"));
        cadre.add(explication, BorderLayout.NORTH);
        cadre.add(apercu, BorderLayout.CENTER);
        cadre.add(bas, BorderLayout.SOUTH);
        return cadre;
    }

    // un cadre avec un titre, et un peu de marge à l'intérieur
    private Border creerBordure(String titre) {
        return BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(titre),
                BorderFactory.createEmptyBorder(4, 8, 8, 8));
    }

    private JSpinner creerChoixNombre(int valeur, int min, int max) {
        JSpinner choix = new JSpinner(new SpinnerNumberModel(valeur, min, max, 1));
        JSpinner.DefaultEditor editeur = (JSpinner.DefaultEditor) choix.getEditor();
        editeur.getTextField().setColumns(3);
        return choix;
    }

    // appelée à la fin du chargement : l'aperçu commence avec Pikachu
    public void setPokemons(ArrayList<Pokemon> pokemons) {
        this.pokemons = pokemons;
        if (pokemons.isEmpty()) {
            apercu.setMessage("Aucun Pokémon disponible.");
            return;
        }
        Pokemon premier = pokemons.get(0);
        for (Pokemon pokemon : pokemons) {
            if (pokemon.getId() == NUMERO_PIKACHU) {
                premier = pokemon;
            }
        }
        boutonAutreExemple.setEnabled(true);
        chargerExemple(premier);
    }

    private void cocherGeneration(int generation, JCheckBox caseGeneration) {
        if (parametres.setGenerationCochee(generation, caseGeneration.isSelected())) {
            enregistrer();
        } else {
            // il faut garder au moins une génération
            caseGeneration.setSelected(true);
            Toolkit.getDefaultToolkit().beep();
        }
    }

    private void toutCocher() {
        for (int i = 0; i < casesGenerations.size(); i++) {
            parametres.setGenerationCochee(i + 1, true);
            casesGenerations.get(i).setSelected(true);
        }
        enregistrer();
    }

    // un Pokémon au hasard parmi les générations cochées, différent de l'exemple actuel
    private void montrerAutreExemple() {
        ArrayList<Pokemon> candidats = new ArrayList<>();
        for (Pokemon pokemon : parametres.pokemonsDuQuiz(pokemons)) {
            if (pokemon.getId() != exemple.getId()) {
                candidats.add(pokemon);
            }
        }
        if (!candidats.isEmpty()) {
            chargerExemple(candidats.get(hasard.nextInt(candidats.size())));
        }
    }

    private void chargerExemple(Pokemon pokemon) {
        numeroChargement++;
        int numero = numeroChargement;
        exemple = pokemon;
        imageExemple = null;
        labelExemple.setText("Exemple : " + pokemon.getNom() + " (n°" + pokemon.getId() + ")");
        apercu.setMessage("Chargement de l'image…");

        // l'image doit peut-être être téléchargée : on la charge dans un autre thread
        new Thread() {
            @Override
            public void run() {
                BufferedImage image = images.chargerImage(pokemon);
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        afficherExemple(numero, image);
                    }
                });
            }
        }.start();
    }

    private void afficherExemple(int numero, BufferedImage image) {
        if (numero != numeroChargement) {
            return; // un autre exemple a été demandé entre-temps
        }
        if (image == null) {
            apercu.setMessage("Image indisponible (vérifiez la connexion).");
        } else {
            imageExemple = image;
            mettreAJourApercu();
        }
    }

    // Les trois indices d'une question et la réponse. Le calcul est rapide (image de 96 × 96) :
    // on le refait à chaque changement de réglage.
    private void mettreAJourApercu() {
        if (imageExemple != null) {
            int sections = parametres.getSections();
            boolean contours = parametres.isIgnorerContoursNoirs();
            Bandes colonnes = new Bandes(imageExemple, sections, contours, true);
            Bandes lignes = new Bandes(imageExemple, sections, contours, false);
            SilhouettePixelisee silhouette = new SilhouettePixelisee(imageExemple, sections);
            apercu.nouvellesCases(imageExemple);
            apercu.ajouterBandes(colonnes, "3 vies : " + PanneauBandes.decrire(colonnes));
            apercu.ajouterBandes(lignes, "2 vies : " + PanneauBandes.decrire(lignes));
            apercu.ajouterImage(silhouette.creerImage(), "1 vie : silhouette pixelisée");
            apercu.ajouterImage(imageExemple, "Réponse");
        }
    }

    private void enregistrer() {
        try {
            parametres.enregistrer(fichier);
        } catch (IOException e) {
            System.err.println("Impossible d'enregistrer les paramètres : " + e.getMessage());
        }
    }
}
