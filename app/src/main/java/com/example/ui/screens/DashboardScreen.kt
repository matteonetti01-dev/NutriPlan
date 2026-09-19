package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.PlanEntity
import com.example.ui.components.ApexHeader
import com.example.ui.components.EditPlanDialog
import com.example.ui.components.EmptyPlanCard
import com.example.ui.components.NewPlanDialog
import com.example.ui.theme.ApexBlack
import com.example.ui.theme.ApexBorder
import com.example.ui.theme.ApexCalories
import com.example.ui.theme.ApexCaloriesBg
import com.example.ui.theme.ApexCarbs
import com.example.ui.theme.ApexCarbsBg
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
import com.example.ui.viewmodel.NutritionViewModel

@Composable
fun DashboardScreen(
    viewModel: NutritionViewModel,
    onNavigateToPlan: () -> Unit
) {
    val activePlan by viewModel.activePlan.collectAsState()
    val savedPlans by viewModel.savedPlans.collectAsState()
    val todayCalories by viewModel.todayCalories.collectAsState()
    val todayProtein by viewModel.todayProtein.collectAsState()
    val todayCarbs by viewModel.todayCarbs.collectAsState()
    val todayFat by viewModel.todayFat.collectAsState()

    var showNewPlanDialog by remember { mutableStateOf(false) }
    var planToEdit by remember { mutableStateOf<PlanEntity?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ApexBlack)
            .padding(horizontal = 20.dp)
            .testTag("dashboard_screen")
    ) {
        item {
            Spacer(modifier = Modifier.height(14.dp))

            // APEX // AI Global Top Header
            ApexHeader()

            Spacer(modifier = Modifier.height(16.dp))

            // Header Row: "I tuoi piani" + "+ Nuovo"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "I tuoi piani",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = ApexTextPrimary,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Crea e gestisci più piani. Attiva quello che stai seguendo.",
                        fontSize = 12.5.sp,
                        color = ApexTextSecondary,
                        lineHeight = 17.sp
                    )
                }

                // "+ Nuovo" Button with neon lime border
                OutlinedButton(
                    onClick = { showNewPlanDialog = true },
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("new_plan_button"),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ApexNeonLime),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = ApexDarkSurfaceHighlight,
                        contentColor = ApexNeonLime
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp)
                ) {
                    Text("+ Nuovo", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = ApexNeonLime)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
        }

        if (activePlan == null && savedPlans.isEmpty()) {
            // Se tutti i piani sono stati eliminati: pagina senza nulla con pulsante nel centro "Crea nuovo piano"
            item {
                Spacer(modifier = Modifier.height(60.dp))
                EmptyPlanCard(
                    onCreatePlanClick = { showNewPlanDialog = true }
                )
                Spacer(modifier = Modifier.height(40.dp))
            }
        } else {
            // Active Plan Card (APEX // AI Mockup Phone 1 Style)
            if (activePlan != null) {
                item {
                    ActivePlanCard(
                        plan = activePlan!!,
                        todayCalories = todayCalories,
                        todayProtein = todayProtein,
                        todayCarbs = todayCarbs,
                        todayFat = todayFat,
                        onEditClick = { planToEdit = activePlan },
                        onOpenPlanClick = onNavigateToPlan
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            } else {
                item {
                    EmptyPlanCard(
                        onCreatePlanClick = { showNewPlanDialog = true }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // Section: Piani salvati
            item {
                Text(
                    text = "Piani salvati",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = ApexTextPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (savedPlans.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(ApexDarkSurface)
                            .border(1.dp, ApexBorder, RoundedCornerShape(16.dp))
                            .padding(22.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nessun altro piano salvato.\nPremi \"+ Nuovo\" per crearne uno.",
                            fontSize = 13.sp,
                            color = ApexTextMuted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                items(savedPlans, key = { it.id }) { savedPlan ->
                    SavedPlanCard(
                        plan = savedPlan,
                        onActivate = { viewModel.switchActivePlan(savedPlan.id) },
                        onEdit = { planToEdit = savedPlan },
                        onDelete = { viewModel.deletePlan(savedPlan) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // Dialogs
    if (showNewPlanDialog) {
        NewPlanDialog(
            onDismiss = { showNewPlanDialog = false },
            onConfirm = { name, cal, prot, c, f, meals ->
                viewModel.createPlan(name, cal, prot, c, f, meals, makeActive = (activePlan == null))
                showNewPlanDialog = false
            }
        )
    }

    planToEdit?.let { plan ->
        EditPlanDialog(
            plan = plan,
            onDismiss = { planToEdit = null },
            onConfirm = { updated ->
                viewModel.updatePlan(updated)
                planToEdit = null
            },
            onDelete = {
                viewModel.deletePlan(plan)
                planToEdit = null
            }
        )
    }
}

@Composable
private fun ActivePlanCard(
    plan: PlanEntity,
    todayCalories: Int,
    todayProtein: Int,
    todayCarbs: Int,
    todayFat: Int,
    onEditClick: () -> Unit,
    onOpenPlanClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, ApexBorder, RoundedCornerShape(20.dp))
            .testTag("active_plan_card"),
        color = ApexDarkSurface
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header: Plan Name & Edit Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = plan.name,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = ApexTextPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ApexNeonLime.copy(alpha = 0.15f))
                            .border(0.8.dp, ApexNeonLime.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "ATTIVO",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Black,
                            color = ApexNeonLime,
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(ApexDarkSurfaceHighlight)
                        .testTag("edit_active_plan_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Modifica piano",
                        tint = ApexNeonLime,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Calories Target Section
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp)),
                color = ApexDarkSurfaceHighlight,
                border = androidx.compose.foundation.BorderStroke(1.dp, ApexBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(ApexCaloriesBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = ApexCalories,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "OBIETTIVO CALORIE",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = ApexTextSecondary,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "${plan.caloriesTarget}",
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Black,
                                    color = ApexTextPrimary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "kcal",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ApexNeonLime,
                                    modifier = Modifier.padding(bottom = 3.dp)
                                )
                            }
                        }
                    }

                    if (todayCalories > 0) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Oggi",
                                fontSize = 10.sp,
                                color = ApexTextSecondary
                            )
                            Text(
                                text = "$todayCalories kcal",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = ApexTextPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3 Clean Macro Columns (Proteine, Carboidrati, Grassi)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Proteine
                CleanMacroBox(
                    label = "Proteine",
                    targetValue = "${plan.proteinTarget}g",
                    todayValue = if (todayProtein > 0) "$todayProtein g" else null,
                    icon = Icons.Default.Restaurant,
                    iconColor = ApexProtein,
                    bgColor = ApexProteinBg,
                    modifier = Modifier.weight(1f)
                )

                // Carboidrati
                CleanMacroBox(
                    label = "Carboidrati",
                    targetValue = "${plan.carbsTarget}g",
                    todayValue = if (todayCarbs > 0) "$todayCarbs g" else null,
                    icon = Icons.Default.Spa,
                    iconColor = ApexCarbs,
                    bgColor = ApexCarbsBg,
                    modifier = Modifier.weight(1f)
                )

                // Grassi
                CleanMacroBox(
                    label = "Grassi",
                    targetValue = "${plan.fatTarget}g",
                    todayValue = if (todayFat > 0) "$todayFat g" else null,
                    icon = Icons.Default.Opacity,
                    iconColor = ApexFats,
                    bgColor = ApexFatsBg,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Button: "Apri il piano" with neon lime border
            OutlinedButton(
                onClick = onOpenPlanClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("open_plan_button"),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, ApexNeonLime),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = ApexDarkSurfaceHighlight,
                    contentColor = ApexNeonLime
                )
            ) {
                Text(
                    text = "Apri il piano",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = ApexNeonLime
                )
            }
        }
    }
}

@Composable
private fun CleanMacroBox(
    label: String,
    targetValue: String,
    todayValue: String?,
    icon: ImageVector,
    iconColor: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, ApexBorder, RoundedCornerShape(12.dp)),
        color = ApexDarkSurfaceHighlight
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = ApexTextSecondary
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = targetValue,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = ApexTextPrimary
            )

            if (todayValue != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = todayValue,
                    fontSize = 10.sp,
                    color = ApexNeonLime
                )
            }
        }
    }
}

@Composable
private fun SavedPlanCard(
    plan: PlanEntity,
    onActivate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, ApexBorder, RoundedCornerShape(14.dp))
            .testTag("saved_plan_${plan.id}"),
        color = ApexDarkSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = plan.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = ApexTextPrimary
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "${plan.caloriesTarget} kcal • P ${plan.proteinTarget}g • C ${plan.carbsTarget}g • G ${plan.fatTarget}g • ${plan.mealsCount} pasti",
                    fontSize = 11.5.sp,
                    color = ApexTextSecondary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = onActivate,
                    modifier = Modifier.height(32.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF374151)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = ApexTextPrimary
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)
                ) {
                    Text("Attiva", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Modifica",
                        tint = ApexTextSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Elimina",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}
