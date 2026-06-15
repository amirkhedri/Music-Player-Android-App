package com.example.musicplayer.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    // Tap into the phone's persistent storage
    private val prefs: SharedPreferences = context.getSharedPreferences("playlists_prefs", Context.MODE_PRIVATE)

    // Holds a Map of [Playlist Name] -> [List of Song URIs]
    private val _playlistState = MutableStateFlow<Map<String, List<String>>>(loadPlaylists())
    val playlistState: StateFlow<Map<String, List<String>>> = _playlistState.asStateFlow()

    private fun loadPlaylists(): Map<String, List<String>> {
        val jsonString = prefs.getString("playlists_data", "{}") ?: "{}"
        val map = mutableMapOf<String, List<String>>()
        try {
            val jsonObject = JSONObject(jsonString)
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val jsonArray = jsonObject.getJSONArray(key)
                val list = mutableListOf<String>()
                for (i in 0 until jsonArray.length()) {
                    list.add(jsonArray.getString(i))
                }
                map[key] = list
            }
        } catch (e: Exception) { e.printStackTrace() }
        return map
    }

    private fun savePlaylists(map: Map<String, List<String>>) {
        try {
            val jsonObject = JSONObject()
            map.forEach { (key, list) ->
                val jsonArray = JSONArray()
                list.forEach { jsonArray.put(it) }
                jsonObject.put(key, jsonArray)
            }
            prefs.edit().putString("playlists_data", jsonObject.toString()).apply()
            _playlistState.value = map
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun createPlaylist(name: String) {
        val current = _playlistState.value.toMutableMap()
        if (!current.containsKey(name)) {
            current[name] = emptyList()
            savePlaylists(current)
        }
    }

    fun deletePlaylist(name: String) {
        val current = _playlistState.value.toMutableMap()
        current.remove(name)
        savePlaylists(current)
    }

    fun renamePlaylist(oldName: String, newName: String) {
        val current = _playlistState.value.toMutableMap()
        if (current.containsKey(oldName) && !current.containsKey(newName)) {
            val songs = current.remove(oldName) ?: emptyList()
            current[newName] = songs
            savePlaylists(current)
        }
    }

    fun addSongToPlaylist(playlistName: String, songUri: String) {
        val current = _playlistState.value.toMutableMap()
        val songs = current[playlistName]?.toMutableList() ?: mutableListOf()
        if (!songs.contains(songUri)) {
            songs.add(songUri)
            current[playlistName] = songs
            savePlaylists(current)
        }
    }

    fun removeSongFromPlaylist(playlistName: String, songUri: String) {
        val current = _playlistState.value.toMutableMap()
        val songs = current[playlistName]?.toMutableList() ?: mutableListOf()
        songs.remove(songUri)
        current[playlistName] = songs
        savePlaylists(current)
    }
}