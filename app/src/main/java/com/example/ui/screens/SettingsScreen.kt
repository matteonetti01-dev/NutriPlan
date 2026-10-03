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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ai.GeminiNutritionService
import com.example.data.entity.PlanEntity
import com.example.ui.components.ApexHeader
import com.example.ui.theme.ApexBlack
import com.example.ui.theme.ApexBorder
import com.example.ui.theme.ApexCyanAccent
import com.example.ui.theme.ApexDarkSurface
import com.example.ui.theme.ApexDarkSurfaceHighlight
import com.example.ui.theme.ApexNeonLime
import com.example.ui.theme.ApexTextMuted
import com.example.ui.theme.ApexTextPrimary
import com.example.ui.theme.ApexTextSecondary
import com.example.ui.theme.NutriCalories
import com.example.ui.theme.NutriCarbs
import com.example.ui.theme.NutriFats
import com.example.ui.theme.NutriProtein
import com.example.ui.theme.nutriTextFieldColors
import com.example.ui.viewmodel.NutritionViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: NutritionViewModel
) {
    val activePlan by viewModel.activePlan.collectAsState()
    val allPlans by viewModel.allPlans.collectAsState()
    val todayMeals by viewModel.todayLoggedMeals.collectAsState()

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Dialog visibility states for functional settings buttons
    var showOnboardingReviewDialog by remember { mutableStateOf(false) }
    var showFatSecretDialog by remember { mutableStateOf(false) }
    var showGeminiDialog by remember { mutableStateOf(false) }
    var showPlansDialog by remember { mutableStateOf(false) }
    var showDatabaseDialog by remember { mutableStateOf(false) }
    var showGuideDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    // Inline Gemini API Key & Connection Test state on Settings screen
    var inlineGeminiKey by remember { mutableStateOf(viewModel.geminiService.customApiKey) }
    var inlineIsKeyVisible by remember { mutableStateOf(false) }
    var isInlineTestingGemini by remember { mutableStateOf(false) }
    var inlineGeminiTestSuccess by remember { mutableStateOf<Boolean?>(null) }
    var inlineGeminiTestMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(viewModel.geminiService.customApiKey) {
        inlineGeminiKey = viewModel.geminiService.customApiKey
    }

    // Subpage navigation inside Settings (e.g. "spesa")
    var currentSubPage by remember { mutableStateOf<String?>(null) }

    if (currentSubPage == "spesa") {
        SpesaScreen(
            viewModel = viewModel,
            onBack = { currentSubPage = null }
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ApexBlack)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .testTag("settings_screen")
        ) {
            item {
                Spacer(modifier = Modifier.height(14.dp))

                // APEX // AI Global Top Header
                ApexHeader()

                Spacer(modifier = Modifier.height(16.dp))

                // Profile / User status card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, ApexBorder, RoundedCornerShape(16.dp)),
                    color = ApexDarkSurface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(ApexDarkSurfaceHighlight)
                                .border(1.5.dp, ApexNeonLime, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = ApexNeonLime,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "ATLETA APEX",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = ApexTextPrimary,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF142918))
                                        .border(1.dp, Color(0xFF22542B), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "ONLINE",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ApexNeonLime
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Piano: ${activePlan?.name ?: "Nessun piano"}",
                                fontSize = 12.5.sp,
                                color = ApexTextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Section: APEX SYSTEM & CONFIGURATION
                Text(
                    text = "STRUMENTI & CONFIGURAZIONE APEX",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ApexCyanAccent,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 0. Spesa (Shopping List from Plan with AI)
                SettingsOptionRow(
                    icon = Icons.Default.ShoppingCart,
                    iconTint = ApexNeonLime,
                    title = "Spesa",
                    subtitle = "Lista della spesa con AI • Cibi dal piano selezionato con spunte",
                    testTag = "settings_spesa_button",
                    onClick = { currentSubPage = "spesa" }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 1. Motore Gemini AI & Inline Connection Test Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(ApexDarkSurface)
                        .border(1.dp, ApexBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column {
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
                                        .background(Color(0xFF1F283E)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = ApexNeonLime,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Motore Gemini AI & Visione",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ApexTextPrimary
                                    )
                                    Text(
                                        text = "${viewModel.geminiService.selectedModel} • ${if (viewModel.geminiService.isUsingCustomKey) "Chiave personale attiva" else "Chiave predefinita"}",
                                        fontSize = 11.sp,
                                        color = if (viewModel.geminiService.isUsingCustomKey) ApexNeonLime else ApexCyanAccent
                                    )
                                }
                            }

                            // Button to open full options dialog
                            IconButton(
                                onClick = { showGeminiDialog = true },
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("settings_gemini_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Configurazione avanzata",
                                    tint = ApexTextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // API Key input
                        OutlinedTextField(
                            value = inlineGeminiKey,
                            onValueChange = {
                                inlineGeminiKey = it
                                inlineGeminiTestMessage = null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_inline_gemini_key_input"),
                            label = { Text("API Key Gemini (Google AI Studio)") },
                            placeholder = { Text("Incolla chiave API (AIzaSy...)") },
                            visualTransformation = if (inlineIsKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { inlineIsKeyVisible = !inlineIsKeyVisible }) {
                                        Icon(
                                            imageVector = if (inlineIsKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Mostra/Nascondi",
                                            tint = ApexTextMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    if (inlineGeminiKey.isNotBlank()) {
                                        IconButton(onClick = { inlineGeminiKey = "" }) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Cancella",
                                                tint = ApexTextMuted,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            },
                            colors = nutriTextFieldColors()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Action Buttons: Save Key & Test Connection
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Save button
                            OutlinedButton(
                                onClick = {
                                    viewModel.geminiService.customApiKey = inlineGeminiKey.trim()
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Chiave Gemini salvata con successo!")
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ApexNeonLime)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Salva", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            // Test Connection button
                            Button(
                                onClick = {
                                    isInlineTestingGemini = true
                                    inlineGeminiTestMessage = null
                                    scope.launch {
                                        val (success, msg) = viewModel.geminiService.testConnection(
                                            keyToTest = inlineGeminiKey,
                                            modelToTest = viewModel.geminiService.selectedModel
                                        )
                                        isInlineTestingGemini = false
                                        inlineGeminiTestSuccess = success
                                        inlineGeminiTestMessage = msg
                                    }
                                },
                                enabled = !isInlineTestingGemini,
                                modifier = Modifier
                                    .weight(1.3f)
                                    .testTag("settings_test_gemini_connection_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ApexNeonLime,
                                    contentColor = ApexBlack
                                )
                            ) {
                                if (isInlineTestingGemini) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = ApexBlack,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Test in corso...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Test Connessione", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Test Result Banner
                        if (inlineGeminiTestMessage != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            val isOk = inlineGeminiTestSuccess == true
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isOk) Color(0xFF102616) else Color(0xFF2E1515))
                                    .border(1.dp, if (isOk) Color(0xFF22542B) else Color(0xFF5E2424), RoundedCornerShape(10.dp))
                                    .padding(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isOk) Icons.Default.CheckCircle else Icons.Default.Error,
                                        contentDescription = null,
                                        tint = if (isOk) ApexNeonLime else Color(0xFFFF6B6B),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = inlineGeminiTestMessage!!,
                                        fontSize = 11.sp,
                                        color = if (isOk) ApexNeonLime else Color(0xFFFF6B6B),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // FatSecret API & Database Alimenti
                val isFatSecretActive by viewModel.foodDatabaseRepository.isConfigured.collectAsState()
                SettingsOptionRow(
                    icon = Icons.Default.Restaurant,
                    iconTint = if (isFatSecretActive) ApexNeonLime else Color(0xFFF97316),
                    title = "Database Cibo & FatSecret API",
                    subtitle = if (isFatSecretActive) "Attivo • Connesso a FatSecret Platform API per macro e kcal esatti"
                               else "Configura API FatSecret per collegare milioni di cibi con macro esatti",
                    testTag = "settings_fatsecret_button",
                    onClick = { showFatSecretDialog = true }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Archivio Piani Nutrizionali Button
                SettingsOptionRow(
                    icon = Icons.Default.Folder,
                    iconTint = Color(0xFF60A5FA),
                    title = "Archivio Piani Nutrizionali",
                    subtitle = "${allPlans.size} piani salvati • Gestisci e attiva piani",
                    testTag = "settings_plans_archive_button",
                    onClick = { showPlansDialog = true }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Database & Dati Locali Button
                SettingsOptionRow(
                    icon = Icons.Default.Storage,
                    iconTint = Color(0xFFFBBF24),
                    title = "Database & Memoria Locale",
                    subtitle = "${todayMeals.size} pasti oggi • Reset e gestione archiviazione Room",
                    testTag = "settings_database_button",
                    onClick = { showDatabaseDialog = true }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 4. Guida & Introduzione all'App
                SettingsOptionRow(
                    icon = Icons.Default.AutoAwesome,
                    iconTint = ApexNeonLime,
                    title = "Guida & Introduzione all'App",
                    subtitle = "Rivedi le spiegazioni su piani, formule scientifiche, voce e chat AI",
                    testTag = "settings_onboarding_review_button",
                    onClick = { showOnboardingReviewDialog = true }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 5. Guida & Istruzioni Scanner Button
                SettingsOptionRow(
                    icon = Icons.Default.HelpOutline,
                    iconTint = Color(0xFFA78BFA),
                    title = "Guida Fotocamera & Scanner",
                    subtitle = "Istruzioni per estrazione perfetta da foto diete e piatti",
                    testTag = "settings_guide_button",
                    onClick = { showGuideDialog = true }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 5. Info Sistema & Licenza Button
                SettingsOptionRow(
                    icon = Icons.Default.Info,
                    iconTint = Color(0xFF34D399),
                    title = "Info Sistema & Specifiche",
                    subtitle = "APEX // AI Core v2.4 • Offline-First Privacy",
                    testTag = "settings_system_info_button",
                    onClick = { showInfoDialog = true }
                )

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

    // Dialog Onboarding Review
    if (showOnboardingReviewDialog) {
        com.example.ui.components.AppOnboardingCarouselDialog(
            isReviewMode = true,
            onDismiss = { showOnboardingReviewDialog = false },
            onFinished = { showOnboardingReviewDialog = false }
        )
    }

    // Dialog FatSecret API
    if (showFatSecretDialog) {
        com.example.ui.components.FatSecretConfigDialog(
            repository = viewModel.foodDatabaseRepository,
            onDismiss = { showFatSecretDialog = false }
        )
    }

    // Dialog 1: Motore Gemini AI
    if (showGeminiDialog) {
        GeminiAiDialog(
            geminiService = viewModel.geminiService,
            onDismiss = { showGeminiDialog = false }
        )
    }

    // Dialog 2: Archivio Piani
    if (showPlansDialog) {
        PlansArchiveDialog(
            plans = allPlans,
            activePlanId = activePlan?.id ?: -1L,
            onSelectPlan = { planId ->
                viewModel.switchActivePlan(planId)
                showPlansDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar("Piano attivato con successo! ✓")
                }
            },
            onDismiss = { showPlansDialog = false }
        )
    }

    // Dialog 3: Database & Dati
    if (showDatabaseDialog) {
        DatabaseManagementDialog(
            plansCount = allPlans.size,
            todayMealsCount = todayMeals.size,
            onClearTodayMeals = {
                viewModel.clearTodayMeals()
                showDatabaseDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar("Pasti di oggi azzerati con successo! ✓")
                }
            },
            onDismiss = { showDatabaseDialog = false }
        )
    }

    // Dialog 4: Guida Fotocamera
    if (showGuideDialog) {
        CameraScannerGuideDialog(
            onDismiss = { showGuideDialog = false }
        )
    }

    // Dialog 5: Info Sistema
    if (showInfoDialog) {
        SystemInfoDialog(
            onDismiss = { showInfoDialog = false }
        )
    }
}

@Composable
private fun SettingsOptionRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, ApexBorder, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag(testTag),
        color = ApexDarkSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(ApexDarkSurfaceHighlight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = ApexTextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.5.sp,
                    color = ApexTextSecondary,
                    lineHeight = 15.sp
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = ApexTextMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// -------------------------------------------------------------
// DIALOGS THAT FULFILL THE INFORMATION PROMISED BY EACH BUTTON
// -------------------------------------------------------------

@Composable
private fun GeminiAiDialog(
    geminiService: GeminiNutritionService,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var apiKeyInput by remember { mutableStateOf(geminiService.customApiKey) }
    var selectedModel by remember { mutableStateOf(geminiService.selectedModel) }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isTesting by remember { mutableStateOf(false) }
    var testSuccess by remember { mutableStateOf<Boolean?>(null) }
    var testResultText by remember { mutableStateOf<String?>(null) }
    var statusFeedback by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, ApexBorder, RoundedCornerShape(24.dp)),
            color = ApexDarkSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
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
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1F283E)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = ApexNeonLime,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Google Gemini AI & Modelli",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = ApexTextPrimary
                            )
                            Text(
                                text = "Configurazione API Key & Switch Modelli",
                                fontSize = 11.sp,
                                color = ApexTextMuted
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Chiudi", tint = ApexTextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                ) {
                    // Current Status Banner
                    val isCustom = geminiService.isUsingCustomKey
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isCustom) Color(0xFF102616) else Color(0xFF182234))
                            .border(1.dp, if (isCustom) Color(0xFF22542B) else Color(0xFF2B3A5A), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isCustom) Icons.Default.CheckCircle else Icons.Default.Key,
                                contentDescription = null,
                                tint = if (isCustom) ApexNeonLime else ApexCyanAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isCustom) "Chiave Personale Gemini Attiva" else "Chiave Predefinita di Sistema",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCustom) ApexNeonLime else ApexCyanAccent
                                )
                                Text(
                                    text = if (isCustom)
                                        "Tutte le richieste AI utilizzano la tua API key di Google Gemini."
                                    else
                                        "Inserisci la tua API Key di Google AI Studio per quota dedicata illimitata.",
                                    fontSize = 11.sp,
                                    color = ApexTextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // SECTION 1: API KEY INPUT
                    Text(
                        text = "1. CHIAVE API GOOGLE GEMINI",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ApexNeonLime,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = {
                            apiKeyInput = it
                            statusFeedback = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("gemini_api_key_input"),
                        label = { Text("Gemini API Key") },
                        placeholder = { Text("AIzaSy...") },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Mostra/Nascondi",
                                        tint = ApexTextMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                if (apiKeyInput.isNotBlank()) {
                                    IconButton(onClick = { apiKeyInput = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Cancella",
                                            tint = ApexTextMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        },
                        colors = nutriTextFieldColors()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                geminiService.customApiKey = apiKeyInput.trim()
                                statusFeedback = "✓ Chiave API salvata con successo!"
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ApexNeonLime,
                                contentColor = ApexBlack
                            )
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Salva Chiave", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        if (geminiService.isUsingCustomKey) {
                            OutlinedButton(
                                onClick = {
                                    geminiService.customApiKey = ""
                                    apiKeyInput = ""
                                    statusFeedback = "Chiave rimossa. Ripristinata chiave predefinita."
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFFFF6B6B)
                                )
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Rimuovi", fontSize = 12.sp)
                            }
                        }
                    }

                    if (statusFeedback != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = statusFeedback!!,
                            fontSize = 11.sp,
                            color = ApexNeonLime,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // SECTION 2: MODEL SELECTION & SWITCHING
                    Text(
                        text = "2. SELEZIONE MODELLO GEMINI",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ApexNeonLime,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Scegli il modello primario. Se un modello non risponde o è sovraccarico, l'app passa automaticamente agli altri candidati.",
                        fontSize = 11.sp,
                        color = ApexTextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val modelOptions = listOf(
                        Triple("gemini-2.5-flash", "Gemini 2.5 Flash (Consigliato)", "Multimodale ad alta velocità, ideale per PDF, foto e chat"),
                        Triple("gemini-3.5-flash", "Gemini 3.5 Flash", "Nuova generazione, alta precisione e sintesi documenti complessi"),
                        Triple("gemini-flash-latest", "Gemini Flash Latest", "Puntatore sempre aggiornato all'ultima versione stabile"),
                        Triple("gemini-3.1-pro-preview", "Gemini 3.1 Pro Preview", "Massima intelligenza per diete articolate e tabelle nutrizionali"),
                        Triple("gemini-3.1-flash-lite-preview", "Gemini 3.1 Flash Lite", "Latenza minima e risposta istantanea")
                    )

                    modelOptions.forEach { (modelCode, title, desc) ->
                        val isSelected = selectedModel == modelCode
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0xFF14241B) else ApexDarkSurfaceHighlight)
                                .border(1.dp, if (isSelected) ApexNeonLime else ApexBorder, RoundedCornerShape(10.dp))
                                .clickable {
                                    selectedModel = modelCode
                                    geminiService.selectedModel = modelCode
                                    testResultText = null
                                }
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        selectedModel = modelCode
                                        geminiService.selectedModel = modelCode
                                        testResultText = null
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = ApexNeonLime,
                                        unselectedColor = ApexTextMuted
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = title,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (isSelected) ApexNeonLime else ApexTextPrimary
                                        )
                                        if (modelCode == "gemini-2.5-flash") {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(ApexNeonLime)
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            ) {
                                                Text("TOP", fontSize = 9.sp, fontWeight = FontWeight.Black, color = ApexBlack)
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = desc,
                                        fontSize = 11.sp,
                                        color = ApexTextSecondary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // SECTION 3: TEST CONNECTION
                    Text(
                        text = "3. VERIFICA CONNESSIONE SERVER GOOGLE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ApexNeonLime,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Verifica in tempo reale che la chiave API e il modello selezionato riescano a comunicare con i server Google.",
                        fontSize = 11.sp,
                        color = ApexTextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (testResultText != null) {
                        val isOk = testSuccess == true
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isOk) Color(0xFF102616) else Color(0xFF2E1515))
                                .border(1.dp, if (isOk) Color(0xFF22542B) else Color(0xFF5E2424), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    imageVector = if (isOk) Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = null,
                                    tint = if (isOk) ApexNeonLime else Color(0xFFFF6B6B),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (isOk) "✓ Connessione a Google Gemini Riuscita!" else "✗ Errore Connessione Gemini",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isOk) ApexNeonLime else Color(0xFFFF6B6B)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = testResultText!!,
                                        fontSize = 11.sp,
                                        color = ApexTextPrimary
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Button(
                        onClick = {
                            isTesting = true
                            testResultText = null
                            scope.launch {
                                val (success, msg) = geminiService.testConnection(
                                    keyToTest = apiKeyInput,
                                    modelToTest = selectedModel
                                )
                                isTesting = false
                                testSuccess = success
                                testResultText = msg
                            }
                        },
                        enabled = !isTesting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("gemini_test_connection_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ApexCyanAccent,
                            contentColor = ApexBlack
                        )
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = ApexBlack,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Interrogazione server Google...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Testa Connessione Server Gemini", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // SECTION 4: APPLICAZIONI DELLA CHIAVE
                    Text(
                        text = "FUNZIONALITÀ COLLEGATE AUTOMATICAMENTE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ApexTextMuted,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    InfoSpecRow(label = "Documenti Dieta", value = "Estrazione fedele PDF/Foto senza inventare cibi")
                    InfoSpecRow(label = "Chat Giornata Fuori", value = "Stima precisa pasti fuori (es. sushi AYCE)")
                    InfoSpecRow(label = "Nuove Alternative", value = "Calcolo scientifico macro su porzioni esatte")
                    InfoSpecRow(label = "Pantry Chef", value = "Proposte bilanciate dagli ingredienti in dispensa")
                }
            }
        }
    }
}

@Composable
private fun PlansArchiveDialog(
    plans: List<PlanEntity>,
    activePlanId: Long,
    onSelectPlan: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, ApexBorder, RoundedCornerShape(20.dp)),
            color = ApexDarkSurface
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Archivio Piani",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ApexTextPrimary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Chiudi", tint = ApexTextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(plans, key = { it.id }) { plan ->
                        val isActive = plan.id == activePlanId
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    1.dp,
                                    if (isActive) ApexNeonLime else ApexBorder,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { onSelectPlan(plan.id) },
                            color = if (isActive) Color(0xFF172018) else ApexDarkSurfaceHighlight
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = plan.name,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ApexTextPrimary
                                        )
                                        if (isActive) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "ATTIVO",
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Black,
                                                color = ApexNeonLime
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${plan.caloriesTarget} kcal • P ${plan.proteinTarget}g • C ${plan.carbsTarget}g • G ${plan.fatTarget}g",
                                        fontSize = 11.5.sp,
                                        color = ApexTextSecondary
                                    )
                                }

                                if (isActive) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = ApexNeonLime,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DatabaseManagementDialog(
    plansCount: Int,
    todayMealsCount: Int,
    onClearTodayMeals: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, ApexBorder, RoundedCornerShape(20.dp)),
            color = ApexDarkSurface
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Database & Memoria",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = ApexTextPrimary
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Chiudi", tint = ApexTextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                InfoSpecRow(label = "Architettura DB", value = "Android Room SQLite (Locale)")
                InfoSpecRow(label = "Piani salvati", value = "$plansCount piani")
                InfoSpecRow(label = "Pasti registrati oggi", value = "$todayMealsCount pasti")
                InfoSpecRow(label = "Privacy & Rete", value = "Nessun dato inviato a server terzi")

                Spacer(modifier = Modifier.height(18.dp))

                OutlinedButton(
                    onClick = onClearTodayMeals,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFEF4444)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Azzera pasti registrati oggi", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CameraScannerGuideDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, ApexBorder, RoundedCornerShape(20.dp)),
            color = ApexDarkSurface
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = null,
                            tint = Color(0xFFA78BFA),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Guida Fotocamera AI",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = ApexTextPrimary
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Chiudi", tint = ApexTextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "1. Scansione Dieta Cartacea",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = ApexNeonLime
                )
                Text(
                    text = "Inquadra dall'alto con buona luce naturale. L'AI leggerà pasti (Colazione, Pranzo, Cena...) e tutte le alternative coi grammi precisi.",
                    fontSize = 12.sp,
                    color = ApexTextSecondary,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "2. Scatto Foto Pasti al Ristorante",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = ApexCyanAccent
                )
                Text(
                    text = "Inquadra il piatto a 45 gradi. L'algoritmo stima grammi e ingredienti visibili (es. olio, carboidrati, proteine).",
                    fontSize = 12.sp,
                    color = ApexTextSecondary,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ApexNeonLime,
                        contentColor = ApexBlack
                    )
                ) {
                    Text("Ho capito", fontWeight = FontWeight.Bold, color = ApexBlack)
                }
            }
        }
    }
}

@Composable
private fun SystemInfoDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, ApexBorder, RoundedCornerShape(20.dp)),
            color = ApexDarkSurface
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = Color(0xFF34D399),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Info Sistema APEX",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = ApexTextPrimary
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Chiudi", tint = ApexTextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                InfoSpecRow(label = "Versione Core", value = "APEX // AI v2.4.0")
                InfoSpecRow(label = "Edizione", value = "Stealth Dark & Neon Volt")
                InfoSpecRow(label = "Framework UI", value = "Android Jetpack Compose M3")
                InfoSpecRow(label = "Architettura", value = "Clean MVVM + StateFlow + Room")
                InfoSpecRow(label = "Ambiente", value = "Google AI Studio Cloud")

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ApexNeonLime,
                        contentColor = ApexBlack
                    )
                ) {
                    Text("Chiudi", fontWeight = FontWeight.Bold, color = ApexBlack)
                }
            }
        }
    }
}

@Composable
private fun InfoSpecRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = ApexTextSecondary)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = ApexTextPrimary)
    }
}
