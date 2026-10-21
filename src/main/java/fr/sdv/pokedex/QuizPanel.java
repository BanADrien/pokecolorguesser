package fr.sdv.pokedex;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;

/** L'onglet Quiz : retrouver un Pokémon tiré au hasard à partir de ses bandes de couleurs. */
public final class QuizPanel extends JPanel {

    private static final Color SUCCESS = new Color(0, 130, 40);
    private static final Color WARNING = new Color(205, 95, 0);
    private static final Color FAILURE = new Color(200, 30, 30);
    private static final Color STATUS = new Color(110, 110, 110);
    private static final int ANSWER_WIDTH = 300;      // largeur de la colonne de réponse, en pixels
    private static final int VISIBLE_SUGGESTIONS = 8; // lignes de la liste visibles sans la faire défiler
    private static final int MAX_SHOWN_TEXT = 14;     // au-delà, le texte tapé est coupé dans la ligne d'état
    private static final String HELP = "Tapez les premières lettres du nom…";

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
    private final JLabel livesLabel = new JLabel("Vies :");
    private final JTextField nameField = new JTextField();
    private final JLabel suggestionStatus = new JLabel(" ");
    private final DefaultListModel<Pokemon> suggestionModel = new DefaultListModel<>();
    private final JList<Pokemon> suggestionList = new JList<>(suggestionModel);
    private final JButton skipButton = new JButton("Passer");
    private final JButton nextButton = new JButton("Commencer");

    private List<Pokemon> pokemons = List.of();
    private QuizGame game;              // null tant qu'aucune partie n'a commencé
    private QuizSettings gameSettings;  // les réglages figés au début de la partie
    private NameSuggestions suggestions = new NameSuggestions(List.of()); // les Pokémon de la partie
    private BufferedImage questionSprite; // gardé pour recalculer les bandes quand on perd une vie
    private ColorBands questionBands;     // les bandes affichées pour la question en cours
    private boolean loading;           // une question est en cours de chargement
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

        nextButton.setPreferredSize(new Dimension(180, nextButton.getPreferredSize().height + 6));
        nextButton.addActionListener(e -> next());

        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.setBorder(BorderFactory.createEmptyBorder(0, 16, 16, 16));
        for (JComponent component : List.of(hintLabel, resultLabel, nextButton)) {
            component.setAlignmentX(CENTER_ALIGNMENT);
        }
        bottom.add(hintLabel);
        bottom.add(Box.createVerticalStrut(6));
        bottom.add(resultLabel);
        bottom.add(Box.createVerticalStrut(14));
        bottom.add(nextButton);

        // à gauche les bandes et le résultat, à droite la colonne de réponse
        JPanel board = new JPanel(new BorderLayout());
        board.add(view, BorderLayout.CENTER);
        board.add(bottom, BorderLayout.SOUTH);

        add(header, BorderLayout.NORTH);
        add(board, BorderLayout.CENTER);
        add(answerColumn(bottom.getPreferredSize().height), BorderLayout.EAST);

        // Après une réponse, Entrée passe à la suite, où que soit le focus dans l'onglet
        // (le champ de saisie garde sa propre touche Entrée pour répondre)
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
        updateLives();
        setAnswerControlsEnabled(false);
        newGameButton.setEnabled(false);
        nextButton.setEnabled(false);
    }

    /**
     * La colonne de réponse : les vies, le champ de saisie, la liste des Pokémon encore possibles en dessous,
     * et « Passer ». bottomHeight : la hauteur du message et du bouton sous les cases, laissée vide en bas
     * de la colonne pour qu'elle soit centrée à la hauteur des cases.
     */
    private JPanel answerColumn(int bottomHeight) {
        // à chaque lettre tapée ou effacée, la liste est recalculée
        nameField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateSuggestions();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateSuggestions();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                // changement de style du texte : rien à faire pour un champ simple
            }
        });
        nameField.addActionListener(e -> answerWithSelection()); // touche Entrée dans le champ
        bindFieldKey(KeyEvent.VK_DOWN, "suggestion-suivante", () -> moveSelection(1));
        bindFieldKey(KeyEvent.VK_UP, "suggestion-precedente", () -> moveSelection(-1));
        nameField.setToolTipText("Flèches haut et bas pour choisir, Entrée pour répondre");

        suggestionStatus.setForeground(STATUS);

        suggestionList.setCellRenderer(new SuggestionRenderer());
        suggestionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        suggestionList.setFocusable(false); // le clavier reste dans le champ de saisie
        // tailles de ligne fixes : la liste n'a pas besoin de mesurer tous les noms à chaque lettre tapée
        int rowHeight = suggestionList.getFontMetrics(suggestionList.getFont()).getHeight() + 4;
        suggestionList.setFixedCellHeight(rowHeight);
        suggestionList.setFixedCellWidth(ANSWER_WIDTH / 2); // les lignes prennent quand même toute la largeur
        suggestionList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int index = suggestionList.locationToIndex(e.getPoint()); // -1 si la liste est vide
                if (index < 0 || !SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }
                // locationToIndex donne la ligne la plus proche : on vérifie que le clic est bien dessus
                Rectangle cell = suggestionList.getCellBounds(index, index);
                if (cell.contains(e.getPoint())) {
                    answerWith(index);
                }
            }
        });
        JScrollPane scroll = new JScrollPane(suggestionList,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        Insets borders = scroll.getInsets();
        scroll.setPreferredSize(new Dimension(ANSWER_WIDTH,
                VISIBLE_SUGGESTIONS * rowHeight + borders.top + borders.bottom));

        skipButton.addActionListener(e -> skipQuestion());
        livesLabel.setHorizontalTextPosition(JLabel.LEADING); // le texte « Vies : », puis les cœurs
        livesLabel.setIconTextGap(8);

        JPanel column = new JPanel(new GridBagLayout()); // sans weighty : le contenu reste centré en hauteur
        column.setBorder(BorderFactory.createEmptyBorder(8, 0, 8 + bottomHeight, 16));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 12, 0);
        column.add(livesLabel, c);
        c.insets = new Insets(0, 0, 4, 0);
        column.add(new JLabel("Nom du Pokémon (français ou anglais) :"), c);
        column.add(nameField, c);
        column.add(suggestionStatus, c);
        c.insets = new Insets(0, 0, 0, 0);
        column.add(scroll, c);
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        c.insets = new Insets(10, 0, 0, 0);
        column.add(skipButton, c);
        return column;
    }

    /** Associe une touche, quand le champ de saisie a le focus, à une action. */
    private void bindFieldKey(int keyCode, String name, Runnable action) {
        nameField.getInputMap().put(KeyStroke.getKeyStroke(keyCode, 0), name);
        nameField.getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
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
        List<Pokemon> pool = gameSettings.pool(pokemons);
        if (pool.isEmpty()) {
            view.setMessage("Aucun Pokémon dans les générations choisies.");
            return;
        }
        game = new QuizGame(pool, gameSettings.getQuestionCount(), random);
        suggestions = new NameSuggestions(pool); // la liste ne propose que les Pokémon de la partie
        finished = false;
        loadQuestion();
    }

    /** Le bouton du bas : commencer, question suivante, voir le score ou rejouer selon le moment. */
    private void next() {
        if (game == null || finished) {
            startGame();
        } else if (game.isQuestionOver() && game.isLastQuestion()) {
            showEnd();
        } else if (game.isQuestionOver()) {
            game.next();
            loadQuestion();
        }
    }

    /** L'image est lue (ou téléchargée) en arrière-plan pour ne pas bloquer la fenêtre. */
    private void loadQuestion() {
        int token = ++loadToken;
        Pokemon pokemon = game.current();
        int sections = game.sections(gameSettings.getSections());
        boolean ignoreDarkOutline = gameSettings.isIgnoreDarkOutline();

        loading = true;
        updateProgress();
        updateLives();
        view.setMessage("Chargement de la question…");
        hintLabel.setText(" ");
        resultLabel.setText(" ");
        setAnswerControlsEnabled(false);
        clearAnswerField();
        nextButton.setText(game.isLastQuestion() ? "Voir le score" : "Question suivante");
        nextButton.setEnabled(false);

        new SwingWorker<LoadedQuestion, Void>() {
            @Override
            protected LoadedQuestion doInBackground() throws Exception {
                BufferedImage sprite = sprites.loadOriginal(pokemon);
                return new LoadedQuestion(sprite, ColorBands.from(sprite, sections, ignoreDarkOutline));
            }

            @Override
            protected void done() {
                if (token != loadToken) {
                    return; // une nouvelle partie a été lancée entre-temps
                }
                loading = false;
                try {
                    LoadedQuestion question = get();
                    questionSprite = question.sprite();
                    questionBands = question.bands();
                    view.show(questionBands, questionSprite, false);
                    showHint(pokemon);
                    setAnswerControlsEnabled(true);
                    nameField.requestFocusInWindow();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    System.err.println("Image du Pokémon n°" + pokemon.id() + " indisponible : " + e.getCause());
                    game.skip();
                    view.setMessage("L'image de " + describe(pokemon) + " est indisponible.\n"
                            + "La question est passée.");
                    resultLabel.setForeground(FAILURE);
                    resultLabel.setText("Question passée faute d'image (vérifiez la connexion).");
                    afterAnswer();
                }
            }
        }.execute();
    }

    private void setAnswerControlsEnabled(boolean enabled) {
        nameField.setEnabled(enabled);
        suggestionList.setEnabled(enabled);
        skipButton.setEnabled(enabled);
        updateStatus();
    }

    /** Vide le champ de saisie, et donc la liste des suggestions. */
    private void clearAnswerField() {
        nameField.setText("");
        updateSuggestions(); // si le champ était déjà vide, setText ne prévient pas le DocumentListener
    }

    /**
     * Remplit la liste avec les Pokémon dont le nom commence par le texte tapé, sauf ceux déjà proposés à tort
     * pour cette question. Le premier est choisi d'office.
     */
    private void updateSuggestions() {
        List<Pokemon> excluded = game == null ? List.of() : game.wrongGuesses();
        suggestionModel.clear();
        suggestionModel.addAll(suggestions.suggest(nameField.getText(), excluded));
        if (!suggestionModel.isEmpty()) {
            suggestionList.setSelectedIndex(0);
            suggestionList.ensureIndexIsVisible(0);
        }
        updateStatus();
    }

    /** La petite ligne grise entre le champ et la liste : aide, nombre de Pokémon possibles ou aucun résultat. */
    private void updateStatus() {
        String typed = nameField.getText().strip();
        int count = suggestionModel.getSize();
        if (QuizGame.normalize(typed).isEmpty()) {
            suggestionStatus.setText(nameField.isEnabled() ? HELP : " ");
        } else if (count == 0) {
            String shown = typed.length() <= MAX_SHOWN_TEXT ? typed : typed.substring(0, MAX_SHOWN_TEXT) + "…";
            suggestionStatus.setText("Aucun Pokémon ne commence par « " + shown + " »");
        } else {
            suggestionStatus.setText(count + (count == 1 ? " Pokémon possible" : " Pokémon possibles"));
        }
    }

    /** Flèches haut et bas dans le champ : on change de ligne dans la liste. */
    private void moveSelection(int delta) {
        int count = suggestionModel.getSize();
        if (!canAnswer() || count == 0) {
            return;
        }
        int index = Math.clamp(suggestionList.getSelectedIndex() + delta, 0, count - 1);
        suggestionList.setSelectedIndex(index);
        suggestionList.ensureIndexIsVisible(index);
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
        return game != null && !finished && !loading && !game.isQuestionOver();
    }

    /** Entrée dans le champ : on répond avec le Pokémon choisi dans la liste. Liste vide : rien ne se passe. */
    private void answerWithSelection() {
        int index = suggestionList.getSelectedIndex();
        if (index >= 0) {
            answerWith(index);
        }
    }

    /** Répond avec le Pokémon de la ligne index (touche Entrée ou clic). */
    private void answerWith(int index) {
        if (!canAnswer()) {
            return;
        }
        Pokemon chosen = suggestionModel.get(index);
        suggestionList.setSelectedIndex(index); // la ligne choisie reste visible après la réponse
        switch (game.answer(chosen)) {
            case CORRECT -> showAnswer(true, false);
            case WRONG_RETRY -> showRetry(chosen);
            case LOST -> showAnswer(false, false);
        }
    }

    private void skipQuestion() {
        if (!canAnswer()) {
            return;
        }
        game.skip();
        showAnswer(false, true);
    }

    /** Une vie perdue, mais il en reste : l'image devient plus précise et on peut réessayer. */
    private void showRetry(Pokemon wrong) {
        int before = questionBands.sections();
        questionBands = ColorBands.from(questionSprite, game.sections(gameSettings.getSections()),
                gameSettings.isIgnoreDarkOutline()); // le sprite est déjà chargé : c'est instantané
        view.show(questionBands, questionSprite, false);
        updateLives();

        int lives = game.livesLeft();
        String left = lives == 1 ? "Plus qu'une vie" : "Plus que " + lives + " vies";
        // si le sprite n'a pas assez de lignes pour plus de bandes, l'image ne change pas
        String precision = questionBands.sections() > before ? " : l'image est plus précise." : ".";
        resultLabel.setForeground(WARNING);
        resultLabel.setText("Ce n'est pas " + wrong.nameFr() + "… " + left + precision);

        clearAnswerField(); // le Pokémon proposé à tort disparaît aussi des suggestions
        nameField.requestFocusInWindow();
    }

    private void showAnswer(boolean found, boolean skipped) {
        Pokemon pokemon = game.current();
        view.reveal();
        updateLives();
        if (found) {
            int attempt = game.attempt();
            resultLabel.setForeground(SUCCESS);
            resultLabel.setText("Bravo ! C'est " + describe(pokemon)
                    + (attempt == 1 ? ", du premier coup !" : ", trouvé au " + attempt + "e essai."));
        } else {
            resultLabel.setForeground(FAILURE);
            resultLabel.setText((skipped ? "Passé… " : "Raté… ") + "C'était " + describe(pokemon));
        }
        setAnswerControlsEnabled(false);
        afterAnswer();
    }

    /** Les cœurs de la colonne de réponse : les vies qui restent pour la question en cours. */
    private void updateLives() {
        int lives = game == null || finished ? QuizGame.LIVES : game.livesLeft();
        livesLabel.setIcon(new LivesIcon(lives, QuizGame.LIVES));
        livesLabel.setToolTipText(lives + (lives > 1 ? " vies" : " vie") + " sur " + QuizGame.LIVES
                + " pour ce Pokémon. À chaque erreur, l'image devient plus précise.");
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
        clearAnswerField();
        updateLives();
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
