package com.example.musicplayer.data.repository

import android.net.Uri
import com.example.musicplayer.data.local.dao.FavoriteDao
import com.example.musicplayer.data.local.entity.FavoriteEntity
import com.example.musicplayer.data.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoriteRepository @Inject constructor(
    private val favoriteDao: FavoriteDao
) {
    // Converts Database Entities back into normal Song objects for the UI
    fun getFavoriteSongs(): Flow<List<Song>> {
        return favoriteDao.getAllFavorites().map { entities ->
            entities.map { entity ->
                Song(
                    id = entity.songId,
                    title = entity.title,
                    artist = entity.artist,
                    albumArtUri = if (entity.albumArtUri.isNotEmpty()) Uri.parse(entity.albumArtUri) else null,
                    uri = android.content.ContentUris.withAppendedId(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, entity.songId),
                    durationMs = 0L
                )
            }
        }
    }

    suspend fun toggleFavorite(song: Song) {
        val exists = favoriteDao.isFavorite(song.id)
        if (exists) {
            favoriteDao.deleteFavorite(song.id)
        } else {
            favoriteDao.insertFavorite(
                FavoriteEntity(
                    songId = song.id,
                    title = song.title,
                    artist = song.artist,
                    albumArtUri = song.albumArtUri?.toString() ?: ""
                )
            )
        }
    }
}