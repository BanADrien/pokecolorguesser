package pokedex;

import javax.swing.Icon;
import javax.swing.table.AbstractTableModel;
import java.util.List;

/** Fait le lien entre la liste de Pokémon et le JTable : une ligne par Pokémon. */
public class PokemonTableModel extends AbstractTableModel {

    public static final int IMAGE_COLUMN = 1;

    private static final String[] COLUMNS = {
            "N°", "Image", "Nom", "Nom anglais", "Gén.", "Type(s)", "Taille (m)", "Poids (kg)",
            "PV", "Attaque", "Défense", "Atq. Spé.", "Déf. Spé.", "Vitesse", "Total"
    };

    private final SpriteCache sprites;
    private List<Pokemon> pokemons = List.of();

    public PokemonTableModel(SpriteCache sprites) {
        this.sprites = sprites;
    }

    public void setPokemons(List<Pokemon> pokemons) {
        this.pokemons = List.copyOf(pokemons);
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return pokemons.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    /** Le type de chaque colonne permet un tri numérique (2 < 10) et un affichage adapté (image, nombre…). */
    @Override
    public Class<?> getColumnClass(int column) {
        return switch (column) {
            case IMAGE_COLUMN -> Icon.class;
            case 2, 3, 5 -> String.class;
            case 6, 7 -> Double.class;
            default -> Integer.class;
        };
    }

    @Override
    public Object getValueAt(int row, int column) {
        Pokemon p = pokemons.get(row);
        return switch (column) {
            case 0 -> p.id();
            case IMAGE_COLUMN -> sprites.get(p); // null tant que l'image n'est pas chargée
            case 2 -> p.nameFr();
            case 3 -> p.nameEn();
            case 4 -> p.generation();
            case 5 -> String.join(" / ", p.types());
            case 6 -> p.heightM();
            case 7 -> p.weightKg();
            case 8 -> p.hp();
            case 9 -> p.attack();
            case 10 -> p.defense();
            case 11 -> p.specialAttack();
            case 12 -> p.specialDefense();
            case 13 -> p.speed();
            case 14 -> p.total();
            default -> throw new IllegalArgumentException("Colonne inconnue : " + column);
        };
    }
}
