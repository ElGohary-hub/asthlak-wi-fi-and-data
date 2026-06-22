package com.example.datatrack.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext

val WifiColor = Color(0xFF2196F3)
val DataColor = Color(0xFF4CAF50)
val WarningColor = Color(0xFFF44336)

private val DarkColorScheme = darkColorScheme(primary = WifiColor, secondary = DataColor, error = WarningColor)
private val LightColorScheme = lightColorScheme(primary = WifiColor, secondary = DataColor, error = WarningColor)

@Composable
fun DataTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}

data class AppUsage(val name: String, val icon: ImageVector, val wifi: Long, val mobile: Long, val lastUsed: String) {
    val total: Long get() = wifi + mobile
}

fun formatBytes(bytes: Long): String {
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format("%.1f MB", mb)
    return String.format("%.1f GB", mb / 1024.0)
}

val MockApps = listOf(
    AppUsage("يوتيوب", Icons.Default.PlayArrow, 1800000000L, 600000000L, "منذ ساعتين"),
    AppUsage("فيسبوك", Icons.Default.ThumbUp, 500000000L, 1200000000L, "منذ 15 دقيقة")
)
