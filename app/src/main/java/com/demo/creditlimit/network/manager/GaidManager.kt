package com.demo.creditlimit.network.manager

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private val Context.gaidDataStore: DataStore<Preferences> by preferencesDataStore(name = "gaid_prefs")

class GaidManager private constructor(private val context: Context) {

    private val KEY_GAID = stringPreferencesKey("gaid")

    @Volatile
    private var cachedGaid: String? = null

    fun getGaid(): String? = cachedGaid

    suspend fun loadFromDataStore() {
        val stored = context.gaidDataStore.data
            .map { it[KEY_GAID] }
            .firstOrNull()
        if (stored != null) cachedGaid = stored
    }

    suspend fun fetchAndSave() {
        val gaid = withContext(Dispatchers.IO) {
            runCatching {
                val info = AdvertisingIdClient.getAdvertisingIdInfo(context)
                if (info.isLimitAdTrackingEnabled) null else info.id
            }.getOrNull()
        } ?: return

        cachedGaid = gaid
        context.gaidDataStore.edit { it[KEY_GAID] = gaid }
    }

    companion object {
        @Volatile
        private var instance: GaidManager? = null

        fun getInstance(context: Context): GaidManager =
            instance ?: synchronized(this) {
                instance ?: GaidManager(context.applicationContext).also { instance = it }
            }
    }
}
