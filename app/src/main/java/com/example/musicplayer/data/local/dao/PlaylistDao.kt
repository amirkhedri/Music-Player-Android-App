package com.example.musicplayer.data.local.dao

import androidx.room.*
import com.example.musicplayer.data.local.entity.PlaylistEntity
import com.example.musicplayer.data.local.entity.PlaylistSongCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addSongToPlaylist(crossRef: PlaylistSongCrossRef)

    @Query(
        """
        DELETE FROM playlist_song_cross_ref
        WHERE playlist_id = :playlistId AND song_id = :songId
        """
    )
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)

    @Query(
        """
        SELECT * FROM playlists
        WHERE owner_user_id = :userId
        ORDER BY created_at DESC
        """
    )
    fun getPlaylistsByUser(userId: Long): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE playlist_id = :playlistId LIMIT 1")
    fun getPlaylistById(playlistId: Long): Flow<PlaylistEntity?>

    @Query(
        """
        SELECT song_id FROM playlist_song_cross_ref
        WHERE playlist_id = :playlistId
        ORDER BY position ASC
        """
    )
    fun getSongIdsForPlaylist(playlistId: Long): Flow<List<Long>>

    @Query(
        """
        SELECT COUNT(*) FROM playlist_song_cross_ref
        WHERE playlist_id = :playlistId AND song_id = :songId
        """
    )
    suspend fun isSongInPlaylist(playlistId: Long, songId: Long): Int
}