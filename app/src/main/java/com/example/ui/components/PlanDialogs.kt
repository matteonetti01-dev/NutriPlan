package com.example.ui.components

import androidx.compose.runtime.Composable
import com.example.data.entity.PlanEntity

/**
 * Dialog for creating a new nutrition plan.
 * Uses the exact same unified, intuitive layout as "Configura il tuo Piano".
 */
@Composable
fun NewPlanDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, calories: Int, protein: Int, carbs: Int, fat: Int, mealsCount: Int) -> Unit
) {
    UnifiedPlanDialog(
        title = "Configura il tuo Piano",
        subtitle = "Crea un nuovo piano nutrizionale e imposta i tuoi target giornalieri.",
        initialName = "Nuovo Piano",
        initialCalories = 2000,
        initialProtein = 140,
        initialCarbs = 220,
        initialFat = 60,
        initialMealsCount = 4,
        confirmButtonText = "Crea Piano",
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}

/**
 * Dialog for editing an existing nutrition plan.
 * Uses the exact same unified, intuitive layout with existing values.
 */
@Composable
fun EditPlanDialog(
    plan: PlanEntity,
    onDismiss: () -> Unit,
    onConfirm: (updated: PlanEntity) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    UnifiedPlanDialog(
        title = "Modifica Piano",
        subtitle = "Aggiorna i parametri e i target nutrizionali di questo piano.",
        initialName = plan.name,
        initialCalories = plan.caloriesTarget,
        initialProtein = plan.proteinTarget,
        initialCarbs = plan.carbsTarget,
        initialFat = plan.fatTarget,
        initialMealsCount = plan.mealsCount,
        confirmButtonText = "Salva Modifiche",
        onDismiss = onDismiss,
        onDelete = onDelete,
        onConfirm = { name, calories, protein, carbs, fat, mealsCount ->
            onConfirm(
                plan.copy(
                    name = name,
                    caloriesTarget = calories,
                    proteinTarget = protein,
                    carbsTarget = carbs,
                    fatTarget = fat,
                    mealsCount = mealsCount
                )
            )
        }
    )
}
