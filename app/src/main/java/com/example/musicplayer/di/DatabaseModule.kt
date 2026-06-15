package com.example.musicplayer.di

import android.content.Context
import androidx.room.Room
import com.example.musicplayer.data.local.MusicPlayerDatabase
import com.example.musicplayer.data.local.dao.FavoriteDao
import com.example.musicplayer.data.local.dao.PlaylistDao
import com.example.musicplayer.data.local.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideMusicPlayerDatabase(
        @ApplicationContext context: Context
    ): MusicPlayerDatabase =
        Room.databaseBuilder(
            context,
            MusicPlayerDatabase::class.java,
            MusicPlayerDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideUserDao(db: MusicPlayerDatabase): UserDao = db.userDao()

    @Provides
    @Singleton
    fun providePlaylistDao(db: MusicPlayerDatabase): PlaylistDao = db.playlistDao()

    @Provides
    @Singleton
    fun provideFavoriteDao(db: MusicPlayerDatabase): FavoriteDao = db.favoriteDao()
}