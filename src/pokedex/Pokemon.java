package pokedex;

import java.util.List;

/** Un Pokémon tel qu'affiché dans le tableau. */
public record Pokemon(
        int id,
        String nameFr,
        String nameEn,
        int generation,
        List<String> types,
        double heightM,
        double weightKg,
        int hp,
        int attack,
        int defense,
        int specialAttack,
        int specialDefense,
        int speed,
        String spriteUrl) {

    public int total() {
        return hp + attack + defense + specialAttack + specialDefense + speed;
    }
}
