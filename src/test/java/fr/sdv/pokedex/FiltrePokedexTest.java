package fr.sdv.pokedex;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FiltrePokedexTest {

    // types séparés par "/", comme dans le fichier cache/pokedex.csv
    private Pokemon creerPokemon(int id, String nom, String nomAnglais, int generation, String types) {
        Pokemon pokemon = new Pokemon(id, nom, nomAnglais, generation);
        for (String type : types.split("/")) {
            pokemon.ajouterType(type);
        }
        return pokemon;
    }

    private ArrayList<Pokemon> creerPokemons() {
        ArrayList<Pokemon> pokemons = new ArrayList<>();
        pokemons.add(creerPokemon(4, "Salamèche", "Charmander", 1, "Feu"));
        pokemons.add(creerPokemon(6, "Dracaufeu", "Charizard", 1, "Feu/Vol"));
        pokemons.add(creerPokemon(25, "Pikachu", "Pikachu", 1, "Électrik"));
        pokemons.add(creerPokemon(26, "Raichu", "Raichu", 1, "Électrik"));
        pokemons.add(creerPokemon(147, "Minidraco", "Dratini", 1, "Dragon"));
        pokemons.add(creerPokemon(155, "Héricendre", "Cyndaquil", 2, "Feu"));
        pokemons.add(creerPokemon(172, "Pichu", "Pichu", 2, "Électrik"));
        pokemons.add(creerPokemon(390, "Ouisticram", "Chimchar", 4, "Feu"));
        pokemons.add(creerPokemon(443, "Griknot", "Gible", 4, "Dragon/Sol"));
        pokemons.add(creerPokemon(444, "Carmache", "Gabite", 4, "Dragon/Sol"));
        pokemons.add(creerPokemon(445, "Carchacrok", "Garchomp", 4, "Dragon/Sol"));
        pokemons.add(creerPokemon(484, "Palkia", "Palkia", 4, "Eau/Dragon"));
        return pokemons;
    }

    // les noms français des Pokémon gardés par le filtre, séparés par des espaces
    private String nomsGardes(FiltrePokedex filtre) {
        String texte = "";
        for (Pokemon pokemon : creerPokemons()) {
            if (filtre.accepte(pokemon)) {
                if (!texte.isEmpty()) {
                    texte = texte + " ";
                }
                texte = texte + pokemon.getNom();
            }
        }
        return texte;
    }

    private String rechercher(String texte) {
        FiltrePokedex filtre = new FiltrePokedex();
        filtre.setRecherche(texte);
        return nomsGardes(filtre);
    }

    @Test
    void rechercheDansLeNomFrancais() {
        assertEquals("Salamèche", rechercher("sala"));
        // le texte peut être au milieu du nom
        assertEquals("Dracaufeu", rechercher("caufeu"));
        assertEquals("Pikachu Raichu Pichu", rechercher("chu"));
    }

    @Test
    void rechercheDansLeNomAnglais() {
        // Charmander, Charizard et Chimchar
        assertEquals("Salamèche Dracaufeu Ouisticram", rechercher("char"));
        assertEquals("Griknot", rechercher("gible"));
    }

    @Test
    void rechercheSansTenirCompteDesAccentsNiDesMajuscules() {
        assertEquals("Salamèche", rechercher("SALAMECHE"));
        assertEquals("Héricendre", rechercher("hericendre"));
        assertEquals("Héricendre", rechercher("HÉRI"));
        assertEquals("Pikachu", rechercher("  pIkA "));
    }

    @Test
    void rechercheParNumero() {
        assertEquals("Pikachu", rechercher("25"));
        assertEquals("Dracaufeu", rechercher(" 6 "));
        assertEquals("", rechercher("999"));
    }

    @Test
    void filtreParGeneration() {
        FiltrePokedex filtre = new FiltrePokedex();
        assertEquals(12, creerPokemons().size());
        assertEquals("Salamèche Dracaufeu Pikachu Raichu Minidraco Héricendre Pichu Ouisticram Griknot Carmache "
                + "Carchacrok Palkia", nomsGardes(filtre));

        filtre.setGeneration(2);
        assertEquals("Héricendre Pichu", nomsGardes(filtre));
    }

    @Test
    void filtreParType() {
        FiltrePokedex filtre = new FiltrePokedex();
        filtre.setType("Dragon");
        assertEquals("Minidraco Griknot Carmache Carchacrok Palkia", nomsGardes(filtre));

        // le deuxième type compte aussi
        filtre.setType("Vol");
        assertEquals("Dracaufeu", nomsGardes(filtre));
    }

    @Test
    void generationTypeEtRechercheEnsemble() {
        FiltrePokedex filtre = new FiltrePokedex();
        filtre.setGeneration(1);
        filtre.setType("Feu");
        assertEquals("Salamèche Dracaufeu", nomsGardes(filtre));

        filtre.setGeneration(4);
        filtre.setType("Dragon");
        filtre.setRecherche("ca");
        assertEquals("Carmache Carchacrok", nomsGardes(filtre));
    }

    @Test
    void aucunPokemonNeCorrespond() {
        assertEquals("", rechercher("xyz"));

        FiltrePokedex filtre = new FiltrePokedex();
        filtre.setGeneration(2);
        filtre.setType("Dragon");
        assertEquals("", nomsGardes(filtre));

        assertEquals("Aucun Pokémon ne correspond", FiltrePokedex.texteCompteur(0));
        assertEquals("1 Pokémon affiché", FiltrePokedex.texteCompteur(1));
        assertEquals("12 Pokémon affichés", FiltrePokedex.texteCompteur(12));
    }

    @Test
    void leTableauNAfficheQueLesPokemonsQuiPassentLeFiltre() {
        ModeleTableau modele = new ModeleTableau(new CacheImages(Path.of("sprites")));
        modele.setPokemons(creerPokemons());
        assertEquals(12, modele.getRowCount());

        FiltrePokedex filtre = new FiltrePokedex();
        filtre.setRecherche("25");
        modele.setFiltre(filtre);
        assertEquals(1, modele.getRowCount());
        assertEquals("Pikachu", modele.getValueAt(0, 2));
        assertEquals("Électrik", modele.getValueAt(0, 5));

        // la liste complète est gardée : on retrouve tout quand on vide la recherche
        filtre.setRecherche("");
        modele.setFiltre(filtre);
        assertEquals(12, modele.getRowCount());
    }

    @Test
    void les18TypesSontDansLOrdreAlphabetique() {
        assertEquals(18, FiltrePokedex.TYPES.length);
        // le Collator range les mots comme un dictionnaire français : « Électrik » avec les E
        Collator collator = Collator.getInstance(Locale.FRENCH);
        for (int i = 1; i < FiltrePokedex.TYPES.length; i++) {
            assertTrue(collator.compare(FiltrePokedex.TYPES[i - 1], FiltrePokedex.TYPES[i]) < 0);
        }
    }
}
