package pokedex;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;

/** L'onglet Quiz : retrouver un Pokémon tiré au hasard à partir de ses bandes de couleurs. */
public final class QuizPanel extends JPanel {

    private static final Color SUCCESS = new Color(0, 130, 40);
    private static final Color FAILURE = new Color(200, 30, 30);
    private static final String CHOICES_CARD = "choix";
    private static final String TYPING_CARD = "saisie";
    private static final String EMPTY_CARD = "vide";

    /** Le résultat du chargement d'une question, préparé en arrière-plan. */
    private record LoadedQuestion(BufferedImage sprite, ColorBands bands) {
    }

    private final QuizSettings settings;
    private final SpriteCache sprites;
    private final Random random = new Random();

    private final JLabel progressLabel = new JLabel(" ");
    private final JButton newGameButton = new JButton("Nouvelle partie");
    private final ColorBandsView view = new ColorBandsView();
    private final JLabel hintLabel = new JLabel(" ");
    private final JLabel resultLabel = new JLabel(" ");
    private final CardLayout answerCards = new CardLayout();
    private final JPanel answerPanel = new JPanel(answerCards);
    private final JButton[] choiceButtons = new JButton[4]; // QCM à 4 propositions
    private final JTextField nameField = new JTextField(18);
    private final JButton validateButton = new JButton("Valider");
    private final JButton skipButton = new JButton("Passer");
    private final JButton nextButton = new JButton("Commencer");
    private final Color choiceForeground;
    private final Font choiceFont;

    private List<Pokemon> pokemons = List.of();
    private QuizGame game;              // null tant qu'aucune partie n'a commencé
    private QuizSettings gameSettings;  // les réglages figés au début de la partie
    private boolean loading;            // une question est en cours de chargement
    private boolean finished;           // l'écran de fin est affiché
    private int loadToken;              // permet d'ignorer un chargement devenu inutile

    public QuizPanel(QuizSettings settings, SpriteCache sprites) {
        super(new BorderLayout());
        this.settings = settings;
        this.sprites = sprites;

        Font labelFont = progressLabel.getFont();
        progressLabel.setFont(labelFont.deriveFont(Font.BOLD, labelFont.getSize2D() + 2));
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setBorder(BorderFactory.createEmptyBorder(10, 16, 0, 16));
        header.add(progressLabel, BorderLayout.CENTER);
        header.add(newGameButton, BorderLayout.EAST);
        newGameButton.addActionListener(e -> startGame());

        view.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));

        resultLabel.setFont(labelFont.deriveFont(Font.BOLD, labelFont.getSize2D() + 2));

        JPanel choicesGrid = new JPanel(new GridLayout(1, choiceButtons.length, 8, 0));
        for (int i = 0; i < choiceButtons.length; i++) {
            int index = i;
            JButton button = new JButton(" ");
            // largeur fixe : les boutons ne changent pas de taille d'une question à l'autre
            button.setPreferredSize(new Dimension(180, button.getPreferredSize().height + 6));
            button.addActionListener(e -> choose(index));
            choiceButtons[i] = button;
            choicesGrid.add(button);
        }
        choiceForeground = choiceButtons[0].getForeground();
        choiceFont = choiceButtons[0].getFont();
        JPanel choicesCard = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        choicesCard.add(choicesGrid);

        JPanel typingCard = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        typingCard.add(new JLabel("Nom du Pokémon :"));
        typingCard.add(nameField);
        typingCard.add(validateButton);
        typingCard.add(skipButton);
        nameField.addActionListener(e -> submitName()); // touche Entrée dans le champ
        validateButton.addActionListener(e -> submitName());
        skipButton.addActionListener(e -> skipQuestion());

        answerPanel.add(choicesCard, CHOICES_CARD);
        answerPanel.add(typingCard, TYPING_CARD);
        answerPanel.add(new JPanel(), EMPTY_CARD);

        nextButton.setPreferredSize(new Dimension(180, nextButton.getPreferredSize().height + 6));
        nextButton.addActionListener(e -> next());

        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.setBorder(BorderFactory.createEmptyBorder(0, 16, 16, 16));
        for (JComponent component : List.of(hintLabel, resultLabel, answerPanel, nextButton)) {
            component.setAlignmentX(CENTER_ALIGNMENT);
        }
        bottom.add(hintLabel);
        bottom.add(Box.createVerticalStrut(6));
        bottom.add(resultLabel);
        bottom.add(Box.createVerticalStrut(12));
        bottom.add(answerPanel);
        bottom.add(Box.createVerticalStrut(14));
        bottom.add(nextButton);

        add(header, BorderLayout.NORTH);
        add(view, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        // Après une réponse, Entrée passe à la suite, où que soit le focus dans l'onglet
        // (le champ de saisie garde sa propre touche Entrée pour valider)
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "suite");
        getActionMap().put("suite", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (nextButton.isEnabled()) {
                    nextButton.doClick();
                }
            }
        });

        // tant que les Pokémon ne sont pas chargés, on ne peut pas jouer
        progressLabel.setText("Quiz des couleurs");
        view.setMessage("Chargement des Pokémon…");
        answerCards.show(answerPanel, EMPTY_CARD);
        newGameButton.setEnabled(false);
        nextButton.setEnabled(false);
    }

    /** Appelé à la fin du chargement : le quiz devient jouable. */
    public void setPokemons(List<Pokemon> pokemons) {
        this.pokemons = List.copyOf(pokemons);
        if (pokemons.isEmpty()) {
            view.setMessage("Aucun Pokémon disponible : impossible de lancer le quiz.");
            return;
        }
        view.setMessage("Retrouvez le Pokémon caché derrière ses bandes de couleurs !\n"
                + "Chaque bande donne sa couleur la plus fréquente et sa largeur à cette hauteur.\n"
                + "Cliquez sur « Commencer » pour lancer une partie.");
        newGameButton.setEnabled(true);
        nextButton.setEnabled(true);
    }

    private void startGame() {
        // une copie : changer les paramètres pendant la partie ne la perturbe pas
        gameSettings = settings.copy();
        List<Pokemon> pool = new ArrayList<>();
        for (Pokemon pokemon : pokemons) {
            if (gameSettings.isGenerationEnabled(pokemon.generation()) && !pokemon.spriteUrl().isEmpty()) {
                pool.add(pokemon);
            }
        }
        if (pool.isEmpty()) {
            view.setMessage("Aucun Pokémon dans les générations choisies.");
            return;
        }
        game = new QuizGame(pool, gameSettings.getQuestionCount(), random);
        finished = false;
        loadQuestion();
    }

    /** Le bouton du bas : commencer, question suivante, voir le score ou rejouer selon le moment. */
    private void next() {
        if (game == null || finished) {
            startGame();
        } else if (game.isAnswered() && game.isLastQuestion()) {
            showEnd();
        } else if (game.isAnswered()) {
            game.next();
            loadQuestion();
        }
    }

    /** L'image est lue (ou téléchargée) en arrière-plan pour ne pas bloquer la fenêtre. */
    private void loadQuestion() {
        int token = ++loadToken;
        Pokemon pokemon = game.current();
        int sections = gameSettings.getSections();

        loading = true;
        updateProgress();
        view.setMessage("Chargement de la question…");
        hintLabel.setText(" ");
        resultLabel.setText(" ");
        prepareAnswerControls();
        setAnswerControlsEnabled(false);
        nextButton.setText(game.isLastQuestion() ? "Voir le score" : "Question suivante");
        nextButton.setEnabled(false);

        new SwingWorker<LoadedQuestion, Void>() {
            @Override
            protected LoadedQuestion doInBackground() throws Exception {
                BufferedImage sprite = sprites.loadOriginal(pokemon);
                return new LoadedQuestion(sprite, ColorBands.from(sprite, sections));
            }

            @Override
            protected void done() {
                if (token != loadToken) {
                    return; // une nouvelle partie a été lancée entre-temps
                }
                loading = false;
                try {
                    LoadedQuestion question = get();
                    view.show(question.bands(), question.sprite(), false);
                    showHint(pokemon);
                    setAnswerControlsEnabled(true);
                    if (!gameSettings.isMultipleChoice()) {
                        nameField.requestFocusInWindow();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    System.err.println("Image du Pokémon n°" + pokemon.id() + " indisponible : " + e.getCause());
                    game.skip();
                    view.setMessage("L'image de " + describe(pokemon) + " est indisponible.\n"
                            + "La question est passée.");
                    answerCards.show(answerPanel, EMPTY_CARD);
                    resultLabel.setForeground(FAILURE);
                    resultLabel.setText("Question passée faute d'image (vérifiez la connexion).");
                    afterAnswer();
                }
            }
        }.execute();
    }

    private void prepareAnswerControls() {
        if (gameSettings.isMultipleChoice()) {
            List<Pokemon> choices = game.choices();
            for (int i = 0; i < choiceButtons.length; i++) {
                JButton button = choiceButtons[i];
                button.setForeground(choiceForeground);
                button.setFont(choiceFont);
                button.setVisible(i < choices.size());
                if (i < choices.size()) {
                    button.setText(choices.get(i).nameFr());
                }
            }
            answerCards.show(answerPanel, CHOICES_CARD);
        } else {
            nameField.setText("");
            nameField.setEditable(true);
            answerCards.show(answerPanel, TYPING_CARD);
        }
    }

    private void setAnswerControlsEnabled(boolean enabled) {
        for (JButton button : choiceButtons) {
            button.setEnabled(enabled);
        }
        nameField.setEnabled(enabled);
        validateButton.setEnabled(enabled);
        skipButton.setEnabled(enabled);
    }

    private void showHint(Pokemon pokemon) {
        if (gameSettings.isShowTypes()) {
            String word = pokemon.types().size() > 1 ? "types " : "type ";
            hintLabel.setText("Indice : " + word + String.join(" / ", pokemon.types()));
        } else {
            hintLabel.setText(" "); // garde la hauteur de la ligne, pour que rien ne bouge
        }
    }

    private boolean canAnswer() {
        return game != null && !finished && !loading && !game.isAnswered();
    }

    private void choose(int index) {
        // après la réponse, les boutons restent actifs (pour garder leurs couleurs lisibles) : on ignore le clic
        if (!canAnswer()) {
            return;
        }
        List<Pokemon> choices = game.choices();
        boolean correct = game.answer(choices.get(index));
        for (int i = 0; i < choices.size(); i++) {
            if (choices.get(i).id() == game.current().id()) {
                choiceButtons[i].setForeground(SUCCESS);
                choiceButtons[i].setFont(choiceFont.deriveFont(Font.BOLD));
            } else if (i == index) {
                choiceButtons[i].setForeground(FAILURE);
            }
        }
        showAnswer(correct, false);
    }

    private void submitName() {
        if (!canAnswer()) {
            return;
        }
        String typed = nameField.getText().trim();
        if (typed.isEmpty()) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        showAnswer(game.answer(typed), false);
    }

    private void skipQuestion() {
        if (!canAnswer()) {
            return;
        }
        game.skip();
        showAnswer(false, true);
    }

    private void showAnswer(boolean correct, boolean skipped) {
        Pokemon pokemon = game.current();
        view.reveal();
        if (correct) {
            resultLabel.setForeground(SUCCESS);
            resultLabel.setText("Bravo ! C'est " + describe(pokemon));
        } else {
            resultLabel.setForeground(FAILURE);
            resultLabel.setText((skipped ? "Passé… " : "Raté… ") + "C'était " + describe(pokemon));
        }
        nameField.setEditable(false);
        validateButton.setEnabled(false);
        skipButton.setEnabled(false);
        afterAnswer();
    }

    private void afterAnswer() {
        updateProgress();
        nextButton.setEnabled(true);
        nextButton.requestFocusInWindow(); // pour qu'Entrée passe à la suite
    }

    private void showEnd() {
        finished = true;
        int score = game.score();
        int count = game.questionCount();
        progressLabel.setText("Partie terminée · Score : " + score + " / " + count);
        view.setMessage("Partie terminée !\nScore final : " + score + " / " + count + "\n" + comment(score, count));
        hintLabel.setText(" ");
        resultLabel.setText(" ");
        answerCards.show(answerPanel, EMPTY_CARD);
        nextButton.setText("Rejouer");
        nextButton.setEnabled(true);
        nextButton.requestFocusInWindow();
    }

    private void updateProgress() {
        progressLabel.setText("Question " + game.questionNumber() + " / " + game.questionCount()
                + " · Score : " + game.score());
    }

    /** "Dracaufeu (Charizard, n°6)", ou "Pikachu (n°25)" quand les deux noms sont identiques. */
    private static String describe(Pokemon pokemon) {
        if (pokemon.nameFr().equalsIgnoreCase(pokemon.nameEn())) {
            return pokemon.nameFr() + " (n°" + pokemon.id() + ")";
        }
        return pokemon.nameFr() + " (" + pokemon.nameEn() + ", n°" + pokemon.id() + ")";
    }

    private static String comment(int score, int count) {
        double ratio = (double) score / count;
        if (score == count) {
            return "Parfait, un vrai maître Pokémon !";
        } else if (ratio >= 0.7) {
            return "Très bien joué !";
        } else if (ratio >= 0.4) {
            return "Pas mal, encore un peu d'entraînement !";
        }
        return "Les couleurs sont trompeuses… Retentez votre chance !";
    }
}
