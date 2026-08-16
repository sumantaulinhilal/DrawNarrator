package com.example.ui.components

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Brush as ComposeBrush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.VideoMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun VideoPreviewPlayer(
    metadata: VideoMetadata,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var previewThumbnail by remember(metadata.uri) { mutableStateOf<Bitmap?>(null) }
    var useNativeVideoView by remember(metadata.uri) { mutableStateOf(false) }

    LaunchedEffect(metadata.uri) {
        withContext(Dispatchers.IO) {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, metadata.uri)
                val bmp = retriever.getFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                previewThumbnail = bmp
                retriever.release()
                useNativeVideoView = true
            } catch (_: Exception) {
                // If native retriever cannot open synthetic mock file, keep image mode
                useNativeVideoView = false
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("video_preview_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.OndemandVideo,
                            contentDescription = "Video Ready",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = metadata.fileName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = "Real-Time Drawing Source Ready",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Video Player / Canvas Preview Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF181B20)),
                contentAlignment = Alignment.Center
            ) {
                if (useNativeVideoView) {
                    AndroidView(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                setOnErrorListener { _, _, _ -> true }
                                try {
                                    setVideoURI(metadata.uri)
                                    val mc = MediaController(ctx)
                                    mc.setAnchorView(this)
                                    setMediaController(mc)
                                    setOnPreparedListener { mp ->
                                        mp.isLooping = true
                                    }
                                } catch (_: Exception) {}
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (previewThumbnail != null) {
                    Image(
                        bitmap = previewThumbnail!!.asImageBitmap(),
                        contentDescription = "Drawing Preview Frame",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Stylized Art Video Canvas Placeholder
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Brush,
                                    contentDescription = "Drawing In Progress",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = metadata.fileName,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Synthetic Drawing Stream Ready For AI Narration",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Metadata Badges (Duration, Resolution, File Size)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetadataBadge(
                    icon = Icons.Default.Schedule,
                    label = "Duration",
                    value = metadata.durationFormatted,
                    modifier = Modifier.weight(1f)
                )
                MetadataBadge(
                    icon = Icons.Default.Hd,
                    label = "Resolution",
                    value = metadata.resolutionFormatted,
                    modifier = Modifier.weight(1f)
                )
                MetadataBadge(
                    icon = Icons.Default.Storage,
                    label = "Size",
                    value = metadata.sizeFormatted,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MetadataBadge(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = label,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
