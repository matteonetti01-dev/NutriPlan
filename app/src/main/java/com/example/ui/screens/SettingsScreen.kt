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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NutriBadgeBg
import com.example.ui.theme.NutriBadgeText
import com.example.ui.theme.NutriBgLight
import com.example.ui.theme.NutriCalories
import com.example.ui.theme.NutriCaloriesBg
import com.example.ui.theme.NutriCarbs
import com.example.ui.theme.NutriCarbsBg
import com.example.ui.theme.NutriCardBg
import com.example.ui.theme.NutriCardBorder
import com.example.ui.theme.NutriDark
import com.example.ui.theme.NutriFats
import com.example.ui.theme.NutriFatsBg
import com.example.ui.theme.NutriGreen
import com.example.ui.theme.NutriProtein
import com.example.ui.theme.NutriProteinBg
import com.example.ui.theme.NutriTextMuted
import com.example.ui.theme.NutriTextPrimary
import com.example.ui.theme.NutriTextSecondary
import com.example.ui.theme.nutriTextFieldColors
import com.example.ui.viewmodel.NutritionViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: NutritionViewModel
) {
    val activePlan by viewModel.activePlan.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var mealsCount by remember { mutableIntStateOf(4) }

    // Sync state when active plan loads or changes
    LaunchedEffect(activePlan) {
        activePlan?.let { plan ->
            calories = plan.caloriesTarget.toString()
            protein = plan.proteinTarget.toString()
            carbs = plan.carbsTarget.toString()
            fat = plan.fatTarget.toString()
            mealsCount = plan.mealsCount
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(NutriBgLight)
                .padding(horizontal = 20.dp)
                .testTag("settings_screen")
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Stai modificando il piano attivo",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NutriTextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = activePlan?.name ?: "Nessun piano",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = NutriTextPrimary
                    )

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(NutriBadgeBg)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = NutriGreen,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Piano attivo",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = NutriBadgeText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Aggiorna gli obiettivi del piano che stai seguendo.",
                    fontSize = 13.sp,
                    color = NutriTextSecondary
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Input Card: Daily Targets
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, NutriCardBorder, RoundedCornerShape(18.dp)),
                    color = NutriCardBg
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        TargetInputRow(
                            icon = Icons.Default.LocalFireDepartment,
                            iconColor = NutriCalories,
                            bgColor = NutriCaloriesBg,
                            label = "Calorie giornaliere",
                            suffix = "kcal",
                            value = calories,
                            testTag = "settings_calories_input",
                            onValueChange = { calories = it }
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        TargetInputRow(
                            icon = Icons.Default.Restaurant,
                            iconColor = NutriProtein,
                            bgColor = NutriProteinBg,
                            label = "Proteine",
                            suffix = "g",
                            value = protein,
                            testTag = "settings_protein_input",
                            onValueChange = { protein = it }
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        TargetInputRow(
                            icon = Icons.Default.Spa,
                            iconColor = NutriCarbs,
                            bgColor = NutriCarbsBg,
                            label = "Carboidrati",
                            suffix = "g",
                            value = carbs,
                            testTag = "settings_carbs_input",
                            onValueChange = { carbs = it }
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        TargetInputRow(
                            icon = Icons.Default.Opacity,
                            iconColor = NutriFats,
                            bgColor = NutriFatsBg,
                            label = "Grassi",
                            suffix = "g",
                            value = fat,
                            testTag = "settings_fat_input",
                            onValueChange = { fat = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Meal Count Selection Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, NutriCardBorder, RoundedCornerShape(18.dp)),
                    color = NutriCardBg
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Numero di pasti al giorno",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NutriTextPrimary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Pill buttons: 1, 2, 3, 4, 5, 6
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            (1..6).forEach { count ->
                                val isSelected = mealsCount == count
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) NutriDark else Color(0xFFF3F4F6))
                                        .clickable { mealsCount = count }
                                        .testTag("settings_meals_$count"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = count.toString(),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else NutriTextPrimary,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "I pasti verranno aggiornati in automatico. Potrai personalizzarli nella pagina Piano.",
                            fontSize = 11.5.sp,
                            color = NutriTextMuted,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // "Salva modifiche" Button
                Button(
                    onClick = {
                        val cal = calories.toIntOrNull() ?: (activePlan?.caloriesTarget ?: 2500)
                        val prot = protein.toIntOrNull() ?: (activePlan?.proteinTarget ?: 150)
                        val c = carbs.toIntOrNull() ?: (activePlan?.carbsTarget ?: 220)
                        val f = fat.toIntOrNull() ?: (activePlan?.fatTarget ?: 60)
                        viewModel.updateActivePlanTargets(cal, prot, c, f, mealsCount)
                        scope.launch {
                            snackbarHostState.showSnackbar("Obiettivi del piano salvati con successo! ✓")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_settings_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NutriDark,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Salva modifiche",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}

@Composable
private fun TargetInputRow(
    icon: ImageVector,
    iconColor: Color,
    bgColor: Color,
    label: String,
    suffix: String,
    value: String,
    testTag: String,
    onValueChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1.2f)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(17.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium,
                color = NutriTextPrimary
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(0.8f),
            horizontalArrangement = Arrangement.End
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .width(90.dp)
                    .height(48.dp)
                    .testTag(testTag),
                shape = RoundedCornerShape(10.dp),
                colors = nutriTextFieldColors(),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NutriTextPrimary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = suffix,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = NutriTextSecondary,
                modifier = Modifier.width(30.dp)
            )
        }
    }
}
