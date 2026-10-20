package fr.sdv.pokedex;

import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

// Le chargement se fait dans ce thread à part, pour que la fenêtre reste réactive.
// La fenêtre n'est modifiée que dans le thread de Swing (SwingUtilities.invokeLater).
public class ChargementPokedex extends Thread {

    private FenetrePokedex fenetre;
    private CachePokedex cache;
    private CacheImages images;

    public ChargementPokedex(FenetrePokedex fenetre, CachePokedex cache, CacheImages images) {
        this.fenetre = fenetre;
        this.cache = cache;
        this.images = images;
    }

    @Override
    public void run() {
        ArrayList<Pokemon> pokemons = cache.lire();
        if (pokemons == null) {
            try {
                pokemons = telechargerTout();
                cache.ecrire(pokemons);
            } catch (Exception e) {
                signalerErreur(e);
                return;
            }
        }
        signalerFin(pokemons);
        chargerIcones(pokemons);
    }

    // la première fois : les Pokémon sont téléchargés depuis PokéAPI, un par un
    private ArrayList<Pokemon> telechargerTout() throws IOException {
        ApiPokemon api = new ApiPokemon();
        ArrayList<Integer> numeros = new ArrayList<>();
        HashMap<Integer, Integer> generations = new HashMap<>();
        for (int generation = 1; generation <= Pokemon.DERNIERE_GENERATION; generation++) {
            for (int numero : api.numerosDeLaGeneration(generation)) {
                numeros.add(numero);
                generations.put(numero, generation);
            }
        }
        Collections.sort(numeros);

        ArrayList<Pokemon> pokemons = new ArrayList<>();
        signalerProgression(0, numeros.size());
        for (int i = 0; i < numeros.size(); i++) {
            int numero = numeros.get(i);
            pokemons.add(api.telechargerPokemon(numero, generations.get(numero)));
            signalerProgression(i + 1, numeros.size());
        }
        return pokemons;
    }

    // les petites images du tableau, une par une (celles qui manquent sont téléchargées)
    private void chargerIcones(ArrayList<Pokemon> pokemons) {
        for (Pokemon pokemon : pokemons) {
            if (!pokemon.getAdresseImage().isEmpty()) {
                BufferedImage image = images.chargerImage(pokemon);
                if (image == null) {
                    // sans doute pas de connexion : on n'essaie pas les autres images
                    return;
                }
                signalerIcone(pokemon, CacheImages.creerIcone(image));
            }
        }
    }

    private void signalerProgression(int fait, int total) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                fenetre.afficherProgression(fait, total);
            }
        });
    }

    private void signalerFin(ArrayList<Pokemon> pokemons) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                fenetre.afficherPokemons(pokemons);
            }
        });
    }

    private void signalerErreur(Exception erreur) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                fenetre.afficherErreur(erreur);
            }
        });
    }

    private void signalerIcone(Pokemon pokemon, ImageIcon icone) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                images.ajouterIcone(pokemon, icone);
                fenetre.redessinerTableau();
            }
        });
    }
}
