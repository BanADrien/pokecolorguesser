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
- Windows 11, l'utilisateur lance l'appli depuis IntelliJ.

## Commandes (PowerShell, depuis la racine du projet)

```powershell
$env:JAVA_HOME = "C:\Users\adrie\.jdks\openjdk-27"
.\mvnw.cmd -B clean verify                 # compilation, tests, jar exécutable
.\mvnw.cmd -B test                         # tests seulement
.\mvnw.cmd -B test -Dtest=QuizGameTest     # une seule classe de test
```

- La construction doit rester **sans aucun avertissement** : le compilateur tourne avec `-Xlint:all,-serial`
  (réglé dans le `pom.xml`) et le `maven-shade-plugin` est configuré pour ne pas en produire.
- `target/` appartient à Maven (ignoré par git). Le jar final `target\pokedex-1.0-SNAPSHOT.jar` contient
  aussi Jackson (`maven-shade-plugin`, `Main-Class: fr.sdv.pokedex.Main`).
- Lancer : ▶ Run sur `fr.sdv.pokedex.Main` dans IntelliJ (dossier de travail = racine du projet),
  ou `java -jar target\pokedex-1.0-SNAPSHOT.jar`.
- Tests de l'interface : lancer le jar **depuis un dossier temporaire** (pour ne pas créer `cache/` ni
  `parametres.properties` dans le projet ; y copier un `cache/pokedex.csv` existant évite de tout re-télécharger),
  peindre le `JRootPane` dans un PNG avec `SwingUtilities.invokeAndWait`, puis regarder l'image.
  La fenêtre s'ouvre brièvement sur l'écran.

## Architecture (`src/main/java/fr/sdv/pokedex/`)

- `Main` : classe de lancement (`public static void main(String[] args)`), appelle seulement `PokedexFrame.launch()`.
- `PokeApiClient` : appels HTTP à PokéAPI (`download` avec nouvelles tentatives + gzip, 16 requêtes en parallèle maximum).
  Le JSON est lu avec Jackson (`ObjectMapper.readTree`, puis `JsonNode.path(...)`, `asText()`, `asInt()`).
  La lecture des réponses est dans des méthodes statiques package-private, sans réseau :
  `speciesIds(generation)`, `toPokemon(species, pokemon, generation)`, `defaultVarietyUrl(species)`.
- `PokemonRepository` : liste des Pokémon depuis le cache `cache/pokedex.csv`, sinon depuis l'API.
  Changer l'en-tête du CSV (`HEADER`) quand son format change : l'ancien cache est alors re-téléchargé automatiquement.
- `SpriteCache` : sprites 96×96 téléchargés à la demande, gardés dans `cache/sprites/`.
- `Pokemon` (record), `PokemonTableModel`, `PokedexFrame` (onglets Pokédex / Quiz / Paramètres).
- Quiz : `ColorBands` (couleur dominante par bande horizontale, contours presque noirs ignorés en option),
  `QuizGame` (logique sans interface : `LIVES` = 3 vies par Pokémon, `answer` renvoie `Result`
  CORRECT / WRONG_RETRY / LOST, `sections(base)` double les bandes à chaque vie perdue),
  `NameSuggestions` (autocomplétion de la réponse : préfixe du nom français ou anglais normalisé,
  français d'abord, puis anglais, tri `Collator` français, sans les Pokémon déjà proposés à tort),
  `QuizSettings` (réglages, fichier `parametres.properties` ; `pool(...)` = Pokémon des générations cochées),
  `ColorBandsView`, `QuizPanel` (champ + liste de suggestions, cœurs des vies), `SuggestionRenderer`
  (ligne de la liste, nom anglais en gris), `LivesIcon` (cœurs dessinés en Java2D), `SettingsPanel`.
- Il n'y a plus qu'un mode de réponse (champ avec autocomplétion) : une ancienne clé `mode=` dans
  `parametres.properties` est ignorée au chargement.

## Tests (`src/test/java/fr/sdv/pokedex/`, JUnit Jupiter)

- `QuizGameTest`, `NameSuggestionsTest`, `ColorBandsTest`, `QuizSettingsTest`, `PokemonRepositoryTest`,
  `PokeApiClientTest` (65 tests).
- Jamais de réseau ni de fenêtre : `BufferedImage` dessinées pixel par pixel, fichiers dans un `@TempDir`,
  JSON d'exemple dans `src/test/resources/pokeapi/` (vraies réponses generation/1, pokemon-species/25 et pokemon/25,
  réduites aux champs utilisés par le code).
- Les tests sont dans le même paquet que le code : ils peuvent appeler les méthodes package-private.
- Toute nouvelle logique (hors Swing) doit avoir ses tests. L'interface Swing n'est pas testée avec JUnit.

## Conventions de code

- Textes de l'interface, commentaires et Javadoc **en français**.
- Java moderne : records, `switch` en expression, `List.of`/`List.copyOf`. Accolades systématiques, imports explicites.
- Logique séparée de l'interface (ex. `QuizGame` ne dépend pas de Swing) pour pouvoir la tester avec JUnit.
- Travail long (réseau, images) dans un `SwingWorker` ou un pool de threads, jamais dans le thread Swing.
- Pas de symboles ✔ ✘ ✓ dans l'interface : ils s'affichent en carrés avec les polices Windows (« … » et « · » sont OK).
  Pour un pictogramme (comme les cœurs des vies), le dessiner en Java2D dans une `Icon` (voir `LivesIcon`)
  plutôt que d'utiliser un caractère.
- PokéAPI demande de mettre les données en cache : ne pas re-télécharger ce qui est déjà dans `cache/`.
- `cache/` et `parametres.properties` sont locaux et ignorés par git.
- Mise en forme : `.editorconfig` (UTF-8, 4 espaces pour le Java, 2 pour XML, YAML et Markdown).

## Façon de travailler

- L'utilisateur est un étudiant qui apprend Java : expliquer simplement.
- Ne pas créer de commit git sans demande explicite.
