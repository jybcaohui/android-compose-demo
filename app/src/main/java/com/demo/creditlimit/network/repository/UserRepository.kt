package com.demo.creditlimit.network.repository

import com.demo.creditlimit.network.api.ApiService
import com.demo.creditlimit.network.model.request2.AadhaarFrontResp
import com.demo.creditlimit.network.model.request2.BasicResp
import com.demo.creditlimit.network.model.request2.BasicUpdateReqV2
import com.demo.creditlimit.network.model.request2.BindBankCardReq
import com.demo.creditlimit.network.model.request2.Emerge
import com.demo.creditlimit.network.model.request2.IdentityInfoReq
import com.demo.creditlimit.network.model.request2.IfscInfoResp
import com.demo.creditlimit.network.model.request2.OcrUpdateReq
import com.demo.creditlimit.network.model.request2.SupplementResp
import com.demo.creditlimit.network.model.request2.SupplementUpdateReqV2
import com.demo.creditlimit.network.model.request2.UserProfileResp
import com.demo.creditlimit.network.state.NetworkResult
import kotlinx.coroutines.flow.Flow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class UserRepository(
    private val apiService: ApiService
) : BaseRepository() {

    fun getUserProfile(): Flow<NetworkResult<UserProfileResp>> =
        executeRequest { apiService.getUserProfile() }

    fun submitBasicInfo(request: BasicUpdateReqV2): Flow<NetworkResult<Void>> =
        executeRequest { apiService.submitBasicInfo(request) }

    fun getEmergencyContacts(): Flow<NetworkResult<List<Emerge>>> =
        executeRequest { apiService.getEmergencyContacts() }

    fun submitEmergencyContacts(contacts: List<Emerge>): Flow<NetworkResult<Void>> =
        executeRequest { apiService.submitEmergencyContacts(contacts) }

    fun submitSuppInfo(request: SupplementUpdateReqV2): Flow<NetworkResult<Void>> =
        executeRequest { apiService.submitSuppInfo(request) }

    fun submitCreditLine(): Flow<NetworkResult<Void>> =
        executeRequest { apiService.submitCreditLine() }

    suspend fun getSuppInfo(): SupplementResp? = runCatching {
        apiService.getSuppInfo().data
    }.getOrNull()

    suspend fun getBasicInfo(): BasicResp? = runCatching {
        apiService.getBasicInfo().data
    }.getOrNull()

    suspend fun getIfscInfo(code: String): IfscInfoResp? = runCatching {
        apiService.getIfscInfo(code).data
    }.getOrNull()

    fun bindBankCard(request: BindBankCardReq): Flow<NetworkResult<Void>> =
        executeRequest { apiService.bindBankCard(request) }

    suspend fun uploadImage(bytes: ByteArray): String? = runCatching {
        val body = bytes.toRequestBody("application/octet-stream".toMediaType())
        apiService.uploadImage(body).data?.url
    }.getOrNull()

    suspend fun getAadhaarFrontHistory(): AadhaarFrontResp? = runCatching {
        apiService.getAadhaarFrontHistory().data
    }.getOrNull()

    suspend fun submitAadhaarFrontOcr(imageUrl: String): AadhaarFrontResp? = runCatching {
        val req = OcrUpdateReq().apply { url = imageUrl }
        apiService.submitAadhaarFrontOcr(req).data
    }.getOrNull()

    fun submitIdentity(request: IdentityInfoReq): Flow<NetworkResult<Void>> =
        executeRequest { apiService.submitIdentity(request) }
}
