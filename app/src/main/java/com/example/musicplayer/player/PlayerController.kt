package com.example.musicplayer.player

import android.content.Context
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerController @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context).build()

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()
        exoPlayer.setAudioAttributes(audioAttributes, true)
    }

    // THE FIX: Handing the entire list to ExoPlayer so it knows there are next songs!
    fun playQueue(songs: List<com.example.musicplayer.data.model.Song>, startIndex: Int) {
        val mediaItems = songs.map { song ->
            val mediaMetadata = MediaMetadata.Builder()
                .setTitle(song.title)
                .setArtist(song.artist)
                .setArtworkUri(song.albumArtUri)
                .build()

            MediaItem.Builder()
                .setUri(song.uri)
                .setMediaId(song.uri.toString()) // We use URI to track the exact song
                .setMediaMetadata(mediaMetadata)
                .build()
        }

        exoPlayer.setMediaItems(mediaItems, startIndex, C.TIME_UNSET)
        exoPlayer.prepare()
        exoPlayer.play()

        val intent = Intent(context, PlaybackService::class.java)
        context.startService(intent)
    }

    fun pause() { exoPlayer.pause() }
    fun resume() { exoPlayer.play() }
}