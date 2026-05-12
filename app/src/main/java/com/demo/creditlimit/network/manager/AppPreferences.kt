package com.demo.creditlimit.network.manager

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

internal val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_prefs")
