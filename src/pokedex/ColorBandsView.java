package pokedex;

import javax.swing.JComponent;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/**
 * Affiche deux cases côte à côte : à gauche les bandes de couleurs (l'indice),
 * à droite un « ? » ou le vrai sprite une fois la réponse révélée.
 * Les deux cases utilisent la même échelle entière, pour garder des pixels nets et bien alignés.
 */
public final class ColorBandsView extends JComponent {

    private static final int GAP = 24;          // espace entre les deux cases
    private static final int PADDING = 8;       // marge intérieure des cases
    private static final int CAPTION_GAP = 4;   // espace entre une case et sa légende
    private static final Color BOX_BACKGROUND = new Color(246, 246, 246);
    private static final Color BOX_BORDER = new Color(200, 200, 200);
    private static final Color QUESTION_MARK = new Color(185, 185, 185);
    private static final Color CAPTION = new Color(110, 110, 110);
    private static final Color MESSAGE = new Color(70, 70, 70);

    private ColorBands bands;
    private BufferedImage sprite;
    private boolean revealed;
    private String message;

    public ColorBandsView() {
        setFont(UIManager.getFont("Label.font"));
        setPreferredSize(new Dimension(640, 320));
    }

    /** Affiche les bandes d'un Pokémon ; le sprite n'est dessiné que si revealed vaut true. */
    public void show(ColorBands bands, BufferedImage sprite, boolean revealed) {
        this.bands = bands;
        this.sprite = sprite;
        this.revealed = revealed;
        this.message = null;
        repaint();
    }

    /** Dévoile le sprite à côté des bandes. */
    public void reveal() {
        revealed = true;
        repaint();
    }

    /** Affiche un texte centré à la place des cases (une ligne par \n). */
    public void setMessage(String message) {
        this.message = message;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Insets insets = getInsets();
            Rectangle area = new Rectangle(insets.left, insets.top,
                    getWidth() - insets.left - insets.right, getHeight() - insets.top - insets.bottom);
            if (message != null) {
                paintMessage(g, area);
            } else if (bands != null) {
                paintBoxes(g, area);
            }
        } finally {
            g.dispose();
        }
    }

    private void paintMessage(Graphics2D g, Rectangle area) {
        g.setFont(getFont().deriveFont(getFont().getSize2D() + 4));
        g.setColor(MESSAGE);
        FontMetrics metrics = g.getFontMetrics();
        String[] lines = message.split("\n");
        int lineHeight = metrics.getHeight() + 4;
        int y = area.y + (area.height - lines.length * lineHeight) / 2 + metrics.getAscent();
        for (String line : lines) {
            g.drawString(line, area.x + (area.width - metrics.stringWidth(line)) / 2, y);
            y += lineHeight;
        }
    }

    private void paintBoxes(Graphics2D g, Rectangle area) {
        Font captionFont = getFont();
        FontMetrics captionMetrics = g.getFontMetrics(captionFont);
        int captionHeight = CAPTION_GAP + captionMetrics.getHeight();

        // la plus grande échelle entière qui fait tenir les deux cases et leurs légendes
        int canvasWidth = bands.canvasWidth();
        int canvasHeight = bands.canvasHeight();
        int scaleX = (area.width - GAP - 4 * PADDING) / (2 * canvasWidth);
        int scaleY = (area.height - captionHeight - 2 * PADDING) / canvasHeight;
        int scale = Math.max(1, Math.min(scaleX, scaleY));

        int boxWidth = canvasWidth * scale + 2 * PADDING;
        int boxHeight = canvasHeight * scale + 2 * PADDING;
        int left = area.x + (area.width - (2 * boxWidth + GAP)) / 2;
        int top = area.y + (area.height - (boxHeight + captionHeight)) / 2;
        int right = left + boxWidth + GAP;

        paintBox(g, left, top, boxWidth, boxHeight);
        paintBands(g, left + PADDING, top + PADDING, scale);

        paintBox(g, right, top, boxWidth, boxHeight);
        if (revealed && sprite != null) {
            paintSprite(g, right + PADDING, top + PADDING, scale);
        } else {
            paintQuestionMark(g, right, top, boxWidth, boxHeight);
        }

        g.setFont(captionFont);
        g.setColor(CAPTION);
        int captionY = top + boxHeight + CAPTION_GAP + captionMetrics.getAscent();
        g.drawString("Indice", left + (boxWidth - captionMetrics.stringWidth("Indice")) / 2, captionY);
        g.drawString("Réponse", right + (boxWidth - captionMetrics.stringWidth("Réponse")) / 2, captionY);
    }

    private static void paintBox(Graphics2D g, int x, int y, int width, int height) {
        g.setColor(BOX_BACKGROUND);
        g.fillRect(x, y, width, height);
        g.setColor(BOX_BORDER);
        g.drawRect(x, y, width - 1, height - 1);
    }

    /**
     * Le Pokémon est centré horizontalement et posé en bas de la case. Le décalage est le même
     * pour les bandes et pour le sprite : les deux dessins se superposent exactement.
     */
    private int offsetX() {
        Rectangle bounds = bands.bounds();
        return (bands.canvasWidth() - bounds.width) / 2 - bounds.x;
    }

    private int offsetY() {
        Rectangle bounds = bands.bounds();
        return bands.canvasHeight() - bounds.height - bounds.y;
    }

    private void paintBands(Graphics2D g, int originX, int originY, int scale) {
        int dx = offsetX();
        int dy = offsetY();
        for (ColorBands.Band band : bands.bands()) {
            int x = originX + (band.left() + dx) * scale;
            int y = originY + (band.top() + dy) * scale;
            int width = (band.right() - band.left()) * scale;
            int height = (band.bottom() - band.top()) * scale;
            g.setColor(band.color());
            g.fillRect(x, y, width, height);
            if (width >= 4 && height >= 4) { // sur une bande très fine, la bordure cacherait la couleur
                g.setColor(band.color().darker());
                g.drawRect(x, y, width - 1, height - 1);
            }
        }
    }

    private void paintSprite(Graphics2D g, int originX, int originY, int scale) {
        Rectangle bounds = bands.bounds();
        int x = originX + (bounds.x + offsetX()) * scale;
        int y = originY + (bounds.y + offsetY()) * scale;
        // agrandissement « pixel art » : chaque pixel devient un carré net, sans flou
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(sprite,
                x, y, x + bounds.width * scale, y + bounds.height * scale,
                bounds.x, bounds.y, bounds.x + bounds.width, bounds.y + bounds.height,
                null);
    }

    private void paintQuestionMark(Graphics2D g, int x, int y, int width, int height) {
        Font font = getFont().deriveFont(Font.BOLD, height * 0.55f);
        // centrage sur le dessin réel du caractère, plus précis que les métriques de la police
        Rectangle2D shape = font.createGlyphVector(g.getFontRenderContext(), "?").getVisualBounds();
        g.setFont(font);
        g.setColor(QUESTION_MARK);
        g.drawString("?",
                (float) (x + (width - shape.getWidth()) / 2 - shape.getX()),
                (float) (y + (height - shape.getHeight()) / 2 - shape.getY()));
    }
}
