@echo off
rem Lance le Pokédex : double-cliquer sur ce fichier, ou taper "lancer" dans un terminal.
rem Il compile le projet (sans relancer les tests), puis ouvre l'application.
chcp 65001 >nul
setlocal
cd /d "%~dp0"

rem Le jar produit par Maven : à changer si la version du pom.xml change
set "JAR=%~dp0target\pokedex-1.0-SNAPSHOT.jar"

if not defined JAVA_HOME set "JAVA_HOME=%USERPROFILE%\.jdks\openjdk-27"
if not exist "%JAVA_HOME%\bin\javaw.exe" (
    echo Java introuvable dans "%JAVA_HOME%".
    echo Installe un JDK 27 ou corrige la variable JAVA_HOME.
    pause
    exit /b 1
)

echo Compilation du Pokédex...
call "%~dp0mvnw.cmd" -q -B -DskipTests package
if errorlevel 1 (
    echo.
    echo La compilation a échoué : regarde les erreurs ci-dessus.
    pause
    exit /b 1
)

echo Ouverture de l'application...
rem /d : l'appli tourne depuis la racine du projet, où sont cache\ et parametres.properties
start "" /d "%~dp0." "%JAVA_HOME%\bin\javaw.exe" -jar "%JAR%"
