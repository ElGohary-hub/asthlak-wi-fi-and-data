package com.example.datatrack.utils

import android.app.AppOpsManager
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.os.Build
import android.os.Process
import java.util.Calendar

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

    fun getAppsDataUsage(context: Context): List<RealAppUsage> {
        val networkStatsManager = context.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager
        val packageManager = context.packageManager

        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startTime = calendar.timeInMillis

        val usageMap = mutableMapOf<Int, Pair<Long, Long>>() 

        try {
            val wifiStats = networkStatsManager.querySummary(ConnectivityManager.TYPE_WIFI, null, startTime, endTime)
            val bucket = NetworkStats.Bucket()
            while (wifiStats.hasNextBucket()) {
                // التعديل هنا: getNextBucket
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
                // التعديل هنا: getNextBucket
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

        for ((uid, bytesPair) in usageMap) {
            val (wifi, mobile) = bytesPair
            if (wifi == 0L && mobile == 0L) continue 

            val packages = packageManager.getPackagesForUid(uid)
            if (!packages.isNullOrEmpty()) {
                val packageName = packages[0]
                try {
                    val appInfo = packageManager.getApplicationInfo(packageName, 0)
                    val appName = packageManager.getApplicationLabel(appInfo).toString()
                    val icon = packageManager.getApplicationIcon(appInfo)

                    appUsageList.add(
                        RealAppUsage(
                            name = appName,
                            packageName = packageName,
                            iconDrawable = icon,
                            wifi = wifi,
                            mobile = mobile
                        )
                    )
                } catch (e: PackageManager.NameNotFoundException) {
                }
            }
        }

        return appUsageList.sortedByDescending { it.wifi + it.mobile }
    }
}
