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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
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
import com.example.musicplayer.viewmodel.ThemeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

suspend fun extractDominantColor(context: android.content.Context, uri: Any, defaultColor: Color): Color = withContext(Dispatchers.IO) {
    try {
        val loader = coil.ImageLoader(context)
        val request = ImageRequest.Builder(context).data(uri).size(64, 64).allowHardware(false).build()
        val result = loader.execute(request)
        val drawable = (result as? SuccessResult)?.drawable ?: return@withContext defaultColor

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    navController: NavController,
    playerViewModel: PlayerViewModel,
    favoriteViewModel: FavoriteViewModel = hiltViewModel(),
    equalizerViewModel: EqualizerViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val song by playerViewModel.currentSong.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val position by playerViewModel.currentPosition.collectAsState()
    val isShuffle by playerViewModel.isShuffleEnabled.collectAsState()
    val realDuration by playerViewModel.duration.collectAsState()
    val repeatState by playerViewModel.repeatState.collectAsState()
    val isGlassy by themeViewModel.isGlassyMode.collectAsState()
    val isDarkMode by themeViewModel.isDarkMode.collectAsState()

    val favoriteSongs by favoriteViewModel.favoriteSongs.collectAsState()
    val isFavorite = favoriteSongs.any { it.id == song?.id }

    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var showEqSheet by remember { mutableStateOf(false) }

    val animatedProgress by animateFloatAsState(
        targetValue = sliderPosition,
        animationSpec = tween(durationMillis = if (isPlaying && !isDragging) 1000 else 0, easing = LinearEasing),
        label = "smooth_progress"
    )

    LaunchedEffect(position) {
        if (!isDragging) sliderPosition = position.toFloat()
    }

    val surfaceColor = MaterialTheme.colorScheme.surface
    var dominantColor by remember { mutableStateOf(surfaceColor) }

    LaunchedEffect(song?.albumArtUri) {
        val uri = song?.albumArtUri
        if (uri == null || uri.toString().isEmpty()) { dominantColor = surfaceColor; return@LaunchedEffect }
        dominantColor = extractDominantColor(context, uri, surfaceColor)
    }

    val animatedDominant by animateColorAsState(targetValue = dominantColor, animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing), label = "dominant_color")

    val dominantDark = animatedDominant.copy(red = (animatedDominant.red * 0.25f).coerceIn(0f, 1f), green = (animatedDominant.green * 0.25f).coerceIn(0f, 1f), blue = (animatedDominant.blue * 0.25f).coerceIn(0f, 1f), alpha = 1f)
    val dominantMid = animatedDominant.copy(alpha = 0.7f)
    val onDominant = if (animatedDominant.luminance() > 0.4f) Color.Black else Color.White

    val bgColorModifier = if (isGlassy) Modifier.background(Brush.radialGradient(colors = listOf(dominantMid, dominantDark, Color.Black.copy(alpha = 0.9f)), radius = 1800f)) else Modifier.background(MaterialTheme.colorScheme.background)
    val textColor = if (isGlassy) onDominant else MaterialTheme.colorScheme.onBackground
    val btnBgColor = if (isGlassy) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
    val btnBorderColor = if (isGlassy) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.outlineVariant
    val progressTrackColor = if (isGlassy) onDominant.copy(alpha = 0.2f) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
    val progressFillColor = if (isGlassy) onDominant else MaterialTheme.colorScheme.primary

    val neonCyan = Color(0xFF00E5FF)
    val deepPurple = Color(0xFF6200EA)

    // Dynamic Active Accent Logic
    val activeAccentColor = when {
        isGlassy -> onDominant
        isDarkMode -> neonCyan
        else -> deepPurple
    }
    val activeAccentGlow = if (isDarkMode && !isGlassy) 16.dp else 0.dp

    val fallbackIcon = rememberVectorPainter(Icons.Default.MusicNote)

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Now Playing", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = textColor) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Back", tint = textColor) } },
                actions = {
                    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                    val currentSpeed by playerViewModel.playbackSpeed.collectAsState()

                    Surface(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .shadow(elevation = if(currentSpeed != 1.0f) activeAccentGlow else 0.dp, shape = CircleShape, spotColor = activeAccentColor, ambientColor = activeAccentColor)
                            .clip(CircleShape)
                            .clickable {
                                val nextIndex = (speeds.indexOf(currentSpeed) + 1) % speeds.size
                                playerViewModel.setPlaybackSpeed(speeds[nextIndex])
                            },
                        color = if (currentSpeed != 1.0f && !isGlassy) activeAccentColor else btnBgColor,
                        shape = CircleShape
                    ) {
                        Text(
                            text = "${currentSpeed}X",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            fontSize = 13.sp, fontWeight = FontWeight.ExtraBold,
                            color = if (currentSpeed != 1.0f && !isGlassy) (if(isDarkMode) Color.Black else Color.White) else textColor
                        )
                    }

                    IconButton(
                        onClick = { showEqSheet = true },
                        modifier = Modifier.padding(end = 8.dp).clip(CircleShape).background(btnBgColor)
                    ) { Icon(Icons.Default.Tune, contentDescription = "Equalizer", tint = textColor) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().then(bgColorModifier)) {
            if (isGlassy) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.1f)))
            }

            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                Box(
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f).shadow(elevation = 32.dp, shape = RoundedCornerShape(32.dp), spotColor = if(isGlassy) animatedDominant else MaterialTheme.colorScheme.primary).clip(RoundedCornerShape(32.dp)).background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(model = ImageRequest.Builder(context).data(song?.albumArtUri).crossfade(true).build(), contentDescription = "Album Art", contentScale = ContentScale.Crop, error = fallbackIcon, fallback = fallbackIcon, modifier = Modifier.fillMaxSize())
                }

                Spacer(modifier = Modifier.height(48.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                        Text(text = song?.title ?: "No Song Selected", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, color = textColor, modifier = Modifier.basicMarquee())
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = song?.artist ?: "Unknown Artist", fontSize = 18.sp, color = textColor.copy(alpha = 0.75f), maxLines = 1, modifier = Modifier.basicMarquee())
                    }
                    Box(
                        modifier = Modifier.size(48.dp).clip(CircleShape).background(btnBgColor).border(1.dp, btnBorderColor, CircleShape).clickable { song?.let { favoriteViewModel.toggleFavorite(it) } },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, contentDescription = "Favorite", tint = if (isFavorite) Color.Red else textColor, modifier = Modifier.size(24.dp))
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                val safeDuration = if (realDuration > 0) realDuration else (song?.durationMs ?: 100L).coerceAtLeast(100L)
                val currentDisplayPosition = if (isDragging) sliderPosition else animatedProgress

                Column(modifier = Modifier.fillMaxWidth()) {
                    GlassyProgressBar(
                        progress = currentDisplayPosition, max = safeDuration.toFloat(), trackColor = progressTrackColor, fillColor = progressFillColor,
                        onProgressChanged = { isDragging = true; sliderPosition = it }, onDragFinished = { isDragging = false; playerViewModel.seekTo(sliderPosition.toLong()) }
                    )
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(formatTime(currentDisplayPosition.toLong()), fontSize = 12.sp, color = textColor.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
                        Text(formatTime(safeDuration), fontSize = 12.sp, color = textColor.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
                    }
                }

                val repeatIcon = when (repeatState) { RepeatState.ONCE -> Icons.Default.RepeatOne; RepeatState.TOTALLY -> Icons.Default.Repeat; else -> Icons.Default.Repeat }
                val repeatTint = if (repeatState == RepeatState.OFF) textColor.copy(alpha = 0.5f) else textColor

                val controlActiveBg = if (isGlassy) textColor.copy(alpha = 0.25f) else MaterialTheme.colorScheme.primaryContainer
                val controlActiveBorder = if (isGlassy) textColor.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary
                val controlActiveTint = if (isGlassy) textColor else MaterialTheme.colorScheme.primary

                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 48.dp, top = 24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
                    GlassyIconButton(onClick = { playerViewModel.toggleShuffle() }, size = 48, glassColor = if (isShuffle) controlActiveBg else btnBgColor, borderColor = if (isShuffle) controlActiveBorder else btnBorderColor) {
                        Icon(Icons.Default.Shuffle, "Shuffle", tint = if (isShuffle) controlActiveTint else textColor.copy(alpha = 0.6f), modifier = Modifier.size(24.dp))
                    }
                    GlassyIconButton(onClick = { playerViewModel.skipPrevious() }, size = 56, glassColor = btnBgColor, borderColor = btnBorderColor) {
                        Icon(Icons.Default.SkipPrevious, "Previous", tint = textColor, modifier = Modifier.size(32.dp))
                    }
                    Box(
                        modifier = Modifier.size(80.dp).shadow(16.dp, CircleShape, spotColor = if(isGlassy) animatedDominant else MaterialTheme.colorScheme.primary).clip(CircleShape).background(if(isGlassy) textColor else MaterialTheme.colorScheme.primary).clickable { playerViewModel.togglePlayPause() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play/Pause", tint = if(isGlassy) animatedDominant else MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(40.dp))
                    }
                    GlassyIconButton(onClick = { playerViewModel.skipNext() }, size = 56, glassColor = btnBgColor, borderColor = btnBorderColor) {
                        Icon(Icons.Default.SkipNext, "Next", tint = textColor, modifier = Modifier.size(32.dp))
                    }
                    GlassyIconButton(onClick = { playerViewModel.toggleRepeatMode() }, size = 48, glassColor = if (repeatState != RepeatState.OFF) controlActiveBg else btnBgColor, borderColor = if (repeatState != RepeatState.OFF) controlActiveBorder else btnBorderColor) {
                        Icon(repeatIcon, "Repeat", tint = repeatTint, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }

        if (showEqSheet) {
            // Eq Bottom sheet background made solid and legible instead of fully transparent
            val sheetContainerColor = if (isGlassy) dominantDark else MaterialTheme.colorScheme.surface
            ModalBottomSheet(
                onDismissRequest = { showEqSheet = false },
                containerColor = sheetContainerColor,
                tonalElevation = 16.dp,
                dragHandle = { BottomSheetDefaults.DragHandle(color = if(isGlassy) onDominant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant) }
            ) {
                EqualizerContent(equalizerViewModel, isGlassy, isDarkMode, activeAccentColor, dominantDark, dominantMid, onDominant)
            }
        }
    }
}

@Composable
fun GlassyIconButton(onClick: () -> Unit, size: Int, glassColor: Color, borderColor: Color, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier.size(size.dp).clip(CircleShape).background(glassColor).border(1.dp, borderColor, CircleShape).clickable(interactionSource = remember { MutableInteractionSource() }, indication = ripple(bounded = true, color = Color.White)) { onClick() },
        contentAlignment = Alignment.Center, content = content
    )
}

@Composable
fun GlassyProgressBar(progress: Float, max: Float, trackColor: Color, fillColor: Color, onProgressChanged: (Float) -> Unit, onDragFinished: () -> Unit) {
    val fraction = if (max > 0) (progress / max).coerceIn(0f, 1f) else 0f
    Box(modifier = Modifier.fillMaxWidth().height(36.dp), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape).background(trackColor))
        Box(modifier = Modifier.fillMaxWidth(fraction).height(5.dp).align(Alignment.CenterStart).clip(CircleShape).background(fillColor))
        Slider(
            value = progress, onValueChange = onProgressChanged, onValueChangeFinished = onDragFinished, valueRange = 0f..max,
            modifier = Modifier.fillMaxWidth().alpha(0f), colors = SliderDefaults.colors(thumbColor = fillColor, activeTrackColor = fillColor, inactiveTrackColor = trackColor)
        )
    }
}

@Composable
fun StudioFader(value: Float, valueRange: ClosedFloatingPointRange<Float>, isEnabled: Boolean, isGlassy: Boolean, isDarkMode: Boolean, activeColor: Color, inactiveColor: Color, onValueChange: (Float) -> Unit) {
    val heightDp = 180.dp
    val widthDp = 44.dp
    val thumbHeight = 24.dp
    val density = LocalDensity.current
    val heightPx = with(density) { heightDp.toPx() }

    val fraction = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
    val animatedFraction by animateFloatAsState(targetValue = fraction, animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy), label = "fader_animation")

    val glowElevation = if (isEnabled && isDarkMode && !isGlassy) 24.dp else if (isEnabled && !isGlassy) 12.dp else 0.dp

    Box(
        modifier = Modifier.width(widthDp).height(heightDp).pointerInput(isEnabled) {
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
        Box(modifier = Modifier.align(Alignment.Center).width(8.dp).fillMaxHeight().clip(CircleShape).background(inactiveColor.copy(alpha = 0.2f)))

        Box(
            modifier = Modifier.align(Alignment.BottomCenter).width(8.dp).fillMaxHeight(animatedFraction).clip(CircleShape).background(Brush.verticalGradient(listOf(activeColor, activeColor.copy(alpha = 0.5f))))
                .shadow(elevation = glowElevation, spotColor = activeColor, ambientColor = activeColor)
        )

        Box(
            modifier = Modifier.align(Alignment.TopCenter).offset(y = (heightDp - thumbHeight) * (1f - animatedFraction)).width(40.dp).height(thumbHeight)
                .shadow(elevation = glowElevation, shape = RoundedCornerShape(6.dp), spotColor = activeColor, ambientColor = activeColor)
                .clip(RoundedCornerShape(6.dp))
                .background(if (isEnabled) activeColor else inactiveColor.copy(alpha = 0.5f))
                .border(2.dp, if (isEnabled) activeColor.copy(alpha = 0.8f) else Color.Transparent, RoundedCornerShape(6.dp))
        ) {
            Box(modifier = Modifier.align(Alignment.Center).width(20.dp).height(3.dp).clip(CircleShape).background(if (isEnabled) Color.Black.copy(alpha=0.6f) else Color.Gray))
        }
    }
}

@Composable
fun EqualizerContent(viewModel: EqualizerViewModel, isGlassy: Boolean, isDarkMode: Boolean, activeAccentColor: Color, dominantDark: Color, dominantMid: Color, onDominant: Color) {
    val isEnabled by viewModel.isEqEnabled.collectAsState()
    val presets by viewModel.presets.collectAsState()
    val currentPreset by viewModel.currentPreset.collectAsState()
    val bands by viewModel.bands.collectAsState()
    val minLevel by viewModel.minBandLevel.collectAsState()
    val maxLevel by viewModel.maxBandLevel.collectAsState()

    val primaryTextColor = if (isGlassy) onDominant else MaterialTheme.colorScheme.onSurface
    val unselectedColor = if (isGlassy) onDominant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

    // Eq background stripped of transparent sweep to ensure reliable solid contrast
    val eqBgModifier = if (isGlassy) {
        Modifier.background(dominantDark)
    } else {
        Modifier.background(MaterialTheme.colorScheme.surface)
    }

    Column(
        modifier = Modifier.fillMaxWidth().then(eqBgModifier).padding(bottom = 48.dp, top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Audio Mixer", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = primaryTextColor)
            Switch(
                checked = isEnabled, onCheckedChange = { viewModel.toggleEqualizer(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = activeAccentColor,
                    checkedTrackColor = activeAccentColor.copy(alpha = 0.5f),
                    uncheckedThumbColor = unselectedColor,
                    uncheckedTrackColor = if (isGlassy) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.shadow(elevation = if (isEnabled && isDarkMode && !isGlassy) 16.dp else 0.dp, spotColor = activeAccentColor, ambientColor = activeAccentColor)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            Text("PRESETS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = unselectedColor, letterSpacing = 1.5.sp, modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 12.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                items(presets) { preset ->
                    val isSelected = preset == currentPreset
                    val animatedBorderColor by animateColorAsState(if (isSelected && isEnabled) activeAccentColor else Color.Transparent, label = "border")
                    val animatedTextColor by animateColorAsState(when { isSelected && isEnabled -> if(isGlassy) dominantDark else (if(isDarkMode) Color.Black else Color.White); isEnabled -> primaryTextColor; else -> unselectedColor }, label = "text")
                    val animatedBgColor by animateColorAsState(if (isSelected && isEnabled) activeAccentColor else if(isGlassy) Color.White.copy(alpha = if (isEnabled) 0.15f else 0.05f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if(isEnabled) 1f else 0.5f), label = "bg")

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .shadow(elevation = if (isSelected && isEnabled && isDarkMode && !isGlassy) 16.dp else 0.dp, spotColor = activeAccentColor, ambientColor = activeAccentColor)
                            .background(animatedBgColor)
                            .border(2.dp, animatedBorderColor, RoundedCornerShape(20.dp))
                            .clickable(enabled = isEnabled) { viewModel.setPreset(preset) }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = preset, color = animatedTextColor, fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium, fontSize = 14.sp, textDecoration = TextDecoration.None)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
            bands.forEach { band ->
                val dbValue = band.level / 100
                val dbString = if (dbValue > 0) "+$dbValue" else "$dbValue"
                val animatedColor by animateColorAsState(if (isEnabled && dbValue != 0) activeAccentColor else unselectedColor, label = "dbColor")
                val friendlyLabel = getFrequencyLabel(band.centerFreqHz)
                val dbBgColor = if (isGlassy) Color.Black.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant
                val dbBorderColor = if (isGlassy) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outlineVariant

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(if(isGlassy) Color.White.copy(alpha = if (isEnabled) 0.15f else 0.05f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isEnabled) 0.5f else 0.1f)).padding(vertical = 16.dp, horizontal = 4.dp)
                ) {
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(dbBgColor).border(1.dp, dbBorderColor, RoundedCornerShape(8.dp)).padding(horizontal = 6.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = dbString, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.ExtraBold, color = animatedColor)
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    StudioFader(
                        value = band.level.toFloat(), valueRange = minLevel.toFloat()..maxLevel.toFloat(), isEnabled = isEnabled, isGlassy = isGlassy, isDarkMode = isDarkMode,
                        activeColor = activeAccentColor, inactiveColor = unselectedColor,
                        onValueChange = { viewModel.setBandLevel(band.index, it.toInt().toShort()) }
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(text = friendlyLabel, fontSize = 11.sp, color = if (isEnabled) activeAccentColor else unselectedColor, fontWeight = FontWeight.Bold, maxLines = 1)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = if (band.centerFreqHz >= 1000) "${band.centerFreqHz / 1000}k" else "${band.centerFreqHz}", fontSize = 14.sp, color = if (isEnabled) primaryTextColor else unselectedColor, fontWeight = FontWeight.ExtraBold)
                    Text(text = "Hz", fontSize = 10.sp, color = unselectedColor, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}