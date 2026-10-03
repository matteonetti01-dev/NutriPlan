package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "food_cache")
data class CachedFoodEntity(
    @PrimaryKey val id: String,
    val name: String,
    val brand: String? = null,
    val servingDescription: String = "100g",
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val source: String = "fatsecret",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toFoodItem(): FoodItem {
        return FoodItem(
            id = id,
            name = name,
            brand = brand,
            servingDescription = servingDescription,
            servingAmount = 100.0,
            servingUnit = "g",
            caloriesPer100g = caloriesPer100g,
            proteinPer100g = proteinPer100g,
            carbsPer100g = carbsPer100g,
            fatPer100g = fatPer100g,
            source = source
        )
    }

    companion object {
        fun fromFoodItem(item: FoodItem): CachedFoodEntity {
            return CachedFoodEntity(
                id = item.id,
                name = item.name,
                brand = item.brand,
                servingDescription = item.servingDescription,
                caloriesPer100g = item.caloriesPer100g,
                proteinPer100g = item.proteinPer100g,
                carbsPer100g = item.carbsPer100g,
                fatPer100g = item.fatPer100g,
                source = item.source,
                timestamp = System.currentTimeMillis()
            )
        }
    }
}
