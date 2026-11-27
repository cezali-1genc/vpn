# APK Üretim Notu

## Doğrulanmış derleme

GitHub Actions çalıştırması **#17** (`45965884717`, commit `4c77b73`) başarıyla tamamladı:
- Kotlin derleme kontrolü, birim testleri ve lint: başarılı
- Gerçek WireGuard native backend içeren Debug APK üretimi: başarılı
- APK boyutu: 22,375,742 bayt
- Artifact: `CloudflareSecureVPN-debug-apk` (Actions artifact, 90 gün)

Bu APK debug imzalı ve `applicationId` son eki `.debug` olan test/önizleme paketidir; üretim imzalı kararlı sürüm değildir. Release AAB adımı R8 minify aşamasında başarısız oldu; bu nedenle AAB yayımlanmış sayılmaz.

## Kaynak arşivleri

- `CloudflareSecureVPN-source.zip` — uygulama + sabitlenmiş resmî WireGuard Android tunnel modülü kaynakları
- `CloudflareSecureVPN-source.swb` — kaynak arşivi + derleme bilgisi
- `SHA256SUMS.txt` — arşivlerin SHA-256 bütünlük özeti

## Yeni APK / GitHub Release üretimi

1. GitHub Actions > **Android Build (APK + AAB)** > **Run workflow**.
2. İsterseniz `publish_release` seçeneğini açıp `release_tag` girin.
3. Workflow gerçek APK'yı GitHub **pre-release** sayfasına ve Actions artifact'ına yükler. Bu mod Debug imzalı önizleme sürümüdür.
4. Üretim imzalı APK/AAB için `docs/BUILD.md` içindeki keystore'u yerel olarak ya da GitHub Secrets ile tanımlayın.

## Yerel gereksinimler

JDK 17, Android SDK API 34, NDK 26.1.10909125, CMake 3.22.1 ve Go 1.21.3 gerekir. Bu geliştirme sandbox'ında Google Maven / Maven Central erişimi kısıtlı olduğundan yerel APK üretilmedi; CI'da sahte olmayan Debug APK başarıyla derlendi.
