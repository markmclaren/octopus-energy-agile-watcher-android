#!/usr/bin/env bash
set -e

echo "⚡ Building Octopus Energy Agile Watcher Debug APK using Docker Compose (No local Java/Gradle required)..."

docker compose run --rm android-builder bash -c \
  "gradle assembleDebug --no-daemon --project-cache-dir /tmp/gradle-project-cache && cp app/build/outputs/apk/debug/app-debug.apk /workspace/app-debug.apk && echo '🎉 BUILD SUCCESSFUL! app-debug.apk created in /workspace/app-debug.apk'"

echo ""
echo "🎉 SUCCESS! Your Android APK is compiled and saved at:"
echo "👉 $(pwd)/app-debug.apk"
echo ""
echo "📱 To install on your phone:"
echo "1. Send 'app-debug.apk' to yourself on WhatsApp / Email / Google Drive."
echo "2. Open the file on your Android phone and tap Install!"
