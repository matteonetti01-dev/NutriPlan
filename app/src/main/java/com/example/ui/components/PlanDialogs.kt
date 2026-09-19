package com.example.ui.components

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
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.entity.PlanEntity
import com.example.ui.theme.ApexBlack
import com.example.ui.theme.ApexBorder
import com.example.ui.theme.ApexCalories
import com.example.ui.theme.ApexCaloriesBg
import com.example.ui.theme.ApexCarbs
import com.example.ui.theme.ApexCarbsBg
import com.example.ui.theme.ApexCyanAccent
import com.example.ui.theme.ApexDarkSurface
import com.example.ui.theme.ApexDarkSurfaceHighlight
import com.example.ui.theme.ApexFats
import com.example.ui.theme.ApexFatsBg
import com.example.ui.theme.ApexNeonLime
import com.example.ui.theme.ApexProtein
import com.example.ui.theme.ApexProteinBg
import com.example.ui.theme.ApexTextMuted
import com.example.ui.theme.ApexTextPrimary
import com.example.ui.theme.ApexTextSecondary
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
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(ApexNeonLime.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = ApexNeonLime,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Nuovo piano",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = ApexTextPrimary
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("close_new_plan_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Chiudi",
                            tint = ApexTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Crea un nuovo piano nutrizionale e imposta i tuoi target giornalieri.",
                    fontSize = 12.5.sp,
                    color = ApexTextSecondary,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Nome piano
                Text(
                    text = "NOME DEL PIANO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ApexCyanAccent,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("es. Definizione estiva", color = ApexTextMuted) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_plan_name_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = nutriTextFieldColors(),
                    textStyle = TextStyle(color = ApexTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Obiettivi giornalieri
                Text(
                    text = "OBIETTIVI GIORNALIERI",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ApexCyanAccent,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, ApexBorder, RoundedCornerShape(14.dp)),
                    color = ApexDarkSurfaceHighlight
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        PlanTargetRow(
                            icon = Icons.Default.LocalFireDepartment,
                            iconColor = ApexCalories,
                            bgColor = ApexCaloriesBg,
                            label = "Calorie",
                            suffix = "kcal",
                            value = calories,
                            testTag = "new_plan_calories_input",
                            onValueChange = { calories = it }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        PlanTargetRow(
                            icon = Icons.Default.Restaurant,
                            iconColor = ApexProtein,
                            bgColor = ApexProteinBg,
                            label = "Proteine",
                            suffix = "g",
                            value = protein,
                            testTag = "new_plan_protein_input",
                            onValueChange = { protein = it }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        PlanTargetRow(
                            icon = Icons.Default.Spa,
                            iconColor = ApexCarbs,
                            bgColor = ApexCarbsBg,
                            label = "Carboidrati",
                            suffix = "g",
                            value = carbs,
                            testTag = "new_plan_carbs_input",
                            onValueChange = { carbs = it }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        PlanTargetRow(
                            icon = Icons.Default.Opacity,
                            iconColor = ApexFats,
                            bgColor = ApexFatsBg,
                            label = "Grassi",
                            suffix = "g",
                            value = fat,
                            testTag = "new_plan_fat_input",
                            onValueChange = { fat = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Numero pasti
                Text(
                    text = "NUMERO DI PASTI AL GIORNO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ApexCyanAccent,
                    letterSpacing = 0.8.sp
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
                                .background(if (isSelected) ApexNeonLime else ApexDarkSurfaceHighlight)
                                .border(
                                    1.dp,
                                    if (isSelected) ApexNeonLime else ApexBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { mealsCount = count }
                                .testTag("new_plan_meals_$count"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = count.toString(),
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                color = if (isSelected) ApexBlack else ApexTextPrimary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

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
                        .height(48.dp)
                        .testTag("submit_new_plan_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ApexNeonLime,
                        contentColor = ApexBlack
                    )
                ) {
                    Text("Crea piano", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun EditPlanDialog(
    plan: PlanEntity,
    onDismiss: () -> Unit,
    onConfirm: (updated: PlanEntity) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(plan.name) }
    var calories by remember { mutableStateOf(plan.caloriesTarget.toString()) }
    var protein by remember { mutableStateOf(plan.proteinTarget.toString()) }
    var carbs by remember { mutableStateOf(plan.carbsTarget.toString()) }
    var fat by remember { mutableStateOf(plan.fatTarget.toString()) }
    var mealsCount by remember { mutableIntStateOf(plan.mealsCount) }

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
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(ApexNeonLime.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = ApexNeonLime,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Modifica piano",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = ApexTextPrimary
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("close_edit_plan_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Chiudi",
                            tint = ApexTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Aggiorna i parametri e i target nutrizionali di questo piano.",
                    fontSize = 12.5.sp,
                    color = ApexTextSecondary,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Nome piano
                Text(
                    text = "NOME DEL PIANO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ApexCyanAccent,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_plan_name_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = nutriTextFieldColors(),
                    textStyle = TextStyle(color = ApexTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Obiettivi giornalieri
                Text(
                    text = "OBIETTIVI GIORNALIERI",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ApexCyanAccent,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, ApexBorder, RoundedCornerShape(14.dp)),
                    color = ApexDarkSurfaceHighlight
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        PlanTargetRow(
                            icon = Icons.Default.LocalFireDepartment,
                            iconColor = ApexCalories,
                            bgColor = ApexCaloriesBg,
                            label = "Calorie",
                            suffix = "kcal",
                            value = calories,
                            testTag = "edit_plan_calories_input",
                            onValueChange = { calories = it }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        PlanTargetRow(
                            icon = Icons.Default.Restaurant,
                            iconColor = ApexProtein,
                            bgColor = ApexProteinBg,
                            label = "Proteine",
                            suffix = "g",
                            value = protein,
                            testTag = "edit_plan_protein_input",
                            onValueChange = { protein = it }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        PlanTargetRow(
                            icon = Icons.Default.Spa,
                            iconColor = ApexCarbs,
                            bgColor = ApexCarbsBg,
                            label = "Carboidrati",
                            suffix = "g",
                            value = carbs,
                            testTag = "edit_plan_carbs_input",
                            onValueChange = { carbs = it }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        PlanTargetRow(
                            icon = Icons.Default.Opacity,
                            iconColor = ApexFats,
                            bgColor = ApexFatsBg,
                            label = "Grassi",
                            suffix = "g",
                            value = fat,
                            testTag = "edit_plan_fat_input",
                            onValueChange = { fat = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Numero pasti
                Text(
                    text = "NUMERO DI PASTI AL GIORNO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ApexCyanAccent,
                    letterSpacing = 0.8.sp
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
                                .background(if (isSelected) ApexNeonLime else ApexDarkSurfaceHighlight)
                                .border(
                                    1.dp,
                                    if (isSelected) ApexNeonLime else ApexBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { mealsCount = count }
                                .testTag("edit_plan_meals_$count"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = count.toString(),
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                color = if (isSelected) ApexBlack else ApexTextPrimary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

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
                        .height(48.dp)
                        .testTag("submit_edit_plan_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ApexNeonLime,
                        contentColor = ApexBlack
                    )
                ) {
                    Text("Salva modifiche", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                if (onDelete != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    androidx.compose.material3.OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("delete_plan_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFF2C0D0E).copy(alpha = 0.4f),
                            contentColor = Color(0xFFEF4444)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Elimina piano",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF4444)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanTargetRow(
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
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = ApexTextPrimary
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .width(92.dp)
                    .height(44.dp)
                    .testTag(testTag),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = nutriTextFieldColors(),
                textStyle = TextStyle(
                    color = ApexTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.End
                )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = suffix,
                fontSize = 12.sp,
                color = ApexTextSecondary,
                modifier = Modifier.width(28.dp)
            )
        }
    }
}

