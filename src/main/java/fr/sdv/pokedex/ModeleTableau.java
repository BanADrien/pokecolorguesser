package fr.sdv.pokedex;

import javax.swing.Icon;
import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;

// Le contenu du tableau du Pokédex : une ligne par Pokémon qui passe la recherche et les filtres
public class ModeleTableau extends AbstractTableModel {

    public static final int COLONNE_IMAGE = 1;

    private static final String[] COLONNES = {"N°", "Image", "Nom", "Nom anglais", "Gén.", "Type(s)"};

    private CacheImages images;
    private FiltrePokedex filtre = new FiltrePokedex();
    private ArrayList<Pokemon> tousLesPokemons = new ArrayList<>();
    private ArrayList<Pokemon> pokemonsAffiches = new ArrayList<>(); // ceux qui passent le filtre

    public ModeleTableau(CacheImages images) {
        this.images = images;
    }

    public void setPokemons(ArrayList<Pokemon> pokemons) {
        tousLesPokemons = pokemons;
        mettreAJourListe();
    }

    public void setFiltre(FiltrePokedex filtre) {
        this.filtre = filtre;
        mettreAJourListe();
    }

    // la liste des Pokémon affichés est refaite à chaque changement de la recherche ou des filtres
    private void mettreAJourListe() {
        pokemonsAffiches = new ArrayList<>();
        for (Pokemon pokemon : tousLesPokemons) {
            if (filtre.accepte(pokemon)) {
                pokemonsAffiches.add(pokemon);
            }
        }
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return pokemonsAffiches.size();
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
        if (colonne == 0 || colonne == 4) {
            return Integer.class;
        }
        return String.class;
    }

    @Override
    public Object getValueAt(int ligne, int colonne) {
        Pokemon pokemon = pokemonsAffiches.get(ligne);
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
            default:
                return pokemon.getTypesTexte();
        }
    }
}
