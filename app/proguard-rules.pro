# Cloudflare Secure VPN — R8/ProGuard kuralları
-keep class com.wireguard.** { *; }
-keep class com.cloudflaresecure.vpn.vpn.service.CloudflareVpnService { *; }
-dontwarn com.wireguard.**
