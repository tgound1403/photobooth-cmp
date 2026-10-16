package com.example.photogether.core.render

import java.util.Base64
import java.util.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class RecipeCodeTest {

    private val sample =
        EditParams(
            filterId = "golden_hour",
            filterIntensity = 0.6f,
            exposure = 0.25f,
            contrast = -0.3f,
            temperature = 0.4f,
            grain = 0.3f,
            crop = CropParams(aspect = CropAspect.R3_4, quarterTurns = 1, flipHorizontal = true),
        )

    private fun base64Of(text: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(text.toByteArray(Charsets.UTF_8))

    private fun assertDecodeError(code: String) {
        val result = RecipeCode.decode(code)
        assertTrue("expected error for <$code> but got $result", result is RecipeCodeResult.Error)
    }

    @Test
    fun encodeThenDecodeGivesTheSameEditParams() {
        val code = RecipeCode.encode(sample)
        assertTrue(code.startsWith(RecipeCode.PREFIX))
        assertEquals(RecipeCodeResult.Ok(sample), RecipeCode.decode(code))
    }

    @Test
    fun codeUsesOnlyUrlSafeCharactersWithoutPadding() {
        val code = RecipeCode.encode(sample).removePrefix(RecipeCode.PREFIX)
        assertTrue(code.all { it.isLetterOrDigit() || it == '-' || it == '_' })
        assertTrue(!code.contains('='))
    }

    @Test
    fun defaultParamsRoundTrip() {
        assertEquals(RecipeCodeResult.Ok(EditParams()), RecipeCode.decode(RecipeCode.encode(EditParams())))
    }

    @Test
    fun surroundingWhitespaceIsIgnored() {
        val code = "  \n${RecipeCode.encode(sample)}\t\n"
        assertEquals(RecipeCodeResult.Ok(sample), RecipeCode.decode(code))
    }

    @Test
    fun badInputReturnsErrorInsteadOfThrowing() {
        assertDecodeError("")
        assertDecodeError("hello")
        assertDecodeError("PGR2:" + base64Of("{}"))
        assertDecodeError(RecipeCode.PREFIX)
        assertDecodeError(RecipeCode.PREFIX + "!!!!")
        assertDecodeError(RecipeCode.PREFIX + "A") // one character cannot be a whole byte
        assertDecodeError(RecipeCode.PREFIX + base64Of("not json"))
        assertDecodeError(RecipeCode.PREFIX + base64Of("[1,2,3]"))
        assertDecodeError(RecipeCode.PREFIX + base64Of("null"))
        assertDecodeError(RecipeCode.PREFIX + base64Of("""{"crop":{"aspect":"NOT_AN_ASPECT"}}"""))
        assertDecodeError(RecipeCode.PREFIX + "A".repeat(9000))
    }

    @Test
    fun invalidUtf8BytesReturnErrorInsteadOfThrowing() {
        val bytes = Base64Url.encode(byteArrayOf(0xff.toByte(), 0xfe.toByte(), 0x00, 0x7b))
        assertDecodeError(RecipeCode.PREFIX + bytes)
    }

    @Test
    fun decodedValuesAreClampedToTheirRanges() {
        val code = RecipeCode.PREFIX + base64Of("""{"exposure":9,"fade":-3,"filterIntensity":2}""")
        assertEquals(
            RecipeCodeResult.Ok(EditParams(exposure = 1f, fade = 0f, filterIntensity = 1f)),
            RecipeCode.decode(code),
        )
    }

    @Test
    fun legacyRecipeJsonWithMissingFieldsDecodesToDefaults() {
        // A recipe saved before most fields existed: only two keys, no crop, no grain.
        val code = RecipeCode.PREFIX + base64Of("""{"filterId":"kodak","exposure":0.5}""")
        assertEquals(
            RecipeCodeResult.Ok(EditParams(filterId = "kodak", exposure = 0.5f)),
            RecipeCode.decode(code),
        )
        assertEquals(EditParams(), EditParams.fromJson("{}"))
    }

    @Test
    fun oldRecipeCodeDecodesWithAutoWhiteBalanceAndNoSoftwareEv() {
        // Recipe code as produced before the Kelvin and EV fields existed.
        val code = RecipeCode.PREFIX + base64Of("""{"filterId":"kodak","temperature":0.4,"grain":0.3}""")
        val result = RecipeCode.decode(code)
        assertTrue("expected Ok but got $result", result is RecipeCodeResult.Ok)
        val params = (result as RecipeCodeResult.Ok).params
        assertEquals(null, params.kelvin)
        assertEquals(0f, params.softwareEv, 0f)
        assertEquals(EditParams(filterId = "kodak", temperature = 0.4f, grain = 0.3f), params)
    }

    @Test
    fun kelvinSurvivesEncodeAndDecode() {
        val withKelvin = sample.copy(kelvin = 3200, softwareEv = 0.5f)
        assertEquals(RecipeCodeResult.Ok(withKelvin), RecipeCode.decode(RecipeCode.encode(withKelvin)))
    }

    @Test
    fun handWrittenBase64MatchesJavaReferenceEncoder() {
        val random = Random(42)
        repeat(200) {
            val bytes = ByteArray(random.nextInt(64)) { random.nextInt().toByte() }
            val expected = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
            assertEquals(expected, Base64Url.encode(bytes))
            assertEquals(bytes.toList(), Base64Url.decode(expected)?.toList())
        }
    }

    @Test
    fun base64DecodeRejectsCharactersOutsideTheAlphabet() {
        assertEquals(null, Base64Url.decode("ab+c"))
        assertEquals(null, Base64Url.decode("ab/c"))
        if (Base64Url.decode("abcde") != null) fail("length 5 is never valid")
    }
}
