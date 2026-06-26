package com.example.musicplayer.ui.screens.player

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.createBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.musicplayer.viewmodel.EqualizerViewModel
import com.example.musicplayer.viewmodel.FavoriteViewModel
import com.example.musicplayer.viewmodel.PlayerViewModel
import com.example.musicplayer.viewmodel.RepeatState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ── Helpers ───────────────────────────────────────────────────────────────────

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
        hz < 100  -> "Sub"
        hz < 300  -> "Bass"
        hz < 2000 -> "Mid"
        hz < 5000 -> "Presence"
        else      -> "Treble"
    }
}

// ── Album-art dominant color extraction (no Palette dependency) ───────────────

/**
 * Loads the album-art bitmap via Coil, then samples pixels across a small grid
 * to compute an average dominant color.  No external Palette library needed.
 * Falls back to [defaultColor] on any error.
 */
suspend fun extractDominantColor(
    context: android.content.Context,
    uri: Any,
    defaultColor: Color
): Color = withContext(Dispatchers.IO) {
    try {
        val loader = coil.ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(uri)
            .size(64, 64)          // tiny – just enough for color sampling
            .allowHardware(false)  // required so we can read pixels
            .build()

        val result = loader.execute(request)
        val drawable = (result as? SuccessResult)?.drawable
            ?: return@withContext defaultColor

        // Convert drawable → software Bitmap
        val bitmap: Bitmap = if (drawable is BitmapDrawable && drawable.bitmap != null) {
            drawable.bitmap
        } else {
            val w = drawable.intrinsicWidth.coerceAtLeast(1)
            val h = drawable.intrinsicHeight.coerceAtLeast(1)
            val bmp = createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bmp)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bmp
        }

        // Sample a 6×6 grid of pixels, accumulate RGB, return average
        val w = bitmap.width
        val h = bitmap.height
        var r = 0L; var g = 0L; var b = 0L; var count = 0
        val steps = 6
        for (xi in 0 until steps) {
            for (yi in 0 until steps) {
                val px = ((xi / (steps - 1).toFloat()) * (w - 1)).toInt().coerceIn(0, w - 1)
                val py = ((yi / (steps - 1).toFloat()) * (h - 1)).toInt().coerceIn(0, h - 1)
                val pixel = bitmap.getPixel(px, py)
                r += android.graphics.Color.red(pixel)
                g += android.graphics.Color.green(pixel)
                b += android.graphics.Color.blue(pixel)
                count++
            }
        }
        if (count == 0) return@withContext defaultColor
        Color(
            red   = (r / count).toInt().coerceIn(0, 255),
            green = (g / count).toInt().coerceIn(0, 255),
            blue  = (b / count).toInt().coerceIn(0, 255)
        )
    } catch (_: Exception) {
        defaultColor
    }
}

// ── PlayerScreen ──────────────────────────────────────────────────────────────

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

    // ── Dynamic background color from album art ───────────────────────────────
    val surfaceColor = MaterialTheme.colorScheme.surface
    var dominantColor by remember { mutableStateOf(surfaceColor) }

    LaunchedEffect(song?.albumArtUri) {
        val uri = song?.albumArtUri
        if (uri == null || uri.toString().isEmpty()) {
            dominantColor = surfaceColor
            return@LaunchedEffect
        }
        dominantColor = extractDominantColor(context, uri, surfaceColor)
    }

    // Smoothly animate between album art colors as the song changes
    val animatedDominant by animateColorAsState(
        targetValue = dominantColor,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "dominant_color"
    )

    // Derive darker variant for gradient stops
    val dominantDark = animatedDominant.copy(
        red   = (animatedDominant.red   * 0.35f).coerceIn(0f, 1f),
        green = (animatedDominant.green * 0.35f).coerceIn(0f, 1f),
        blue  = (animatedDominant.blue  * 0.35f).coerceIn(0f, 1f),
        alpha = 1f
    )
    val dominantMid = animatedDominant.copy(alpha = 0.55f)

    // Text/icon color: black on light backgrounds, white on dark
    val onDominant = if (animatedDominant.luminance() > 0.4f) Color.Black else Color.White

    // Glass surface colors for controls
    val glassLight  = Color.White.copy(alpha = 0.15f)
    val glassBorder = Color.White.copy(alpha = 0.25f)

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(dominantDark, dominantMid, dominantDark)
    )

    val fallbackIcon = rememberVectorPainter(Icons.Default.MusicNote)

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Now Playing",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = onDominant
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = "Back",
                            tint = onDominant
                        )
                    }
                },
                actions = {
                    // Speed control
                    var showSpeedMenu by remember { mutableStateOf(false) }
                    val currentSpeed by playerViewModel.playbackSpeed.collectAsState()

                    Box {
                        Surface(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .clip(CircleShape)
                                .clickable { showSpeedMenu = true },
                            color = glassLight,
                            shape = CircleShape
                        ) {
                            Text(
                                text = "${currentSpeed}X",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = onDominant
                            )
                        }
                        DropdownMenu(
                            expanded = showSpeedMenu,
                            onDismissRequest = { showSpeedMenu = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "${speed}x",
                                            fontWeight = if (speed == currentSpeed) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = { playerViewModel.setPlaybackSpeed(speed); showSpeedMenu = false },
                                    trailingIcon = {
                                        if (speed == currentSpeed)
                                            Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                )
                            }
                        }
                    }

                    // Equalizer button
                    IconButton(
                        onClick = { showEqSheet = true },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(CircleShape)
                            .background(glassLight)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = "Equalizer", tint = onDominant)
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
        ) {
            // Dark overlay for depth / readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.18f))
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                // Album art
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .shadow(
                            elevation = 32.dp,
                            shape = RoundedCornerShape(32.dp),
                            spotColor = animatedDominant
                        )
                        .clip(RoundedCornerShape(32.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
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

                // Song title + favorite
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
                            color = onDominant,
                            modifier = Modifier.basicMarquee()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = song?.artist ?: "Unknown Artist",
                            fontSize = 18.sp,
                            color = onDominant.copy(alpha = 0.75f),
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                    }

                    // Glassy favorite button
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(glassLight)
                            .border(1.dp, glassBorder, CircleShape)
                            .clickable { song?.let { favoriteViewModel.toggleFavorite(it) } },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) Color.Red else onDominant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Progress bar
                val safeDuration = if (realDuration > 0) realDuration
                else (song?.durationMs ?: 100L).coerceAtLeast(100L)
                val currentDisplayPosition = if (isDragging) sliderPosition else animatedProgress

                Column(modifier = Modifier.fillMaxWidth()) {
                    GlassyProgressBar(
                        progress = currentDisplayPosition,
                        max = safeDuration.toFloat(),
                        trackColor = onDominant.copy(alpha = 0.2f),
                        fillColor = onDominant,
                        onProgressChanged = { isDragging = true; sliderPosition = it },
                        onDragFinished = {
                            isDragging = false
                            playerViewModel.seekTo(sliderPosition.toLong())
                        }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            formatTime(currentDisplayPosition.toLong()),
                            fontSize = 12.sp,
                            color = onDominant.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            formatTime(safeDuration),
                            fontSize = 12.sp,
                            color = onDominant.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Transport controls
                val repeatIcon = when (repeatState) {
                    RepeatState.ONCE   -> Icons.Default.RepeatOne
                    RepeatState.TOTALLY -> Icons.Default.Repeat
                    else               -> Icons.Default.Repeat
                }
                val repeatTint = if (repeatState == RepeatState.OFF)
                    onDominant.copy(alpha = 0.5f) else onDominant

                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 48.dp, top = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Shuffle
                    GlassyIconButton(
                        onClick = { playerViewModel.toggleShuffle() },
                        size = 48,
                        glassColor = if (isShuffle) onDominant.copy(alpha = 0.25f) else glassLight,
                        borderColor = if (isShuffle) onDominant.copy(alpha = 0.5f) else glassBorder
                    ) {
                        Icon(
                            Icons.Default.Shuffle, "Shuffle",
                            tint = if (isShuffle) onDominant else onDominant.copy(alpha = 0.6f),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Previous
                    GlassyIconButton(
                        onClick = { playerViewModel.skipPrevious() },
                        size = 56,
                        glassColor = glassLight,
                        borderColor = glassBorder
                    ) {
                        Icon(Icons.Default.SkipPrevious, "Previous", tint = onDominant, modifier = Modifier.size(32.dp))
                    }

                    // Play / Pause – solid button, pops against the glass
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .shadow(16.dp, CircleShape, spotColor = animatedDominant)
                            .clip(CircleShape)
                            .background(onDominant)
                            .clickable { playerViewModel.togglePlayPause() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            "Play/Pause",
                            tint = animatedDominant,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    // Next
                    GlassyIconButton(
                        onClick = { playerViewModel.skipNext() },
                        size = 56,
                        glassColor = glassLight,
                        borderColor = glassBorder
                    ) {
                        Icon(Icons.Default.SkipNext, "Next", tint = onDominant, modifier = Modifier.size(32.dp))
                    }

                    // Repeat
                    GlassyIconButton(
                        onClick = { playerViewModel.toggleRepeatMode() },
                        size = 48,
                        glassColor = if (repeatState != RepeatState.OFF) onDominant.copy(alpha = 0.25f) else glassLight,
                        borderColor = if (repeatState != RepeatState.OFF) onDominant.copy(alpha = 0.5f) else glassBorder
                    ) {
                        Icon(repeatIcon, "Repeat", tint = repeatTint, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }

        // Equalizer bottom sheet
        if (showEqSheet) {
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

// ── Glassy circular icon button ───────────────────────────────────────────────

@Composable
fun GlassyIconButton(
    onClick: () -> Unit,
    size: Int,
    glassColor: Color,
    borderColor: Color,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(glassColor)
            .border(1.dp, borderColor, CircleShape)
            // Use plain clickable — no deprecated rememberRipple needed
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = Color.White)
            ) { onClick() },
        contentAlignment = Alignment.Center,
        content = content
    )
}

// ── Progress bar (glassy style) ───────────────────────────────────────────────

@Composable
fun GlassyProgressBar(
    progress: Float,
    max: Float,
    trackColor: Color,
    fillColor: Color,
    onProgressChanged: (Float) -> Unit,
    onDragFinished: () -> Unit
) {
    val fraction = if (max > 0) (progress / max).coerceIn(0f, 1f) else 0f
    Box(modifier = Modifier.fillMaxWidth().height(36.dp), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape).background(trackColor))
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(5.dp)
                .align(Alignment.CenterStart)
                .clip(CircleShape)
                .background(fillColor)
        )
        Slider(
            value = progress,
            onValueChange = onProgressChanged,
            onValueChangeFinished = onDragFinished,
            valueRange = 0f..max,
            modifier = Modifier.fillMaxWidth().alpha(0f),
            colors = SliderDefaults.colors(
                thumbColor = fillColor,
                activeTrackColor = fillColor,
                inactiveTrackColor = trackColor
            )
        )
    }
}

// ── Equalizer ─────────────────────────────────────────────────────────────────

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
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioNoBouncy
        ),
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
                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                    )
                )
                .shadow(
                    if (isEnabled && animatedFraction > 0.1f) 8.dp else 0.dp,
                    spotColor = MaterialTheme.colorScheme.primary
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (heightDp - thumbHeight) * (1f - animatedFraction))
                .width(40.dp)
                .height(thumbHeight)
                .shadow(
                    elevation = if (isEnabled) 12.dp else 0.dp,
                    shape = RoundedCornerShape(6.dp),
                    spotColor = MaterialTheme.colorScheme.primary
                )
                .clip(RoundedCornerShape(6.dp))
                .background(
                    if (isEnabled) MaterialTheme.colorScheme.surface
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .border(
                    2.dp,
                    if (isEnabled) MaterialTheme.colorScheme.primary else Color.Gray,
                    RoundedCornerShape(6.dp)
                )
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

@Composable
fun EqualizerContent(viewModel: EqualizerViewModel) {
    val isEnabled by viewModel.isEqEnabled.collectAsState()
    val presets by viewModel.presets.collectAsState()
    val currentPreset by viewModel.currentPreset.collectAsState()
    val bands by viewModel.bands.collectAsState()
    val minLevel by viewModel.minBandLevel.collectAsState()
    val maxLevel by viewModel.maxBandLevel.collectAsState()

    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 48.dp, top = 8.dp),
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

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                "PRESETS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 12.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(presets) { preset ->
                    val isSelected = preset == currentPreset
                    val animatedBorderColor by animateColorAsState(
                        if (isSelected && isEnabled) MaterialTheme.colorScheme.primary else Color.Transparent,
                        label = "border"
                    )
                    val animatedTextColor by animateColorAsState(
                        when {
                            isSelected && isEnabled -> MaterialTheme.colorScheme.primary
                            isEnabled              -> MaterialTheme.colorScheme.onSurface
                            else                   -> Color.Gray
                        },
                        label = "text"
                    )
                    val animatedBgColor by animateColorAsState(
                        if (isSelected && isEnabled)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isEnabled) 0.5f else 0.2f),
                        label = "bg"
                    )

                    Box(
                        modifier = Modifier
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
                            fontSize = 14.sp,
                            textDecoration = TextDecoration.None
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            bands.forEach { band ->
                val dbValue = band.level / 100
                val dbString = if (dbValue > 0) "+$dbValue" else "$dbValue"
                val animatedColor by animateColorAsState(
                    if (isEnabled && dbValue != 0) MaterialTheme.colorScheme.primary else Color.Gray,
                    label = "dbColor"
                )
                val friendlyLabel = getFrequencyLabel(band.centerFreqHz)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isEnabled) 0.5f else 0.1f))
                        .padding(vertical = 16.dp, horizontal = 4.dp)
                ) {
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
                    Text(text = "Hz", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}