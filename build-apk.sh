#!/usr/bin/env bash
# Cloudflare Secure VPN — APK/AAB üretim betiği (Türkçe)
# Gereksinim: JDK 17 + Android SDK + internet
set -euo pipefail
cd "$(dirname "$0")"
chmod +x gradlew
echo "== Temizlik =="
./gradlew clean
echo "== Testler =="
./gradlew test
echo "== Lint =="
./gradlew lint
echo "== Debug APK =="
./gradlew assembleDebug
echo "== Release AAB =="
./gradlew bundleRelease
echo "== Release APK (imzalı, keystore.properties gerekli) =="
./gradlew assembleRelease || echo "Release APK atlandı: keystore.properties yok (bkz. docs/BUILD.md)"
echo "Çıktılar:"
find app/build/outputs -name "*.apk" -o -name "*.aab" | sort
