# Pokédex Java (cours SDV)

Application Java Swing : un Pokédex (tableau des Pokémon des générations 1 à 8 récupérés sur PokéAPI),
un quiz « deviner le Pokémon à partir de ses bandes de couleurs » et une page de paramètres.
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
  `parametres.properties` dans le projet ; y copier un `cache/pokedex.csv` existant évite de tout re-télécharger),
  peindre le `JRootPane` dans un PNG avec `SwingUtilities.invokeAndWait`, puis regarder l'image.
  La fenêtre s'ouvre brièvement sur l'écran. Ne pas faire biper l'appli (champ désactivé, dernière génération décochée).

## Architecture (`src/main/java/fr/sdv/pokedex/`)

- `Main` : `main` crée la `FenetrePokedex` dans le thread de Swing (`SwingUtilities.invokeLater`) avec l'apparence Windows.
- `FenetrePokedex` (`extends JFrame`) : onglets Pokédex / Quiz / Paramètres, barre d'état et barre de progression.
  `chargerPokemons()` démarre un `ChargementPokedex`.
- `ChargementPokedex` (`extends Thread`) : lit `cache/pokedex.csv` (`CachePokedex`), sinon télécharge les Pokémon
  **un par un** avec `ApiPokemon` (progression envoyée à la fenêtre), puis charge une par une les images du tableau
  (`CacheImages`). La fenêtre n'est modifiée que via `SwingUtilities.invokeLater`.
- `ApiPokemon` : appels HTTP à PokéAPI (`HttpURLConnection`, gzip, 3 essais), JSON lu avec Jackson
  (`ObjectMapper.readTree`, `path(...)`, `asText()`, `asInt()`).
- `CachePokedex` : lecture et écriture de `cache/pokedex.csv`. Changer l'en-tête (`EN_TETE`) quand le format change :
  l'ancien fichier est alors ignoré et tout est re-téléchargé.
- `CacheImages` : sprites 96 × 96 dans `cache/sprites/{id}.png` (`chargerImage` est `synchronized`, renvoie `null`
  si l'image est indisponible), icônes du tableau (`HashMap`, utilisée seulement dans le thread de Swing).
- `Pokemon` (attributs privés, getters/setters), `ModeleTableau` (`AbstractTableModel` du tableau ;
  `getColumnClass` renvoie `Class` sans `<?>`, comme dans le tutoriel Swing d'Oracle).
- Quiz : `Bandes` et `Bande` (couleur la plus fréquente par bande horizontale, contours presque noirs ignorés en option),
  `PartieQuiz` (logique sans interface : `VIES` = 3, `repondre` renvoie `true` si c'est le bon Pokémon,
  `nombreDeBandes(base)` double les bandes à chaque vie perdue), `Suggestions` (autocomplétion : préfixe du nom
  français ou anglais normalisé par `normaliser`, français d'abord, puis anglais, tri `Collator` français,
  sans les Pokémon déjà proposés à tort), `Parametres` (réglages, fichier `parametres.properties` avec les clés
  `sections`, `questions`, `generations`, `indiceTypes`, `ignorerContoursNoirs` ; une ancienne clé `mode` est ignorée ;
  `pokemonsDuQuiz(...)` = Pokémon des générations cochées).
- Interface du quiz : `PanneauQuiz`, `PanneauParametres`, `PanneauBandes` (les deux cases dessinées),
  `RenduSuggestion` (ligne de la liste, nom anglais en gris ; la liste est un `JTable` d'une colonne),
  `IconeVies` (cœurs dessinés en Java2D). Le chargement d'une image pour le quiz ou l'aperçu se fait dans un
  `new Thread() { ... }`, avec un numéro de chargement pour ignorer un résultat devenu inutile.

## Tests (`src/test/java/fr/sdv/pokedex/`, JUnit Jupiter)

- `SuggestionsTest`, `PartieQuizTest`, `ParametresTest`, `BandesTest` (11 tests).
- Écrits simplement, comme en cours : `assertEquals`, `assertTrue`, `assertFalse`.
- Jamais de réseau ni de fenêtre : `BufferedImage` dessinées à la main pour les bandes.
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
  (même en-tête CSV, mêmes clés) pour que les fichiers existants restent lisibles.
- Mise en forme : `.editorconfig` (UTF-8, 4 espaces pour le Java, 2 pour XML, YAML et Markdown).

## Façon de travailler

- L'utilisateur est un étudiant qui apprend Java : expliquer simplement.
- Ne pas créer de commit git sans demande explicite.
