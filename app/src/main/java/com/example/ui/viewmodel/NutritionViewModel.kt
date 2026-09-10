package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.DishEstimateResult
import com.example.ai.GeminiNutritionService
import com.example.ai.LabelScanResult
import com.example.data.db.AppDatabase
import com.example.data.entity.Ingredient
import com.example.data.entity.LoggedMealEntity
import com.example.data.entity.MealAlternativeEntity
import com.example.data.entity.MealSlotEntity
import com.example.data.entity.PlanEntity
import com.example.data.repository.NutritionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class NutritionViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = NutritionRepository(db.nutritionDao())
    val geminiService = GeminiNutritionService(application)

    // Current selected tab: 0=Dashboard, 1=Piano, 2=Giornata fuori, 3=Impostazioni
    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    fun selectTab(tab: Int) {
        _currentTab.value = tab
    }

    val allPlans: StateFlow<List<PlanEntity>> = repository.allPlans.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val activePlan: StateFlow<PlanEntity?> = repository.activePlan.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val savedPlans: StateFlow<List<PlanEntity>> = allPlans.map { plans ->
        plans.filter { !it.isActive }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val activePlanSlots: StateFlow<List<MealSlotEntity>> = activePlan.flatMapLatest { plan ->
        if (plan != null) repository.getSlotsForPlan(plan.id) else flowOf(emptyList())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val activePlanAlternatives: StateFlow<List<MealAlternativeEntity>> = activePlan.flatMapLatest { plan ->
        if (plan != null) repository.getAlternativesForPlan(plan.id) else flowOf(emptyList())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val todayLoggedMeals: StateFlow<List<LoggedMealEntity>> = repository.getLoggedMealsForDate(
        NutritionRepository.getTodayDateString()
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Logged totals today
    val todayCalories: StateFlow<Int> = todayLoggedMeals.map { list ->
        list.sumOf { it.calories }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val todayProtein: StateFlow<Int> = todayLoggedMeals.map { list ->
        list.sumOf { it.protein }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val todayCarbs: StateFlow<Int> = todayLoggedMeals.map { list ->
        list.sumOf { it.carbs }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val todayFat: StateFlow<Int> = todayLoggedMeals.map { list ->
        list.sumOf { it.fat }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        viewModelScope.launch {
            repository.checkAndSeedInitialData()
        }
    }

    fun switchActivePlan(planId: Long) {
        viewModelScope.launch {
            repository.switchActivePlan(planId)
        }
    }

    fun createPlan(
        name: String,
        calories: Int,
        protein: Int,
        carbs: Int,
        fat: Int,
        mealsCount: Int,
        makeActive: Boolean = (activePlan.value == null)
    ) {
        viewModelScope.launch {
            repository.createPlan(name, calories, protein, carbs, fat, mealsCount, makeActive)
        }
    }

    fun updatePlan(plan: PlanEntity) {
        viewModelScope.launch {
            repository.updatePlan(plan)
        }
    }

    fun updateActivePlanTargets(
        calories: Int,
        protein: Int,
        carbs: Int,
        fat: Int,
        mealsCount: Int
    ) {
        val current = activePlan.value ?: return
        val updated = current.copy(
            caloriesTarget = calories,
            proteinTarget = protein,
            carbsTarget = carbs,
            fatTarget = fat,
            mealsCount = mealsCount
        )
        viewModelScope.launch {
            repository.updatePlan(updated)
        }
    }

    fun deletePlan(plan: PlanEntity) {
        viewModelScope.launch {
            repository.deletePlan(plan)
            // If active was deleted, pick another if available
            val remaining = allPlans.value.filter { it.id != plan.id }
            if (remaining.isNotEmpty()) {
                repository.switchActivePlan(remaining.first().id)
            }
        }
    }

    fun updateSlotName(slot: MealSlotEntity, newName: String) {
        viewModelScope.launch {
            repository.updateSlotName(slot, newName)
        }
    }

    fun updateSlotTargets(
        slot: MealSlotEntity,
        name: String,
        calories: Int,
        protein: Int,
        carbs: Int,
        fat: Int
    ) {
        viewModelScope.launch {
            repository.updateSlotTargets(slot, name, calories, protein, carbs, fat)
        }
    }

    fun addAlternative(
        slotId: Long,
        planId: Long,
        name: String,
        ingredients: List<Ingredient>,
        notes: String = "",
        photoUri: String? = null
    ) {
        viewModelScope.launch {
            val totalCal = ingredients.sumOf { it.calories }
            val totalProt = ingredients.sumOf { it.protein }
            val totalCarbs = ingredients.sumOf { it.carbs }
            val totalFat = ingredients.sumOf { it.fat }
            repository.insertAlternative(
                MealAlternativeEntity(
                    slotId = slotId,
                    planId = planId,
                    name = name.ifBlank { "Alternativa" },
                    ingredientsJson = Ingredient.listToJson(ingredients),
                    totalCalories = totalCal,
                    totalProtein = totalProt,
                    totalCarbs = totalCarbs,
                    totalFat = totalFat,
                    notes = notes,
                    photoUri = photoUri
                )
            )
        }
    }

    fun addDirectAlternative(
        slotId: Long,
        planId: Long,
        name: String,
        calories: Int,
        protein: Int,
        carbs: Int,
        fat: Int,
        notes: String = "",
        photoUri: String? = null,
        ingredients: List<Ingredient> = emptyList()
    ) {
        viewModelScope.launch {
            repository.insertAlternative(
                MealAlternativeEntity(
                    slotId = slotId,
                    planId = planId,
                    name = name.ifBlank { "Alternativa" },
                    ingredientsJson = Ingredient.listToJson(ingredients),
                    totalCalories = calories,
                    totalProtein = protein,
                    totalCarbs = carbs,
                    totalFat = fat,
                    notes = notes,
                    photoUri = photoUri
                )
            )
        }
    }

    fun updateAlternative(alt: MealAlternativeEntity) {
        viewModelScope.launch {
            repository.updateAlternative(alt)
        }
    }

    fun copyAlternativeToSlot(alt: MealAlternativeEntity, targetSlotId: Long) {
        viewModelScope.launch {
            repository.insertAlternative(alt.copy(id = 0, slotId = targetSlotId))
        }
    }

    fun deleteAlternative(alt: MealAlternativeEntity) {
        viewModelScope.launch {
            repository.deleteAlternative(alt)
        }
    }

    fun logAlternativeToToday(alt: MealAlternativeEntity, slotName: String) {
        viewModelScope.launch {
            val ingSummary = alt.ingredients.joinToString(", ") { "${it.name} ${it.quantity}" }
            repository.logMeal(
                name = if (alt.name.isNotBlank()) alt.name else slotName,
                calories = alt.totalCalories,
                protein = alt.totalProtein,
                carbs = alt.totalCarbs,
                fat = alt.totalFat,
                notes = alt.notes,
                photoUri = alt.photoUri,
                ingredientsSummary = ingSummary,
                sourceAlternativeId = alt.id,
                ingredientsJson = alt.ingredientsJson
            )
        }
    }

    fun logDirectMeal(
        name: String,
        calories: Int,
        protein: Int,
        carbs: Int,
        fat: Int,
        notes: String = "",
        photoUri: String? = null,
        ingredientsSummary: String = "",
        ingredientsJson: String = "[]"
    ) {
        viewModelScope.launch {
            repository.logMeal(
                name = name,
                calories = calories,
                protein = protein,
                carbs = carbs,
                fat = fat,
                notes = notes,
                photoUri = photoUri,
                ingredientsSummary = ingredientsSummary,
                ingredientsJson = ingredientsJson
            )
        }
    }

    fun updateLoggedMeal(meal: LoggedMealEntity) {
        viewModelScope.launch {
            repository.updateLoggedMeal(meal)
        }
    }

    fun copyLoggedMealToPlanSlot(meal: LoggedMealEntity, targetSlotId: Long, planId: Long) {
        viewModelScope.launch {
            repository.insertAlternative(
                MealAlternativeEntity(
                    slotId = targetSlotId,
                    planId = planId,
                    name = meal.name.ifBlank { "Alternativa da Giornata Fuori" },
                    ingredientsJson = meal.ingredientsJson,
                    totalCalories = meal.calories,
                    totalProtein = meal.protein,
                    totalCarbs = meal.carbs,
                    totalFat = meal.fat,
                    notes = meal.notes,
                    photoUri = meal.photoUri
                )
            )
        }
    }

    fun deleteLoggedMeal(meal: LoggedMealEntity) {
        viewModelScope.launch {
            repository.deleteLoggedMeal(meal)
        }
    }
}
