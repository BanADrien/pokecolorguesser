package fr.sdv.pokedex;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

// Les images des Pokémon : chaque image est téléchargée une seule fois, puis relue depuis cache/sprites/
public class CacheImages {

    // taille maximale d'une image dans le tableau, en pixels
    public static final int TAILLE_MAX = 64;

    private Path dossier;
    private ApiPokemon api = new ApiPokemon();
    // les petites images du tableau, rangées par numéro (utilisées seulement dans le thread de Swing)
    private HashMap<Integer, ImageIcon> icones = new HashMap<>();

    public CacheImages(Path dossier) {
        this.dossier = dossier;
    }

    // null tant que l'image n'est pas chargée
    public ImageIcon getIcone(Pokemon pokemon) {
        return icones.get(pokemon.getId());
    }

    public void ajouterIcone(Pokemon pokemon, ImageIcon icone) {
        icones.put(pokemon.getId(), icone);
    }

    // L'image d'origine (96 × 96), téléchargée si besoin, ou null si elle est indisponible.
    // synchronized : un seul thread à la fois lit ou écrit les fichiers d'images.
    public synchronized BufferedImage chargerImage(Pokemon pokemon) {
        if (pokemon.getAdresseImage().isEmpty()) {
            return null;
        }
        Path fichier = dossier.resolve(pokemon.getId() + ".png");
        try {
            if (!Files.exists(fichier)) {
                byte[] octets = api.telecharger(pokemon.getAdresseImage());
                Files.createDirectories(dossier);
                Files.write(fichier, octets);
            }
            BufferedImage image = ImageIO.read(fichier.toFile());
            if (image == null) {
                // fichier abîmé : on le supprime pour qu'il soit téléchargé à nouveau
                Files.deleteIfExists(fichier);
                throw new IOException("fichier image illisible");
            }
            return image;
        } catch (IOException e) {
            System.err.println("Image du Pokémon n°" + pokemon.getId() + " indisponible : " + e.getMessage());
            return null;
        }
    }

    // l'icône du tableau : l'image sans ses bords transparents, réduite si elle est trop grande
    public static ImageIcon creerIcone(BufferedImage image) {
        return new ImageIcon(reduire(rogner(image)));
    }

    private static BufferedImage rogner(BufferedImage image) {
        // le plus petit rectangle qui contient tous les pixels visibles
        int gauche = image.getWidth();
        int haut = image.getHeight();
        int droite = -1;
        int bas = -1;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                Color pixel = new Color(image.getRGB(x, y), true);
                if (pixel.getAlpha() != 0) {
                    gauche = Math.min(gauche, x);
                    haut = Math.min(haut, y);
                    droite = Math.max(droite, x);
                    bas = Math.max(bas, y);
                }
            }
        }
        if (droite == -1) {
            return image; // image entièrement transparente
        }
        return image.getSubimage(gauche, haut, droite - gauche + 1, bas - haut + 1);
    }

    private static BufferedImage reduire(BufferedImage image) {
        int plusGrandCote = Math.max(image.getWidth(), image.getHeight());
        if (plusGrandCote <= TAILLE_MAX) {
            return image; // pas d'agrandissement : le pixel art deviendrait flou
        }
        double echelle = (double) TAILLE_MAX / plusGrandCote;
        int largeur = Math.max(1, (int) Math.round(image.getWidth() * echelle));
        int hauteur = Math.max(1, (int) Math.round(image.getHeight() * echelle));
        BufferedImage petite = new BufferedImage(largeur, hauteur, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = petite.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(image, 0, 0, largeur, hauteur, null);
        g.dispose();
        return petite;
    }
}
