package fr.sdv.pokedex;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Les Pokémon proposés sous le champ de réponse du quiz : ceux dont le nom français ou anglais
 * commence par le texte tapé, sans tenir compte des majuscules, des accents ni de la ponctuation.
 */
public final class NameSuggestions {

    /** Un Pokémon et ses deux noms déjà normalisés, pour ne pas les recalculer à chaque lettre tapée. */
    private record Entry(Pokemon pokemon, String nameFr, String nameEn) {
    }

    private final List<Entry> entries;

    /** pool : les Pokémon de la partie, rangés ici une fois pour toutes dans l'ordre alphabétique français. */
    public NameSuggestions(List<Pokemon> pool) {
        // le Collator compare comme un dictionnaire français : « Chétiflor » est rangé avant « Chrysacier »
        Collator collator = Collator.getInstance(Locale.FRENCH);
        List<Pokemon> sorted = new ArrayList<>(pool);
        sorted.sort(Comparator.comparing(Pokemon::nameFr, collator).thenComparingInt(Pokemon::id));
        List<Entry> list = new ArrayList<>();
        for (Pokemon pokemon : sorted) {
            list.add(new Entry(pokemon, QuizGame.normalize(pokemon.nameFr()), QuizGame.normalize(pokemon.nameEn())));
        }
        entries = List.copyOf(list);
    }

    /**
     * Les Pokémon dont un nom commence par typed : d'abord ceux qui correspondent par leur nom français,
     * puis ceux qui ne correspondent que par leur nom anglais, chaque groupe dans l'ordre alphabétique français.
     * Liste vide si typed ne contient ni lettre ni chiffre.
     */
    public List<Pokemon> suggest(String typed) {
        return suggest(typed, List.of());
    }

    /** Comme suggest(typed), sans les Pokémon de excluded (ceux déjà proposés à tort pour la question). */
    public List<Pokemon> suggest(String typed, List<Pokemon> excluded) {
        String prefix = QuizGame.normalize(typed);
        if (prefix.isEmpty()) {
            return List.of();
        }
        Set<Integer> excludedIds = new HashSet<>();
        for (Pokemon pokemon : excluded) {
            excludedIds.add(pokemon.id());
        }
        List<Pokemon> result = new ArrayList<>();
        List<Pokemon> englishOnly = new ArrayList<>();
        for (Entry entry : entries) {
            if (excludedIds.contains(entry.pokemon().id())) {
                continue;
            }
            if (entry.nameFr().startsWith(prefix)) {
                result.add(entry.pokemon());
            } else if (entry.nameEn().startsWith(prefix)) {
                englishOnly.add(entry.pokemon());
            }
        }
        result.addAll(englishOnly);
        return List.copyOf(result);
    }
}
