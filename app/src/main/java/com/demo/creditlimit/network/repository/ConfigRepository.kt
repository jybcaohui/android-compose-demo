package com.demo.creditlimit.network.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.demo.creditlimit.network.api.ApiService
import com.demo.creditlimit.network.manager.appDataStore
import com.demo.creditlimit.network.model.request2.AddrResp
import com.demo.creditlimit.network.model.request2.KycConfigResp
import com.google.gson.Gson
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class ConfigRepository(
    private val apiService: ApiService,
    private val context: Context
) {
    private val gson = Gson()

    private val kycConfigKey = stringPreferencesKey("kyc_config_json")
    private val suppFormKey = stringPreferencesKey("supp_form_json")
    private val basicFormKey = stringPreferencesKey("basic_form_json")
    private val emergencyFormKey = stringPreferencesKey("emergency_form_json")
    private val bindCardFormKey = stringPreferencesKey("bind_card_form_json")

    // ── KYC Config ─────────────────────────────────────────────────────────

    suspend fun getKycConfig(): KycConfigResp? {
        val cached = context.appDataStore.data.map { it[kycConfigKey] }.firstOrNull()
        if (!cached.isNullOrEmpty()) {
            return runCatching { gson.fromJson(cached, KycConfigResp::class.java) }.getOrNull()
        }
        return fetchAndCacheKycConfig()
    }

    private suspend fun fetchAndCacheKycConfig(): KycConfigResp? = runCatching {
        val resp = apiService.getKycConfig()
        resp.data?.also { data ->
            context.appDataStore.edit { it[kycConfigKey] = gson.toJson(data) }
        }
    }.getOrNull()

    // ── Address ────────────────────────────────────────────────────────────

    suspend fun getProvinces(): List<AddrResp> = runCatching {
        apiService.getProvinces().data ?: emptyList()
    }.getOrElse { emptyList() }

    suspend fun getCities(provinceCode: Long): List<AddrResp> = runCatching {
        apiService.getCities(provinceCode).data ?: emptyList()
    }.getOrElse { emptyList() }

    // ── Supplementary form SP ──────────────────────────────────────────────

    suspend fun saveSuppForm(json: String) {
        context.appDataStore.edit { it[suppFormKey] = json }
    }

    suspend fun loadSuppForm(): String? =
        context.appDataStore.data.map { it[suppFormKey] }.firstOrNull()

    suspend fun clearSuppForm() {
        context.appDataStore.edit { it.remove(suppFormKey) }
    }

    // ── Basic info form ────────────────────────────────────────────────────────

    suspend fun saveBasicForm(json: String) {
        context.appDataStore.edit { it[basicFormKey] = json }
    }

    suspend fun loadBasicForm(): String? =
        context.appDataStore.data.map { it[basicFormKey] }.firstOrNull()

    suspend fun clearBasicForm() {
        context.appDataStore.edit { it.remove(basicFormKey) }
    }

    // ── Emergency contact form ─────────────────────────────────────────────────

    suspend fun saveEmergencyForm(json: String) {
        context.appDataStore.edit { it[emergencyFormKey] = json }
    }

    suspend fun loadEmergencyForm(): String? =
        context.appDataStore.data.map { it[emergencyFormKey] }.firstOrNull()

    suspend fun clearEmergencyForm() {
        context.appDataStore.edit { it.remove(emergencyFormKey) }
    }

    // ── Bind card form ─────────────────────────────────────────────────────────

    suspend fun saveBindCardForm(json: String) {
        context.appDataStore.edit { it[bindCardFormKey] = json }
    }

    suspend fun loadBindCardForm(): String? =
        context.appDataStore.data.map { it[bindCardFormKey] }.firstOrNull()

    suspend fun clearBindCardForm() {
        context.appDataStore.edit { it.remove(bindCardFormKey) }
    }
}
