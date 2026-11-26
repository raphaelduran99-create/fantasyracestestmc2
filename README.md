# Fantasy Races — NeoForge 1.21.1

15 races, 45 classes (3 par race), arbres de compétences, 2 compétences actives par classe.

## Compiler le .jar
**Option A — sans rien installer (GitHub Actions)**
1. Crée un dépôt GitHub, envoie-y ce dossier (le workflow `.github/workflows/build.yml` est inclus).
2. Onglet *Actions* → build terminé → télécharge l'artefact `fantasyraces-jar`.

**Option B — en local** : JDK 21 + Gradle 8.10+ → `gradle build` → `build/libs/fantasyraces-1.0.0.jar`
(ou copie `gradlew` + `gradle/` depuis le MDK NeoForge 1.21.1 et lance `./gradlew build`).

Si Gradle ne trouve pas une version : ajuste `net.neoforged.moddev` (build.gradle) ou `neoForge.version`
(21.1.x) avec les dernières versions publiées.

## Jeu
- **K** : menu (race → classe → arbre). Au premier spawn le menu s'ouvre seul.
- **R / V** : compétences 1 / 2 de la classe (une fois débloquées dans l'arbre).
- Points de compétence : 3 au choix de la classe, puis 1 tous les 60 XP ramassés.
- `/fantasyraces respec` : rend les points. `/fantasyraces reset` (op) : change de race/classe.
