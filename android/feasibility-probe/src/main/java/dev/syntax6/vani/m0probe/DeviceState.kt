package dev.syntax6.vani.m0probe

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

object DeviceState {
    data class OfflineSnapshot(
        val airplaneMode: Boolean,
        val validatedInternetAvailable: Boolean,
        val internetCapabilityDeclared: Boolean,
        val localRadios: List<String>,
    )

    data class DeviceSnapshot(
        val manufacturer: String,
        val model: String,
        val soc: String,
        val androidVersion: String,
        val ramMb: Long,
        val batteryPercent: Double?,
        val batteryTemperatureC: Double?,
    ) {
        fun shortLabel(): String =
            "$manufacturer $model | Android $androidVersion | ${ramMb}MB RAM"
    }

    fun offlineSnapshot(context: Context): OfflineSnapshot {
        val airplaneMode = Settings.Global.getInt(
            context.contentResolver,
            Settings.Global.AIRPLANE_MODE_ON,
            0,
        ) == 1

        val connectivity = context.getSystemService(ConnectivityManager::class.java)
        val capabilities = connectivity.activeNetwork?.let(connectivity::getNetworkCapabilities)

        val radios = buildList {
            if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) add("wifi")
            if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true) add("cellular")
            if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true) add("ethernet")
            if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) == true) add("bluetooth")
        }

        return OfflineSnapshot(
            airplaneMode = airplaneMode,
            validatedInternetAvailable = capabilities?.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_VALIDATED
            ) == true,
            internetCapabilityDeclared = capabilities?.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_INTERNET
            ) == true,
            localRadios = radios,
        )
    }

    fun deviceSnapshot(context: Context): DeviceSnapshot {
        val activityManager = context.getSystemService(ActivityManager::class.java)
        val memoryInfo = ActivityManager.MemoryInfo().also(activityManager::getMemoryInfo)
        val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPercent = if (level >= 0 && scale > 0) level * 100.0 / scale else null

        val tempTenthsC = battery?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
            ?: Int.MIN_VALUE
        val batteryTempC = tempTenthsC.takeIf { it != Int.MIN_VALUE }?.div(10.0)

        val soc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.SOC_MODEL.ifBlank { Build.HARDWARE }
        } else {
            Build.HARDWARE
        }

        return DeviceSnapshot(
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            soc = soc,
            androidVersion = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            ramMb = memoryInfo.totalMem / (1024L * 1024L),
            batteryPercent = batteryPercent,
            batteryTemperatureC = batteryTempC,
        )
    }

    fun deviceInfoString(context: Context): String = deviceSnapshot(context).shortLabel()
}

object LocalAddressProvider {
    fun ipv4Addresses(): List<String> = try {
        Collections.list(NetworkInterface.getNetworkInterfaces())
            .asSequence()
            .filter { it.isUp && !it.isLoopback }
            .flatMap { Collections.list(it.inetAddresses).asSequence() }
            .filterIsInstance<Inet4Address>()
            .filter { !it.isLoopbackAddress }
            .mapNotNull { it.hostAddress }
            .distinct()
            .sorted()
            .toList()
    } catch (_: Exception) {
        emptyList()
    }
}
