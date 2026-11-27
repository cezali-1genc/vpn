# Test ve Kalite Kontrol (Türkçe)

## Otomatik testler
- Birim testleri: `app/src/test/` (DnsMessageCodec, DnsCache, ServerValidator,
  KillSwitchManager, WireGuardConfigParser) — `./gradlew test`
- Instrumented: `app/src/androidTest/` — cihaz/emülatörde `./gradlew connectedAndroidTest`
- Lint: `./gradlew lint`

## Gerçek cihaz test listesi (spec §12)
- [ ] Android 8+ (API 26+) cihazda kurulum ve açılış
- [ ] Wi-Fi ve mobil veri ile bağlanma
- [ ] Ağ değişimi (Wi-Fi <-> mobil) sırasında bağlantının korunması
- [ ] Ekran kilidi ve arka plan servisi
- [ ] VPN izni iptali (onRevoke)
- [ ] Sunucu kapatılması (handshake timeout + yeniden bağlanma)
- [ ] DNS endpoint kapatılması (yedek DoH + fail-closed)
- [ ] IPv4 / IPv6 ağları
- [ ] Kill Switch (tünel kesilince trafik engeli)
- [ ] DNS sızıntısı kontrolü (DNS ekranı)
- [ ] Cihaz yeniden başlatma + "başlangıçta VPN" ayarı

Gerçek cihazda çalıştırılmayan testler çalıştırılmış gibi gösterilmez.
Sonuçlar bu dosyaya tarih ve cihaz bilgisiyle işlenir.
