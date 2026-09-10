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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.PlanEntity
import com.example.ui.components.EditPlanDialog
import com.example.ui.components.NewPlanDialog
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
import com.example.ui.viewmodel.NutritionViewModel

@Composable
fun DashboardScreen(
    viewModel: NutritionViewModel,
    onNavigateToPlan: () -> Unit
) {
    val activePlan by viewModel.activePlan.collectAsState()
    val savedPlans by viewModel.savedPlans.collectAsState()
    val todayCalories by viewModel.todayCalories.collectAsState()

    var showNewPlanDialog by remember { mutableStateOf(false) }
    var planToEdit by remember { mutableStateOf<PlanEntity?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NutriBgLight)
            .padding(horizontal = 20.dp)
            .testTag("dashboard_screen")
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))

            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "I tuoi piani",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = NutriTextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Crea e gestisci più piani. Attiva quello che stai seguendo.",
                        fontSize = 13.sp,
                        color = NutriTextSecondary,
                        lineHeight = 18.sp
                    )
                }

                // "+ Nuovo" Button
                Button(
                    onClick = { showNewPlanDialog = true },
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("new_plan_button"),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NutriDark,
                        contentColor = Color.White
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Nuovo", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Active Plan Card
        if (activePlan != null) {
            item {
                ActivePlanCard(
                    plan = activePlan!!,
                    todayCalories = todayCalories,
                    onEditClick = { planToEdit = activePlan },
                    onOpenPlanClick = onNavigateToPlan
                )
                Spacer(modifier = Modifier.height(28.dp))
            }
        } else {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                        .border(1.dp, NutriCardBorder, RoundedCornerShape(20.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Nessun piano attivo",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = NutriTextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Crea il tuo primo piano per iniziare a tracciare la tua nutrizione.",
                            fontSize = 13.sp,
                            color = NutriTextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { showNewPlanDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NutriDark,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Crea piano", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(28.dp))
            }
        }

        // Section: Piani salvati
        item {
            Text(
                text = "Piani salvati",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = NutriTextPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (savedPlans.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(NutriCardBg)
                        .border(1.dp, NutriCardBorder, RoundedCornerShape(16.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nessun altro piano salvato.\nPremi \"+ Nuovo\" per crearne uno.",
                        fontSize = 13.sp,
                        color = NutriTextMuted,
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
            }
        )
    }
}

@Composable
private fun ActivePlanCard(
    plan: PlanEntity,
    todayCalories: Int,
    onEditClick: () -> Unit,
    onOpenPlanClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, NutriCardBorder, RoundedCornerShape(20.dp))
            .testTag("active_plan_card"),
        color = NutriCardBg,
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Top Row: Badge "✓ In uso ora" & Edit button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                        text = "In uso ora",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = NutriBadgeText
                    )
                }

                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("edit_active_plan_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Modifica piano",
                        tint = NutriTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Plan Name
            Text(
                text = plan.name,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = NutriTextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 4 Target Macro Cards in a Row (Flame, Steak, Wheat, Drop)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MacroPillCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.LocalFireDepartment,
                    iconColor = NutriCalories,
                    bgColor = NutriCaloriesBg,
                    value = plan.caloriesTarget.toString(),
                    label = "kcal"
                )
                MacroPillCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Restaurant,
                    iconColor = NutriProtein,
                    bgColor = NutriProteinBg,
                    value = plan.proteinTarget.toString(),
                    label = "prot"
                )
                MacroPillCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Spa,
                    iconColor = NutriCarbs,
                    bgColor = NutriCarbsBg,
                    value = plan.carbsTarget.toString(),
                    label = "carbo"
                )
                MacroPillCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Opacity,
                    iconColor = NutriFats,
                    bgColor = NutriFatsBg,
                    value = plan.fatTarget.toString(),
                    label = "grassi"
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Progress Bar "Oggi"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Oggi",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NutriTextPrimary
                )
                Text(
                    text = "$todayCalories / ${plan.caloriesTarget} kcal",
                    fontSize = 13.sp,
                    color = NutriTextSecondary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            val progressFraction = if (plan.caloriesTarget > 0) {
                (todayCalories.toFloat() / plan.caloriesTarget.toFloat()).coerceIn(0f, 1f)
            } else 0f

            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = NutriDark,
                trackColor = Color(0xFFE5E7EB)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Button: "Apri il piano >"
            Button(
                onClick = onOpenPlanClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("open_plan_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NutriDark,
                    contentColor = Color.White
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Apri il piano",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MacroPillCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconColor: Color,
    bgColor: Color,
    value: String,
    label: String
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF9FAFB))
            .border(1.dp, Color(0xFFF0F2F5), RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = NutriTextPrimary
            )
            Text(
                text = label,
                fontSize = 10.5.sp,
                color = NutriTextSecondary
            )
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
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, NutriCardBorder, RoundedCornerShape(16.dp))
            .testTag("saved_plan_${plan.id}"),
        color = NutriCardBg
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = plan.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = NutriTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${plan.caloriesTarget} kcal • P ${plan.proteinTarget}g • C ${plan.carbsTarget}g • G ${plan.fatTarget}g • ${plan.mealsCount} pasti",
                    fontSize = 12.sp,
                    color = NutriTextSecondary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                // "Attiva" outlined button
                OutlinedButton(
                    onClick = onActivate,
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("activate_plan_${plan.id}"),
                    shape = RoundedCornerShape(8.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFE5E7EB))
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)
                ) {
                    Text("Attiva", fontSize = 12.sp, color = NutriTextPrimary, fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Modifica",
                        tint = NutriTextSecondary,
                        modifier = Modifier.size(17.dp)
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
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}
