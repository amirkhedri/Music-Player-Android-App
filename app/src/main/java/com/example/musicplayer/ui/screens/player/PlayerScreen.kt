package com.example.musicplayer.ui.screens.player

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.Player
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.musicplayer.viewmodel.EqualizerViewModel
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

// NEW: Helper function to translate raw Hz into human-readable terms
fun getFrequencyLabel(hz: Int): String {
    return when {
        hz < 100 -> "Sub Bass"
        hz < 300 -> "Bass"
        hz < 2000 -> "Mid"
        hz < 5000 -> "Presence"
        else -> "Treble"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    navController: NavController,
    playerViewModel: PlayerViewModel,
    favoriteViewModel: FavoriteViewModel = hiltViewModel(),
    equalizerViewModel: EqualizerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val song by playerViewModel.currentSong.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val position by playerViewModel.currentPosition.collectAsState()
    val isShuffle by playerViewModel.isShuffleEnabled.collectAsState()
    val realDuration by playerViewModel.duration.collectAsState()
    val repeatState by playerViewModel.repeatState.collectAsState()

    val favoriteSongs by favoriteViewModel.favoriteSongs.collectAsState()
    val isFavorite = favoriteSongs.any { it.id == song?.id }

    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    var showEqSheet by remember { mutableStateOf(false) }

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
                actions = {
                    IconButton(
                        onClick = { showEqSheet = true },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = "Equalizer", tint = MaterialTheme.colorScheme.primary)
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

                    IconButton(
                        onClick = { playerViewModel.toggleRepeatMode() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(repeatIcon, "Repeat", tint = repeatTint, modifier = Modifier.size(28.dp))
                    }
                }
            }
        }

        if (showEqSheet) {
            @OptIn(ExperimentalMaterial3Api::class)
            ModalBottomSheet(
                onDismissRequest = { showEqSheet = false },
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 16.dp,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                EqualizerContent(equalizerViewModel)
            }
        }
    }
}

// ------------------------------------------------------------------
// PREMIUM STUDIO MIXER EQUALIZER UI
// ------------------------------------------------------------------

@Composable
fun EqualizerContent(viewModel: EqualizerViewModel) {
    val isEnabled by viewModel.isEqEnabled.collectAsState()
    val presets by viewModel.presets.collectAsState()
    val currentPreset by viewModel.currentPreset.collectAsState()
    val bands by viewModel.bands.collectAsState()
    val minLevel by viewModel.minBandLevel.collectAsState()
    val maxLevel by viewModel.maxBandLevel.collectAsState()

    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 48.dp, top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Audio Mixer", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            Switch(
                checked = isEnabled,
                onCheckedChange = { viewModel.toggleEqualizer(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(enabled = isEnabled) { expanded = true },
                color = if (isEnabled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("PRESET", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text(
                            text = currentPreset ?: "Select Preset",
                            color = if (isEnabled) MaterialTheme.colorScheme.onSurface else Color.Gray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                    Icon(Icons.Default.ArrowDropDown, null, tint = if (isEnabled) MaterialTheme.colorScheme.primary else Color.Gray)
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.85f).background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                presets.forEach { preset ->
                    DropdownMenuItem(
                        text = { Text(preset, fontWeight = if (preset == currentPreset) FontWeight.Bold else FontWeight.Normal) },
                        onClick = {
                            viewModel.setPreset(preset)
                            expanded = false
                        },
                        trailingIcon = {
                            if (preset == currentPreset) {
                                Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Studio Channel Strips (Frequency Bands)
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            bands.forEach { band ->
                val dbValue = band.level / 100
                val dbString = if (dbValue > 0) "+$dbValue" else "$dbValue"

                // NEW: Calculate the dynamic label (Bass, Mid, Treble)
                val friendlyLabel = getFrequencyLabel(band.centerFreqHz)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isEnabled) 0.4f else 0.1f))
                        .padding(vertical = 16.dp, horizontal = 8.dp)
                ) {

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.background.copy(alpha = if (isEnabled) 0.8f else 0.4f))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dbString,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (isEnabled) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Slider(
                            value = band.level.toFloat(),
                            onValueChange = { viewModel.setBandLevel(band.index, it.toInt().toShort()) },
                            valueRange = minLevel.toFloat()..maxLevel.toFloat(),
                            enabled = isEnabled,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier
                                .requiredWidth(160.dp)
                                .requiredHeight(40.dp)
                                .graphicsLayer { rotationZ = -90f }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // NEW: Displaying the friendly text label (Bass, Treble, etc)
                    Text(
                        text = friendlyLabel,
                        fontSize = 10.sp,
                        color = if (isEnabled) MaterialTheme.colorScheme.primary else Color.Gray,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = if (band.centerFreqHz >= 1000) "${band.centerFreqHz / 1000}k" else "${band.centerFreqHz}",
                        fontSize = 14.sp,
                        color = if (isEnabled) MaterialTheme.colorScheme.onSurface else Color.Gray,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Hz",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------
// PROGRESS BAR COMPONENT
// ------------------------------------------------------------------

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