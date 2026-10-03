package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.repository.FoodDatabaseRepository
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
import kotlinx.coroutines.launch

@Composable
fun FatSecretConfigDialog(
    repository: FoodDatabaseRepository,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()

    val currentId by repository.clientId.collectAsState()
    val currentSecret by repository.clientSecret.collectAsState()
    val isConfigured by repository.isConfigured.collectAsState()

    var inputClientId by remember(currentId) { mutableStateOf(currentId) }
    var inputClientSecret by remember(currentSecret) { mutableStateOf(currentSecret) }

    var isTesting by remember { mutableStateOf(false) }
    var testSuccessMessage by remember { mutableStateOf<String?>(null) }
    var testErrorMessage by remember { mutableStateOf<String?>(null) }
    var isSavedNotification by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .clip(RoundedCornerShape(22.dp))
                .border(1.dp, ApexBorder, RoundedCornerShape(22.dp)),
            color = ApexDarkSurface
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
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
                                .background(Color(0xFF1B3822)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                tint = ApexNeonLime,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Database FatSecret",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = ApexTextPrimary
                            )
                            Text(
                                text = "Platform REST API 2.0",
                                fontSize = 11.5.sp,
                                color = ApexNeonLime
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Chiudi", tint = ApexTextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Status Banner
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            1.dp,
                            if (isConfigured) Color(0xFF22542B) else Color(0xFF3A3420),
                            RoundedCornerShape(12.dp)
                        ),
                    color = if (isConfigured) Color(0xFF142918) else Color(0xFF262013)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isConfigured) ApexNeonLime else Color(0xFFFFB300))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isConfigured) "Stato: API FatSecret Attiva" else "Stato: Modalità Locale Attiva",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isConfigured) ApexNeonLime else Color(0xFFFFB300)
                            )
                            Text(
                                text = if (isConfigured) "Ricerche collegate in tempo reale a milioni di alimenti certificati e marche italiane."
                                else "Usa il database interno certificato di 120+ alimenti base. Inserisci le tue chiavi per espandere il catalogo.",
                                fontSize = 11.sp,
                                color = ApexTextSecondary,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Input Client ID
                Text(
                    text = "FatSecret Client ID",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ApexTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = inputClientId,
                    onValueChange = {
                        inputClientId = it
                        testSuccessMessage = null
                        testErrorMessage = null
                    },
                    placeholder = { Text("es. 4f9b2d8e...", color = ApexTextMuted, fontSize = 13.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("fatsecret_client_id_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = nutriTextFieldColors(),
                    textStyle = TextStyle(color = ApexTextPrimary, fontSize = 13.sp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Input Client Secret
                Text(
                    text = "FatSecret Client Secret",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ApexTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = inputClientSecret,
                    onValueChange = {
                        inputClientSecret = it
                        testSuccessMessage = null
                        testErrorMessage = null
                    },
                    placeholder = { Text("es. a1b2c3d4e5...", color = ApexTextMuted, fontSize = 13.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("fatsecret_client_secret_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = nutriTextFieldColors(),
                    textStyle = TextStyle(color = ApexTextPrimary, fontSize = 13.sp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Feedback Messages
                if (testSuccessMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF142918))
                            .border(1.dp, Color(0xFF22542B), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = testSuccessMessage!!,
                            fontSize = 12.sp,
                            color = ApexNeonLime,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (testErrorMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF331616))
                            .border(1.dp, Color(0xFF6B2525), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = testErrorMessage!!,
                            fontSize = 12.sp,
                            color = Color(0xFFFF6B6B),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (isSavedNotification) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF132A3B))
                            .border(1.dp, ApexCyanAccent, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "Credenziali salvate con successo!",
                            fontSize = 12.sp,
                            color = ApexCyanAccent,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Action Buttons: Testa Connessione & Salva
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                isTesting = true
                                testSuccessMessage = null
                                testErrorMessage = null
                                isSavedNotification = false

                                val result = repository.testConnection(inputClientId, inputClientSecret)
                                if (result.isSuccess) {
                                    testSuccessMessage = result.getOrNull()
                                } else {
                                    testErrorMessage = result.exceptionOrNull()?.message ?: "Errore di connessione"
                                }
                                isTesting = false
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("test_fatsecret_connection_button"),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, ApexBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ApexTextPrimary),
                        enabled = !isTesting && inputClientId.isNotBlank() && inputClientSecret.isNotBlank()
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = ApexNeonLime, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp), tint = ApexNeonLime)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Testa Connessione", fontSize = 12.sp)
                        }
                    }

                    Button(
                        onClick = {
                            repository.saveCredentials(inputClientId, inputClientSecret)
                            isSavedNotification = true
                            testSuccessMessage = null
                            testErrorMessage = null
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("save_fatsecret_credentials_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ApexNeonLime,
                            contentColor = ApexBlack
                        )
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Salva", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Info footer
                Text(
                    text = "Nota: Le credenziali possono essere salvate anche nel file .env (FATSECRET_CLIENT_ID e FATSECRET_CLIENT_SECRET) tramite il Secrets Panel.",
                    fontSize = 11.sp,
                    color = ApexTextMuted,
                    lineHeight = 15.sp
                )
            }
        }
    }
}
