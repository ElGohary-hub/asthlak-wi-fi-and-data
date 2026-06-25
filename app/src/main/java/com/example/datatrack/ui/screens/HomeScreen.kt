package com.example.datatrack.ui.screens

import android.content.Intent
import android.provider.Settings
import android.widget.ImageView
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var hasPermission by remember { mutableStateOf(NetworkStatsHelper.hasUsageStatsPermission(context)) }
    var appsList by remember { mutableStateOf(listOf<RealAppUsage>()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    
    // متغير للتحكم في الـ Tabs (0 للواي فاي، 1 للداتا)
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("الواي فاي", "بيانات الهاتف")

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

    LaunchedEffect(hasPermission, refreshTrigger) {
        if (hasPermission) {
            isLoading = true
            appsList = withContext(Dispatchers.IO) {
                NetworkStatsHelper.getAppsDataUsage(context)
            }
            isLoading = false
        }
    }

    // فلترة وترتيب القائمة بناءً على التاب المحدد
    val displayList = remember(appsList, selectedTabIndex) {
        if (selectedTabIndex == 0) {
            // عرض تطبيقات الواي فاي وترتيبها من الأكبر للأصغر
            appsList.filter { it.wifi > 0 }.sortedByDescending { it.wifi }
        } else {
            // عرض تطبيقات الداتا وترتيبها من الأكبر للأصغر
            appsList.filter { it.mobile > 0 }.sortedByDescending { it.mobile }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("استهلاك البيانات", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { refreshTrigger++ }) { 
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh") 
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            
            // تصميم الـ Tabs
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
                                // التعديل هنا: استخدمنا Phone بدل CellularAlt عشان ميعملش Error
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
                        text = "لا يوجد استهلاك مسجل في هذا القسم اليوم", 
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
                Text(
                    text = app.name, 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 15.sp, 
                    maxLines = 1
                )
                Text(
                    text = app.packageName, 
                    color = Color.Gray, 
                    fontSize = 10.sp, 
                    maxLines = 1
                )
            }
            
            // عرض الاستهلاك المخصص للتاب الحالي
            Column(horizontalAlignment = Alignment.End) {
                val currentUsage = if (isWifiTab) app.wifi else app.mobile
                val usageColor = if (isWifiTab) WifiColor else DataColor
                
                Text(
                    text = formatBytes(currentUsage), 
                    color = usageColor, 
                    fontSize = 18.sp, 
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "الإجمالي: ${formatBytes(app.total)}", 
                    color = Color.Gray, 
                    fontSize = 11.sp
                )
            }
        }
    }
}
