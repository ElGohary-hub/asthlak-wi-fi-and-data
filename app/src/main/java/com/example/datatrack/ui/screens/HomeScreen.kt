package com.example.datatrack.ui.screens

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

    var capsuleText by remember { mutableStateOf("استهلاك الإنترنت") }
    
    var showDatePickerDialog by remember { mutableStateOf(false) }

    val isDark = isSystemInDarkTheme()
    val capsuleBgColor = if (isDark) Color(0xFF212121) else Color.White
    val capsuleBorderColor = if (isDark) Color.White.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.26f)
    val capsuleShadow = if (isDark) 0.dp else 4.dp
    val capsuleShape = RoundedCornerShape(30.dp)

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

    // استدعاء التقويم المصغر الجديد
    if (showDatePickerDialog) {
        CompactDateRangePicker(
            onDismiss = { showDatePickerDialog = false },
            onDateSelected = { start, end ->
                currentStartTime = start
                currentEndTime = end
            }
        )
    }

    Scaffold(
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding() 
            ) {
                if (selectedFilter == 2) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clickable { 
                                showDatePickerDialog = true
                            },
                        shape = capsuleShape,
                        color = capsuleBgColor,
                        border = BorderStroke(1.dp, capsuleBorderColor),
                        shadowElevation = capsuleShadow
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "من ${dateFormatter.format(currentStartTime)} إلى ${dateFormatter.format(currentEndTime)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color.Black
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.Edit, contentDescription = "تعديل", modifier = Modifier.size(16.dp), tint = Color.Gray)
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = capsuleShape,
                    color = capsuleBgColor,
                    border = BorderStroke(1.dp, capsuleBorderColor),
                    shadowElevation = capsuleShadow
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            filterOptions.forEachIndexed { index, text ->
                                val isSelected = selectedFilter == index
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    modifier = Modifier.clickable {
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
                                                showDatePickerDialog = true
                                            }
                                        }
                                    }
                                ) {
                                    Text(
                                        text = text,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else if (isDark) Color.White else Color.Black
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            modifier = Modifier.clickable { 
                                if (selectedFilter != 2) currentEndTime = System.currentTimeMillis()
                                refreshTrigger++ 
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh, 
                                contentDescription = "Refresh", 
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(10.dp).size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .statusBarsPadding() 
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                AnimatedStarCapsule(
                    text = capsuleText,
                    onClick = {
                        capsuleText = if (capsuleText == "استهلاك الإنترنت") "DataTrack Pro ✨" else "استهلاك الإنترنت"
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = capsuleShape,
                color = capsuleBgColor,
                border = BorderStroke(1.dp, capsuleBorderColor),
                shadowElevation = capsuleShadow
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEachIndexed { index, title ->
                        val isSelected = selectedTabIndex == index
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedTabIndex = index },
                            shape = RoundedCornerShape(26.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (index == 0) Icons.Default.Wifi else Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else Color.Gray
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))

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

// التقويم المتسلسل المصغر (أصغر وأشيك بكتير)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactDateRangePicker(
    onDismiss: () -> Unit,
    onDateSelected: (Long, Long) -> Unit
) {
    // خطوة 1 = تاريخ البدء، خطوة 2 = تاريخ الانتهاء
    var step by remember { mutableIntStateOf(1) } 
    var tempStart by remember { mutableLongStateOf(0L) }

    if (step == 1) {
        val startState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = {
                        if (startState.selectedDateMillis != null) {
                            tempStart = startState.selectedDateMillis!!
                            step = 2 // الانتقال لاختيار النهاية
                        }
                    }
                ) { Text("التالي", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("إلغاء", color = Color.Gray) }
            }
        ) {
            DatePicker(
                state = startState,
                title = { Text("اختر تاريخ البدء", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold) },
                showModeToggle = false
            )
        }
    } else if (step == 2) {
        val endState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = {
                        if (endState.selectedDateMillis != null) {
                            // إضافة وقت لنهاية اليوم لضمان حساب اليوم كاملاً
                            onDateSelected(tempStart, endState.selectedDateMillis!! + 86399999L)
                            onDismiss()
                        }
                    }
                ) { Text("تأكيد", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { step = 1 }) { Text("رجوع", color = Color.Gray) }
            }
        ) {
            DatePicker(
                state = endState,
                title = { Text("اختر تاريخ الانتهاء", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold) },
                showModeToggle = false
            )
        }
    }
}

@Composable
fun RealAppCard(app: RealAppUsage, isWifiTab: Boolean) {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) Color(0xFF212121) else Color.White
    val borderColor = if (isDark) Color.White.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.26f)
    val shadowElevation = if (isDark) 0.dp else 4.dp

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp), 
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = shadowElevation
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
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(30.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = shadowElevation
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
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
