package com.example.data.repository

import com.example.data.dao.NutritionDao
import com.example.data.entity.Ingredient
import com.example.data.entity.LoggedMealEntity
import com.example.data.entity.MealAlternativeEntity
import com.example.data.entity.MealSlotEntity
import com.example.data.entity.PlanEntity
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NutritionRepository(private val dao: NutritionDao) {

    val allPlans: Flow<List<PlanEntity>> = dao.getAllPlans()
    val activePlan: Flow<PlanEntity?> = dao.getActivePlan()

    fun getSlotsForPlan(planId: Long): Flow<List<MealSlotEntity>> = dao.getSlotsForPlan(planId)
    fun getAlternativesForPlan(planId: Long): Flow<List<MealAlternativeEntity>> = dao.getAlternativesForPlan(planId)
    fun getLoggedMealsForDate(date: String): Flow<List<LoggedMealEntity>> = dao.getLoggedMealsForDate(date)

    suspend fun getActivePlanSync(): PlanEntity? = dao.getActivePlanSync()

    suspend fun checkAndSeedInitialData() {
        // App starts empty as requested by user. The user is prompted to create the first plan.
    }

    suspend fun switchActivePlan(planId: Long) {
        dao.switchActivePlan(planId)
    }

    suspend fun createPlan(
        name: String,
        calories: Int,
        protein: Int,
        carbs: Int,
        fat: Int,
        mealsCount: Int,
        makeActive: Boolean = false
    ): Long {
        if (makeActive) {
            dao.clearActivePlans()
        }
        val planId = dao.insertPlan(
            PlanEntity(
                name = name,
                caloriesTarget = calories,
                proteinTarget = protein,
                carbsTarget = carbs,
                fatTarget = fat,
                mealsCount = mealsCount,
                isActive = makeActive
            )
        )
        syncSlotsForPlan(planId, mealsCount, calories, protein, carbs, fat)
        return planId
    }

    suspend fun updatePlan(plan: PlanEntity) {
        dao.updatePlan(plan)
        syncSlotsForPlan(plan.id, plan.mealsCount, plan.caloriesTarget, plan.proteinTarget, plan.carbsTarget, plan.fatTarget)
    }

    suspend fun deletePlan(plan: PlanEntity) {
        val wasActive = plan.isActive
        dao.deletePlan(plan)
        if (wasActive) {
            // Find another plan to activate
            val all = dao.getAllPlans()
            // We can pick first available
        }
    }

    suspend fun syncSlotsForPlan(
        planId: Long,
        count: Int,
        totalCalories: Int,
        totalProtein: Int = 0,
        totalCarbs: Int = 0,
        totalFat: Int = 0
    ) {
        val existing = dao.getSlotsForPlanSync(planId)
        val defaultNames = listOf(
            "Colazione",
            "Spuntino",
            "Pranzo",
            "Merenda",
            "Cena",
            "Spuntino serale"
        )
        val propCal = if (count > 0) totalCalories / count else 0
        val propProt = if (count > 0) totalProtein / count else 0
        val propCarbs = if (count > 0) totalCarbs / count else 0
        val propFat = if (count > 0) totalFat / count else 0

        if (existing.size < count) {
            // Add missing slots
            for (i in existing.size until count) {
                val slotName = defaultNames.getOrElse(i) { "Pasto ${i + 1}" }
                dao.insertSlot(
                    MealSlotEntity(
                        planId = planId,
                        orderIndex = i + 1,
                        name = slotName,
                        customCalories = propCal,
                        customProtein = propProt,
                        customCarbs = propCarbs,
                        customFat = propFat
                    )
                )
            }
        } else if (existing.size > count) {
            // Remove excess slots
            for (i in count until existing.size) {
                dao.deleteSlot(existing[i])
            }
        }
    }

    suspend fun updateSlotName(slot: MealSlotEntity, newName: String) {
        dao.updateSlot(slot.copy(name = newName))
    }

    suspend fun updateSlotTargets(
        slot: MealSlotEntity,
        name: String,
        calories: Int,
        protein: Int,
        carbs: Int,
        fat: Int
    ) {
        dao.updateSlot(
            slot.copy(
                name = name,
                customCalories = calories,
                customProtein = protein,
                customCarbs = carbs,
                customFat = fat
            )
        )
    }

    suspend fun insertAlternative(alt: MealAlternativeEntity): Long {
        return dao.insertAlternative(alt)
    }

    suspend fun updateAlternative(alt: MealAlternativeEntity) {
        dao.updateAlternative(alt)
    }

    suspend fun deleteAlternative(alt: MealAlternativeEntity) {
        dao.deleteAlternative(alt)
    }

    suspend fun logMeal(
        name: String,
        calories: Int,
        protein: Int,
        carbs: Int,
        fat: Int,
        notes: String = "",
        photoUri: String? = null,
        ingredientsSummary: String = "",
        sourceAlternativeId: Long? = null,
        ingredientsJson: String = "[]"
    ) {
        val today = getTodayDateString()
        val timeNow = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        dao.insertLoggedMeal(
            LoggedMealEntity(
                date = today,
                time = timeNow,
                name = name,
                calories = calories,
                protein = protein,
                carbs = carbs,
                fat = fat,
                notes = notes,
                photoUri = photoUri,
                ingredientsSummary = ingredientsSummary,
                sourceAlternativeId = sourceAlternativeId,
                ingredientsJson = ingredientsJson
            )
        )
    }

    suspend fun updateLoggedMeal(meal: LoggedMealEntity) {
        dao.updateLoggedMeal(meal)
    }

    suspend fun deleteLoggedMeal(meal: LoggedMealEntity) {
        dao.deleteLoggedMeal(meal)
    }

    companion object {
        fun getTodayDateString(): String {
            return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        }
    }
}
