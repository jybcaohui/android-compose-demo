package com.demo.creditlimit

import android.app.Application
import com.demo.creditlimit.network.di.AppContainer
import com.demo.creditlimit.network.manager.EnvironmentManager
import com.demo.creditlimit.network.manager.RetrofitManager

class CreditLimitApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()

        // 1. Set environment based on build type (must be first)
        EnvironmentManager.init(isDebugBuild = BuildConfig.DEBUG)

        // 2. Build the Retrofit client with correct base URL
        RetrofitManager.init(this)

        // 3. Wire up all dependencies
        container = AppContainer(this)
    }
}
