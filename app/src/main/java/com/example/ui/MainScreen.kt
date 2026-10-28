package com.example.ui

import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.model.AppInfo
import com.example.state.KioskStateManager
import com.example.ui.components.AppPickerSheet
import com.example.ui.components.KioskActiveCard
import com.example.ui.components.PermissionGuideCard
import com.example.ui.components.PinSetupDialog
import com.example.ui.theme.AccentGreen
import com.example.util.PermissionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // State from KioskStateManager
    val isLocked by KioskStateManager.isLocked.collectAsState()
    val lockedPackage by KioskStateManager.lockedPackage.collectAsState()
    val lockedAppName by KioskStateManager.lockedAppName.collectAsState()
    val lockStartTime by KioskStateManager.lockStartTime.collectAsState()

    // Selected App
    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }
    var installedApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var showAppPicker by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Permission states
    var hasOverlay by remember { mutableStateOf(PermissionHelper.canDrawOverlays(context)) }
    var hasAccessibility by remember { mutableStateOf(PermissionHelper.isAccessibilityServiceEnabled(context)) }
    var hasUsageStats by remember { mutableStateOf(PermissionHelper.hasUsageStatsPermission(context)) }
    var hasNotification by remember { mutableStateOf(PermissionHelper.hasNotificationPermission(context)) }

    // Dialogs
    var showPinSetupDialog by remember { mutableStateOf(false) }
    var showUnlockVerifyDialog by remember { mutableStateOf(false) }
    var unlockPinInput by remember { mutableStateOf("") }
    var unlockError by remember { mutableStateOf<String?>(null) }

    fun refreshPermissions(showToast: Boolean = false) {
        val o = PermissionHelper.canDrawOverlays(context)
        val a = PermissionHelper.isAccessibilityServiceEnabled(context)
        val u = PermissionHelper.hasUsageStatsPermission(context)
        val n = PermissionHelper.hasNotificationPermission(context)

        hasOverlay = o
        hasAccessibility = a
        hasUsageStats = u
        hasNotification = n

        if (showToast) {
            val grantedCount = listOf(o, a, u, n).count { it }
            Toast.makeText(
                context,
                "สถานะ: อนุญาตแล้ว $grantedCount จาก 4 สิทธิ์",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Lifecycle observer to auto-refresh whenever user comes back to the app from Settings!
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPermissions(showToast = false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Notification Permission Launcher (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotification = isGranted
        if (!isGranted) {
            PermissionHelper.openNotificationSettings(context)
        }
    }

    // Load installed apps
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val apps = PermissionHelper.getInstalledLaunchableApps(context)
            withContext(Dispatchers.Main) {
                installedApps = apps
                if (selectedApp == null && apps.isNotEmpty()) {
                    val previousTarget = lockedPackage
                    val found = apps.find { it.packageName == previousTarget }
                    selectedApp = found ?: apps.firstOrNull { !it.isSystemApp } ?: apps.firstOrNull()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher_cloud_lock),
                            contentDescription = "Rapper Cloud Logo",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Column {
                            Text(
                                text = "Rapper Cloud",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Kiosk Mode & App Locker",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { refreshPermissions(showToast = true) },
                        modifier = Modifier.testTag("top_refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Status",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        color = if (isLocked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isLocked) AccentGreen else Color.Gray)
                            )
                            Text(
                                text = if (isLocked) "LOCKED" else "READY",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 18.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. If currently locked, show Kiosk Active Card
            if (isLocked && !lockedPackage.isNullOrEmpty()) {
                item {
                    KioskActiveCard(
                        appName = lockedAppName ?: "Target App",
                        packageName = lockedPackage ?: "",
                        startTime = lockStartTime,
                        onResumeTargetApp = {
                            val intent = context.packageManager.getLaunchIntentForPackage(lockedPackage ?: "")
                            if (intent != null) {
                                context.startActivity(intent)
                            }
                        },
                        onUnlockClick = {
                            showUnlockVerifyDialog = true
                        }
                    )
                }
            }

            // 2. Target App Selection Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("target_app_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Apps,
                                    contentDescription = "Target App",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "แอปพลิเคชันเป้าหมาย (Target App)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            OutlinedButton(
                                onClick = { showAppPicker = true },
                                modifier = Modifier.testTag("change_app_button"),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = ButtonDefaults.TextButtonContentPadding
                            ) {
                                Text("เลือกแอป")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val currentApp = selectedApp
                        if (currentApp != null) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { showAppPicker = true },
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (currentApp.iconBitmap != null) {
                                        Image(
                                            bitmap = currentApp.iconBitmap.asImageBitmap(),
                                            contentDescription = currentApp.appName,
                                            modifier = Modifier
                                                .size(52.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(52.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = currentApp.appName.take(1).uppercase(),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 20.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = currentApp.appName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = currentApp.packageName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp
                                        )
                                    }

                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Switch",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        } else {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showAppPicker = true },
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "ยังไม่ได้เลือกแอปพลิเคชัน",
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "แตะเพื่อเลือกแอปที่ต้องการล็อกหน้าจอ",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Play / Lock Action Button
            item {
                Button(
                    onClick = {
                        val app = selectedApp
                        if (app == null) {
                            Toast.makeText(context, "กรุณาเลือกแอปเป้าหมายก่อน", Toast.LENGTH_SHORT).show()
                            showAppPicker = true
                            return@Button
                        }
                        if (!hasOverlay) {
                            Toast.makeText(
                                context,
                                "กรุณาเปิดสิทธิ์ 'แสดงทับแอปอื่น' เพื่อให้ปุ่มปลดล็อกที่มุมจอทำงานได้",
                                Toast.LENGTH_LONG
                            ).show()
                            PermissionHelper.openOverlaySettings(context)
                            return@Button
                        }
                        showPinSetupDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("play_lock_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Start Lock",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isLocked) "เริ่มล็อกแอปอีกครั้ง (Restart Lock)" else "เริ่มล็อกหน้าจอ (Play & Lock)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 4. Permissions Checklist & Step Guide
            item {
                PermissionGuideCard(
                    hasOverlay = hasOverlay,
                    hasAccessibility = hasAccessibility,
                    hasUsageStats = hasUsageStats,
                    hasNotification = hasNotification,
                    onRequestOverlay = {
                        PermissionHelper.openOverlaySettings(context)
                    },
                    onRequestAccessibility = {
                        PermissionHelper.openAccessibilitySettings(context)
                    },
                    onRequestUsageStats = {
                        PermissionHelper.openUsageAccessSettings(context)
                    },
                    onRequestNotification = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            PermissionHelper.openNotificationSettings(context)
                        }
                    },
                    onOpenAppInfo = {
                        PermissionHelper.openAppDetailsSettings(context)
                    },
                    onRefreshPermissions = {
                        refreshPermissions(showToast = true)
                    }
                )
            }

            // 5. Secret Unlock Mechanism Explainer
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("secret_unlock_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = "Secret Unlock",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "กลไกการปลดล็อก (Secret Unlock Mechanism)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "• ปุ่มปลดล็อกซ่อน (Tiny Hidden Button): เมื่อเริ่มการล็อก จะมีปุ่มแม่กุญแจขนาดเล็กมาก ลอยอยู่ที่มุมขวาบนของหน้าจอ\n" +
                                    "• แตะที่ปุ่มซ่อน: หน้าต่างใส่รหัสผ่านจะเด้งขึ้นมาแบบ Overlay ทับหน้าจอ\n" +
                                    "• ใส่ PIN ถูกต้อง: ระบบจะหยุดดักจับ ปิด Kiosk และอนุญาตให้ใช้งานเครื่องได้ตามปกติ\n" +
                                    "• ป้องกันการออก: หากแตะ Home หรือ Recent Apps หรือสลับแอป ระบบจะดึงกลับมาที่แอปเป้าหมายทันที",
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 22.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // App Picker Bottom Sheet
    if (showAppPicker) {
        AppPickerSheet(
            sheetState = sheetState,
            apps = installedApps,
            selectedPackage = selectedApp?.packageName,
            onAppSelected = { app ->
                selectedApp = app
                showAppPicker = false
            },
            onDismiss = {
                showAppPicker = false
            }
        )
    }

    // PIN Setup Dialog (When user clicks Play)
    if (showPinSetupDialog) {
        val app = selectedApp
        if (app != null) {
            PinSetupDialog(
                targetAppName = app.appName,
                onConfirm = { pin ->
                    showPinSetupDialog = false
                    val success = KioskStateManager.startLock(
                        context = context,
                        packageName = app.packageName,
                        appName = app.appName,
                        pin = pin
                    )
                    if (success) {
                        Toast.makeText(
                            context,
                            "🔒 เริ่มโหมด Kiosk แล้ว! แตะปุ่มแม่กุญแจมุมขวาบนเมื่อต้องการปลดล็อก",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            context,
                            "ไม่สามารถเปิดแอปที่เลือกได้",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                onDismiss = {
                    showPinSetupDialog = false
                }
            )
        }
    }

    // In-App Unlock PIN Dialog
    if (showUnlockVerifyDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {
                showUnlockVerifyDialog = false
                unlockPinInput = ""
                unlockError = null
            },
            title = {
                Text(
                    text = "ปลดล็อกโหมด Kiosk",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "ใส่รหัส PIN 4 หลักที่ตั้งไว้เพื่อยกเลิกการล็อก:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    androidx.compose.material3.OutlinedTextField(
                        value = unlockPinInput,
                        onValueChange = {
                            if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                                unlockPinInput = it
                                unlockError = null
                            }
                        },
                        label = { Text("PIN 4 หลัก") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                        ),
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("in_app_unlock_pin_input")
                    )
                    if (unlockError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = unlockError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val isValid = KioskStateManager.verifyPin(context, unlockPinInput)
                        if (isValid) {
                            KioskStateManager.unlock(context)
                            showUnlockVerifyDialog = false
                            unlockPinInput = ""
                            unlockError = null
                            Toast.makeText(context, "✓ ปลดล็อกเรียบร้อยแล้ว", Toast.LENGTH_SHORT).show()
                        } else {
                            unlockError = "รหัส PIN ไม่ถูกต้อง"
                        }
                    },
                    modifier = Modifier.testTag("confirm_in_app_unlock_button")
                ) {
                    Text("ปลดล็อก")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showUnlockVerifyDialog = false
                        unlockPinInput = ""
                        unlockError = null
                    }
                ) {
                    Text("ยกเลิก")
                }
            }
        )
    }
}
