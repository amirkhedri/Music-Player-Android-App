package com.example.musicplayer.data.repository

import com.example.musicplayer.data.local.dao.PlaylistDao
import com.example.musicplayer.data.local.entity.PlaylistEntity
import com.example.musicplayer.data.local.entity.PlaylistSongCrossRef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepository @Inject constructor(
    private val playlistDao: PlaylistDao
) {
    fun getPlaylistsByUser(userId: Long): Flow<List<PlaylistEntity>> {
        return playlistDao.getPlaylistsByUser(userId)
    }

    fun getSongIdsForPlaylist(playlistId: Long): Flow<List<Long>> {
        return playlistDao.getSongIdsForPlaylist(playlistId)
    }

    suspend fun createPlaylist(userId: Long, name: String, description: String? = null) {
        withContext(Dispatchers.IO) {
            val newPlaylist = PlaylistEntity(
                ownerUserId = userId,
                name = name,
                description = description
            )
            playlistDao.insertPlaylist(newPlaylist)
        }
    }

    suspend fun addSongToPlaylist(playlistId: Long, songId: Long) {
        withContext(Dispatchers.IO) {
            val crossRef = PlaylistSongCrossRef(playlistId, songId)
            playlistDao.addSongToPlaylist(crossRef)
        }
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        withContext(Dispatchers.IO) {
            playlistDao.removeSongFromPlaylist(playlistId, songId)
        }
    }
}