package com.demo.creditlimit.network.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.demo.creditlimit.network.api.ApiService
import com.demo.creditlimit.network.manager.GaidManager
import com.demo.creditlimit.network.manager.RetrofitManager
import com.demo.creditlimit.network.manager.RuntimeManager
import com.demo.creditlimit.network.manager.TokenManager
import com.demo.creditlimit.network.repository.AuthRepository
import com.demo.creditlimit.network.repository.ConfigRepository
import com.demo.creditlimit.network.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppContainer(context: Context) {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val tokenManager: TokenManager = TokenManager.getInstance(context)
    val gaidManager: GaidManager = GaidManager.getInstance(context)

    private val apiService: ApiService = RetrofitManager.getApiService()

    val authRepository: AuthRepository = AuthRepository(apiService, tokenManager)
    val userRepository: UserRepository = UserRepository(apiService)
    val configRepository: ConfigRepository = ConfigRepository(apiService, context)
    val runtimeManager: RuntimeManager = RuntimeManager(context, apiService, gaidManager)

    val appName: String = "SweetMoney"

    /**
     * null = DataStore not yet loaded
     * true  = token exists (logged in)
     * false = no token (not logged in)
     */
    private val _initialized = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean?> = combine(
        _initialized,
        tokenManager.accessTokenFlow
    ) { initialized, token ->
        if (!initialized) null else !token.isNullOrEmpty()
    }.stateIn(appScope, SharingStarted.Eagerly, null)

    init {
        appScope.launch {
            tokenManager.loadFromDataStore()
            gaidManager.loadFromDataStore()
            _initialized.value = true
            gaidManager.fetchAndSave()
        }
    }

    fun <VM : ViewModel> viewModelFactory(creator: () -> VM): ViewModelProvider.Factory =
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = creator() as T
        }
}
