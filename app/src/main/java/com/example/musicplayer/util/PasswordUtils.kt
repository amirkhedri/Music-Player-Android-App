package com.example.musicplayer.util

import java.security.MessageDigest

object PasswordUtils {
    fun hash(username: String, password: String): String {
        val input = "${username.lowercase().trim()}:$password"
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun verify(username: String, password: String, storedHash: String): Boolean =
        hash(username, password) == storedHash
}