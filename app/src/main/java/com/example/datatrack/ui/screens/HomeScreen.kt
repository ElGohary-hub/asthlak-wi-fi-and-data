package com.example.datatrack.ui.screens

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.ImageView
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.datatrack.ui.theme.*
import com.example.datatrack.utils.NetworkStatsHelper
import com.example.datatrack.utils.RealAppUsage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

fun getTodayStart(): Long {
    return Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
    }.timeInMillis
}

fun getMonthStart(): Long {
    return Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
    }.timeInMillis
}

fun showDateRangePicker(context: Context, onRangeSelected: (Long, Long) -> Unit) {
    val currentCal = Calendar.getInstance()
    DatePickerDialog(context, { _, startYear, startMonth, startDay ->
        val startCal = Calendar.getInstance().apply {
            set(startYear, startMonth, startDay, 0, 0, 0)
        }
        DatePickerDialog(context, { _, endYear, endMonth, endDay ->
            val endCal = Calendar.getInstance().apply {
                set(endYear, endMonth, endDay, 23, 59, 59)
            }
            onRangeSelected(startCal.timeInMillis, endCal.timeInMillis)
        }, startYear, startMonth, startDay).show()
    }, currentCal.get(Calendar.YEAR), currentCal.get(Calendar.MONTH), currentCal.get(Calendar.DAY_OF_MONTH)).show()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var hasPermission by remember { mutableStateOf(NetworkStatsHelper.hasUsageStatsPermission(context)) }
    var appsList by remember { mutableStateOf(listOf<RealAppUsage>()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("الواي فاي", "بيانات الهاتف")

    val filterOptions = listOf("اليوم", "الشهر", "مخصص")
    var selectedFilter by remember { mutableIntStateOf(0) }
    var currentStartTime by remember { mutableLongStateOf(getTodayStart()) }
    var currentEndTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // المتغير الخاص بالكبسولة التفاعلية
    var capsuleText by remember { mutableStateOf("تتبع استهلاك ذكي") }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = NetworkStatsHelper.hasUsageStatsPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (!hasPermission) {
        PermissionScreen(onOpenSettings = {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            context.startActivity(intent)
        })
        return
    }

    LaunchedEffect(hasPermission, refreshTrigger, currentStartTime, currentEndTime) {
        if (hasPermission) {
            isLoading = true
            appsList = withContext(Dispatchers.IO) {
                NetworkStatsHelper.getAppsDataUsage(context, currentStartTime, currentEndTime)
            }
            isLoading = false
        }
    }

    val displayList = remember(appsList, selectedTabIndex) {
        if (selectedTabIndex == 0) appsList.filter { it.wifi > 0 }.sortedByDescending { it.wifi }
        else appsList.filter { it.mobile > 0 }.sortedByDescending { it.mobile }
    }

    val dateFormatter = SimpleDateFormat("dd MMM", Locale("ar"))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("استهلاك البيانات", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { 
                        if (selectedFilter != 2) currentEndTime = System.currentTimeMillis()
                        refreshTrigger++ 
                    }) { 
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh") 
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            
            // الكبسولة التفاعلية تم إضافتها هنا في منتصف الشاشة من الأعلى
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                AnimatedStarCapsule(
                    text = capsuleText,
                    onClick = {
                        // النص هيتغير بأنيميشن لما تدوس عليه
                        capsuleText = if (capsuleText == "تتبع استهلاك ذكي") "DataTrack Pro ✨" else "تتبع استهلاك ذكي"
                    }
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(1.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        filterOptions.forEachIndexed { index, text ->
                            FilterChip(
                                selected = selectedFilter == index,
                                onClick = { 
                                    selectedFilter = index
                                    when(index) {
                                        0 -> { 
                                            currentStartTime = getTodayStart()
                                            currentEndTime = System.currentTimeMillis()
                                        }
                                        1 -> { 
                                            currentStartTime = getMonthStart()
                                            currentEndTime = System.currentTimeMillis()
                                        }
                                        2 -> { 
                                            showDateRangePicker(context) { start, end ->
                                                currentStartTime = start
                                                currentEndTime = end
                                            }
                                        }
                                    }
                                },
                                label = { Text(text) }
                            )
                        }
                    }
                    
                    if (selectedFilter == 2) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "من ${dateFormatter.format(currentStartTime)} إلى ${dateFormatter.format(currentEndTime)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(onClick = { 
                                showDateRangePicker(context) { start, end ->
                                    currentStartTime = start
                                    currentEndTime = end
                                }
                            }) {
                                Text("تغيير", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    if (selectedTabIndex < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            height = 3.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { 
                            Text(
                                text = title, 
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTabIndex == index) MaterialTheme.colorScheme.primary else Color.Gray
                            ) 
                        },
                        icon = {
                            Icon(
                                imageVector = if (index == 0) Icons.Default.Wifi else Icons.Default.Phone,
                                contentDescription = null
                            )
                        }
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (displayList.isEmpty()) {
                    Text(
                        text = "لا يوجد استهلاك مسجل في هذه الفترة", 
                        modifier = Modifier.align(Alignment.Center), 
                        color = Color.Gray
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(displayList) { app -> 
                            RealAppCard(app = app, isWifiTab = selectedTabIndex == 0) 
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RealAppCard(app: RealAppUsage, isWifiTab: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ) {
                AndroidView(
                    factory = { ctx ->
                        ImageView(ctx).apply { scaleType = ImageView.ScaleType.FIT_CENTER }
                    },
                    update = { imageView -> imageView.setImageDrawable(app.iconDrawable) },
                    modifier = Modifier.padding(8.dp).fillMaxSize()
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(text = app.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
                Text(text = app.packageName, color = Color.Gray, fontSize = 10.sp, maxLines = 1)
            }
            
            Column(horizontalAlignment = Alignment.End) {
                val currentUsage = if (isWifiTab) app.wifi else app.mobile
                val usageColor = if (isWifiTab) WifiColor else DataColor
                
                Text(text = formatBytes(currentUsage), color = usageColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(text = "الإجمالي: ${formatBytes(app.total)}", color = Color.Gray, fontSize = 11.sp)
            }
        }
    }
}

// دالة الكبسولة التفاعلية اللي صممناها مع بعض
@Composable
fun AnimatedStarCapsule(
    text: String,
    onClick: () -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) Color(0xFF212121) else Color.White
    val borderColor = if (isDark) Color.White.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.26f)
    val shadowElevation = if (isDark) 0.dp else 4.dp

    Surface(
        modifier = Modifier
            .padding(8.dp)
            .clickable { onClick() },
        shape = CircleShape,
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = shadowElevation
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Star Icon",
                tint = Color(0xFFFFC107), 
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Crossfade(
                targetState = text, 
                animationSpec = tween(durationMillis = 500), 
                label = "TextFadeAnimation"
            ) { animatedText ->
                Text(
                    text = animatedText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color.Black
                )
            }
        }
    }
}
