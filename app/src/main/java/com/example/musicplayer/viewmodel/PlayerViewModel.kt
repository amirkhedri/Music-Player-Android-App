package com.example.musicplayer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.example.musicplayer.data.model.Song
import com.example.musicplayer.player.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerController: PlayerController
) : ViewModel() {

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private var currentQueue: List<Song> = emptyList()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    init {
        playerController.exoPlayer.addListener(object : Player.Listener {

            // THE FIX: Listen to ExoPlayer changing tracks automatically
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val playingUri = mediaItem?.localConfiguration?.uri?.toString()
                val song = currentQueue.find { it.uri.toString() == playingUri }
                if (song != null) {
                    _currentSong.value = song
                    _duration.value = 0L
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }
        })

        viewModelScope.launch {
            while (true) {
                if (_isPlaying.value) {
                    _currentPosition.value = playerController.exoPlayer.currentPosition
                    val actualDuration = playerController.exoPlayer.duration
                    if (actualDuration > 0) _duration.value = actualDuration
                }
                delay(1000)
            }
        }
    }

    fun playQueue(songs: List<Song>, startIndex: Int) {
        currentQueue = songs
        playerController.playQueue(songs, startIndex)
    }

    fun togglePlayPause() {
        if (_isPlaying.value) playerController.exoPlayer.pause()
        else playerController.exoPlayer.play()
    }

    fun toggleShuffle() {
        _isShuffleEnabled.value = !_isShuffleEnabled.value
        playerController.exoPlayer.shuffleModeEnabled = _isShuffleEnabled.value
    }

    // THE FIX: Let ExoPlayer handle skipping so the Notification stays in sync
    fun skipNext() { playerController.exoPlayer.seekToNext() }
    fun skipPrevious() { playerController.exoPlayer.seekToPrevious() }

    fun seekTo(position: Long) {
        playerController.exoPlayer.seekTo(position)
        _currentPosition.value = position
    }
}