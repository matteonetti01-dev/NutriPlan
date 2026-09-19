package com.example.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.entity.PlanEntity
import com.example.data.entity.ShoppingItemEntity
import com.example.ui.theme.ApexBlack
import com.example.ui.theme.ApexBorder
import com.example.ui.theme.ApexCyanAccent
import com.example.ui.theme.ApexDarkSurface
import com.example.ui.theme.ApexDarkSurfaceHighlight
import com.example.ui.theme.ApexNeonLime
import com.example.ui.theme.ApexTextMuted
import com.example.ui.theme.ApexTextPrimary
import com.example.ui.theme.ApexTextSecondary
import com.example.ui.theme.nutriTextFieldColors
import com.example.ui.viewmodel.NutritionViewModel
import kotlinx.coroutines.launch

@Composable
fun SpesaScreen(
    viewModel: NutritionViewModel,
    onBack: () -> Unit
) {
    val allPlans by viewModel.allPlans.collectAsState()
    val activePlan by viewModel.activePlan.collectAsState()
    val currentShoppingPlan by viewModel.currentShoppingPlan.collectAsState()
    val shoppingItems by viewModel.shoppingItems.collectAsState()
    val isGenerating by viewModel.isGeneratingShoppingList.collectAsState()

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Manual item edit dialog state
    var itemToEdit by remember { mutableStateOf<ShoppingItemEntity?>(null) }

    // Quick custom item input states
    var newItemName by remember { mutableStateOf("") }
    var newItemQuantity by remember { mutableStateOf("") }
    var newItemPackageCount by remember { mutableStateOf(1) }
    var newItemPackageGrammage by remember { mutableStateOf("") }
    var newItemCategory by remember { mutableStateOf("Altro") }
    var showCategoryDropdown by remember { mutableStateOf(false) }

    val categories = listOf(
        "Carne, Pesce & Uova",
        "Frutta & Verdura",
        "Latticini & Formaggi",
        "Cereali, Pasta & Pane",
        "Condimenti & Dispensa",
        "Snack & Frutta Secca",
        "Altro"
    )

    // Auto-generate if plan has items and shopping list is empty
    LaunchedEffect(currentShoppingPlan?.id) {
        val plan = currentShoppingPlan
        if (plan != null && shoppingItems.isEmpty() && !isGenerating) {
            viewModel.generateOrRefreshShoppingList(plan.id)
        }
    }

    val totalCount = shoppingItems.size
    val checkedCount = shoppingItems.count { it.isChecked }
    val progress = if (totalCount > 0) checkedCount.toFloat() / totalCount else 0f

    // Group items by category
    val itemsByCategory = remember(shoppingItems) {
        shoppingItems.groupBy { it.category }
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
                .testTag("spesa_screen")
        ) {
            item {
                Spacer(modifier = Modifier.height(14.dp))

                // Top Bar with Back button and Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(ApexDarkSurfaceHighlight)
                            .border(1.dp, ApexBorder, CircleShape)
                            .testTag("spesa_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Torna indietro",
                            tint = ApexTextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "SPESA",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = ApexTextPrimary,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF1B2E1D))
                                    .border(1.dp, Color(0xFF2E6333), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "AI GROCERY",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = ApexNeonLime,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                        Text(
                            text = "Lista della spesa intelligente generata dal piano",
                            fontSize = 12.sp,
                            color = ApexTextSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF142417))
                            .border(1.dp, ApexNeonLime.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = ApexNeonLime,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Plan Selector Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, ApexBorder, RoundedCornerShape(16.dp)),
                    color = ApexDarkSurface
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "PIANO SELEZIONATO",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ApexCyanAccent,
                                letterSpacing = 0.8.sp
                            )

                            if (currentShoppingPlan?.id == activePlan?.id) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF16331C))
                                        .border(1.dp, ApexNeonLime, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "PIANO ATTIVO",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ApexNeonLime
                                    )
                                }
                            } else if (currentShoppingPlan != null) {
                                Text(
                                    text = "Tocca per attivare",
                                    fontSize = 11.sp,
                                    color = ApexNeonLime,
                                    modifier = Modifier.clickable {
                                        currentShoppingPlan?.let { viewModel.switchActivePlan(it.id) }
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Piano '${currentShoppingPlan?.name}' attivato!")
                                        }
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = currentShoppingPlan?.name ?: "Nessun piano disponibile",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = ApexTextPrimary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Horizontal chips for plans switching
                        if (allPlans.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                allPlans.forEach { plan ->
                                    val isSelected = plan.id == currentShoppingPlan?.id
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSelected) ApexNeonLime.copy(alpha = 0.15f)
                                                else ApexDarkSurfaceHighlight
                                            )
                                            .border(
                                                1.dp,
                                                if (isSelected) ApexNeonLime else ApexBorder,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                viewModel.selectShoppingPlan(plan.id)
                                            }
                                            .padding(horizontal = 12.dp, vertical = 7.dp)
                                            .testTag("spesa_plan_chip_${plan.id}")
                                    ) {
                                        Text(
                                            text = plan.name,
                                            fontSize = 12.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) ApexNeonLime else ApexTextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // AI Refresh and Stats Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, ApexBorder, RoundedCornerShape(16.dp)),
                    color = ApexDarkSurface
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "PROGRESSO SPESA",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ApexTextMuted,
                                    letterSpacing = 0.8.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$checkedCount di $totalCount cibi presi",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = ApexTextPrimary
                                )
                            }

                            Button(
                                onClick = {
                                    currentShoppingPlan?.let { plan ->
                                        viewModel.generateOrRefreshShoppingList(plan.id) { msg ->
                                            scope.launch { snackbarHostState.showSnackbar(msg) }
                                        }
                                    }
                                },
                                enabled = !isGenerating && currentShoppingPlan != null,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ApexNeonLime,
                                    contentColor = Color.Black,
                                    disabledContainerColor = ApexDarkSurfaceHighlight,
                                    disabledContentColor = ApexTextMuted
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("spesa_refresh_ai_button")
                            ) {
                                if (isGenerating) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = ApexNeonLime,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Analisi AI...",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Aggiorna con AI",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Sleek Progress Bar
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = ApexNeonLime,
                            trackColor = ApexDarkSurfaceHighlight
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Quick action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    currentShoppingPlan?.let {
                                        viewModel.setAllShoppingItemsChecked(it.id, false)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("spesa_uncheck_all_button"),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ApexBorder),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = ApexTextSecondary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Deseleziona tutti",
                                    fontSize = 11.5.sp
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    currentShoppingPlan?.let {
                                        viewModel.clearCheckedShoppingItems(it.id)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("spesa_clear_checked_button"),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ApexBorder),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFFEF4444)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Rimuovi presi",
                                    fontSize = 11.5.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick Add Custom Item Section
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, ApexBorder, RoundedCornerShape(16.dp)),
                    color = ApexDarkSurface
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "+ AGGIUNGI PRODOTTO O INGREDIENTE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ApexTextMuted,
                            letterSpacing = 0.8.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newItemName,
                                onValueChange = { newItemName = it },
                                placeholder = { Text("Nome (es. Petto di pollo...)", fontSize = 12.5.sp, color = ApexTextMuted) },
                                modifier = Modifier
                                    .weight(1.3f)
                                    .testTag("spesa_input_name"),
                                colors = nutriTextFieldColors(),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            OutlinedTextField(
                                value = newItemQuantity,
                                onValueChange = { newItemQuantity = it },
                                placeholder = { Text("Qtà totale (es. 600g)", fontSize = 12.5.sp, color = ApexTextMuted) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("spesa_input_quantity"),
                                colors = nutriTextFieldColors(),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Row for package count and package grammage
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Package count stepper
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = ApexDarkSurfaceHighlight,
                                border = BorderStroke(1.dp, ApexBorder),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "Pacchi:",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ApexTextMuted,
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(ApexBlack)
                                            .clickable {
                                                if (newItemPackageCount > 1) newItemPackageCount--
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("-", color = ApexTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "$newItemPackageCount",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        color = ApexNeonLime
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(ApexBlack)
                                            .clickable { newItemPackageCount++ },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("+", color = ApexNeonLime, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = newItemPackageGrammage,
                                onValueChange = { newItemPackageGrammage = it },
                                placeholder = { Text("Grammatura pacco (es. 300g)", fontSize = 11.5.sp, color = ApexTextMuted) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("spesa_input_package_grammage"),
                                colors = nutriTextFieldColors(),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Category selector dropdown
                            Box {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ApexDarkSurfaceHighlight)
                                        .border(1.dp, ApexBorder, RoundedCornerShape(8.dp))
                                        .clickable { showCategoryDropdown = true }
                                        .padding(horizontal = 10.dp, vertical = 7.dp)
                                        .testTag("spesa_category_selector")
                                ) {
                                    Text(
                                        text = "Reparto: $newItemCategory ▼",
                                        fontSize = 11.5.sp,
                                        color = ApexTextSecondary
                                    )
                                }

                                DropdownMenu(
                                    expanded = showCategoryDropdown,
                                    onDismissRequest = { showCategoryDropdown = false },
                                    modifier = Modifier.background(ApexDarkSurface)
                                ) {
                                    categories.forEach { cat ->
                                        DropdownMenuItem(
                                            text = { Text(cat, color = ApexTextPrimary, fontSize = 12.sp) },
                                            onClick = {
                                                newItemCategory = cat
                                                showCategoryDropdown = false
                                            }
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    currentShoppingPlan?.let { plan ->
                                        if (newItemName.isNotBlank()) {
                                            viewModel.addCustomShoppingItem(
                                                planId = plan.id,
                                                name = newItemName,
                                                quantity = newItemQuantity.ifBlank { "1 pz" },
                                                packageCount = newItemPackageCount,
                                                packageGrammage = newItemPackageGrammage,
                                                category = newItemCategory
                                            )
                                            newItemName = ""
                                            newItemQuantity = ""
                                            newItemPackageCount = 1
                                            newItemPackageGrammage = ""
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Prodotto aggiunto alla lista!")
                                            }
                                        }
                                    }
                                },
                                enabled = newItemName.isNotBlank() && currentShoppingPlan != null,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ApexCyanAccent,
                                    contentColor = Color.Black,
                                    disabledContainerColor = ApexDarkSurfaceHighlight,
                                    disabledContentColor = ApexTextMuted
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("spesa_add_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Aggiungi",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // List Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "COSE DA COMPRARE (${shoppingItems.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ApexNeonLime,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            // If empty list
            if (shoppingItems.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, ApexBorder, RoundedCornerShape(16.dp)),
                        color = ApexDarkSurface
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(ApexDarkSurfaceHighlight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = null,
                                    tint = ApexTextMuted,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "Nessun cibo nella lista della spesa",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = ApexTextPrimary
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "L'AI genera automaticamente la lista della spesa prendendo tutti i cibi e gli ingredienti dai pasti configurati in questo piano.",
                                fontSize = 12.5.sp,
                                color = ApexTextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    currentShoppingPlan?.let { plan ->
                                        viewModel.generateOrRefreshShoppingList(plan.id) { msg ->
                                            scope.launch { snackbarHostState.showSnackbar(msg) }
                                        }
                                    }
                                },
                                enabled = !isGenerating && currentShoppingPlan != null,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ApexNeonLime,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("spesa_generate_empty_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Genera Spesa dal Piano",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } else {
                // Grouped by Category
                itemsByCategory.forEach { (category, items) ->
                    item {
                        CategoryHeader(category = category, count = items.size)
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    items(items, key = { it.id }) { item ->
                        ShoppingItemRow(
                            item = item,
                            onToggle = { viewModel.toggleShoppingItem(item) },
                            onDelete = { viewModel.deleteShoppingItem(item) },
                            onEdit = { itemToEdit = item },
                            onQuickAdjustQuantity = { newQty ->
                                viewModel.updateShoppingItemQuantity(item, newQty)
                            }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(50.dp))
            }
        }

        // Edit Item Quantity & Details Dialog
        itemToEdit?.let { item ->
            EditShoppingItemDialog(
                item = item,
                onDismiss = { itemToEdit = null },
                onSave = { newName, newQuantity, newPackageCount, newPackageGrammage, newNotes ->
                    viewModel.updateShoppingItemDetails(
                        item = item,
                        newName = newName,
                        newQuantity = newQuantity,
                        newPackageCount = newPackageCount,
                        newPackageGrammage = newPackageGrammage,
                        newNotes = newNotes
                    )
                    itemToEdit = null
                    scope.launch {
                        snackbarHostState.showSnackbar("Prodotto '$newName' aggiornato!")
                    }
                }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
        )
    }
}

@Composable
private fun CategoryHeader(category: String, count: Int) {
    val categoryIcon = when {
        category.contains("Carne", ignoreCase = true) || category.contains("Pesce", ignoreCase = true) -> "🥩"
        category.contains("Frutta", ignoreCase = true) || category.contains("Verdura", ignoreCase = true) -> "🥦"
        category.contains("Latticini", ignoreCase = true) || category.contains("Formaggi", ignoreCase = true) -> "🧀"
        category.contains("Cereali", ignoreCase = true) || category.contains("Pasta", ignoreCase = true) || category.contains("Pane", ignoreCase = true) -> "🌾"
        category.contains("Condimenti", ignoreCase = true) -> "🫒"
        category.contains("Snack", ignoreCase = true) || category.contains("Secca", ignoreCase = true) -> "🥜"
        else -> "🛒"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$categoryIcon  $category",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = ApexCyanAccent
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "($count)",
            fontSize = 12.sp,
            color = ApexTextMuted
        )
    }
}

@Composable
private fun ShoppingItemRow(
    item: ShoppingItemEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onQuickAdjustQuantity: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(
                1.dp,
                if (item.isChecked) ApexBorder.copy(alpha = 0.4f) else ApexBorder,
                RoundedCornerShape(14.dp)
            )
            .testTag("spesa_item_${item.id}"),
        color = if (item.isChecked) Color(0xFF0D1217) else ApexDarkSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Main Top Row: Checkbox, Full Width Description, and Subtle Delete button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Checkbox
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(
                            if (item.isChecked) ApexNeonLime else Color.Transparent
                        )
                        .border(
                            1.5.dp,
                            if (item.isChecked) ApexNeonLime else ApexBorder,
                            RoundedCornerShape(7.dp)
                        )
                        .clickable { onToggle() },
                    contentAlignment = Alignment.Center
                ) {
                    if (item.isChecked) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Spuntato",
                            tint = Color.Black,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Name & Notes - takes ALL available horizontal space without being squashed
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onToggle() }
                ) {
                    Text(
                        text = item.name,
                        fontSize = 15.sp,
                        fontWeight = if (item.isChecked) FontWeight.Normal else FontWeight.Bold,
                        color = if (item.isChecked) ApexTextMuted else ApexTextPrimary,
                        textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None,
                        lineHeight = 20.sp
                    )

                    if (item.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = item.notes,
                            fontSize = 12.sp,
                            color = ApexTextMuted,
                            textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Delete button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Elimina prodotto",
                        tint = ApexTextMuted.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dedicated Bottom Row: Quantity Badge + Packages Pill + Quick +/- Buttons
            // Indented to align cleanly under the text (start padding 38dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 38.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Interactive Quantity Pill (tapping it opens manual edit dialog)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (item.isChecked) ApexDarkSurfaceHighlight.copy(alpha = 0.5f) else ApexDarkSurfaceHighlight,
                        border = BorderStroke(
                            1.dp,
                            if (item.isChecked) ApexBorder.copy(alpha = 0.4f) else ApexCyanAccent.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onEdit() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Modifica quantità",
                                tint = if (item.isChecked) ApexTextMuted else ApexCyanAccent,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (item.quantity.isNotBlank()) "Qtà: ${item.quantity}" else "Qtà",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (item.isChecked) ApexTextMuted else ApexNeonLime
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "✎",
                                fontSize = 11.sp,
                                color = if (item.isChecked) ApexTextMuted else ApexCyanAccent
                            )
                        }
                    }

                    // Interactive Package Pill (displays packages and packaging grammage, tap to edit)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (item.isChecked) ApexDarkSurfaceHighlight.copy(alpha = 0.5f) else Color(0xFF162329),
                        border = BorderStroke(
                            1.dp,
                            if (item.isChecked) ApexBorder.copy(alpha = 0.3f) else ApexCyanAccent.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onEdit() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📦",
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            val pkgText = buildString {
                                append("${item.packageCount.coerceAtLeast(1)} ${if (item.packageCount == 1) "pacco" else "pacchi"}")
                                if (item.packageGrammage.isNotBlank()) {
                                    append(" (${item.packageGrammage})")
                                }
                            }
                            Text(
                                text = pkgText,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (item.isChecked) ApexTextMuted else ApexCyanAccent
                            )
                        }
                    }
                }

                // Quick minus / plus buttons for rapid one-touch adjustment if quantity has numbers
                val hasDigits = item.quantity.any { it.isDigit() }
                if (hasDigits && !item.isChecked) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Minus button
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(ApexDarkSurfaceHighlight)
                                .border(1.dp, ApexBorder, RoundedCornerShape(6.dp))
                                .clickable {
                                    val updated = quickAdjust(item.quantity, -1)
                                    onQuickAdjustQuantity(updated)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Riduci quantità",
                                tint = ApexTextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        // Plus button
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(ApexDarkSurfaceHighlight)
                                .border(1.dp, ApexBorder, RoundedCornerShape(6.dp))
                                .clickable {
                                    val updated = quickAdjust(item.quantity, +1)
                                    onQuickAdjustQuantity(updated)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Aumenta quantità",
                                tint = ApexNeonLime,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditShoppingItemDialog(
    item: ShoppingItemEntity,
    onDismiss: () -> Unit,
    onSave: (newName: String, newQuantity: String, newPackageCount: Int, newPackageGrammage: String, newNotes: String) -> Unit
) {
    var editName by remember { mutableStateOf(item.name) }
    var editQuantity by remember { mutableStateOf(item.quantity) }
    var editPackageCount by remember { mutableStateOf(item.packageCount.coerceAtLeast(1)) }
    var editPackageGrammage by remember { mutableStateOf(item.packageGrammage) }
    var editNotes by remember { mutableStateOf(item.notes) }

    val presetQuantities = listOf(
        "50g", "100g", "150g", "200g", "250g", "300g", "500g", "1 kg", "2 pz", "4 fette", "6 uova", "1 conf", "q.b."
    )

    val presetGrammages = listOf(
        "100g", "150g", "200g", "250g", "300g", "400g", "500g", "1 kg", "250ml", "500ml", "1L"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, ApexBorder, RoundedCornerShape(20.dp)),
            color = ApexDarkSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(ApexDarkSurfaceHighlight)
                                .border(1.dp, ApexBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = ApexNeonLime,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "MODIFICA CIBO E PACCHI",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = ApexTextPrimary,
                            letterSpacing = 0.8.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Chiudi",
                            tint = ApexTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Name Field
                Text(
                    text = "NOME ALIMENTO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ApexTextMuted,
                    letterSpacing = 0.6.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    colors = nutriTextFieldColors(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Quantity Field
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "QUANTITÀ TOTALE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ApexCyanAccent,
                        letterSpacing = 0.6.sp
                    )
                    Text(
                        text = "Scrivi o tocca un preset",
                        fontSize = 10.5.sp,
                        color = ApexTextMuted
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = editQuantity,
                    onValueChange = { editQuantity = it },
                    placeholder = { Text("es. 200g, 500g, 1 kg...", color = ApexTextMuted, fontSize = 13.sp) },
                    colors = nutriTextFieldColors(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Preset Chips scrollable
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetQuantities.forEach { preset ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (editQuantity == preset) ApexCyanAccent.copy(alpha = 0.2f)
                                    else ApexDarkSurfaceHighlight
                                )
                                .border(
                                    1.dp,
                                    if (editQuantity == preset) ApexCyanAccent else ApexBorder,
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { editQuantity = preset }
                                .padding(horizontal = 9.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = preset,
                                fontSize = 11.sp,
                                fontWeight = if (editQuantity == preset) FontWeight.Bold else FontWeight.Normal,
                                color = if (editQuantity == preset) ApexCyanAccent else ApexTextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Package info section (Pacchi effettivi + Grammatura singolo pacco)
                Text(
                    text = "PACCHI EFFETTIVI & GRAMMATURA",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ApexNeonLime,
                    letterSpacing = 0.6.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Number of packages counter
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ApexDarkSurfaceHighlight,
                        border = BorderStroke(1.dp, ApexBorder),
                        modifier = Modifier.weight(0.9f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ApexBlack)
                                    .clickable {
                                        if (editPackageCount > 1) editPackageCount--
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("-", color = ApexTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$editPackageCount",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = ApexNeonLime
                                )
                                Text(
                                    text = if (editPackageCount == 1) "pacco" else "pacchi",
                                    fontSize = 10.sp,
                                    color = ApexTextMuted
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ApexBlack)
                                    .clickable { editPackageCount++ },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("+", color = ApexNeonLime, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Packaging grammage text field
                    OutlinedTextField(
                        value = editPackageGrammage,
                        onValueChange = { editPackageGrammage = it },
                        placeholder = { Text("es. 250g a pacco", color = ApexTextMuted, fontSize = 11.5.sp) },
                        colors = nutriTextFieldColors(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.1f)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Quick Grammage Preset Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetGrammages.forEach { grammage ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (editPackageGrammage.contains(grammage)) ApexNeonLime.copy(alpha = 0.18f)
                                    else ApexDarkSurfaceHighlight
                                )
                                .border(
                                    1.dp,
                                    if (editPackageGrammage.contains(grammage)) ApexNeonLime else ApexBorder,
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { editPackageGrammage = "$grammage a pacco" }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = grammage,
                                fontSize = 10.5.sp,
                                fontWeight = if (editPackageGrammage.contains(grammage)) FontWeight.Bold else FontWeight.Normal,
                                color = if (editPackageGrammage.contains(grammage)) ApexNeonLime else ApexTextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Notes Field
                Text(
                    text = "NOTE / DETTAGLI (FACOLTATIVO)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ApexTextMuted,
                    letterSpacing = 0.6.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = editNotes,
                    onValueChange = { editNotes = it },
                    placeholder = { Text("es. Senza lattosio, Bio, Taglio magro...", color = ApexTextMuted, fontSize = 13.sp) },
                    colors = nutriTextFieldColors(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, ApexBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = ApexTextSecondary
                        )
                    ) {
                        Text("Annulla", fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            if (editName.isNotBlank()) {
                                onSave(editName, editQuantity, editPackageCount, editPackageGrammage, editNotes)
                            }
                        },
                        enabled = editName.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ApexNeonLime,
                            contentColor = Color.Black
                        )
                    ) {
                        Text("Salva", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun quickAdjust(currentQty: String, direction: Int): String {
    val regex = Regex("(\\d+)(\\s*[a-zA-Z%]+)?")
    val match = regex.find(currentQty) ?: return currentQty
    val num = match.groupValues[1].toIntOrNull() ?: return currentQty
    val unit = match.groupValues[2].ifEmpty { "g" }

    val step = when {
        unit.contains("kg", ignoreCase = true) -> 1
        unit.contains("g", ignoreCase = true) || unit.contains("ml", ignoreCase = true) -> 50
        else -> 1
    }

    val newNum = (num + (step * direction)).coerceAtLeast(1)
    return currentQty.replaceFirst(match.value, "$newNum$unit")
}
