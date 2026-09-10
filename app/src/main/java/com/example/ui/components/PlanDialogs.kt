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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.entity.PlanEntity
import com.example.ui.theme.NutriDark
import com.example.ui.theme.NutriTextMuted
import com.example.ui.theme.NutriTextPrimary
import com.example.ui.theme.NutriTextSecondary
import com.example.ui.theme.nutriTextFieldColors

@Composable
fun NewPlanDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, calories: Int, protein: Int, carbs: Int, fat: Int, mealsCount: Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("2500") }
    var protein by remember { mutableStateOf("150") }
    var carbs by remember { mutableStateOf("220") }
    var fat by remember { mutableStateOf("60") }
    var mealsCount by remember { mutableIntStateOf(4) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp)),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Nuovo piano",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = NutriTextPrimary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_new_plan_dialog")) {
                        Icon(Icons.Default.Close, contentDescription = "Chiudi", tint = NutriTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Nome piano", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = NutriTextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("es. Definizione estiva", color = NutriTextMuted) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_plan_name_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = nutriTextFieldColors(),
                    textStyle = TextStyle(color = NutriTextPrimary, fontSize = 14.sp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Calorie (kcal)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = NutriTextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = calories,
                            onValueChange = { calories = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("new_plan_calories_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = nutriTextFieldColors(),
                            textStyle = TextStyle(color = NutriTextPrimary, fontSize = 14.sp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Proteine (g)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = NutriTextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = protein,
                            onValueChange = { protein = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("new_plan_protein_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = nutriTextFieldColors(),
                            textStyle = TextStyle(color = NutriTextPrimary, fontSize = 14.sp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Carboidrati (g)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = NutriTextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = carbs,
                            onValueChange = { carbs = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("new_plan_carbs_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = nutriTextFieldColors(),
                            textStyle = TextStyle(color = NutriTextPrimary, fontSize = 14.sp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Grassi (g)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = NutriTextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = fat,
                            onValueChange = { fat = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("new_plan_fat_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = nutriTextFieldColors(),
                            textStyle = TextStyle(color = NutriTextPrimary, fontSize = 14.sp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Numero pasti", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = NutriTextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    (1..6).forEach { count ->
                        val isSelected = mealsCount == count
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) NutriDark else Color(0xFFF3F4F6))
                                .clickable { mealsCount = count }
                                .testTag("new_plan_meals_$count"),
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

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val cal = calories.toIntOrNull() ?: 2000
                        val prot = protein.toIntOrNull() ?: 150
                        val c = carbs.toIntOrNull() ?: 200
                        val f = fat.toIntOrNull() ?: 60
                        val planName = if (name.isNotBlank()) name else "Nuovo piano"
                        onConfirm(planName, cal, prot, c, f, mealsCount)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("submit_new_plan_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NutriDark,
                        contentColor = Color.White
                    )
                ) {
                    Text("Crea piano", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun EditPlanDialog(
    plan: PlanEntity,
    onDismiss: () -> Unit,
    onConfirm: (updated: PlanEntity) -> Unit
) {
    var name by remember { mutableStateOf(plan.name) }
    var calories by remember { mutableStateOf(plan.caloriesTarget.toString()) }
    var protein by remember { mutableStateOf(plan.proteinTarget.toString()) }
    var carbs by remember { mutableStateOf(plan.carbsTarget.toString()) }
    var fat by remember { mutableStateOf(plan.fatTarget.toString()) }
    var mealsCount by remember { mutableIntStateOf(plan.mealsCount) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp)),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Modifica piano",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = NutriTextPrimary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_edit_plan_dialog")) {
                        Icon(Icons.Default.Close, contentDescription = "Chiudi", tint = NutriTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Nome piano", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = NutriTextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_plan_name_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = nutriTextFieldColors(),
                    textStyle = TextStyle(color = NutriTextPrimary, fontSize = 14.sp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Calorie (kcal)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = NutriTextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = calories,
                            onValueChange = { calories = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_plan_calories_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = nutriTextFieldColors(),
                            textStyle = TextStyle(color = NutriTextPrimary, fontSize = 14.sp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Proteine (g)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = NutriTextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = protein,
                            onValueChange = { protein = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_plan_protein_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = nutriTextFieldColors(),
                            textStyle = TextStyle(color = NutriTextPrimary, fontSize = 14.sp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Carboidrati (g)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = NutriTextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = carbs,
                            onValueChange = { carbs = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_plan_carbs_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = nutriTextFieldColors(),
                            textStyle = TextStyle(color = NutriTextPrimary, fontSize = 14.sp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Grassi (g)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = NutriTextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = fat,
                            onValueChange = { fat = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_plan_fat_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = nutriTextFieldColors(),
                            textStyle = TextStyle(color = NutriTextPrimary, fontSize = 14.sp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Numero pasti", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = NutriTextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    (1..6).forEach { count ->
                        val isSelected = mealsCount == count
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) NutriDark else Color(0xFFF3F4F6))
                                .clickable { mealsCount = count }
                                .testTag("edit_plan_meals_$count"),
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

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val cal = calories.toIntOrNull() ?: plan.caloriesTarget
                        val prot = protein.toIntOrNull() ?: plan.proteinTarget
                        val c = carbs.toIntOrNull() ?: plan.carbsTarget
                        val f = fat.toIntOrNull() ?: plan.fatTarget
                        val planName = if (name.isNotBlank()) name else plan.name
                        onConfirm(
                            plan.copy(
                                name = planName,
                                caloriesTarget = cal,
                                proteinTarget = prot,
                                carbsTarget = c,
                                fatTarget = f,
                                mealsCount = mealsCount
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("submit_edit_plan_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NutriDark,
                        contentColor = Color.White
                    )
                ) {
                    Text("Salva modifiche", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
