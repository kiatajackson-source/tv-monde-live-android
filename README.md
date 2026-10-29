# TV MONDE LIVE — application Android

Projet de départ Android natif en Kotlin, avec lecteur intégré Media3/ExoPlayer, lecture HLS `.m3u8`, recherche, rubriques, favoris et catalogue JSON modifiable.

## Ce que cette première version contient

- Interface bleu ciel sur fond sombre.
- Lecteur intégré avec contrôles vidéo.
- Sélection de pistes vidéo jusqu'à 1920 × 1080 lorsque le flux et l'appareil le permettent.
- Recherche, catégories et favoris sauvegardés sur le téléphone.
- Catalogue dans `app/src/main/assets/channels.json`.
- Compilation APK de test automatique via GitHub Actions.
- Page d'accueil Web dans `site/index.html`, publiable comme site statique Render.

## Important : catalogue de test

Le catalogue contient uniquement un **flux HLS de démonstration technique**. Ce flux n'est pas une chaîne TV et peut être modifié ou devenir indisponible. Il sert à tester le lecteur. Avant d'ajouter une chaîne réelle, utiliser son flux officiel et vérifier que ses conditions autorisent la diffusion dans cette application. N'ajoutez pas de flux piratés ni de liens récupérés sans autorisation.

Pour ajouter une chaîne, modifiez `app/src/main/assets/channels.json` avec cette structure :

```json
{
  "id": "identifiant-unique",
  "name": "Nom officiel de la chaîne",
  "country": "Pays",
  "region": "AFRIQUE",
  "category": "ACTUALITÉS",
  "url": "https://exemple.org/flux.m3u8",
  "description": "Source officielle autorisée",
  "demo": false
}
```

Les régions prises en charge par les filtres sont `AFRIQUE`, `EUROPE`, `ASIE` et `AMÉRIQUE`. Les catégories d'exemple sont `ACTUALITÉS`, `SPORT`, `MUSIQUE` et `ENFANTS`.

## Construire l'APK gratuitement sur GitHub

1. Créez un dépôt GitHub public, par exemple `tv-monde-live-android`.
2. Importez tous les fichiers de ce dossier à la racine du dépôt (y compris le dossier caché `.github`).
3. Ouvrez l'onglet **Actions** du dépôt. Le workflow « Construire TV MONDE LIVE APK » démarre après un push, ou lancez-le avec **Run workflow**.
4. Quand le workflow est terminé avec une coche verte, ouvrez son exécution et téléchargez l'artefact `TV-MONDE-LIVE-APK-TEST`.
5. Dans l'archive téléchargée, récupérez `app-debug.apk`, puis installez-le sur Android en autorisant l'installation depuis la source utilisée.

L'APK généré ici est un **APK de test signé avec la clé de débogage**, pas une version de production signée pour Google Play. Gardez votre vraie clé de signature privée pour la version de publication.

## Déployer la page sur Render

Render héberge la page Web d'information, pas la compilation Android elle-même.

1. Connectez le dépôt GitHub à Render.
2. Créez un **Static Site**.
3. Indiquez `site` comme **Root Directory** et `.` comme **Publish Directory** si Render demande les deux champs ; laissez la commande de build vide si l'interface le permet.
4. Déployez. Le code Android continue d'être compilé par GitHub Actions.

## Gratuité et Internet

L'application n'intègre aucun abonnement ni paiement. Cela ne rend pas la connexion Internet gratuite : regarder la télévision en direct consomme des données mobiles ou du Wi-Fi. Les flux HD peuvent consommer beaucoup de données.

Une application gratuite ne reçoit pas automatiquement de l'argent à chaque téléchargement. Pour gagner de l'argent, il faut par exemple vendre l'application (donc la rendre payante), utiliser des publicités conformes aux règles ou proposer des dons volontaires. Cette version n'inclut aucune publicité.

## Versions techniques

- Android `minSdk 23`, cible Android 16 / API 36.
- Kotlin, Android Gradle Plugin 8.13.2 et Gradle 8.13.
- AndroidX Media3/ExoPlayer 1.11.1.
