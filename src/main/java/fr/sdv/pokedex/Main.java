package fr.sdv.pokedex;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {

    public static void main(String[] args) {
        // les fenêtres Swing doivent être créées dans le thread de Swing
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    // l'apparence de Windows plutôt que celle de Java
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception e) {
                    System.err.println("Apparence du système indisponible : " + e.getMessage());
                }
                FenetrePokedex fenetre = new FenetrePokedex();
                fenetre.setVisible(true);
                fenetre.chargerPokemons();
            }
        });
    }
}
