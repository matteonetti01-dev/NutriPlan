package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.entity.LoggedMealEntity
import com.example.data.entity.MealAlternativeEntity
import com.example.data.entity.MealSlotEntity
import com.example.data.entity.PlanEntity
import com.example.data.entity.ShoppingItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NutritionDao {

    // --- Plans ---
    @Query("SELECT * FROM plans ORDER BY createdAt ASC")
    fun getAllPlans(): Flow<List<PlanEntity>>

    @Query("SELECT * FROM plans WHERE isActive = 1 LIMIT 1")
    fun getActivePlan(): Flow<PlanEntity?>

    @Query("SELECT * FROM plans WHERE isActive = 1 LIMIT 1")
    suspend fun getActivePlanSync(): PlanEntity?

    @Query("SELECT * FROM plans WHERE id = :id LIMIT 1")
    suspend fun getPlanById(id: Long): PlanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: PlanEntity): Long

    @Update
    suspend fun updatePlan(plan: PlanEntity)

    @Delete
    suspend fun deletePlan(plan: PlanEntity)

    @Query("UPDATE plans SET isActive = 0")
    suspend fun clearActivePlans()

    @Query("UPDATE plans SET isActive = 1 WHERE id = :planId")
    suspend fun setPlanActive(planId: Long)

    @Transaction
    suspend fun switchActivePlan(planId: Long) {
        clearActivePlans()
        setPlanActive(planId)
    }

    // --- Meal Slots ---
    @Query("SELECT * FROM meal_slots WHERE planId = :planId ORDER BY orderIndex ASC")
    fun getSlotsForPlan(planId: Long): Flow<List<MealSlotEntity>>

    @Query("SELECT * FROM meal_slots WHERE planId = :planId ORDER BY orderIndex ASC")
    suspend fun getSlotsForPlanSync(planId: Long): List<MealSlotEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlot(slot: MealSlotEntity): Long

    @Update
    suspend fun updateSlot(slot: MealSlotEntity)

    @Delete
    suspend fun deleteSlot(slot: MealSlotEntity)

    @Query("DELETE FROM meal_slots WHERE planId = :planId")
    suspend fun deleteSlotsForPlan(planId: Long)

    // --- Meal Alternatives ---
    @Query("SELECT * FROM meal_alternatives WHERE slotId = :slotId ORDER BY id ASC")
    fun getAlternativesForSlot(slotId: Long): Flow<List<MealAlternativeEntity>>

    @Query("SELECT * FROM meal_alternatives WHERE planId = :planId ORDER BY id ASC")
    fun getAlternativesForPlan(planId: Long): Flow<List<MealAlternativeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlternative(alt: MealAlternativeEntity): Long

    @Update
    suspend fun updateAlternative(alt: MealAlternativeEntity)

    @Delete
    suspend fun deleteAlternative(alt: MealAlternativeEntity)

    @Query("DELETE FROM meal_alternatives WHERE id = :id")
    suspend fun deleteAlternativeById(id: Long)

    // --- Logged Meals (Giornata fuori / Daily Log) ---
    @Query("SELECT * FROM logged_meals WHERE date = :date ORDER BY id DESC")
    fun getLoggedMealsForDate(date: String): Flow<List<LoggedMealEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoggedMeal(meal: LoggedMealEntity): Long

    @Update
    suspend fun updateLoggedMeal(meal: LoggedMealEntity)

    @Delete
    suspend fun deleteLoggedMeal(meal: LoggedMealEntity)

    @Query("DELETE FROM logged_meals WHERE id = :id")
    suspend fun deleteLoggedMealById(id: Long)

    // --- Shopping List (Spesa) ---
    @Query("SELECT * FROM shopping_items WHERE planId = :planId ORDER BY isChecked ASC, category ASC, id ASC")
    fun getShoppingItemsForPlan(planId: Long): Flow<List<ShoppingItemEntity>>

    @Query("SELECT * FROM shopping_items WHERE planId = :planId")
    suspend fun getShoppingItemsForPlanSync(planId: Long): List<ShoppingItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingItem(item: ShoppingItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingItems(items: List<ShoppingItemEntity>)

    @Update
    suspend fun updateShoppingItem(item: ShoppingItemEntity)

    @Delete
    suspend fun deleteShoppingItem(item: ShoppingItemEntity)

    @Query("DELETE FROM shopping_items WHERE planId = :planId")
    suspend fun clearShoppingItemsForPlan(planId: Long)

    @Query("UPDATE shopping_items SET isChecked = :checked WHERE planId = :planId")
    suspend fun setAllShoppingItemsChecked(planId: Long, checked: Boolean)

    @Query("DELETE FROM shopping_items WHERE planId = :planId AND isChecked = 1")
    suspend fun deleteCheckedShoppingItems(planId: Long)
}
