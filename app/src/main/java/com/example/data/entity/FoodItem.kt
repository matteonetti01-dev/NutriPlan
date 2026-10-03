package com.example.data.entity

import kotlin.math.roundToInt

data class NutritionValues(
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int
)

data class FoodItem(
    val id: String,
    val name: String,
    val brand: String? = null,
    val servingDescription: String = "100g",
    val servingAmount: Double = 100.0,
    val servingUnit: String = "g",
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val source: String = "fatsecret" // "fatsecret" or "verified_db" or "custom"
) {
    val displayName: String
        get() = if (!brand.isNullOrBlank() && !name.contains(brand, ignoreCase = true)) {
            "$name ($brand)"
        } else {
            name
        }

    fun calculateForGrams(grams: Double): NutritionValues {
        val factor = if (grams > 0) grams / 100.0 else 1.0
        return NutritionValues(
            calories = (caloriesPer100g * factor).roundToInt().coerceAtLeast(0),
            protein = (proteinPer100g * factor).roundToInt().coerceAtLeast(0),
            carbs = (carbsPer100g * factor).roundToInt().coerceAtLeast(0),
            fat = (fatPer100g * factor).roundToInt().coerceAtLeast(0)
        )
    }

    fun toIngredient(quantityStr: String): Ingredient {
        val grams = parseQuantityToGrams(quantityStr)
        val macros = calculateForGrams(grams)
        return Ingredient(
            name = displayName,
            quantity = quantityStr.ifBlank { "${grams.toInt()}g" },
            calories = macros.calories,
            protein = macros.protein,
            carbs = macros.carbs,
            fat = macros.fat
        )
    }

    companion object {
        fun parseQuantityToGrams(quantityStr: String): Double {
            val cleaned = quantityStr.trim().lowercase()
            if (cleaned.isBlank()) return 100.0

            val regex = """([0-9]+(?:[.,][0-9]+)?)\s*(kg|g|ml|l|gr)?""".toRegex()
            val match = regex.find(cleaned)
            if (match != null) {
                val numStr = match.groupValues[1].replace(',', '.')
                val num = numStr.toDoubleOrNull() ?: 100.0
                val unit = match.groupValues.getOrNull(2) ?: "g"
                return when (unit) {
                    "kg", "l" -> num * 1000.0
                    else -> num
                }
            }
            return 100.0
        }
    }
}
