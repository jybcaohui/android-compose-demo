package com.demo.creditlimit.network.manager

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class GaidManager private constructor(private val context: Context) {

    private val KEY_GAID = stringPreferencesKey("gaid")

    @Volatile
    private var cachedGaid: String? = null

    fun getGaid(): String? = cachedGaid

    suspend fun loadFromDataStore() {
        val stored = context.appDataStore.data
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
        context.appDataStore.edit { it[KEY_GAID] = gaid }
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
