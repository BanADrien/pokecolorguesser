package fr.sdv.pokedex;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

// Les Pokémon proposés sous le champ de réponse du quiz : ceux dont le nom français ou anglais
// commence par le texte tapé, sans tenir compte des majuscules, des accents ni de la ponctuation.
public class Suggestions {

    private static final String AVEC_ACCENTS = "àáâãäåçèéêëìíîïñòóôõöùúûüýÿ";
    private static final String SANS_ACCENTS = "aaaaaaceeeeiiiinooooouuuuyy";

    private ArrayList<Pokemon> pokemons; // dans l'ordre alphabétique français

    public Suggestions(ArrayList<Pokemon> pokemonsDuQuiz) {
        pokemons = new ArrayList<>(pokemonsDuQuiz);
        // le Collator range les noms comme un dictionnaire français : « Chétiflor » avant « Chrysacier »
        Collator collator = Collator.getInstance(Locale.FRENCH);
        Collections.sort(pokemons, new Comparator<Pokemon>() {
            @Override
            public int compare(Pokemon a, Pokemon b) {
                int resultat = collator.compare(a.getNom(), b.getNom());
                if (resultat == 0) {
                    resultat = a.getId() - b.getId();
                }
                return resultat;
            }
        });
    }

    // D'abord les Pokémon trouvés par leur nom français, puis ceux trouvés seulement par leur nom anglais.
    // Les Pokémon de la liste exclus (déjà proposés à tort) ne sont pas repris.
    public ArrayList<Pokemon> chercher(String texte, ArrayList<Pokemon> exclus) {
        ArrayList<Pokemon> resultat = new ArrayList<>();
        ArrayList<Pokemon> parNomAnglais = new ArrayList<>();
        if (normaliser(texte).isEmpty()) {
            return resultat;
        }
        for (Pokemon pokemon : pokemons) {
            if (!exclus.contains(pokemon)) {
                if (commencePar(pokemon.getNom(), texte)) {
                    resultat.add(pokemon);
                } else if (commencePar(pokemon.getNomAnglais(), texte)) {
                    parNomAnglais.add(pokemon);
                }
            }
        }
        resultat.addAll(parNomAnglais);
        return resultat;
    }

    // "Dracaufeu" commence par "dràc"
    public static boolean commencePar(String nom, String texte) {
        return normaliser(nom).startsWith(normaliser(texte));
    }

    // "  Électrik " donne "electrik", "M. Mime" donne "mmime"
    public static String normaliser(String texte) {
        String minuscules = texte.toLowerCase();
        String resultat = "";
        for (int i = 0; i < minuscules.length(); i++) {
            char lettre = minuscules.charAt(i);
            // on enlève les accents
            int position = AVEC_ACCENTS.indexOf(lettre);
            if (position >= 0) {
                lettre = SANS_ACCENTS.charAt(position);
            }
            // on ne garde que les lettres et les chiffres
            if ((lettre >= 'a' && lettre <= 'z') || (lettre >= '0' && lettre <= '9')) {
                resultat = resultat + lettre;
            }
        }
        return resultat;
    }
}
