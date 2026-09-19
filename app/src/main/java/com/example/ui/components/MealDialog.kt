package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Image
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.core.content.ContextCompat
import com.example.util.CameraUtils
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
import com.example.ui.theme.ApexBlack
import com.example.ui.theme.ApexBorder
import com.example.ui.theme.ApexCalories
import com.example.ui.theme.ApexCarbs
import com.example.ui.theme.ApexCyanAccent
import com.example.ui.theme.ApexDarkSurface
import com.example.ui.theme.ApexDarkSurfaceHighlight
import com.example.ui.theme.ApexFats
import com.example.ui.theme.ApexGreen
import com.example.ui.theme.ApexNeonLime
import com.example.ui.theme.ApexProtein
import com.example.ui.theme.ApexTextMuted
import com.example.ui.theme.ApexTextPrimary
import com.example.ui.theme.ApexTextSecondary
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
            color = ApexDarkSurface,
            border = BorderStroke(1.dp, ApexBorder),
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
                        color = ApexTextPrimary
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_meal_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Chiudi",
                            tint = ApexTextSecondary
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
                        .background(ApexDarkSurfaceHighlight)
                        .border(1.dp, ApexBorder, RoundedCornerShape(12.dp))
                        .padding(4.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        tabTitles.forEachIndexed { index, name ->
                            val isSelected = selectedTab == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) ApexNeonLime else Color.Transparent)
                                    .clickable { selectedTab = index }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = name,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) ApexBlack else ApexTextSecondary
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var foodName by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val ingredientsList = remember { mutableStateListOf<Ingredient>() }

    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

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

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            scope.launch {
                isLoading = true
                val result = geminiService.scanNutritionalLabel(tempCameraUri)
                foodName = result.foodName
                quantity = result.portion
                isLoading = false
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                val uri = CameraUtils.createTempImageUri(context)
                tempCameraUri = uri
                takePictureLauncher.launch(uri)
            } catch (e: Exception) {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        } else {
            photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
    }

    fun launchCameraForLabel() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            try {
                val uri = CameraUtils.createTempImageUri(context)
                tempCameraUri = uri
                takePictureLauncher.launch(uri)
            } catch (e: Exception) {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = foodName,
            onValueChange = { foodName = it },
            placeholder = { Text("Nome alimento (es. pasta)", color = ApexTextMuted, fontSize = 14.sp) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ingredient_name_input"),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = nutriTextFieldColors(),
            textStyle = TextStyle(color = ApexTextPrimary, fontSize = 14.sp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = quantity,
            onValueChange = { quantity = it },
            placeholder = { Text("Quantità (es. 80g)", color = ApexTextMuted, fontSize = 14.sp) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ingredient_quantity_input"),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = nutriTextFieldColors(),
            textStyle = TextStyle(color = ApexTextPrimary, fontSize = 14.sp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Buttons: Fotocamera Etichetta, Archivio & Aggiungi
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { launchCameraForLabel() },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("label_photo_button"),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, ApexBorder),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = ApexTextPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = ApexNeonLime
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Scatta etichetta", color = ApexTextPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
            }

            IconButton(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier
                    .size(44.dp)
                    .border(1.dp, ApexBorder, RoundedCornerShape(10.dp))
                    .testTag("label_gallery_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = "Scegli da galleria",
                    tint = ApexTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
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
                    containerColor = ApexNeonLime,
                    contentColor = ApexBlack
                ),
                enabled = !isLoading && foodName.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = ApexBlack, strokeWidth = 2.dp)
                } else {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Aggiungi", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Scrivi nome e quantità e premi Aggiungi per la stima AI, oppure fotografa l'etichetta nutrizionale.",
            fontSize = 11.5.sp,
            color = ApexTextMuted,
            lineHeight = 16.sp
        )

        // Summary of added ingredients
        if (ingredientsList.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ApexDarkSurfaceHighlight)
                    .border(1.dp, ApexBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Ingredienti aggiunti (${ingredientsList.size}):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ApexTextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    ingredientsList.forEachIndexed { index, ing ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "• ${ing.name} ${ing.quantity}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = ApexTextPrimary
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${ing.calories} kcal",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ApexCalories
                                    )
                                    Text(text = "•", fontSize = 9.sp, color = ApexBorder)
                                    Text(
                                        text = "P ${ing.protein}g",
                                        fontSize = 11.sp,
                                        color = ApexProtein
                                    )
                                    Text(text = "•", fontSize = 9.sp, color = ApexBorder)
                                    Text(
                                        text = "C ${ing.carbs}g",
                                        fontSize = 11.sp,
                                        color = ApexCarbs
                                    )
                                    Text(text = "•", fontSize = 9.sp, color = ApexBorder)
                                    Text(
                                        text = "G ${ing.fat}g",
                                        fontSize = 11.sp,
                                        color = ApexFats
                                    )
                                }
                            }
                            IconButton(
                                onClick = { ingredientsList.removeAt(index) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Rimuovi",
                                    tint = ApexTextMuted,
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Totale: $totCal kcal",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ApexCalories
                        )
                        Text(text = "•", fontSize = 10.sp, color = ApexBorder)
                        Text(
                            text = "P: ${totProt}g",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ApexProtein
                        )
                        Text(text = "•", fontSize = 10.sp, color = ApexBorder)
                        Text(
                            text = "C: ${totCarbs}g",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ApexCarbs
                        )
                        Text(text = "•", fontSize = 10.sp, color = ApexBorder)
                        Text(
                            text = "G: ${totFat}g",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ApexFats
                        )
                    }
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
                containerColor = ApexNeonLime,
                contentColor = ApexBlack
            ),
            enabled = ingredientsList.isNotEmpty()
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Concludi pasto", fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var notes by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    var estimateResult by remember { mutableStateOf<DishEstimateResult?>(null) }
    var followUpAnswer by remember { mutableStateOf("") }
    val conversationHistory = remember { mutableStateListOf<Pair<String, String>>() }

    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            selectedImageUri = tempCameraUri
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                val uri = CameraUtils.createTempImageUri(context)
                tempCameraUri = uri
                takePictureLauncher.launch(uri)
            } catch (e: Exception) {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        } else {
            photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
    }

    fun launchCameraForDish() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            try {
                val uri = CameraUtils.createTempImageUri(context)
                tempCameraUri = uri
                takePictureLauncher.launch(uri)
            } catch (e: Exception) {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
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
                    .background(ApexDarkSurfaceHighlight)
                    .border(1.dp, ApexBorder, RoundedCornerShape(12.dp)),
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
                        .background(ApexBlack.copy(alpha = 0.7f), CircleShape)
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
            placeholder = { Text("Note (es. porzione abbondante, fuori casa...)", color = ApexTextMuted, fontSize = 14.sp) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("dish_notes_input"),
            shape = RoundedCornerShape(12.dp),
            colors = nutriTextFieldColors(),
            textStyle = TextStyle(color = ApexTextPrimary, fontSize = 14.sp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { launchCameraForDish() },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("dish_photo_button"),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, ApexBorder),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = ApexTextPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = ApexNeonLime
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (selectedImageUri != null) "Rifai foto" else "Scatta foto",
                    color = ApexTextPrimary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            IconButton(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier
                    .size(44.dp)
                    .border(1.dp, ApexBorder, RoundedCornerShape(10.dp))
                    .testTag("dish_gallery_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = "Scegli da galleria",
                    tint = ApexTextSecondary,
                    modifier = Modifier.size(18.dp)
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
                    containerColor = ApexNeonLime,
                    contentColor = ApexBlack
                ),
                enabled = !isLoading && (notes.isNotBlank() || selectedImageUri != null)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = ApexBlack, strokeWidth = 2.dp)
                } else {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Stima", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Foto del piatto intero: l'AI può farti domande di follow-up per affinare la stima.",
            fontSize = 11.5.sp,
            color = ApexTextMuted,
            lineHeight = 16.sp
        )

        // Estimated result display (AI SCAN & ESTIMATION BANNER)
        estimateResult?.let { result ->
            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ApexDarkSurfaceHighlight)
                    .border(1.dp, ApexNeonLime.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = result.mealName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = ApexTextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(ApexNeonLime.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Stima AI",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ApexNeonLime
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${result.calories} kcal",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = ApexCalories
                        )
                        Text(text = "•", fontSize = 10.sp, color = ApexBorder)
                        Text(
                            text = "P: ${result.protein}g",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ApexProtein
                        )
                        Text(text = "•", fontSize = 10.sp, color = ApexBorder)
                        Text(
                            text = "C: ${result.carbs}g",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ApexCarbs
                        )
                        Text(text = "•", fontSize = 10.sp, color = ApexBorder)
                        Text(
                            text = "G: ${result.fat}g",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ApexFats
                        )
                    }

                    // Follow-up question if present
                    if (!result.followUpQuestion.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(ApexDarkSurface)
                                .border(1.dp, ApexCyanAccent.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(
                                    text = "💡 Follow-up AI:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ApexCyanAccent
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = result.followUpQuestion,
                                    fontSize = 12.sp,
                                    color = ApexTextPrimary,
                                    lineHeight = 16.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = followUpAnswer,
                                        onValueChange = { followUpAnswer = it },
                                        placeholder = { Text("Rispondi qui...", fontSize = 12.sp, color = ApexTextMuted) },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp),
                                        colors = nutriTextFieldColors(),
                                        textStyle = TextStyle(color = ApexTextPrimary, fontSize = 12.sp)
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
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = ApexNeonLime,
                                            contentColor = ApexBlack
                                        )
                                    ) {
                                        Text("Affina", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                    containerColor = ApexNeonLime,
                    contentColor = ApexBlack
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Concludi pasto", fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
        Text("Nome", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = ApexTextSecondary)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = { Text("es. Pranzo fatto in casa", color = ApexTextMuted, fontSize = 14.sp) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("manual_name_input"),
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = nutriTextFieldColors(),
            textStyle = TextStyle(color = ApexTextPrimary, fontSize = 14.sp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Kcal", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = ApexTextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = kcal,
                    onValueChange = { kcal = it },
                    placeholder = { Text("0", color = ApexTextMuted) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_kcal_input"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    colors = nutriTextFieldColors(),
                    textStyle = TextStyle(color = ApexTextPrimary, fontSize = 14.sp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text("Proteine (g)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = ApexTextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = protein,
                    onValueChange = { protein = it },
                    placeholder = { Text("0", color = ApexTextMuted) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_protein_input"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    colors = nutriTextFieldColors(),
                    textStyle = TextStyle(color = ApexTextPrimary, fontSize = 14.sp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Carbo (g)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = ApexTextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = carbs,
                    onValueChange = { carbs = it },
                    placeholder = { Text("0", color = ApexTextMuted) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_carbs_input"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    colors = nutriTextFieldColors(),
                    textStyle = TextStyle(color = ApexTextPrimary, fontSize = 14.sp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text("Grassi (g)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = ApexTextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = fat,
                    onValueChange = { fat = it },
                    placeholder = { Text("0", color = ApexTextMuted) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_fat_input"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    colors = nutriTextFieldColors(),
                    textStyle = TextStyle(color = ApexTextPrimary, fontSize = 14.sp)
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
                containerColor = ApexNeonLime,
                contentColor = ApexBlack
            ),
            enabled = isFormValid
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Concludi pasto", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}
