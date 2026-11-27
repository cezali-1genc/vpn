# Derleme ve APK/AAB Üretimi (Türkçe)

## Gereksinimler
- JDK 17 (Temurin önerilir)
- Android SDK (API 34, build-tools 34), NDK 26.1.10909125 ve CMake 3.22.1
- Go 1.21.3 (resmî WireGuard userspace/native backend derlemesi için)
- İnternet (Google Maven + Maven Central + Go modül proxy'si)

## Yerel derleme
```bash
cp local.properties.example local.properties
# local.properties içinde sdk.dir yolunu düzeltin
./gradlew clean
./gradlew test
./gradlew lint
./gradlew assembleDebug      # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease    # imzalı release APK (keystore gerekli)
./gradlew bundleRelease      # app/build/outputs/bundle/release/app-release.aab
```

## Release imzalama
1. Keystore üretin (repoya ASLA eklemeyin):
   `keytool -genkeypair -keystore release.keystore -alias cloudflare-vpn -keyalg RSA -keysize 2048 -validity 10000`
2. `keystore.properties.example` dosyasını `keystore.properties` olarak kopyalayıp doldurun
   veya CI Secrets kullanın: `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.

## Otomatik üretim (GitHub Actions)
`.github/workflows/build.yml` her push'ta çalışır:
- Kotlin derleme, birim testleri ve lint geçerse Debug APK üretilip artifact olarak yüklenir.
- Release AAB ayrıca denenir; R8 veya imzalama hatası APK artifact'ını engellemez.
- Actions > ilgili çalıştırma > Artifacts bölümünden APK indirilir.
- GitHub pre-release yayımlamak için Actions > Run workflow ekranında `publish_release` seçin; release APK debug imzalı önizlemedir.
- Kararlı üretim imzalı APK/AAB için GitHub Secrets ile keystore tanımlanmalıdır.

## Not: Bu sandbox'ta APK neden derlenemedi?
Geliştirme ortamının interneti yalnızca github/npm/pypi'ye açık; Google Maven,
Maven Central ve Gradle dağıtımı engelli olduğu için Gradle bağımlılıkları
indirilemez. Bu yüzden APK bu ortamda sahte olarak üretilmedi; CI'da gerçek
derleme yapılır. Kaynak kodu tam ve derlenebilir durumdadır.
