package com.example.musicplayer.util

import java.security.MessageDigest

object PasswordUtils {
    fun hash(email: String, password: String): String {
        val input = "${email.lowercase().trim()}:$password"
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun verify(email: String, password: String, storedHash: String): Boolean =
        hash(email, password) == storedHash
}