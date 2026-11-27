# APK Üretim Notu (Türkçe)

Bu geliştirme ortamında gerçek APK derlenemedi — nedeni:
- Java/Gradle/Android SDK kurulu değil
- İnternet yalnızca github.com / npm / pypi'ye açık; Google Maven,
  Maven Central ve Gradle dağıtımı engelli

Sahte/boş APK üretilmedi (spec: sahte veri bırakma).

## Gerçek APK nasıl üretilir?
1. **Otomatik (önerilen):** Bu kod push edildiğinde GitHub Actions çalışır.
   Actions > ilgili çalıştırma > Artifacts bölümünden
   `CloudflareSecureVPN-debug-apk` (kurulabilir) ve
   `CloudflareSecureVPN-release-aab` indirilir.
2. **Yerel:** JDK 17 + Android SDK kurulu bir makinede:
   ```bash
   cp local.properties.example local.properties
   ./build-apk.sh
   # veya: ./gradlew assembleDebug
   ```

## Bu klasördeki dosyalar
- `CloudflareSecureVPN-source.zip` — tam kaynak arşivi (ZIP)
- `CloudflareSecureVPN-source.swb` — kaynak + build bilgisi arşivi (SWB)
- `SHA256SUMS.txt` — bütünlük doğrulama
