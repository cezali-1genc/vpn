# WireGuard VPN Sunucusu Kurulumu — Ubuntu VPS (Türkçe)

> **Önemli ayrım (spec §15):** VPN sunucusu, DNS servisi ve Android uygulaması üç ayrı parçadır.
> Bu uygulama sunucuya *bağlanan istemcidir*; sunucunun kendisi APK içine gömülmez.
> Sunucu olmadan uygulama "sunucu yapılandırılmadı" gösterir — sahte sunucu gösterilmez.

## 1. VPS hazırlığı (Ubuntu 22.04/24.04)
```bash
sudo apt update && sudo apt upgrade -y
sudo apt install -y wireguard iptables qrencode
sudo sysctl -w net.ipv4.ip_forward=1
sudo sysctl -w net.ipv6.conf.all.forwarding=1
echo "net.ipv4.ip_forward=1" | sudo tee -a /etc/sysctl.conf
echo "net.ipv6.conf.all.forwarding=1" | sudo tee -a /etc/sysctl.conf
sudo sysctl -p
```

## 2. Sunucu anahtar çifti
```bash
cd /etc/wireguard
wg genkey | tee server.key | wg pubkey > server.pub
cat server.pub
```

## 3. Sunucu yapılandırması (/etc/wireguard/wg0.conf)
```ini
[Interface]
PrivateKey = <SERVER_PRIVATE_KEY>
Address = 10.8.0.1/24, fd00:8::1/64
ListenPort = 51820
PostUp = iptables -A FORWARD -i wg0 -j ACCEPT; iptables -t nat -A POSTROUTING -o eth0 -j MASQUERADE
PostDown = iptables -D FORWARD -i wg0 -j ACCEPT; iptables -t nat -D POSTROUTING -o eth0 -j MASQUERADE

[Peer]
# İstemci 1 — anahtarları aşağıda üretildikten sonra ekleyin
PublicKey = <CLIENT1_PUBLIC_KEY>
AllowedIPs = 10.8.0.2/32, fd00:8::2/128
```

## 4. İstemci anahtarları ve ekleme
```bash
wg genkey | tee client1.key | wg pubkey > client1.pub
# client1.pub içeriğini wg0.conf [Peer] bölümüne ekleyin
sudo systemctl enable --now wg-quick@wg0
sudo wg show
sudo ufw allow 51820/udp
```

## 5. İstemci iptal etme / anahtar yenileme
- İptal: ilgili `[Peer]` bloğunu silip `sudo wg syncconf wg0 <(wg-quick strip wg0)` çalıştırın.
- Yenileme: yeni anahtar çifti üretip public key'i güncelleyin.

## 6. Uygulamaya bağlama
Uygulama > Sunucular > ".conf İçe Aktar" ile istemci .conf dosyasını ekleyin:
```ini
[Interface]
PrivateKey = <CLIENT_PRIVATE_KEY>
Address = 10.8.0.2/32

[Peer]
PublicKey = <SERVER_PUBLIC_KEY>
Endpoint = <VPS_IP>:51820
AllowedIPs = 0.0.0.0/0, ::/0
PersistentKeepalive = 25
```
Private key yalnızca cihazda Keystore ile şifrelenerek saklanır.

## 7. Durum kontrolü
```bash
sudo wg show
sudo systemctl status wg-quick@wg0
sudo wg show wg0 latest-handshakes
```
