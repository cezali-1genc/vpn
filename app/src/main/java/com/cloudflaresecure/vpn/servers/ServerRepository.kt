package com.cloudflaresecure.vpn.servers

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cloudflaresecure.vpn.core.security.SecureConfigStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.serverStore: DataStore<Preferences> by preferencesDataStore("vpn_servers")

/**
 * Sunucu CRUD deposu (spec §4B).
 * Sunucu meta verisi DataStore'da, private key SecureConfigStore'da tutulur.
 */
class ServerRepository(context: Context) {

    private val appContext = context.applicationContext
    private val secure = SecureConfigStore(appContext)
    private val key = stringPreferencesKey("servers_json")

    val servers: Flow<List<VpnServer>> = appContext.serverStore.data.map { p ->
        decode(p[key])
    }

    suspend fun add(server: VpnServer, clientPrivateKeyBase64: String) {
        val error = ServerValidator.validate(server)
        require(error == null) { error }
        val current = servers.firstOrNull() ?: emptyList()
        secure.putPrivateKey(server.id, clientPrivateKeyBase64)
        appContext.serverStore.edit { it[key] = encode(current + server) }
    }

    suspend fun update(server: VpnServer, clientPrivateKeyBase64: String? = null) {
        val error = ServerValidator.validate(server)
        require(error == null) { error }
        val current = servers.firstOrNull() ?: emptyList()
        if (clientPrivateKeyBase64 != null) secure.putPrivateKey(server.id, clientPrivateKeyBase64)
        appContext.serverStore.edit {
            it[key] = encode(current.map { if (it.id == server.id) server else it })
        }
    }

    suspend fun delete(serverId: String) {
        val current = servers.firstOrNull() ?: emptyList()
        secure.removePrivateKey(serverId)
        appContext.serverStore.edit { it[key] = encode(current.filter { it.id != serverId }) }
    }

    suspend fun getById(serverId: String): VpnServer? =
        (servers.firstOrNull() ?: emptyList()).firstOrNull { it.id == serverId }

    fun privateKeyFor(serverId: String): String? = secure.getPrivateKey(serverId)

    private fun encode(list: List<VpnServer>): String {
        val arr = JSONArray()
        for (s in list) {
            arr.put(JSONObject().apply {
                put("id", s.id); put("name", s.name); put("country", s.country)
                put("city", s.city); put("endpointHost", s.endpointHost)
                put("endpointPort", s.endpointPort); put("serverPublicKey", s.serverPublicKey)
                put("clientAddress", s.clientAddress); put("allowedIps", s.allowedIps)
                put("persistentKeepalive", s.persistentKeepalive)
                put("isActive", s.isActive)
                put("lastLatencyMs", s.lastLatencyMs ?: JSONObject.NULL)
                put("isHealthy", s.isHealthy ?: JSONObject.NULL)
            })
        }
        return arr.toString()
    }

    private fun decode(json: String?): List<VpnServer> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                VpnServer(
                    id = o.optString("id"),
                    name = o.optString("name"),
                    country = o.optString("country"),
                    city = o.optString("city"),
                    endpointHost = o.optString("endpointHost"),
                    endpointPort = o.optInt("endpointPort", 51820),
                    serverPublicKey = o.optString("serverPublicKey"),
                    clientAddress = o.optString("clientAddress"),
                    allowedIps = o.optString("allowedIps", "0.0.0.0/0, ::/0"),
                    persistentKeepalive = o.optInt("persistentKeepalive", 25),
                    isActive = o.optBoolean("isActive", false),
                    lastLatencyMs = if (o.isNull("lastLatencyMs")) null else o.optLong("lastLatencyMs"),
                    isHealthy = if (o.isNull("isHealthy")) null else o.optBoolean("isHealthy")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
