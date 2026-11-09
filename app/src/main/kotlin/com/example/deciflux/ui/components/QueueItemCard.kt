package com.example.deciflux.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.deciflux.model.BatchItemStatus
import com.example.deciflux.model.VideoBatchItem
import com.example.deciflux.ui.theme.DecifluxCardBorder
import com.example.deciflux.ui.theme.DecifluxCardSurface
import com.example.deciflux.ui.theme.DecifluxElectricCyan
import com.example.deciflux.ui.theme.DecifluxEmeraldPeak
import com.example.deciflux.ui.theme.DecifluxErrorRed
import com.example.deciflux.ui.theme.DecifluxLuminousViolet
import com.example.deciflux.ui.theme.DecifluxSurfaceVariantDark
import com.example.deciflux.ui.theme.DecifluxWarningAmber

@Composable
fun QueueItemCard(
    item: VideoBatchItem,
    isBatchRunning: Boolean,
    onRemove: () -> Unit,
    onPreview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(targetValue = item.progress, label = "ItemProgress")

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, DecifluxCardBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = DecifluxCardSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Video Type Icon / Status Indicator Box
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DecifluxSurfaceVariantDark),
                    contentAlignment = Alignment.Center
                ) {
                    when (item.status) {
                        BatchItemStatus.COMPLETED -> {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Completed",
                                tint = DecifluxEmeraldPeak,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        BatchItemStatus.PROCESSING -> {
                            CircularProgressIndicator(
                                progress = { animatedProgress },
                                strokeWidth = 3.dp,
                                color = DecifluxElectricCyan,
                                trackColor = Color(0xFF1E293B),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        BatchItemStatus.ANALYZING -> {
                            CircularProgressIndicator(
                                strokeWidth = 3.dp,
                                color = DecifluxLuminousViolet,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        BatchItemStatus.FAILED -> {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "Failed",
                                tint = DecifluxErrorRed,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        else -> {
                            Icon(
                                imageVector = Icons.Default.Movie,
                                contentDescription = "Video",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title, Duration, Size, and Status Details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.filename,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${item.formattedDuration} • ${item.formattedSize}",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )

                        // Completed Loudness Delta Badge
                        if (item.status == BatchItemStatus.COMPLETED && item.initialLufs != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF132F24))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = String.format("%.0f ➔ %.0f LUFS", item.initialLufs, item.normalizedLufs ?: -14f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DecifluxEmeraldPeak
                                )
                            }
                        }
                    }
                }

                // Actions: Preview or Remove
                if (item.status == BatchItemStatus.COMPLETED) {
                    IconButton(onClick = onPreview, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Preview",
                            tint = DecifluxEmeraldPeak,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else if (!isBatchRunning) {
                    IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Real-time Progress Bar & Status Text (During Active Processing)
            if (item.status == BatchItemStatus.PROCESSING || item.status == BatchItemStatus.ANALYZING) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = DecifluxElectricCyan,
                    trackColor = Color(0xFF1E293B)
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.statusMessage,
                    fontSize = 10.sp,
                    color = DecifluxElectricCyan,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else if (item.status == BatchItemStatus.FAILED && item.errorReason != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.errorReason,
                    fontSize = 10.sp,
                    color = DecifluxErrorRed,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
