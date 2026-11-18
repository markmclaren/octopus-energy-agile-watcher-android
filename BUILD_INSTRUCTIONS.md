# 📱 Build & Installation Instructions — Agile Watcher (Android)

## 🐳 Option 1: Zero-Installation Docker Compose Build (Recommended)

You can compile both Debug and Release Android APKs inside an isolated Docker container without installing Java, Gradle, or Android Studio on your Mac:

1. Open Terminal in this directory:
   ```bash
   cd /Users/ismjml/Experiments/octopus-energy-agile-watcher-android
   ```
2. Run the unit & API verification suite:
   ```bash
   python3 test_app_logic.py
   ```
3. Run the Docker Compose build script:
   ```bash
   ./build_with_docker.sh
   ```
   Or for a Release APK:
   ```bash
   ./build_release_with_docker.sh
   ```
4. Once complete, your compiled APK will be saved right in the project root:
   - `app-debug.apk`
   - `app-release.apk`

---

## 📲 Installing on Your Android Phone

1. Send `app-debug.apk` (or `app-release.apk`) to your phone via Google Drive, WhatsApp, or Email, **or** serve it over your local Wi-Fi network:
   ```bash
   python3 -m http.server 8000
   ```
2. Open the APK on your Android phone and tap **Install**.

---

## ☁️ Option 2: GitHub Actions Automated Release

When you push a version tag (`v*`, e.g. `git tag v1.0.0 && git push origin v1.0.0`) on the `main` branch, `.github/workflows/build.yml` runs automatically:
1. Verifies the tagged commit belongs to `main`
2. Executes `python3 test_app_logic.py`
3. Builds `app-debug.apk` and `app-release.apk`
4. Publishes a GitHub Release with both APKs attached.
