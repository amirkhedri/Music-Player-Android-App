package com.example.musicplayer.ui.screens.player

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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

fun getFrequencyLabel(hz: Int): String {
    return when {
        hz < 100 -> "Sub"
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
// CUSTOM ANIMATED STUDIO FADER COMPONENT
// ------------------------------------------------------------------
@Composable
fun StudioFader(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    isEnabled: Boolean,
    onValueChange: (Float) -> Unit
) {
    val heightDp = 180.dp
    val widthDp = 44.dp
    val thumbHeight = 24.dp
    val density = LocalDensity.current
    val heightPx = with(density) { heightDp.toPx() }

    val fraction = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)

    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "fader_animation"
    )

    Box(
        modifier = Modifier
            .width(widthDp)
            .height(heightDp)
            .pointerInput(isEnabled) {
                if (!isEnabled) return@pointerInput
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        val newFraction = 1f - (offset.y / heightPx).coerceIn(0f, 1f)
                        val newValue = valueRange.start + (newFraction * (valueRange.endInclusive - valueRange.start))
                        onValueChange(newValue)
                    },
                    onVerticalDrag = { change, _ ->
                        val newFraction = 1f - (change.position.y / heightPx).coerceIn(0f, 1f)
                        val newValue = valueRange.start + (newFraction * (valueRange.endInclusive - valueRange.start))
                        onValueChange(newValue)
                    }
                )
            }
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .width(8.dp)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .width(8.dp)
                .fillMaxHeight(animatedFraction)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                    )
                )
                .shadow(if (isEnabled && animatedFraction > 0.1f) 8.dp else 0.dp, spotColor = MaterialTheme.colorScheme.primary)
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (heightDp - thumbHeight) * (1f - animatedFraction))
                .width(40.dp)
                .height(thumbHeight)
                .shadow(elevation = if (isEnabled) 12.dp else 0.dp, shape = RoundedCornerShape(6.dp), spotColor = MaterialTheme.colorScheme.primary)
                .clip(RoundedCornerShape(6.dp))
                .background(if (isEnabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant)
                .border(2.dp, if (isEnabled) MaterialTheme.colorScheme.primary else Color.Gray, RoundedCornerShape(6.dp))
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(20.dp)
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(if (isEnabled) MaterialTheme.colorScheme.primary else Color.Gray)
            )
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 48.dp, top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title & Switch Header
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

        // THE FIX: Premium Horizontal Preset Bank (Replaces Dropdown)
        Column(modifier = Modifier.fillMaxWidth()) {
            // THE FIX: Chaining the padding modifiers instead of mixing them
            Text(
                text = "PRESETS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                letterSpacing = 1.5.sp,
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 12.dp)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(presets) { preset ->
                    val isSelected = preset == currentPreset
                    val animatedBorderColor by animateColorAsState(
                        targetValue = if (isSelected && isEnabled) MaterialTheme.colorScheme.primary else Color.Transparent,
                        label = "border_color"
                    )
                    val animatedTextColor by animateColorAsState(
                        targetValue = if (isSelected && isEnabled) MaterialTheme.colorScheme.primary else if (isEnabled) MaterialTheme.colorScheme.onSurface else Color.Gray,
                        label = "text_color"
                    )
                    val animatedBgColor by animateColorAsState(
                        targetValue = if (isSelected && isEnabled) Color.Black.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if(isEnabled) 0.5f else 0.2f),
                        label = "bg_color"
                    )

                    Box(
                        modifier = Modifier
                            .shadow(if (isSelected && isEnabled) 8.dp else 0.dp, RoundedCornerShape(20.dp), spotColor = MaterialTheme.colorScheme.primary)
                            .clip(RoundedCornerShape(20.dp))
                            .background(animatedBgColor)
                            .border(2.dp, animatedBorderColor, RoundedCornerShape(20.dp))
                            .clickable(enabled = isEnabled) { viewModel.setPreset(preset) }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = preset,
                            color = animatedTextColor,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Studio Channel Strips (Frequency Bands)
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            bands.forEach { band ->
                val dbValue = band.level / 100
                val dbString = if (dbValue > 0) "+$dbValue" else "$dbValue"
                val animatedColor by animateColorAsState(if (isEnabled && dbValue != 0) MaterialTheme.colorScheme.primary else Color.Gray, label = "dbColor")

                val friendlyLabel = getFrequencyLabel(band.centerFreqHz)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isEnabled) 0.5f else 0.1f))
                        .padding(vertical = 16.dp, horizontal = 4.dp)
                ) {

                    // Premium Glassmorphic LCD Readout
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.8f))
                            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dbString,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            color = animatedColor
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // The Custom Animated Studio Fader
                    StudioFader(
                        value = band.level.toFloat(),
                        valueRange = minLevel.toFloat()..maxLevel.toFloat(),
                        isEnabled = isEnabled,
                        onValueChange = { viewModel.setBandLevel(band.index, it.toInt().toShort()) }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = friendlyLabel,
                        fontSize = 11.sp,
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