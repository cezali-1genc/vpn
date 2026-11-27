# Güvenlik Politikası

## Bildirim
Güvenlik açığı bulursanız lütfen herkese açık issue açmayın; repo sahibine
özel olarak bildirin.

## Uygulama güvenlik modeli
- WireGuard private key'ler Android Keystore (AES256-GCM) ile şifrelenir.
- `usesCleartextTraffic=false`; tüm ağ HTTPS/TLS ile doğrulanır.
- DoH başarısız olursa şifresiz DNS'e düşülmez (fail-closed).
- VPN trafiği reklam/takip/izinsiz analiz amacıyla kullanılmaz.
- Hassas veriler analitik sistemine gönderilmez.
- İmzalama anahtarları repoya eklenmez.
