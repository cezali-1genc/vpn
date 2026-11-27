package com.cloudflaresecure.vpn.core.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

/**
 * Android Keystore tabanlı şifreli yapılandırma deposu (spec §7).
 * WireGuard private key'leri yalnızca burada, AES256-GCM ile şifrelenerek tutulur.
 * Hassas değerler loglanmaz.
 */
class SecureConfigStore(context: Context) {

    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_vpn_config",
        masterKeyAlias,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun putPrivateKey(serverId: String, privateKeyBase64: String) {
        prefs.edit().putString("privkey:$serverId", privateKeyBase64).apply()
    }

    fun getPrivateKey(serverId: String): String? =
        prefs.getString("privkey:$serverId", null)

    fun removePrivateKey(serverId: String) {
        prefs.edit().remove("privkey:$serverId").apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
