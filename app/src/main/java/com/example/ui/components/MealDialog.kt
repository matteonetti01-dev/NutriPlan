package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.ai.DishEstimateResult
import com.example.ai.GeminiNutritionService
import com.example.data.entity.Ingredient
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

@Composable
fun MealDialog(
    title: String = "Nuova alternativa",
    geminiService: GeminiNutritionService,
    onDismiss: () -> Unit,
    onConfirm: (name: String, calories: Int, protein: Int, carbs: Int, fat: Int, ingredients: List<Ingredient>, notes: String, photoUri: String?) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Ingredienti, 1: Foto piatto, 2: Manuale
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(24.dp)),
            color = Color.White,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header with title and close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = NutriTextPrimary
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_meal_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Chiudi",
                            tint = NutriTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Segmented Tabs: Ingredienti | Foto piatto | Manuale
                val tabTitles = listOf("Ingredienti", "Foto piatto", "Manuale")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF3F4F6))
                        .padding(4.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        tabTitles.forEachIndexed { index, name ->
                            val isSelected = selectedTab == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color.White else Color.Transparent)
                                    .clickable { selectedTab = index }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = name,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) NutriTextPrimary else NutriTextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Content based on tab
                when (selectedTab) {
                    0 -> TabIngredients(
                        geminiService = geminiService,
                        onConclude = onConfirm
                    )
                    1 -> TabPhotoDish(
                        geminiService = geminiService,
                        onConclude = onConfirm
                    )
                    2 -> TabManual(
                        onConclude = onConfirm
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 1: INGREDIENTI
// -------------------------------------------------------------
@Composable
private fun TabIngredients(
    geminiService: GeminiNutritionService,
    onConclude: (name: String, calories: Int, protein: Int, carbs: Int, fat: Int, ingredients: List<Ingredient>, notes: String, photoUri: String?) -> Unit
) {
    val scope = rememberCoroutineScope()
    var foodName by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val ingredientsList = remember { mutableStateListOf<Ingredient>() }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isLoading = true
                val result = geminiService.scanNutritionalLabel(uri)
                foodName = result.foodName
                quantity = result.portion
                isLoading = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = foodName,
            onValueChange = { foodName = it },
            placeholder = { Text("Nome alimento (es. pasta)", color = NutriTextMuted, fontSize = 14.sp) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ingredient_name_input"),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = nutriTextFieldColors(),
            textStyle = TextStyle(color = NutriTextPrimary, fontSize = 14.sp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = quantity,
            onValueChange = { quantity = it },
            placeholder = { Text("Quantità (es. 80g)", color = NutriTextMuted, fontSize = 14.sp) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ingredient_quantity_input"),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = nutriTextFieldColors(),
            textStyle = TextStyle(color = NutriTextPrimary, fontSize = 14.sp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Two buttons: Etichetta & Aggiungi
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("label_photo_button"),
                shape = RoundedCornerShape(10.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFE5E7EB)))
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = NutriTextPrimary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Etichetta", color = NutriTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }

            Button(
                onClick = {
                    if (foodName.isNotBlank()) {
                        scope.launch {
                            isLoading = true
                            val ing = geminiService.estimateIngredient(
                                foodName,
                                if (quantity.isBlank()) "100g" else quantity
                            )
                            ingredientsList.add(ing)
                            foodName = ""
                            quantity = ""
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("add_ingredient_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF71717A),
                    contentColor = Color.White
                ),
                enabled = !isLoading && foodName.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Aggiungi", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Scrivi nome e quantità e premi Aggiungi per la stima AI, oppure fotografa l'etichetta nutrizionale.",
            fontSize = 11.5.sp,
            color = NutriTextMuted,
            lineHeight = 16.sp
        )

        // Summary of added ingredients
        if (ingredientsList.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF9FAFB))
                    .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Ingredienti aggiunti (${ingredientsList.size}):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NutriTextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    ingredientsList.forEachIndexed { index, ing ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "• ${ing.name} ${ing.quantity}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = NutriTextPrimary
                                )
                                Text(
                                    text = "${ing.calories} kcal • P ${ing.protein}g • C ${ing.carbs}g • G ${ing.fat}g",
                                    fontSize = 11.sp,
                                    color = NutriTextSecondary
                                )
                            }
                            IconButton(
                                onClick = { ingredientsList.removeAt(index) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Rimuovi",
                                    tint = NutriTextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    val totCal = ingredientsList.sumOf { it.calories }
                    val totProt = ingredientsList.sumOf { it.protein }
                    val totCarbs = ingredientsList.sumOf { it.carbs }
                    val totFat = ingredientsList.sumOf { it.fat }

                    Text(
                        text = "Totale: $totCal kcal • P ${totProt}g • C ${totCarbs}g • G ${totFat}g",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NutriCalories
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Big action button: Concludi pasto
        Button(
            onClick = {
                if (ingredientsList.isNotEmpty()) {
                    val totCal = ingredientsList.sumOf { it.calories }
                    val totProt = ingredientsList.sumOf { it.protein }
                    val totCarbs = ingredientsList.sumOf { it.carbs }
                    val totFat = ingredientsList.sumOf { it.fat }
                    val defaultName = ingredientsList.firstOrNull()?.name ?: "Pasto composto"
                    onConclude(defaultName, totCal, totProt, totCarbs, totFat, ingredientsList.toList(), "", null)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("conclude_meal_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF71717A),
                contentColor = Color.White
            ),
            enabled = ingredientsList.isNotEmpty()
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Concludi pasto", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// -------------------------------------------------------------
// TAB 2: FOTO PIATTO
// -------------------------------------------------------------
@Composable
private fun TabPhotoDish(
    geminiService: GeminiNutritionService,
    onConclude: (name: String, calories: Int, protein: Int, carbs: Int, fat: Int, ingredients: List<Ingredient>, notes: String, photoUri: String?) -> Unit
) {
    val scope = rememberCoroutineScope()
    var notes by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    var estimateResult by remember { mutableStateOf<DishEstimateResult?>(null) }
    var followUpAnswer by remember { mutableStateOf("") }
    val conversationHistory = remember { mutableStateListOf<Pair<String, String>>() }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Thumbnail preview if photo chosen
        if (selectedImageUri != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF3F4F6)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = selectedImageUri,
                    contentDescription = "Foto piatto",
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Crop
                )
                IconButton(
                    onClick = { selectedImageUri = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(28.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Rimuovi foto",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            placeholder = { Text("Note (es. porzione abbondante, fuori casa...)", color = NutriTextMuted, fontSize = 14.sp) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("dish_notes_input"),
            shape = RoundedCornerShape(12.dp),
            colors = nutriTextFieldColors(),
            textStyle = TextStyle(color = NutriTextPrimary, fontSize = 14.sp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("dish_photo_button"),
                shape = RoundedCornerShape(10.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFE5E7EB)))
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = NutriTextPrimary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (selectedImageUri != null) "Cambia" else "Foto",
                    color = NutriTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        val res = geminiService.estimateDish(
                            photoUri = selectedImageUri,
                            notes = notes,
                            conversationHistory = conversationHistory.toList()
                        )
                        estimateResult = res
                        isLoading = false
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("dish_estimate_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF71717A),
                    contentColor = Color.White
                ),
                enabled = !isLoading && (notes.isNotBlank() || selectedImageUri != null)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Stima", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Foto del piatto intero: l'AI può farti domande di follow-up per affinare la stima.",
            fontSize = 11.5.sp,
            color = NutriTextMuted,
            lineHeight = 16.sp
        )

        // Estimated result display
        estimateResult?.let { result ->
            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF9FAFB))
                    .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = result.mealName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = NutriTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${result.calories} kcal • P ${result.protein}g • C ${result.carbs}g • G ${result.fat}g",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NutriCalories
                    )

                    // Follow-up question if present
                    if (!result.followUpQuestion.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFEFF6FF))
                                .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(
                                    text = "💡 Follow-up AI:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1D4ED8)
                                )
                                Text(
                                    text = result.followUpQuestion,
                                    fontSize = 12.sp,
                                    color = Color(0xFF1E40AF)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = followUpAnswer,
                                        onValueChange = { followUpAnswer = it },
                                        placeholder = { Text("Rispondi qui...", fontSize = 12.sp, color = NutriTextMuted) },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp),
                                        colors = nutriTextFieldColors(),
                                        textStyle = TextStyle(color = NutriTextPrimary, fontSize = 12.sp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Button(
                                        onClick = {
                                            if (followUpAnswer.isNotBlank()) {
                                                conversationHistory.add(result.followUpQuestion to followUpAnswer)
                                                scope.launch {
                                                    isLoading = true
                                                    val refined = geminiService.estimateDish(
                                                        photoUri = selectedImageUri,
                                                        notes = "$notes (Follow up: $followUpAnswer)",
                                                        conversationHistory = conversationHistory.toList()
                                                    )
                                                    estimateResult = refined
                                                    followUpAnswer = ""
                                                    isLoading = false
                                                }
                                            }
                                        },
                                        modifier = Modifier.height(40.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                                    ) {
                                        Text("Affina", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Concludi pasto button
            Button(
                onClick = {
                    onConclude(
                        result.mealName,
                        result.calories,
                        result.protein,
                        result.carbs,
                        result.fat,
                        result.ingredients,
                        notes,
                        selectedImageUri?.toString()
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("conclude_photo_meal_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NutriDark,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Concludi pasto", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 3: MANUALE
// -------------------------------------------------------------
@Composable
private fun TabManual(
    onConclude: (name: String, calories: Int, protein: Int, carbs: Int, fat: Int, ingredients: List<Ingredient>, notes: String, photoUri: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var kcal by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Nome", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NutriTextPrimary)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = { Text("es. Pranzo fatto in casa", color = NutriTextMuted, fontSize = 14.sp) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("manual_name_input"),
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = nutriTextFieldColors(),
            textStyle = TextStyle(color = NutriTextPrimary, fontSize = 14.sp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Kcal", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NutriTextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = kcal,
                    onValueChange = { kcal = it },
                    placeholder = { Text("0", color = NutriTextMuted) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_kcal_input"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    colors = nutriTextFieldColors(),
                    textStyle = TextStyle(color = NutriTextPrimary, fontSize = 14.sp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text("Proteine (g)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NutriTextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = protein,
                    onValueChange = { protein = it },
                    placeholder = { Text("0", color = NutriTextMuted) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_protein_input"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    colors = nutriTextFieldColors(),
                    textStyle = TextStyle(color = NutriTextPrimary, fontSize = 14.sp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Carbo (g)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NutriTextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = carbs,
                    onValueChange = { carbs = it },
                    placeholder = { Text("0", color = NutriTextMuted) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_carbs_input"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    colors = nutriTextFieldColors(),
                    textStyle = TextStyle(color = NutriTextPrimary, fontSize = 14.sp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text("Grassi (g)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NutriTextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = fat,
                    onValueChange = { fat = it },
                    placeholder = { Text("0", color = NutriTextMuted) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_fat_input"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    colors = nutriTextFieldColors(),
                    textStyle = TextStyle(color = NutriTextPrimary, fontSize = 14.sp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        val isFormValid = (kcal.toIntOrNull() != null || protein.toIntOrNull() != null)
        Button(
            onClick = {
                val calVal = kcal.toIntOrNull() ?: 0
                val protVal = protein.toIntOrNull() ?: 0
                val carbsVal = carbs.toIntOrNull() ?: 0
                val fatVal = fat.toIntOrNull() ?: 0
                val finalName = if (name.isNotBlank()) name else "Pasto manuale"
                onConclude(finalName, calVal, protVal, carbsVal, fatVal, emptyList(), "", null)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("conclude_manual_meal_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = NutriDark,
                contentColor = Color.White
            ),
            enabled = isFormValid
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Concludi pasto", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
