package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ai.GeminiNutritionService
import com.example.data.entity.Ingredient
import com.example.data.entity.LoggedMealEntity
import com.example.data.entity.MealSlotEntity
import com.example.ui.theme.ApexBlack
import com.example.ui.theme.ApexBorder
import com.example.ui.theme.ApexCalories
import com.example.ui.theme.ApexCarbs
import com.example.ui.theme.ApexDarkSurface
import com.example.ui.theme.ApexDarkSurfaceHighlight
import com.example.ui.theme.ApexFats
import com.example.ui.theme.ApexNeonLime
import com.example.ui.theme.ApexNeonLimeDim
import com.example.ui.theme.ApexProtein
import com.example.ui.theme.ApexTextMuted
import com.example.ui.theme.ApexTextPrimary
import com.example.ui.theme.ApexTextSecondary
import com.example.ui.theme.NutriCalories
import com.example.ui.theme.NutriCarbs
import com.example.ui.theme.NutriDark
import com.example.ui.theme.NutriFats
import com.example.ui.theme.NutriProtein
import com.example.ui.theme.NutriTextMuted
import com.example.ui.theme.NutriTextPrimary
import com.example.ui.theme.NutriTextSecondary
import com.example.ui.theme.nutriTextFieldColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoggedMealDetailSheet(
    meal: LoggedMealEntity,
    planSlots: List<MealSlotEntity>,
    geminiService: GeminiNutritionService? = null,
    foodDatabaseRepository: com.example.data.repository.FoodDatabaseRepository? = null,
    onDismiss: () -> Unit,
    onSave: (LoggedMealEntity) -> Unit,
    onCopyToPlanSlot: (LoggedMealEntity, MealSlotEntity) -> Unit,
    onDelete: (LoggedMealEntity) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val localContext = androidx.compose.ui.platform.LocalContext.current
    val actualFoodRepo = remember(foodDatabaseRepository) {
        foodDatabaseRepository ?: com.example.data.repository.FoodDatabaseRepository(
            localContext,
            com.example.data.db.AppDatabase.getDatabase(localContext).nutritionDao()
        )
    }

    var name by remember { mutableStateOf(meal.name) }
    var time by remember { mutableStateOf(meal.time) }
    var notes by remember { mutableStateOf(meal.notes) }
    var totalCalories by remember { mutableStateOf(meal.calories.toString()) }
    var totalProtein by remember { mutableStateOf(meal.protein.toString()) }
    var totalCarbs by remember { mutableStateOf(meal.carbs.toString()) }
    var totalFat by remember { mutableStateOf(meal.fat.toString()) }

    // Ingredient items
    val ingredients = remember {
        mutableStateListOf<EditableFoodItem>().apply {
            val existing = meal.ingredients
            if (existing.isNotEmpty()) {
                addAll(
                    existing.map {
                        EditableFoodItem(
                            name = it.name,
                            quantity = it.quantity,
                            calories = it.calories.toString(),
                            protein = it.protein.toString(),
                            carbs = it.carbs.toString(),
                            fat = it.fat.toString()
                        )
                    }
                )
            }
        }
    }

    // New food adding state
    var newFoodName by remember { mutableStateOf("") }
    var newFoodQuantity by remember { mutableStateOf("") }
    var isEstimatingFood by remember { mutableStateOf(false) }

    var showCopyDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    fun recalculateTotals() {
        val sumCal = ingredients.sumOf { it.calories.toIntOrNull() ?: 0 }
        val sumProt = ingredients.sumOf { it.protein.toIntOrNull() ?: 0 }
        val sumCarbs = ingredients.sumOf { it.carbs.toIntOrNull() ?: 0 }
        val sumFat = ingredients.sumOf { it.fat.toIntOrNull() ?: 0 }
        totalCalories = sumCal.toString()
        totalProtein = sumProt.toString()
        totalCarbs = sumCarbs.toString()
        totalFat = sumFat.toString()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ApexDarkSurface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(ApexBorder)
            )
        },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Dettaglio Pasto",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = ApexTextPrimary
                    )
                    Text(
                        text = "Giornata fuori • ${meal.date}",
                        fontSize = 12.5.sp,
                        color = ApexTextSecondary
                    )
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Chiudi",
                        tint = ApexTextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Actions: Copia nel Piano alimentare
            if (planSlots.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showCopyDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .testTag("copy_outdoor_meal_button"),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ApexBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ApexTextPrimary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = ApexNeonLime
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Copia nel Piano alimentare",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ApexTextPrimary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            HorizontalDivider(color = ApexBorder)
            Spacer(modifier = Modifier.height(16.dp))

            // Nome Pasto & Orario
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(modifier = Modifier.weight(1.3f)) {
                    Text(
                        text = "Nome del pasto",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ApexTextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        shape = RoundedCornerShape(10.dp),
                        colors = nutriTextFieldColors(),
                        textStyle = TextStyle(color = ApexTextPrimary, fontSize = 14.sp),
                        placeholder = { Text("es. Pranzo fuori", color = ApexTextMuted) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("logged_sheet_name_input")
                    )
                }

                Column(modifier = Modifier.weight(0.7f)) {
                    Text(
                        text = "Orario",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ApexTextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = time,
                        onValueChange = { time = it },
                        shape = RoundedCornerShape(10.dp),
                        colors = nutriTextFieldColors(),
                        textStyle = TextStyle(color = ApexTextPrimary, fontSize = 14.sp),
                        placeholder = { Text("12:30", color = ApexTextMuted) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("logged_sheet_time_input")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Note
            Text(
                text = "Note opzionali",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = ApexTextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                shape = RoundedCornerShape(10.dp),
                colors = nutriTextFieldColors(),
                textStyle = TextStyle(color = ApexTextPrimary, fontSize = 13.5.sp),
                placeholder = { Text("Note...", color = ApexTextMuted) },
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("logged_sheet_notes_input")
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Macro totali
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Valori nutrizionali totali",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = ApexTextPrimary
                )

                if (ingredients.isNotEmpty()) {
                    TextButton(
                        onClick = { recalculateTotals() },
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = ApexNeonLime
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Ricalcola da cibi",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ApexNeonLime
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MacroInputField(
                    label = "Calorie",
                    value = totalCalories,
                    onValueChange = { totalCalories = it },
                    unit = "kcal",
                    accentColor = ApexCalories,
                    modifier = Modifier.weight(1f),
                    tag = "logged_sheet_total_cal"
                )
                MacroInputField(
                    label = "Proteine",
                    value = totalProtein,
                    onValueChange = { totalProtein = it },
                    unit = "g",
                    accentColor = ApexProtein,
                    modifier = Modifier.weight(1f),
                    tag = "logged_sheet_total_prot"
                )
                MacroInputField(
                    label = "Carboidrati",
                    value = totalCarbs,
                    onValueChange = { totalCarbs = it },
                    unit = "g",
                    accentColor = ApexCarbs,
                    modifier = Modifier.weight(1f),
                    tag = "logged_sheet_total_carbs"
                )
                MacroInputField(
                    label = "Grassi",
                    value = totalFat,
                    onValueChange = { totalFat = it },
                    unit = "g",
                    accentColor = ApexFats,
                    modifier = Modifier.weight(1f),
                    tag = "logged_sheet_total_fat"
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = ApexBorder)
            Spacer(modifier = Modifier.height(16.dp))

            // --- SEZIONE: AGGIUNGI CIBO AL PASTO COMPLETO ---
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, ApexBorder, RoundedCornerShape(14.dp)),
                color = ApexDarkSurfaceHighlight
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(ApexBlack)
                                .border(1.dp, ApexBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restaurant,
                                contentDescription = null,
                                tint = ApexNeonLime,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = "Aggiungi altro cibo al pasto",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = ApexTextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    FoodSearchSection(
                        repository = actualFoodRepo,
                        onIngredientSelected = { ing ->
                            ingredients.add(
                                EditableFoodItem(
                                    name = ing.name,
                                    quantity = ing.quantity,
                                    calories = ing.calories.toString(),
                                    protein = ing.protein.toString(),
                                    carbs = ing.carbs.toString(),
                                    fat = ing.fat.toString()
                                )
                            )
                            recalculateTotals()
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Oppure inserimento manuale o stima AI:",
                        fontSize = 11.5.sp,
                        color = ApexTextSecondary,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newFoodName,
                            onValueChange = { newFoodName = it },
                            placeholder = { Text("Nome cibo (es. Frutta, Pane)", fontSize = 12.sp, color = ApexTextMuted) },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            colors = nutriTextFieldColors(),
                            textStyle = TextStyle(color = ApexTextPrimary, fontSize = 13.sp),
                            modifier = Modifier
                                .weight(1.3f)
                                .testTag("logged_add_food_name_input")
                        )

                        OutlinedTextField(
                            value = newFoodQuantity,
                            onValueChange = { newFoodQuantity = it },
                            placeholder = { Text("Quantità (es. 100g)", fontSize = 12.sp, color = ApexTextMuted) },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            colors = nutriTextFieldColors(),
                            textStyle = TextStyle(color = ApexTextPrimary, fontSize = 13.sp),
                            modifier = Modifier
                                .weight(0.9f)
                                .testTag("logged_add_food_qty_input")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Stima AI & Aggiungi
                        if (geminiService != null) {
                            Button(
                                onClick = {
                                    if (newFoodName.isNotBlank()) {
                                        coroutineScope.launch {
                                            isEstimatingFood = true
                                            val ing = geminiService.estimateIngredient(
                                                name = newFoodName.trim(),
                                                quantity = if (newFoodQuantity.isBlank()) "100g" else newFoodQuantity.trim()
                                            )
                                            ingredients.add(
                                                EditableFoodItem(
                                                    name = ing.name,
                                                    quantity = ing.quantity,
                                                    calories = ing.calories.toString(),
                                                    protein = ing.protein.toString(),
                                                    carbs = ing.carbs.toString(),
                                                    fat = ing.fat.toString()
                                                )
                                            )
                                            recalculateTotals()
                                            newFoodName = ""
                                            newFoodQuantity = ""
                                            isEstimatingFood = false
                                        }
                                    }
                                },
                                enabled = newFoodName.isNotBlank() && !isEstimatingFood,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .testTag("logged_estimate_and_add_food_btn"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ApexNeonLime,
                                    contentColor = ApexBlack
                                )
                            ) {
                                if (isEstimatingFood) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        color = ApexBlack,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Stima in corso...", fontSize = 11.5.sp, color = ApexBlack)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = ApexBlack,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Stima AI & Aggiungi", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = ApexBlack)
                                }
                            }
                        }

                        // Aggiungi Manuale / Veloce
                        OutlinedButton(
                            onClick = {
                                if (newFoodName.isNotBlank()) {
                                    ingredients.add(
                                        EditableFoodItem(
                                            name = newFoodName.trim(),
                                            quantity = if (newFoodQuantity.isBlank()) "1 porzione" else newFoodQuantity.trim(),
                                            calories = "0",
                                            protein = "0",
                                            carbs = "0",
                                            fat = "0"
                                        )
                                    )
                                    newFoodName = ""
                                    newFoodQuantity = ""
                                } else {
                                    ingredients.add(EditableFoodItem())
                                }
                            },
                            modifier = Modifier
                                .weight(0.9f)
                                .height(38.dp)
                                .testTag("logged_quick_add_food_btn"),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ApexBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ApexTextPrimary)
                        ) {
                            Text(
                                text = if (newFoodName.isNotBlank()) "+ Aggiungi" else "+ Riga vuota",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ApexTextPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Dati approfonditi dei singoli cibi / ingredienti
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Singoli Cibi nel Pasto (${ingredients.size})",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = ApexTextPrimary
                    )
                    Text(
                        text = "Valori nutrizionali dettagliati di ogni alimento",
                        fontSize = 11.5.sp,
                        color = ApexTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (ingredients.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ApexDarkSurfaceHighlight)
                        .border(1.dp, ApexBorder, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nessun alimento dettagliato. Usa il riquadro sopra per aggiungere cibi a questo pasto!",
                        fontSize = 12.5.sp,
                        color = ApexTextMuted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                ingredients.forEachIndexed { index, item ->
                    SingleFoodItemCard(
                        index = index,
                        item = item,
                        onUpdate = { updated ->
                            ingredients[index] = updated
                        },
                        onDelete = {
                            ingredients.removeAt(index)
                            recalculateTotals()
                        }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save and Delete action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { showDeleteConfirmDialog = true },
                    modifier = Modifier
                        .weight(0.35f)
                        .height(48.dp)
                        .testTag("delete_logged_meal_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7F1D1D)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Elimina",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Button(
                    onClick = {
                        val parsedIngredients = ingredients
                            .filter { it.name.isNotBlank() }
                            .map {
                                Ingredient(
                                    name = it.name.trim(),
                                    quantity = it.quantity.trim(),
                                    calories = it.calories.toIntOrNull() ?: 0,
                                    protein = it.protein.toIntOrNull() ?: 0,
                                    carbs = it.carbs.toIntOrNull() ?: 0,
                                    fat = it.fat.toIntOrNull() ?: 0
                                )
                            }

                        val summary = if (parsedIngredients.isNotEmpty()) {
                            parsedIngredients.joinToString(", ") { "${it.name} ${it.quantity}" }
                        } else meal.ingredientsSummary

                        val updated = meal.copy(
                            name = name.ifBlank { "Pasto" },
                            time = time.ifBlank { meal.time },
                            notes = notes,
                            ingredientsJson = Ingredient.listToJson(parsedIngredients),
                            ingredientsSummary = summary,
                            calories = totalCalories.toIntOrNull() ?: 0,
                            protein = totalProtein.toIntOrNull() ?: 0,
                            carbs = totalCarbs.toIntOrNull() ?: 0,
                            fat = totalFat.toIntOrNull() ?: 0
                        )
                        onSave(updated)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("save_logged_meal_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ApexNeonLime,
                        contentColor = ApexBlack
                    )
                ) {
                    Text(
                        text = "Salva modifiche",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = ApexBlack
                    )
                }
            }
        }
    }

    // Dialog to choose target meal slot for copying into active plan
    if (showCopyDialog) {
        CopyAlternativeDialog(
            alternativeName = name.ifBlank { meal.name },
            currentSlotName = "Giornata fuori",
            availableSlots = planSlots,
            onDismiss = { showCopyDialog = false },
            onSelectSlot = { targetSlot ->
                val parsedIngredients = ingredients
                    .filter { it.name.isNotBlank() }
                    .map {
                        Ingredient(
                            name = it.name.trim(),
                            quantity = it.quantity.trim(),
                            calories = it.calories.toIntOrNull() ?: 0,
                            protein = it.protein.toIntOrNull() ?: 0,
                            carbs = it.carbs.toIntOrNull() ?: 0,
                            fat = it.fat.toIntOrNull() ?: 0
                        )
                    }

                val summary = if (parsedIngredients.isNotEmpty()) {
                    parsedIngredients.joinToString(", ") { "${it.name} ${it.quantity}" }
                } else meal.ingredientsSummary

                val updated = meal.copy(
                    name = name.ifBlank { "Pasto" },
                    time = time.ifBlank { meal.time },
                    notes = notes,
                    ingredientsJson = Ingredient.listToJson(parsedIngredients),
                    ingredientsSummary = summary,
                    calories = totalCalories.toIntOrNull() ?: 0,
                    protein = totalProtein.toIntOrNull() ?: 0,
                    carbs = totalCarbs.toIntOrNull() ?: 0,
                    fat = totalFat.toIntOrNull() ?: 0
                )
                onCopyToPlanSlot(updated, targetSlot)
                showCopyDialog = false
                onDismiss()
            }
        )
    }

    // Confirmation dialog for deletion
    if (showDeleteConfirmDialog) {
        Dialog(onDismissRequest = { showDeleteConfirmDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp)),
                color = ApexDarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, ApexBorder)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Elimina pasto",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ApexTextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Sei sicuro di voler eliminare questo pasto registrato?",
                        fontSize = 13.5.sp,
                        color = ApexTextSecondary
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showDeleteConfirmDialog = false }) {
                            Text("Annulla", color = ApexTextMuted)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                showDeleteConfirmDialog = false
                                onDelete(meal)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                        ) {
                            Text("Elimina", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
