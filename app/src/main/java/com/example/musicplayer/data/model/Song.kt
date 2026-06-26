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
    val uri: android.net.Uri,
    val albumArtUri: android.net.Uri?,
    val durationMs: Long,
    val dateAdded: Long = 0L // Add this line!
)