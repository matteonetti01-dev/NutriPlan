package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.Ingredient
import com.example.data.entity.LoggedMealEntity
import com.example.ui.components.LoggedMealDetailSheet
import com.example.ui.components.MealDialog
import com.example.ui.theme.NutriBgLight
import com.example.ui.theme.NutriCalories
import com.example.ui.theme.NutriCarbs
import com.example.ui.theme.NutriCardBg
import com.example.ui.theme.NutriCardBorder
import com.example.ui.theme.NutriDark
import com.example.ui.theme.NutriFats
import com.example.ui.theme.NutriProtein
import com.example.ui.theme.NutriTextMuted
import com.example.ui.theme.NutriTextPrimary
import com.example.ui.theme.NutriTextSecondary
import com.example.ui.viewmodel.NutritionViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun OutdoorDayScreen(
    viewModel: NutritionViewModel
) {
    val activePlan by viewModel.activePlan.collectAsState()
    val slots by viewModel.activePlanSlots.collectAsState()
    val loggedMeals by viewModel.todayLoggedMeals.collectAsState()
    val todayCalories by viewModel.todayCalories.collectAsState()
    val todayProtein by viewModel.todayProtein.collectAsState()
    val todayCarbs by viewModel.todayCarbs.collectAsState()
    val todayFat by viewModel.todayFat.collectAsState()

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showAddMealDialog by remember { mutableStateOf(false) }
    var selectedMealForDetail by remember { mutableStateOf<LoggedMealEntity?>(null) }

    val targetCalories = activePlan?.caloriesTarget ?: 2500
    val targetProtein = activePlan?.proteinTarget ?: 150
    val targetCarbs = activePlan?.carbsTarget ?: 220
    val targetFat = activePlan?.fatTarget ?: 60

    // Formatted current date e.g. "Martedì 8 Settembre"
    val formattedDate = remember {
        SimpleDateFormat("EEEE d MMMM", Locale.ITALIAN).format(Date())
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ITALIAN) else it.toString() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NutriBgLight)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .testTag("outdoor_day_screen")
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))

                // Header with Airplane Icon & Title
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEFF6FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flight,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Giornata fuori",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = NutriTextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Conta calorie con stima AI per ogni pasto, anche in vacanza.",
                    fontSize = 13.sp,
                    color = NutriTextSecondary,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formattedDate,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = NutriTextMuted
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Daily Progress Card
                DailyMacrosProgressCard(
                    currentCalories = todayCalories,
                    targetCalories = targetCalories,
                    currentProtein = todayProtein,
                    targetProtein = targetProtein,
                    currentCarbs = todayCarbs,
                    targetCarbs = targetCarbs,
                    currentFat = todayFat,
                    targetFat = targetFat
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Pasti registrati oggi",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = NutriTextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (loggedMeals.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(NutriCardBg)
                            .border(1.dp, NutriCardBorder, RoundedCornerShape(16.dp))
                            .padding(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nessun pasto registrato oggi.\nAggiungi il primo con la stima AI.",
                            fontSize = 13.5.sp,
                            color = NutriTextMuted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }
            } else {
                items(loggedMeals, key = { it.id }) { meal ->
                    LoggedMealCard(
                        meal = meal,
                        onClick = { selectedMealForDetail = meal },
                        onDelete = { viewModel.deleteLoggedMeal(meal) }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            // Bottom space so content isn't covered by bottom button
            item {
                Spacer(modifier = Modifier.height(90.dp))
            }
        }

        // Snackbar Host for feedback
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 75.dp)
        )

        // Floating Bottom "+ Aggiungi pasto" Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Button(
                onClick = { showAddMealDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("add_outdoor_meal_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NutriDark,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Aggiungi pasto",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    // Tendina / Bottom Sheet for inspecting, editing, and copying logged meal in Giornata Fuori
    selectedMealForDetail?.let { meal ->
        LoggedMealDetailSheet(
            meal = meal,
            planSlots = slots,
            geminiService = viewModel.geminiService,
            onDismiss = { selectedMealForDetail = null },
            onSave = { updated ->
                viewModel.updateLoggedMeal(updated)
                selectedMealForDetail = null
                scope.launch {
                    snackbarHostState.showSnackbar("Pasto aggiornato con successo! ✓")
                }
            },
            onCopyToPlanSlot = { mealToCopy, targetSlot ->
                activePlan?.let { plan ->
                    viewModel.copyLoggedMealToPlanSlot(mealToCopy, targetSlot.id, plan.id)
                    scope.launch {
                        snackbarHostState.showSnackbar("Pasto copiato in ${targetSlot.name}! ✓")
                    }
                }
            },
            onDelete = { toDelete ->
                viewModel.deleteLoggedMeal(toDelete)
                selectedMealForDetail = null
                scope.launch {
                    snackbarHostState.showSnackbar("Pasto eliminato.")
                }
            }
        )
    }

    if (showAddMealDialog) {
        MealDialog(
            title = "Aggiungi pasto",
            geminiService = viewModel.geminiService,
            onDismiss = { showAddMealDialog = false },
            onConfirm = { name, cal, prot, c, f, ings, notes, photoUri ->
                val summary = if (ings.isNotEmpty()) {
                    ings.joinToString(", ") { "${it.name} ${it.quantity}" }
                } else notes
                viewModel.logDirectMeal(
                    name = name,
                    calories = cal,
                    protein = prot,
                    carbs = c,
                    fat = f,
                    notes = notes,
                    photoUri = photoUri,
                    ingredientsSummary = summary,
                    ingredientsJson = Ingredient.listToJson(ings)
                )
                showAddMealDialog = false
            }
        )
    }
}

@Composable
private fun DailyMacrosProgressCard(
    currentCalories: Int,
    targetCalories: Int,
    currentProtein: Int,
    targetProtein: Int,
    currentCarbs: Int,
    targetCarbs: Int,
    currentFat: Int,
    targetFat: Int
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, NutriCardBorder, RoundedCornerShape(20.dp))
            .testTag("daily_progress_card"),
        color = NutriCardBg
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Obiettivi del giorno",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NutriTextPrimary
                )
                Text(
                    text = "$currentCalories / $targetCalories kcal",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = NutriDark
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4 Macro progress lines: Calorie, Proteine, Carbo, Grassi
            MacroProgressBar(
                icon = Icons.Default.LocalFireDepartment,
                iconColor = NutriCalories,
                label = "Calorie",
                current = currentCalories,
                target = targetCalories,
                unit = "kcal"
            )

            Spacer(modifier = Modifier.height(12.dp))

            MacroProgressBar(
                icon = Icons.Default.Restaurant,
                iconColor = NutriProtein,
                label = "Proteine",
                current = currentProtein,
                target = targetProtein,
                unit = "g"
            )

            Spacer(modifier = Modifier.height(12.dp))

            MacroProgressBar(
                icon = Icons.Default.Spa,
                iconColor = NutriCarbs,
                label = "Carbo",
                current = currentCarbs,
                target = targetCarbs,
                unit = "g"
            )

            Spacer(modifier = Modifier.height(12.dp))

            MacroProgressBar(
                icon = Icons.Default.Opacity,
                iconColor = NutriFats,
                label = "Grassi",
                current = currentFat,
                target = targetFat,
                unit = "g"
            )
        }
    }
}

@Composable
private fun MacroProgressBar(
    icon: ImageVector,
    iconColor: Color,
    label: String,
    current: Int,
    target: Int,
    unit: String
) {
    val progress = if (target > 0) (current.toFloat() / target.toFloat()).coerceIn(0f, 1f) else 0f

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = NutriTextPrimary
                )
            }
            Text(
                text = "$current / $target $unit",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = NutriTextSecondary
            )
        }
        Spacer(modifier = Modifier.height(5.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = iconColor,
            trackColor = Color(0xFFF3F4F6)
        )
    }
}

@Composable
private fun LoggedMealCard(
    meal: LoggedMealEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag("logged_meal_${meal.id}"),
        color = Color(0xFFFAFAFA)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = meal.time,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NutriTextMuted
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = meal.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = NutriTextPrimary
                    )
                }

                if (meal.ingredientsSummary.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = meal.ingredientsSummary,
                        fontSize = 11.5.sp,
                        color = NutriTextSecondary,
                        maxLines = 2
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${meal.calories} kcal • P ${meal.protein}g • C ${meal.carbs}g • G ${meal.fat}g",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NutriCalories
                )

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Tocca per visualizzare o modificare",
                    fontSize = 10.5.sp,
                    color = NutriTextMuted
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Elimina",
                    tint = NutriTextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
