#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"
gradle --no-daemon :app:assembleMonedamoDebug
APK="$(find "app/build/outputs/apk/monedamo/debug" -maxdepth 1 -type f -name '*.apk' | head -n1)"
test -n "$APK" && test -s "$APK"
DEST="$HOME/downloads/MonedAMO-v0.1.0-debug.apk"
mkdir -p "$HOME/downloads"
cp "$APK" "$DEST"
sha256sum "$DEST"
if command -v termux-open >/dev/null 2>&1; then termux-open --view "$DEST"; else echo "APK listo: $DEST"; fi
