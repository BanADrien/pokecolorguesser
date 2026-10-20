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
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.nio.file.Path;
import java.util.ArrayList;

// La fenêtre principale : trois onglets (Pokédex, Quiz, Paramètres) et une barre d'état en bas
public class FenetrePokedex extends JFrame {

    private static final int[] LARGEURS_COLONNES = {45, 76, 130, 130, 45, 150, 75, 75, 55, 60, 60, 65, 65, 60, 55};
    private static final Path FICHIER_PARAMETRES = Path.of("parametres.properties");

    private CacheImages images = new CacheImages(Path.of("cache", "sprites"));
    private ModeleTableau modele = new ModeleTableau(images);
    private JTable tableau = new JTable(modele);
    private Parametres parametres = Parametres.charger(FICHIER_PARAMETRES);
    private PanneauQuiz panneauQuiz = new PanneauQuiz(parametres, images);
    private PanneauParametres panneauParametres = new PanneauParametres(parametres, FICHIER_PARAMETRES, images);
    private JLabel labelEtat = new JLabel("Chargement…");
    private JProgressBar barreProgression = new JProgressBar();

    public FenetrePokedex() {
        super("Pokédex – Générations 1 à " + Pokemon.DERNIERE_GENERATION);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JTabbedPane onglets = new JTabbedPane();
        onglets.addTab("Pokédex", new JScrollPane(creerTableau()));
        onglets.addTab("Quiz", panneauQuiz);
        onglets.addTab("Paramètres", panneauParametres);

        add(onglets, BorderLayout.CENTER);
        add(creerBarreEtat(), BorderLayout.SOUTH);
        setSize(1230, 760);
        setLocationRelativeTo(null);
    }

    private JTable creerTableau() {
        // un clic sur un en-tête trie la colonne (sauf celle des images)
        TableRowSorter<ModeleTableau> tri = new TableRowSorter<>(modele);
        tri.setSortable(ModeleTableau.COLONNE_IMAGE, false);
        tableau.setRowSorter(tri);
        tableau.setFillsViewportHeight(true);
        tableau.setRowHeight(CacheImages.TAILLE_MAX + 8);
        for (int i = 0; i < LARGEURS_COLONNES.length; i++) {
            tableau.getColumnModel().getColumn(i).setPreferredWidth(LARGEURS_COLONNES[i]);
        }
        return tableau;
    }

    private JPanel creerBarreEtat() {
        barreProgression.setIndeterminate(true);
        barreProgression.setStringPainted(true);
        JPanel barreEtat = new JPanel(new BorderLayout(8, 0));
        barreEtat.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        barreEtat.add(labelEtat, BorderLayout.CENTER);
        barreEtat.add(barreProgression, BorderLayout.EAST);
        return barreEtat;
    }

    public void chargerPokemons() {
        ChargementPokedex chargement = new ChargementPokedex(this,
                new CachePokedex(Path.of("cache", "pokedex.csv")), images);
        chargement.start();
    }

    // les méthodes suivantes sont appelées par ChargementPokedex, dans le thread de Swing

    public void afficherProgression(int fait, int total) {
        barreProgression.setIndeterminate(false);
        barreProgression.setMaximum(total);
        barreProgression.setValue(fait);
        labelEtat.setText("Téléchargement depuis PokéAPI… " + fait + " / " + total);
    }

    public void afficherPokemons(ArrayList<Pokemon> pokemons) {
        barreProgression.setVisible(false);
        modele.setPokemons(pokemons);
        panneauQuiz.setPokemons(pokemons);
        panneauParametres.setPokemons(pokemons);
        labelEtat.setText(pokemons.size() + " Pokémon (générations 1 à " + Pokemon.DERNIERE_GENERATION
                + ") – cliquez sur un en-tête pour trier");
    }

    public void afficherErreur(Exception erreur) {
        barreProgression.setVisible(false);
        labelEtat.setText("Erreur de chargement");
        panneauQuiz.setPokemons(new ArrayList<>());
        panneauParametres.setPokemons(new ArrayList<>());
        String message = erreur.getMessage();
        if (message == null) {
            message = erreur.toString();
        }
        JOptionPane.showMessageDialog(this, "Impossible de récupérer les Pokémon :\n" + message,
                "Erreur", JOptionPane.ERROR_MESSAGE);
    }

    public void redessinerTableau() {
        tableau.repaint();
    }
}
