package com.demo.creditlimit.network.manager

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "token_prefs")

/**
 * Manages access / refresh tokens.
 *
 * Dual storage strategy:
 * - **DataStore** (persistent) — survives process restarts.
 * - **In-memory cache** (`@Volatile`) — used by [AuthInterceptor] which
 *   runs on OkHttp's synchronous I/O threads and cannot call suspend fns.
 *
 * Call [loadFromDataStore] once at startup to warm the cache.
 */
class TokenManager private constructor(private val context: Context) {

    // ------- in-memory cache (fast, synchronous) -------

    @Volatile
    private var cachedAccessToken: String? = null

    fun getCachedToken(): String? = cachedAccessToken

    // ------- DataStore keys -------

    private val accessTokenKey = stringPreferencesKey("access_token")
    private val refreshTokenKey = stringPreferencesKey("refresh_token")

    // ------- reactive streams -------

    val accessTokenFlow: Flow<String?> = context.dataStore.data.map { it[accessTokenKey] }
    val refreshTokenFlow: Flow<String?> = context.dataStore.data.map { it[refreshTokenKey] }

    // ------- suspend write helpers -------

    suspend fun saveAccessToken(token: String) {
        cachedAccessToken = token
        context.dataStore.edit { it[accessTokenKey] = token }
    }

    suspend fun saveRefreshToken(token: String) {
        context.dataStore.edit { it[refreshTokenKey] = token }
    }

    suspend fun clearTokens() {
        cachedAccessToken = null
        context.dataStore.edit { prefs ->
            prefs.remove(accessTokenKey)
            prefs.remove(refreshTokenKey)
        }
    }

    // ------- startup warm-up -------

    /** Load persisted token into the in-memory cache on first launch. */
    suspend fun loadFromDataStore() {
        cachedAccessToken = context.dataStore.data.firstOrNull()?.get(accessTokenKey)
    }

    // ------- singleton factory -------

    companion object {
        @Volatile
        private var instance: TokenManager? = null

        fun getInstance(context: Context): TokenManager =
            instance ?: synchronized(this) {
                instance ?: TokenManager(context.applicationContext).also { instance = it }
            }
    }
}
