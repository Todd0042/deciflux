package com.example.deciflux.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.deciflux.ui.theme.DecifluxCardSurface
import com.example.deciflux.ui.theme.DecifluxElectricCyan
import com.example.deciflux.ui.theme.DecifluxEmeraldPeak
import com.example.deciflux.ui.theme.DecifluxLuminousViolet
import com.example.deciflux.ui.theme.DecifluxWarningAmber

@Composable
fun PermissionRationaleDialog(
    onGrantClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DecifluxCardSurface,
        shape = RoundedCornerShape(16.dp),
        icon = {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = DecifluxElectricCyan,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Permissions Required",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column {
                Text(
                    text = "Deciflux needs the following permissions to normalize videos in batches reliably:",
                    fontSize = 13.sp,
                    color = Color(0xFFCBD5E1),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                PermissionItemRow(
                    icon = Icons.Default.Folder,
                    iconTint = DecifluxElectricCyan,
                    title = "Video Storage Access",
                    description = "To read your selected videos and save normalized copies."
                )

                Spacer(modifier = Modifier.height(10.dp))

                PermissionItemRow(
                    icon = Icons.Default.Notifications,
                    iconTint = DecifluxLuminousViolet,
                    title = "Foreground Progress Notifications",
                    description = "Required on Android 13+ to keep long batch jobs alive with a live progress bar."
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onGrantClick,
                colors = ButtonDefaults.buttonColors(containerColor = DecifluxElectricCyan, contentColor = Color.Black)
            ) {
                Text("Grant Permissions", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                    onDismiss()
                }
            ) {
                Text("App Settings", color = Color(0xFF94A3B8))
            }
        }
    )
}

@Composable
fun BatteryOptimizationDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DecifluxCardSurface,
        shape = RoundedCornerShape(16.dp),
        icon = {
            Icon(
                imageVector = Icons.Default.BatteryAlert,
                contentDescription = null,
                tint = DecifluxWarningAmber,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Uninterrupted Background Batches",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Text(
                text = "Many phone manufacturers (Samsung, Xiaomi, Pixel) aggressively terminate long background tasks when the screen turns off.\n\nTo ensure your video batches finish without interruption, you can disable battery optimization for Deciflux.",
                fontSize = 13.sp,
                color = Color(0xFFCBD5E1)
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        try {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            context.startActivity(fallbackIntent)
                        }
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = DecifluxEmeraldPeak, contentColor = Color.Black)
            ) {
                Text("Exempt App", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Not Now", color = Color(0xFF94A3B8))
            }
        }
    )
}

@Composable
private fun PermissionItemRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier
                .size(20.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
            Text(text = description, fontSize = 11.sp, color = Color(0xFF94A3B8))
        }
    }
}
