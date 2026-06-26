package com.example.musicplayer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import com.example.musicplayer.data.model.Song
import com.example.musicplayer.player.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class RepeatState { OFF, ONCE, TOTALLY }

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerController: PlayerController
) : ViewModel() {

    private val exoPlayer = playerController.exoPlayer

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentPosition = MutableStateFlow(0f)
    val currentPosition: StateFlow<Float> = _currentPosition

    // THE RESTORED DURATION VARIABLE
    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration

    // THE SPEED VARIABLE
    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled

    private val _repeatState = MutableStateFlow(RepeatState.OFF)
    val repeatState: StateFlow<RepeatState> = _repeatState

    private var progressJob: Job? = null
    private var currentQueue: List<Song> = emptyList()

    init {
        _isShuffleEnabled.value = exoPlayer.shuffleModeEnabled

        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) startProgressTracker() else stopProgressTracker()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) {
                    if (_repeatState.value == RepeatState.ONCE) {
                        _repeatState.value = RepeatState.OFF
                        exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
                    }
                } else if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK || reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                    if (_repeatState.value == RepeatState.ONCE) {
                        _repeatState.value = RepeatState.OFF
                        exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
                    }
                }
                updateCurrentSong(mediaItem)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _duration.value = exoPlayer.duration.coerceAtLeast(0L)
                }
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _isShuffleEnabled.value = shuffleModeEnabled
            }
        })
    }

    // THE SPEED LOGIC
    fun setPlaybackSpeed(speed: Float) {
        exoPlayer.playbackParameters = PlaybackParameters(speed)
        _playbackSpeed.value = speed
    }

    fun playQueue(songs: List<Song>, startIndex: Int) {
        currentQueue = songs
        playerController.playQueue(songs, startIndex)
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
    }

    fun skipNext() {
        when (_repeatState.value) {
            RepeatState.TOTALLY -> exoPlayer.seekTo(0L)
            RepeatState.ONCE -> {
                _repeatState.value = RepeatState.OFF
                exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
                exoPlayer.seekTo(0L)
            }
            RepeatState.OFF -> {
                val nextIndex = exoPlayer.currentMediaItemIndex + 1
                if (nextIndex < exoPlayer.mediaItemCount) {
                    exoPlayer.seekTo(nextIndex, 0L)
                } else {
                    exoPlayer.seekTo(0, 0L)
                    exoPlayer.pause()
                }
            }
        }
    }

    fun skipPrevious() {
        when (_repeatState.value) {
            RepeatState.TOTALLY -> exoPlayer.seekTo(0L)
            RepeatState.ONCE -> {
                _repeatState.value = RepeatState.OFF
                exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
                exoPlayer.seekTo(0L)
            }
            RepeatState.OFF -> {
                val prevIndex = exoPlayer.currentMediaItemIndex - 1
                if (prevIndex >= 0) {
                    exoPlayer.seekTo(prevIndex, 0L)
                } else {
                    exoPlayer.seekTo(0, 0L)
                }
            }
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
        _currentPosition.value = positionMs.toFloat()
    }

    fun toggleShuffle() {
        exoPlayer.shuffleModeEnabled = !exoPlayer.shuffleModeEnabled
    }

    fun toggleRepeatMode(): RepeatState {
        val nextState = when (_repeatState.value) {
            RepeatState.OFF -> {
                exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
                RepeatState.ONCE
            }
            RepeatState.ONCE -> {
                exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
                RepeatState.TOTALLY
            }
            RepeatState.TOTALLY -> {
                exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
                RepeatState.OFF
            }
        }
        _repeatState.value = nextState
        return nextState
    }

    private fun updateCurrentSong(mediaItem: MediaItem?) {
        if (mediaItem == null) return
        val songId = mediaItem.mediaId
        _currentSong.value = currentQueue.find { it.uri.toString() == songId }
        _duration.value = exoPlayer.duration.coerceAtLeast(0L)
        _currentPosition.value = 0f
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                _currentPosition.value = exoPlayer.currentPosition.toFloat()
                delay(1000L)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        _currentPosition.value = exoPlayer.currentPosition.toFloat()
    }

    override fun onCleared() {
        super.onCleared()
        stopProgressTracker()
    }
}