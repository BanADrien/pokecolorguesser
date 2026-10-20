package fr.sdv.pokedex;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Donne la liste des Pokémon : depuis le fichier cache s'il existe, sinon depuis PokéAPI.
 * PokéAPI demande de garder les données en local plutôt que de tout re-télécharger à chaque lancement.
 */
public class PokemonRepository {

    public static final int FIRST_GENERATION = 1;
    public static final int LAST_GENERATION = 8; // toutes les générations avant la 9

    /** Première ligne du fichier cache (sans private : les tests s'en servent pour écrire un cache valide). */
    static final String HEADER =
            "id;nom;nom_anglais;generation;types;taille_m;poids_kg;pv;attaque;defense;atq_spe;def_spe;vitesse;image";

    private final Path cacheFile;
    private final PokeApiClient api = new PokeApiClient();

    public PokemonRepository(Path cacheFile) {
        this.cacheFile = cacheFile;
    }

    public List<Pokemon> loadAll(BiConsumer<Integer, Integer> progress) throws IOException, InterruptedException {
        if (Files.exists(cacheFile)) {
            try {
                return readCache();
            } catch (IOException | RuntimeException e) {
                System.err.println("Cache illisible, nouveau téléchargement : " + e.getMessage());
            }
        }
        List<Pokemon> pokemons = api.fetchGenerations(FIRST_GENERATION, LAST_GENERATION, progress);
        writeCache(pokemons);
        return pokemons;
    }

    private List<Pokemon> readCache() throws IOException {
        List<String> lines = Files.readAllLines(cacheFile, StandardCharsets.UTF_8);
        if (lines.isEmpty() || !lines.getFirst().equals(HEADER)) {
            throw new IOException("format de cache inattendu");
        }
        List<Pokemon> pokemons = new ArrayList<>();
        for (String line : lines.subList(1, lines.size())) {
            String[] f = line.split(";", -1);
            pokemons.add(new Pokemon(
                    Integer.parseInt(f[0]), f[1], f[2], Integer.parseInt(f[3]),
                    List.of(f[4].split("/")),
                    Double.parseDouble(f[5]), Double.parseDouble(f[6]),
                    Integer.parseInt(f[7]), Integer.parseInt(f[8]), Integer.parseInt(f[9]),
                    Integer.parseInt(f[10]), Integer.parseInt(f[11]), Integer.parseInt(f[12]),
                    f[13]));
        }
        return pokemons;
    }

    private void writeCache(List<Pokemon> pokemons) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        for (Pokemon p : pokemons) {
            lines.add("%d;%s;%s;%d;%s;%s;%s;%d;%d;%d;%d;%d;%d;%s".formatted(
                    p.id(), p.nameFr(), p.nameEn(), p.generation(), String.join("/", p.types()),
                    p.heightM(), p.weightKg(),
                    p.hp(), p.attack(), p.defense(), p.specialAttack(), p.specialDefense(), p.speed(),
                    p.spriteUrl()));
        }
        Path folder = cacheFile.toAbsolutePath().getParent();
        Files.createDirectories(folder);
        Files.write(cacheFile, lines, StandardCharsets.UTF_8);
    }
}
