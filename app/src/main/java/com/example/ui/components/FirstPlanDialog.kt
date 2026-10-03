package com.example.ui.components

import androidx.compose.runtime.Composable

/**
 * First plan onboarding dialog presented on first startup when no plans exist.
 * Uses the exact same unified, intuitive layout as the New Plan dialog.
 */
@Composable
fun FirstPlanOnboardingDialog(
    onConfirm: (name: String, calories: Int, protein: Int, carbs: Int, fat: Int, mealsCount: Int) -> Unit
) {
    UnifiedPlanDialog(
        title = "Configura il tuo Piano",
        subtitle = "Imposta i tuoi obiettivi giornalieri e i pasti per iniziare ad usare l'app.",
        initialName = "Piano Principale",
        initialCalories = 2000,
        initialProtein = 140,
        initialCarbs = 220,
        initialFat = 60,
        initialMealsCount = 4,
        confirmButtonText = "Conferma Piano & Inizia",
        onDismiss = null,
        onConfirm = onConfirm
    )
}
