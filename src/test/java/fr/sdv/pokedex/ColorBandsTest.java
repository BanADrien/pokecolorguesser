package fr.sdv.pokedex;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests du découpage en bandes, sur de petites images dessinées pixel par pixel (aucune fenêtre). */
class ColorBandsTest {

    private static final int RED = 0xFFFF0000; // format ARGB : opacité, rouge, vert, bleu
    private static final int BLUE = 0xFF0000FF;
    private static final int GREEN = 0xFF00FF00;

    /** Une image 96×96 entièrement transparente, comme le fond d'un sprite. */
    private static BufferedImage transparentSprite() {
        return new BufferedImage(96, 96, BufferedImage.TYPE_INT_ARGB);
    }

    /** Colorie le rectangle (x, y, largeur, hauteur) avec une couleur ARGB. */
    private static void fill(BufferedImage image, int x, int y, int width, int height, int argb) {
        for (int row = y; row < y + height; row++) {
            for (int column = x; column < x + width; column++) {
                image.setRGB(column, row, argb);
            }
        }
    }

    @ParameterizedTest(name = "{0} section(s), {1} ligne(s) visibles -> {2} bande(s)")
    @CsvSource({
            "1, 64, 1",
            "8, 64, 8",
            "32, 64, 32",
            "32, 20, 20",
            "32, 3, 3",
            "8, 1, 1"
    })
    void bandCountIsSectionsLimitedByVisibleHeight(int sections, int visibleHeight, int expectedBands) {
        BufferedImage sprite = transparentSprite();
        fill(sprite, 30, 10, 40, visibleHeight, RED);
        assertEquals(expectedBands, ColorBands.from(sprite, sections).bands().size());
    }

    @Test
    void bandsAreStackedFromTopToBottomInsideVisibleBounds() {
        BufferedImage sprite = transparentSprite();
        fill(sprite, 30, 20, 40, 64, RED);

        ColorBands result = ColorBands.from(sprite, 8);

        assertEquals(96, result.canvasWidth());
        assertEquals(96, result.canvasHeight());
        assertEquals(new Rectangle(30, 20, 40, 64), result.bounds());
        List<ColorBands.Band> bands = result.bands();
        assertEquals(20, bands.getFirst().top());
        assertEquals(84, bands.getLast().bottom());
        for (int i = 0; i < bands.size(); i++) {
            ColorBands.Band band = bands.get(i);
            assertEquals(8, band.bottom() - band.top(), "64 lignes / 8 bandes");
            assertEquals(30, band.left());
            assertEquals(70, band.right(), "right est exclu : 30 + 40");
            assertEquals(Color.RED, band.color());
            if (i > 0) {
                assertEquals(bands.get(i - 1).bottom(), band.top(), "les bandes se suivent sans trou");
            }
        }
    }

    @Test
    void bandColorIsTheMostFrequentColor() {
        BufferedImage sprite = transparentSprite();
        fill(sprite, 10, 10, 7, 4, RED);  // 28 pixels rouges
        fill(sprite, 17, 10, 3, 4, BLUE); // 12 pixels bleus sur les mêmes lignes

        ColorBands.Band band = ColorBands.from(sprite, 1).bands().getFirst();

        assertEquals(Color.RED, band.color());
        assertEquals(10, band.left());
        assertEquals(20, band.right(), "la largeur compte tous les pixels visibles, rouges et bleus");
    }

    @Test
    void closeShadesAreGroupedAndAveraged() {
        BufferedImage sprite = transparentSprite();
        fill(sprite, 0, 0, 2, 1, 0xFFF80000); // 2 pixels rouges (248, 0, 0)
        fill(sprite, 2, 0, 2, 1, 0xFFFF0000); // 2 pixels rouges (255, 0, 0), même groupe de nuances
        fill(sprite, 4, 0, 3, 1, BLUE);       // 3 pixels bleus : plus que chaque rouge, moins que les deux réunis

        ColorBands.Band band = ColorBands.from(sprite, 1).bands().getFirst();

        assertEquals(new Color(251, 0, 0), band.color(), "moyenne des rouges : (2 × 248 + 2 × 255) / 4");
    }

    @Test
    void sectionsWithoutVisiblePixelsHaveNoBand() {
        BufferedImage sprite = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        fill(sprite, 0, 0, 10, 1, RED);  // première ligne
        fill(sprite, 0, 9, 10, 1, BLUE); // dernière ligne, avec 8 lignes vides entre les deux

        List<ColorBands.Band> bands = ColorBands.from(sprite, 10).bands();

        assertEquals(2, bands.size());
        assertEquals(new ColorBands.Band(0, 1, 0, 10, Color.RED), bands.get(0));
        assertEquals(new ColorBands.Band(9, 10, 0, 10, Color.BLUE), bands.get(1));
    }

    @Test
    void fullyTransparentSpriteHasNoBand() {
        ColorBands result = ColorBands.from(transparentSprite(), 8);

        assertTrue(result.bands().isEmpty());
        assertTrue(result.bounds().isEmpty());
        assertEquals(96, result.canvasWidth());
        assertEquals(96, result.canvasHeight());
    }

    @Test
    void almostTransparentPixelsAreIgnored() {
        BufferedImage sprite = transparentSprite();
        fill(sprite, 10, 10, 5, 5, 0x7FFF0000);  // opacité 127 : considéré comme transparent
        fill(sprite, 40, 50, 2, 2, 0x8000FF00);  // opacité 128 : visible

        ColorBands result = ColorBands.from(sprite, 4);

        assertEquals(new Rectangle(40, 50, 2, 2), result.bounds());
        assertEquals(2, result.bands().size(), "seulement 2 lignes visibles");
        assertEquals(Color.GREEN, result.bands().getFirst().color());
    }
}
