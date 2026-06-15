package com.example.musicplayer.ui.screens.library

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    playlistViewModel: PlaylistViewModel = hiltViewModel() // NEW: The persistent playlist brain
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

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
    }

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
            onResult = { }
        )
        LaunchedEffect(Unit) {
            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val songs by libraryViewModel.allSongs.collectAsState()
    val sortOrder by libraryViewModel.sortOrder.collectAsState()
    val favoriteSongs by favoriteViewModel.favoriteSongs.collectAsState()
    val currentSong by playerViewModel.currentSong.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val isDarkMode by themeViewModel.isDarkMode.collectAsState()

    // NEW: Observe the saved lists and link the URIs back to your actual Song objects
    val savedPlaylists by playlistViewModel.playlistState.collectAsState()
    val customPlaylists = remember(savedPlaylists, songs) {
        savedPlaylists.mapValues { (_, uris) ->
            uris.mapNotNull { uri -> songs.find { it.uri.toString() == uri } }
        }
    }

    val displayedSongs = remember(songs, searchQuery) {
        if (searchQuery.isBlank()) songs
        else songs.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.artist.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = when(selectedTab) { 0 -> "My Library"; 1 -> "Favorites"; else -> "Playlists" }, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp) },
                actions = {
                    IconButton(onClick = { themeViewModel.toggleTheme() }) {
                        Icon(imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode, contentDescription = "Toggle Theme", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    TextButton(onClick = {
                        authViewModel.logout()
                        navController.navigate("login") { popUpTo(0) }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout", tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Logout", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0; selectedPlaylist = null }, icon = { Icon(Icons.Default.LibraryMusic, "Library") }, label = { Text("Library") })
                NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1; selectedPlaylist = null }, icon = { Icon(Icons.Default.Favorite, "Favorites") }, label = { Text("Favorites") })
                NavigationBarItem(selected = selectedTab == 2, onClick = { selectedTab = 2 }, icon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, "Playlists") }, label = { Text("Playlists") })
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (selectedTab) {
                0 -> Column {
                    OutlinedTextField(
                        value = searchQuery, onValueChange = { searchQuery = it }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        placeholder = { Text("Search songs or artists...") }, leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        singleLine = true, shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    )
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
                                song = song, isFavorite = favoriteSongs.any { it.id == song.id }, isCurrentlyPlaying = currentSong?.id == song.id,
                                onFavoriteToggle = { favoriteViewModel.toggleFavorite(song) }, onAddToPlaylist = { songToAdd = song }
                            ) { playerViewModel.playQueue(displayedSongs, index) }
                        }
                    }
                }
                1 -> {
                    if (favoriteSongs.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.FavoriteBorder, contentDescription = null, modifier = Modifier.size(80.dp), tint = Color.Gray)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Tap the heart on a song to add it here!", color = Color.Gray)
                            }
                        }
                    } else {
                        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            itemsIndexed(favoriteSongs) { index, song ->
                                SongCard(
                                    song = song, isFavorite = true, isCurrentlyPlaying = currentSong?.id == song.id,
                                    onFavoriteToggle = { favoriteViewModel.toggleFavorite(song) }, onAddToPlaylist = { songToAdd = song }
                                ) { playerViewModel.playQueue(favoriteSongs, index) }
                            }
                        }
                    }
                }
                2 -> {
                    if (selectedPlaylist == null) {
                        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            item { Button(onClick = { showPlaylistDialog = true }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) { Text("Create New Playlist") } }

                            if (customPlaylists.isEmpty()) {
                                item {
                                    Column(modifier = Modifier.fillMaxWidth().padding(top = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null, modifier = Modifier.size(80.dp), tint = Color.Gray)
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text("No playlists yet.", color = Color.Gray)
                                    }
                                }
                            } else {
                                itemsIndexed(customPlaylists.keys.toList()) { _, playlistName ->
                                    val songCount = customPlaylists[playlistName]?.size ?: 0

                                    Row(
                                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)).clickable {
                                            selectedPlaylist = playlistName
                                        }.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Folder, contentDescription = "Folder", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = playlistName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                            Text(text = "$songCount songs", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        IconButton(onClick = {
                                            playlistToRename = playlistName; renamePlaylistName = playlistName
                                        }) { Icon(Icons.Default.Edit, contentDescription = "Rename", tint = MaterialTheme.colorScheme.primary) }

                                        IconButton(onClick = {
                                            playlistViewModel.deletePlaylist(playlistName) // Saved securely!
                                        }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error) }
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
                                IconButton(onClick = { selectedPlaylist = null; playlistSearchQuery = "" }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = activePlaylistName, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                            }

                            OutlinedTextField(
                                value = playlistSearchQuery, onValueChange = { playlistSearchQuery = it }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                placeholder = { Text("Search in $activePlaylistName...") }, leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                                singleLine = true, shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            )

                            if (rawPlaylistSongs.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("This playlist is empty.", color = Color.Gray)
                                }
                            } else {
                                LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    itemsIndexed(filteredPlaylistSongs) { index, song ->
                                        SongCard(
                                            song = song, isFavorite = favoriteSongs.any { it.id == song.id }, isCurrentlyPlaying = currentSong?.id == song.id,
                                            onFavoriteToggle = { favoriteViewModel.toggleFavorite(song) }, onAddToPlaylist = { songToAdd = song },
                                            onRemoveFromPlaylist = {
                                                playlistViewModel.removeSongFromPlaylist(activePlaylistName, song.uri.toString()) // Saved securely!
                                            }
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
                    onClick = { navController.navigate("player") }, modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).padding(bottom = 8.dp)
                )
            }
        }

        if (showPlaylistDialog) {
            AlertDialog(
                onDismissRequest = { showPlaylistDialog = false }, title = { Text("New Playlist") },
                text = { OutlinedTextField(value = newPlaylistName, onValueChange = { newPlaylistName = it }, label = { Text("Playlist Name") }) },
                confirmButton = {
                    Button(onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            playlistViewModel.createPlaylist(newPlaylistName) // Saved securely!
                        }
                        showPlaylistDialog = false; newPlaylistName = ""
                    }) { Text("Create") }
                },
                dismissButton = { TextButton(onClick = { showPlaylistDialog = false }) { Text("Cancel") } }
            )
        }

        if (playlistToRename != null) {
            AlertDialog(
                onDismissRequest = { playlistToRename = null }, title = { Text("Rename Playlist") },
                text = { OutlinedTextField(value = renamePlaylistName, onValueChange = { renamePlaylistName = it }, label = { Text("New Name") }) },
                confirmButton = {
                    Button(onClick = {
                        if (renamePlaylistName.isNotBlank()) {
                            playlistViewModel.renamePlaylist(playlistToRename!!, renamePlaylistName) // Saved securely!
                        }
                        playlistToRename = null
                    }) { Text("Save") }
                },
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
                                TextButton(onClick = {
                                    playlistViewModel.addSongToPlaylist(playlistName, songToAdd!!.uri.toString()) // Saved securely!
                                    songToAdd = null
                                }) { Text(playlistName, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary) }
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { songToAdd = null }) { Text("Cancel") } }
            )
        }
    }
}

@Composable
fun SongCard(
    song: Song, isFavorite: Boolean, isCurrentlyPlaying: Boolean,
    onFavoriteToggle: () -> Unit, onAddToPlaylist: () -> Unit,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val fallbackIcon = rememberVectorPainter(Icons.Default.MusicNote)
    val bgColor = if (isCurrentlyPlaying) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val textColor = if (isCurrentlyPlaying) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(bgColor).clickable { onClick() }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(60.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
            if (song.albumArtUri == null || song.albumArtUri.toString().isEmpty()) {
                Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
            } else {
                AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(song.albumArtUri).crossfade(true).build(), contentDescription = null, contentScale = ContentScale.Crop, error = fallbackIcon, fallback = fallbackIcon, modifier = Modifier.fillMaxSize())
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = song.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = textColor)
            Text(text = song.artist, fontSize = 14.sp, maxLines = 1, color = textColor.copy(alpha = 0.8f))
        }
        IconButton(onClick = onFavoriteToggle) { Icon(imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, contentDescription = "Favorite", tint = if (isFavorite) Color.Red else textColor.copy(alpha = 0.8f)) }

        if (onRemoveFromPlaylist != null) {
            IconButton(onClick = onRemoveFromPlaylist) {
                Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Remove from Playlist", tint = MaterialTheme.colorScheme.error)
            }
        } else {
            IconButton(onClick = onAddToPlaylist) {
                Icon(Icons.Default.Add, contentDescription = "Add to Playlist", tint = textColor.copy(alpha = 0.8f))
            }
        }
    }
}

@Composable
fun MiniPlayer(
    song: Song, isPlaying: Boolean, onPlayPause: () -> Unit, onSkipNext: () -> Unit, onSkipPrevious: () -> Unit, onClick: () -> Unit, modifier: Modifier = Modifier
) {
    val fallbackIcon = rememberVectorPainter(Icons.Default.MusicNote)
    Row(modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primaryContainer).clickable { onClick() }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
            if (song.albumArtUri == null || song.albumArtUri.toString().isEmpty()) { Icon(Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer) }
            else { AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(song.albumArtUri).crossfade(true).build(), contentDescription = null, contentScale = ContentScale.Crop, error = fallbackIcon, fallback = fallbackIcon, modifier = Modifier.fillMaxSize()) }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = song.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(text = song.artist, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onSkipPrevious) { Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = MaterialTheme.colorScheme.onPrimaryContainer) }
            IconButton(onClick = onPlayPause) { Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "Play/Pause", tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(32.dp)) }
            IconButton(onClick = onSkipNext) { Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = MaterialTheme.colorScheme.onPrimaryContainer) }
        }
    }
}