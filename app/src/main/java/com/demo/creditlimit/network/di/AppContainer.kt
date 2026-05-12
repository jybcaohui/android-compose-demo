package com.demo.creditlimit.network.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.demo.creditlimit.network.api.ApiService
import com.demo.creditlimit.network.manager.GaidManager
import com.demo.creditlimit.network.manager.RetrofitManager
import com.demo.creditlimit.network.manager.TokenManager
import com.demo.creditlimit.network.repository.AuthRepository
import com.demo.creditlimit.network.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Manual DI container — created once in [CreditLimitApplication].
 *
 * **Replacing with Hilt:** annotate each property with @Provides / @Singleton
 * inside a @Module and delete this file.  No other changes needed in the
 * network layer itself.
 */
class AppContainer(context: Context) {

    /** Application-scoped coroutine scope for one-shot startup work. */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val tokenManager: TokenManager = TokenManager.getInstance(context)
    val gaidManager: GaidManager = GaidManager.getInstance(context)

    private val apiService: ApiService = RetrofitManager.getApiService()

    val authRepository: AuthRepository = AuthRepository(apiService, tokenManager)
    val userRepository: UserRepository = UserRepository(apiService)

    val appName: String = "creditlimit"

    init {
        appScope.launch { tokenManager.loadFromDataStore() }
        appScope.launch {
            gaidManager.loadFromDataStore()
            gaidManager.fetchAndSave()
        }
    }

    /** Generic ViewModel factory — avoids boilerplate per-ViewModel factories. */
    fun <VM : ViewModel> viewModelFactory(creator: () -> VM): ViewModelProvider.Factory =
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = creator() as T
        }
}
