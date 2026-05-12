package com.demo.creditlimit.network.repository

import com.demo.creditlimit.network.api.ApiService
import com.demo.creditlimit.network.model.request2.BasicUpdateReqV2
import com.demo.creditlimit.network.model.request2.Emerge
import com.demo.creditlimit.network.model.request2.SupplementResp
import com.demo.creditlimit.network.model.request2.SupplementUpdateReqV2
import com.demo.creditlimit.network.model.request2.UserProfileResp
import com.demo.creditlimit.network.state.NetworkResult
import kotlinx.coroutines.flow.Flow

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
}
