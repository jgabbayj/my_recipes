package com.example.myrecipes.data

import org.junit.Assert.assertEquals
import org.junit.Test

class GeminiParserTest {

    @Test
    fun extractYouTubeDescription_extractsShortDescriptionCorrectly() {
        val html = """
            {"videoDetails":{"videoId":"123","title":"Yummy Cake","shortDescription":"Ingredients:\n- 1 cup sugar\n- 2 eggs\nEnjoy!","lengthSeconds":"600"}}
        """.trimIndent()
        
        val expected = "Ingredients:\n- 1 cup sugar\n- 2 eggs\nEnjoy!"
        val actual = GeminiParser.extractYouTubeDescription(html)
        
        assertEquals(expected, actual)
    }

    @Test
    fun extractYouTubeDescription_extractsSimpleTextFallbackCorrectly() {
        val html = """
            "description":{"simpleText":"This is the fallback description with \"escaped quotes\" and \\ backslashes."}
        """.trimIndent()
        
        val expected = "This is the fallback description with \"escaped quotes\" and \\ backslashes."
        val actual = GeminiParser.extractYouTubeDescription(html)
        
        assertEquals(expected, actual)
    }

    @Test
    fun extractYouTubeDescription_returnsEmptyIfNotFound() {
        val html = """
            <html><body>No description here</body></html>
        """.trimIndent()
        
        val actual = GeminiParser.extractYouTubeDescription(html)
        
        assertEquals("", actual)
    }
}
