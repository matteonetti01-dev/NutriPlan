package com.example.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject

data class Ingredient(
    val name: String,
    val quantity: String,
    val calories: Int = 0,
    val protein: Int = 0,
    val carbs: Int = 0,
    val fat: Int = 0
) {
    fun toFormattedString(): String {
        return "• $name $quantity"
    }

    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("name", name)
            put("quantity", quantity)
            put("calories", calories)
            put("protein", protein)
            put("carbs", carbs)
            put("fat", fat)
        }
    }

    companion object {
        fun fromJsonObject(obj: JSONObject): Ingredient {
            return Ingredient(
                name = obj.optString("name", ""),
                quantity = obj.optString("quantity", ""),
                calories = obj.optInt("calories", 0),
                protein = obj.optInt("protein", 0),
                carbs = obj.optInt("carbs", 0),
                fat = obj.optInt("fat", 0)
            )
        }

        fun listToJson(list: List<Ingredient>): String {
            val arr = JSONArray()
            list.forEach { arr.put(it.toJsonObject()) }
            return arr.toString()
        }

        fun listFromJson(jsonStr: String?): List<Ingredient> {
            if (jsonStr.isNullOrBlank()) return emptyList()
            val result = mutableListOf<Ingredient>()
            try {
                val arr = JSONArray(jsonStr)
                for (i in 0 until arr.length()) {
                    result.add(fromJsonObject(arr.getJSONObject(i)))
                }
            } catch (e: Exception) {
                // Return empty if parsing failed
            }
            return result
        }
    }
}

@Entity(tableName = "plans")
data class PlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val caloriesTarget: Int,
    val proteinTarget: Int,
    val carbsTarget: Int,
    val fatTarget: Int,
    val mealsCount: Int = 4,
    val isActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "meal_slots",
    foreignKeys = [
        ForeignKey(
            entity = PlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("planId")]
)
data class MealSlotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long,
    val orderIndex: Int,
    val name: String,
    val customCalories: Int? = null,
    val customProtein: Int? = null,
    val customCarbs: Int? = null,
    val customFat: Int? = null
)

@Entity(
    tableName = "meal_alternatives",
    foreignKeys = [
        ForeignKey(
            entity = MealSlotEntity::class,
            parentColumns = ["id"],
            childColumns = ["slotId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("slotId"), Index("planId")]
)
data class MealAlternativeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val slotId: Long,
    val planId: Long,
    val name: String = "",
    val ingredientsJson: String = "[]",
    val totalCalories: Int = 0,
    val totalProtein: Int = 0,
    val totalCarbs: Int = 0,
    val totalFat: Int = 0,
    val notes: String = "",
    val photoUri: String? = null
) {
    val ingredients: List<Ingredient>
        get() = Ingredient.listFromJson(ingredientsJson)
}

@Entity(tableName = "logged_meals")
data class LoggedMealEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // "YYYY-MM-DD"
    val time: String, // "12:30"
    val name: String,
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int,
    val ingredientsSummary: String = "",
    val notes: String = "",
    val photoUri: String? = null,
    val sourceAlternativeId: Long? = null,
    val ingredientsJson: String = "[]"
) {
    val ingredients: List<Ingredient>
        get() {
            val fromJson = Ingredient.listFromJson(ingredientsJson)
            if (fromJson.isNotEmpty()) return fromJson
            if (ingredientsSummary.isNotBlank()) {
                val parts = ingredientsSummary.split(",")
                return parts.mapNotNull { p ->
                    val trimmed = p.trim()
                    if (trimmed.isNotBlank()) {
                        val tokens = trimmed.split(" ")
                        if (tokens.size >= 2) {
                            val qty = tokens.last()
                            val nm = tokens.dropLast(1).joinToString(" ")
                            Ingredient(name = nm, quantity = qty)
                        } else {
                            Ingredient(name = trimmed, quantity = "")
                        }
                    } else null
                }
            }
            return emptyList()
        }
}
