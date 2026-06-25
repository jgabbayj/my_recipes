package com.morsel.recipes.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ImageCacheTest {

    @Test
    fun getFileName_generatesConsistentNames() {
        val url = "https://images.unsplash.com/photo-1612874742237-6526221588e3?auto=format&fit=crop&w=800&q=80"
        
        val name1 = ImageCache.getFileName(url)
        val name2 = ImageCache.getFileName(url)
        
        assertEquals(name1, name2)
        assert(name1.endsWith(".jpg"))
    }

    @Test
    fun getFileName_generatesDifferentNamesForDifferentUrls() {
        val url1 = "https://images.unsplash.com/photo-1612874742237-6526221588e3"
        val url2 = "https://images.unsplash.com/photo-1499636136210-6f4ee915583e"
        
        val name1 = ImageCache.getFileName(url1)
        val name2 = ImageCache.getFileName(url2)
        
        assertNotEquals(name1, name2)
    }
}
