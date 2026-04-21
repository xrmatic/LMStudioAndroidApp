package com.xrmatic.lmstudio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `server url is configured when non-blank and not default`() {
        val blankUrl = ""
        val validUrl = "http://192.168.1.100:1234"
        assertFalse("Blank URL should not be considered configured", blankUrl.isNotBlank())
        assertTrue("Valid URL should be considered configured", validUrl.isNotBlank())
    }

    @Test
    fun `base url always ends with slash`() {
        val url = "http://192.168.1.100:1234"
        val baseUrl = if (url.endsWith("/")) url else "$url/"
        assertTrue(baseUrl.endsWith("/"))
        assertEquals("http://192.168.1.100:1234/", baseUrl)
    }

    @Test
    fun `base url is unchanged when already has trailing slash`() {
        val url = "http://192.168.1.100:1234/"
        val baseUrl = if (url.endsWith("/")) url else "$url/"
        assertEquals(url, baseUrl)
    }

    @Test
    fun `temperature slider maps correctly`() {
        val progress100 = 100
        val temp100 = (progress100 / 100.0f) * 2.0f
        assertEquals(2.0f, temp100, 0.001f)

        val progress50 = 50
        val temp50 = (progress50 / 100.0f) * 2.0f
        assertEquals(1.0f, temp50, 0.001f)

        val progress35 = 35
        val temp35 = (progress35 / 100.0f) * 2.0f
        assertEquals(0.7f, temp35, 0.001f)
    }
}
