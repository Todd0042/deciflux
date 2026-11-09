package com.example.deciflux.ui.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.deciflux.ui.theme.DecifluxCardBorder
import com.example.deciflux.ui.theme.DecifluxCardSurface
import com.example.deciflux.ui.theme.DecifluxElectricCyan
import com.example.deciflux.ui.theme.DecifluxEmeraldPeak
import com.example.deciflux.ui.theme.DecifluxLuminousViolet
import com.example.deciflux.ui.theme.DecifluxSurfaceVariantDark
import com.example.deciflux.ui.theme.DecifluxWarningAmber

@Composable
fun InitialSetupScreen(
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    var currentStep by remember { mutableIntStateOf(0) }

    var isStorageGranted by remember { mutableStateOf(com.example.deciflux.util.PermissionChecker.hasStoragePermission(context)) }
    var isNotificationGranted by remember { mutableStateOf(com.example.deciflux.util.PermissionChecker.hasNotificationPermission(context)) }
    var isBatteryExempt by remember { mutableStateOf(com.example.deciflux.util.PermissionChecker.isBatteryOptimizationExempt(context)) }

    // Storage permission launcher
    val storageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        isStorageGranted = granted || com.example.deciflux.util.PermissionChecker.hasStoragePermission(context)
    }

    // Notification permission launcher
    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        isNotificationGranted = granted || com.example.deciflux.util.PermissionChecker.hasNotificationPermission(context)
    }

    val totalSteps = 4

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp),
                color = Color.Transparent
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 0 && currentStep < totalSteps - 1) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Back", color = Color(0xFF94A3B8))
                        }
                    } else {
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    if (currentStep < totalSteps - 1) {
                        Button(
                            onClick = { currentStep++ },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DecifluxElectricCyan,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Next", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    } else {
                        Button(
                            onClick = onComplete,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DecifluxEmeraldPeak,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Done — Start Normalizing", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Step Progress Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(totalSteps) { index ->
                    Box(
                        modifier = Modifier
                            .size(if (index == currentStep) 24.dp else 10.dp, 10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                if (index == currentStep) DecifluxElectricCyan
                                else if (index < currentStep) DecifluxEmeraldPeak
                                else Color(0xFF262D3D)
                            )
                    )
                    if (index < totalSteps - 1) {
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            AnimatedContent(
                targetState = currentStep,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "OnboardingStep"
            ) { step ->
                when (step) {
                    0 -> OnboardingStepCard(
                        icon = Icons.Default.FolderSpecial,
                        iconColor = DecifluxElectricCyan,
                        stepNumber = "Step 1 of 4",
                        title = "Video Storage Access",
                        description = "Deciflux analyzes video audio tracks with zero video re-encoding (-c:v copy) and saves normalized copies.\n\nTo scan folders and open your video files, storage permission is required.",
                        isGranted = isStorageGranted,
                        actionLabel = if (isStorageGranted) "Permission Granted" else "Grant Storage Access",
                        onAction = {
                            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                Manifest.permission.READ_MEDIA_VIDEO
                            } else {
                                Manifest.permission.READ_EXTERNAL_STORAGE
                            }
                            storageLauncher.launch(permission)
                        }
                    )

                    1 -> OnboardingStepCard(
                        icon = Icons.Default.NotificationsActive,
                        iconColor = DecifluxLuminousViolet,
                        stepNumber = "Step 2 of 4",
                        title = "Foreground Progress Notifications",
                        description = "Normalizing large batches takes time. Android requires Notification permission so Deciflux can display a persistent progress bar and keep processing safely in the background while you use other apps.",
                        isGranted = isNotificationGranted,
                        actionLabel = if (isNotificationGranted) "Permission Granted" else "Enable Notifications",
                        onAction = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                isNotificationGranted = true
                            }
                        }
                    )

                    2 -> OnboardingStepCard(
                        icon = Icons.Default.BatteryAlert,
                        iconColor = DecifluxWarningAmber,
                        stepNumber = "Step 3 of 4",
                        title = "Unrestricted Background Processing",
                        description = "Android phone manufacturers frequently kill long background tasks when your screen turns off.\n\nDisabling battery optimization guarantees that your video batches continue processing uninterrupted overnight or with the screen locked.",
                        isGranted = isBatteryExempt,
                        actionLabel = if (isBatteryExempt) "Battery Optimization Exempt" else "Request Battery Exemption",
                        onAction = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                try {
                                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                    context.startActivity(intent)
                                }
                            }
                            // Recheck upon user return
                            isBatteryExempt = com.example.deciflux.util.PermissionChecker.isBatteryOptimizationExempt(context)
                        }
                    )

                    3 -> OnboardingSummaryCard(
                        isStorageGranted = isStorageGranted,
                        isNotificationGranted = isNotificationGranted,
                        isBatteryExempt = isBatteryExempt,
                        onOpenSettings = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingStepCard(
    icon: ImageVector,
    iconColor: Color,
    stepNumber: String,
    title: String,
    description: String,
    isGranted: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(DecifluxCardSurface)
                .border(1.dp, DecifluxCardBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stepNumber.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = iconColor,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DecifluxCardSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, DecifluxCardBorder),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = description,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = Color(0xFFCBD5E1)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onAction,
                    enabled = !isGranted,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isGranted) DecifluxEmeraldPeak else iconColor,
                        contentColor = Color.Black,
                        disabledContainerColor = Color(0xFF1E293B),
                        disabledContentColor = DecifluxEmeraldPeak
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isGranted) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(actionLabel, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun OnboardingSummaryCard(
    isStorageGranted: Boolean,
    isNotificationGranted: Boolean,
    isBatteryExempt: Boolean,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(DecifluxCardSurface)
                .border(1.dp, DecifluxCardBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.GraphicEq,
                contentDescription = null,
                tint = DecifluxEmeraldPeak,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "STEP 4 OF 4",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = DecifluxEmeraldPeak,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Ready to Normalize",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DecifluxCardSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, DecifluxCardBorder),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Permission & Setup Status",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(12.dp))

                SetupStatusItem(
                    label = "Storage Access",
                    isConfigured = isStorageGranted
                )

                Spacer(modifier = Modifier.height(8.dp))

                SetupStatusItem(
                    label = "Background Notifications",
                    isConfigured = isNotificationGranted
                )

                Spacer(modifier = Modifier.height(8.dp))

                SetupStatusItem(
                    label = "Battery Optimization Disabled",
                    isConfigured = isBatteryExempt
                )

                Spacer(modifier = Modifier.height(18.dp))

                OutlinedButton(
                    onClick = onOpenSettings,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DecifluxElectricCyan)
                ) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open App Permissions in Settings", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

@Composable
private fun SetupStatusItem(
    label: String,
    isConfigured: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DecifluxSurfaceVariantDark)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 13.sp, color = Color.White)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isConfigured) Icons.Default.CheckCircle else Icons.Default.Security,
                contentDescription = null,
                tint = if (isConfigured) DecifluxEmeraldPeak else DecifluxWarningAmber,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isConfigured) "Ready" else "Action needed",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isConfigured) DecifluxEmeraldPeak else DecifluxWarningAmber
            )
        }
    }
}
