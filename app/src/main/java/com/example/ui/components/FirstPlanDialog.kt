package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.NutriCalories
import com.example.ui.theme.NutriCaloriesBg
import com.example.ui.theme.NutriCarbs
import com.example.ui.theme.NutriCarbsBg
import com.example.ui.theme.NutriDark
import com.example.ui.theme.NutriFats
import com.example.ui.theme.NutriFatsBg
import com.example.ui.theme.NutriProtein
import com.example.ui.theme.NutriProteinBg
import com.example.ui.theme.NutriTextMuted
import com.example.ui.theme.NutriTextPrimary
import com.example.ui.theme.NutriTextSecondary
import com.example.ui.theme.nutriTextFieldColors

@Composable
fun FirstPlanOnboardingDialog(
    onConfirm: (name: String, calories: Int, protein: Int, carbs: Int, fat: Int, mealsCount: Int) -> Unit
) {
    var name by remember { mutableStateOf("Piano principale") }
    var calories by remember { mutableStateOf("2500") }
    var protein by remember { mutableStateOf("150") }
    var carbs by remember { mutableStateOf("220") }
    var fat by remember { mutableStateOf("60") }
    var mealsCount by remember { mutableIntStateOf(4) }

    Dialog(
        onDismissRequest = { /* Non dismissable until created */ },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .testTag("first_plan_dialog"),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp)
            ) {
                // Header badge
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF3F4F6)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🥗", fontSize = 24.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Benvenuto in NutriPlan!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = NutriTextPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Per iniziare, crea il tuo primo piano nutrizionale. Imposta i tuoi obiettivi giornalieri di calorie, macronutrienti e numero di pasti.",
                    fontSize = 12.5.sp,
                    color = NutriTextSecondary,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Nome piano
                Text(
                    text = "Nome del piano",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NutriTextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("es. Piano principale", color = NutriTextMuted) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("first_plan_name_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = nutriTextFieldColors(),
                    textStyle = TextStyle(color = NutriTextPrimary, fontSize = 14.sp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Calorie Target
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(NutriCaloriesBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = NutriCalories,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Calorie giornaliere",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = NutriTextPrimary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = calories,
                            onValueChange = { calories = it },
                            modifier = Modifier
                                .width(90.dp)
                                .height(46.dp)
                                .testTag("first_plan_calories_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = nutriTextFieldColors(),
                            textStyle = TextStyle(
                                color = NutriTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.End
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "kcal", fontSize = 12.sp, color = NutriTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Proteine
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(NutriProteinBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restaurant,
                                contentDescription = null,
                                tint = NutriProtein,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Proteine",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = NutriTextPrimary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = protein,
                            onValueChange = { protein = it },
                            modifier = Modifier
                                .width(90.dp)
                                .height(46.dp)
                                .testTag("first_plan_protein_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = nutriTextFieldColors(),
                            textStyle = TextStyle(
                                color = NutriTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.End
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "g", fontSize = 12.sp, color = NutriTextSecondary, modifier = Modifier.width(22.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Carbo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(NutriCarbsBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Spa,
                                contentDescription = null,
                                tint = NutriCarbs,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Carboidrati",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = NutriTextPrimary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = carbs,
                            onValueChange = { carbs = it },
                            modifier = Modifier
                                .width(90.dp)
                                .height(46.dp)
                                .testTag("first_plan_carbs_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = nutriTextFieldColors(),
                            textStyle = TextStyle(
                                color = NutriTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.End
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "g", fontSize = 12.sp, color = NutriTextSecondary, modifier = Modifier.width(22.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Grassi
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(NutriFatsBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Opacity,
                                contentDescription = null,
                                tint = NutriFats,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Grassi",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = NutriTextPrimary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = fat,
                            onValueChange = { fat = it },
                            modifier = Modifier
                                .width(90.dp)
                                .height(46.dp)
                                .testTag("first_plan_fat_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = nutriTextFieldColors(),
                            textStyle = TextStyle(
                                color = NutriTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.End
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "g", fontSize = 12.sp, color = NutriTextSecondary, modifier = Modifier.width(22.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Numero pasti
                Text(
                    text = "Numero di pasti al giorno",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NutriTextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    (1..6).forEach { count ->
                        val isSelected = mealsCount == count
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) NutriDark else Color(0xFFF3F4F6))
                                .clickable { mealsCount = count }
                                .testTag("first_plan_meals_$count"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = count.toString(),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else NutriTextPrimary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Submit button
                Button(
                    onClick = {
                        val cal = calories.toIntOrNull() ?: 2500
                        val prot = protein.toIntOrNull() ?: 150
                        val c = carbs.toIntOrNull() ?: 220
                        val f = fat.toIntOrNull() ?: 60
                        val finalName = if (name.isNotBlank()) name else "Piano principale"
                        onConfirm(finalName, cal, prot, c, f, mealsCount)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("submit_first_plan_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NutriDark,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Inizia con questo piano",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
