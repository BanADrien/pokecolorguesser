package fr.sdv.pokedex;

// La recherche et les filtres de l'onglet Pokédex, sans interface graphique :
// le texte cherché, la génération et le type choisis
public class FiltrePokedex {

    // les 18 types, dans l'ordre alphabétique français (« Électrik » se range avec les E)
    public static final String[] TYPES = {
            "Acier", "Combat", "Dragon", "Eau", "Électrik", "Fée", "Feu", "Glace", "Insecte",
            "Normal", "Plante", "Poison", "Psy", "Roche", "Sol", "Spectre", "Ténèbres", "Vol"
    };

    private String recherche = "";
    private int generation = 0; // 0 : toutes les générations
    private String type = "";   // vide : tous les types

    public String getRecherche() {
        return recherche;
    }

    public void setRecherche(String recherche) {
        this.recherche = recherche;
    }

    public int getGeneration() {
        return generation;
    }

    public void setGeneration(int generation) {
        this.generation = generation;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    // true si le Pokémon doit rester dans le tableau : il faut qu'il passe la recherche ET les deux filtres
    public boolean accepte(Pokemon pokemon) {
        if (generation != 0 && pokemon.getGeneration() != generation) {
            return false;
        }
        if (!type.isEmpty() && !pokemon.getTypes().contains(type)) {
            return false;
        }
        return correspondALaRecherche(pokemon);
    }

    // "25" trouve Pikachu par son numéro, "chu" le trouve par son nom (français ou anglais)
    private boolean correspondALaRecherche(Pokemon pokemon) {
        String texte = recherche.trim();
        if (estUnNombre(texte)) {
            return pokemon.getId() == Integer.parseInt(texte);
        }
        // sans accents ni majuscules : "electrik" trouve "Électrik"
        String cherche = Suggestions.normaliser(texte);
        return Suggestions.normaliser(pokemon.getNom()).contains(cherche)
                || Suggestions.normaliser(pokemon.getNomAnglais()).contains(cherche);
    }

    // "25" est un nombre, mais pas "" ni "25a"
    private boolean estUnNombre(String texte) {
        // au-delà de 9 chiffres, Integer.parseInt ne marche plus
        if (texte.isEmpty() || texte.length() > 9) {
            return false;
        }
        for (int i = 0; i < texte.length(); i++) {
            char chiffre = texte.charAt(i);
            if (chiffre < '0' || chiffre > '9') {
                return false;
            }
        }
        return true;
    }

    // le texte du compteur au-dessus du tableau
    public static String texteCompteur(int nombre) {
        if (nombre == 0) {
            return "Aucun Pokémon ne correspond";
        }
        if (nombre == 1) {
            return "1 Pokémon affiché";
        }
        return nombre + " Pokémon affichés";
    }
}
