package de.vyacheslav.kushchenko.sales.funnel.util

import java.security.SecureRandom

object PasswordGenerator {
    private val random = SecureRandom()
    private const val CHARS =
        "ABCDEFGHJKLMNPQRSTUVWXYZ" +
                "abcdefghijkmnopqrstuvwxyz" +
                "23456789"

    fun generate(length: Int = 16): String {
        return buildString(length) {
            repeat(length) {
                append(CHARS[random.nextInt(CHARS.length)])
            }
        }
    }
}