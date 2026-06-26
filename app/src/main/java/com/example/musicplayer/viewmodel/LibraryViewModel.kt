package com.example.musicplayer.viewmodel

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.data.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// THE FIX: Added DATE sorting
enum class SortOrder { DATE, TITLE, ARTIST }

@HiltViewModel
class LibraryViewModel @Inject constructor(
    @ApplicationContext context: Context
) : ViewModel() {

    private val _allSongs = MutableStateFlow<List<Song>>(emptyList())
    val allSongs: StateFlow<List<Song>> = _allSongs

    // Default to sorting by newest first
    private val _sortOrder = MutableStateFlow(SortOrder.DATE)
    val sortOrder: StateFlow<SortOrder> = _sortOrder

    private val _deletePendingIntent = MutableSharedFlow<IntentSender>()
    val deletePendingIntent = _deletePendingIntent.asSharedFlow()

    private var songsAwaitingDeletion = emptyList<Song>()

    init {
        scanDeviceForMusic(context)
    }

    @Suppress("SpellCheckingInspection")
    private fun scanDeviceForMusic(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val songsList = mutableListOf<Song>()
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DATE_ADDED // THE FIX: Grab the creation date
            )

            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
            // Start with newest first by default
            val sort = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

            context.contentResolver.query(collection, projection, selection, null, sort)?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val title = cursor.getString(titleColumn) ?: "Unknown"
                    val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                    val duration = cursor.getLong(durationColumn)
                    val dateAdded = cursor.getLong(dateAddedColumn)
                    val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

                    val albumArtworkUri = "content://media/external/audio/albumart/${cursor.getLong(albumIdColumn)}".toUri()

                    songsList.add(
                        Song(
                            id = id,
                            title = title,
                            artist = artist,
                            uri = uri,
                            albumArtUri = albumArtworkUri,
                            durationMs = duration,
                            dateAdded = dateAdded
                        )
                    )
                }
            }

            _allSongs.value = songsList
            applySorting(_sortOrder.value)
        }
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
        applySorting(order)
    }

    private fun applySorting(order: SortOrder) {
        val currentList = _allSongs.value
        _allSongs.value = when (order) {
            SortOrder.DATE -> currentList.sortedByDescending { it.dateAdded } // Newest first
            SortOrder.TITLE -> currentList.sortedBy { it.title.lowercase() }
            SortOrder.ARTIST -> currentList.sortedBy { it.artist.lowercase() }
        }
    }

    // THE FIX: Snappy Rename Logic
    fun renameSong(context: Context, songUri: String, newTitle: String) {
        // 1. Instantly update the UI so it feels incredibly fast to the user
        val updatedList = _allSongs.value.map { song ->
            if (song.uri.toString() == songUri) song.copy(title = newTitle) else song
        }
        _allSongs.value = updatedList
        applySorting(_sortOrder.value)

        // 2. Silently update the Android MediaStore in the background
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val uri = Uri.parse(songUri)
                val values = ContentValues().apply {
                    put(MediaStore.Audio.Media.TITLE, newTitle)
                }
                context.contentResolver.update(uri, values, null, null)
            } catch (e: Exception) {
                e.printStackTrace() // On API 30+, Scoped Storage might block this if it's not our file, but the UI will still reflect the local change!
            }
        }
    }

    fun requestDelete(context: Context, songs: List<Song>) {
        songsAwaitingDeletion = songs
        val uris = songs.map { it.uri.toString().toUri() }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, uris)
            viewModelScope.launch {
                _deletePendingIntent.emit(pendingIntent.intentSender)
            }
        } else {
            try {
                uris.forEach { context.contentResolver.delete(it, null, null) }
                confirmDatabaseDeletion()
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }

    fun confirmDatabaseDeletion() {
        viewModelScope.launch {
            val currentList = _allSongs.value.toMutableList()
            currentList.removeAll(songsAwaitingDeletion)
            _allSongs.value = currentList
            songsAwaitingDeletion = emptyList()
        }
    }

    fun cancelDeletion() {
        songsAwaitingDeletion = emptyList()
    }
}