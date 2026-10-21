package fr.sdv.pokedex;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;

/**
 * Dessine une ligne de la liste des suggestions du quiz : « Dracaufeu (Charizard) », le nom anglais en gris.
 * Quand les deux noms sont identiques (Pikachu), le nom n'est écrit qu'une fois.
 */
public final class SuggestionRenderer extends JPanel implements ListCellRenderer<Pokemon> {

    private static final Color ENGLISH = new Color(120, 120, 120);
    private static final Color DISABLED_TEXT = new Color(150, 150, 150);
    private static final Color DISABLED_SELECTION = new Color(225, 225, 225);

    private final JLabel frenchLabel = new JLabel();
    private final JLabel englishLabel = new JLabel();

    public SuggestionRenderer() {
        super(new BorderLayout(5, 0));
        setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
        add(frenchLabel, BorderLayout.WEST);
        add(englishLabel, BorderLayout.CENTER);
    }

    /** Swing appelle cette méthode pour chaque ligne visible : on remplit le même panneau, puis Swing le dessine. */
    @Override
    public Component getListCellRendererComponent(JList<? extends Pokemon> list, Pokemon pokemon, int index,
                                                  boolean selected, boolean focused) {
        frenchLabel.setText(pokemon.nameFr());
        boolean sameName = pokemon.nameFr().equalsIgnoreCase(pokemon.nameEn());
        englishLabel.setText(sameName ? "" : "(" + pokemon.nameEn() + ")");
        frenchLabel.setFont(list.getFont());
        englishLabel.setFont(list.getFont());

        Color background;
        Color french;
        Color english;
        if (!list.isEnabled()) {
            // après la réponse : la liste est grisée, la ligne choisie reste repérable
            background = selected ? DISABLED_SELECTION : list.getBackground();
            french = DISABLED_TEXT;
            english = DISABLED_TEXT;
        } else if (selected) {
            background = list.getSelectionBackground();
            french = list.getSelectionForeground();
            english = mix(french, background); // un gris clair, lisible sur la couleur de sélection
        } else {
            background = list.getBackground();
            french = list.getForeground();
            english = ENGLISH;
        }
        setBackground(background);
        frenchLabel.setForeground(french);
        englishLabel.setForeground(english);
        return this;
    }

    /** Un mélange des deux couleurs : trois quarts de la première, un quart de la seconde. */
    private static Color mix(Color main, Color other) {
        return new Color((3 * main.getRed() + other.getRed()) / 4,
                (3 * main.getGreen() + other.getGreen()) / 4,
                (3 * main.getBlue() + other.getBlue()) / 4);
    }
}
