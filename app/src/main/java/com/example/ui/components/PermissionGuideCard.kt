package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentGreen

@Composable
fun PermissionGuideCard(
    hasOverlay: Boolean,
    hasAccessibility: Boolean,
    hasUsageStats: Boolean,
    hasNotification: Boolean,
    onRequestOverlay: () -> Unit,
    onRequestAccessibility: () -> Unit,
    onRequestUsageStats: () -> Unit,
    onRequestNotification: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onRefreshPermissions: () -> Unit
) {
    var showStepGuide by remember { mutableStateOf(false) }
    var showRestrictedSettingsGuide by remember { mutableStateOf(false) }
    val allGranted = hasOverlay && hasAccessibility && hasUsageStats && hasNotification

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("permission_guide_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header with Refresh Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (allGranted) AccentGreen.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.primaryContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (allGranted) Icons.Default.CheckCircle else Icons.Default.Security,
                            contentDescription = "Permission Security",
                            tint = if (allGranted) AccentGreen else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "สิทธิ์การเข้าถึง (Permissions)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (allGranted) "✓ ได้รับอนุญาตครบทุกสิทธิ์แล้ว"
                            else "กรุณากดเปิดสิทธิ์ แล้วกด 'รีเฟรช' เพื่ออัปเดต",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (allGranted) AccentGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Refresh Button
                IconButton(
                    onClick = onRefreshPermissions,
                    modifier = Modifier.testTag("refresh_permissions_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh permissions",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Overlay Permission
            PermissionItemRow(
                title = "1. แสดงทับแอปอื่น (Draw Over Apps)",
                description = "สำหรับปุ่มปลดล็อกซ่อนที่มุมจอ และหน้าต่างใส่รหัส PIN",
                isGranted = hasOverlay,
                testTag = "perm_overlay_row",
                onGrantClick = onRequestOverlay
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Accessibility Service
            PermissionItemRow(
                title = "2. Accessibility Service (Watchdog)",
                description = "ตรวจจับการกด Home, Recents และดึงกลับเข้าแอปเป้าหมายทันที",
                isGranted = hasAccessibility,
                testTag = "perm_accessibility_row",
                onGrantClick = onRequestAccessibility
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Usage Access
            PermissionItemRow(
                title = "3. เข้าถึงข้อมูลการใช้งาน (Usage Access)",
                description = "ตรวจจับแอปที่กำลังเปิดอยู่เบื้องหน้าแบบคู่ขนาน",
                isGranted = hasUsageStats,
                testTag = "perm_usage_row",
                onGrantClick = onRequestUsageStats
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 4. Notifications
            PermissionItemRow(
                title = "4. การแจ้งเตือน (Notifications)",
                description = "รักษาสถานะเบื้องหน้าไม่ให้ระบบหยุดการทำงาน",
                isGranted = hasNotification,
                testTag = "perm_notification_row",
                onGrantClick = onRequestNotification
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Manual Refresh Status Bar
            OutlinedButton(
                onClick = onRefreshPermissions,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("manual_refresh_permissions_bar"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("กดตรวจสอบสถานะอีกครั้ง (Refresh Status)")
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Guide 1: Step-by-step Accessibility Guide Accordion
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showStepGuide = !showStepGuide }
                    .testTag("accessibility_guide_toggle"),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
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
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = "Help",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "วิธีเปิดใช้งาน Accessibility Service",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Icon(
                            imageVector = if (showStepGuide) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand guide",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    AnimatedVisibility(visible = showStepGuide) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            Text(
                                text = "1. แตะปุ่ม 'เปิดสิทธิ์' ในรายการข้อ 2 (Accessibility Service)\n" +
                                        "2. ระบบจะพาไปยังหน้า 'การเข้าถึง' (Accessibility)\n" +
                                        "3. เลื่อนลงมาหาหัวข้อ 'แอปที่ดาวน์โหลด' (Downloaded Apps)\n" +
                                        "4. แตะที่ 'Rapper Cloud Kiosk Watchdog'\n" +
                                        "5. กดเปิดสวิตช์ (Toggle ON) และกดยืนยัน 'อนุญาต' (Allow)\n" +
                                        "6. เมื่อกลับมาที่แอปนี้ ระบบจะอัปเดตเป็นสีเขียวทันที (หรือกดปุ่มรีเฟรชด้านบน)",
                                style = MaterialTheme.typography.bodySmall,
                                lineHeight = 20.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Guide 2: Android 13/14+ Restricted Setting Fix Accordion
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showRestrictedSettingsGuide = !showRestrictedSettingsGuide }
                    .testTag("restricted_settings_toggle"),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
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
                                imageVector = Icons.Default.Info,
                                contentDescription = "Restricted settings info",
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "ปุ่มเป็นสีเทา / 'การตั้งค่าที่ถูกจำกัด'?",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFF59E0B)
                            )
                        }
                        Icon(
                            imageVector = if (showRestrictedSettingsGuide) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand restricted guide",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    AnimatedVisibility(visible = showRestrictedSettingsGuide) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            Text(
                                text = "บน Android 13/14/15 บางรุ่น ระบบความปลอดภัยจะล็อก Accessibility ไว้เป็นสีเทา (Restricted Setting):\n\n" +
                                        "วิธีแก้:\n" +
                                        "1. แตะปุ่ม 'เปิดหน้าข้อมูลแอป' ด้านล่างนี้\n" +
                                        "2. แตะปุ่ม 3 จุด (⋮) ที่มุมขวาบนของหน้าจอ\n" +
                                        "3. เลือก 'อนุญาตการตั้งค่าที่ถูกจำกัด' (Allow restricted settings)\n" +
                                        "4. สแกนลายนิ้วมือหรือใส่รหัสผ่านหน้าจอ\n" +
                                        "5. จากนั้นกลับมาเปิดสิทธิ์ Accessibility จะเปิดได้ทันที",
                                style = MaterialTheme.typography.bodySmall,
                                lineHeight = 20.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = onOpenAppInfo,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            ) {
                                Icon(imageVector = Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("เปิดหน้าข้อมูลแอป (App Info)", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionItemRow(
    title: String,
    description: String,
    isGranted: Boolean,
    testTag: String,
    onGrantClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = if (isGranted) "Granted" else "Requires setup",
                tint = if (isGranted) AccentGreen else Color(0xFFF59E0B),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isGranted) {
                Surface(
                    color = AccentGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "✓ อนุญาตแล้ว",
                        color = AccentGreen,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            } else {
                Button(
                    onClick = onGrantClick,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = ButtonDefaults.TextButtonContentPadding,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text("เปิดสิทธิ์", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Go",
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
