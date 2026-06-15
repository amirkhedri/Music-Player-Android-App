package com.example.musicplayer.data.model

import android.net.Uri

/**
 * Represents an audio file found on the device.
 * This does NOT go in the Room database. It is generated dynamically
 * by scanning the device's MediaStore.
 */
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val uri: Uri,
    val albumArtUri: Uri? = null
)