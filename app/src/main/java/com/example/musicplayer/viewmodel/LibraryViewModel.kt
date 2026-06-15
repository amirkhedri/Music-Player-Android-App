package com.example.musicplayer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.data.model.Song
import com.example.musicplayer.data.repository.AudioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

enum class SortOrder { TITLE, ARTIST }

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val audioRepository: AudioRepository
) : ViewModel() {

    private val _sortOrder = MutableStateFlow(SortOrder.TITLE)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    val allSongs: StateFlow<List<Song>> = combine(
        audioRepository.getAudioFiles(),
        _sortOrder
    ) { songs, order ->
        when (order) {
            SortOrder.TITLE -> songs.sortedBy { it.title.lowercase() }
            SortOrder.ARTIST -> songs.sortedBy { it.artist.lowercase() }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }
}