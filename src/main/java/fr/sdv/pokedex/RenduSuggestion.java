package fr.sdv.pokedex;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;

// Dessine une ligne de la liste des suggestions : « Dracaufeu (Charizard) », avec le nom anglais en gris.
// Quand les deux noms sont pareils (Pikachu), le nom n'est écrit qu'une fois.
public class RenduSuggestion extends JPanel implements TableCellRenderer {

    private static final Color GRIS_ANGLAIS = new Color(120, 120, 120);
    private static final Color TEXTE_DESACTIVE = new Color(150, 150, 150);
    private static final Color SELECTION_DESACTIVEE = new Color(225, 225, 225);

    private JLabel labelNom = new JLabel();
    private JLabel labelNomAnglais = new JLabel();

    public RenduSuggestion() {
        super(new BorderLayout(5, 0));
        setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
        add(labelNom, BorderLayout.WEST);
        add(labelNomAnglais, BorderLayout.CENTER);
    }

    // le tableau appelle cette méthode pour chaque ligne qu'il dessine
    @Override
    public Component getTableCellRendererComponent(JTable table, Object valeur, boolean selectionnee,
                                                   boolean focus, int ligne, int colonne) {
        Pokemon pokemon = (Pokemon) valeur;
        labelNom.setText(pokemon.getNom());
        if (pokemon.getNom().equalsIgnoreCase(pokemon.getNomAnglais())) {
            labelNomAnglais.setText("");
        } else {
            labelNomAnglais.setText("(" + pokemon.getNomAnglais() + ")");
        }

        if (!table.isEnabled()) {
            // après la réponse : tout est grisé, mais la ligne choisie reste visible
            if (selectionnee) {
                setBackground(SELECTION_DESACTIVEE);
            } else {
                setBackground(table.getBackground());
            }
            labelNom.setForeground(TEXTE_DESACTIVE);
            labelNomAnglais.setForeground(TEXTE_DESACTIVE);
        } else if (selectionnee) {
            setBackground(table.getSelectionBackground());
            labelNom.setForeground(table.getSelectionForeground());
            // un gris clair, lisible sur le bleu de la sélection
            labelNomAnglais.setForeground(melanger(table.getSelectionForeground(), table.getSelectionBackground()));
        } else {
            setBackground(table.getBackground());
            labelNom.setForeground(table.getForeground());
            labelNomAnglais.setForeground(GRIS_ANGLAIS);
        }
        return this;
    }

    // trois quarts de la première couleur, un quart de la seconde
    private Color melanger(Color a, Color b) {
        return new Color((3 * a.getRed() + b.getRed()) / 4,
                (3 * a.getGreen() + b.getGreen()) / 4,
                (3 * a.getBlue() + b.getBlue()) / 4);
    }
}
