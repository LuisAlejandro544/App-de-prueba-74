package com.example.security

import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.SecureRandom

class KeyPairService {
    fun generateKeyPair(algorithm: String, keySize: Int): KeyPair {
        val keyGen = KeyPairGenerator.getInstance(algorithm, "BC")
        keyGen.initialize(keySize, SecureRandom())
        return keyGen.generateKeyPair()
    }
}
