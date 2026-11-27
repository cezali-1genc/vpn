# Cloudflare Secure VPN

Gerçek WireGuard VPN bağlantısı + Cloudflare DNS-over-HTTPS (DoH) + Kill Switch
içeren, tek Android uygulaması (Kotlin + Jetpack Compose, API 26+).

> **Durum:** Tam Android Studio kaynak projesi. Gerçek VPN sunucusu olmadan
> uygulama "sunucu yapılandırılmadı" gösterir; sahte sunucu/hız gösterilmez.
> Sunucu kurulumu: [docs/server-setup.md](docs/server-setup.md)

## Özellikler
- **Gerçek VPN motoru:** Android `VpnService` + resmî WireGuard Tunnel Library,
  TUN arayüzü, handshake doğrulaması, otomatik yeniden bağlanma, `onRevoke()`.
- **Cloudflare DoH:** RFC 8484 binary DNS, birincil/yedek endpoint, TTL önbellek,
  yerel DNS proxy; şifresiz DNS'e düşülmez (fail-closed).
- **Güvenlik:** Kill Switch, DNS/IPv6 sızıntı koruması, Keystore şifreli saklama.
- **Sunucu merkezi:** Ekle/sil/düzenle/test et, `.conf`/QR içe aktarma, gecikme ölçümü.
- **Analiz:** Gerçek indirme/yükleme (TrafficStats + Cloudflare hız testi), ping (DoH).
- **Bildirim:** Foreground servis bildirimi (durum, sunucu, süre, kes/yeniden bağlan).
- **Arayüz:** Ana Sayfa, Sunucular, DNS, Güvenlik, Ayarlar (alt menü).

## Hızlı başlangıç
```bash
# Gereksinim: JDK 17 + Android SDK (bkz. docs/BUILD.md)
cp local.properties.example local.properties   # sdk.dir yolunu düzeltin
./gradlew assembleDebug
# Çıktı: app/build/outputs/apk/debug/app-debug.apk
```

Otomatik APK/AAB: GitHub Actions (`.github/workflows/build.yml`) her push'ta
Debug APK + Release AAB üretir; Actions > Artifacts bölümünden indirilir.

## Mimari (tek uygulama, modüler paketler)
```
app/src/main/java/com/cloudflaresecure/vpn/
  core/network|security|storage/   durum, hata, Keystore, sızıntı kontrolü
  vpn/wireguard|service|routing/   WireGuard, VpnService, Kill Switch
  dns/doh|resolver|cache/          DoH motoru, yerel proxy, önbellek
  servers/  monitoring/  settings/  notifications/
  ui/home|servers|dns|security|settings/
```

## Önemli ayrım
1. **VPN sunucusu:** Trafiği taşıyan gerçek WireGuard sunucusu (VPS).
2. **DNS servisi:** Cloudflare DoH.
3. **Android uygulaması:** İkisini yöneten istemci (bu repo).
APK, sunucuya bağlanan istemcidir; sunucu APK içine gömülmez.

## Lisans
Boost Software License 1.0 — bkz. [LICENSE](LICENSE) ve [NOTICE](NOTICE).
Gizlilik: [PRIVACY.md](PRIVACY.md) • Güvenlik: [SECURITY.md](SECURITY.md)
