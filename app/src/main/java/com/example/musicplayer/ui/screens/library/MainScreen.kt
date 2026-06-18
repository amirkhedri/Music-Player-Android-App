package com.example.musicplayer.ui.screens.library

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.musicplayer.data.model.Song
import com.example.musicplayer.ui.screens.player.PlayingVisualizer
import com.example.musicplayer.viewmodel.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavController,
    playerViewModel: PlayerViewModel,
    libraryViewModel: LibraryViewModel = hiltViewModel(),
    favoriteViewModel: FavoriteViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel(),
    playlistViewModel: PlaylistViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }

    var songToAdd by remember { mutableStateOf<Song?>(null) }
    var showPlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var playlistToRename by remember { mutableStateOf<String?>(null) }
    var renamePlaylistName by remember { mutableStateOf("") }

    var selectedPlaylist by remember { mutableStateOf<String?>(null) }
    var playlistSearchQuery by remember { mutableStateOf("") }

    BackHandler(enabled = selectedPlaylist != null) {
        selectedPlaylist = null
        playlistSearchQuery = ""
        isSearchExpanded = false
    }

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
            onResult = { }
        )
        LaunchedEffect(Unit) { permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS) }
    }

    val songs by libraryViewModel.allSongs.collectAsState()
    val sortOrder by libraryViewModel.sortOrder.collectAsState()
    val favoriteSongs by favoriteViewModel.favoriteSongs.collectAsState()
    val currentSong by playerViewModel.currentSong.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val isDarkMode by themeViewModel.isDarkMode.collectAsState()

    val savedPlaylists by playlistViewModel.playlistState.collectAsState()
    val customPlaylists = remember(savedPlaylists, songs) {
        savedPlaylists.mapValues { (_, uris) -> uris.mapNotNull { uri -> songs.find { it.uri.toString() == uri } } }
    }

    val displayedSongs = remember(songs, searchQuery) {
        if (searchQuery.isBlank()) songs
        else songs.filter { it.title.contains(searchQuery, ignoreCase = true) || it.artist.contains(searchQuery, ignoreCase = true) }
    }

    // NEW: Proper filtering for the Favorites tab!
    val displayedFavoriteSongs = remember(favoriteSongs, searchQuery) {
        if (searchQuery.isBlank()) favoriteSongs
        else favoriteSongs.filter { it.title.contains(searchQuery, ignoreCase = true) || it.artist.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = when(selectedTab) { 0 -> "Library"; 1 -> "Favorites"; else -> "Playlists" }, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp) },
                actions = {
                    // FIX: Search icon now shows on Tab 0 (Library) and Tab 1 (Favorites)
                    if (selectedTab == 0 || selectedTab == 1 || (selectedTab == 2 && selectedPlaylist != null)) {
                        IconButton(onClick = { isSearchExpanded = !isSearchExpanded }) { Icon(Icons.Default.Search, contentDescription = "Search") }
                    }
                    IconButton(onClick = { themeViewModel.toggleTheme() }) {
                        Icon(imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode, contentDescription = "Toggle Theme")
                    }
                    IconButton(onClick = { authViewModel.logout(); navController.navigate("login") { popUpTo(0) } }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            PremiumBottomBar(
                selectedTab = selectedTab,
                onTabSelected = { tab ->
                    selectedTab = tab
                    selectedPlaylist = null
                    isSearchExpanded = false
                    searchQuery = "" // Clears search when switching tabs for a clean UX
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (selectedTab) {
                0 -> Column {
                    AnimatedVisibility(visible = isSearchExpanded) {
                        OutlinedTextField(
                            value = searchQuery, onValueChange = { searchQuery = it }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            placeholder = { Text("Search library...") }, leadingIcon = { Icon(Icons.Default.Search, null) },
                            trailingIcon = { IconButton(onClick = { isSearchExpanded = false; searchQuery = "" }) { Icon(Icons.Default.Close, null) } },
                            singleLine = true, shape = RoundedCornerShape(16.dp)
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Sort by:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row {
                            TextButton(onClick = { libraryViewModel.setSortOrder(SortOrder.TITLE) }) { Text("Title", color = if (sortOrder == SortOrder.TITLE) MaterialTheme.colorScheme.primary else Color.Gray) }
                            TextButton(onClick = { libraryViewModel.setSortOrder(SortOrder.ARTIST) }) { Text("Artist", color = if (sortOrder == SortOrder.ARTIST) MaterialTheme.colorScheme.primary else Color.Gray) }
                        }
                    }
                    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        itemsIndexed(displayedSongs) { index, song ->
                            SongCard(
                                song = song, isFavorite = favoriteSongs.any { it.id == song.id }, isCurrentlyPlaying = currentSong?.id == song.id, isPlaying = isPlaying,
                                onFavoriteToggle = { favoriteViewModel.toggleFavorite(song) }, onAddToPlaylist = { songToAdd = song }
                            ) { playerViewModel.playQueue(displayedSongs, index) }
                        }
                    }
                }
                1 -> Column {
                    // FIX: Search Bar added to Favorites!
                    AnimatedVisibility(visible = isSearchExpanded) {
                        OutlinedTextField(
                            value = searchQuery, onValueChange = { searchQuery = it }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            placeholder = { Text("Search favorites...") }, leadingIcon = { Icon(Icons.Default.Search, null) },
                            trailingIcon = { IconButton(onClick = { isSearchExpanded = false; searchQuery = "" }) { Icon(Icons.Default.Close, null) } },
                            singleLine = true, shape = RoundedCornerShape(16.dp)
                        )
                    }

                    if (favoriteSongs.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.FavoriteBorder, null, modifier = Modifier.size(80.dp), tint = Color.Gray)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Tap the heart on a song to add it here!", color = Color.Gray)
                            }
                        }
                    } else {
                        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Uses the properly filtered list now
                            itemsIndexed(displayedFavoriteSongs) { index, song ->
                                SongCard(
                                    song = song, isFavorite = true, isCurrentlyPlaying = currentSong?.id == song.id, isPlaying = isPlaying,
                                    onFavoriteToggle = { favoriteViewModel.toggleFavorite(song) }, onAddToPlaylist = { songToAdd = song }
                                ) { playerViewModel.playQueue(displayedFavoriteSongs, index) }
                            }
                        }
                    }
                }
                2 -> {
                    if (selectedPlaylist == null) {
                        // FIX: Changed from Grid to Apple Music/Spotify style vertical List
                        LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Button(onClick = { showPlaylistDialog = true }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp)) { Text("Create New Playlist") }
                            }

                            if (customPlaylists.isEmpty()) {
                                item {
                                    Column(modifier = Modifier.fillMaxWidth().padding(top = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null, modifier = Modifier.size(80.dp), tint = Color.Gray)
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text("No playlists yet.", color = Color.Gray)
                                    }
                                }
                            } else {
                                items(customPlaylists.keys.toList()) { playlistName ->
                                    val songCount = customPlaylists[playlistName]?.size ?: 0
                                    val firstSong = customPlaylists[playlistName]?.firstOrNull() // Pulls the first song's data for the cover

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                            .clickable { selectedPlaylist = playlistName }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Premium Album Art Cover for the Playlist
                                        Box(
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (firstSong?.albumArtUri == null || firstSong.albumArtUri.toString().isEmpty()) {
                                                Icon(Icons.AutoMirrored.Filled.PlaylistPlay, null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
                                            } else {
                                                AsyncImage(
                                                    model = ImageRequest.Builder(LocalContext.current).data(firstSong.albumArtUri).crossfade(true).build(),
                                                    contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(16.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = playlistName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(text = "$songCount songs", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }

                                        // Large, properly sized Edit and Delete buttons
                                        IconButton(onClick = { playlistToRename = playlistName; renamePlaylistName = playlistName }, modifier = Modifier.size(48.dp)) {
                                            Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.primary)
                                        }
                                        IconButton(onClick = { playlistViewModel.deletePlaylist(playlistName) }, modifier = Modifier.size(48.dp)) {
                                            Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        val activePlaylistName = selectedPlaylist!!
                        val rawPlaylistSongs = customPlaylists[activePlaylistName] ?: emptyList()

                        val filteredPlaylistSongs = remember(rawPlaylistSongs, playlistSearchQuery) {
                            if (playlistSearchQuery.isBlank()) rawPlaylistSongs
                            else rawPlaylistSongs.filter { it.title.contains(playlistSearchQuery, ignoreCase = true) || it.artist.contains(playlistSearchQuery, ignoreCase = true) }
                        }

                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { selectedPlaylist = null; playlistSearchQuery = ""; isSearchExpanded = false }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = activePlaylistName, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                            }

                            AnimatedVisibility(visible = isSearchExpanded) {
                                OutlinedTextField(
                                    value = playlistSearchQuery, onValueChange = { playlistSearchQuery = it }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                    placeholder = { Text("Search in $activePlaylistName...") }, leadingIcon = { Icon(Icons.Default.Search, null) },
                                    trailingIcon = { IconButton(onClick = { isSearchExpanded = false; playlistSearchQuery = "" }) { Icon(Icons.Default.Close, null) } },
                                    singleLine = true, shape = RoundedCornerShape(16.dp)
                                )
                            }

                            if (rawPlaylistSongs.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("This playlist is empty.", color = Color.Gray) }
                            } else {
                                LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    itemsIndexed(filteredPlaylistSongs) { index, song ->
                                        SongCard(
                                            song = song, isFavorite = favoriteSongs.any { it.id == song.id }, isCurrentlyPlaying = currentSong?.id == song.id, isPlaying = isPlaying,
                                            onFavoriteToggle = { favoriteViewModel.toggleFavorite(song) }, onAddToPlaylist = { songToAdd = song },
                                            onRemoveFromPlaylist = { playlistViewModel.removeSongFromPlaylist(activePlaylistName, song.uri.toString()) }
                                        ) { playerViewModel.playQueue(filteredPlaylistSongs, index) }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (currentSong != null) {
                MiniPlayer(
                    song = currentSong!!, isPlaying = isPlaying, onPlayPause = { playerViewModel.togglePlayPause() },
                    onSkipNext = { playerViewModel.skipNext() }, onSkipPrevious = { playerViewModel.skipPrevious() },
                    onClick = { navController.navigate("player") },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
                        .shadow(16.dp, RoundedCornerShape(24.dp))
                )
            }
        }

        if (showPlaylistDialog) {
            AlertDialog(
                onDismissRequest = { showPlaylistDialog = false }, title = { Text("New Playlist") },
                text = { OutlinedTextField(value = newPlaylistName, onValueChange = { newPlaylistName = it }, label = { Text("Playlist Name") }) },
                confirmButton = { Button(onClick = { if (newPlaylistName.isNotBlank()) playlistViewModel.createPlaylist(newPlaylistName); showPlaylistDialog = false; newPlaylistName = "" }) { Text("Create") } },
                dismissButton = { TextButton(onClick = { showPlaylistDialog = false }) { Text("Cancel") } }
            )
        }

        if (playlistToRename != null) {
            AlertDialog(
                onDismissRequest = { playlistToRename = null }, title = { Text("Rename Playlist") },
                text = { OutlinedTextField(value = renamePlaylistName, onValueChange = { renamePlaylistName = it }, label = { Text("New Name") }) },
                confirmButton = { Button(onClick = { if (renamePlaylistName.isNotBlank()) playlistViewModel.renamePlaylist(playlistToRename!!, renamePlaylistName); playlistToRename = null }) { Text("Save") } },
                dismissButton = { TextButton(onClick = { playlistToRename = null }) { Text("Cancel") } }
            )
        }

        if (songToAdd != null) {
            AlertDialog(
                onDismissRequest = { songToAdd = null }, title = { Text("Add to Playlist") },
                text = {
                    if (customPlaylists.isEmpty()) { Text("You don't have any playlists yet. Create one first!") } else {
                        Column {
                            customPlaylists.keys.forEach { playlistName ->
                                TextButton(onClick = { playlistViewModel.addSongToPlaylist(playlistName, songToAdd!!.uri.toString()); songToAdd = null }) { Text(playlistName, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary) }
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { songToAdd = null }) { Text("Cancel") } }
            )
        }
    }
}

// ---------------------------------------------------------
// CUSTOM UI COMPONENTS
// ---------------------------------------------------------

// FIX: Floating, Curvy Navigation Bar
@Composable
fun PremiumBottomBar(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp) // Creates the floating effect
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f), // Dark, glassy feel
            shape = RoundedCornerShape(32.dp), // Extremely curvy pill shape
            modifier = Modifier.shadow(24.dp, RoundedCornerShape(32.dp), spotColor = MaterialTheme.colorScheme.primary)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavItem(
                    icon = Icons.Default.LibraryMusic,
                    isSelected = selectedTab == 0,
                    onClick = { onTabSelected(0) }
                )
                BottomNavItem(
                    icon = Icons.Default.Favorite,
                    isSelected = selectedTab == 1,
                    onClick = { onTabSelected(1) }
                )
                BottomNavItem(
                    icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                    isSelected = selectedTab == 2,
                    onClick = { onTabSelected(2) }
                )
            }
        }
    }
}

@Composable
fun BottomNavItem(icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    val animatedColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray, label = "color")
    val animatedScale by animateFloatAsState(if (isSelected) 1.15f else 1.0f, label = "scale")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = animatedColor,
            modifier = Modifier.size(28.dp).scale(animatedScale)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(if (isSelected) animatedColor else Color.Transparent)
                .shadow(if (isSelected) 8.dp else 0.dp, CircleShape, spotColor = animatedColor)
        )
    }
}

@Composable
fun SongCard(
    song: Song, isFavorite: Boolean, isCurrentlyPlaying: Boolean, isPlaying: Boolean,
    onFavoriteToggle: () -> Unit, onAddToPlaylist: () -> Unit, onRemoveFromPlaylist: (() -> Unit)? = null, onClick: () -> Unit
) {
    val fallbackIcon = rememberVectorPainter(Icons.Default.MusicNote)
    val bgColor = if (isCurrentlyPlaying) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    val textColor = if (isCurrentlyPlaying) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(bgColor).clickable { onClick() }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(60.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            if (song.albumArtUri == null || song.albumArtUri.toString().isEmpty()) {
                Icon(Icons.Default.MusicNote, null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(song.albumArtUri).crossfade(true).build(), contentDescription = null, contentScale = ContentScale.Crop, error = fallbackIcon, fallback = fallbackIcon, modifier = Modifier.fillMaxSize())
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = song.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    color = textColor,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .then(if (isCurrentlyPlaying) Modifier.basicMarquee() else Modifier)
                )
                if (isCurrentlyPlaying) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.padding(bottom = 4.dp)) { PlayingVisualizer(isPlaying = isPlaying) }
                }
            }
            Text(
                text = song.artist,
                fontSize = 14.sp,
                maxLines = 1,
                color = textColor.copy(alpha = 0.8f),
                modifier = if (isCurrentlyPlaying) Modifier.basicMarquee() else Modifier
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(onClick = onFavoriteToggle) { Icon(imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, contentDescription = "Favorite", tint = if (isFavorite) Color.Red else textColor.copy(alpha = 0.8f)) }

        if (onRemoveFromPlaylist != null) {
            IconButton(onClick = onRemoveFromPlaylist) { Icon(Icons.Default.RemoveCircleOutline, null, tint = MaterialTheme.colorScheme.error) }
        } else {
            IconButton(onClick = onAddToPlaylist) { Icon(Icons.Default.Add, null, tint = textColor.copy(alpha = 0.8f)) }
        }
    }
}

@Composable
fun MiniPlayer(
    song: Song, isPlaying: Boolean, onPlayPause: () -> Unit, onSkipNext: () -> Unit, onSkipPrevious: () -> Unit, onClick: () -> Unit, modifier: Modifier = Modifier
) {
    val fallbackIcon = rememberVectorPainter(Icons.Default.MusicNote)
    Row(modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.primaryContainer).clickable { onClick() }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
            if (song.albumArtUri == null || song.albumArtUri.toString().isEmpty()) { Icon(Icons.Default.MusicNote, null, tint = MaterialTheme.colorScheme.onPrimaryContainer) }
            else { AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(song.albumArtUri).crossfade(true).build(), contentDescription = null, contentScale = ContentScale.Crop, error = fallbackIcon, fallback = fallbackIcon, modifier = Modifier.fillMaxSize()) }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.basicMarquee()
            )
            Text(
                text = song.artist,
                fontSize = 12.sp,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                modifier = Modifier.basicMarquee()
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onSkipPrevious) { Icon(Icons.Default.SkipPrevious, null, tint = MaterialTheme.colorScheme.onPrimaryContainer) }
            IconButton(onClick = onPlayPause) { Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(32.dp)) }
            IconButton(onClick = onSkipNext) { Icon(Icons.Default.SkipNext, null, tint = MaterialTheme.colorScheme.onPrimaryContainer) }
        }
    }
}