package fr.sdv.pokedex;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    /** Lit le JSON (bibliothèque Jackson). Un seul ObjectMapper suffit : il peut servir à plusieurs threads. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

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
            for (int speciesId : speciesIds(getJson(BASE_URL + "generation/" + gen + "/"))) {
                generationBySpeciesId.put(speciesId, gen);
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
        JsonNode species = getJson(BASE_URL + "pokemon-species/" + speciesId + "/");
        JsonNode pokemon = getJson(defaultVarietyUrl(species));
        return toPokemon(species, pokemon, generation);
    }

    // Les méthodes suivantes lisent les réponses JSON sans faire d'appel réseau : on peut les tester
    // avec des fichiers d'exemple. path("clé") ne renvoie jamais null : si la clé manque,
    // on obtient un nœud « absent », qui donne 0 avec asInt() et "" avec asText().

    /** Les numéros des espèces apparues dans une génération (réponse de /generation/{n}/). */
    static List<Integer> speciesIds(JsonNode generation) {
        List<Integer> ids = new ArrayList<>();
        for (JsonNode species : generation.path("pokemon_species")) {
            ids.add(idFromUrl(species.path("url").asText()));
        }
        return ids;
    }

    /**
     * Assemble un Pokémon à partir de son espèce (réponse de /pokemon-species/{id}/ : numéro et noms traduits)
     * et de sa forme par défaut (réponse de /pokemon/{id}/ : types, taille, poids, statistiques et image).
     */
    static Pokemon toPokemon(JsonNode species, JsonNode pokemon, int generation) {
        List<String> types = new ArrayList<>();
        for (JsonNode type : pokemon.path("types")) {
            String name = type.path("type").path("name").asText();
            types.add(TYPES_FR.getOrDefault(name, name));
        }

        Map<String, Integer> stats = new HashMap<>();
        for (JsonNode stat : pokemon.path("stats")) {
            stats.put(stat.path("stat").path("name").asText(), stat.path("base_stat").asInt());
        }

        // "front_default" vaut null quand le Pokémon n'a pas d'image
        JsonNode sprite = pokemon.path("sprites").path("front_default");

        return new Pokemon(
                species.path("id").asInt(),
                localizedName(species, "fr"),
                localizedName(species, "en"),
                generation,
                List.copyOf(types),
                pokemon.path("height").asInt() / 10.0, // l'API donne des décimètres
                pokemon.path("weight").asInt() / 10.0, // et des hectogrammes
                stats.getOrDefault("hp", 0),
                stats.getOrDefault("attack", 0),
                stats.getOrDefault("defense", 0),
                stats.getOrDefault("special-attack", 0),
                stats.getOrDefault("special-defense", 0),
                stats.getOrDefault("speed", 0),
                sprite.isTextual() ? sprite.asText() : "");
    }

    /** Le nom dans la langue demandée ("fr", "en"…), sinon le nom technique de l'espèce ("pikachu"). */
    private static String localizedName(JsonNode species, String language) {
        for (JsonNode name : species.path("names")) {
            if (language.equals(name.path("language").path("name").asText())) {
                return name.path("name").asText();
            }
        }
        return species.path("name").asText();
    }

    /** Une espèce peut avoir plusieurs formes (ex. méga-évolutions) : on garde la forme par défaut. */
    static String defaultVarietyUrl(JsonNode species) {
        for (JsonNode variety : species.path("varieties")) {
            if (variety.path("is_default").asBoolean()) {
                return variety.path("pokemon").path("url").asText();
            }
        }
        return BASE_URL + "pokemon/" + species.path("id").asInt() + "/";
    }

    /** "https://pokeapi.co/api/v2/pokemon-species/25/" -> 25 */
    private static int idFromUrl(String url) {
        String[] parts = url.split("/");
        return Integer.parseInt(parts[parts.length - 1]);
    }

    private JsonNode getJson(String url) throws IOException, InterruptedException {
        return MAPPER.readTree(download(url));
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
