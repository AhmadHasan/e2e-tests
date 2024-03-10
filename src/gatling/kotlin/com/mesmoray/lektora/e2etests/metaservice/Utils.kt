package com.mesmoray.lektora.e2etests.metaservice

import kotlin.random.Random

class Utils {
    companion object {
        fun randomCountryCode(): String {
            val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            val random = Random.Default
            val firstChar = alphabet[random.nextInt(alphabet.length)]
            val secondChar = alphabet[random.nextInt(alphabet.length)]
            return "$firstChar$secondChar"
        }
    }
}
