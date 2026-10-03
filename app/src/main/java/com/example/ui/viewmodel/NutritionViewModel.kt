package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.DishEstimateResult
import com.example.ai.GeminiNutritionService
import com.example.ai.LabelScanResult
import com.example.data.db.AppDatabase
import com.example.data.entity.ChatMessage
import com.example.data.entity.GeneratedMealProposal
import com.example.data.entity.Ingredient
import com.example.data.entity.LoggedMealEntity
import com.example.data.entity.MealAlternativeEntity
import com.example.data.entity.MealSlotEntity
import com.example.data.entity.PlanEntity
import com.example.data.entity.ShoppingItemEntity
import com.example.data.repository.NutritionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
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
    val foodDatabaseRepository = com.example.data.repository.FoodDatabaseRepository(application, db.nutritionDao())

    private val prefs = application.getSharedPreferences("apex_nutrition_prefs", android.content.Context.MODE_PRIVATE)
    private val _hasCompletedOnboarding = MutableStateFlow(
        prefs.getBoolean("has_completed_onboarding", false)
    )
    val hasCompletedOnboarding: StateFlow<Boolean> = _hasCompletedOnboarding.asStateFlow()

    fun completeOnboarding() {
        prefs.edit().putBoolean("has_completed_onboarding", true).apply()
        _hasCompletedOnboarding.value = true
    }

    fun resetOnboarding() {
        prefs.edit().putBoolean("has_completed_onboarding", false).apply()
        _hasCompletedOnboarding.value = false
    }

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

    // --- Shopping List (Spesa) State ---
    private val _selectedShoppingPlanId = MutableStateFlow<Long?>(null)
    val selectedShoppingPlanId: StateFlow<Long?> = _selectedShoppingPlanId.asStateFlow()

    fun selectShoppingPlan(planId: Long) {
        _selectedShoppingPlanId.value = planId
    }

    val currentShoppingPlan: StateFlow<PlanEntity?> = combine(
        allPlans,
        activePlan,
        _selectedShoppingPlanId
    ) { plans, active, selectedId ->
        if (selectedId != null) {
            plans.find { it.id == selectedId } ?: active ?: plans.firstOrNull()
        } else {
            active ?: plans.firstOrNull()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val shoppingItems: StateFlow<List<ShoppingItemEntity>> = currentShoppingPlan.flatMapLatest { plan ->
        if (plan != null) {
            repository.getShoppingItemsForPlan(plan.id)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isGeneratingShoppingList = MutableStateFlow(false)
    val isGeneratingShoppingList: StateFlow<Boolean> = _isGeneratingShoppingList.asStateFlow()

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
        photoUri: String? = null,
        calories: Int = 0,
        protein: Int = 0,
        carbs: Int = 0,
        fat: Int = 0
    ) {
        viewModelScope.launch {
            val ingCal = ingredients.sumOf { it.calories }
            val ingProt = ingredients.sumOf { it.protein }
            val ingCarbs = ingredients.sumOf { it.carbs }
            val ingFat = ingredients.sumOf { it.fat }

            val totalCal = if (ingCal > 0) ingCal else calories
            val totalProt = if (ingProt > 0 || ingCal > 0) ingProt else protein
            val totalCarbs = if (ingCarbs > 0 || ingCal > 0) ingCarbs else carbs
            val totalFat = if (ingFat > 0 || ingCal > 0) ingFat else fat

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

    fun clearTodayMeals() {
        viewModelScope.launch {
            val list = todayLoggedMeals.value
            list.forEach { repository.deleteLoggedMeal(it) }
        }
    }

    // --- Shopping List (Spesa) Actions ---

    fun toggleShoppingItem(item: ShoppingItemEntity) {
        viewModelScope.launch {
            repository.updateShoppingItem(item.copy(isChecked = !item.isChecked))
        }
    }

    fun updateShoppingItemQuantity(item: ShoppingItemEntity, newQuantity: String) {
        viewModelScope.launch {
            repository.updateShoppingItem(item.copy(quantity = newQuantity.trim()))
        }
    }

    fun updateShoppingItemPackageCount(item: ShoppingItemEntity, newCount: Int) {
        viewModelScope.launch {
            repository.updateShoppingItem(item.copy(packageCount = newCount.coerceAtLeast(1)))
        }
    }

    fun updateShoppingItemDetails(
        item: ShoppingItemEntity,
        newName: String,
        newQuantity: String,
        newPackageCount: Int,
        newPackageGrammage: String,
        newNotes: String
    ) {
        viewModelScope.launch {
            repository.updateShoppingItem(
                item.copy(
                    name = newName.trim(),
                    quantity = newQuantity.trim(),
                    packageCount = newPackageCount.coerceAtLeast(1),
                    packageGrammage = newPackageGrammage.trim(),
                    notes = newNotes.trim()
                )
            )
        }
    }

    fun addCustomShoppingItem(
        planId: Long,
        name: String,
        quantity: String,
        packageCount: Int = 1,
        packageGrammage: String = "",
        category: String
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.insertShoppingItem(
                ShoppingItemEntity(
                    planId = planId,
                    name = name.trim(),
                    quantity = quantity.trim(),
                    packageCount = packageCount.coerceAtLeast(1),
                    packageGrammage = packageGrammage.trim(),
                    category = category.ifBlank { "Altro" },
                    isChecked = false,
                    isCustom = true
                )
            )
        }
    }

    fun deleteShoppingItem(item: ShoppingItemEntity) {
        viewModelScope.launch {
            repository.deleteShoppingItem(item)
        }
    }

    fun clearCheckedShoppingItems(planId: Long) {
        viewModelScope.launch {
            repository.deleteCheckedShoppingItems(planId)
        }
    }

    fun setAllShoppingItemsChecked(planId: Long, checked: Boolean) {
        viewModelScope.launch {
            repository.setAllShoppingItemsChecked(planId, checked)
        }
    }

    fun generateOrRefreshShoppingList(
        planId: Long? = null,
        onComplete: ((String) -> Unit)? = null
    ) {
        val targetPlanId = planId ?: currentShoppingPlan.value?.id ?: return
        viewModelScope.launch {
            _isGeneratingShoppingList.value = true
            try {
                val targetPlan = allPlans.value.find { it.id == targetPlanId }
                    ?: activePlan.value
                val planName = targetPlan?.name ?: "Piano"

                val alternatives = repository.getAlternativesForPlan(targetPlanId).firstOrNull() ?: emptyList()
                val foodStrings = mutableListOf<String>()

                for (alt in alternatives) {
                    if (alt.ingredients.isNotEmpty()) {
                        for (ing in alt.ingredients) {
                            val str = "${ing.name} ${ing.quantity}".trim()
                            if (str.isNotBlank()) foodStrings.add(str)
                        }
                    } else if (alt.name.isNotBlank()) {
                        foodStrings.add(alt.name.trim())
                    }
                }

                if (foodStrings.isEmpty()) {
                    onComplete?.invoke("Nessun alimento o pasto trovato nel piano '$planName'. Aggiungi prima dei pasti nel Piano!")
                    return@launch
                }

                // Call AI to categorize and consolidate
                val aiResults = geminiService.generateShoppingListFromPlan(planName, foodStrings)

                // Preserve checked status of previously checked items and user custom items
                val existing = repository.getShoppingItemsForPlanSync(targetPlanId)
                val checkedNames = existing.filter { it.isChecked }.map { it.name.lowercase().trim() }.toSet()
                val customItems = existing.filter { it.isCustom }

                // Clear current items for this plan and insert fresh organized list
                repository.clearShoppingItemsForPlan(targetPlanId)

                val entities = aiResults.map { item ->
                    val wasChecked = checkedNames.contains(item.name.lowercase().trim())
                    ShoppingItemEntity(
                        planId = targetPlanId,
                        name = item.name,
                        quantity = item.quantity,
                        packageCount = item.packageCount.coerceAtLeast(1),
                        packageGrammage = item.packageGrammage,
                        category = item.category,
                        isChecked = wasChecked,
                        isCustom = false,
                        notes = item.notes
                    )
                }

                repository.insertShoppingItems(entities + customItems)
                onComplete?.invoke("Lista della spesa generata con successo dall'AI! (${entities.size} alimenti) ✓")
            } catch (e: Exception) {
                Log.e("NutritionViewModel", "Error in generateOrRefreshShoppingList: ${e.message}", e)
                onComplete?.invoke("Errore durante la generazione della spesa: ${e.message}")
            } finally {
                _isGeneratingShoppingList.value = false
            }
        }
    }

    // --- Gemini Pantry Chef Chat State & Actions ---
    private val initialWelcomeMessage = ChatMessage(
        isUser = false,
        text = "Ciao! Sono il tuo assistente nutrizionista AI.\n\nDimmi cosa c'è nella tua dispensa o nel frigorifero e per quale pasto vorresti la ricetta (es. 'Ho uova, zucchine e pane, cosa preparo per pranzo?').\n\nCreerò per te il pasto perfetto calibrato al millimetro sui macronutrienti del tuo piano attivo, che potrai aggiungere al Piano o registrare per oggi in Giornata Fuori!"
    )

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(listOf(initialWelcomeMessage))
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    val quickPantryItems: List<String> = listOf(
        "Uova", "Albumi", "Pollo", "Riso basmati", "Pasta", "Tonno",
        "Zucchine", "Pomodorini", "Olio EVO", "Avena", "Yogurt greco",
        "Pane integrale", "Mela", "Noci", "Salmone", "Bresaola"
    )

    fun sendChatMessage(text: String) {
        val clean = text.trim()
        if (clean.isBlank()) return

        val userMsg = ChatMessage(isUser = true, text = clean)
        _chatMessages.value = _chatMessages.value + userMsg
        _isChatLoading.value = true

        viewModelScope.launch {
            try {
                val currentPlan = activePlan.value
                val slots = activePlanSlots.value

                val planContextBuilder = StringBuilder()
                if (currentPlan != null) {
                    planContextBuilder.append("PIANO ATTIVO: \"${currentPlan.name}\"\n")
                    planContextBuilder.append("TARGET GIORNALIERO: ${currentPlan.caloriesTarget} kcal | ${currentPlan.proteinTarget}g Proteine | ${currentPlan.carbsTarget}g Carboidrati | ${currentPlan.fatTarget}g Grassi\n")
                    if (slots.isNotEmpty()) {
                        planContextBuilder.append("PASTI PREVISTI NEL PIANO:\n")
                        val mealsCount = currentPlan.mealsCount.coerceAtLeast(1)
                        slots.forEach { s ->
                            val sCal = s.customCalories ?: (currentPlan.caloriesTarget / mealsCount)
                            val sProt = s.customProtein ?: (currentPlan.proteinTarget / mealsCount)
                            val sCarb = s.customCarbs ?: (currentPlan.carbsTarget / mealsCount)
                            val sFat = s.customFat ?: (currentPlan.fatTarget / mealsCount)
                            planContextBuilder.append("- Slot ${s.orderIndex}: \"${s.name}\" -> Target: $sCal kcal (Prot: ${sProt}g, Carb: ${sCarb}g, Gras: ${sFat}g)\n")
                        }
                    }
                } else {
                    planContextBuilder.append("Nessun piano attivo al momento. Fai riferimento a un fabbisogno standard bilanciato (circa 500-700 kcal a pasto).\n")
                }

                val response = geminiService.sendPantryChefMessage(
                    history = _chatMessages.value,
                    userMessage = clean,
                    activePlanContext = planContextBuilder.toString(),
                    pantryItems = emptyList()
                )

                val assistantMsg = ChatMessage(
                    isUser = false,
                    text = response.replyText,
                    mealProposal = response.mealProposal
                )
                _chatMessages.value = _chatMessages.value + assistantMsg
            } catch (e: Exception) {
                Log.e("NutritionViewModel", "Chat error: ${e.message}", e)
                _chatMessages.value = _chatMessages.value + ChatMessage(
                    isUser = false,
                    text = "Mi dispiace, si è verificato un errore durante la generazione della ricetta: ${e.message}"
                )
            } finally {
                _isChatLoading.value = false
            }
        }
    }

    fun clearChat() {
        _chatMessages.value = listOf(initialWelcomeMessage)
    }

    fun copyGeneratedMealToSlot(
        proposal: GeneratedMealProposal,
        targetSlotId: Long,
        planId: Long
    ) {
        viewModelScope.launch {
            val ingJson = Ingredient.listToJson(proposal.ingredients)
            repository.insertAlternative(
                MealAlternativeEntity(
                    slotId = targetSlotId,
                    planId = planId,
                    name = proposal.name.ifBlank { "Alternativa dalla Dispensa" },
                    ingredientsJson = ingJson,
                    totalCalories = proposal.calories,
                    totalProtein = proposal.protein,
                    totalCarbs = proposal.carbs,
                    totalFat = proposal.fat,
                    notes = proposal.notes
                )
            )
        }
    }

    fun logGeneratedMealToToday(proposal: GeneratedMealProposal) {
        viewModelScope.launch {
            val ingSummary = proposal.ingredients.joinToString(", ") { "${it.name} ${it.quantity}" }
            val ingJson = Ingredient.listToJson(proposal.ingredients)
            repository.logMeal(
                name = proposal.name.ifBlank { "Pasto dalla Dispensa" },
                calories = proposal.calories,
                protein = proposal.protein,
                carbs = proposal.carbs,
                fat = proposal.fat,
                notes = proposal.notes,
                ingredientsSummary = ingSummary,
                ingredientsJson = ingJson
            )
        }
    }

    fun addGeneratedMealToShoppingList(proposal: GeneratedMealProposal, planId: Long) {
        viewModelScope.launch {
            val items = proposal.ingredients.map { ing ->
                ShoppingItemEntity(
                    planId = planId,
                    name = ing.name,
                    quantity = ing.quantity,
                    category = "Da Dispensa",
                    isCustom = true
                )
            }
            if (items.isNotEmpty()) {
                repository.insertShoppingItems(items)
            }
        }
    }
}
