package fr.sdv.pokedex;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;

/**
 * Les vies du quiz, dessinées en petits cœurs : pleins en rouge pour les vies qui restent, gris pour les autres.
 * Les cœurs sont tracés avec Java2D, et non avec un caractère cœur, qui s'afficherait mal avec certaines polices.
 */
public final class LivesIcon implements Icon {

    private static final int SIZE = 16; // taille d'un cœur, en pixels
    private static final int GAP = 4;
    private static final Color FULL = new Color(220, 40, 50);
    private static final Color FULL_BORDER = new Color(150, 20, 30);
    private static final Color EMPTY = new Color(215, 215, 215);
    private static final Color EMPTY_BORDER = new Color(160, 160, 160);

    /** Un cœur dans un carré de 1 × 1 : deux bosses en haut, la pointe en bas au milieu. */
    private static final Shape HEART = heart();

    private final int lives;
    private final int total;

    public LivesIcon(int lives, int total) {
        this.lives = lives;
        this.total = total;
    }

    private static Shape heart() {
        Path2D.Double path = new Path2D.Double();
        path.moveTo(0.5, 0.28);
        path.curveTo(0.5, 0.08, 0.25, 0.0, 0.11, 0.1);
        path.curveTo(-0.05, 0.22, 0.0, 0.52, 0.5, 0.92);
        path.curveTo(1.0, 0.52, 1.05, 0.22, 0.89, 0.1);
        path.curveTo(0.75, 0.0, 0.5, 0.08, 0.5, 0.28);
        path.closePath();
        return path;
    }

    @Override
    public void paintIcon(Component c, Graphics graphics, int x, int y) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setStroke(new BasicStroke(1f));
            for (int i = 0; i < total; i++) {
                AffineTransform position = AffineTransform.getTranslateInstance(x + i * (SIZE + GAP), y);
                position.scale(SIZE - 1, SIZE - 1); // - 1 : le trait du bord reste dans l'icône
                Shape shape = position.createTransformedShape(HEART);
                boolean full = i < lives;
                g.setColor(full ? FULL : EMPTY);
                g.fill(shape);
                g.setColor(full ? FULL_BORDER : EMPTY_BORDER);
                g.draw(shape);
            }
        } finally {
            g.dispose();
        }
    }

    @Override
    public int getIconWidth() {
        return total * SIZE + (total - 1) * GAP;
    }

    @Override
    public int getIconHeight() {
        return SIZE;
    }
}
