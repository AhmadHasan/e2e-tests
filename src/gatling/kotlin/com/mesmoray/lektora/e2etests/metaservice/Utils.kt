package com.mesmoray.lektora.e2etests.metaservice

import kotlin.random.Random

class Utils {
    companion object {
        private val countryCodes = mutableListOf<String>()
        private val languageCodes = mutableListOf<String>()
        private val random = Random.Default

        private const val CAPITALS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        private const val SMALLS = "abcdefghijklmnopqrstuvwxyz"

        fun randomChars(
            source: String,
            log: MutableList<String>,
            length: Int,
        ): String {
            var result: String
            do {
                result =
                    (1..length)
                        .map { source[random.nextInt(source.length)] }
                        .joinToString("")
            } while (log.contains(result))
            log.add(result)
            return result
        }

        @Synchronized
        fun randomCountryCode(): String {
            return randomChars(CAPITALS, countryCodes, 2)
        }

        @Synchronized
        fun randomLanguageCode(): String {
            return randomChars(SMALLS, languageCodes, 2)
        }

        fun randomString(length: Int): String {
            return (1..length)
                .map { SMALLS.random() }
                .joinToString("")
        }
    }
}
