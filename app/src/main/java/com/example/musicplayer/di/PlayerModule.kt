package com.example.musicplayer.di

import android.content.Context
import com.example.musicplayer.player.PlayerController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlayerModule {

    @Provides
    @Singleton
    fun providePlayerController(
        @ApplicationContext context: Context
    ): PlayerController {
        return PlayerController(context)
    }
}