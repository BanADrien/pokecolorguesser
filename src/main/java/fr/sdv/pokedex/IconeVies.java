package fr.sdv.pokedex;

import javax.swing.Icon;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;

// Les vies du quiz en petits cœurs : rouges pour les vies qui restent, gris pour les autres.
// On les dessine nous-mêmes : le caractère cœur s'affiche mal avec certaines polices.
public class IconeVies implements Icon {

    private static final int TAILLE = 16; // taille d'un cœur, en pixels
    private static final int ESPACE = 4;
    private static final Color ROUGE = new Color(220, 40, 50);
    private static final Color BORD_ROUGE = new Color(150, 20, 30);
    private static final Color GRIS = new Color(215, 215, 215);
    private static final Color BORD_GRIS = new Color(160, 160, 160);

    private int vies;
    private int total;

    public IconeVies(int vies, int total) {
        this.vies = vies;
        this.total = total;
    }

    @Override
    public void paintIcon(Component composant, Graphics graphics, int x, int y) {
        // une copie, pour ne pas changer les réglages du dessin du texte à côté
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (int i = 0; i < total; i++) {
            dessinerCoeur(g, x + i * (TAILLE + ESPACE), y, i < vies);
        }
        g.dispose();
    }

    private void dessinerCoeur(Graphics2D g, int x, int y, boolean plein) {
        double t = TAILLE - 1; // - 1 : le trait du bord reste dans l'icône
        // deux bosses en haut, la pointe en bas au milieu
        Path2D.Double coeur = new Path2D.Double();
        coeur.moveTo(x + 0.5 * t, y + 0.28 * t);
        coeur.curveTo(x + 0.5 * t, y + 0.08 * t, x + 0.25 * t, y, x + 0.11 * t, y + 0.1 * t);
        coeur.curveTo(x - 0.05 * t, y + 0.22 * t, x, y + 0.52 * t, x + 0.5 * t, y + 0.92 * t);
        coeur.curveTo(x + t, y + 0.52 * t, x + 1.05 * t, y + 0.22 * t, x + 0.89 * t, y + 0.1 * t);
        coeur.curveTo(x + 0.75 * t, y, x + 0.5 * t, y + 0.08 * t, x + 0.5 * t, y + 0.28 * t);
        coeur.closePath();

        if (plein) {
            g.setColor(ROUGE);
            g.fill(coeur);
            g.setColor(BORD_ROUGE);
        } else {
            g.setColor(GRIS);
            g.fill(coeur);
            g.setColor(BORD_GRIS);
        }
        g.draw(coeur);
    }

    @Override
    public int getIconWidth() {
        return total * TAILLE + (total - 1) * ESPACE;
    }

    @Override
    public int getIconHeight() {
        return TAILLE;
    }
}
