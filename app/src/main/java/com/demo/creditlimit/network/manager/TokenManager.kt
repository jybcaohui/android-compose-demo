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
    @Volatile
    private var cachedPhone: String? = null

    fun getCachedToken(): String? = cachedAccessToken
    fun getCachedPhone(): String? = cachedPhone

    private val accessTokenKey = stringPreferencesKey("access_token")
    private val refreshTokenKey = stringPreferencesKey("refresh_token")
    private val phoneKey = stringPreferencesKey("user_phone")

    val accessTokenFlow: Flow<String?> = context.appDataStore.data.map { it[accessTokenKey] }
    val refreshTokenFlow: Flow<String?> = context.appDataStore.data.map { it[refreshTokenKey] }

    suspend fun saveAccessToken(token: String) {
        cachedAccessToken = token
        context.appDataStore.edit { it[accessTokenKey] = token }
    }

    suspend fun saveRefreshToken(token: String) {
        context.appDataStore.edit { it[refreshTokenKey] = token }
    }

    suspend fun savePhone(phone: String) {
        cachedPhone = phone
        context.appDataStore.edit { it[phoneKey] = phone }
    }

    suspend fun clearTokens() {
        cachedAccessToken = null
        cachedPhone = null
        context.appDataStore.edit { prefs ->
            prefs.remove(accessTokenKey)
            prefs.remove(refreshTokenKey)
            prefs.remove(phoneKey)
        }
    }

    suspend fun loadFromDataStore() {
        val prefs = context.appDataStore.data.firstOrNull()
        cachedAccessToken = prefs?.get(accessTokenKey)
        cachedPhone = prefs?.get(phoneKey)
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
