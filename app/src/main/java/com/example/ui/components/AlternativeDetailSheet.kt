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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
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
import android.util.Log
import androidx.compose.ui.graphics.Brush
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
import com.example.data.entity.MealAlternativeEntity
import com.example.data.entity.MealSlotEntity
import com.example.data.entity.PlanEntity
import com.example.ui.theme.NutriCalories
import com.example.ui.theme.NutriCarbs
import com.example.ui.theme.NutriDark
import com.example.ui.theme.NutriFats
import com.example.ui.theme.NutriGreen
import com.example.ui.theme.NutriProtein
import com.example.ui.theme.NutriTextMuted
import com.example.ui.theme.NutriTextPrimary
import com.example.ui.theme.NutriTextSecondary
import com.example.ui.theme.nutriTextFieldColors
import kotlinx.coroutines.launch

data class EditableFoodItem(
    var name: String = "",
    var quantity: String = "",
    var calories: String = "0",
    var protein: String = "0",
    var carbs: String = "0",
    var fat: String = "0"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlternativeDetailSheet(
    alternative: MealAlternativeEntity,
    currentSlot: MealSlotEntity?,
    allSlots: List<MealSlotEntity>,
    geminiService: GeminiNutritionService? = null,
    activePlan: PlanEntity? = null,
    onDismiss: () -> Unit,
    onSave: (MealAlternativeEntity) -> Unit,
    onCopyToSlot: (MealAlternativeEntity, MealSlotEntity) -> Unit,
    onDelete: (MealAlternativeEntity) -> Unit,
    onLog: (MealAlternativeEntity) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    var name by remember { mutableStateOf(alternative.name) }
    var notes by remember { mutableStateOf(alternative.notes) }
    var totalCalories by remember { mutableStateOf(alternative.totalCalories.toString()) }
    var totalProtein by remember { mutableStateOf(alternative.totalProtein.toString()) }
    var totalCarbs by remember { mutableStateOf(alternative.totalCarbs.toString()) }
    var totalFat by remember { mutableStateOf(alternative.totalFat.toString()) }

    // Ingredient items
    val ingredients = remember {
        mutableStateListOf<EditableFoodItem>().apply {
            val existing = alternative.ingredients
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

    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
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
                        text = "Dettaglio Alternativa",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = NutriTextPrimary
                    )
                    currentSlot?.let {
                        Text(
                            text = "Nel pasto: ${it.name} (Pasto ${it.orderIndex})",
                            fontSize = 12.5.sp,
                            color = NutriTextSecondary
                        )
                    }
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Chiudi",
                        tint = NutriTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Actions: Copia in altro pasto & Mangia oggi
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showCopyDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .testTag("copy_alternative_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NutriDark)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = NutriDark
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Copia in altro pasto",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedButton(
                    onClick = {
                        onLog(alternative)
                        onDismiss()
                    },
                    modifier = Modifier
                        .height(40.dp)
                        .testTag("log_from_sheet_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NutriGreen)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = NutriGreen
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Mangia oggi",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NutriGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFF3F4F6))
            Spacer(modifier = Modifier.height(16.dp))

            // Nome Alternativa
            Text(
                text = "Nome dell'alternativa / piatto",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = NutriTextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                shape = RoundedCornerShape(10.dp),
                colors = nutriTextFieldColors(),
                textStyle = TextStyle(color = NutriTextPrimary, fontSize = 14.sp),
                placeholder = { Text("es. Toast integrale con avocado e uovo") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sheet_alt_name_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Note
            Text(
                text = "Note o preparazione",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = NutriTextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                shape = RoundedCornerShape(10.dp),
                colors = nutriTextFieldColors(),
                textStyle = TextStyle(color = NutriTextPrimary, fontSize = 13.5.sp),
                placeholder = { Text("Note opzionali...") },
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sheet_alt_notes_input")
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
                    color = NutriTextPrimary
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
                            tint = NutriDark
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Ricalcola da cibi",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NutriDark
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
                    accentColor = NutriCalories,
                    modifier = Modifier.weight(1f),
                    tag = "sheet_total_cal"
                )
                MacroInputField(
                    label = "Proteine",
                    value = totalProtein,
                    onValueChange = { totalProtein = it },
                    unit = "g",
                    accentColor = NutriProtein,
                    modifier = Modifier.weight(1f),
                    tag = "sheet_total_prot"
                )
                MacroInputField(
                    label = "Carboidrati",
                    value = totalCarbs,
                    onValueChange = { totalCarbs = it },
                    unit = "g",
                    accentColor = NutriCarbs,
                    modifier = Modifier.weight(1f),
                    tag = "sheet_total_carbs"
                )
                MacroInputField(
                    label = "Grassi",
                    value = totalFat,
                    onValueChange = { totalFat = it },
                    unit = "g",
                    accentColor = NutriFats,
                    modifier = Modifier.weight(1f),
                    tag = "sheet_total_fat"
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = Color(0xFFF3F4F6))
            Spacer(modifier = Modifier.height(16.dp))

            // --- SEZIONE: AGGIUNGI CIBO AL PASTO ---
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp)),
                color = Color(0xFFF8FAFC)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE2E8F0)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restaurant,
                                contentDescription = null,
                                tint = NutriDark,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Text(
                            text = "Aggiungi altro cibo al pasto",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = NutriTextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newFoodName,
                            onValueChange = { newFoodName = it },
                            placeholder = { Text("Nome cibo (es. Mela, Pane)", fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            colors = nutriTextFieldColors(),
                            textStyle = TextStyle(color = NutriTextPrimary, fontSize = 13.sp),
                            modifier = Modifier
                                .weight(1.3f)
                                .testTag("add_food_name_input")
                        )

                        OutlinedTextField(
                            value = newFoodQuantity,
                            onValueChange = { newFoodQuantity = it },
                            placeholder = { Text("Quantità (es. 100g)", fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            colors = nutriTextFieldColors(),
                            textStyle = TextStyle(color = NutriTextPrimary, fontSize = 13.sp),
                            modifier = Modifier
                                .weight(0.9f)
                                .testTag("add_food_qty_input")
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
                                    .testTag("estimate_and_add_food_btn"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NutriDark,
                                    contentColor = Color.White
                                )
                            ) {
                                if (isEstimatingFood) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Stima in corso...", fontSize = 11.5.sp)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Stima AI & Aggiungi", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
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
                                .testTag("quick_add_food_btn"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (newFoodName.isNotBlank()) "+ Aggiungi" else "+ Riga vuota",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
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
                        color = NutriTextPrimary
                    )
                    Text(
                        text = "Valori nutrizionali dettagliati di ogni alimento",
                        fontSize = 11.5.sp,
                        color = NutriTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (ingredients.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF9FAFB))
                        .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nessun alimento dettagliato. Usa il riquadro sopra per aggiungere cibi!",
                        fontSize = 12.5.sp,
                        color = NutriTextSecondary,
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
                        .weight(0.4f)
                        .height(48.dp)
                        .testTag("delete_alternative_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626))
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Elimina",
                        tint = Color(0xFFDC2626),
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

                        val updatedAlt = alternative.copy(
                            name = name.ifBlank { "Alternativa" },
                            notes = notes,
                            ingredientsJson = Ingredient.listToJson(parsedIngredients),
                            totalCalories = totalCalories.toIntOrNull() ?: 0,
                            totalProtein = totalProtein.toIntOrNull() ?: 0,
                            totalCarbs = totalCarbs.toIntOrNull() ?: 0,
                            totalFat = totalFat.toIntOrNull() ?: 0
                        )
                        onSave(updatedAlt)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("save_alternative_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NutriDark,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Salva modifiche",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }

    // Dialog to choose target meal slot for copying
    if (showCopyDialog) {
        val otherSlots = allSlots.filter { it.id != alternative.slotId }
        CopyAlternativeDialog(
            alternativeName = name.ifBlank { alternative.name },
            currentSlotName = currentSlot?.name ?: "Pasto attuale",
            availableSlots = if (otherSlots.isNotEmpty()) otherSlots else allSlots,
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
                val updatedAlt = alternative.copy(
                    name = name.ifBlank { "Alternativa" },
                    notes = notes,
                    ingredientsJson = Ingredient.listToJson(parsedIngredients),
                    totalCalories = totalCalories.toIntOrNull() ?: 0,
                    totalProtein = totalProtein.toIntOrNull() ?: 0,
                    totalCarbs = totalCarbs.toIntOrNull() ?: 0,
                    totalFat = totalFat.toIntOrNull() ?: 0
                )
                onCopyToSlot(updatedAlt, targetSlot)
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
                    .clip(RoundedCornerShape(16.dp)),
                color = Color.White
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Elimina alternativa",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = NutriTextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Sei sicuro di voler eliminare questa alternativa dal piano?",
                        fontSize = 13.5.sp,
                        color = NutriTextSecondary
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showDeleteConfirmDialog = false }) {
                            Text("Annulla", color = NutriTextSecondary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                showDeleteConfirmDialog = false
                                onDelete(alternative)
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

@Composable
internal fun MacroInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    unit: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    tag: String = ""
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = NutriTextSecondary
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            colors = nutriTextFieldColors(),
            textStyle = TextStyle(
                color = NutriTextPrimary,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(tag)
        )
    }
}

@Composable
internal fun SingleFoodItemCard(
    index: Int,
    item: EditableFoodItem,
    onUpdate: (EditableFoodItem) -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp)),
        color = Color(0xFFFAFAFA)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header: Name, quantity and delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = item.name,
                    onValueChange = { onUpdate(item.copy(name = it)) },
                    placeholder = { Text("Nome cibo", fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = nutriTextFieldColors(),
                    textStyle = TextStyle(
                        color = NutriTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("ing_name_$index")
                )

                OutlinedTextField(
                    value = item.quantity,
                    onValueChange = { onUpdate(item.copy(quantity = it)) },
                    placeholder = { Text("es. 100g", fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = nutriTextFieldColors(),
                    textStyle = TextStyle(color = NutriTextPrimary, fontSize = 13.sp),
                    modifier = Modifier
                        .weight(0.9f)
                        .testTag("ing_qty_$index")
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Rimuovi alimento",
                        tint = NutriTextMuted,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sub-values for this ingredient: kcal, prot, carbs, fat
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SingleFoodMacroSmallInput(
                    label = "Kcal",
                    value = item.calories,
                    onValueChange = { onUpdate(item.copy(calories = it)) },
                    color = NutriCalories,
                    modifier = Modifier.weight(1f),
                    tag = "ing_cal_$index"
                )
                SingleFoodMacroSmallInput(
                    label = "Prot(g)",
                    value = item.protein,
                    onValueChange = { onUpdate(item.copy(protein = it)) },
                    color = NutriProtein,
                    modifier = Modifier.weight(1f),
                    tag = "ing_prot_$index"
                )
                SingleFoodMacroSmallInput(
                    label = "Carb(g)",
                    value = item.carbs,
                    onValueChange = { onUpdate(item.copy(carbs = it)) },
                    color = NutriCarbs,
                    modifier = Modifier.weight(1f),
                    tag = "ing_carbs_$index"
                )
                SingleFoodMacroSmallInput(
                    label = "Gras(g)",
                    value = item.fat,
                    onValueChange = { onUpdate(item.copy(fat = it)) },
                    color = NutriFats,
                    modifier = Modifier.weight(1f),
                    tag = "ing_fat_$index"
                )
            }
        }
    }
}

@Composable
internal fun SingleFoodMacroSmallInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    color: Color,
    modifier: Modifier = Modifier,
    tag: String = ""
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = NutriTextSecondary
        )
        Spacer(modifier = Modifier.height(2.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            shape = RoundedCornerShape(6.dp),
            colors = nutriTextFieldColors(),
            textStyle = TextStyle(
                color = NutriTextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(tag)
        )
    }
}

@Composable
internal fun CopyAlternativeDialog(
    alternativeName: String,
    currentSlotName: String,
    availableSlots: List<MealSlotEntity>,
    onDismiss: () -> Unit,
    onSelectSlot: (MealSlotEntity) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp)),
            color = Color.White
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Copia alternativa",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = NutriTextPrimary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Chiudi",
                            tint = NutriTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Scegli in quale pasto vuoi duplicare \"$alternativeName\":",
                    fontSize = 13.sp,
                    color = NutriTextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableSlots.forEach { slot ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp))
                                .clickable { onSelectSlot(slot) }
                                .testTag("copy_to_slot_${slot.id}"),
                            color = Color(0xFFF9FAFB)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = slot.name,
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NutriTextPrimary
                                    )
                                    Text(
                                        text = "Pasto ${slot.orderIndex}" + (if (slot.customCalories != null) " • ${slot.customCalories} kcal target" else ""),
                                        fontSize = 12.sp,
                                        color = NutriTextSecondary
                                    )
                                }

                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    tint = NutriDark,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Annulla", color = NutriTextSecondary)
                }
            }
        }
    }
}
