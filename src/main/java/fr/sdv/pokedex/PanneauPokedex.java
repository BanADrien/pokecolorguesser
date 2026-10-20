package fr.sdv.pokedex;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

// L'onglet Pokédex : la recherche et les filtres en haut, le tableau des Pokémon en dessous
public class PanneauPokedex extends JPanel {

    private static final int[] LARGEURS_COLONNES = {55, 85, 280, 280, 60, 420};
    // la région de chaque génération (case 0 : génération 1)
    private static final String[] REGIONS = {"Kanto", "Johto", "Hoenn", "Sinnoh", "Unys", "Kalos", "Alola", "Galar"};

    private ModeleTableau modele;
    private JTable tableau;
    private FiltrePokedex filtre = new FiltrePokedex();

    private JTextField champRecherche = new JTextField(16);
    private JComboBox<String> choixGeneration = new JComboBox<>();
    private JComboBox<String> choixType = new JComboBox<>();
    private JButton boutonReinitialiser = new JButton("Réinitialiser");
    private JLabel labelCompteur = new JLabel(" ");

    public PanneauPokedex(CacheImages images) {
        super(new BorderLayout());
        modele = new ModeleTableau(images);
        tableau = new JTable(modele);

        add(creerBarreRecherche(), BorderLayout.NORTH);
        add(new JScrollPane(creerTableau()), BorderLayout.CENTER);
        // tant que les Pokémon ne sont pas chargés, il n'y a rien à chercher
        activerRecherche(false);
    }

    private JPanel creerBarreRecherche() {
        champRecherche.setToolTipText("Nom français ou anglais (même un bout du nom), ou numéro");
        // à chaque lettre tapée ou effacée, le tableau est filtré à nouveau
        champRecherche.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                mettreAJourFiltre();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                mettreAJourFiltre();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                // changement de style du texte : rien à faire
            }
        });

        choixGeneration.addItem("Toutes");
        for (int i = 0; i < REGIONS.length; i++) {
            choixGeneration.addItem((i + 1) + " – " + REGIONS[i]);
        }
        choixGeneration.setMaximumRowCount(choixGeneration.getItemCount());

        choixType.addItem("Tous");
        for (String type : FiltrePokedex.TYPES) {
            choixType.addItem(type);
        }
        choixType.setMaximumRowCount(choixType.getItemCount());

        ActionListener changementDeFiltre = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mettreAJourFiltre();
            }
        };
        choixGeneration.addActionListener(changementDeFiltre);
        choixType.addActionListener(changementDeFiltre);
        boutonReinitialiser.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reinitialiser();
            }
        });

        JPanel champs = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        champs.add(new JLabel("Recherche :"));
        champs.add(champRecherche);
        champs.add(Box.createHorizontalStrut(12));
        champs.add(new JLabel("Génération :"));
        champs.add(choixGeneration);
        champs.add(Box.createHorizontalStrut(12));
        champs.add(new JLabel("Type :"));
        champs.add(choixType);
        champs.add(Box.createHorizontalStrut(12));
        champs.add(boutonReinitialiser);

        JPanel barre = new JPanel(new BorderLayout(12, 0));
        barre.setBorder(BorderFactory.createEmptyBorder(8, 2, 8, 12));
        barre.add(champs, BorderLayout.CENTER);
        barre.add(labelCompteur, BorderLayout.EAST);
        return barre;
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
        // le numéro et la génération au milieu de leur colonne (sinon le « 1 » colle au type d'à côté)
        DefaultTableCellRenderer auCentre = new DefaultTableCellRenderer();
        auCentre.setHorizontalAlignment(JLabel.CENTER);
        tableau.getColumnModel().getColumn(0).setCellRenderer(auCentre);
        tableau.getColumnModel().getColumn(4).setCellRenderer(auCentre);
        return tableau;
    }

    private void activerRecherche(boolean actif) {
        champRecherche.setEnabled(actif);
        choixGeneration.setEnabled(actif);
        choixType.setEnabled(actif);
        boutonReinitialiser.setEnabled(actif);
    }

    // appelée à la fin du chargement
    public void setPokemons(ArrayList<Pokemon> pokemons) {
        modele.setPokemons(pokemons);
        activerRecherche(true);
        mettreAJourFiltre();
    }

    // le texte tapé et les deux listes déroulantes sont lus à chaque changement
    private void mettreAJourFiltre() {
        filtre.setRecherche(champRecherche.getText());
        // l'élément 0 est « Toutes », puis viennent les générations 1 à 8 dans l'ordre
        filtre.setGeneration(choixGeneration.getSelectedIndex());
        if (choixType.getSelectedIndex() == 0) {
            filtre.setType(""); // « Tous »
        } else {
            filtre.setType((String) choixType.getSelectedItem());
        }
        modele.setFiltre(filtre);
        labelCompteur.setText(FiltrePokedex.texteCompteur(modele.getRowCount()));
    }

    private void reinitialiser() {
        champRecherche.setText("");
        choixGeneration.setSelectedIndex(0);
        choixType.setSelectedIndex(0);
        mettreAJourFiltre();
        champRecherche.requestFocusInWindow();
    }

    public void redessinerTableau() {
        tableau.repaint();
    }
}
