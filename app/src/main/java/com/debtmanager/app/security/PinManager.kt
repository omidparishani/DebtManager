package com.debtmanager.app.security

import java.security.MessageDigest

object PinManager {
    fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(pin.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun verifyPin(pin: String, hash: String): Boolean = hashPin(pin) == hash
}
