package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.entity.ChatMessage
import com.example.data.entity.GeneratedMealProposal
import com.example.data.entity.Ingredient
import com.example.data.entity.MealSlotEntity
import com.example.ui.theme.ApexBlack
import com.example.ui.theme.ApexBorder
import com.example.ui.theme.ApexCalories
import com.example.ui.theme.ApexCarbs
import com.example.ui.theme.ApexCyanAccent
import com.example.ui.theme.ApexDarkSurface
import com.example.ui.theme.ApexDarkSurfaceHighlight
import com.example.ui.theme.ApexFats
import com.example.ui.theme.ApexNeonLime
import com.example.ui.theme.ApexProtein
import com.example.ui.theme.ApexTextMuted
import com.example.ui.theme.ApexTextPrimary
import com.example.ui.theme.ApexTextSecondary
import com.example.ui.theme.nutriTextFieldColors
import com.example.ui.viewmodel.NutritionViewModel
import java.util.Locale

@Composable
fun ChatScreen(
    viewModel: NutritionViewModel,
    onNavigateToPlan: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val chatMessages by viewModel.chatMessages.collectAsState()
    val isChatLoading by viewModel.isChatLoading.collectAsState()
    val activePlan by viewModel.activePlan.collectAsState()
    val activePlanSlots by viewModel.activePlanSlots.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var selectedMealForCopy by remember { mutableStateOf<GeneratedMealProposal?>(null) }
    var showSlotPicker by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Voice recognition launcher for speech dictation
    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                inputText = if (inputText.isBlank()) spokenText else "$inputText $spokenText"
            }
        }
    }

    // Auto-scroll when new messages arrive
    LaunchedEffect(chatMessages.size, isChatLoading) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ApexBlack)
            .imePadding()
    ) {
        // --- Apex Top Header ---
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = ApexDarkSurface,
            border = BorderStroke(1.dp, ApexBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ApexDarkSurfaceHighlight)
                                .border(1.dp, ApexBorder, RoundedCornerShape(10.dp)),
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Chat Nutrizionista AI",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ApexTextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(ApexNeonLime.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Gemini",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ApexNeonLime
                                    )
                                }
                            }
                            Text(
                                text = "Crea il pasto perfetto con ciò che hai in dispensa",
                                fontSize = 11.5.sp,
                                color = ApexTextSecondary
                            )
                        }
                    }

                    // Reset / Clear chat button
                    IconButton(
                        onClick = { viewModel.clearChat() },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("clear_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Nuova conversazione",
                            tint = ApexTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Active plan target summary pill
                activePlan?.let { plan ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = ApexDarkSurfaceHighlight,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, ApexBorder)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Piano attivo: ${plan.name}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ApexTextPrimary
                                )
                                Text(
                                    text = "${plan.caloriesTarget} kcal",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ApexNeonLime
                                )
                            }

                            // Slot quick suggestion chips
                            if (activePlanSlots.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                val mealsCount = plan.mealsCount.coerceAtLeast(1)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    activePlanSlots.forEach { slot ->
                                        val slotCal = slot.customCalories ?: (plan.caloriesTarget / mealsCount)
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(ApexDarkSurface)
                                                .border(1.dp, ApexBorder, RoundedCornerShape(6.dp))
                                                .clickable {
                                                    inputText = "Voglio preparare il pasto: ${slot.name} ($slotCal kcal). In dispensa ho: "
                                                }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "${slot.name}: $slotCal kcal",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = ApexCyanAccent
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

        // --- Quick Pantry Ingredients Bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ApexBlack)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Dispensa rapida:",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = ApexTextMuted
            )

            viewModel.quickPantryItems.forEach { ingredient ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(ApexDarkSurfaceHighlight)
                        .border(1.dp, ApexBorder, RoundedCornerShape(14.dp))
                        .clickable {
                            inputText = if (inputText.isBlank()) {
                                "In dispensa ho $ingredient"
                            } else if (inputText.endsWith(" ")) {
                                "$inputText$ingredient, "
                            } else {
                                "$inputText, $ingredient, "
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "+ $ingredient",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = ApexTextPrimary
                    )
                }
            }
        }

        HorizontalDivider(color = ApexBorder)

        // --- Chat Thread (Scrollable) ---
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Initial suggestion prompts when chat is at the start
            if (chatMessages.size <= 1) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Text(
                            text = "Suggerimenti pronti da provare:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ApexTextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val prompts = listOf(
                            "Ho uova, zucchine e pane integrale: cosa preparo per pranzo?",
                            "Ho petto di pollo, riso basmati e olio EVO: cena da piano",
                            "Ho yogurt greco, avena, mela e noci per lo spuntino",
                            "Ho pasta di semola, tonno al naturale e pomodorini"
                        )

                        prompts.forEach { prompt ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        viewModel.sendChatMessage(prompt)
                                    },
                                color = ApexDarkSurface,
                                border = BorderStroke(1.dp, ApexBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Restaurant,
                                        contentDescription = null,
                                        tint = ApexNeonLime,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = prompt,
                                        fontSize = 12.5.sp,
                                        color = ApexTextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Message items
            items(chatMessages, key = { it.id }) { message ->
                ChatMessageItem(
                    message = message,
                    onCopyToPlan = { proposal ->
                        selectedMealForCopy = proposal
                        showSlotPicker = true
                    },
                    onLogOutdoorDay = { proposal ->
                        viewModel.logGeneratedMealToToday(proposal)
                        Toast.makeText(context, "Pasto registrato in Giornata Fuori! ✓", Toast.LENGTH_SHORT).show()
                    },
                    onAddToShopping = { proposal ->
                        activePlan?.let { plan ->
                            viewModel.addGeneratedMealToShoppingList(proposal, plan.id)
                            Toast.makeText(context, "Ingredienti aggiunti alla Spesa! ✓", Toast.LENGTH_SHORT).show()
                        } ?: run {
                            Toast.makeText(context, "Nessun piano attivo per la lista spesa.", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            // Loading indicator when waiting for Gemini
            if (isChatLoading) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(ApexDarkSurfaceHighlight)
                                .border(1.dp, ApexNeonLime, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = ApexNeonLime,
                                strokeWidth = 2.dp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Gemini sta elaborando il pasto perfetto dalla dispensa...",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = ApexNeonLime
                        )
                    }
                }
            }
        }

        // --- Bottom Input Area ---
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = ApexDarkSurface,
            border = BorderStroke(1.dp, ApexBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            text = "Scrivi cosa hai in dispensa o chiedi un pasto...",
                            fontSize = 13.sp,
                            color = ApexTextMuted
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    shape = RoundedCornerShape(20.dp),
                    colors = nutriTextFieldColors(),
                    textStyle = TextStyle(color = ApexTextPrimary, fontSize = 13.5.sp),
                    maxLines = 3,
                    trailingIcon = {
                        if (inputText.isNotBlank()) {
                            IconButton(onClick = { inputText = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Cancella",
                                    tint = ApexTextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                )

                // Voice Dictation Button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(ApexDarkSurfaceHighlight)
                        .border(1.dp, ApexBorder, CircleShape)
                        .clickable {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ITALIAN.toLanguageTag())
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Parla adesso: detta la tua richiesta o ingredienti...")
                            }
                            try {
                                speechRecognizerLauncher.launch(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Dettatura vocale non disponibile sul dispositivo", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .testTag("chat_voice_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Dettatura Vocale",
                        tint = ApexNeonLime,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Send Button
                val canSend = inputText.isNotBlank() && !isChatLoading
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (canSend) ApexNeonLime else ApexDarkSurfaceHighlight)
                        .clickable(enabled = canSend) {
                            val textToSend = inputText
                            inputText = ""
                            viewModel.sendChatMessage(textToSend)
                        }
                        .testTag("chat_send_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Invia",
                        tint = if (canSend) ApexBlack else ApexTextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    // --- Slot Picker Dialog for "Copia nel Piano" ---
    if (showSlotPicker && selectedMealForCopy != null) {
        val meal = selectedMealForCopy!!
        val currentPlan = activePlan

        Dialog(onDismissRequest = { showSlotPicker = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp)),
                color = ApexDarkSurface,
                border = BorderStroke(1.dp, ApexBorder)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Copia nel Piano",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = ApexTextPrimary
                        )
                        IconButton(onClick = { showSlotPicker = false }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Chiudi",
                                tint = ApexTextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Scegli in quale pasto del piano \"${currentPlan?.name ?: "Attivo"}\" vuoi aggiungere come alternativa:",
                        fontSize = 13.sp,
                        color = ApexTextSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = ApexDarkSurfaceHighlight,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, ApexBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restaurant,
                                contentDescription = null,
                                tint = ApexNeonLime,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = meal.name,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ApexTextPrimary
                                )
                                Text(
                                    text = "${meal.calories} kcal • ${meal.protein}P • ${meal.carbs}C • ${meal.fat}G",
                                    fontSize = 11.5.sp,
                                    color = ApexNeonLime
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (activePlanSlots.isEmpty()) {
                        Text(
                            text = "Nessun pasto configurato nel piano attivo. Crea o imposta prima un piano!",
                            fontSize = 13.sp,
                            color = ApexTextMuted
                        )
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            activePlanSlots.forEach { slot ->
                                val isTargetMatch = meal.targetSlotName?.contains(slot.name, ignoreCase = true) == true

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            if (currentPlan != null) {
                                                viewModel.copyGeneratedMealToSlot(
                                                    proposal = meal,
                                                    targetSlotId = slot.id,
                                                    planId = currentPlan.id
                                                )
                                                showSlotPicker = false
                                                Toast.makeText(
                                                    context,
                                                    "Aggiunto a \"${slot.name}\" nel piano attivo! ✓",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        },
                                    color = if (isTargetMatch) ApexNeonLime.copy(alpha = 0.12f) else ApexDarkSurfaceHighlight,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isTargetMatch) ApexNeonLime else ApexBorder
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = slot.name,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = ApexTextPrimary
                                                )
                                                if (isTargetMatch) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(ApexNeonLime)
                                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                                    ) {
                                                        Text(
                                                            text = "Consigliato",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = ApexBlack
                                                        )
                                                    }
                                                }
                                            }
                                            val mealsCount = (currentPlan?.mealsCount ?: 4).coerceAtLeast(1)
                                            val sCal = slot.customCalories ?: ((currentPlan?.caloriesTarget ?: 2000) / mealsCount)
                                            val sP = slot.customProtein ?: ((currentPlan?.proteinTarget ?: 140) / mealsCount)
                                            val sC = slot.customCarbs ?: ((currentPlan?.carbsTarget ?: 200) / mealsCount)
                                            val sF = slot.customFat ?: ((currentPlan?.fatTarget ?: 60) / mealsCount)
                                            Text(
                                                text = "Target slot: $sCal kcal (P:${sP}g C:${sC}g G:${sF}g)",
                                                fontSize = 11.5.sp,
                                                color = ApexTextSecondary
                                            )
                                        }

                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Seleziona",
                                            tint = if (isTargetMatch) ApexNeonLime else ApexTextMuted,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showSlotPicker = false }) {
                            Text("Annulla", color = ApexTextMuted)
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Chat Message Item Component
// ---------------------------------------------------------------------------
@Composable
private fun ChatMessageItem(
    message: ChatMessage,
    onCopyToPlan: (GeneratedMealProposal) -> Unit,
    onLogOutdoorDay: (GeneratedMealProposal) -> Unit,
    onAddToShopping: (GeneratedMealProposal) -> Unit
) {
    if (message.isUser) {
        // User Message Bubble (Right aligned)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)),
                color = ApexDarkSurfaceHighlight,
                border = BorderStroke(1.dp, ApexBorder)
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(
                        text = message.text,
                        fontSize = 13.5.sp,
                        color = ApexTextPrimary,
                        lineHeight = 19.sp
                    )
                }
            }
        }
    } else {
        // Assistant Message Bubble (Left aligned)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Column(modifier = Modifier.fillMaxWidth(0.96f)) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)),
                    color = ApexDarkSurface,
                    border = BorderStroke(1.dp, ApexBorder)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(ApexNeonLime.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = ApexNeonLime,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Apex Nutritionist",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = ApexNeonLime
                            )
                        }

                        Text(
                            text = message.text,
                            fontSize = 13.5.sp,
                            color = ApexTextPrimary,
                            lineHeight = 19.sp
                        )
                    }
                }

                // If a structured meal proposal was created, show the rich Action Card
                message.mealProposal?.let { proposal ->
                    Spacer(modifier = Modifier.height(10.dp))
                    MealActionCard(
                        proposal = proposal,
                        onCopyToPlan = { onCopyToPlan(proposal) },
                        onLogOutdoorDay = { onLogOutdoorDay(proposal) },
                        onAddToShopping = { onAddToShopping(proposal) }
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Interactive Action Card for the Generated Meal
// ---------------------------------------------------------------------------
@Composable
private fun MealActionCard(
    proposal: GeneratedMealProposal,
    onCopyToPlan: () -> Unit,
    onLogOutdoorDay: () -> Unit,
    onAddToShopping: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        color = ApexDarkSurfaceHighlight,
        border = BorderStroke(1.5.dp, ApexNeonLime)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Dish Name & Target Slot badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = proposal.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ApexTextPrimary
                    )
                    proposal.targetSlotName?.let { slotName ->
                        Text(
                            text = "Calibrato per: $slotName",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ApexCyanAccent
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(ApexNeonLime)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${proposal.calories} KCAL",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ApexBlack
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4 Macro Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MacroPill(
                    label = "Proteine",
                    value = "${proposal.protein}g",
                    color = ApexProtein,
                    modifier = Modifier.weight(1f)
                )
                MacroPill(
                    label = "Carboidrati",
                    value = "${proposal.carbs}g",
                    color = ApexCarbs,
                    modifier = Modifier.weight(1f)
                )
                MacroPill(
                    label = "Grassi",
                    value = "${proposal.fat}g",
                    color = ApexFats,
                    modifier = Modifier.weight(1f)
                )
            }

            // Ingredients list
            if (proposal.ingredients.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Ingredienti e dosaggi precisi:",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ApexTextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))

                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    proposal.ingredients.forEach { ing ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "• ${ing.name}",
                                fontSize = 12.sp,
                                color = ApexTextPrimary
                            )
                            Text(
                                text = ing.quantity,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ApexNeonLime
                            )
                        }
                    }
                }
            }

            // Recipe notes / preparation
            if (proposal.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = proposal.notes,
                    fontSize = 11.5.sp,
                    color = ApexTextMuted,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = ApexBorder)
            Spacer(modifier = Modifier.height(12.dp))

            // Functional Buttons: Copia nel Piano & Registra in Giornata Fuori
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onCopyToPlan,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .testTag("copy_to_plan_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ApexNeonLime,
                        contentColor = ApexBlack
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Assignment,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = ApexBlack
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Copia nel Piano",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ApexBlack
                    )
                }

                Button(
                    onClick = onLogOutdoorDay,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .testTag("log_outdoor_day_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ApexDarkSurfaceHighlight,
                        contentColor = ApexTextPrimary
                    ),
                    border = BorderStroke(1.dp, ApexBorder)
                ) {
                    Icon(
                        imageVector = Icons.Default.Flight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = ApexCyanAccent
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Giornata Fuori",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ApexTextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Bonus Button: Aggiungi ingredienti mancanti alla Spesa
            OutlinedButton(
                onClick = onAddToShopping,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .testTag("add_to_shopping_button"),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, ApexBorder),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ApexTextSecondary)
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = ApexTextSecondary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Aggiungi ingredienti alla Lista Spesa",
                    fontSize = 11.5.sp,
                    color = ApexTextSecondary
                )
            }
        }
    }
}

@Composable
private fun MacroPill(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = ApexDarkSurfaceHighlight,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, ApexBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = ApexTextSecondary
            )
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}
