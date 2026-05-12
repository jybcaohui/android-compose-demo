package com.demo.creditlimit.network.manager

/**
 * Manages the active server environment and its base URL.
 *
 * Call [init] once in Application.onCreate() to set the default
 * based on the build type, then call [switchTo] to change at runtime
 * (e.g. from an in-app developer menu).
 *
 * After switching environments, call [RetrofitManager.rebuild] so that
 * the new base URL takes effect.
 */
object EnvironmentManager {

    enum class Environment(val baseUrl: String, val isDebug: Boolean) {
        DEV("http://api.rp735.xyz/leopard_in/", true),
        TEST("http://api.rp735.xyz/leopard_in/", true),
        PROD("http://api.rp735.xyz/leopard_in/", false)
    }

    @Volatile
    private var current: Environment = Environment.DEV

    fun init(isDebugBuild: Boolean) {
        current = if (isDebugBuild) Environment.DEV else Environment.PROD
    }

    fun switchTo(env: Environment) {
        current = env
    }

    fun getBaseUrl(): String = current.baseUrl

    fun isDebug(): Boolean = current.isDebug

    fun current(): Environment = current
}
