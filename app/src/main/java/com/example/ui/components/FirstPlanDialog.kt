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
import androidx.compose.material.icons.filled.AutoAwesome
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
import com.example.ui.theme.ApexNeonLimeDim
import com.example.ui.theme.ApexProtein
import com.example.ui.theme.ApexProteinBg
import com.example.ui.theme.ApexTextMuted
import com.example.ui.theme.ApexTextPrimary
import com.example.ui.theme.ApexTextSecondary
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
                .clip(RoundedCornerShape(22.dp))
                .border(1.dp, ApexBorder, RoundedCornerShape(22.dp))
                .testTag("first_plan_dialog"),
            color = ApexDarkSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp)
            ) {
                // APEX Brand Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(ApexNeonLimeDim)
                            .border(1.2.dp, ApexNeonLime, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = ApexNeonLime,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF142918))
                            .border(1.dp, Color(0xFF22542B), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "APEX // ONBOARDING",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ApexNeonLime,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Configura il tuo Piano",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = ApexTextPrimary,
                    letterSpacing = (-0.3).sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Imposta i tuoi obiettivi giornalieri e la suddivisione dei pasti per calibrare il motore nutrizionale APEX.",
                    fontSize = 12.5.sp,
                    color = ApexTextSecondary,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

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
                    placeholder = { Text("es. Piano principale", color = ApexTextMuted) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("first_plan_name_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = nutriTextFieldColors(),
                    textStyle = TextStyle(color = ApexTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                )

                Spacer(modifier = Modifier.height(16.dp))

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
                        // Calorie Target
                        OnboardingTargetRow(
                            icon = Icons.Default.LocalFireDepartment,
                            iconColor = ApexCalories,
                            bgColor = ApexCaloriesBg,
                            label = "Calorie",
                            suffix = "kcal",
                            value = calories,
                            testTag = "first_plan_calories_input",
                            onValueChange = { calories = it }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Proteine
                        OnboardingTargetRow(
                            icon = Icons.Default.Restaurant,
                            iconColor = ApexProtein,
                            bgColor = ApexProteinBg,
                            label = "Proteine",
                            suffix = "g",
                            value = protein,
                            testTag = "first_plan_protein_input",
                            onValueChange = { protein = it }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Carbo
                        OnboardingTargetRow(
                            icon = Icons.Default.Spa,
                            iconColor = ApexCarbs,
                            bgColor = ApexCarbsBg,
                            label = "Carboidrati",
                            suffix = "g",
                            value = carbs,
                            testTag = "first_plan_carbs_input",
                            onValueChange = { carbs = it }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Grassi
                        OnboardingTargetRow(
                            icon = Icons.Default.Opacity,
                            iconColor = ApexFats,
                            bgColor = ApexFatsBg,
                            label = "Grassi",
                            suffix = "g",
                            value = fat,
                            testTag = "first_plan_fat_input",
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
                                .testTag("first_plan_meals_$count"),
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
                        .height(48.dp)
                        .testTag("submit_first_plan_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ApexNeonLime,
                        contentColor = ApexBlack
                    )
                ) {
                    Text(
                        text = "Attiva Piano & Inizia",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = ApexBlack
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingTargetRow(
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
                    .width(88.dp)
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
