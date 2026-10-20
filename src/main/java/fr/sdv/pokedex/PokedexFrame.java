package fr.sdv.pokedex;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

/** Fenêtre principale : trois onglets (tableau des Pokémon, quiz, paramètres) et une barre d'état. */
public class PokedexFrame extends JFrame {

    private static final int[] COLUMN_WIDTHS = {45, 76, 130, 130, 45, 150, 75, 75, 55, 60, 60, 65, 65, 60, 55};
    private static final Path SETTINGS_FILE = Path.of("parametres.properties");

    private final PokemonRepository repository = new PokemonRepository(Path.of("cache", "pokedex.csv"));
    private final JTable table = new JTable();
    // à chaque image téléchargée, on redessine le tableau pour l'afficher
    private final SpriteCache sprites = new SpriteCache(Path.of("cache", "sprites"), table::repaint);
    private final PokemonTableModel model = new PokemonTableModel(sprites);
    private final QuizSettings settings = QuizSettings.load(SETTINGS_FILE);
    private final QuizPanel quizPanel = new QuizPanel(settings, sprites);
    private final SettingsPanel settingsPanel = new SettingsPanel(settings, SETTINGS_FILE, sprites);
    private final JLabel status = new JLabel("Chargement…");
    private final JProgressBar progressBar = new JProgressBar();

    public static void launch() {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException e) {
                // on garde l'apparence Swing par défaut
            }
            PokedexFrame frame = new PokedexFrame();
            frame.setVisible(true);
            frame.loadPokemons();
        });
    }

    private PokedexFrame() {
        super("Pokédex – Générations " + PokemonRepository.FIRST_GENERATION
                + " à " + PokemonRepository.LAST_GENERATION);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        table.setModel(model);
        TableRowSorter<PokemonTableModel> sorter = new TableRowSorter<>(model);
        sorter.setSortable(PokemonTableModel.IMAGE_COLUMN, false);
        table.setRowSorter(sorter); // un clic sur un en-tête trie la colonne
        table.setFillsViewportHeight(true);
        table.setRowHeight(SpriteCache.MAX_SIZE + 8);
        TableColumnModel columns = table.getColumnModel();
        for (int i = 0; i < COLUMN_WIDTHS.length; i++) {
            columns.getColumn(i).setPreferredWidth(COLUMN_WIDTHS[i]);
        }

        progressBar.setIndeterminate(true);
        progressBar.setStringPainted(true);

        JPanel statusBar = new JPanel(new BorderLayout(8, 0));
        statusBar.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        statusBar.add(status, BorderLayout.CENTER);
        statusBar.add(progressBar, BorderLayout.EAST);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Pokédex", new JScrollPane(table));
        tabs.addTab("Quiz", quizPanel);
        tabs.addTab("Paramètres", settingsPanel);

        add(tabs, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);
        setSize(1230, 760);
        setLocationRelativeTo(null);
    }

    /** Le chargement se fait en arrière-plan pour que la fenêtre reste réactive. */
    private void loadPokemons() {
        new SwingWorker<List<Pokemon>, Void>() {
            @Override
            protected List<Pokemon> doInBackground() throws Exception {
                return repository.loadAll((done, total) -> SwingUtilities.invokeLater(() -> {
                    progressBar.setIndeterminate(false);
                    progressBar.setMaximum(total);
                    progressBar.setValue(Math.max(done, progressBar.getValue()));
                    status.setText("Téléchargement depuis PokéAPI… " + progressBar.getValue() + " / " + total);
                }));
            }

            @Override
            protected void done() {
                progressBar.setVisible(false);
                try {
                    List<Pokemon> pokemons = get();
                    model.setPokemons(pokemons);
                    quizPanel.setPokemons(pokemons);
                    settingsPanel.setPokemons(pokemons);
                    status.setText(pokemons.size() + " Pokémon (générations " + PokemonRepository.FIRST_GENERATION
                            + " à " + PokemonRepository.LAST_GENERATION + ") – cliquez sur un en-tête pour trier");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    status.setText("Erreur de chargement");
                    quizPanel.setPokemons(List.of());
                    settingsPanel.setPokemons(List.of());
                    JOptionPane.showMessageDialog(PokedexFrame.this,
                            "Impossible de récupérer les Pokémon :\n"
                                    + Objects.requireNonNullElse(cause.getMessage(), cause.toString()),
                            "Erreur", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }
}
