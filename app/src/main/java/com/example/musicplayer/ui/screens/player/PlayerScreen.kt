package com.example.musicplayer.ui.screens.player

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.musicplayer.viewmodel.PlayerViewModel

@SuppressLint("DefaultLocale")
fun formatTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(navController: NavController, playerViewModel: PlayerViewModel) {
    val song by playerViewModel.currentSong.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val position by playerViewModel.currentPosition.collectAsState()
    val isShuffle by playerViewModel.isShuffleEnabled.collectAsState()

    val realDuration by playerViewModel.duration.collectAsState()

    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(position) {
        if (!isDragging) sliderPosition = position.toFloat()
    }

    val fallbackIcon = rememberVectorPainter(Icons.Default.MusicNote)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Now Playing", fontSize = 16.sp, fontWeight = FontWeight.Medium) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Box(
                modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(32.dp)).background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(song?.albumArtUri).crossfade(true).build(),
                    contentDescription = "Album Art",
                    contentScale = ContentScale.Crop,
                    error = fallbackIcon,
                    fallback = fallbackIcon,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(text = song?.title ?: "No Song Selected", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = song?.artist ?: "Unknown Artist", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            Spacer(modifier = Modifier.weight(1f))

            val safeDuration = if (realDuration > 0) realDuration else (song?.durationMs ?: 100L).coerceAtLeast(100L)

            Slider(
                value = sliderPosition.coerceIn(0f, safeDuration.toFloat()),
                onValueChange = { isDragging = true; sliderPosition = it },
                onValueChangeFinished = { isDragging = false; playerViewModel.seekTo(sliderPosition.toLong()) },
                valueRange = 0f..safeDuration.toFloat()
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = formatTime(sliderPosition.toLong()), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = formatTime(safeDuration), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // NEW: The Perfectly Balanced Media Controls Layout
            Box(
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp, top = 16.dp)
            ) {
                // Shuffle Button anchored to the far left edge
                IconButton(
                    onClick = { playerViewModel.toggleShuffle() },
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        Icons.Default.Shuffle,
                        "Shuffle",
                        tint = if (isShuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Core media controls grouped dead center
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { playerViewModel.skipPrevious() }) {
                        Icon(Icons.Default.SkipPrevious, "Previous", modifier = Modifier.size(48.dp))
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    FloatingActionButton(
                        onClick = { playerViewModel.togglePlayPause() },
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(80.dp)
                    ) {
                        Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play/Pause", modifier = Modifier.size(40.dp))
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    IconButton(onClick = { playerViewModel.skipNext() }) {
                        Icon(Icons.Default.SkipNext, "Next", modifier = Modifier.size(48.dp))
                    }
                }
            }
        }
    }
}