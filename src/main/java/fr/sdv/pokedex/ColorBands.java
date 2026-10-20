package fr.sdv.pokedex;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Le résumé d'un sprite pour le quiz : on le découpe en bandes horizontales (du haut vers le bas)
 * et chaque bande garde la couleur la plus fréquente à ce niveau, ainsi que la largeur du Pokémon à cet endroit.
 *
 * @param canvasWidth  largeur du sprite d'origine (96 px), pour dessiner tous les Pokémon à la même échelle
 * @param canvasHeight hauteur du sprite d'origine
 * @param bounds       le rectangle qui contient les pixels visibles du Pokémon
 * @param bands        les bandes, de haut en bas (une section vide, sans pixel visible, n'a pas de bande)
 */
public record ColorBands(int canvasWidth, int canvasHeight, Rectangle bounds, List<Band> bands) {

    /** Une bande, en pixels du sprite (bottom et right exclus), avec sa couleur dominante. */
    public record Band(int top, int bottom, int left, int right, Color color) {
    }

    /** En dessous de cette opacité, un pixel est considéré comme transparent. */
    private static final int MIN_ALPHA = 128;

    public static ColorBands from(BufferedImage sprite, int sections) {
        Rectangle bounds = visibleBounds(sprite);
        List<Band> bands = new ArrayList<>();
        // impossible d'avoir plus de sections que de lignes de pixels
        int count = Math.min(sections, bounds.height);
        for (int i = 0; i < count; i++) {
            int top = bounds.y + i * bounds.height / count;
            int bottom = bounds.y + (i + 1) * bounds.height / count;
            Band band = band(sprite, bounds, top, bottom);
            if (band != null) {
                bands.add(band);
            }
        }
        return new ColorBands(sprite.getWidth(), sprite.getHeight(), bounds, List.copyOf(bands));
    }

    private static Band band(BufferedImage sprite, Rectangle bounds, int top, int bottom) {
        // Les couleurs très proches sont regroupées (32 nuances par canal au lieu de 256),
        // sinon un dégradé compterait comme plein de couleurs différentes.
        // Pour chaque groupe : {nombre de pixels, somme du rouge, somme du vert, somme du bleu}
        Map<Integer, long[]> groups = new HashMap<>();
        int left = Integer.MAX_VALUE;
        int right = -1;
        for (int y = top; y < bottom; y++) {
            for (int x = bounds.x; x < bounds.x + bounds.width; x++) {
                int argb = sprite.getRGB(x, y);
                if ((argb >>> 24) < MIN_ALPHA) {
                    continue;
                }
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                int key = (r >> 3) << 10 | (g >> 3) << 5 | (b >> 3);
                long[] group = groups.computeIfAbsent(key, k -> new long[4]);
                group[0]++;
                group[1] += r;
                group[2] += g;
                group[3] += b;
                left = Math.min(left, x);
                right = Math.max(right, x);
            }
        }
        if (groups.isEmpty()) {
            return null;
        }
        long[] mostCommon = Collections.max(groups.values(), Comparator.comparingLong(group -> group[0]));
        long pixels = mostCommon[0];
        Color color = new Color((int) (mostCommon[1] / pixels), (int) (mostCommon[2] / pixels),
                (int) (mostCommon[3] / pixels));
        return new Band(top, bottom, left, right + 1, color);
    }

    private static Rectangle visibleBounds(BufferedImage sprite) {
        int minX = sprite.getWidth(), minY = sprite.getHeight(), maxX = -1, maxY = -1;
        for (int y = 0; y < sprite.getHeight(); y++) {
            for (int x = 0; x < sprite.getWidth(); x++) {
                if ((sprite.getRGB(x, y) >>> 24) >= MIN_ALPHA) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        if (maxX < 0) {
            return new Rectangle(); // sprite entièrement transparent
        }
        return new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }
}
