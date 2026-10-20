package pokedex;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.zip.GZIPInputStream;

/** Récupère les Pokémon sur PokéAPI (https://pokeapi.co), une API REST gratuite et sans clé. */
public class PokeApiClient {

    private static final String BASE_URL = "https://pokeapi.co/api/v2/";
    private static final int PARALLEL_REQUESTS = 16;
    private static final int MAX_ATTEMPTS = 3;

    private static final Map<String, String> TYPES_FR = Map.ofEntries(
            Map.entry("normal", "Normal"),
            Map.entry("fighting", "Combat"),
            Map.entry("flying", "Vol"),
            Map.entry("poison", "Poison"),
            Map.entry("ground", "Sol"),
            Map.entry("rock", "Roche"),
            Map.entry("bug", "Insecte"),
            Map.entry("ghost", "Spectre"),
            Map.entry("steel", "Acier"),
            Map.entry("fire", "Feu"),
            Map.entry("water", "Eau"),
            Map.entry("grass", "Plante"),
            Map.entry("electric", "Électrik"),
            Map.entry("psychic", "Psy"),
            Map.entry("ice", "Glace"),
            Map.entry("dragon", "Dragon"),
            Map.entry("dark", "Ténèbres"),
            Map.entry("fairy", "Fée"));

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /**
     * Récupère tous les Pokémon apparus entre firstGen et lastGen, triés par numéro.
     * progress reçoit (nombre téléchargé, total) au fur et à mesure, depuis plusieurs threads.
     */
    public List<Pokemon> fetchGenerations(int firstGen, int lastGen, BiConsumer<Integer, Integer> progress)
            throws IOException, InterruptedException {
        // 1. Chaque génération donne la liste des espèces apparues dans cette génération
        Map<Integer, Integer> generationBySpeciesId = new TreeMap<>();
        for (int gen = firstGen; gen <= lastGen; gen++) {
            Map<String, Object> generation = getJson(BASE_URL + "generation/" + gen + "/");
            for (Object species : Json.asArray(generation.get("pokemon_species"))) {
                generationBySpeciesId.put(idFromUrl((String) Json.get(species, "url")), gen);
            }
        }

        // 2. Le détail de chaque espèce, avec plusieurs requêtes en parallèle
        int total = generationBySpeciesId.size();
        AtomicInteger done = new AtomicInteger();
        progress.accept(0, total);

        ExecutorService pool = Executors.newFixedThreadPool(PARALLEL_REQUESTS);
        try {
            List<Future<Pokemon>> futures = new ArrayList<>();
            for (Map.Entry<Integer, Integer> entry : generationBySpeciesId.entrySet()) {
                futures.add(pool.submit(() -> {
                    Pokemon pokemon = fetchPokemon(entry.getKey(), entry.getValue());
                    progress.accept(done.incrementAndGet(), total);
                    return pokemon;
                }));
            }
            List<Pokemon> pokemons = new ArrayList<>(total);
            for (Future<Pokemon> future : futures) {
                pokemons.add(future.get());
            }
            return pokemons;
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw new IOException("Échec du téléchargement : " + cause.getMessage(), cause);
        } finally {
            pool.shutdownNow();
        }
    }

    private Pokemon fetchPokemon(int speciesId, int generation) throws IOException, InterruptedException {
        // L'espèce contient les noms traduits, le Pokémon contient les types et les statistiques
        Map<String, Object> species = getJson(BASE_URL + "pokemon-species/" + speciesId + "/");
        Map<String, Object> pokemon = getJson(defaultVarietyUrl(species, speciesId));

        List<String> types = new ArrayList<>();
        for (Object type : Json.asArray(pokemon.get("types"))) {
            String name = (String) Json.get(type, "type", "name");
            types.add(TYPES_FR.getOrDefault(name, name));
        }

        Map<String, Integer> stats = new HashMap<>();
        for (Object stat : Json.asArray(pokemon.get("stats"))) {
            String name = (String) Json.get(stat, "stat", "name");
            stats.put(name, ((Number) Json.get(stat, "base_stat")).intValue());
        }

        return new Pokemon(
                speciesId,
                localizedName(species, "fr"),
                localizedName(species, "en"),
                generation,
                List.copyOf(types),
                ((Number) pokemon.get("height")).intValue() / 10.0, // l'API donne des décimètres
                ((Number) pokemon.get("weight")).intValue() / 10.0, // et des hectogrammes
                stats.getOrDefault("hp", 0),
                stats.getOrDefault("attack", 0),
                stats.getOrDefault("defense", 0),
                stats.getOrDefault("special-attack", 0),
                stats.getOrDefault("special-defense", 0),
                stats.getOrDefault("speed", 0),
                Objects.requireNonNullElse((String) Json.get(pokemon, "sprites", "front_default"), ""));
    }

    private static String localizedName(Map<String, Object> species, String language) {
        for (Object name : Json.asArray(species.get("names"))) {
            if (language.equals(Json.get(name, "language", "name"))) {
                return (String) Json.get(name, "name");
            }
        }
        return (String) species.get("name");
    }

    /** Une espèce peut avoir plusieurs formes (ex. méga-évolutions) : on garde la forme par défaut. */
    private static String defaultVarietyUrl(Map<String, Object> species, int speciesId) {
        for (Object variety : Json.asArray(species.get("varieties"))) {
            if (Boolean.TRUE.equals(Json.get(variety, "is_default"))) {
                return (String) Json.get(variety, "pokemon", "url");
            }
        }
        return BASE_URL + "pokemon/" + speciesId + "/";
    }

    /** "https://pokeapi.co/api/v2/pokemon-species/25/" -> 25 */
    private static int idFromUrl(String url) {
        String[] parts = url.split("/");
        return Integer.parseInt(parts[parts.length - 1]);
    }

    private Map<String, Object> getJson(String url) throws IOException, InterruptedException {
        return Json.asObject(Json.parse(new String(download(url), StandardCharsets.UTF_8)));
    }

    /** Télécharge une URL (JSON ou image), en réessayant en cas d'erreur réseau ou de serveur surchargé. */
    byte[] download(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Accept-Encoding", "gzip") // réponses ~30 fois plus légères
                .header("User-Agent", "pokedex-java-cours")
                .GET()
                .build();

        for (int attempt = 1; ; attempt++) {
            HttpResponse<byte[]> response;
            try {
                response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
            } catch (IOException e) {
                if (attempt == MAX_ATTEMPTS) {
                    throw e;
                }
                Thread.sleep(1000L * attempt);
                continue;
            }

            int status = response.statusCode();
            if (status == 200) {
                return decodedBody(response);
            }
            boolean retryable = status == 429 || status >= 500;
            if (!retryable || attempt == MAX_ATTEMPTS) {
                throw new IOException("Erreur HTTP " + status + " sur " + url);
            }
            Thread.sleep(1000L * attempt);
        }
    }

    private static byte[] decodedBody(HttpResponse<byte[]> response) throws IOException {
        byte[] body = response.body();
        boolean gzip = response.headers().firstValue("Content-Encoding")
                .map(encoding -> encoding.equalsIgnoreCase("gzip"))
                .orElse(false);
        if (gzip) {
            try (InputStream in = new GZIPInputStream(new ByteArrayInputStream(body))) {
                body = in.readAllBytes();
            }
        }
        return body;
    }
}
