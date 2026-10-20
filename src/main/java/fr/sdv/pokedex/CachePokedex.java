package fr.sdv.pokedex;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// Le fichier cache/pokedex.csv : la liste des Pokémon, pour ne pas tout re-télécharger à chaque lancement
public class CachePokedex {

    // si le format du fichier change, il faut changer cette ligne : l'ancien fichier sera alors ignoré
    private static final String EN_TETE =
            "id;nom;nom_anglais;generation;types;taille_m;poids_kg;pv;attaque;defense;atq_spe;def_spe;vitesse;image";

    private Path fichier;

    public CachePokedex(Path fichier) {
        this.fichier = fichier;
    }

    // renvoie null si le fichier n'existe pas encore ou s'il est illisible
    public ArrayList<Pokemon> lire() {
        if (!Files.exists(fichier)) {
            return null;
        }
        try {
            List<String> lignes = Files.readAllLines(fichier, StandardCharsets.UTF_8);
            if (lignes.isEmpty() || !lignes.get(0).equals(EN_TETE)) {
                System.err.println("Cache illisible, nouveau téléchargement : format de cache inattendu");
                return null;
            }
            ArrayList<Pokemon> pokemons = new ArrayList<>();
            for (int i = 1; i < lignes.size(); i++) {
                pokemons.add(lireLigne(lignes.get(i)));
            }
            return pokemons;
        } catch (Exception e) {
            System.err.println("Cache illisible, nouveau téléchargement : " + e.getMessage());
            return null;
        }
    }

    private Pokemon lireLigne(String ligne) {
        // -1 : on garde le dernier champ même s'il est vide (Pokémon sans image)
        String[] champs = ligne.split(";", -1);
        Pokemon pokemon = new Pokemon(Integer.parseInt(champs[0]), champs[1], champs[2],
                Integer.parseInt(champs[3]));
        for (String type : champs[4].split("/")) {
            pokemon.ajouterType(type);
        }
        pokemon.setTaille(Double.parseDouble(champs[5]));
        pokemon.setPoids(Double.parseDouble(champs[6]));
        pokemon.setPv(Integer.parseInt(champs[7]));
        pokemon.setAttaque(Integer.parseInt(champs[8]));
        pokemon.setDefense(Integer.parseInt(champs[9]));
        pokemon.setAttaqueSpeciale(Integer.parseInt(champs[10]));
        pokemon.setDefenseSpeciale(Integer.parseInt(champs[11]));
        pokemon.setVitesse(Integer.parseInt(champs[12]));
        pokemon.setAdresseImage(champs[13]);
        return pokemon;
    }

    public void ecrire(ArrayList<Pokemon> pokemons) throws IOException {
        ArrayList<String> lignes = new ArrayList<>();
        lignes.add(EN_TETE);
        for (Pokemon p : pokemons) {
            lignes.add(p.getId() + ";" + p.getNom() + ";" + p.getNomAnglais() + ";" + p.getGeneration() + ";"
                    + String.join("/", p.getTypes()) + ";" + p.getTaille() + ";" + p.getPoids() + ";"
                    + p.getPv() + ";" + p.getAttaque() + ";" + p.getDefense() + ";"
                    + p.getAttaqueSpeciale() + ";" + p.getDefenseSpeciale() + ";" + p.getVitesse() + ";"
                    + p.getAdresseImage());
        }
        Files.createDirectories(fichier.getParent());
        Files.write(fichier, lignes, StandardCharsets.UTF_8);
    }
}
