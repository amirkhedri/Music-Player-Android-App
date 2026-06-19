package com.example.musicplayer.ui.screens.player

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.musicplayer.viewmodel.FavoriteViewModel
import com.example.musicplayer.viewmodel.PlayerViewModel
import com.example.musicplayer.viewmodel.RepeatState

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
fun PlayerScreen(
    navController: NavController,
    playerViewModel: PlayerViewModel,
    favoriteViewModel: FavoriteViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val song by playerViewModel.currentSong.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val position by playerViewModel.currentPosition.collectAsState()
    val isShuffle by playerViewModel.isShuffleEnabled.collectAsState()
    val realDuration by playerViewModel.duration.collectAsState()

    // THE FIX: Reading the custom RepeatState
    val repeatState by playerViewModel.repeatState.collectAsState()

    val favoriteSongs by favoriteViewModel.favoriteSongs.collectAsState()
    val isFavorite = favoriteSongs.any { it.id == song?.id }

    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    val animatedProgress by animateFloatAsState(
        targetValue = sliderPosition,
        animationSpec = tween(
            durationMillis = if (isPlaying && !isDragging) 1000 else 0,
            easing = LinearEasing
        ),
        label = "smooth_progress"
    )

    LaunchedEffect(position) {
        if (!isDragging) sliderPosition = position.toFloat()
    }

    val fallbackIcon = rememberVectorPainter(Icons.Default.MusicNote)
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Now Playing", fontSize = 16.sp, fontWeight = FontWeight.Medium) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .shadow(elevation = 24.dp, shape = RoundedCornerShape(32.dp), spotColor = MaterialTheme.colorScheme.primary)
                        .clip(RoundedCornerShape(32.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(song?.albumArtUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Album Art",
                        contentScale = ContentScale.Crop,
                        error = fallbackIcon,
                        fallback = fallbackIcon,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                        Text(
                            text = song?.title ?: "No Song Selected",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = song?.artist ?: "Unknown Artist",
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                    }

                    IconButton(
                        onClick = { song?.let { favoriteViewModel.toggleFavorite(it) } },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                val safeDuration = if (realDuration > 0) realDuration else (song?.durationMs ?: 100L).coerceAtLeast(100L)
                val currentDisplayPosition = if (isDragging) sliderPosition else animatedProgress

                Column(modifier = Modifier.fillMaxWidth()) {
                    PremiumProgressBar(
                        progress = currentDisplayPosition,
                        max = safeDuration.toFloat(),
                        onProgressChanged = {
                            isDragging = true
                            sliderPosition = it
                        },
                        onDragFinished = {
                            isDragging = false
                            playerViewModel.seekTo(sliderPosition.toLong())
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = formatTime(currentDisplayPosition.toLong()), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        Text(text = formatTime(safeDuration), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    }
                }

                // THE FIX: Accurate Icon mapping for your specific states
                val repeatIcon = when (repeatState) {
                    RepeatState.ONCE -> Icons.Default.RepeatOne
                    RepeatState.TOTALLY -> Icons.Default.Repeat
                    else -> Icons.Default.Repeat
                }

                val repeatTint = if (repeatState == RepeatState.OFF) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.primary
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 48.dp, top = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(onClick = { playerViewModel.toggleShuffle() }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Shuffle, "Shuffle", tint = if (isShuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(28.dp))
                    }
                    IconButton(onClick = { playerViewModel.skipPrevious() }, modifier = Modifier.size(56.dp)) {
                        Icon(Icons.Default.SkipPrevious, "Previous", modifier = Modifier.size(40.dp))
                    }
                    FloatingActionButton(
                        onClick = { playerViewModel.togglePlayPause() },
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(80.dp).shadow(8.dp, CircleShape)
                    ) {
                        Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play/Pause", modifier = Modifier.size(40.dp))
                    }
                    IconButton(onClick = { playerViewModel.skipNext() }, modifier = Modifier.size(56.dp)) {
                        Icon(Icons.Default.SkipNext, "Next", modifier = Modifier.size(40.dp))
                    }

                    // THE FIX: Explicit Toast Messages
                    IconButton(
                        onClick = {
                            val newState = playerViewModel.toggleRepeatMode()
                            val msg = when(newState) {
                                RepeatState.ONCE -> "Repeat: Once"
                                RepeatState.TOTALLY -> "Repeat: Totally"
                                else -> "Repeat: Off"
                            }
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(repeatIcon, "Repeat", tint = repeatTint, modifier = Modifier.size(28.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumProgressBar(progress: Float, max: Float, onProgressChanged: (Float) -> Unit, onDragFinished: () -> Unit) {
    val fraction = if (max > 0) (progress / max).coerceIn(0f, 1f) else 0f

    val activeBrush = Brush.horizontalGradient(
        colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
    )

    Box(
        modifier = Modifier.fillMaxWidth().height(36.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(12.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)))
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(12.dp)
                .align(Alignment.CenterStart)
                .clip(CircleShape)
                .background(activeBrush)
        )

        Slider(
            value = progress,
            onValueChange = onProgressChanged,
            onValueChangeFinished = onDragFinished,
            valueRange = 0f..max,
            modifier = Modifier.fillMaxWidth().alpha(0f)
        )
    }
}