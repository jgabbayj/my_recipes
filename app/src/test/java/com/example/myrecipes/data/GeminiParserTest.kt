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

    @Test
    fun extractImageFromHtml_extractsOgImageCorrectly() {
        val html = """
            <html>
            <head>
                <meta property="og:image" content="https://example.com/images/lasagna.jpg" />
                <title>Best Lasagna</title>
            </head>
            <body>Yummy food</body>
            </html>
        """.trimIndent()

        val actual = GeminiParser.extractImageFromHtml(html, "https://example.com/recipe/lasagna")
        assertEquals("https://example.com/images/lasagna.jpg", actual)
    }

    @Test
    fun extractImageFromHtml_extractsJsonLdImageCorrectly() {
        val html = """
            <html>
            <script type="application/ld+json">
            {
                "@context": "https://schema.org",
                "@type": "Recipe",
                "name": "Chocolate Cookies",
                "image": "https://example.com/cookie.png"
            }
            </script>
            </html>
        """.trimIndent()

        val actual = GeminiParser.extractImageFromHtml(html, "https://example.com/recipe/cookies")
        assertEquals("https://example.com/cookie.png", actual)
    }

    @Test
    fun extractImageFromHtml_resolvesRelativeUrlsCorrectly() {
        val html = """
            <html>
            <head>
                <meta property="og:image" content="/assets/pasta.webp" />
            </head>
            </html>
        """.trimIndent()

        val actual = GeminiParser.extractImageFromHtml(html, "https://example.com/recipes/pasta")
        assertEquals("https://example.com/assets/pasta.webp", actual)
    }

    @Test
    fun extractYouTubeThumbnail_resolvesCorrectly() {
        val url1 = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        val thumbnail1 = GeminiParser.extractYouTubeThumbnail(url1)
        assertEquals("https://img.youtube.com/vi/dQw4w9WgXcQ/hqdefault.jpg", thumbnail1)

        val url2 = "https://youtu.be/dQw4w9WgXcQ?t=10"
        val thumbnail2 = GeminiParser.extractYouTubeThumbnail(url2)
        assertEquals("https://img.youtube.com/vi/dQw4w9WgXcQ/hqdefault.jpg", thumbnail2)

        val url3 = "https://www.youtube.com/shorts/j3MbZOcl0ug"
        val thumbnail3 = GeminiParser.extractYouTubeThumbnail(url3)
        assertEquals("https://img.youtube.com/vi/j3MbZOcl0ug/hqdefault.jpg", thumbnail3)
    }

    @Test
    fun extractImageFromHtml_handlesSpacesAroundEquals() {
        val html = """
            <html>
            <head>
                <meta property  =  "og:image" content  =  "https://example.com/images/lasagna.jpg" />
            </head>
            </html>
        """.trimIndent()

        val actual = GeminiParser.extractImageFromHtml(html, "https://example.com/recipe/lasagna")
        assertEquals("https://example.com/images/lasagna.jpg", actual)
    }

    @Test
    fun extractImageFromHtml_decodesHtmlEntities() {
        val html = """
            <html>
            <head>
                <meta property="og:image" content="https://images.unsplash.com/photo-154?auto=format&amp;fit=crop" />
            </head>
            </html>
        """.trimIndent()

        val actual = GeminiParser.extractImageFromHtml(html, "https://example.com/recipe/lasagna")
        assertEquals("https://images.unsplash.com/photo-154?auto=format&fit=crop", actual)
    }

    @Test
    fun extractImageFromHtml_handlesNestedJsonLdArray() {
        val html = """
            <html>
            <script type="application/ld+json">
            {
                "@context": "https://schema.org",
                "@type": "Recipe",
                "image": [
                    "https://example.com/photo-1x1.jpg",
                    "https://example.com/photo-4x3.jpg"
                ]
            }
            </script>
            </html>
        """.trimIndent()

        val actual = GeminiParser.extractImageFromHtml(html, "https://example.com/recipe/lasagna")
        assertEquals("https://example.com/photo-1x1.jpg", actual)
    }

    @Test
    fun extractImageFromHtml_handlesJsonLdGraph() {
        val html = """
            <html>
            <script type="application/ld+json">
            {
                "@context": "https://schema.org",
                "@graph": [
                    {
                        "@type": "Recipe",
                        "image": {
                            "@type": "ImageObject",
                            "url": "https://example.com/graph-image.jpg"
                        }
                    }
                ]
            }
            </script>
            </html>
        """.trimIndent()

        val actual = GeminiParser.extractImageFromHtml(html, "https://example.com/recipe/lasagna")
        assertEquals("https://example.com/graph-image.jpg", actual)
    }
}
