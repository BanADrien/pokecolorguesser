# Pokédex Java (cours SDV)

Application Java Swing : un Pokédex (tableau des Pokémon des générations 1 à 8 récupérés sur PokéAPI),
un quiz « deviner le Pokémon à partir de ses bandes de couleurs » et une page de paramètres.

## Environnement

- Projet IntelliJ simple : sources dans `src/`, **pas de Maven/Gradle, aucune dépendance externe**.
- JDK 27 : `C:\Users\adrie\.jdks\openjdk-27` (aussi dans le PATH utilisateur et `JAVA_HOME`).
  Si `java`/`javac` ne sont pas trouvés dans le shell, utiliser le chemin complet `...\openjdk-27\bin\javac.exe`.
- Windows 11, l'utilisateur lance l'appli depuis IntelliJ.

## Commandes

- Lancer : ▶ Run sur `src/Main.java` dans IntelliJ (dossier de travail = racine du projet).
- Vérifier que tout compile (PowerShell), **toujours vers un dossier temporaire, jamais dans le projet** (`out/` appartient à IntelliJ) :
  ```powershell
  $src = Get-ChildItem src -Recurse -Filter *.java | ForEach-Object FullName
  javac -encoding UTF-8 -Xlint:all -d "$env:TEMP\pokedex-out" $src
  ```
  Seuls les avertissements `[serial]` (classes Swing) sont acceptés.
- Tests de l'interface : lancer l'appli depuis un dossier temporaire (pour ne pas créer `cache/` ni
  `parametres.properties` dans le projet), peindre le `JRootPane` dans un PNG avec
  `SwingUtilities.invokeAndWait`, puis regarder l'image. La fenêtre s'ouvre brièvement sur l'écran.

## Architecture (`src/pokedex/`)

- `Main.java` (racine de `src/`) : fichier source compact Java 25, appelle seulement `PokedexFrame.launch()`.
- `PokeApiClient` : appels HTTP à PokéAPI (`download` avec nouvelles tentatives + gzip, 16 requêtes en parallèle maximum).
- `PokemonRepository` : liste des Pokémon depuis le cache `cache/pokedex.csv`, sinon depuis l'API.
  Changer l'en-tête du CSV quand son format change : l'ancien cache est alors re-téléchargé automatiquement.
- `SpriteCache` : sprites 96×96 téléchargés à la demande, gardés dans `cache/sprites/`.
- `Json` : petit parseur JSON maison (le JDK n'en a pas).
- `Pokemon` (record), `PokemonTableModel`, `PokedexFrame` (onglets Pokédex / Quiz / Paramètres).
- Quiz : `ColorBands` (couleur dominante par bande horizontale), `QuizGame` (logique sans interface),
  `QuizSettings` (réglages, fichier `parametres.properties`), `ColorBandsView`, `QuizPanel`, `SettingsPanel`.

## Conventions de code

- Textes de l'interface, commentaires et Javadoc **en français**.
- Java moderne : records, `switch` en expression, `List.of`/`List.copyOf`. Accolades systématiques, imports explicites.
- Logique séparée de l'interface (ex. `QuizGame` ne dépend pas de Swing) pour pouvoir la tester en console.
- Travail long (réseau, images) dans un `SwingWorker` ou un pool de threads, jamais dans le thread Swing.
- Pas de symboles ✔ ✘ ✓ dans l'interface : ils s'affichent en carrés avec les polices Windows (« … » et « · » sont OK).
- PokéAPI demande de mettre les données en cache : ne pas re-télécharger ce qui est déjà dans `cache/`.
- `cache/` et `parametres.properties` sont locaux et ignorés par git.

## Façon de travailler

- L'utilisateur est un étudiant qui apprend Java : expliquer simplement.
- Ne pas créer de commit git sans demande explicite.
