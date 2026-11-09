package com.example.deciflux.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.deciflux.model.LogTag
import com.example.deciflux.model.TerminalLogEntry
import com.example.deciflux.ui.theme.DecifluxElectricCyan
import com.example.deciflux.ui.theme.DecifluxEmeraldPeak
import com.example.deciflux.ui.theme.DecifluxLuminousViolet
import com.example.deciflux.ui.theme.TerminalBackground
import com.example.deciflux.ui.theme.TerminalBorder
import com.example.deciflux.ui.theme.TerminalHeaderBg
import com.example.deciflux.ui.theme.TerminalTagAnalysis
import com.example.deciflux.ui.theme.TerminalTagError
import com.example.deciflux.ui.theme.TerminalTagInfo
import com.example.deciflux.ui.theme.TerminalTagScene
import com.example.deciflux.ui.theme.TerminalTagSuccess
import com.example.deciflux.ui.theme.TerminalTagWarn
import com.example.deciflux.ui.theme.TerminalTextMuted
import com.example.deciflux.ui.theme.TerminalTextPrimary

@Composable
fun TerminalConsoleView(
    logs: List<TerminalLogEntry>,
    isProcessing: Boolean,
    currentSpeed: String,
    currentEta: String,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    var autoScroll by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()
    val context = LocalContext.current

    // Auto-scroll to bottom when new logs arrive
    LaunchedEffect(logs.size, autoScroll) {
        if (autoScroll && logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    val consoleHeight = if (isExpanded) 340.dp else 190.dp

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, TerminalBorder, RoundedCornerShape(12.dp)),
        color = TerminalBackground
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Terminal Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TerminalHeaderBg)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Title and Terminal Prompt Symbol
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = ">_ TERMINAL STATUS",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = DecifluxElectricCyan
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    // Telemetry: Speed and ETA (when active)
                    if (isProcessing && currentSpeed != "--") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF1E2638))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "Speed",
                                tint = DecifluxEmeraldPeak,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = currentSpeed,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = DecifluxEmeraldPeak
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "ETA",
                                tint = DecifluxLuminousViolet,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "ETA $currentEta",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = DecifluxLuminousViolet
                            )
                        }
                    }
                }

                // Actions: Auto-scroll, Copy, Clear, Expand
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Auto-scroll toggle
                    IconButton(
                        onClick = { autoScroll = !autoScroll },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerticalAlignBottom,
                            contentDescription = "Auto-scroll",
                            tint = if (autoScroll) DecifluxElectricCyan else TerminalTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Copy logs to clipboard
                    IconButton(
                        onClick = {
                            val text = logs.joinToString("\n") { "[${it.formattedTime}] ${it.tag.display} ${it.message}" }
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Deciflux Logs", text))
                            Toast.makeText(context, "Logs copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy logs",
                            tint = TerminalTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Clear logs
                    IconButton(
                        onClick = onClearLogs,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear logs",
                            tint = TerminalTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Expand/Collapse height
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                            contentDescription = "Toggle height",
                            tint = DecifluxElectricCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Log Console Stream View
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(consoleHeight)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                items(logs, key = { it.id }) { log ->
                    LogLineItem(log = log)
                }
            }
        }
    }
}

@Composable
private fun LogLineItem(log: TerminalLogEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.5.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Timestamp
        Text(
            text = log.formattedTime,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = TerminalTextMuted
        )

        Spacer(modifier = Modifier.width(6.dp))

        // Color-coded log tag
        val tagColor = when (log.tag) {
            LogTag.INFO -> TerminalTagInfo
            LogTag.ANALYSIS -> TerminalTagAnalysis
            LogTag.SCENE -> TerminalTagScene
            LogTag.REMUX -> DecifluxLuminousViolet
            LogTag.SUCCESS -> TerminalTagSuccess
            LogTag.WARN -> TerminalTagWarn
            LogTag.ERROR -> TerminalTagError
        }

        Text(
            text = log.tag.display,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = tagColor
        )

        Spacer(modifier = Modifier.width(6.dp))

        // Message text
        Text(
            text = log.message,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = TerminalTextPrimary,
            modifier = Modifier.weight(1f)
        )
    }
}
