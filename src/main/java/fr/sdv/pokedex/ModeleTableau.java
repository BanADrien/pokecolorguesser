package fr.sdv.pokedex;

import javax.swing.Icon;
import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;

// Le contenu du tableau du Pokédex : une ligne par Pokémon
public class ModeleTableau extends AbstractTableModel {

    public static final int COLONNE_IMAGE = 1;

    private static final String[] COLONNES = {
            "N°", "Image", "Nom", "Nom anglais", "Gén.", "Type(s)", "Taille (m)", "Poids (kg)",
            "PV", "Attaque", "Défense", "Atq. Spé.", "Déf. Spé.", "Vitesse", "Total"
    };

    private CacheImages images;
    private ArrayList<Pokemon> pokemons = new ArrayList<>();

    public ModeleTableau(CacheImages images) {
        this.images = images;
    }

    public void setPokemons(ArrayList<Pokemon> pokemons) {
        this.pokemons = pokemons;
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return pokemons.size();
    }

    @Override
    public int getColumnCount() {
        return COLONNES.length;
    }

    @Override
    public String getColumnName(int colonne) {
        return COLONNES[colonne];
    }

    // le type de chaque colonne : le tableau s'en sert pour trier (2 avant 10) et pour afficher les images
    @Override
    public Class getColumnClass(int colonne) {
        if (colonne == COLONNE_IMAGE) {
            return Icon.class;
        }
        if (colonne == 2 || colonne == 3 || colonne == 5) {
            return String.class;
        }
        if (colonne == 6 || colonne == 7) {
            return Double.class;
        }
        return Integer.class;
    }

    @Override
    public Object getValueAt(int ligne, int colonne) {
        Pokemon pokemon = pokemons.get(ligne);
        switch (colonne) {
            case 0:
                return pokemon.getId();
            case COLONNE_IMAGE:
                return images.getIcone(pokemon);
            case 2:
                return pokemon.getNom();
            case 3:
                return pokemon.getNomAnglais();
            case 4:
                return pokemon.getGeneration();
            case 5:
                return pokemon.getTypesTexte();
            case 6:
                return pokemon.getTaille();
            case 7:
                return pokemon.getPoids();
            case 8:
                return pokemon.getPv();
            case 9:
                return pokemon.getAttaque();
            case 10:
                return pokemon.getDefense();
            case 11:
                return pokemon.getAttaqueSpeciale();
            case 12:
                return pokemon.getDefenseSpeciale();
            case 13:
                return pokemon.getVitesse();
            default:
                return pokemon.getTotal();
        }
    }
}
