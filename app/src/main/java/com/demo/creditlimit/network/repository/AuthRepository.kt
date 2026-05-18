package com.demo.creditlimit.network.repository

import com.demo.creditlimit.network.api.ApiService
import com.demo.creditlimit.network.manager.TokenManager
import com.demo.creditlimit.network.model.request2.LoginReq
import com.demo.creditlimit.network.model.request2.LoginResp
import com.demo.creditlimit.network.model.request2.VCodeReq
import com.demo.creditlimit.network.state.NetworkResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach

class AuthRepository(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) : BaseRepository() {

    fun sendVcode(phone: String, appName: String): Flow<NetworkResult<Void>> =
        executeRequest {
            apiService.sendVcode(VCodeReq().apply {
                this.phone = phone
                this.app = appName
                this.intlCode = "91"
                this.vcodeMethod = 0
            })
        }

    fun login(
        phone: String,
        vcode: String,
        deviceId: String,
        appName: String
    ): Flow<NetworkResult<LoginResp>> =
        executeRequest {
            apiService.login(LoginReq().apply {
                this.app = appName
                this.phone = phone
                this.vcode = vcode
                this.deviceId = deviceId
            })
        }.onEach { result ->
            if (result is NetworkResult.Success) {
                tokenManager.saveAccessToken(result.data.token)
                tokenManager.savePhone(phone)
            }
        }

    fun logout(): Flow<NetworkResult<Unit>> = flow {
        emit(NetworkResult.Loading)
        tokenManager.clearTokens()
        emit(NetworkResult.Success(Unit))
    }
}
