package com.example.datatrack.utils

import android.app.AppOpsManager
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.os.Build
import android.os.Process

data class RealAppUsage(
    val name: String,
    val packageName: String,
    val iconDrawable: Drawable,
    val wifi: Long,
    val mobile: Long
) {
    val total: Long get() = wifi + mobile
}

object NetworkStatsHelper {

    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    // التعديل هنا: الدالة بقت بتاخد وقت البداية والنهاية من بره
    fun getAppsDataUsage(context: Context, startTime: Long, endTime: Long): List<RealAppUsage> {
        val networkStatsManager = context.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager
        val packageManager = context.packageManager

        val usageMap = mutableMapOf<Int, Pair<Long, Long>>() 

        try {
            val wifiStats = networkStatsManager.querySummary(ConnectivityManager.TYPE_WIFI, null, startTime, endTime)
            val bucket = NetworkStats.Bucket()
            while (wifiStats.hasNextBucket()) {
                wifiStats.getNextBucket(bucket)
                val uid = bucket.uid
                val bytes = bucket.rxBytes + bucket.txBytes
                val existing = usageMap[uid] ?: Pair(0L, 0L)
                usageMap[uid] = Pair(existing.first + bytes, existing.second)
            }
            wifiStats.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val mobileStats = networkStatsManager.querySummary(ConnectivityManager.TYPE_MOBILE, null, startTime, endTime)
            val bucket = NetworkStats.Bucket()
            while (mobileStats.hasNextBucket()) {
                mobileStats.getNextBucket(bucket)
                val uid = bucket.uid
                val bytes = bucket.rxBytes + bucket.txBytes
                val existing = usageMap[uid] ?: Pair(0L, 0L)
                usageMap[uid] = Pair(existing.first, existing.second + bytes)
            }
            mobileStats.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val appUsageList = mutableListOf<RealAppUsage>()
        var unknownWifi = 0L
        var unknownMobile = 0L

        val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        val appInfoMap = mutableMapOf<Int, ApplicationInfo>()
        for (info in installedApps) {
            appInfoMap[info.uid % 100000] = info 
        }

        for ((uid, bytesPair) in usageMap) {
            val (wifi, mobile) = bytesPair
            if (wifi == 0L && mobile == 0L) continue 

            val baseUid = uid % 100000
            val isClonedApp = uid >= 100000

            val appInfo = appInfoMap[baseUid]
            if (appInfo != null) {
                try {
                    var appName = packageManager.getApplicationLabel(appInfo).toString()
                    if (isClonedApp) {
                        appName += " (نسخة 2)" 
                    }
                    val icon = packageManager.getApplicationIcon(appInfo)
                    appUsageList.add(RealAppUsage(appName, appInfo.packageName, icon, wifi, mobile))
                } catch (e: Exception) {
                    unknownWifi += wifi
                    unknownMobile += mobile
                }
            } else {
                unknownWifi += wifi
                unknownMobile += mobile
            }
        }

        if (unknownWifi > 0 || unknownMobile > 0) {
            try {
                val defaultIcon = context.getDrawable(android.R.drawable.sym_def_app_icon)!!
                appUsageList.add(RealAppUsage("نظام أندرويد وخدمات مخفية", "android.system", defaultIcon, unknownWifi, unknownMobile))
            } catch (e: Exception) {}
        }

        return appUsageList.sortedByDescending { it.wifi + it.mobile }
    }
}
