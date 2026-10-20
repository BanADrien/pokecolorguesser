package pokedex;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Charge les images des Pokémon à la demande : pour les lignes affichées du tableau et pour le quiz.
 * Chaque image est téléchargée une seule fois, puis relue depuis le dossier cache.
 */
public class SpriteCache {

    /** Taille maximale d'une image dans le tableau, en pixels. */
    public static final int MAX_SIZE = 64;

    private final Path folder;
    private final Runnable onImageLoaded;
    private final PokeApiClient api = new PokeApiClient();
    private final ExecutorService pool = Executors.newFixedThreadPool(8, Thread.ofPlatform().daemon().factory());
    private final Map<Integer, Icon> icons = new ConcurrentHashMap<>();
    private final Set<Integer> requested = ConcurrentHashMap.newKeySet();

    /** onImageLoaded est appelé (dans le thread Swing) à chaque fois qu'une image devient disponible. */
    public SpriteCache(Path folder, Runnable onImageLoaded) {
        this.folder = folder;
        this.onImageLoaded = onImageLoaded;
    }

    /** Renvoie l'image si elle est prête ; sinon lance son chargement en arrière-plan et renvoie null. */
    public Icon get(Pokemon pokemon) {
        Icon icon = icons.get(pokemon.id());
        if (icon == null && !pokemon.spriteUrl().isEmpty() && requested.add(pokemon.id())) {
            pool.execute(() -> load(pokemon));
        }
        return icon;
    }

    private void load(Pokemon pokemon) {
        try {
            icons.put(pokemon.id(), new ImageIcon(trimAndShrink(loadOriginal(pokemon))));
            SwingUtilities.invokeLater(onImageLoaded);
        } catch (IOException e) {
            System.err.println("Image du Pokémon n°" + pokemon.id() + " indisponible : " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Renvoie le sprite d'origine (96×96, bords transparents compris), en le téléchargeant si besoin.
     * Bloquant : à appeler en dehors du thread Swing.
     */
    public BufferedImage loadOriginal(Pokemon pokemon) throws IOException, InterruptedException {
        if (pokemon.spriteUrl().isEmpty()) {
            throw new IOException("pas d'image pour ce Pokémon");
        }
        Path file = folder.resolve(pokemon.id() + ".png");
        if (!Files.exists(file)) {
            download(pokemon, file);
        }
        BufferedImage image = ImageIO.read(file.toFile());
        if (image == null) {
            Files.deleteIfExists(file);
            throw new IOException("fichier image illisible");
        }
        return image;
    }

    /**
     * On écrit d'abord dans un fichier temporaire au nom unique, pour ne jamais laisser une image
     * à moitié téléchargée, même si deux threads téléchargent la même image en même temps.
     */
    private void download(Pokemon pokemon, Path file) throws IOException, InterruptedException {
        byte[] bytes = api.download(pokemon.spriteUrl());
        Files.createDirectories(folder);
        Path temp = Files.createTempFile(folder, pokemon.id() + "-", ".tmp");
        try {
            Files.write(temp, bytes);
            Files.move(temp, file); // sans REPLACE_EXISTING : échoue si le fichier existe déjà
        } catch (FileAlreadyExistsException e) {
            // un autre thread a enregistré la même image entre-temps : on garde la sienne
        } finally {
            Files.deleteIfExists(temp); // ne fait rien si le déplacement a réussi
        }
    }

    /** Les sprites ont de larges bords transparents : on les enlève, puis on réduit si c'est trop grand. */
    private static BufferedImage trimAndShrink(BufferedImage image) {
        int minX = image.getWidth(), minY = image.getHeight(), maxX = -1, maxY = -1;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                boolean visible = (image.getRGB(x, y) >>> 24) != 0;
                if (visible) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        if (maxX < 0) {
            return image; // image entièrement transparente
        }
        BufferedImage trimmed = image.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);

        double scale = (double) MAX_SIZE / Math.max(trimmed.getWidth(), trimmed.getHeight());
        if (scale >= 1) {
            return trimmed; // pas d'agrandissement : le pixel art resterait flou
        }
        int width = Math.max(1, (int) Math.round(trimmed.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(trimmed.getHeight() * scale));
        BufferedImage shrunk = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = shrunk.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(trimmed, 0, 0, width, height, null);
        g.dispose();
        return shrunk;
    }
}
