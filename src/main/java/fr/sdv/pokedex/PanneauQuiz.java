package fr.sdv.pokedex;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

// L'onglet Quiz : retrouver un Pokémon tiré au hasard à partir de ses bandes de couleurs
public class PanneauQuiz extends JPanel {

    private static final Color VERT = new Color(0, 130, 40);
    private static final Color ORANGE = new Color(205, 95, 0);
    private static final Color ROUGE = new Color(200, 30, 30);
    private static final Color GRIS = new Color(110, 110, 110);
    private static final int LARGEUR_REPONSE = 300;  // largeur de la colonne de réponse, en pixels
    private static final int LIGNES_VISIBLES = 8;     // lignes de la liste visibles sans faire défiler
    private static final int LONGUEUR_MAX = 14;       // au-delà, le texte tapé est coupé dans le message
    private static final String AIDE = "Tapez les premières lettres du nom…";

    private Parametres parametres;
    private CacheImages images;

    private JLabel labelProgression = new JLabel("Quiz des couleurs");
    private JButton boutonNouvellePartie = new JButton("Nouvelle partie");
    private PanneauBandes dessin = new PanneauBandes();
    private JLabel labelIndice = new JLabel(" ");
    private JLabel labelResultat = new JLabel(" ");
    private JLabel labelVies = new JLabel("Vies :");
    private JTextField champNom = new JTextField();
    private JLabel labelEtat = new JLabel(" ");
    // la liste des suggestions est un tableau d'une seule colonne, qu'on ne peut pas modifier
    private DefaultTableModel modeleSuggestions = new DefaultTableModel(0, 1) {
        @Override
        public boolean isCellEditable(int ligne, int colonne) {
            return false;
        }
    };
    private JTable tableSuggestions = new JTable(modeleSuggestions);
    private JButton boutonPasser = new JButton("Passer");
    private JButton boutonSuivant = new JButton("Commencer");

    private ArrayList<Pokemon> pokemons = new ArrayList<>();
    private PartieQuiz partie;              // null tant qu'aucune partie n'a commencé
    private Parametres parametresPartie;    // les réglages copiés au début de la partie
    private Suggestions suggestions = new Suggestions(new ArrayList<>());
    private BufferedImage imageQuestion;    // gardée pour calculer le nouvel indice après une erreur
    private boolean enChargement;          // une question est en train de charger
    private boolean partieTerminee;         // l'écran de fin est affiché
    private int numeroChargement;           // pour ignorer un chargement devenu inutile

    public PanneauQuiz(Parametres parametres, CacheImages images) {
        super(new BorderLayout());
        this.parametres = parametres;
        this.images = images;

        dessin.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        JPanel bas = creerBas();
        // à gauche les bandes et le résultat, à droite la colonne de réponse
        JPanel plateau = new JPanel(new BorderLayout());
        plateau.add(dessin, BorderLayout.CENTER);
        plateau.add(bas, BorderLayout.SOUTH);

        add(creerEnTete(), BorderLayout.NORTH);
        add(plateau, BorderLayout.CENTER);
        add(creerColonneReponse(bas.getPreferredSize().height), BorderLayout.EAST);

        // tant que les Pokémon ne sont pas chargés, on ne peut pas jouer
        dessin.setMessage("Chargement des Pokémon…");
        mettreAJourVies();
        activerReponse(false);
        boutonNouvellePartie.setEnabled(false);
        boutonSuivant.setEnabled(false);
    }

    private JPanel creerEnTete() {
        Font police = labelProgression.getFont();
        labelProgression.setFont(police.deriveFont(Font.BOLD, police.getSize2D() + 2));
        boutonNouvellePartie.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                commencerPartie();
            }
        });
        JPanel enTete = new JPanel(new BorderLayout(12, 0));
        enTete.setBorder(BorderFactory.createEmptyBorder(10, 16, 0, 16));
        enTete.add(labelProgression, BorderLayout.CENTER);
        enTete.add(boutonNouvellePartie, BorderLayout.EAST);
        return enTete;
    }

    // sous les cases : l'indice, le message et le bouton « Question suivante »
    private JPanel creerBas() {
        Font police = labelResultat.getFont();
        labelResultat.setFont(police.deriveFont(Font.BOLD, police.getSize2D() + 2));

        boutonSuivant.setPreferredSize(new Dimension(180, boutonSuivant.getPreferredSize().height + 6));
        boutonSuivant.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                suivant();
            }
        });
        // après une réponse, le bouton a le focus : Entrée passe à la suite
        boutonSuivant.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && boutonSuivant.isEnabled()) {
                    boutonSuivant.doClick();
                }
            }
        });

        labelIndice.setAlignmentX(CENTER_ALIGNMENT);
        labelResultat.setAlignmentX(CENTER_ALIGNMENT);
        boutonSuivant.setAlignmentX(CENTER_ALIGNMENT);
        JPanel bas = new JPanel();
        bas.setLayout(new BoxLayout(bas, BoxLayout.Y_AXIS));
        bas.setBorder(BorderFactory.createEmptyBorder(0, 16, 16, 16));
        bas.add(labelIndice);
        bas.add(Box.createVerticalStrut(6));
        bas.add(labelResultat);
        bas.add(Box.createVerticalStrut(14));
        bas.add(boutonSuivant);
        return bas;
    }

    // les vies, le champ, la liste des Pokémon possibles et « Passer »
    private JPanel creerColonneReponse(int hauteurBas) {
        preparerChampNom();
        JScrollPane liste = creerListeSuggestions();
        labelEtat.setForeground(GRIS);
        labelVies.setHorizontalTextPosition(JLabel.LEADING); // le texte « Vies : », puis les cœurs
        labelVies.setIconTextGap(8);
        boutonPasser.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                passerQuestion();
            }
        });

        JPanel colonne = new JPanel(new GridBagLayout());
        // marge du bas = hauteur sous les cases : la colonne est centrée à la hauteur des cases
        colonne.setBorder(BorderFactory.createEmptyBorder(8, 0, 8 + hauteurBas, 16));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 12, 0);
        colonne.add(labelVies, c);
        c.insets = new Insets(0, 0, 4, 0);
        colonne.add(new JLabel("Nom du Pokémon (français ou anglais) :"), c);
        colonne.add(champNom, c);
        colonne.add(labelEtat, c);
        c.insets = new Insets(0, 0, 0, 0);
        colonne.add(liste, c);
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        c.insets = new Insets(10, 0, 0, 0);
        colonne.add(boutonPasser, c);
        return colonne;
    }

    private void preparerChampNom() {
        // à chaque lettre tapée ou effacée, la liste est recalculée
        champNom.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                mettreAJourSuggestions();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                mettreAJourSuggestions();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                // changement de style du texte : rien à faire
            }
        });
        // Entrée dans le champ : on répond avec le Pokémon choisi dans la liste
        champNom.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                repondreAvecSelection();
            }
        });
        // flèches haut et bas : on change de ligne dans la liste
        champNom.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    deplacerSelection(1);
                } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                    deplacerSelection(-1);
                }
            }
        });
        champNom.setToolTipText("Flèches haut et bas pour choisir, Entrée pour répondre");
    }

    private JScrollPane creerListeSuggestions() {
        tableSuggestions.setTableHeader(null);
        tableSuggestions.setShowGrid(false);
        tableSuggestions.setIntercellSpacing(new Dimension(0, 0));
        tableSuggestions.getColumnModel().getColumn(0).setCellRenderer(new RenduSuggestion());
        tableSuggestions.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableSuggestions.setFocusable(false); // le clavier reste dans le champ de saisie
        tableSuggestions.setFillsViewportHeight(true);
        int hauteurLigne = tableSuggestions.getFontMetrics(tableSuggestions.getFont()).getHeight() + 4;
        tableSuggestions.setRowHeight(hauteurLigne);
        // un clic sur une ligne : on répond avec ce Pokémon
        tableSuggestions.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int ligne = tableSuggestions.rowAtPoint(e.getPoint()); // -1 en dessous des lignes
                if (ligne >= 0 && SwingUtilities.isLeftMouseButton(e)) {
                    repondreAvec(ligne);
                }
            }
        });

        JScrollPane defilement = new JScrollPane(tableSuggestions,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        Insets bords = defilement.getInsets();
        defilement.setPreferredSize(new Dimension(LARGEUR_REPONSE,
                LIGNES_VISIBLES * hauteurLigne + bords.top + bords.bottom));
        return defilement;
    }

    // appelée à la fin du chargement : le quiz devient jouable
    public void setPokemons(ArrayList<Pokemon> pokemons) {
        this.pokemons = pokemons;
        if (pokemons.isEmpty()) {
            dessin.setMessage("Aucun Pokémon disponible : impossible de lancer le quiz.");
            return;
        }
        dessin.setMessage("Retrouvez le Pokémon caché derrière ses bandes de couleurs !\n"
                + "Chaque bande donne sa couleur la plus fréquente et la taille du Pokémon à cet endroit.\n"
                + "3 vies par Pokémon : d'abord des colonnes, puis des lignes, puis sa silhouette.\n"
                + "Cliquez sur « Commencer » pour lancer une partie.");
        boutonNouvellePartie.setEnabled(true);
        boutonSuivant.setEnabled(true);
    }

    private void commencerPartie() {
        // une copie : changer les paramètres pendant la partie ne la change pas
        parametresPartie = parametres.copier();
        ArrayList<Pokemon> pokemonsDuQuiz = parametresPartie.pokemonsDuQuiz(pokemons);
        if (pokemonsDuQuiz.isEmpty()) {
            dessin.setMessage("Aucun Pokémon dans les générations choisies.");
            return;
        }
        partie = new PartieQuiz(pokemonsDuQuiz, parametresPartie.getNombreQuestions());
        // la liste ne propose que les Pokémon de la partie
        suggestions = new Suggestions(pokemonsDuQuiz);
        partieTerminee = false;
        chargerQuestion();
    }

    // le bouton du bas : commencer, question suivante, voir le score ou rejouer
    private void suivant() {
        if (partie == null || partieTerminee) {
            commencerPartie();
        } else if (partie.isQuestionFinie() && partie.isDerniereQuestion()) {
            afficherFin();
        } else if (partie.isQuestionFinie()) {
            partie.questionSuivante();
            chargerQuestion();
        }
    }

    private void chargerQuestion() {
        numeroChargement++;
        int numero = numeroChargement;
        Pokemon pokemon = partie.getPokemon();

        enChargement = true;
        afficherProgression();
        mettreAJourVies();
        dessin.setMessage("Chargement de la question…");
        labelIndice.setText(" ");
        labelResultat.setText(" ");
        activerReponse(false);
        viderChamp();
        if (partie.isDerniereQuestion()) {
            boutonSuivant.setText("Voir le score");
        } else {
            boutonSuivant.setText("Question suivante");
        }
        boutonSuivant.setEnabled(false);

        // l'image doit peut-être être téléchargée : on la charge dans un autre thread
        new Thread() {
            @Override
            public void run() {
                BufferedImage image = images.chargerImage(pokemon);
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        afficherQuestion(numero, pokemon, image);
                    }
                });
            }
        }.start();
    }

    private void afficherQuestion(int numero, Pokemon pokemon, BufferedImage image) {
        if (numero != numeroChargement) {
            return; // une nouvelle partie a été lancée entre-temps
        }
        enChargement = false;
        if (image == null) {
            questionSansImage(pokemon);
            return;
        }
        imageQuestion = image;
        dessinerIndice();
        afficherTypes(pokemon);
        activerReponse(true);
        champNom.requestFocusInWindow();
    }

    private void questionSansImage(Pokemon pokemon) {
        partie.passer();
        dessin.setMessage("L'image de " + decrire(pokemon) + " est indisponible.\nLa question est passée.");
        labelResultat.setForeground(ROUGE);
        labelResultat.setText("Question passée faute d'image (vérifiez la connexion).");
        apresReponse();
    }

    // L'indice qui correspond aux vies restantes (colonnes, lignes ou silhouette), et la réponse cachée.
    // L'image est déjà chargée : le calcul est immédiat.
    private void dessinerIndice() {
        String indice = partie.getIndice();
        dessin.nouvellesCases(imageQuestion);
        if (indice.equals(PartieQuiz.INDICE_SILHOUETTE)) {
            dessin.ajouterImage(Bandes.silhouette(imageQuestion), "Indice · silhouette");
        } else {
            boolean vertical = indice.equals(PartieQuiz.INDICE_COLONNES);
            Bandes bandes = new Bandes(imageQuestion, parametresPartie.getSections(),
                    parametresPartie.isIgnorerContoursNoirs(), vertical);
            dessin.ajouterBandes(bandes, "Indice · " + PanneauBandes.decrire(bandes));
        }
        dessin.ajouterImage(null, "Réponse"); // un « ? » jusqu'à la fin de la question
    }

    private void afficherTypes(Pokemon pokemon) {
        if (!parametresPartie.isIndiceTypes()) {
            labelIndice.setText(" "); // garde la hauteur de la ligne, pour que rien ne bouge
        } else if (pokemon.getTypes().size() > 1) {
            labelIndice.setText("Indice : types " + pokemon.getTypesTexte());
        } else {
            labelIndice.setText("Indice : type " + pokemon.getTypesTexte());
        }
    }

    private void activerReponse(boolean actif) {
        champNom.setEnabled(actif);
        tableSuggestions.setEnabled(actif);
        boutonPasser.setEnabled(actif);
        mettreAJourEtat();
    }

    // vide le champ, et donc aussi la liste des suggestions
    private void viderChamp() {
        champNom.setText("");
        // si le champ était déjà vide, setText ne prévient pas le DocumentListener
        mettreAJourSuggestions();
    }

    // Les Pokémon dont le nom commence par le texte tapé, sauf ceux déjà proposés à tort.
    // Le premier est choisi d'office.
    private void mettreAJourSuggestions() {
        ArrayList<Pokemon> exclus = new ArrayList<>();
        if (partie != null) {
            exclus = partie.getMauvaisesReponses();
        }
        ArrayList<Pokemon> trouves = suggestions.chercher(champNom.getText(), exclus);
        modeleSuggestions.setRowCount(0);
        for (Pokemon pokemon : trouves) {
            modeleSuggestions.addRow(new Object[] {pokemon});
        }
        if (!trouves.isEmpty()) {
            choisirLigne(0);
        }
        mettreAJourEtat();
    }

    // la petite ligne grise sous le champ : l'aide, le nombre de Pokémon possibles ou aucun résultat
    private void mettreAJourEtat() {
        String texte = champNom.getText().trim();
        int nombre = modeleSuggestions.getRowCount();
        if (Suggestions.normaliser(texte).isEmpty()) {
            if (champNom.isEnabled()) {
                labelEtat.setText(AIDE);
            } else {
                labelEtat.setText(" ");
            }
        } else if (nombre == 0) {
            if (texte.length() > LONGUEUR_MAX) {
                texte = texte.substring(0, LONGUEUR_MAX) + "…";
            }
            labelEtat.setText("Aucun Pokémon ne commence par « " + texte + " »");
        } else if (nombre == 1) {
            labelEtat.setText("1 Pokémon possible");
        } else {
            labelEtat.setText(nombre + " Pokémon possibles");
        }
    }

    private void choisirLigne(int ligne) {
        tableSuggestions.setRowSelectionInterval(ligne, ligne);
        tableSuggestions.scrollRectToVisible(tableSuggestions.getCellRect(ligne, 0, true));
    }

    // flèches haut et bas : la ligne choisie monte ou descend, sans sortir de la liste
    private void deplacerSelection(int decalage) {
        int nombre = modeleSuggestions.getRowCount();
        if (!peutRepondre() || nombre == 0) {
            return;
        }
        int ligne = tableSuggestions.getSelectedRow() + decalage;
        if (ligne < 0) {
            ligne = 0;
        }
        if (ligne > nombre - 1) {
            ligne = nombre - 1;
        }
        choisirLigne(ligne);
    }

    private boolean peutRepondre() {
        return partie != null && !partieTerminee && !enChargement && !partie.isQuestionFinie();
    }

    // Entrée dans le champ : on répond avec la ligne choisie (liste vide : rien ne se passe)
    private void repondreAvecSelection() {
        int ligne = tableSuggestions.getSelectedRow();
        if (ligne >= 0) {
            repondreAvec(ligne);
        }
    }

    private void repondreAvec(int ligne) {
        if (!peutRepondre()) {
            return;
        }
        Pokemon choisi = (Pokemon) modeleSuggestions.getValueAt(ligne, 0);
        // la ligne choisie reste marquée après la réponse
        tableSuggestions.setRowSelectionInterval(ligne, ligne);
        if (partie.repondre(choisi)) {
            afficherBravo();
        } else if (partie.isQuestionFinie()) {
            afficherRate("Raté… ");
        } else {
            afficherErreur(choisi);
        }
    }

    private void passerQuestion() {
        if (peutRepondre()) {
            partie.passer();
            afficherRate("Passé… ");
        }
    }

    // une vie perdue, mais il en reste : l'indice change (des lignes, puis la silhouette) et on peut réessayer
    private void afficherErreur(Pokemon mauvais) {
        dessinerIndice();
        mettreAJourVies();

        String message = "Ce n'est pas " + mauvais.getNom() + "… ";
        if (partie.getIndice().equals(PartieQuiz.INDICE_LIGNES)) {
            message = message + "Plus que " + partie.getVies() + " vies : voici l'image en lignes.";
        } else {
            message = message + "Plus qu'une vie : voici la silhouette.";
        }
        labelResultat.setForeground(ORANGE);
        labelResultat.setText(message);

        // le Pokémon proposé à tort disparaît aussi des suggestions
        viderChamp();
        champNom.requestFocusInWindow();
    }

    private void afficherBravo() {
        String message = "Bravo ! C'est " + decrire(partie.getPokemon());
        if (partie.getEssai() == 1) {
            message = message + ", du premier coup !";
        } else {
            message = message + ", trouvé au " + partie.getEssai() + "e essai.";
        }
        labelResultat.setForeground(VERT);
        labelResultat.setText(message);
        terminerQuestion();
    }

    private void afficherRate(String debut) {
        labelResultat.setForeground(ROUGE);
        labelResultat.setText(debut + "C'était " + decrire(partie.getPokemon()));
        terminerQuestion();
    }

    private void terminerQuestion() {
        dessin.montrerImage();
        mettreAJourVies();
        activerReponse(false);
        apresReponse();
    }

    private void apresReponse() {
        afficherProgression();
        boutonSuivant.setEnabled(true);
        boutonSuivant.requestFocusInWindow(); // pour qu'Entrée passe à la suite
    }

    // les cœurs au-dessus du champ : les vies qui restent pour la question en cours
    private void mettreAJourVies() {
        int vies = PartieQuiz.VIES;
        if (partie != null && !partieTerminee) {
            vies = partie.getVies();
        }
        labelVies.setIcon(new IconeVies(vies, PartieQuiz.VIES));
        String texte = vies + " vie";
        if (vies > 1) {
            texte = vies + " vies";
        }
        labelVies.setToolTipText(texte + " sur " + PartieQuiz.VIES
                + " pour ce Pokémon. À chaque erreur, l'indice change : des lignes, puis la silhouette.");
    }

    private void afficherFin() {
        partieTerminee = true;
        int score = partie.getScore();
        int total = partie.getNombreQuestions();
        labelProgression.setText("Partie terminée · Score : " + score + " / " + total);
        dessin.setMessage("Partie terminée !\nScore final : " + score + " / " + total + "\n"
                + commentaire(score, total));
        labelIndice.setText(" ");
        labelResultat.setText(" ");
        viderChamp();
        mettreAJourVies();
        boutonSuivant.setText("Rejouer");
        boutonSuivant.setEnabled(true);
        boutonSuivant.requestFocusInWindow();
    }

    private void afficherProgression() {
        labelProgression.setText("Question " + partie.getNumeroQuestion() + " / " + partie.getNombreQuestions()
                + " · Score : " + partie.getScore());
    }

    // "Dracaufeu (Charizard, n°6)", ou "Pikachu (n°25)" quand les deux noms sont pareils
    private String decrire(Pokemon pokemon) {
        if (pokemon.getNom().equalsIgnoreCase(pokemon.getNomAnglais())) {
            return pokemon.getNom() + " (n°" + pokemon.getId() + ")";
        }
        return pokemon.getNom() + " (" + pokemon.getNomAnglais() + ", n°" + pokemon.getId() + ")";
    }

    private String commentaire(int score, int total) {
        double proportion = (double) score / total;
        if (score == total) {
            return "Parfait, un vrai maître Pokémon !";
        } else if (proportion >= 0.7) {
            return "Très bien joué !";
        } else if (proportion >= 0.4) {
            return "Pas mal, encore un peu d'entraînement !";
        }
        return "Les couleurs sont trompeuses… Retentez votre chance !";
    }
}
