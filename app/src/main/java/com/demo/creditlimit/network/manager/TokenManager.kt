package com.demo.creditlimit.network.manager

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class TokenManager private constructor(private val context: Context) {

    @Volatile
    private var cachedAccessToken: String? = null

    fun getCachedToken(): String? = cachedAccessToken

    private val accessTokenKey = stringPreferencesKey("access_token")
    private val refreshTokenKey = stringPreferencesKey("refresh_token")

    val accessTokenFlow: Flow<String?> = context.appDataStore.data.map { it[accessTokenKey] }
    val refreshTokenFlow: Flow<String?> = context.appDataStore.data.map { it[refreshTokenKey] }

    suspend fun saveAccessToken(token: String) {
        cachedAccessToken = token
        context.appDataStore.edit { it[accessTokenKey] = token }
    }

    suspend fun saveRefreshToken(token: String) {
        context.appDataStore.edit { it[refreshTokenKey] = token }
    }

    suspend fun clearTokens() {
        cachedAccessToken = null
        context.appDataStore.edit { prefs ->
            prefs.remove(accessTokenKey)
            prefs.remove(refreshTokenKey)
        }
    }

    suspend fun loadFromDataStore() {
        cachedAccessToken = context.appDataStore.data.firstOrNull()?.get(accessTokenKey)
    }

    companion object {
        @Volatile
        private var instance: TokenManager? = null

        fun getInstance(context: Context): TokenManager =
            instance ?: synchronized(this) {
                instance ?: TokenManager(context.applicationContext).also { instance = it }
            }
    }
}
