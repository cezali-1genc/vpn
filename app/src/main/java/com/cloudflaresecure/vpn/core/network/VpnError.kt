package com.cloudflaresecure.vpn.core.network

/**
 * Spec §10 — merkezi hata taksonomisi.
 * Her hata: kullanıcı açıklaması + sebep + çözüm + teknik kod.
 */
enum class VpnError(
    val code: String,
    val title: String,
    val explanation: String,
    val solution: String
) {
    VPN_PERMISSION_DENIED(
        "VPN_PERMISSION_DENIED", "VPN izni reddedildi",
        "Android VPN izni verilmedi. İzinsiz tünel kurulamaz.",
        "Bağlan'a tekrar dokunun ve sistem penceresinde 'Bağlan' seçeneğini onaylayın."
    ),
    SERVER_NOT_CONFIGURED(
        "SERVER_NOT_CONFIGURED", "Sunucu yapılandırılmadı",
        "Bağlanılacak gerçek bir WireGuard sunucusu tanımlı değil.",
        "Sunucular ekranından gerçek sunucu bilgilerinizi ekleyin (docs/server-setup.md)."
    ),
    INVALID_CONFIGURATION(
        "INVALID_CONFIGURATION", "Geçersiz yapılandırma",
        "WireGuard yapılandırması doğrulanamadı (anahtar, adres veya endpoint hatalı).",
        "Sunucu bilgilerini kontrol edin; .conf dosyasını yeniden içe aktarın."
    ),
    HANDSHAKE_TIMEOUT(
        "HANDSHAKE_TIMEOUT", "Handshake zaman aşımı",
        "Sunucu ile WireGuard handshake tamamlanamadı.",
        "İnternet bağlantınızı kontrol edin, sunucunun çalıştığını doğrulayın, tekrar deneyin."
    ),
    SERVER_UNREACHABLE(
        "SERVER_UNREACHABLE", "Sunucuya ulaşılamıyor",
        "Sunucu endpoint'ine ağ bağlantısı kurulamadı.",
        "Endpoint adresi/portu kontrol edin; ağ değişikliğinden sonra tekrar deneyin."
    ),
    DNS_CONNECTION_FAILED(
        "DNS_CONNECTION_FAILED", "DNS bağlantısı başarısız",
        "Cloudflare DoH endpoint'ine ulaşılamadı.",
        "İnternet bağlantınızı kontrol edin; yedek DoH endpoint'i otomatik denenir."
    ),
    DOH_CERTIFICATE_ERROR(
        "DOH_CERTIFICATE_ERROR", "DoH sertifika hatası",
        "DoH bağlantısında TLS sertifika doğrulaması başarısız oldu.",
        "Cihaz tarih/saatini kontrol edin; güvenli olmayan ağlarda denemeyin."
    ),
    DNS_LEAK_DETECTED(
        "DNS_LEAK_DETECTED", "DNS sızıntısı tespit edildi",
        "DNS sorgularının tünel dışına çıktığı tespit edildi.",
        "Kill Switch ve DNS Koruması'nı açın; bağlantıyı yeniden kurun."
    ),
    IPV6_ROUTE_ERROR(
        "IPV6_ROUTE_ERROR", "IPv6 yönlendirme hatası",
        "IPv6 trafiği güvenli şekilde yönlendirilemedi/engellenemedi.",
        "IPv6 Koruması'nı etkinleştirin; ağınızı değiştirip tekrar deneyin."
    ),
    TUNNEL_INTERRUPTED(
        "TUNNEL_INTERRUPTED", "Tünel kesildi",
        "WireGuard tüneli beklenmedik şekilde sonlandı.",
        "Otomatik yeniden bağlanma devreye girer; sorun sürerse uygulamayı yeniden başlatın."
    ),
    NETWORK_UNAVAILABLE(
        "NETWORK_UNAVAILABLE", "Ağ kullanılamıyor",
        "Cihazda aktif internet bağlantısı yok.",
        "Wi-Fi/mobil veriyi kontrol edip tekrar deneyin."
    ),
    KEY_INVALID(
        "KEY_INVALID", "Anahtar geçersiz",
        "WireGuard private/public anahtarı geçersiz formatta.",
        "Anahtarları sunucu tarafında yeniden üretip yapılandırmayı güncelleyin."
    ),
    STORAGE_ERROR(
        "STORAGE_ERROR", "Depolama hatası",
        "Şifreli yapılandırma okunamadı/yazılamadı.",
        "Uygulama verisini temizleyip sunucuyu yeniden ekleyin."
    ),
    SERVICE_START_FAILED(
        "SERVICE_START_FAILED", "Servis başlatılamadı",
        "VPN foreground servisi başlatılamadı.",
        "Uygulamayı yeniden başlatın; Android sürümünüzün desteklendiğini doğrulayın."
    );

    fun userMessage(): String = "$title — $explanation Çözüm: $solution"
}
