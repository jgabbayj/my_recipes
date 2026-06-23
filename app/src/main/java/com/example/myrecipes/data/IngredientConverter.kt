package com.example.myrecipes.data

object IngredientConverter {
    enum class UnitType { MASS, VOLUME }
    enum class UnitSystem { METRIC, IMPERIAL }

    data class UnitDetails(
        val type: UnitType,
        val system: UnitSystem,
        val symbol: String,
        val singular: String,
        val plural: String,
        val toBaseFactor: Double, // Factor to convert to base unit (grams for MASS, ml for VOLUME)
        val matchRegex: Regex
    )

    val ALL_UNITS = listOf(
        UnitDetails(
            type = UnitType.MASS,
            system = UnitSystem.METRIC,
            symbol = "g",
            singular = "gram",
            plural = "grams",
            toBaseFactor = 1.0,
            matchRegex = Regex("^(?:g|grams?)$", RegexOption.IGNORE_CASE)
        ),
        UnitDetails(
            type = UnitType.MASS,
            system = UnitSystem.METRIC,
            symbol = "kg",
            singular = "kilogram",
            plural = "kilograms",
            toBaseFactor = 1000.0,
            matchRegex = Regex("^(?:kg|kilograms?|kilos?)$", RegexOption.IGNORE_CASE)
        ),
        UnitDetails(
            type = UnitType.MASS,
            system = UnitSystem.IMPERIAL,
            symbol = "oz",
            singular = "ounce",
            plural = "ounces",
            toBaseFactor = 28.3495,
            matchRegex = Regex("^(?:oz|ounces?)$", RegexOption.IGNORE_CASE)
        ),
        UnitDetails(
            type = UnitType.MASS,
            system = UnitSystem.IMPERIAL,
            symbol = "lb",
            singular = "pound",
            plural = "pounds",
            toBaseFactor = 453.592,
            matchRegex = Regex("^(?:lb|lbs|pounds?)$", RegexOption.IGNORE_CASE)
        ),
        UnitDetails(
            type = UnitType.VOLUME,
            system = UnitSystem.METRIC,
            symbol = "ml",
            singular = "milliliter",
            plural = "milliliters",
            toBaseFactor = 1.0,
            matchRegex = Regex("^(?:ml|milliliters?)$", RegexOption.IGNORE_CASE)
        ),
        UnitDetails(
            type = UnitType.VOLUME,
            system = UnitSystem.METRIC,
            symbol = "l",
            singular = "liter",
            plural = "liters",
            toBaseFactor = 1000.0,
            matchRegex = Regex("^(?:l|liters?)$", RegexOption.IGNORE_CASE)
        ),
        UnitDetails(
            type = UnitType.VOLUME,
            system = UnitSystem.IMPERIAL,
            symbol = "tsp",
            singular = "teaspoon",
            plural = "teaspoons",
            toBaseFactor = 5.0,
            matchRegex = Regex("^(?:tsp|teaspoons?|tspns?)$", RegexOption.IGNORE_CASE)
        ),
        UnitDetails(
            type = UnitType.VOLUME,
            system = UnitSystem.IMPERIAL,
            symbol = "tbsp",
            singular = "tablespoon",
            plural = "tablespoons",
            toBaseFactor = 15.0,
            matchRegex = Regex("^(?:tbsp|tablespoons?|tbspns?)$", RegexOption.IGNORE_CASE)
        ),
        UnitDetails(
            type = UnitType.VOLUME,
            system = UnitSystem.IMPERIAL,
            symbol = "cup",
            singular = "cup",
            plural = "cups",
            toBaseFactor = 240.0,
            matchRegex = Regex("^(?:cups?)$", RegexOption.IGNORE_CASE)
        ),
        UnitDetails(
            type = UnitType.VOLUME,
            system = UnitSystem.IMPERIAL,
            symbol = "fl oz",
            singular = "fluid ounce",
            plural = "fluid ounces",
            toBaseFactor = 29.5735,
            matchRegex = Regex("^(?:fl\\.?\\s*oz|fl\\.?\\s*ounces?|floz)$", RegexOption.IGNORE_CASE)
        )
    )

    fun findUnitDetails(unitStr: String): UnitDetails? {
        val trimmed = unitStr.trim().lowercase()
        return ALL_UNITS.find { it.matchRegex.matches(trimmed) }
    }

    fun parseNumber(str: String): Double? {
        val vulgarFractionMap = mapOf(
            '½' to 0.5, '⅓' to 1.0/3.0, '⅔' to 2.0/3.0, '¼' to 0.25, '¾' to 0.75,
            '⅛' to 0.125, '⅜' to 0.375, '⅝' to 0.625, '⅞' to 0.875,
            '⅕' to 0.2, '⅖' to 0.4, '⅗' to 0.6, '⅘' to 0.8,
            '⅙' to 1.0/6.0, '⅚' to 5.0/6.0
        )
        val normalized = str.trim().replace(Regex("""[-‐‑‒–—\s]+"""), " ")

        val vulgarRegex = Regex("""^(?:(\d+)\s*)?([½⅓⅔¼¾⅛⅜⅝⅞⅕⅖⅗⅘⅙⅚])$""")
        val vulgarMatch = vulgarRegex.matchEntire(normalized)
        if (vulgarMatch != null) {
            val whole = vulgarMatch.groupValues[1].toIntOrNull() ?: 0
            val charFraction = vulgarMatch.groupValues[2].firstOrNull()
            val fraction = vulgarFractionMap[charFraction] ?: 0.0
            return whole.toDouble() + fraction
        }

        val slashRegex = Regex("""^(?:(\d+)\s+)?(\d+)/(\d+)$""")
        val slashMatch = slashRegex.matchEntire(normalized)
        if (slashMatch != null) {
            val whole = slashMatch.groupValues[1].toIntOrNull() ?: 0
            val num = slashMatch.groupValues[2].toDoubleOrNull() ?: 0.0
            val den = slashMatch.groupValues[3].toDoubleOrNull() ?: 1.0
            if (den != 0.0) {
                return whole.toDouble() + (num / den)
            }
        }

        return normalized.toDoubleOrNull()
    }

    fun formatDecimal(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toInt().toString()
        } else {
            val rounded = Math.round(value * 100.0) / 100.0
            if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
        }
    }

    fun convertIngredientUnit(value: Double, unitDetails: UnitDetails, targetSystem: UnitSystem): String {
        var finalValue = value
        var finalUnitDetails = unitDetails

        if (unitDetails.system != targetSystem) {
            // Need to convert systems
            if (targetSystem == UnitSystem.METRIC) {
                if (unitDetails.type == UnitType.MASS) {
                    val grams = value * unitDetails.toBaseFactor
                    if (grams >= 1000.0) {
                        finalValue = grams / 1000.0
                        finalUnitDetails = findUnitDetails("kg")!!
                    } else {
                        finalValue = grams
                        finalUnitDetails = findUnitDetails("g")!!
                    }
                } else {
                    val ml = value * unitDetails.toBaseFactor
                    if (ml >= 1000.0) {
                        finalValue = ml / 1000.0
                        finalUnitDetails = findUnitDetails("l")!!
                    } else {
                        finalValue = ml
                        finalUnitDetails = findUnitDetails("ml")!!
                    }
                }
            } else {
                if (unitDetails.type == UnitType.MASS) {
                    val grams = value * unitDetails.toBaseFactor
                    val oz = grams * 0.035274
                    if (oz >= 16.0) {
                        finalValue = oz / 16.0
                        finalUnitDetails = findUnitDetails("lb")!!
                    } else {
                        finalValue = oz
                        finalUnitDetails = findUnitDetails("oz")!!
                    }
                } else {
                    val ml = value * unitDetails.toBaseFactor
                    if (ml >= 120.0) {
                        finalValue = ml / 240.0
                        finalUnitDetails = findUnitDetails("cup")!!
                    } else if (ml >= 15.0) {
                        finalValue = ml / 15.0
                        finalUnitDetails = findUnitDetails("tbsp")!!
                    } else {
                        finalValue = ml / 5.0
                        finalUnitDetails = findUnitDetails("tsp")!!
                    }
                }
            }
        }

        // Format the value
        val formattedValue = formatDecimal(finalValue)

        // Always format as symbol
        val formattedUnit = finalUnitDetails.symbol

        return "$formattedValue $formattedUnit"
    }

    fun convertIngredient(text: String, targetSystem: UnitSystem): String {
        // Regex matches quantity and unit
        val regex = Regex("""((?:\d+(?:\s+|[-‐‑‒–—])?)?(?:[½⅓⅔¼¾⅛⅜⅝⅞⅕⅖⅗⅘⅙⅚]|\d+/\d+)|\d+(?:\.\d+)?)\s*(g|kg|ml|l|grams?|kilograms?|kilos?|milliliters?|liters?|oz|ounces?|lbs?|pounds?|cups?|tbsp|tablespoons?|tbspns?|tsp|teaspoons?|tspns?|fl\.?\s*oz)\b""", RegexOption.IGNORE_CASE)

        return regex.replace(text) { matchResult ->
            val numStr = matchResult.groupValues[1]
            val unitStr = matchResult.groupValues[2]

            val value = parseNumber(numStr) ?: return@replace matchResult.value
            val unitDetails = findUnitDetails(unitStr) ?: return@replace matchResult.value

            convertIngredientUnit(value, unitDetails, targetSystem)
        }
    }
}
