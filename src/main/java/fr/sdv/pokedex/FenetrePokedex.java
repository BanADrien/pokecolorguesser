package fr.sdv.pokedex;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.nio.file.Path;
import java.util.ArrayList;

// La fenêtre principale : trois onglets (Pokédex, Quiz, Paramètres) et une barre d'état en bas
public class FenetrePokedex extends JFrame {

    private static final Path FICHIER_PARAMETRES = Path.of("parametres.properties");

    private CacheImages images = new CacheImages(Path.of("cache", "sprites"));
    private PanneauPokedex panneauPokedex = new PanneauPokedex(images);
    private Parametres parametres = Parametres.charger(FICHIER_PARAMETRES);
    private PanneauQuiz panneauQuiz = new PanneauQuiz(parametres, images);
    private PanneauParametres panneauParametres = new PanneauParametres(parametres, FICHIER_PARAMETRES, images);
    private JLabel labelEtat = new JLabel("Chargement…");
    private JProgressBar barreProgression = new JProgressBar();

    public FenetrePokedex() {
        super("Pokédex – Générations 1 à " + Pokemon.DERNIERE_GENERATION);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JTabbedPane onglets = new JTabbedPane();
        onglets.addTab("Pokédex", panneauPokedex);
        onglets.addTab("Quiz", panneauQuiz);
        onglets.addTab("Paramètres", panneauParametres);

        add(onglets, BorderLayout.CENTER);
        add(creerBarreEtat(), BorderLayout.SOUTH);
        setSize(1230, 760);
        setLocationRelativeTo(null);
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
        panneauPokedex.setPokemons(pokemons);
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
        panneauPokedex.redessinerTableau();
    }
}
