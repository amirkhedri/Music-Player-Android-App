package com.example.musicplayer.viewmodel

import android.content.ContentUris
import android.content.Context
import android.content.IntentSender
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

enum class SortOrder { TITLE, ARTIST }

@HiltViewModel
class LibraryViewModel @Inject constructor(
    // THE FIX: Removed 'private val' so the annotation cleanly targets the parameter
    @ApplicationContext context: Context
) : ViewModel() {

    // --- State ---
    private val _allSongs = MutableStateFlow<List<Song>>(emptyList())
    val allSongs: StateFlow<List<Song>> = _allSongs

    private val _sortOrder = MutableStateFlow(SortOrder.TITLE)
    val sortOrder: StateFlow<SortOrder> = _sortOrder

    // --- OS Level Deletion State ---
    private val _deletePendingIntent = MutableSharedFlow<IntentSender>()
    val deletePendingIntent = _deletePendingIntent.asSharedFlow()

    private var songsAwaitingDeletion = emptyList<Song>()

    init {
        scanDeviceForMusic(context)
    }

    // --- The Core Scanner Logic ---
    // THE FIX: Suppressed the spell checker warning for the word "albumart"
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
                MediaStore.Audio.Media.ALBUM_ID
            )

            // Only grab actual music files (ignore voice memos, ringtones, etc.)
            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
            val sort = "${MediaStore.Audio.Media.TITLE} ASC"

            context.contentResolver.query(collection, projection, selection, null, sort)?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val title = cursor.getString(titleColumn) ?: "Unknown"
                    val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                    val duration = cursor.getLong(durationColumn)
                    val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

                    // THE FIX: Used the .toUri() extension function instead of Uri.parse()
                    val albumArtworkUri = "content://media/external/audio/albumart/${cursor.getLong(albumIdColumn)}".toUri()

                    // THE FIX: Passed 'id' directly as a Long instead of id.toString()
                    songsList.add(
                        Song(
                            id = id,
                            title = title,
                            artist = artist,
                            uri = uri,
                            albumArtUri = albumArtworkUri,
                            durationMs = duration
                        )
                    )
                }
            }

            // Push the loaded songs to the UI
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
            SortOrder.TITLE -> currentList.sortedBy { it.title.lowercase() }
            SortOrder.ARTIST -> currentList.sortedBy { it.artist.lowercase() }
        }
    }

    // --- OS Level Deletion Logic ---
    fun requestDelete(context: Context, songs: List<Song>) {
        songsAwaitingDeletion = songs

        // THE FIX: Replaced Uri.parse() with .toUri() extension
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
            // Remove the physically deleted songs from the UI state instantly
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