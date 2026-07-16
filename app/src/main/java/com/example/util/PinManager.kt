package com.example.util

import android.content.Context
import java.security.MessageDigest

object PinManager {
    private const val PREFS_NAME = "secure_app_prefs"
    private const val KEY_PIN_HASH = "app_pin_hash"
    private const val KEY_PIN_SALT = "app_pin_salt"

    fun isPinSet(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.contains(KEY_PIN_HASH)
    }

    fun savePin(context: Context, pin: String) {
        val salt = System.currentTimeMillis().toString()
        val hash = hashPin(pin, salt)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_PIN_HASH, hash)
            .putString(KEY_PIN_SALT, salt)
            .apply()
    }

    fun checkPin(context: Context, pin: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val salt = prefs.getString(KEY_PIN_SALT, "") ?: ""
        val inputHash = hashPin(pin, salt)
        return savedHash == inputHash
    }

    private fun hashPin(pin: String, salt: String): String {
        return try {
            val bytes = (pin + salt).toByteArray(Charsets.UTF_8)
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            digest.fold("") { str, it -> str + "%02x".format(it) }
        } catch (e: Exception) {
            pin
        }
    }
}
