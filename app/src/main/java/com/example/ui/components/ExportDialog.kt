package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.audio.AudioExporter
import com.example.model.AnalysisResult
import kotlinx.coroutines.launch

@Composable
fun ExportDialog(
    result: AnalysisResult,
    audioExporter: AudioExporter,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("export_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Export Narration",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Download or share your generated tutorial assets",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 1. Audio Master (.wav / .mp3)
                ExportActionCard(
                    icon = Icons.Default.Audiotrack,
                    title = "Export Master Audio (.WAV)",
                    subtitle = "High fidelity voice track (${result.audioDurationFormatted})",
                    buttonText = "Share Audio",
                    onClick = {
                        result.audioFile?.let { file ->
                            audioExporter.shareFile(file, "audio/wav", "Share Narration Audio")
                        }
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Narration Script (.txt)
                ExportActionCard(
                    icon = Icons.Default.Description,
                    title = "Narration Script (.TXT)",
                    subtitle = "Step timestamps, drawing cues, and voiceover text",
                    buttonText = "Share Script",
                    onClick = {
                        scope.launch {
                            val file = audioExporter.generateScriptTxt(result)
                            audioExporter.shareFile(file, "text/plain", "Share Narration Script")
                        }
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Subtitles (.srt)
                ExportActionCard(
                    icon = Icons.Default.Subtitles,
                    title = "Video Subtitles (.SRT)",
                    subtitle = "Synchronized SubRip captions ready for YouTube / Premiere",
                    buttonText = "Share SRT",
                    onClick = {
                        scope.launch {
                            val file = audioExporter.generateSrtSubtitles(result)
                            audioExporter.shareFile(file, "application/x-subrip", "Share Subtitles")
                        }
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 4. Analysis Data (.json)
                ExportActionCard(
                    icon = Icons.Default.Code,
                    title = "Step Analysis (.JSON)",
                    subtitle = "Structured vision metadata with timestamps & coordinates",
                    buttonText = "Share JSON",
                    onClick = {
                        scope.launch {
                            val file = audioExporter.generateAnalysisJson(result)
                            audioExporter.shareFile(file, "application/json", "Share JSON Metadata")
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Close button
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Done")
                }
            }
        }
    }
}

@Composable
private fun ExportActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    buttonText: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            FilledTonalButton(
                onClick = onClick,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Default.IosShare,
                    contentDescription = "Share",
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(buttonText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
