package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
import com.example.ui.theme.ApexCyanAccent
import com.example.ui.theme.ApexDarkSurface
import com.example.ui.theme.ApexDarkSurfaceHighlight
import com.example.ui.theme.ApexFats
import com.example.ui.theme.ApexNeonLime
import com.example.ui.theme.ApexNeonLimeDim
import com.example.ui.theme.ApexProtein
import com.example.ui.theme.ApexTextMuted
import com.example.ui.theme.ApexTextPrimary
import com.example.ui.theme.ApexTextSecondary
import com.example.ui.theme.nutriTextFieldColors
import kotlin.math.roundToInt

/**
 * Unified, simple, and intuitive Plan Configuration Dialog.
 * Used identically for "Configura il tuo Piano" (first setup) and "Nuovo Piano" (in-app creation),
 * with only the essential fields: Name, Daily Calories, Macros (P, C, F), and Meals count.
 */
@Composable
fun UnifiedPlanDialog(
    title: String,
    subtitle: String = "Imposta i tuoi obiettivi giornalieri e i pasti.",
    initialName: String = "Piano Principale",
    initialCalories: Int = 2000,
    initialProtein: Int = 140,
    initialCarbs: Int = 220,
    initialFat: Int = 60,
    initialMealsCount: Int = 4,
    confirmButtonText: String = "Conferma e Salva",
    onDismiss: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onConfirm: (name: String, calories: Int, protein: Int, carbs: Int, fat: Int, mealsCount: Int) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var caloriesStr by remember { mutableStateOf(initialCalories.toString()) }
    var proteinStr by remember { mutableStateOf(initialProtein.toString()) }
    var carbsStr by remember { mutableStateOf(initialCarbs.toString()) }
    var fatStr by remember { mutableStateOf(initialFat.toString()) }
    var mealsCount by remember { mutableIntStateOf(initialMealsCount) }

    val parsedCal = caloriesStr.toIntOrNull() ?: 0
    val parsedP = proteinStr.toIntOrNull() ?: 0
    val parsedC = carbsStr.toIntOrNull() ?: 0
    val parsedF = fatStr.toIntOrNull() ?: 0

    // Calorie calcolate dai macronutrienti inseriti
    val sumKcalFromMacros = (parsedP * 4) + (parsedC * 4) + (parsedF * 9)
    val totalMacroEnergy = sumKcalFromMacros.coerceAtLeast(1)
    val pPct = ((parsedP * 4 * 100.0) / totalMacroEnergy).roundToInt()
    val cPct = ((parsedC * 4 * 100.0) / totalMacroEnergy).roundToInt()
    val fPct = (100 - pPct - cPct).coerceIn(0, 100)

    Dialog(
        onDismissRequest = { onDismiss?.invoke() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = onDismiss != null,
            dismissOnClickOutside = onDismiss != null
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, ApexBorder, RoundedCornerShape(24.dp))
                .testTag("plan_config_dialog"),
            color = ApexDarkSurface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // --- HEADER ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ApexNeonLimeDim)
                                .border(1.2.dp, ApexNeonLime, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = ApexNeonLime,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = title,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = ApexTextPrimary,
                                letterSpacing = (-0.3).sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = subtitle,
                                fontSize = 11.5.sp,
                                color = ApexTextSecondary,
                                lineHeight = 15.sp
                            )
                        }
                    }
                    if (onDismiss != null) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("close_plan_dialog")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Chiudi",
                                tint = ApexTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- 1. NOME DEL PIANO ---
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Assignment,
                        contentDescription = null,
                        tint = ApexCyanAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "NOME DEL PIANO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ApexCyanAccent,
                        letterSpacing = 0.6.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("es. Piano Principale, Definizione, Massa", color = ApexTextMuted, fontSize = 13.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("plan_name_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = nutriTextFieldColors(),
                    textStyle = TextStyle(color = ApexTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // --- 2. OBIETTIVO CALORICO (KCAL) ---
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = ApexCalories,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "CALORIE GIORNALIERE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ApexCalories,
                        letterSpacing = 0.6.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.2.dp, ApexCalories.copy(alpha = 0.45f), RoundedCornerShape(14.dp)),
                    color = ApexDarkSurfaceHighlight
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Target Energetico",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = ApexTextPrimary
                            )
                            Text(
                                text = "Obiettivo giornaliero",
                                fontSize = 11.sp,
                                color = ApexTextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Dedicated full-width keyboard input box
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ApexBlack)
                                .border(1.5.dp, ApexCalories.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                BasicTextField(
                                    value = caloriesStr,
                                    onValueChange = { newText ->
                                        val filtered = newText.filter { it.isDigit() }
                                        if (filtered.length <= 5) {
                                            caloriesStr = filtered
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("plan_calories_input"),
                                    textStyle = TextStyle(
                                        color = ApexCalories,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        textAlign = TextAlign.Center
                                    ),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Next
                                    ),
                                    singleLine = true,
                                    cursorBrush = SolidColor(ApexCalories),
                                    decorationBox = { innerTextField ->
                                        Box(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (caloriesStr.isEmpty()) {
                                                Text(
                                                    text = "es. 2000",
                                                    color = ApexTextMuted,
                                                    fontSize = 20.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                            innerTextField()
                                        }
                                    }
                                )
                            }
                            Text(
                                text = "kcal",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = ApexCalories,
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- 3. MACRONUTRIENTI (TARGET IN GRAMMI) ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Scale,
                            contentDescription = null,
                            tint = ApexCyanAccent,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "MACRONUTRIENTI (g)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ApexCyanAccent,
                            letterSpacing = 0.6.sp
                        )
                    }
                    Text(
                        text = "Stima: ~$sumKcalFromMacros kcal",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ApexTextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 3 Macro Input Cards in a clean row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Proteine
                    MacroInputCard(
                        modifier = Modifier.weight(1f),
                        label = "Proteine",
                        value = proteinStr,
                        onValueChange = { proteinStr = it },
                        color = ApexProtein,
                        testTag = "plan_protein_input"
                    )
                    // Carboidrati
                    MacroInputCard(
                        modifier = Modifier.weight(1f),
                        label = "Carboidrati",
                        value = carbsStr,
                        onValueChange = { carbsStr = it },
                        color = ApexCarbs,
                        testTag = "plan_carbs_input"
                    )
                    // Grassi
                    MacroInputCard(
                        modifier = Modifier.weight(1f),
                        label = "Grassi",
                        value = fatStr,
                        onValueChange = { fatStr = it },
                        color = ApexFats,
                        testTag = "plan_fat_input"
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Visual Macro Distribution Bar
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ApexDarkSurfaceHighlight)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    color = ApexDarkSurfaceHighlight
                ) {
                    Column {
                        // Colored progress segments
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(ApexBlack)
                        ) {
                            if (pPct > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight(pPct.toFloat())
                                        .height(6.dp)
                                        .background(ApexProtein)
                                )
                            }
                            if (cPct > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight(cPct.toFloat())
                                        .height(6.dp)
                                        .background(ApexCarbs)
                                )
                            }
                            if (fPct > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight(fPct.toFloat())
                                        .height(6.dp)
                                        .background(ApexFats)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Prot: $pPct%", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ApexProtein)
                            Text("Carb: $cPct%", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ApexCarbs)
                            Text("Grassi: $fPct%", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ApexFats)
                        }
                    }
                }



                Spacer(modifier = Modifier.height(16.dp))

                // --- 4. NUMERO DI PASTI AL GIORNO ---
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = ApexCyanAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "NUMERO DI PASTI AL GIORNO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ApexCyanAccent,
                        letterSpacing = 0.6.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        Triple(3, "3 pasti", "Colaz, Pranzo, Cena"),
                        Triple(4, "4 pasti", "+ Spuntino"),
                        Triple(5, "5 pasti", "+ 2 Spuntini"),
                        Triple(6, "6 pasti", "Frequenti")
                    ).forEach { (count, label, sub) ->
                        val isSelected = mealsCount == count
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) ApexNeonLime else ApexBorder,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { mealsCount = count }
                                .testTag("plan_meals_$count"),
                            color = if (isSelected) ApexNeonLimeDim.copy(alpha = 0.35f) else ApexDarkSurfaceHighlight
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isSelected) ApexNeonLime else ApexTextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = sub,
                                    fontSize = 9.sp,
                                    color = if (isSelected) ApexTextPrimary else ApexTextMuted,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 11.sp,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // --- 5. PULSANTE DI CONFERMA ---
                val isFormValid = parsedCal > 0 && parsedP > 0 && parsedC > 0 && parsedF > 0

                Button(
                    onClick = {
                        val finalName = if (name.isNotBlank()) name.trim() else initialName
                        val finalCal = parsedCal.coerceAtLeast(500)
                        val finalP = parsedP.coerceAtLeast(10)
                        val finalC = parsedC.coerceAtLeast(10)
                        val finalF = parsedF.coerceAtLeast(5)
                        onConfirm(finalName, finalCal, finalP, finalC, finalF, mealsCount)
                    },
                    enabled = isFormValid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("submit_plan_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ApexNeonLime,
                        contentColor = ApexBlack,
                        disabledContainerColor = ApexDarkSurfaceHighlight,
                        disabledContentColor = ApexTextMuted
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = if (isFormValid) ApexBlack else ApexTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = confirmButtonText,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isFormValid) ApexBlack else ApexTextMuted
                    )
                }

                // Opzione Elimina se presente (modalità modifica piano)
                if (onDelete != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("delete_plan_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFEF4444)
                        ),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Elimina questo piano",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFEF4444)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MacroInputCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    color: Color,
    testTag: String
) {
    val grams = value.toIntOrNull() ?: 0
    val kcalPerGram = if (label.equals("Grassi", ignoreCase = true)) 9 else 4
    val calculatedKcal = grams * kcalPerGram

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.2.dp, color.copy(alpha = 0.45f), RoundedCornerShape(12.dp)),
        color = ApexDarkSurfaceHighlight
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Label with colored indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = label,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Dedicated High-Contrast Numeric Input Box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(ApexBlack)
                    .border(1.2.dp, color.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    BasicTextField(
                        value = value,
                        onValueChange = { newText ->
                            val filtered = newText.filter { it.isDigit() }
                            if (filtered.length <= 4) {
                                onValueChange(filtered)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(testTag),
                        textStyle = TextStyle(
                            color = ApexTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(color),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (value.isEmpty()) {
                                    Text(
                                        text = "0",
                                        color = ApexTextMuted,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }
                Text(
                    text = "g",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Live kcal calculation from this macro
            Text(
                text = "$calculatedKcal kcal",
                fontSize = 11.sp,
                color = ApexTextSecondary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
