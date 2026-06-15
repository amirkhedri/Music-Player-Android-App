package com.example.musicplayer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.musicplayer.ui.screens.auth.LoginScreen
import com.example.musicplayer.ui.screens.auth.RegisterScreen
import com.example.musicplayer.ui.screens.library.MainScreen
import com.example.musicplayer.ui.screens.player.PlayerScreen
import com.example.musicplayer.viewmodel.PlayerViewModel

@Composable
fun AppNavGraph(startDestination: String) {
    val navController = rememberNavController()
    // We hoist this here so the music keeps playing when navigating screens!
    val playerViewModel: PlayerViewModel = hiltViewModel()

    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") { LoginScreen(navController) }
        composable("register") { RegisterScreen(navController) }
        composable("library") { MainScreen(navController, playerViewModel) }
        composable("player") { PlayerScreen(navController, playerViewModel) }
    }
}