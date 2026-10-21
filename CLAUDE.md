# Pokédex Java (cours SDV)

Application Java Swing : un Pokédex (tableau des Pokémon des générations 1 à 8 récupérés sur PokéAPI),
un quiz « deviner le Pokémon à partir de ses bandes de couleurs, puis de sa silhouette pixelisée », avec des points,
et une page de paramètres.
`README.md` : la documentation pour l'utilisateur (à garder à jour).

## Environnement

- Projet **Maven** standard : `pom.xml` (`fr.sdv:pokedex:1.0-SNAPSHOT`), paquet `fr.sdv.pokedex`.
  IntelliJ l'importe comme projet Maven (`MavenProjectsManager` dans `.idea/misc.xml`) : pas de `.iml` dans le projet.
- Dépendances : `jackson-databind` 2.x (JSON), JUnit Jupiter 6 (tests, version donnée par `junit-bom`).
  Versions des plugins fixées dans le `pluginManagement`. N'ajouter une dépendance que si elle est utile,
  avec une version fixe (dernière version stable sur Maven Central).
- Maven n'est pas installé : toujours passer par le **Maven Wrapper** (`mvnw.cmd`, Maven 3.10.0).
- JDK 27 : `C:\Users\adrie\.jdks\openjdk-27` (`maven.compiler.release` = 27).
  Le shell n'a pas forcément `JAVA_HOME` : le définir avant `mvnw.cmd`.
- Windows 11, l'utilisateur lance l'appli depuis IntelliJ ou en double-cliquant sur `lancer.cmd`.

## Commandes (PowerShell, depuis la racine du projet)

```powershell
$env:JAVA_HOME = "C:\Users\adrie\.jdks\openjdk-27"
.\mvnw.cmd -B clean verify                 # compilation, tests, jar exécutable
.\mvnw.cmd -B test                         # tests seulement
.\mvnw.cmd -B test -Dtest=PartieQuizTest   # une seule classe de test
```

- La construction doit rester **sans aucun avertissement** avec les réglages par défaut du compilateur
  (pas d'option `-Xlint` dans le `pom.xml`), et le `maven-shade-plugin` est configuré pour ne pas en produire.
- `target/` appartient à Maven (ignoré par git). Le jar final `target\pokedex-1.0-SNAPSHOT.jar` contient
  aussi Jackson (`maven-shade-plugin`, `Main-Class: fr.sdv.pokedex.Main`).
- Lancer : ▶ Run sur `fr.sdv.pokedex.Main` dans IntelliJ (dossier de travail = racine du projet),
  double-clic sur `lancer.cmd` (compile sans les tests puis lance `javaw -jar`), ou `java -jar target\pokedex-1.0-SNAPSHOT.jar`.
  Si le nom du jar change, le changer aussi dans `lancer.cmd`.
- Tests de l'interface : lancer le jar **depuis un dossier temporaire** (pour ne pas créer `cache/` ni
  `parametres.properties` dans le projet ; y copier un `cache/pokedex.csv` **au format actuel** et `cache/sprites/`
  évite de tout re-télécharger : un CSV à l'ancien en-tête se convertit en gardant les colonnes 1 à 5 et `image`),
  peindre le `JRootPane` dans un PNG avec `SwingUtilities.invokeAndWait`, puis regarder l'image.
  La fenêtre s'ouvre brièvement sur l'écran. Ne pas faire biper l'appli (champ désactivé, dernière génération décochée).

## Architecture (`src/main/java/fr/sdv/pokedex/`)

- `Main` : `main` crée la `FenetrePokedex` dans le thread de Swing (`SwingUtilities.invokeLater`) avec l'apparence Windows.
- `FenetrePokedex` (`extends JFrame`) : onglets Pokédex (`PanneauPokedex`) / Quiz / Paramètres, barre d'état et
  barre de progression. `chargerPokemons()` démarre un `ChargementPokedex`.
- `ChargementPokedex` (`extends Thread`) : lit `cache/pokedex.csv` (`CachePokedex`), sinon télécharge les Pokémon
  **un par un** avec `ApiPokemon` (progression envoyée à la fenêtre), puis charge une par une les images du tableau
  (`CacheImages`). La fenêtre n'est modifiée que via `SwingUtilities.invokeLater`.
- `ApiPokemon` : appels HTTP à PokéAPI (`HttpURLConnection`, gzip, 3 essais), JSON lu avec Jackson
  (`ObjectMapper.readTree`, `path(...)`, `asText()`, `asInt()`).
- `CachePokedex` : lecture et écriture de `cache/pokedex.csv`, en-tête `id;nom;nom_anglais;generation;types;image`
  (types séparés par `/`, image vide si le Pokémon n'en a pas). Changer l'en-tête (`EN_TETE`) quand le format change :
  l'ancien fichier est alors ignoré et tout est re-téléchargé une fois (les sprites de `cache/sprites/` restent valables).
  La taille, le poids et les statistiques ont été retirés de l'appli (classe `Pokemon`, API, CSV, tableau).
- `CacheImages` : sprites 96 × 96 dans `cache/sprites/{id}.png` (`chargerImage` est `synchronized`, renvoie `null`
  si l'image est indisponible), icônes du tableau (`HashMap`, utilisée seulement dans le thread de Swing).
- `Pokemon` (attributs privés, getters/setters : numéro, noms français et anglais, génération, types, image).
- Onglet Pokédex : `PanneauPokedex` (barre « Recherche : », listes `JComboBox<String>` « Génération : » et « Type : »,
  bouton « Réinitialiser », compteur ; colonnes N°, Image, Nom, Nom anglais, Gén., Type(s), triables par clic
  sur l'en-tête avec un `TableRowSorter`, sauf l'image). `FiltrePokedex` (logique sans Swing : `accepte(pokemon)`,
  nom français ou anglais qui **contient** le texte normalisé par `Suggestions.normaliser`, ou numéro si le texte
  est un nombre ; génération 0 = toutes, type vide = tous ; `TYPES` = les 18 types dans l'ordre alphabétique ;
  `texteCompteur`). `ModeleTableau` (`AbstractTableModel` : liste complète + liste filtrée refaite par une boucle à
  chaque changement, puis `fireTableDataChanged()` ; pas de `RowFilter` ; `getColumnClass` renvoie `Class` sans `<?>`,
  comme dans le tutoriel Swing d'Oracle).
- Quiz : l'indice dépend des vies restantes. `PartieQuiz.getIndice()` vaut `INDICE_COLONNES` à 3 vies,
  `INDICE_LIGNES` à 2 vies, `INDICE_SILHOUETTE` (silhouette pixelisée) à 1 vie (et à 0 : elle reste à côté du
  Pokémon dévoilé). Colonnes, lignes et silhouette utilisent le même réglage `sections` (12 par défaut).
- `Bandes` et `Bande` (colonnes verticales ou lignes horizontales selon le paramètre `vertical` du constructeur :
  couleur la plus fréquente de chaque section, contours presque noirs ignorés en option ; une colonne va du pixel
  visible le plus haut au plus bas de sa section, une ligne du plus à gauche au plus à droite ; nombre de sections
  plafonné à la largeur visible pour les colonnes, à la hauteur visible pour les lignes).
  `Bandes.zoneVisible(image)` : rectangle des pixels visibles ; `Bandes.estVisible(pixel)` : opacité >= 128.
- `SilhouettePixelisee(image, sections)` : la zone visible découpée en cases carrées de côté
  `ceil(plus grand côté / sections)` (donc au plus `sections` cases sur le plus grand côté) ; la grille couvre
  toute la zone, centrée en largeur et posée en bas (elle dépasse de moins d'une case, les cases du bord sont
  coupées par `getCase`) ; une case est noire si ses pixels visibles >= `REMPLISSAGE_MIN` (0,5) × côté²
  (seuil choisi en regardant de vrais sprites : la forme reste, oreilles, doigts et queues fines disparaissent).
  Getters `getCote`, `getNombreColonnes`, `getNombreLignes`, `isNoire(ligne, colonne)`, `getCase` ; `creerImage()`
  donne une image de la taille du sprite (cases noires, reste transparent). L'option des contours ne la change pas.
- `PartieQuiz` (logique sans interface : `VIES` = 3, `repondre` renvoie `true` si c'est le bon Pokémon, `getIndice()`).
  Points : un Pokémon trouvé rapporte les vies qui restent (3, 2 ou 1), 0 si la question est perdue ;
  `getPoints()`, `getPointsMax()` = 3 × nombre de questions de la partie, `getPointsQuestion()`.
  `passer()` (bouton « Passer ») : une vie en moins, comme une erreur, sans mauvais essai ; à la dernière vie,
  la question est perdue. `abandonner()` : finit la question tout de suite (image indisponible).
  `Suggestions` (autocomplétion : préfixe du nom français ou anglais normalisé par `normaliser`, français d'abord,
  puis anglais, tri `Collator` français, sans les Pokémon déjà proposés à tort), `Parametres` (réglages, fichier
  `parametres.properties` avec les clés `sections`, `questions`, `generations`, `indiceTypes`, `ignorerContoursNoirs` ;
  les anciennes clés `mode` et `sens` sont ignorées ; `pokemonsDuQuiz(...)` = Pokémon des générations cochées).
- Dans `PanneauQuiz`, `dessinerIndice()` dessine l'indice de `partie.getIndice()` au début de la question,
  après chaque erreur et après « Passer » (`afficherIndiceSuivant` : « Ce n'est pas X… » ou « Indice suivant… »,
  puis « Plus que 2 vies : voici l'image en lignes. » ou « Plus qu'une vie : voici la silhouette pixelisée. »).
  En haut : « Question 4 / 10 · Score : 8 / 30 » ; réussite : « Bravo ! C'est X, du premier coup : +3 points »
  (« trouvé au 2e essai : +2 points », « au 3e essai : +1 point ») ; échec : « Raté… C'était X : 0 point » ;
  fin : « Score final : 23 / 30 » et un commentaire selon la part des points.
- Interface du quiz : `PanneauQuiz`, `PanneauParametres` (aperçu : les 3 indices et la réponse, recalculés à chaque
  changement de réglage), `PanneauBandes` (des cases côte à côte avec leur légende : `nouvellesCases(sprite)`, puis
  `ajouterBandes` ou `ajouterImage` (silhouette pixelisée, sprite, ou `null` pour un « ? ») ; même échelle entière
  et même placement dans toutes les cases (centré, posé en bas), donc alignés sur le sprite ; légendes
  « Indice · 12 colonnes », « Indice · 12 lignes », « Indice · silhouette pixelisée » ; les 4 cases de l'aperçu
  passent sur 2 rangées si elles y sont plus grandes),
  `RenduSuggestion` (ligne de la liste, nom anglais en gris ; la liste est un `JTable` d'une colonne),
  `IconeVies` (cœurs dessinés en Java2D). Le chargement d'une image pour le quiz ou l'aperçu se fait dans un
  `new Thread() { ... }`, avec un numéro de chargement pour ignorer un résultat devenu inutile.

## Tests (`src/test/java/fr/sdv/pokedex/`, JUnit Jupiter)

- `SuggestionsTest`, `PartieQuizTest`, `ParametresTest`, `BandesTest`, `SilhouettePixeliseeTest`,
  `FiltrePokedexTest`, `CachePokedexTest` (37 tests : 4 + 11 + 3 + 4 + 3 + 10 + 2 ;
  `CachePokedexTest` écrit dans un dossier `@TempDir` de JUnit).
- Écrits simplement, comme en cours : `assertEquals`, `assertTrue`, `assertFalse`.
- Jamais de réseau ni de fenêtre : `BufferedImage` dessinées à la main pour les bandes et la silhouette pixelisée.
- Les tests sont dans le même paquet que le code.
- Toute nouvelle logique (hors Swing) doit avoir ses tests. L'interface Swing n'est pas testée avec JUnit.

## Conventions de code

L'utilisateur est un étudiant débutant : le code doit ressembler à celui qu'il écrirait lui-même, avec ce qu'il a vu
en cours (classes avec getters/setters, `ArrayList`, `HashMap`).

- Noms **en français** pour les classes, les méthodes et les variables (`PanneauQuiz`, `chargerImage`, `nombreQuestions`).
  Textes de l'interface en français.
- **Autorisé** : classes avec attributs `private`, constructeurs, getters/setters, héritage simple
  (`extends JPanel`, `extends JFrame`, `extends Thread`) ; `ArrayList`, `HashMap`, boucles `for`/`while`,
  `if`/`else`, `switch` classique (`case ...: break;`), `try`/`catch`, constantes `static final` ;
  méthodes de `String`, `Properties` ; classes anonymes pour les écouteurs (`ActionListener`, `DocumentListener`,
  `KeyAdapter`, `MouseAdapter`, `Runnable`, `Comparator`…) ; `SwingUtilities.invokeLater(new Runnable() { ... })`.
- **Interdit** : `record`, lambdas `->`, références de méthode `::`, streams, `Optional`, `var` ;
  `switch` en expression (`case X ->`, `yield`), text blocks `"""`, `instanceof` avec variable ;
  `List.of`, `List.copyOf`, `Map.of`, `Map.ofEntries`, `Set.of`, `Math.clamp` ;
  `ExecutorService`, `Future`, `AtomicInteger`, `ConcurrentHashMap`, `SwingWorker`, `CompletableFuture` ;
  classes `final`, enums sophistiqués, génériques complexes (`Class<?>`, `? extends`), regex compliquées,
  `@SuppressWarnings`, `Normalizer`.
- Méthodes courtes et concrètes. Peu de commentaires : quelques `//` courts et naturels là où ça aide,
  pas de Javadoc `/** */`. Accolades systématiques, imports explicites, indentation de 4 espaces.
- Logique séparée de l'interface (ex. `PartieQuiz` ne dépend pas de Swing) pour pouvoir la tester avec JUnit.
- Travail long (réseau, images) dans un `Thread` à part, jamais dans le thread de Swing ; la fenêtre n'est modifiée
  que dans le thread de Swing (`SwingUtilities.invokeLater`).
- Pas de symboles ✔ ✘ ✓ dans l'interface : ils s'affichent en carrés avec les polices Windows (« … » et « · » sont OK).
  Pour un pictogramme (comme les cœurs des vies), le dessiner en Java2D dans une `Icon` (voir `IconeVies`)
  plutôt que d'utiliser un caractère.
- PokéAPI demande de mettre les données en cache : ne pas re-télécharger ce qui est déjà dans `cache/`.
- `cache/` et `parametres.properties` sont locaux et ignorés par git. Garder leur format compatible
  (même en-tête CSV, mêmes clés) pour que les fichiers existants restent lisibles. Exception décidée avec
  l'utilisateur : l'en-tête du CSV a changé quand les statistiques ont été retirées (un ancien fichier est
  re-téléchargé une fois).
- Mise en forme : `.editorconfig` (UTF-8, 4 espaces pour le Java, 2 pour XML, YAML et Markdown).

## Façon de travailler

- L'utilisateur est un étudiant qui apprend Java : expliquer simplement.
- Ne pas créer de commit git sans demande explicite.
