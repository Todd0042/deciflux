package com.example.deciflux.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.deciflux.model.NormalizationConfig
import com.example.deciflux.model.NormalizationPreset
import com.example.deciflux.model.SceneLevelingIntensity
import com.example.deciflux.ui.theme.DecifluxCardBorder
import com.example.deciflux.ui.theme.DecifluxCardSurface
import com.example.deciflux.ui.theme.DecifluxElectricCyan
import com.example.deciflux.ui.theme.DecifluxEmeraldPeak
import com.example.deciflux.ui.theme.DecifluxLuminousViolet
import com.example.deciflux.ui.theme.DecifluxSurfaceVariantDark

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PresetAndSceneSheet(
    config: NormalizationConfig,
    onConfigChange: (NormalizationConfig) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var dropdownExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DecifluxCardSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = DecifluxElectricCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Audio Normalization Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Lossless video passthrough with EBU R128 audio leveling",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            // 1. Loudness Presets Section
            Text(
                text = "Target Loudness Standard",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = DecifluxElectricCyan,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NormalizationPreset.entries.forEach { preset ->
                    val isSelected = config.preset == preset
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            onConfigChange(config.copy(
                                preset = preset,
                                targetLufs = preset.targetLufs,
                                targetTruePeak = preset.targetTruePeak,
                                targetLra = preset.targetLra
                            ))
                        },
                        label = {
                            Text(
                                text = "${preset.title} (${preset.targetLufs.toInt()} LUFS)",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = Color.Black
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DecifluxElectricCyan,
                            selectedLabelColor = Color.Black,
                            containerColor = DecifluxSurfaceVariantDark,
                            labelColor = Color(0xFFCBD5E1)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = DecifluxCardBorder,
                            selectedBorderColor = DecifluxElectricCyan
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 2. SCENE-BY-SCENE NORMALIZATION CARD (Toggle + Conditional Dropdown)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, if (config.sceneLevelingEnabled) DecifluxLuminousViolet.copy(alpha = 0.6f) else DecifluxCardBorder, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = DecifluxSurfaceVariantDark),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Master Switch Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Waves,
                                contentDescription = null,
                                tint = if (config.sceneLevelingEnabled) DecifluxLuminousViolet else Color(0xFF94A3B8),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Scene-by-Scene Normalization",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "Detects and levels abrupt quiet & loud scene shifts",
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        Switch(
                            checked = config.sceneLevelingEnabled,
                            onCheckedChange = { isEnabled ->
                                onConfigChange(config.copy(sceneLevelingEnabled = isEnabled))
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = DecifluxLuminousViolet,
                                uncheckedThumbColor = Color(0xFF64748B),
                                uncheckedTrackColor = Color(0xFF1E293B)
                            )
                        )
                    }

                    // CONDITIONAL DROPDOWN MENU (Appears below switch when enabled)
                    AnimatedVisibility(
                        visible = config.sceneLevelingEnabled,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            Text(
                                text = "Amount of normalization by scene",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DecifluxLuminousViolet
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            ExposedDropdownMenuBox(
                                expanded = dropdownExpanded,
                                onExpandedChange = { dropdownExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = config.sceneLevelingIntensity.label,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded)
                                    },
                                    modifier = Modifier
                                        .menuAnchor(androidx.compose.material3.ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                        .fillMaxWidth(),
                                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                                        focusedBorderColor = DecifluxLuminousViolet,
                                        unfocusedBorderColor = DecifluxCardBorder,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )

                                ExposedDropdownMenu(
                                    expanded = dropdownExpanded,
                                    onDismissRequest = { dropdownExpanded = false },
                                    modifier = Modifier.background(DecifluxCardSurface)
                                ) {
                                    SceneLevelingIntensity.entries.forEach { intensity ->
                                        DropdownMenuItem(
                                            text = {
                                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                                    Text(
                                                        text = intensity.label,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        color = if (config.sceneLevelingIntensity == intensity) DecifluxLuminousViolet else Color.White
                                                    )
                                                    Text(
                                                        text = intensity.description,
                                                        fontSize = 11.sp,
                                                        color = Color(0xFF94A3B8)
                                                    )
                                                }
                                            },
                                            onClick = {
                                                onConfigChange(config.copy(sceneLevelingIntensity = intensity))
                                                dropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // If CUSTOM intensity selected: show slider
                            if (config.sceneLevelingIntensity == SceneLevelingIntensity.CUSTOM) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Leveling Strength", fontSize = 12.sp, color = Color.White)
                                    Text("${config.customLevelingPercentage}%", fontSize = 12.sp, color = DecifluxLuminousViolet, fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = config.customLevelingPercentage.toFloat(),
                                    onValueChange = { onConfigChange(config.copy(customLevelingPercentage = it.toInt())) },
                                    valueRange = 10f..100f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = DecifluxLuminousViolet,
                                        activeTrackColor = DecifluxLuminousViolet
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 3. Audio Codec & Bitrate Options
            Text(
                text = "Audio Encoding Parameters",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = DecifluxElectricCyan,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Codec selector (AAC vs Opus)
                listOf("aac", "opus").forEach { codec ->
                    val isSelected = config.audioCodec == codec
                    FilterChip(
                        selected = isSelected,
                        onClick = { onConfigChange(config.copy(audioCodec = codec)) },
                        label = { Text(codec.uppercase(), fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DecifluxEmeraldPeak,
                            selectedLabelColor = Color.Black,
                            containerColor = DecifluxSurfaceVariantDark,
                            labelColor = Color.White
                        )
                    )
                }

                // Bitrate selector
                listOf("128k", "192k", "256k").forEach { bitrate ->
                    val isSelected = config.audioBitrate == bitrate
                    FilterChip(
                        selected = isSelected,
                        onClick = { onConfigChange(config.copy(audioBitrate = bitrate)) },
                        label = { Text(bitrate, fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DecifluxElectricCyan,
                            selectedLabelColor = Color.Black,
                            containerColor = DecifluxSurfaceVariantDark,
                            labelColor = Color.White
                        )
                    )
                }
            }
        }
    }
}
