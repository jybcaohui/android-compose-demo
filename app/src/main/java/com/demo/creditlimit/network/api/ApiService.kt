package com.demo.creditlimit.network.api

import com.demo.creditlimit.network.model.request2.AddrResp
import com.demo.creditlimit.network.model.request2.BasicResp
import com.demo.creditlimit.network.model.request2.BasicUpdateReqV2
import com.demo.creditlimit.network.model.request2.BindBankCardReq
import com.demo.creditlimit.network.model.request2.Emerge
import com.demo.creditlimit.network.model.request2.IfscInfoResp
import com.demo.creditlimit.network.model.request2.KycConfigResp
import com.demo.creditlimit.network.model.request2.LoginReq
import com.demo.creditlimit.network.model.request2.LoginResp
import com.demo.creditlimit.network.model.request2.SupplementResp
import com.demo.creditlimit.network.model.request2.SupplementUpdateReqV2
import com.demo.creditlimit.network.model.request2.UserProfileResp
import com.demo.creditlimit.network.model.request2.VCodeReq
import com.demo.creditlimit.network.response.BaseResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

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

    @GET("v1/user/basic")
    suspend fun getBasicInfo(): BaseResponse<BasicResp>

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

    // ── Config / Address ──────────────────────────────────────────────────

    @GET("v1/config/kyc")
    suspend fun getKycConfig(): BaseResponse<KycConfigResp>

    @GET("v1/addr/provinces")
    suspend fun getProvinces(): BaseResponse<List<AddrResp>>

    @GET("v1/addr/cities")
    suspend fun getCities(@Query("pc") provinceCode: Long): BaseResponse<List<AddrResp>>

    @GET("v1/user/supp")
    suspend fun getSuppInfo(): BaseResponse<SupplementResp>

    // ── Bank card ─────────────────────────────────────────────────────────────

    @GET("v1/user/ifsc/info")
    suspend fun getIfscInfo(@Query("ifscCode") code: String): BaseResponse<IfscInfoResp>

    @POST("v1/user/bindBankCard")
    suspend fun bindBankCard(@Body request: BindBankCardReq): BaseResponse<Void>

    // ── Credit line ────────────────────────────────────────────────────────

    @POST("v1/user/creditLine")
    suspend fun submitCreditLine(
        @Body body: Map<String, @JvmSuppressWildcards Any> = emptyMap()
    ): BaseResponse<Void>
}
