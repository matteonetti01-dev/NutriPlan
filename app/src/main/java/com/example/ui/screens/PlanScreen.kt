package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.data.entity.Ingredient
import com.example.data.entity.MealAlternativeEntity
import com.example.data.entity.MealSlotEntity
import com.example.data.entity.PlanEntity
import com.example.ui.components.AlternativeDetailSheet
import com.example.ui.components.MealDialog
import com.example.ui.theme.NutriBadgeBg
import com.example.ui.theme.NutriBadgeText
import com.example.ui.theme.NutriBgLight
import com.example.ui.theme.NutriCardBg
import com.example.ui.theme.NutriCardBorder
import com.example.ui.theme.NutriDark
import com.example.ui.theme.NutriGreen
import com.example.ui.theme.NutriTextMuted
import com.example.ui.theme.NutriTextPrimary
import com.example.ui.theme.NutriTextSecondary
import com.example.ui.theme.nutriTextFieldColors
import com.example.ui.viewmodel.NutritionViewModel
import kotlinx.coroutines.launch

@Composable
fun PlanScreen(
    viewModel: NutritionViewModel
) {
    val activePlan by viewModel.activePlan.collectAsState()
    val slots by viewModel.activePlanSlots.collectAsState()
    val alternatives by viewModel.activePlanAlternatives.collectAsState()

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // State for expanded slots (default slot 1 is expanded)
    val expandedSlots = remember {
        mutableStateMapOf<Long, Boolean>()
    }

    var addingToSlot by remember { mutableStateOf<MealSlotEntity?>(null) }
    var slotToEdit by remember { mutableStateOf<MealSlotEntity?>(null) }
    var selectedAlternativeForDetail by remember { mutableStateOf<MealAlternativeEntity?>(null) }

    // Total calculations across all meal slots vs the plan targets
    val currentPlan = activePlan
    val (totalSlotsCal, totalSlotsProt, totalSlotsCarbs, totalSlotsFat) = remember(currentPlan, slots) {
        if (currentPlan == null) {
            listOf(0, 0, 0, 0)
        } else {
            val count = currentPlan.mealsCount.coerceAtLeast(1)
            val propCal = currentPlan.caloriesTarget / count
            val propProt = currentPlan.proteinTarget / count
            val propCarbs = currentPlan.carbsTarget / count
            val propFat = currentPlan.fatTarget / count

            val cal = slots.sumOf { it.customCalories ?: propCal }
            val prot = slots.sumOf { it.customProtein ?: propProt }
            val carbs = slots.sumOf { it.customCarbs ?: propCarbs }
            val fat = slots.sumOf { it.customFat ?: propFat }
            listOf(cal, prot, carbs, fat)
        }
    }

    val calDiff = currentPlan?.let { totalSlotsCal - it.caloriesTarget } ?: 0
    val protDiff = currentPlan?.let { totalSlotsProt - it.proteinTarget } ?: 0
    val carbsDiff = currentPlan?.let { totalSlotsCarbs - it.carbsTarget } ?: 0
    val fatDiff = currentPlan?.let { totalSlotsFat - it.fatTarget } ?: 0

    val hasMismatch = currentPlan != null && slots.isNotEmpty() &&
        (calDiff != 0 || protDiff != 0 || carbsDiff != 0 || fatDiff != 0)

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(NutriBgLight)
                .padding(horizontal = 20.dp)
                .testTag("plan_screen")
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))

                // Header Badge "✓ Piano attivo"
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(NutriBadgeBg)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = NutriGreen,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Piano attivo",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = NutriBadgeText
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Plan Name and Targets
                activePlan?.let { plan ->
                    Text(
                        text = plan.name,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = NutriTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${plan.caloriesTarget} kcal • P ${plan.proteinTarget}g • C ${plan.carbsTarget}g • G ${plan.fatTarget}g",
                        fontSize = 14.sp,
                        color = NutriTextSecondary
                    )
                } ?: run {
                    Text(
                        text = "Nessun piano attivo",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = NutriTextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Notification Banner if slot sum differs from total plan
            if (hasMismatch && currentPlan != null) {
                item {
                    MismatchNotificationBanner(
                        plan = currentPlan,
                        totalCal = totalSlotsCal,
                        totalProt = totalSlotsProt,
                        totalCarbs = totalSlotsCarbs,
                        totalFat = totalSlotsFat,
                        calDiff = calDiff,
                        protDiff = protDiff,
                        carbsDiff = carbsDiff,
                        fatDiff = fatDiff
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Meal Slots
            activePlan?.let { currentPlan ->
                items(slots, key = { it.id }) { slot ->
                    // By default first slot is expanded
                    val isExpanded = expandedSlots[slot.id] ?: (slot.orderIndex == 1)
                    val slotAlternatives = alternatives.filter { it.slotId == slot.id }

                    MealSlotCard(
                        slot = slot,
                        plan = currentPlan,
                        isExpanded = isExpanded,
                        alternatives = slotAlternatives,
                        onToggleExpand = {
                            expandedSlots[slot.id] = !isExpanded
                        },
                        onEditSlot = { slotToEdit = slot },
                        onAddAlternative = { addingToSlot = slot },
                        onSelectAlternative = { alt -> selectedAlternativeForDetail = alt },
                        onLogAlternative = { alt ->
                            viewModel.logAlternativeToToday(alt, slot.name)
                            scope.launch {
                                snackbarHostState.showSnackbar("Pasto registrato in Giornata fuori! ✓")
                            }
                        },
                        onDeleteAlternative = { alt ->
                            viewModel.deleteAlternative(alt)
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                }
            } ?: run {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .border(1.dp, NutriCardBorder, RoundedCornerShape(16.dp))
                            .padding(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nessun piano attivo al momento.\nVai nella Dashboard per attivare o creare il tuo primo piano.",
                            fontSize = 14.sp,
                            color = NutriTextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            item {
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

    // Dialog for adding alternative
    addingToSlot?.let { slot ->
        activePlan?.let { plan ->
            MealDialog(
                title = "Nuova alternativa",
                geminiService = viewModel.geminiService,
                onDismiss = { addingToSlot = null },
                onConfirm = { name, cal, prot, c, f, ings, notes, photoUri ->
                    if (ings.isNotEmpty()) {
                        viewModel.addAlternative(
                            slotId = slot.id,
                            planId = plan.id,
                            name = name,
                            ingredients = ings,
                            notes = notes,
                            photoUri = photoUri
                        )
                    } else {
                        viewModel.addDirectAlternative(
                            slotId = slot.id,
                            planId = plan.id,
                            name = name,
                            calories = cal,
                            protein = prot,
                            carbs = c,
                            fat = f,
                            notes = notes,
                            photoUri = photoUri
                        )
                    }
                    addingToSlot = null
                }
            )
        }
    }

    // Dialog for editing slot name and target macros/calories
    slotToEdit?.let { slot ->
        currentPlan?.let { plan ->
            EditSlotDialog(
                slot = slot,
                plan = plan,
                otherSlots = slots.filter { it.id != slot.id },
                onDismiss = { slotToEdit = null },
                onConfirm = { newName, newCal, newProt, newCarbs, newFat ->
                    viewModel.updateSlotTargets(slot, newName, newCal, newProt, newCarbs, newFat)
                    slotToEdit = null
                }
            )
        }
    }

    // Tendina / Bottom Sheet for inspecting, editing, and copying alternative
    selectedAlternativeForDetail?.let { alt ->
        val slotOfAlt = slots.find { it.id == alt.slotId }
        AlternativeDetailSheet(
            alternative = alt,
            currentSlot = slotOfAlt,
            allSlots = slots,
            geminiService = viewModel.geminiService,
            activePlan = activePlan,
            onDismiss = { selectedAlternativeForDetail = null },
            onSave = { updated ->
                viewModel.updateAlternative(updated)
                selectedAlternativeForDetail = null
                scope.launch {
                    snackbarHostState.showSnackbar("Alternativa aggiornata con successo! ✓")
                }
            },
            onCopyToSlot = { sourceAlt, targetSlot ->
                viewModel.copyAlternativeToSlot(sourceAlt, targetSlot.id)
                scope.launch {
                    snackbarHostState.showSnackbar("Alternativa copiata in ${targetSlot.name}! ✓")
                }
            },
            onDelete = { toDelete ->
                viewModel.deleteAlternative(toDelete)
                selectedAlternativeForDetail = null
                scope.launch {
                    snackbarHostState.showSnackbar("Alternativa eliminata.")
                }
            },
            onLog = { toLog ->
                val slotName = slotOfAlt?.name ?: "Pasto"
                viewModel.logAlternativeToToday(toLog, slotName)
                scope.launch {
                    snackbarHostState.showSnackbar("Pasto registrato in Giornata fuori! ✓")
                }
            }
        )
    }
}

@Composable
private fun MealSlotCard(
    slot: MealSlotEntity,
    plan: PlanEntity,
    isExpanded: Boolean,
    alternatives: List<MealAlternativeEntity>,
    onToggleExpand: () -> Unit,
    onEditSlot: () -> Unit,
    onAddAlternative: () -> Unit,
    onSelectAlternative: (MealAlternativeEntity) -> Unit,
    onLogAlternative: (MealAlternativeEntity) -> Unit,
    onDeleteAlternative: (MealAlternativeEntity) -> Unit
) {
    // Proportional target calculation if not custom
    val count = plan.mealsCount.coerceAtLeast(1)
    val propCal = plan.caloriesTarget / count
    val propProt = plan.proteinTarget / count
    val propCarbs = plan.carbsTarget / count
    val propFat = plan.fatTarget / count

    val targetCalories = slot.customCalories ?: propCal
    val targetProtein = slot.customProtein ?: propProt
    val targetCarbs = slot.customCarbs ?: propCarbs
    val targetFat = slot.customFat ?: propFat

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, NutriCardBorder, RoundedCornerShape(18.dp))
            .testTag("meal_slot_${slot.id}"),
        color = NutriCardBg
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: "Pasto X" and actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onToggleExpand() }
                ) {
                    Text(
                        text = "Pasto ${slot.orderIndex}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = NutriTextSecondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Edit slot button (modifies kcal, macro targets and name)
                    IconButton(
                        onClick = onEditSlot,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("edit_slot_${slot.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Modifica pasto e macro",
                            tint = NutriTextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    IconButton(
                        onClick = onToggleExpand,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Comprimi" else "Espandi",
                            tint = NutriTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Slot Name (e.g. "Colazione")
            Text(
                text = slot.name,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = NutriTextPrimary,
                modifier = Modifier.clickable { onToggleExpand() }
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Target Subtitle: "625 kcal • P 38g • C 55g • G 15g" - clickable to edit
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onEditSlot() }
                    .padding(vertical = 2.dp)
            ) {
                Text(
                    text = "$targetCalories kcal • P ${targetProtein}g • C ${targetCarbs}g • G ${targetFat}g",
                    fontSize = 13.sp,
                    color = NutriTextSecondary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = NutriTextMuted,
                    modifier = Modifier.size(11.dp)
                )
            }

            // Animated Expanded Section
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(14.dp))

                    // Alternatives list
                    alternatives.forEach { alt ->
                        AlternativeItemCard(
                            alternative = alt,
                            onClick = { onSelectAlternative(alt) },
                            onLog = { onLogAlternative(alt) },
                            onDelete = { onDeleteAlternative(alt) }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // "+ Aggiungi alternativa" Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF9FAFB))
                            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp))
                            .clickable { onAddAlternative() }
                            .padding(vertical = 12.dp)
                            .testTag("add_alternative_${slot.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = NutriTextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Aggiungi alternativa",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NutriTextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlternativeItemCard(
    alternative: MealAlternativeEntity,
    onClick: () -> Unit,
    onLog: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag("alternative_item_${alternative.id}"),
        color = Color(0xFFFAFAFA)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left content: ingredients list or name
            Column(
                modifier = Modifier
                    .weight(1f)
            ) {
                val ings = alternative.ingredients
                if (ings.isNotEmpty()) {
                    ings.forEach { ing ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "• ${ing.name} ${ing.quantity}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = NutriTextPrimary
                            )
                        }
                    }
                } else {
                    Text(
                        text = alternative.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NutriTextPrimary
                    )
                    if (alternative.notes.isNotBlank()) {
                        Text(
                            text = alternative.notes,
                            fontSize = 11.5.sp,
                            color = NutriTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Summary macros: e.g. "573 kcal • P 35g • C 62g • G 20g"
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${alternative.totalCalories} kcal • P ${alternative.totalProtein}g • C ${alternative.totalCarbs}g • G ${alternative.totalFat}g",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NutriTextSecondary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Modifica e dettagli",
                        tint = NutriTextMuted,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action icons on right: Checkmark (log to today), Delete
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onLog,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8F5E9))
                        .testTag("log_alternative_${alternative.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Segna come mangiato oggi",
                        tint = NutriGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

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
}

@Composable
private fun MismatchNotificationBanner(
    plan: PlanEntity,
    totalCal: Int,
    totalProt: Int,
    totalCarbs: Int,
    totalFat: Int,
    calDiff: Int,
    protDiff: Int,
    carbsDiff: Int,
    fatDiff: Int
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(14.dp))
            .testTag("mismatch_notification_banner"),
        color = Color(0xFFFFFBEB)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFEF3C7)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(17.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Discrepanza obiettivi pasti",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E)
                        )
                        Text(
                            text = "Informativa",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFB45309),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFEF3C7))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "La somma dei tuoi pasti differisce dall'obiettivo giornaliero (${plan.caloriesTarget} kcal). Puoi comunque continuare a usare l'app senza blocchi.",
                        fontSize = 11.5.sp,
                        color = Color(0xFFB45309),
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Badges showing differences
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MacroDiffBadge(
                    label = "Kcal",
                    current = totalCal,
                    target = plan.caloriesTarget,
                    diff = calDiff,
                    unit = "",
                    modifier = Modifier.weight(1f)
                )
                MacroDiffBadge(
                    label = "Prot",
                    current = totalProt,
                    target = plan.proteinTarget,
                    diff = protDiff,
                    unit = "g",
                    modifier = Modifier.weight(1f)
                )
                MacroDiffBadge(
                    label = "Carb",
                    current = totalCarbs,
                    target = plan.carbsTarget,
                    diff = carbsDiff,
                    unit = "g",
                    modifier = Modifier.weight(1f)
                )
                MacroDiffBadge(
                    label = "Gras",
                    current = totalFat,
                    target = plan.fatTarget,
                    diff = fatDiff,
                    unit = "g",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MacroDiffBadge(
    label: String,
    current: Int,
    target: Int,
    diff: Int,
    unit: String,
    modifier: Modifier = Modifier
) {
    val isExact = diff == 0
    val diffText = when {
        diff > 0 -> "+$diff$unit"
        diff < 0 -> "$diff$unit"
        else -> "OK"
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isExact) Color(0xFFECFDF5) else Color(0xFFFEF3C7))
            .border(
                1.dp,
                if (isExact) Color(0xFFA7F3D0) else Color(0xFFFDE68A),
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 4.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isExact) Color(0xFF047857) else Color(0xFF92400E)
            )
            Text(
                text = diffText,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isExact) Color(0xFF047857) else Color(0xFFB45309)
            )
            Text(
                text = "$current/$target",
                fontSize = 9.sp,
                color = if (isExact) Color(0xFF059669) else Color(0xFFB45309)
            )
        }
    }
}

@Composable
private fun EditSlotDialog(
    slot: MealSlotEntity,
    plan: PlanEntity,
    otherSlots: List<MealSlotEntity>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, calories: Int, protein: Int, carbs: Int, fat: Int) -> Unit
) {
    val count = plan.mealsCount.coerceAtLeast(1)
    val propCal = plan.caloriesTarget / count
    val propProt = plan.proteinTarget / count
    val propCarbs = plan.carbsTarget / count
    val propFat = plan.fatTarget / count

    var name by remember { mutableStateOf(slot.name) }
    var calories by remember { mutableStateOf((slot.customCalories ?: propCal).toString()) }
    var protein by remember { mutableStateOf((slot.customProtein ?: propProt).toString()) }
    var carbs by remember { mutableStateOf((slot.customCarbs ?: propCarbs).toString()) }
    var fat by remember { mutableStateOf((slot.customFat ?: propFat).toString()) }

    val otherCalSum = otherSlots.sumOf { it.customCalories ?: propCal }
    val otherProtSum = otherSlots.sumOf { it.customProtein ?: propProt }
    val otherCarbsSum = otherSlots.sumOf { it.customCarbs ?: propCarbs }
    val otherFatSum = otherSlots.sumOf { it.customFat ?: propFat }

    val currentSlotCal = calories.toIntOrNull() ?: 0
    val currentSlotProt = protein.toIntOrNull() ?: 0
    val currentSlotCarbs = carbs.toIntOrNull() ?: 0
    val currentSlotFat = fat.toIntOrNull() ?: 0

    val totalCalPreview = otherCalSum + currentSlotCal
    val totalProtPreview = otherProtSum + currentSlotProt
    val totalCarbsPreview = otherCarbsSum + currentSlotCarbs
    val totalFatPreview = otherFatSum + currentSlotFat

    val calDiff = totalCalPreview - plan.caloriesTarget
    val protDiff = totalProtPreview - plan.proteinTarget
    val carbsDiff = totalCarbsPreview - plan.carbsTarget
    val fatDiff = totalFatPreview - plan.fatTarget

    val hasMismatch = calDiff != 0 || protDiff != 0 || carbsDiff != 0 || fatDiff != 0

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
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Modifica Pasto ${slot.orderIndex}",
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

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Personalizza nome, calorie e macronutrienti target per questo pasto.",
                    fontSize = 12.sp,
                    color = NutriTextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Nome pasto
                Text(
                    text = "Nome del pasto",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NutriTextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = nutriTextFieldColors(),
                    textStyle = TextStyle(color = NutriTextPrimary, fontSize = 14.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_slot_name_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Kcal & Proteine row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Calorie (kcal)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NutriTextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = calories,
                            onValueChange = { calories = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = nutriTextFieldColors(),
                            textStyle = TextStyle(color = NutriTextPrimary, fontSize = 14.sp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_slot_calories_input")
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Proteine (g)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NutriTextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = protein,
                            onValueChange = { protein = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = nutriTextFieldColors(),
                            textStyle = TextStyle(color = NutriTextPrimary, fontSize = 14.sp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_slot_protein_input")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Carboidrati & Grassi row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Carboidrati (g)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NutriTextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = carbs,
                            onValueChange = { carbs = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = nutriTextFieldColors(),
                            textStyle = TextStyle(color = NutriTextPrimary, fontSize = 14.sp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_slot_carbs_input")
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Grassi (g)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NutriTextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = fat,
                            onValueChange = { fat = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = nutriTextFieldColors(),
                            textStyle = TextStyle(color = NutriTextPrimary, fontSize = 14.sp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_slot_fat_input")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Live feedback box
                if (hasMismatch) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFFFFBEB))
                            .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Somma pasti vs totale piano",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF92400E)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Kcal totali con questa modifica: $totalCalPreview / ${plan.caloriesTarget} (${if (calDiff > 0) "+$calDiff" else "$calDiff"} kcal)\nPuoi comunque salvare e continuare a usare l'app.",
                                fontSize = 11.5.sp,
                                color = Color(0xFFB45309),
                                lineHeight = 16.sp
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF0FDF4))
                            .border(1.dp, Color(0xFFBBF7D0), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Perfetto! La somma dei pasti coincide con l'obiettivo (${plan.caloriesTarget} kcal).",
                                fontSize = 11.5.sp,
                                color = Color(0xFF15803D)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Annulla", color = NutriTextSecondary)
                    }

                    Button(
                        onClick = {
                            val finalName = if (name.isNotBlank()) name else slot.name
                            onConfirm(finalName, currentSlotCal, currentSlotProt, currentSlotCarbs, currentSlotFat)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("submit_edit_slot_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NutriDark,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Salva", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
