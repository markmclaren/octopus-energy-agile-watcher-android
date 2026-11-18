#!/usr/bin/env bash
set -e

echo "⚡ Building Octopus Energy Agile Watcher Release APK using Docker Compose..."

docker compose run --rm android-builder bash -c \
  "gradle assembleRelease --no-daemon --project-cache-dir /tmp/gradle-project-cache && cp app/build/outputs/apk/release/app-release.apk /workspace/app-release.apk && echo '🎉 BUILD SUCCESSFUL! app-release.apk created in /workspace/app-release.apk'"

echo ""
echo "🎉 SUCCESS! Your Release Android APK is ready at:"
echo "👉 $(pwd)/app-release.apk"
echo ""
echo "📱 To install on your phone:"
echo "1. Send 'app-release.apk' to yourself on WhatsApp / Email / Google Drive."
echo "2. Open the file on your Android phone and tap Install!"
