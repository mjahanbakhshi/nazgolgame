# Perfume Smell

An Android perfume mixing game. Pick colorful scent ingredients, fill the perfume bottle, tap `Mix`, and get a playful perfume name, score, and smell description.

The first version is simple and kid-friendly: rose, vanilla, lemon, mint, lavender, and candy can be mixed into funny perfume creations.

## Build The APK Online

This project includes a GitHub Actions workflow that builds the APK online.

1. Create a new GitHub repository.
2. Upload/push this whole folder to that repository.
3. Open the repository on GitHub.
4. Go to the `Actions` tab.
5. Run the `Build APK` workflow, or push to the `main` branch.
6. Open the finished workflow run.
7. Download the `simple-grid-game-debug-apk` artifact.
8. Unzip it to get `app-debug.apk`.

To install it on an Android phone, send `app-debug.apk` to the phone and open it. Android may ask you to allow installing unknown apps.

The main game code is in `app/src/main/java/com/example/simplegridgame/GameView.java`.
