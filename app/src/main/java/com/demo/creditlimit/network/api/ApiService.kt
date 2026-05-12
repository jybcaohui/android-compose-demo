package com.demo.creditlimit.network.api

import com.demo.creditlimit.network.model.request2.BasicUpdateReqV2
import com.demo.creditlimit.network.model.request2.Emerge
import com.demo.creditlimit.network.model.request2.LoginReq
import com.demo.creditlimit.network.model.request2.LoginResp
import com.demo.creditlimit.network.model.request2.SupplementUpdateReqV2
import com.demo.creditlimit.network.model.request2.UserProfileResp
import com.demo.creditlimit.network.model.request2.VCodeReq
import com.demo.creditlimit.network.response.BaseResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST

interface ApiService {

    // ── Auth (public, no token) ────────────────────────────────────────────

    @Headers("No-Auth: true")
    @POST("v1/login/vcode")
    suspend fun sendVcode(
        @Body request: VCodeReq
    ): BaseResponse<Void>

    @Headers("No-Auth: true")
    @POST("v1/login")
    suspend fun login(
        @Body request: LoginReq
    ): BaseResponse<LoginResp>

    // ── User profile ───────────────────────────────────────────────────────

    @GET("v1/user/profile")
    suspend fun getUserProfile(): BaseResponse<UserProfileResp>

    // ── KYC ───────────────────────────────────────────────────────────────

    @POST("v1/user/basic/v2")
    suspend fun submitBasicInfo(
        @Body request: BasicUpdateReqV2
    ): BaseResponse<Void>

    @GET("v1/user/contacts/emergency")
    suspend fun getEmergencyContacts(): BaseResponse<List<Emerge>>

    @POST("v1/user/contacts/emergency")
    suspend fun submitEmergencyContacts(
        @Body contacts: List<Emerge>
    ): BaseResponse<Void>

    @POST("v1/user/supp/v2")
    suspend fun submitSuppInfo(
        @Body request: SupplementUpdateReqV2
    ): BaseResponse<Void>

    // ── Credit line ────────────────────────────────────────────────────────

    @POST("v1/user/creditLine")
    suspend fun submitCreditLine(
        @Body body: Map<String, @JvmSuppressWildcards Any> = emptyMap()
    ): BaseResponse<Void>
}
