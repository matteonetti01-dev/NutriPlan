package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.outlined.Info
import androidx.core.content.ContextCompat
import com.example.util.CameraUtils
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ai.ImportedAlternative
import com.example.ai.ImportedSlotWithAlternatives
import com.example.data.entity.Ingredient
import com.example.data.entity.MealAlternativeEntity
import com.example.data.entity.MealSlotEntity
import com.example.data.entity.PlanEntity
import com.example.ui.components.AlternativeDetailSheet
import com.example.ui.components.EmptyPlanCard
import com.example.ui.components.MealDialog
import com.example.ui.components.NewPlanDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.NutritionViewModel
import kotlinx.coroutines.launch

private fun getFileNameFromUri(context: android.content.Context, uri: Uri): String {
    var result = "documento"
    try {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    result = it.getString(nameIndex) ?: "documento"
                }
            }
        }
    } catch (e: Exception) {
        // ignore
    }
    return result
}

@Composable
fun PlanScreen(
    viewModel: NutritionViewModel
) {
    val context = LocalContext.current
    val activePlan by viewModel.activePlan.collectAsState()
    val slots by viewModel.activePlanSlots.collectAsState()
    val alternatives by viewModel.activePlanAlternatives.collectAsState()

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // State for expanded slots (default slot 1 is expanded)
    val expandedSlots = remember {
        mutableStateMapOf<Long, Boolean>()
    }

    var showNewPlanDialog by remember { mutableStateOf(false) }
    var addingToSlot by remember { mutableStateOf<MealSlotEntity?>(null) }
    var slotToEdit by remember { mutableStateOf<MealSlotEntity?>(null) }
    var selectedAlternativeForDetail by remember { mutableStateOf<MealAlternativeEntity?>(null) }

    // States for Document Import with AI into a specific meal slot
    var slotForDocImport by remember { mutableStateOf<MealSlotEntity?>(null) }
    var isScanningDoc by remember { mutableStateOf(false) }
    var scanningFileName by remember { mutableStateOf("") }
    var scanResultAlternatives by remember { mutableStateOf<List<ImportedAlternative>?>(null) }
    var scanErrorMessage by remember { mutableStateOf<String?>(null) }

    // States for Full-Plan Document Import with AI (all meals mapped)
    var isScanningFullPlanDoc by remember { mutableStateOf(false) }
    var scanningFullPlanFileName by remember { mutableStateOf("") }
    var fullPlanScanResults by remember { mutableStateOf<List<ImportedSlotWithAlternatives>?>(null) }

    val fullPlanDocumentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = getFileNameFromUri(context, uri)
            scanningFullPlanFileName = fileName
            isScanningFullPlanDoc = true
            fullPlanScanResults = null
            scanErrorMessage = null

            scope.launch {
                try {
                    val extracted = viewModel.geminiService.extractAllPlanAlternativesFromDocument(
                        uri = uri,
                        availableSlots = slots
                    )
                    isScanningFullPlanDoc = false
                    val totalExtracted = extracted.sumOf { it.alternatives.size }
                    if (totalExtracted > 0) {
                        fullPlanScanResults = extracted
                    } else {
                        scanErrorMessage = "Nessun pasto o alternativa identificata nel file. Assicurati che il file contenga pasti con alimenti e quantità."
                    }
                } catch (e: Exception) {
                    isScanningFullPlanDoc = false
                    scanErrorMessage = "Errore durante l'analisi del documento del piano: ${e.localizedMessage ?: "Errore sconosciuto"}"
                }
            }
        }
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val targetSlot = slotForDocImport
            if (targetSlot != null) {
                val fileName = getFileNameFromUri(context, uri)
                scanningFileName = fileName
                isScanningDoc = true
                scanResultAlternatives = null
                scanErrorMessage = null

                scope.launch {
                    try {
                        val extracted = viewModel.geminiService.extractAlternativesFromDocument(
                            uri = uri,
                            mealSlotName = targetSlot.name
                        )
                        isScanningDoc = false
                        if (extracted.isNotEmpty()) {
                            scanResultAlternatives = extracted
                        } else {
                            scanErrorMessage = "Nessuna alternativa trovata nel file per il pasto '${targetSlot.name}'. Assicurati che il file contenga informazioni su alimenti e grammature."
                        }
                    } catch (e: Exception) {
                        isScanningDoc = false
                        scanErrorMessage = "Impossibile leggere il documento: ${e.localizedMessage ?: "Errore sconosciuto"}"
                    }
                }
            }
        } else {
            slotForDocImport = null
        }
    }

    var tempFullPlanPhotoUri by remember { mutableStateOf<Uri?>(null) }
    val fullPlanCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempFullPlanPhotoUri != null) {
            val photoUri = tempFullPlanPhotoUri!!
            scanningFullPlanFileName = "Foto piano scattata"
            isScanningFullPlanDoc = true
            fullPlanScanResults = null
            scanErrorMessage = null

            scope.launch {
                try {
                    val extracted = viewModel.geminiService.extractAllPlanAlternativesFromDocument(
                        uri = photoUri,
                        availableSlots = slots
                    )
                    isScanningFullPlanDoc = false
                    val totalExtracted = extracted.sumOf { it.alternatives.size }
                    if (totalExtracted > 0) {
                        fullPlanScanResults = extracted
                    } else {
                        scanErrorMessage = "Nessun pasto o alternativa identificata nella foto. Assicurati che il foglio sia ben illuminato e leggibile."
                    }
                } catch (e: Exception) {
                    isScanningFullPlanDoc = false
                    scanErrorMessage = "Errore durante l'analisi della foto del piano: ${e.localizedMessage ?: "Errore sconosciuto"}"
                }
            }
        }
    }

    val fullPlanCameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                val uri = CameraUtils.createTempImageUri(context)
                tempFullPlanPhotoUri = uri
                fullPlanCameraLauncher.launch(uri)
            } catch (e: Exception) {
                fullPlanDocumentPickerLauncher.launch(arrayOf("*/*"))
            }
        } else {
            fullPlanDocumentPickerLauncher.launch(arrayOf("*/*"))
        }
    }

    fun launchCameraForFullPlan() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            try {
                val uri = CameraUtils.createTempImageUri(context)
                tempFullPlanPhotoUri = uri
                fullPlanCameraLauncher.launch(uri)
            } catch (e: Exception) {
                fullPlanDocumentPickerLauncher.launch(arrayOf("*/*"))
            }
        } else {
            fullPlanCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var tempSlotPhotoUri by remember { mutableStateOf<Uri?>(null) }
    val slotCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempSlotPhotoUri != null) {
            val photoUri = tempSlotPhotoUri!!
            val targetSlot = slotForDocImport
            if (targetSlot != null) {
                scanningFileName = "Foto pasto scattata"
                isScanningDoc = true
                scanResultAlternatives = null
                scanErrorMessage = null

                scope.launch {
                    try {
                        val extracted = viewModel.geminiService.extractAlternativesFromDocument(
                            uri = photoUri,
                            mealSlotName = targetSlot.name
                        )
                        isScanningDoc = false
                        if (extracted.isNotEmpty()) {
                            scanResultAlternatives = extracted
                        } else {
                            scanErrorMessage = "Nessuna alternativa trovata nella foto per il pasto '${targetSlot.name}'."
                        }
                    } catch (e: Exception) {
                        isScanningDoc = false
                        scanErrorMessage = "Impossibile leggere la foto: ${e.localizedMessage ?: "Errore sconosciuto"}"
                    }
                }
            }
        }
    }

    val slotCameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                val uri = CameraUtils.createTempImageUri(context)
                tempSlotPhotoUri = uri
                slotCameraLauncher.launch(uri)
            } catch (e: Exception) {
                documentPickerLauncher.launch(arrayOf("*/*"))
            }
        } else {
            documentPickerLauncher.launch(arrayOf("*/*"))
        }
    }

    fun launchCameraForSlot(slot: MealSlotEntity) {
        slotForDocImport = slot
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            try {
                val uri = CameraUtils.createTempImageUri(context)
                tempSlotPhotoUri = uri
                slotCameraLauncher.launch(uri)
            } catch (e: Exception) {
                documentPickerLauncher.launch(arrayOf("*/*"))
            }
        } else {
            slotCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val currentPlan = activePlan

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(NutriBgLight)
                .padding(horizontal = 20.dp)
                .testTag("plan_screen")
        ) {
            item {
                Spacer(modifier = Modifier.height(14.dp))

                // APEX // AI Global Top Header
                com.example.ui.components.ApexHeader()

                Spacer(modifier = Modifier.height(16.dp))

                // Plan Name and Targets
                activePlan?.let { plan ->
                    Text(
                        text = plan.name,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = NutriTextPrimary,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${plan.caloriesTarget} kcal • P ${plan.proteinTarget}g • C ${plan.carbsTarget}g • G ${plan.fatTarget}g",
                        fontSize = 13.5.sp,
                        color = NutriTextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Compact AI Diet Import Card (APEX Style)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, com.example.ui.theme.ApexBorder, RoundedCornerShape(14.dp))
                            .testTag("import_full_plan_doc_button"),
                        shape = RoundedCornerShape(14.dp),
                        color = com.example.ui.theme.ApexDarkSurface
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(com.example.ui.theme.ApexDarkSurfaceHighlight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = com.example.ui.theme.ApexNeonLime,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Scansiona Dieta con AI",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = com.example.ui.theme.ApexTextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Estrai automaticamente pasti e alternative da foto o PDF.",
                                        fontSize = 11.5.sp,
                                        color = com.example.ui.theme.ApexTextSecondary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Dual Action Buttons: Scatta Foto (Camera) & Carica File
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { launchCameraForFullPlan() },
                                    modifier = Modifier
                                        .weight(1.1f)
                                        .height(38.dp)
                                        .testTag("full_plan_camera_button"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = com.example.ui.theme.ApexNeonLime,
                                        contentColor = com.example.ui.theme.ApexBlack
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoCamera,
                                        contentDescription = null,
                                        tint = com.example.ui.theme.ApexBlack,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Scatta foto", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = com.example.ui.theme.ApexBlack)
                                }

                                OutlinedButton(
                                    onClick = {
                                        try {
                                            fullPlanDocumentPickerLauncher.launch(
                                                arrayOf(
                                                    "application/pdf",
                                                    "image/*",
                                                    "text/*",
                                                    "*/*"
                                                )
                                            )
                                        } catch (e: Exception) {
                                            fullPlanDocumentPickerLauncher.launch(arrayOf("*/*"))
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(0.9f)
                                        .height(38.dp)
                                        .testTag("full_plan_file_button"),
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.ApexBorder),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = com.example.ui.theme.ApexDarkSurfaceHighlight,
                                        contentColor = com.example.ui.theme.ApexTextPrimary
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.UploadFile,
                                        contentDescription = null,
                                        tint = com.example.ui.theme.ApexTextSecondary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Carica file", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = com.example.ui.theme.ApexTextPrimary)
                                }
                            }
                        }
                    }
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
                        },
                        onTakePhoto = {
                            launchCameraForSlot(slot)
                        },
                        onImportDocument = {
                            slotForDocImport = slot
                            try {
                                documentPickerLauncher.launch(
                                    arrayOf(
                                        "application/pdf",
                                        "image/*",
                                        "text/*",
                                        "*/*"
                                    )
                                )
                            } catch (e: Exception) {
                                documentPickerLauncher.launch(arrayOf("*/*"))
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                }
            } ?: run {
                item {
                    EmptyPlanCard(
                        onCreatePlanClick = { showNewPlanDialog = true }
                    )
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

    // Modal progress dialog during AI document scanning
    if (isScanningDoc) {
        val targetName = slotForDocImport?.name ?: "Pasto"
        ScanningDocumentDialog(
            fileName = scanningFileName,
            slotName = targetName
        )
    }

    // Modal progress dialog during AI full-plan scanning
    if (isScanningFullPlanDoc) {
        ScanningFullPlanDocumentDialog(
            fileName = scanningFullPlanFileName,
            mealsCount = slots.size
        )
    }

    // Dialog showing all alternatives extracted across the entire plan
    fullPlanScanResults?.let { results ->
        ImportedFullPlanPreviewDialog(
            fileName = scanningFullPlanFileName,
            results = results,
            onDismiss = {
                fullPlanScanResults = null
            },
            onConfirmAddAll = {
                var totalAdded = 0
                results.forEach { slotGroup ->
                    val matchedSlot = slots.find { it.id == slotGroup.slotId }
                    if (matchedSlot != null) {
                        slotGroup.alternatives.forEach { alt ->
                            viewModel.addDirectAlternative(
                                slotId = matchedSlot.id,
                                planId = matchedSlot.planId,
                                name = alt.name,
                                calories = alt.totalCalories,
                                protein = alt.totalProtein,
                                carbs = alt.totalCarbs,
                                fat = alt.totalFat,
                                notes = alt.notes,
                                ingredients = alt.ingredients
                            )
                            totalAdded++
                        }
                    }
                }
                fullPlanScanResults = null
                scope.launch {
                    snackbarHostState.showSnackbar("✓ Inserite con successo $totalAdded alternative nei rispettivi pasti del piano!")
                }
            }
        )
    }

    // Dialog showing alternatives extracted by AI from the document for a single meal slot
    val pendingAlternatives = scanResultAlternatives
    val currentImportSlot = slotForDocImport
    if (pendingAlternatives != null && currentImportSlot != null) {
        ImportedAlternativesPreviewDialog(
            slotName = currentImportSlot.name,
            fileName = scanningFileName,
            alternatives = pendingAlternatives,
            onDismiss = {
                scanResultAlternatives = null
                slotForDocImport = null
            },
            onConfirmAdd = {
                val count = pendingAlternatives.size
                val targetSlotName = currentImportSlot.name
                pendingAlternatives.forEach { alt ->
                    viewModel.addDirectAlternative(
                        slotId = currentImportSlot.id,
                        planId = currentImportSlot.planId,
                        name = alt.name,
                        calories = alt.totalCalories,
                        protein = alt.totalProtein,
                        carbs = alt.totalCarbs,
                        fat = alt.totalFat,
                        notes = alt.notes,
                        ingredients = alt.ingredients
                    )
                }
                scanResultAlternatives = null
                slotForDocImport = null
                scope.launch {
                    snackbarHostState.showSnackbar("✓ Aggiunte $count alternative a $targetSlotName!")
                }
            }
        )
    }

    // Error or warning dialog if document could not be read or had no alternatives
    scanErrorMessage?.let { err ->
        ScanErrorDialog(
            errorMessage = err,
            onDismiss = {
                scanErrorMessage = null
                slotForDocImport = null
            }
        )
    }

    if (showNewPlanDialog) {
        NewPlanDialog(
            onDismiss = { showNewPlanDialog = false },
            onConfirm = { name, cal, prot, c, f, meals ->
                viewModel.createPlan(name, cal, prot, c, f, meals, makeActive = true)
                showNewPlanDialog = false
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
    onDeleteAlternative: (MealAlternativeEntity) -> Unit,
    onTakePhoto: () -> Unit,
    onImportDocument: () -> Unit
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
                    // Quick camera photo button for this meal slot
                    IconButton(
                        onClick = onTakePhoto,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("take_photo_header_${slot.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Scatta foto con fotocamera",
                            tint = com.example.ui.theme.ApexNeonLime,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Quick import document button for this meal slot
                    IconButton(
                        onClick = onImportDocument,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("import_doc_header_${slot.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.UploadFile,
                            contentDescription = "Importa da archivio con AI",
                            tint = NutriTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

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

                    // Primary "+ Aggiungi alternativa" Button matching Mockup Phone 2
                    OutlinedButton(
                        onClick = { onAddAlternative() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .testTag("add_alternative_${slot.id}"),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, com.example.ui.theme.ApexNeonLime),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = com.example.ui.theme.ApexDarkSurfaceHighlight,
                            contentColor = com.example.ui.theme.ApexNeonLime
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = com.example.ui.theme.ApexNeonLime,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Aggiungi alternativa",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = com.example.ui.theme.ApexNeonLime
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Secondary Quick Actions: "📷 Scatta foto" and "📄 Archivio"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // "📷 Scatta foto" Button
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onTakePhoto() }
                                .testTag("take_photo_btn_${slot.id}"),
                            shape = RoundedCornerShape(10.dp),
                            color = com.example.ui.theme.ApexDarkSurfaceHighlight,
                            border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.ApexBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 9.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = null,
                                    tint = com.example.ui.theme.ApexNeonLime,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Scatta foto",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = com.example.ui.theme.ApexTextPrimary
                                )
                            }
                        }

                        // "📄 Archivio" Button
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onImportDocument() }
                                .testTag("import_doc_btn_${slot.id}"),
                            shape = RoundedCornerShape(10.dp),
                            color = com.example.ui.theme.ApexDarkSurfaceHighlight,
                            border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.ApexBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 9.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.UploadFile,
                                    contentDescription = null,
                                    tint = NutriTextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Archivio",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = NutriTextPrimary
                                )
                            }
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
            .border(1.dp, com.example.ui.theme.ApexBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag("alternative_item_${alternative.id}"),
        color = com.example.ui.theme.ApexDarkSurfaceHighlight
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
                        .background(Color(0xFF1C2B14))
                        .testTag("log_alternative_${alternative.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Segna come mangiato oggi",
                        tint = com.example.ui.theme.ApexNeonLime,
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
            color = ApexDarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, ApexBorder)
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
                        color = ApexTextPrimary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Chiudi",
                            tint = ApexTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Personalizza nome, calorie e macronutrienti target per questo pasto.",
                    fontSize = 12.sp,
                    color = ApexTextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Nome pasto
                Text(
                    text = "Nome del pasto",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ApexTextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = nutriTextFieldColors(),
                    textStyle = TextStyle(color = ApexTextPrimary, fontSize = 14.sp),
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
                            color = ApexTextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = calories,
                            onValueChange = { calories = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = nutriTextFieldColors(),
                            textStyle = TextStyle(color = ApexTextPrimary, fontSize = 14.sp),
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
                            color = ApexTextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = protein,
                            onValueChange = { protein = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = nutriTextFieldColors(),
                            textStyle = TextStyle(color = ApexTextPrimary, fontSize = 14.sp),
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
                            color = ApexTextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = carbs,
                            onValueChange = { carbs = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = nutriTextFieldColors(),
                            textStyle = TextStyle(color = ApexTextPrimary, fontSize = 14.sp),
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
                            color = ApexTextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = fat,
                            onValueChange = { fat = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = nutriTextFieldColors(),
                            textStyle = TextStyle(color = ApexTextPrimary, fontSize = 14.sp),
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
                            .background(Color(0xFF251C08))
                            .border(1.dp, Color(0xFF5E491B), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = null,
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Somma pasti vs totale piano",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFFDE68A)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Kcal totali con questa modifica: $totalCalPreview / ${plan.caloriesTarget} (${if (calDiff > 0) "+$calDiff" else "$calDiff"} kcal)\nPuoi comunque salvare e continuare a usare l'app.",
                                fontSize = 11.5.sp,
                                color = ApexTextSecondary,
                                lineHeight = 16.sp
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0D2818))
                            .border(1.dp, Color(0xFF1B5E20), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = ApexGreen,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Perfetto! La somma dei pasti coincide con l'obiettivo (${plan.caloriesTarget} kcal).",
                                fontSize = 11.5.sp,
                                color = ApexGreen
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
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ApexBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = ApexTextSecondary
                        )
                    ) {
                        Text("Annulla", color = ApexTextSecondary)
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
                            containerColor = ApexNeonLime,
                            contentColor = ApexBlack
                        )
                    ) {
                        Text("Salva", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanningDocumentDialog(
    fileName: String,
    slotName: String
) {
    Dialog(onDismissRequest = { /* Modal while analyzing */ }) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ApexDarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, ApexBorder),
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(ApexNeonLime.copy(alpha = 0.15f))
                        .border(1.dp, ApexNeonLime.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = ApexNeonLime,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Scansione con AI in corso",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ApexTextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Pasto di destinazione: $slotName",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ApexCyanAccent,
                    textAlign = TextAlign.Center
                )

                if (fileName.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "File: $fileName",
                        fontSize = 12.sp,
                        color = ApexTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                CircularProgressIndicator(
                    color = ApexNeonLime,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(36.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "L'AI sta analizzando il documento ed estraendo le varie alternative salvate con alimenti, porzioni e valori nutrizionali...",
                    fontSize = 12.sp,
                    color = ApexTextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

@Composable
private fun ImportedAlternativesPreviewDialog(
    slotName: String,
    fileName: String,
    alternatives: List<ImportedAlternative>,
    onDismiss: () -> Unit,
    onConfirmAdd: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ApexDarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, ApexBorder),
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
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
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(ApexNeonLime.copy(alpha = 0.15f))
                                .border(1.dp, ApexNeonLime.copy(alpha = 0.35f), CircleShape),
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
                                text = "Alternative trovate (${alternatives.size})",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = ApexTextPrimary
                            )
                            Text(
                                text = "Pasto: $slotName",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = ApexCyanAccent
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Chiudi",
                            tint = ApexTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "I seguenti pasti sono stati estratti dal file \"$fileName\". Verranno aggiunti a \"$slotName\" senza eliminare le alternative esistenti.",
                    fontSize = 12.sp,
                    color = ApexTextSecondary,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable list of extracted alternatives
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    alternatives.forEach { alt ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = ApexDarkSurfaceHighlight,
                            border = androidx.compose.foundation.BorderStroke(1.dp, ApexBorder)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = alt.name,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ApexTextPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${alt.totalCalories} kcal",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ApexCalories
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Macros tag
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "P: ${alt.totalProtein}g",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ApexProtein
                                    )
                                    Text(
                                        text = "•",
                                        fontSize = 10.sp,
                                        color = ApexBorder
                                    )
                                    Text(
                                        text = "C: ${alt.totalCarbs}g",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ApexCarbs
                                    )
                                    Text(
                                        text = "•",
                                        fontSize = 10.sp,
                                        color = ApexBorder
                                    )
                                    Text(
                                        text = "G: ${alt.totalFat}g",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ApexFats
                                    )
                                }

                                if (alt.ingredients.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = alt.ingredients.joinToString(", ") { "${it.name} (${it.quantity})" },
                                        fontSize = 11.sp,
                                        color = ApexTextSecondary,
                                        maxLines = 3
                                    )
                                }

                                if (alt.notes.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Note: ${alt.notes}",
                                        fontSize = 11.sp,
                                        color = ApexTextMuted,
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ApexBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = ApexTextSecondary
                        )
                    ) {
                        Text("Annulla", fontSize = 13.sp)
                    }

                    Button(
                        onClick = onConfirmAdd,
                        modifier = Modifier
                            .weight(1.3f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ApexNeonLime,
                            contentColor = ApexBlack
                        )
                    ) {
                        Text(
                            text = "Aggiungi (${alternatives.size})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanErrorDialog(
    errorMessage: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ApexDarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, ApexBorder),
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2C0D0E))
                        .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = Color(0xFFFF4D4D),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Importazione documento",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = ApexTextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = errorMessage,
                    fontSize = 13.sp,
                    color = ApexTextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ApexNeonLime,
                        contentColor = ApexBlack
                    )
                ) {
                    Text("Ho capito", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ScanningFullPlanDocumentDialog(
    fileName: String,
    mealsCount: Int
) {
    Dialog(onDismissRequest = { /* Modal while analyzing */ }) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ApexDarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, ApexBorder),
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(ApexNeonLime.copy(alpha = 0.15f))
                        .border(1.dp, ApexNeonLime.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = ApexNeonLime,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Scansione Intero Piano",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ApexTextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Analisi e associazione automatica a $mealsCount pasti",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ApexCyanAccent,
                    textAlign = TextAlign.Center
                )

                if (fileName.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "File: $fileName",
                        fontSize = 12.sp,
                        color = ApexTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                CircularProgressIndicator(
                    color = ApexNeonLime,
                    strokeWidth = 3.5.dp,
                    modifier = Modifier.size(38.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "L'AI sta leggendo il documento completo, individuando tutte le alternative per ogni momento della giornata (colazione, spuntini, pranzo, cena) e calcolando i relativi valori nutrizionali...",
                    fontSize = 12.sp,
                    color = ApexTextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

@Composable
private fun ImportedFullPlanPreviewDialog(
    fileName: String,
    results: List<ImportedSlotWithAlternatives>,
    onDismiss: () -> Unit,
    onConfirmAddAll: () -> Unit
) {
    val totalAlternatives = results.sumOf { it.alternatives.size }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ApexDarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, ApexBorder),
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                // Header
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
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(ApexNeonLime.copy(alpha = 0.15f))
                                .border(1.dp, ApexNeonLime.copy(alpha = 0.35f), CircleShape),
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
                                text = "Piano Generale Estratto",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = ApexTextPrimary
                            )
                            Text(
                                text = "$totalAlternatives alternative in ${results.size} pasti",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = ApexCyanAccent
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Chiudi",
                            tint = ApexTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "L'AI ha distribuito le opzioni del file \"$fileName\" nei rispettivi pasti del tuo piano. Le nuove alternative si aggiungeranno a quelle esistenti.",
                    fontSize = 12.sp,
                    color = ApexTextSecondary,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable container grouped by meal slot
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    results.forEach { slotGroup ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(ApexDarkSurfaceHighlight)
                                .border(1.dp, ApexBorder, RoundedCornerShape(14.dp))
                                .padding(12.dp)
                        ) {
                            // Meal Slot Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(ApexNeonLime.copy(alpha = 0.15f))
                                            .border(0.8.dp, ApexNeonLime.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "Pasto ${slotGroup.slotOrderIndex}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ApexNeonLime
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = slotGroup.slotName,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ApexTextPrimary
                                    )
                                }
                                Text(
                                    text = "${slotGroup.alternatives.size} alternative",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = ApexTextSecondary
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Alternatives for this meal slot
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                slotGroup.alternatives.forEach { alt ->
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        color = ApexDarkSurface,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, ApexBorder)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = alt.name,
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = ApexTextPrimary,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Text(
                                                    text = "${alt.totalCalories} kcal",
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = ApexCalories
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))

                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "P: ${alt.totalProtein}g",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = ApexProtein
                                                )
                                                Text(
                                                    text = "•",
                                                    fontSize = 9.sp,
                                                    color = ApexBorder
                                                )
                                                Text(
                                                    text = "C: ${alt.totalCarbs}g",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = ApexCarbs
                                                )
                                                Text(
                                                    text = "•",
                                                    fontSize = 9.sp,
                                                    color = ApexBorder
                                                )
                                                Text(
                                                    text = "G: ${alt.totalFat}g",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = ApexFats
                                                )
                                            }

                                            if (alt.ingredients.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = alt.ingredients.joinToString(", ") { "${it.name} (${it.quantity})" },
                                                    fontSize = 10.5.sp,
                                                    color = ApexTextSecondary,
                                                    maxLines = 2
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ApexBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = ApexTextSecondary
                        )
                    ) {
                        Text("Annulla", color = ApexTextSecondary, fontSize = 13.sp)
                    }

                    Button(
                        onClick = onConfirmAddAll,
                        modifier = Modifier
                            .weight(1.4f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ApexNeonLime,
                            contentColor = ApexBlack
                        )
                    ) {
                        Text(
                            text = "Aggiungi tutte ($totalAlternatives)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
