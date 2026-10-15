package com.example.cameraxapp.core.render

import java.io.ByteArrayOutputStream

/** Outcome of [RecipeCode.decode]. Bad input is reported here, never thrown. */
sealed interface RecipeCodeResult {
    /** [params] are already clamped to their documented ranges. */
    data class Ok(val params: EditParams) : RecipeCodeResult

    data class Error(val message: String) : RecipeCodeResult
}

/**
 * Share code for a recipe: [PREFIX] followed by the URL-safe base64 (no padding) of the
 * recipe's EditParams JSON. Pure Kotlin, usable from JVM unit tests.
 */
object RecipeCode {
    const val PREFIX = "PGR1:"

    /** Longer codes are rejected before decoding. A real recipe is well under 1 KB. */
    private const val MAX_CODE_LENGTH = 8192

    fun encode(params: EditParams): String =
        PREFIX + Base64Url.encode(params.toJson().toByteArray(Charsets.UTF_8))

    /** Accepts surrounding whitespace, so a code pasted with a trailing newline still works. */
    fun decode(code: String): RecipeCodeResult {
        val text = code.trim()
        if (!text.startsWith(PREFIX)) return RecipeCodeResult.Error("Not a recipe code")
        if (text.length > MAX_CODE_LENGTH) return RecipeCodeResult.Error("Recipe code is too long")
        val bytes = Base64Url.decode(text.substring(PREFIX.length))
            ?: return RecipeCodeResult.Error("Recipe code is damaged")
        val params = runCatching { EditParams.fromJson(String(bytes, Charsets.UTF_8)) }
            .getOrElse { return RecipeCodeResult.Error("Recipe data is not valid") }
        return RecipeCodeResult.Ok(params.clamped())
    }
}

/**
 * URL-safe base64 without padding. Written by hand because android.util.Base64 is not
 * available to JVM unit tests and java.util.Base64 needs API 26 (minSdk is 24).
 */
internal object Base64Url {
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"

    fun encode(input: ByteArray): String {
        val out = StringBuilder((input.size + 2) / 3 * 4)
        var i = 0
        while (i < input.size) {
            val b0 = input[i].toInt() and 0xff
            val b1 = if (i + 1 < input.size) input[i + 1].toInt() and 0xff else 0
            val b2 = if (i + 2 < input.size) input[i + 2].toInt() and 0xff else 0
            val n = (b0 shl 16) or (b1 shl 8) or b2
            out.append(ALPHABET[(n shr 18) and 63])
            out.append(ALPHABET[(n shr 12) and 63])
            if (i + 1 < input.size) out.append(ALPHABET[(n shr 6) and 63])
            if (i + 2 < input.size) out.append(ALPHABET[n and 63])
            i += 3
        }
        return out.toString()
    }

    /** Returns null for characters outside the alphabet or an impossible length. */
    fun decode(text: String): ByteArray? {
        if (text.length % 4 == 1) return null
        val out = ByteArrayOutputStream(text.length / 4 * 3 + 3)
        var buffer = 0
        var bits = 0
        for (c in text) {
            val value = ALPHABET.indexOf(c)
            if (value < 0) return null
            buffer = (buffer shl 6) or value
            bits += 6
            if (bits >= 8) {
                bits -= 8
                out.write((buffer shr bits) and 0xff)
            }
        }
        return out.toByteArray()
    }
}
