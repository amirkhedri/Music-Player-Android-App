package com.example.musicplayer.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "playlist_song_cross_ref",
    primaryKeys = ["playlist_id", "song_id"],
    foreignKeys = [
        ForeignKey(
            entity        = PlaylistEntity::class,
            parentColumns = ["playlist_id"],
            childColumns  = ["playlist_id"],
            onDelete      = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["playlist_id"]), Index(value = ["song_id"])]
)
data class PlaylistSongCrossRef(
    @ColumnInfo(name = "playlist_id")
    val playlistId: Long,

    @ColumnInfo(name = "song_id")
    val songId: Long,

    @ColumnInfo(name = "position")
    val position: Int = 0
)