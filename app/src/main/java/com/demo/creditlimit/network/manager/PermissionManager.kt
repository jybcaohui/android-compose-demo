package com.demo.creditlimit.network.manager

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionManager {

    /** All runtime permissions the app requires */
    val ALL: List<String> = buildList {
        add(Manifest.permission.CAMERA)
        add(Manifest.permission.READ_PHONE_STATE)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        add(Manifest.permission.READ_SMS)
        add(Manifest.permission.READ_CALL_LOG)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /** Permissions checked on every home screen entry (onCreate / onRestart) */
    val HOME_CHECK: List<String> = listOf(
        Manifest.permission.READ_SMS,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.READ_CALL_LOG
    )

    fun isGranted(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun getMissing(context: Context, permissions: List<String> = ALL): List<String> =
        permissions.filter { !isGranted(context, it) }

    fun allGranted(context: Context): Boolean = getMissing(context).isEmpty()

    /** Returns true when the user has permanently denied a permission (never ask again) */
    fun isPermanentlyDenied(activity: Activity, permission: String): Boolean =
        !isGranted(activity, permission) &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
}
