package com.example.musicplayer.ui.screens.library

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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
import com.example.musicplayer.viewmodel.*

fun shareSongs(context: Context, songs: List<Song>) {
    if (songs.isEmpty()) return
    val uris = ArrayList(songs.map { Uri.parse(it.uri.toString()) })
    val intent = Intent().apply {
        action = if (uris.size > 1) Intent.ACTION_SEND_MULTIPLE else Intent.ACTION_SEND
        if (uris.size > 1) {
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        } else {
            putExtra(Intent.EXTRA_STREAM, uris.first())
        }
        type = "audio/*"
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share Music"))
}

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
    val context = LocalContext.current

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            libraryViewModel.confirmDatabaseDeletion()
            Toast.makeText(context, "Deleted successfully", Toast.LENGTH_SHORT).show()
        } else {
            libraryViewModel.cancelDeletion()
        }
    }

    LaunchedEffect(Unit) {
        libraryViewModel.deletePendingIntent.collect { intentSender ->
            deleteLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }

    var selectedSongUris by remember { mutableStateOf<Set<String>>(emptySet()) }
    val isSelectionMode = selectedSongUris.isNotEmpty()

    var songToAdd by remember { mutableStateOf<Song?>(null) }
    var songToDelete by remember { mutableStateOf<Song?>(null) }
    var showBulkDeleteDialog by remember { mutableStateOf(false) }
    var showPlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var playlistToRename by remember { mutableStateOf<String?>(null) }
    var renamePlaylistName by remember { mutableStateOf("") }

    var selectedPlaylist by remember { mutableStateOf<String?>(null) }
    var selectedArtist by remember { mutableStateOf<String?>(null) }
    var subScreenSearchQuery by remember { mutableStateOf("") }

    fun clearSelectionAndSearch() {
        selectedSongUris = emptySet()
        isSearchExpanded = false
        searchQuery = ""
        subScreenSearchQuery = ""
    }

    BackHandler(enabled = selectedPlaylist != null || selectedArtist != null || isSelectionMode) {
        if (isSelectionMode) {
            selectedSongUris = emptySet()
        } else {
            selectedPlaylist = null
            selectedArtist = null
            clearSelectionAndSearch()
        }
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

    val artistGroups = remember(songs) {
        songs.groupBy { it.artist.ifBlank { "Unknown Artist" } }
    }

    val displayedSongs = remember(songs, searchQuery) {
        if (searchQuery.isBlank()) songs
        else songs.filter { it.title.contains(searchQuery, ignoreCase = true) || it.artist.contains(searchQuery, ignoreCase = true) }
    }

    val displayedFavoriteSongs = remember(favoriteSongs, searchQuery) {
        if (searchQuery.isBlank()) favoriteSongs
        else favoriteSongs.filter { it.title.contains(searchQuery, ignoreCase = true) || it.artist.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    title = { Text("${selectedSongUris.size} Selected", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { selectedSongUris = emptySet() }) { Icon(Icons.Default.Close, "Cancel") }
                    },
                    actions = {
                        IconButton(onClick = {
                            val songsToShare = songs.filter { selectedSongUris.contains(it.uri.toString()) }
                            shareSongs(context, songsToShare)
                            selectedSongUris = emptySet()
                        }) {
                            Icon(Icons.Default.Share, "Share")
                        }
                        IconButton(onClick = { showBulkDeleteDialog = true }) {
                            Icon(Icons.Outlined.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                )
            } else {
                TopAppBar(
                    title = {
                        Text(
                            text = when(selectedTab) { 0 -> "Library"; 1 -> "Favorites"; 2 -> "Artists"; else -> "Playlists" },
                            fontWeight = FontWeight.ExtraBold, fontSize = 28.sp
                        )
                    },
                    actions = {
                        if (selectedTab != 3 || selectedPlaylist != null) {
                            IconButton(onClick = { isSearchExpanded = !isSearchExpanded }) { Icon(Icons.Default.Search, contentDescription = "Search") }
                        }
                        IconButton(onClick = { themeViewModel.toggleTheme() }) {
                            Icon(imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode, contentDescription = "Toggle Theme")
                        }
                        IconButton(onClick = {
                            if (isPlaying) playerViewModel.togglePlayPause()
                            authViewModel.logout()
                            navController.navigate("login") { popUpTo(0) }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout", tint = MaterialTheme.colorScheme.error)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            }
        },
        bottomBar = {
            PremiumBottomBar(
                selectedTab = selectedTab,
                onTabSelected = { tab ->
                    selectedTab = tab
                    selectedPlaylist = null
                    selectedArtist = null
                    clearSelectionAndSearch()
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
                    if (!isSelectionMode) {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Sort by:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row {
                                TextButton(onClick = { libraryViewModel.setSortOrder(SortOrder.TITLE) }) { Text("Title", color = if (sortOrder == SortOrder.TITLE) MaterialTheme.colorScheme.primary else Color.Gray) }
                                TextButton(onClick = { libraryViewModel.setSortOrder(SortOrder.ARTIST) }) { Text("Artist", color = if (sortOrder == SortOrder.ARTIST) MaterialTheme.colorScheme.primary else Color.Gray) }
                            }
                        }
                    }
                    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        itemsIndexed(displayedSongs) { index, song ->
                            SongCard(
                                song = song, isFavorite = favoriteSongs.any { it.id == song.id }, isCurrentlyPlaying = currentSong?.id == song.id, isPlaying = isPlaying,
                                isSelectionMode = isSelectionMode, isSelected = selectedSongUris.contains(song.uri.toString()),
                                onFavoriteToggle = { favoriteViewModel.toggleFavorite(song) }, onAddToPlaylist = { songToAdd = song },
                                onLongClick = { selectedSongUris = if (selectedSongUris.contains(song.uri.toString())) selectedSongUris - song.uri.toString() else selectedSongUris + song.uri.toString() },
                                onClick = {
                                    if (isSelectionMode) selectedSongUris = if (selectedSongUris.contains(song.uri.toString())) selectedSongUris - song.uri.toString() else selectedSongUris + song.uri.toString()
                                    else playerViewModel.playQueue(displayedSongs, index)
                                }
                            )
                        }
                    }
                }
                1 -> Column {
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
                            itemsIndexed(displayedFavoriteSongs) { index, song ->
                                SongCard(
                                    song = song, isFavorite = true, isCurrentlyPlaying = currentSong?.id == song.id, isPlaying = isPlaying,
                                    isSelectionMode = isSelectionMode, isSelected = selectedSongUris.contains(song.uri.toString()),
                                    onFavoriteToggle = { favoriteViewModel.toggleFavorite(song) }, onAddToPlaylist = { songToAdd = song },
                                    onLongClick = { selectedSongUris = if (selectedSongUris.contains(song.uri.toString())) selectedSongUris - song.uri.toString() else selectedSongUris + song.uri.toString() },
                                    onClick = {
                                        if (isSelectionMode) selectedSongUris = if (selectedSongUris.contains(song.uri.toString())) selectedSongUris - song.uri.toString() else selectedSongUris + song.uri.toString()
                                        else playerViewModel.playQueue(displayedFavoriteSongs, index)
                                    }
                                )
                            }
                        }
                    }
                }
                2 -> {
                    if (selectedArtist == null) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(artistGroups.keys.toList().sorted()) { artistName ->
                                val artistSongs = artistGroups[artistName] ?: emptyList()
                                val firstSong = artistSongs.firstOrNull()

                                Box(
                                    modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(24.dp)).shadow(8.dp, RoundedCornerShape(24.dp)).clickable { selectedArtist = artistName }
                                ) {
                                    if (firstSong?.albumArtUri == null || firstSong.albumArtUri.toString().isEmpty()) {
                                        Box(
                                            modifier = Modifier.fillMaxSize().background(Brush.radialGradient(colors = listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.surfaceVariant))),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.MicExternalOn, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                                        }
                                    } else {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current).data(firstSong.albumArtUri).crossfade(true).build(),
                                            contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                                        )
                                    }

                                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)), startY = 150f)))

                                    Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                                        Text(text = artistName, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(text = "${artistSongs.size} songs", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    } else {
                        val artistSongs = artistGroups[selectedArtist] ?: emptyList()
                        val filteredSongs = remember(artistSongs, subScreenSearchQuery) {
                            if (subScreenSearchQuery.isBlank()) artistSongs else artistSongs.filter { it.title.contains(subScreenSearchQuery, ignoreCase = true) }
                        }

                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { selectedArtist = null; clearSelectionAndSearch() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = selectedArtist!!, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                            }
                            AnimatedVisibility(visible = isSearchExpanded) {
                                OutlinedTextField(
                                    value = subScreenSearchQuery, onValueChange = { subScreenSearchQuery = it }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                    placeholder = { Text("Search in $selectedArtist...") }, leadingIcon = { Icon(Icons.Default.Search, null) },
                                    trailingIcon = { IconButton(onClick = { isSearchExpanded = false; subScreenSearchQuery = "" }) { Icon(Icons.Default.Close, null) } },
                                    singleLine = true, shape = RoundedCornerShape(16.dp)
                                )
                            }
                            LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                itemsIndexed(filteredSongs) { index, song ->
                                    SongCard(
                                        song = song, isFavorite = favoriteSongs.any { it.id == song.id }, isCurrentlyPlaying = currentSong?.id == song.id, isPlaying = isPlaying,
                                        isSelectionMode = isSelectionMode, isSelected = selectedSongUris.contains(song.uri.toString()),
                                        onFavoriteToggle = { favoriteViewModel.toggleFavorite(song) }, onAddToPlaylist = { songToAdd = song },
                                        onLongClick = { selectedSongUris = if (selectedSongUris.contains(song.uri.toString())) selectedSongUris - song.uri.toString() else selectedSongUris + song.uri.toString() },
                                        onClick = {
                                            if (isSelectionMode) selectedSongUris = if (selectedSongUris.contains(song.uri.toString())) selectedSongUris - song.uri.toString() else selectedSongUris + song.uri.toString()
                                            else playerViewModel.playQueue(filteredSongs, index)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                3 -> {
                    if (selectedPlaylist == null) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item(span = { GridItemSpan(2) }) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp).clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)).clickable { showPlaylistDialog = true }.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Create New Playlist", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }

                            if (customPlaylists.isEmpty()) {
                                item(span = { GridItemSpan(2) }) {
                                    Column(modifier = Modifier.fillMaxWidth().padding(top = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null, modifier = Modifier.size(80.dp), tint = Color.Gray)
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text("No playlists yet.", color = Color.Gray)
                                    }
                                }
                            } else {
                                items(customPlaylists.keys.toList()) { playlistName ->
                                    val songCount = customPlaylists[playlistName]?.size ?: 0
                                    val firstSong = customPlaylists[playlistName]?.firstOrNull()

                                    Box(
                                        modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(24.dp)).shadow(8.dp, RoundedCornerShape(24.dp)).clickable { selectedPlaylist = playlistName }
                                    ) {
                                        if (firstSong?.albumArtUri == null || firstSong.albumArtUri.toString().isEmpty()) {
                                            Box(
                                                modifier = Modifier.fillMaxSize().background(Brush.linearGradient(colors = listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.background))),
                                                contentAlignment = Alignment.Center
                                            ) { Icon(Icons.AutoMirrored.Filled.PlaylistPlay, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)) }
                                        } else {
                                            AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(firstSong.albumArtUri).crossfade(true).build(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                        }

                                        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)), startY = 150f)))

                                        Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                                            Text(text = playlistName, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(text = "$songCount songs", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Medium)
                                        }

                                        Row(
                                            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).clip(RoundedCornerShape(16.dp)).background(Color.Black.copy(alpha = 0.5f)).padding(horizontal = 4.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(onClick = { playlistToRename = playlistName; renamePlaylistName = playlistName }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Edit, "Edit", tint = Color.White, modifier = Modifier.size(18.dp)) }
                                            IconButton(onClick = { playlistViewModel.deletePlaylist(playlistName) }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Delete, "Delete", tint = Color.White, modifier = Modifier.size(18.dp)) }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        val activePlaylistName = selectedPlaylist!!
                        val rawPlaylistSongs = customPlaylists[activePlaylistName] ?: emptyList()

                        val filteredPlaylistSongs = remember(rawPlaylistSongs, subScreenSearchQuery) {
                            if (subScreenSearchQuery.isBlank()) rawPlaylistSongs
                            else rawPlaylistSongs.filter { it.title.contains(subScreenSearchQuery, ignoreCase = true) || it.artist.contains(subScreenSearchQuery, ignoreCase = true) }
                        }

                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { selectedPlaylist = null; clearSelectionAndSearch() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = activePlaylistName, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                            }
                            AnimatedVisibility(visible = isSearchExpanded) {
                                OutlinedTextField(
                                    value = subScreenSearchQuery, onValueChange = { subScreenSearchQuery = it }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                    placeholder = { Text("Search in $activePlaylistName...") }, leadingIcon = { Icon(Icons.Default.Search, null) },
                                    trailingIcon = { IconButton(onClick = { isSearchExpanded = false; subScreenSearchQuery = "" }) { Icon(Icons.Default.Close, null) } },
                                    singleLine = true, shape = RoundedCornerShape(16.dp)
                                )
                            }
                            LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                itemsIndexed(filteredPlaylistSongs) { index, song ->
                                    SongCard(
                                        song = song, isFavorite = favoriteSongs.any { it.id == song.id }, isCurrentlyPlaying = currentSong?.id == song.id, isPlaying = isPlaying,
                                        isSelectionMode = isSelectionMode, isSelected = selectedSongUris.contains(song.uri.toString()),
                                        onFavoriteToggle = { favoriteViewModel.toggleFavorite(song) }, onAddToPlaylist = { songToAdd = song },
                                        onLongClick = { selectedSongUris = if (selectedSongUris.contains(song.uri.toString())) selectedSongUris - song.uri.toString() else selectedSongUris + song.uri.toString() },
                                        onRemoveFromPlaylist = { playlistViewModel.removeSongFromPlaylist(activePlaylistName, song.uri.toString()) },
                                        onClick = {
                                            if (isSelectionMode) selectedSongUris = if (selectedSongUris.contains(song.uri.toString())) selectedSongUris - song.uri.toString() else selectedSongUris + song.uri.toString()
                                            else playerViewModel.playQueue(filteredPlaylistSongs, index)
                                        }
                                    )
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
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 0.dp, start = 16.dp, end = 16.dp).shadow(16.dp, RoundedCornerShape(24.dp))
                )
            }
        }

        if (showBulkDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showBulkDeleteDialog = false },
                icon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp)) },
                title = { Text("Delete Songs") },
                text = { Text("Are you sure you want to delete ${selectedSongUris.size} selected song(s)? This action cannot be undone.") },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        onClick = {
                            val songsToDelete = songs.filter { selectedSongUris.contains(it.uri.toString()) }
                            libraryViewModel.requestDelete(context, songsToDelete)

                            selectedSongUris = emptySet()
                            showBulkDeleteDialog = false
                        }
                    ) { Text("Delete", color = MaterialTheme.colorScheme.onError) }
                },
                dismissButton = { TextButton(onClick = { showBulkDeleteDialog = false }) { Text("Cancel") } }
            )
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

@Composable
fun PremiumBottomBar(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp).navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f), shape = RoundedCornerShape(32.dp),
            modifier = Modifier.shadow(24.dp, RoundedCornerShape(32.dp), spotColor = MaterialTheme.colorScheme.primary)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavItem(icon = Icons.Default.LibraryMusic, isSelected = selectedTab == 0, onClick = { onTabSelected(0) })
                BottomNavItem(icon = Icons.Default.Favorite, isSelected = selectedTab == 1, onClick = { onTabSelected(1) })
                BottomNavItem(icon = Icons.Default.Person, isSelected = selectedTab == 2, onClick = { onTabSelected(2) })
                BottomNavItem(icon = Icons.AutoMirrored.Filled.PlaylistAdd, isSelected = selectedTab == 3, onClick = { onTabSelected(3) })
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
        modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick).padding(8.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = animatedColor, modifier = Modifier.size(28.dp).scale(animatedScale))
        Spacer(modifier = Modifier.height(6.dp))
        Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(if (isSelected) animatedColor else Color.Transparent).shadow(if (isSelected) 8.dp else 0.dp, CircleShape, spotColor = animatedColor))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongCard(
    song: Song, isFavorite: Boolean, isCurrentlyPlaying: Boolean, isPlaying: Boolean,
    isSelectionMode: Boolean, isSelected: Boolean,
    onFavoriteToggle: () -> Unit, onAddToPlaylist: () -> Unit, onLongClick: () -> Unit,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val fallbackIcon = rememberVectorPainter(Icons.Default.MusicNote)
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    else if (isCurrentlyPlaying) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    val textColor = if (isCurrentlyPlaying || isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(60.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            if (song.albumArtUri == null || song.albumArtUri.toString().isEmpty()) {
                Icon(Icons.Default.MusicNote, null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(song.albumArtUri).crossfade(true).build(), contentDescription = null, contentScale = ContentScale.Crop, error = fallbackIcon, fallback = fallbackIcon, modifier = Modifier.fillMaxSize())
            }
            if (isSelected) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                }
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = song.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, color = textColor,
                    modifier = Modifier.weight(1f, fill = false).then(if (isCurrentlyPlaying) Modifier.basicMarquee() else Modifier)
                )
                if (isCurrentlyPlaying && !isSelectionMode) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.padding(bottom = 4.dp)) { PlayingVisualizer(isPlaying = isPlaying) }
                }
            }
            Text(text = song.artist, fontSize = 14.sp, maxLines = 1, color = textColor.copy(alpha = 0.8f), modifier = if (isCurrentlyPlaying) Modifier.basicMarquee() else Modifier)
        }
        Spacer(modifier = Modifier.width(8.dp))

        if (!isSelectionMode) {
            IconButton(onClick = onFavoriteToggle) { Icon(imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, contentDescription = "Favorite", tint = if (isFavorite) Color.Red else textColor.copy(alpha = 0.8f)) }

            if (onRemoveFromPlaylist != null) {
                IconButton(onClick = onRemoveFromPlaylist) { Icon(Icons.Default.RemoveCircleOutline, null, tint = MaterialTheme.colorScheme.error) }
            } else {
                IconButton(onClick = onAddToPlaylist) { Icon(Icons.Default.Add, null, tint = textColor.copy(alpha = 0.8f)) }
            }
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
            Text(text = song.title, fontWeight = FontWeight.Bold, maxLines = 1, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.basicMarquee())
            Text(text = song.artist, fontSize = 12.sp, maxLines = 1, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f), modifier = Modifier.basicMarquee())
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onSkipPrevious) { Icon(Icons.Default.SkipPrevious, null, tint = MaterialTheme.colorScheme.onPrimaryContainer) }
            IconButton(onClick = onPlayPause) { Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(32.dp)) }
            IconButton(onClick = onSkipNext) { Icon(Icons.Default.SkipNext, null, tint = MaterialTheme.colorScheme.onPrimaryContainer) }
        }
    }
}

@Composable
fun PlayingVisualizer(isPlaying: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "eq_transition")
    val heights = List(4) { index ->
        infiniteTransition.animateFloat(
            initialValue = 0.2f, targetValue = 1f,
            animationSpec = infiniteRepeatable(animation = tween(durationMillis = 300 + (index * 150), easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
            label = "bar_$index"
        )
    }
    val animatedHeights = heights.map { heightState ->
        animateFloatAsState(targetValue = if (isPlaying) heightState.value else 0.2f, animationSpec = tween(durationMillis = 300), label = "pause_settle")
    }

    Row(modifier = Modifier.height(28.dp).padding(bottom = 6.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        animatedHeights.forEach { animatedHeight ->
            Box(modifier = Modifier.width(4.dp).fillMaxHeight(animatedHeight.value).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
        }
    }
}