package fr.sdv.pokedex;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;

/** L'onglet Paramètres : les réglages du quiz, enregistrés à chaque changement, et un aperçu des bandes. */
public final class SettingsPanel extends JPanel {

    private static final int DEFAULT_EXAMPLE_ID = 25; // Pikachu
    /** Le nom de la région de chaque génération (indice 0 : génération 1). */
    private static final String[] REGIONS = {"Kanto", "Johto", "Hoenn", "Sinnoh", "Unys", "Kalos", "Alola", "Galar"};

    private final QuizSettings settings;
    private final Path file;
    private final SpriteCache sprites;
    private final Random random = new Random();

    private final JSpinner sectionsSpinner;
    private final JSpinner questionsSpinner;
    private final JRadioButton choiceMode = new JRadioButton("QCM – 4 propositions");
    private final JRadioButton typingMode = new JRadioButton("Saisie libre (nom français ou anglais)");
    private final JCheckBox showTypesBox = new JCheckBox("Afficher les types en indice");
    private final List<JCheckBox> generationBoxes = new ArrayList<>(); // indice 0 : première génération
    private final ColorBandsView preview = new ColorBandsView();
    private final JLabel exampleLabel = new JLabel(" ");
    private final JButton otherExampleButton = new JButton("Autre exemple");

    private List<Pokemon> pokemons = List.of();
    private Pokemon example;
    private BufferedImage exampleSprite; // null tant que l'image de l'exemple n'est pas chargée
    private int exampleToken;            // permet d'ignorer un chargement devenu inutile

    public SettingsPanel(QuizSettings settings, Path file, SpriteCache sprites) {
        super(new BorderLayout(16, 0));
        this.settings = settings;
        this.file = file;
        this.sprites = sprites;
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        sectionsSpinner = spinner(settings.getSections(), QuizSettings.MIN_SECTIONS, QuizSettings.MAX_SECTIONS);
        sectionsSpinner.setToolTipText("Nombre de bandes de couleur horizontales");
        sectionsSpinner.addChangeListener(e -> {
            settings.setSections(((Number) sectionsSpinner.getValue()).intValue());
            save();
            updatePreview();
        });
        questionsSpinner = spinner(settings.getQuestionCount(), QuizSettings.MIN_QUESTIONS, QuizSettings.MAX_QUESTIONS);
        questionsSpinner.addChangeListener(e -> {
            settings.setQuestionCount(((Number) questionsSpinner.getValue()).intValue());
            save();
        });

        ButtonGroup modes = new ButtonGroup();
        modes.add(choiceMode);
        modes.add(typingMode);
        choiceMode.setSelected(settings.isMultipleChoice());
        typingMode.setSelected(!settings.isMultipleChoice());
        choiceMode.addActionListener(e -> {
            settings.setMultipleChoice(true);
            save();
        });
        typingMode.addActionListener(e -> {
            settings.setMultipleChoice(false);
            save();
        });

        showTypesBox.setSelected(settings.isShowTypes());
        showTypesBox.addActionListener(e -> {
            settings.setShowTypes(showTypesBox.isSelected());
            save();
        });

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 12, 0);
        form.add(quizSection(), c);
        form.add(generationsSection(), c);
        JLabel note = new JLabel("<html>Les changements sont enregistrés automatiquement<br>"
                + "et s'appliquent à la prochaine partie.</html>");
        note.setForeground(new Color(100, 100, 100));
        c.insets = new Insets(0, 4, 0, 0);
        form.add(note, c);
        c.weighty = 1; // pousse le formulaire vers le haut
        form.add(Box.createVerticalGlue(), c);

        add(form, BorderLayout.WEST);
        add(previewSection(), BorderLayout.CENTER);

        preview.setMessage("Chargement des Pokémon…");
        otherExampleButton.setEnabled(false);
        otherExampleButton.addActionListener(e -> showRandomExample());
    }

    private JPanel quizSection() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Quiz"), BorderFactory.createEmptyBorder(4, 8, 8, 8)));
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 0, 3, 12);

        c.gridy = 0;
        panel.add(new JLabel("Nombre de sections :"), c);
        panel.add(sectionsSpinner, c);
        c.gridy = 1;
        panel.add(new JLabel("Questions par partie :"), c);
        panel.add(questionsSpinner, c);
        c.gridy = 2;
        panel.add(new JLabel("Mode de réponse :"), c);
        panel.add(choiceMode, c);
        c.gridy = 3;
        c.gridx = 1;
        panel.add(typingMode, c);
        c.gridy = 4;
        c.gridx = 0;
        c.gridwidth = 2;
        c.insets = new Insets(8, 0, 0, 0);
        panel.add(showTypesBox, c);
        return panel;
    }

    private JPanel generationsSection() {
        int count = PokemonRepository.LAST_GENERATION - PokemonRepository.FIRST_GENERATION + 1;
        for (int i = 0; i < count; i++) {
            int generation = PokemonRepository.FIRST_GENERATION + i;
            JCheckBox box = new JCheckBox(generation + " – " + REGIONS[generation - 1],
                    settings.isGenerationEnabled(generation));
            box.addActionListener(e -> toggleGeneration(generation, box));
            generationBoxes.add(box);
        }

        // deux colonnes remplies de haut en bas : 1 à 4 à gauche, 5 à 8 à droite
        int rows = (count + 1) / 2;
        JPanel grid = new JPanel(new GridLayout(rows, 2, 24, 0));
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < 2; column++) {
                int i = column * rows + row;
                grid.add(i < count ? generationBoxes.get(i) : new JLabel());
            }
        }

        JButton checkAllButton = new JButton("Tout cocher");
        checkAllButton.addActionListener(e -> checkAllGenerations());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        buttons.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        buttons.add(checkAllButton);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Générations"), BorderFactory.createEmptyBorder(4, 8, 8, 8)));
        panel.add(grid, BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel previewSection() {
        JLabel explanation = new JLabel("Ce que montre le quiz avec ces réglages (ici avec la réponse dévoilée) :");

        Font font = exampleLabel.getFont();
        exampleLabel.setFont(font.deriveFont(Font.BOLD));
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        footer.add(exampleLabel);
        footer.add(otherExampleButton);

        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Aperçu"), BorderFactory.createEmptyBorder(4, 8, 8, 8)));
        panel.add(explanation, BorderLayout.NORTH);
        panel.add(preview, BorderLayout.CENTER);
        panel.add(footer, BorderLayout.SOUTH);
        return panel;
    }

    /** Appelé à la fin du chargement : l'aperçu commence avec Pikachu. */
    public void setPokemons(List<Pokemon> pokemons) {
        this.pokemons = List.copyOf(pokemons);
        if (pokemons.isEmpty()) {
            preview.setMessage("Aucun Pokémon disponible.");
            return;
        }
        Pokemon first = pokemons.getFirst();
        for (Pokemon pokemon : pokemons) {
            if (pokemon.id() == DEFAULT_EXAMPLE_ID) {
                first = pokemon;
            }
        }
        otherExampleButton.setEnabled(true);
        loadExample(first);
    }

    private void toggleGeneration(int generation, JCheckBox box) {
        if (!settings.setGenerationEnabled(generation, box.isSelected())) {
            // il faut garder au moins une génération
            box.setSelected(true);
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        save();
    }

    private void checkAllGenerations() {
        for (int i = 0; i < generationBoxes.size(); i++) {
            settings.setGenerationEnabled(PokemonRepository.FIRST_GENERATION + i, true);
            generationBoxes.get(i).setSelected(true);
        }
        save();
    }

    /** Un Pokémon au hasard parmi les générations cochées, différent de l'exemple actuel. */
    private void showRandomExample() {
        List<Pokemon> candidates = new ArrayList<>();
        for (Pokemon pokemon : pokemons) {
            if (settings.isGenerationEnabled(pokemon.generation()) && !pokemon.spriteUrl().isEmpty()
                    && pokemon.id() != example.id()) {
                candidates.add(pokemon);
            }
        }
        if (!candidates.isEmpty()) {
            loadExample(candidates.get(random.nextInt(candidates.size())));
        }
    }

    private void loadExample(Pokemon pokemon) {
        int token = ++exampleToken;
        example = pokemon;
        exampleSprite = null;
        exampleLabel.setText("Exemple : " + pokemon.nameFr() + " (n°" + pokemon.id() + ")");
        preview.setMessage("Chargement de l'image…");

        new SwingWorker<BufferedImage, Void>() {
            @Override
            protected BufferedImage doInBackground() throws Exception {
                return sprites.loadOriginal(pokemon);
            }

            @Override
            protected void done() {
                if (token != exampleToken) {
                    return; // un autre exemple a été demandé entre-temps
                }
                try {
                    exampleSprite = get();
                    updatePreview();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    System.err.println("Image du Pokémon n°" + pokemon.id() + " indisponible : " + e.getCause());
                    preview.setMessage("Image indisponible (vérifiez la connexion).");
                }
            }
        }.execute();
    }

    /** Le découpage est rapide (image de 96×96) : on le refait à chaque changement du nombre de sections. */
    private void updatePreview() {
        if (exampleSprite != null) {
            preview.show(ColorBands.from(exampleSprite, settings.getSections()), exampleSprite, true);
        }
    }

    private void save() {
        try {
            settings.save(file);
        } catch (IOException e) {
            System.err.println("Impossible d'enregistrer les paramètres : " + e.getMessage());
        }
    }

    private static JSpinner spinner(int value, int min, int max) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(value, min, max, 1));
        ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setColumns(3);
        return spinner;
    }
}
