package com.morsel.recipes.data

import org.junit.Assert.assertEquals
import org.junit.Test

class IngredientConverterTest {

    @Test
    fun testParseNumber() {
        assertEquals(0.5, IngredientConverter.parseNumber("½") ?: 0.0, 0.001)
        assertEquals(1.5, IngredientConverter.parseNumber("1 ½") ?: 0.0, 0.001)
        assertEquals(0.5, IngredientConverter.parseNumber("1/2") ?: 0.0, 0.001)
        assertEquals(1.5, IngredientConverter.parseNumber("1 1/2") ?: 0.0, 0.001)
        assertEquals(2.0, IngredientConverter.parseNumber("2") ?: 0.0, 0.001)
        assertEquals(2.5, IngredientConverter.parseNumber("2.5") ?: 0.0, 0.001)
    }

    @Test
    fun testFormatDecimalWithDecimals() {
        // Whole numbers
        assertEquals("3", IngredientConverter.formatDecimal(3.0))
        assertEquals("0", IngredientConverter.formatDecimal(0.0))

        // Decimals
        assertEquals("0.5", IngredientConverter.formatDecimal(0.5))
        assertEquals("7.5", IngredientConverter.formatDecimal(7.5))
        assertEquals("1.5", IngredientConverter.formatDecimal(1.5))
        assertEquals("0.25", IngredientConverter.formatDecimal(0.25))
        assertEquals("2.25", IngredientConverter.formatDecimal(2.25))
        assertEquals("0.75", IngredientConverter.formatDecimal(0.75))
        assertEquals("0.33", IngredientConverter.formatDecimal(1.0/3.0))
        assertEquals("0.67", IngredientConverter.formatDecimal(2.0/3.0))
        assertEquals("0.13", IngredientConverter.formatDecimal(0.125))

        // Fallbacks
        assertEquals("1.7", IngredientConverter.formatDecimal(1.7))
        assertEquals("0.91", IngredientConverter.formatDecimal(0.91))
    }

    @Test
    fun testConvertIngredient_metricToImperial_symbols() {
        // 250g Ground Beef -> 250 * 0.035274 = 8.8185 oz -> 8.82 oz
        val input = "250g Ground Beef"
        val expected = "8.82 oz Ground Beef"
        val actual = IngredientConverter.convertIngredient(
            input,
            IngredientConverter.UnitSystem.IMPERIAL
        )
        assertEquals(expected, actual)
    }

    @Test
    fun testConvertIngredient_metricToImperial_pounds() {
        // 500g Ground Beef -> 1.1023 lb -> 1.1 lb
        val input = "500g Ground Beef"
        val expected = "1.1 lb Ground Beef"
        val actual = IngredientConverter.convertIngredient(
            input,
            IngredientConverter.UnitSystem.IMPERIAL
        )
        assertEquals(expected, actual)
    }

    @Test
    fun testConvertIngredient_metricToMetric_symbols() {
        val input = "500 grams Ground Beef"
        val expected = "500 g Ground Beef"
        val actual = IngredientConverter.convertIngredient(
            input,
            IngredientConverter.UnitSystem.METRIC
        )
        assertEquals(expected, actual)
    }

    @Test
    fun testConvertIngredient_imperialToMetric_symbols() {
        val input = "1/2 cup Water"
        val expected = "120 ml Water"
        val actual = IngredientConverter.convertIngredient(
            input,
            IngredientConverter.UnitSystem.METRIC
        )
        assertEquals(expected, actual)
    }

    @Test
    fun testConvertIngredient_singularPluralToSymbols() {
        // 1 gram -> 1 g
        assertEquals(
            "1 g sugar",
            IngredientConverter.convertIngredient("1 gram sugar", IngredientConverter.UnitSystem.METRIC)
        )
        // 2 grams -> 2 g
        assertEquals(
            "2 g sugar",
            IngredientConverter.convertIngredient("2 grams sugar", IngredientConverter.UnitSystem.METRIC)
        )
    }

    @Test
    fun testConvertIngredient_largeMetricVolume() {
        // 1.5 liters water -> 1.5 l water
        assertEquals(
            "1.5 l water",
            IngredientConverter.convertIngredient("1.5 liters water", IngredientConverter.UnitSystem.METRIC)
        )

        // 5 cups broth = 1200 ml = 1.2 liters -> 1.2 l broth
        assertEquals(
            "1.2 l broth",
            IngredientConverter.convertIngredient("5 cups broth", IngredientConverter.UnitSystem.METRIC)
        )
    }
}
