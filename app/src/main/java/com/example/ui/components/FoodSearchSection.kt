package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.data.entity.FoodItem
import com.example.data.entity.Ingredient
import com.example.data.repository.FoodDatabaseRepository
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
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FoodSearchSection(
    repository: FoodDatabaseRepository,
    onIngredientSelected: (Ingredient) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<FoodItem>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var selectedFood by remember { mutableStateOf<FoodItem?>(null) }
    var customQuantityStr by remember { mutableStateOf("100g") }
    var currentGrams by remember { mutableDoubleStateOf(100.0) }

    val isConfigured by repository.isConfigured.collectAsState()

    // Debounced search
    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) {
            searchResults = emptyList()
            isSearching = false
            return@LaunchedEffect
        }
        isSearching = true
        delay(250) // Debounce
        repository.searchFoods(searchQuery).collect { list ->
            searchResults = list
            isSearching = false
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Source indicator badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Database Alimenti Certificato",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = ApexTextSecondary
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isConfigured) ApexNeonLime else ApexCyanAccent)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isConfigured) "FatSecret Live" else "Database Locale Certificato",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isConfigured) ApexNeonLime else ApexCyanAccent
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Search input bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                if (selectedFood != null && it != selectedFood?.displayName) {
                    selectedFood = null
                }
            },
            placeholder = {
                Text(
                    text = if (isConfigured) "Cerca in FatSecret (es. pasta, petto di pollo, fage)..."
                    else "Cerca alimento (es. pasta, riso, pollo, mela)...",
                    color = ApexTextMuted,
                    fontSize = 13.5.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = if (isConfigured) ApexNeonLime else ApexCyanAccent,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (isSearching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = ApexNeonLime
                    )
                } else if (searchQuery.isNotBlank()) {
                    IconButton(
                        onClick = {
                            searchQuery = ""
                            selectedFood = null
                            searchResults = emptyList()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Cancella",
                            tint = ApexTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("food_database_search_input"),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = nutriTextFieldColors(),
            textStyle = TextStyle(color = ApexTextPrimary, fontSize = 14.sp)
        )

        // Dropdown Search Results
        AnimatedVisibility(
            visible = searchResults.isNotEmpty() && selectedFood == null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, ApexBorder, RoundedCornerShape(12.dp)),
                color = ApexDarkSurfaceHighlight
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp)
                        .padding(vertical = 4.dp)
                ) {
                    items(searchResults) { food ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedFood = food
                                    searchQuery = food.displayName
                                    customQuantityStr = "100g"
                                    currentGrams = 100.0
                                    searchResults = emptyList()
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = food.name,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ApexTextPrimary
                                    )
                                    if (!food.brand.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = food.brand,
                                            fontSize = 11.sp,
                                            color = ApexTextMuted,
                                            fontWeight = FontWeight.Normal
                                        )
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${food.caloriesPer100g.toInt()} kcal",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ApexCalories
                                    )
                                    Text("•", fontSize = 9.sp, color = ApexBorder)
                                    Text("P ${food.proteinPer100g.toInt()}g", fontSize = 11.sp, color = ApexProtein)
                                    Text("•", fontSize = 9.sp, color = ApexBorder)
                                    Text("C ${food.carbsPer100g.toInt()}g", fontSize = 11.sp, color = ApexCarbs)
                                    Text("•", fontSize = 9.sp, color = ApexBorder)
                                    Text("G ${food.fatPer100g.toInt()}g", fontSize = 11.sp, color = ApexFats)
                                    Text("(su 100g)", fontSize = 10.sp, color = ApexTextMuted)
                                }
                            }

                            // Source Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (food.source == "fatsecret") Color(0xFF1B3822)
                                        else Color(0xFF132A3B)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (food.source == "fatsecret") "FatSecret" else "Certificato",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (food.source == "fatsecret") ApexNeonLime else ApexCyanAccent
                                )
                            }
                        }
                    }
                }
            }
        }

        // Selected Food Configuration Card (Grammage & Macro Scaling)
        if (selectedFood != null) {
            val food = selectedFood!!
            val calculated = food.calculateForGrams(currentGrams)

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, ApexNeonLime.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                color = ApexDarkSurfaceHighlight
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = food.displayName,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = ApexTextPrimary
                            )
                            Text(
                                text = "Fonte: ${if (food.source == "fatsecret") "FatSecret Platform API" else "Database Certificato"}",
                                fontSize = 11.sp,
                                color = if (food.source == "fatsecret") ApexNeonLime else ApexCyanAccent
                            )
                        }

                        IconButton(
                            onClick = { selectedFood = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Deseleziona", tint = ApexTextMuted)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick portion chips
                    Text(
                        text = "Seleziona grammatura rapida o digita:",
                        fontSize = 11.sp,
                        color = ApexTextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val portions = listOf("50g", "80g", "100g", "120g", "150g", "200g", "250g")
                        portions.forEach { portion ->
                            val isSelected = customQuantityStr == portion
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) ApexNeonLime else ApexDarkSurface)
                                    .border(1.dp, if (isSelected) ApexNeonLime else ApexBorder, RoundedCornerShape(8.dp))
                                    .clickable {
                                        customQuantityStr = portion
                                        currentGrams = FoodItem.parseQuantityToGrams(portion)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = portion,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) ApexBlack else ApexTextPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Manual grams input
                    OutlinedTextField(
                        value = customQuantityStr,
                        onValueChange = {
                            customQuantityStr = it
                            currentGrams = FoodItem.parseQuantityToGrams(it)
                        },
                        placeholder = { Text("es. 80g", color = ApexTextMuted, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = nutriTextFieldColors(),
                        textStyle = TextStyle(color = ApexTextPrimary, fontSize = 13.5.sp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Live exact macro values card
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, ApexBorder, RoundedCornerShape(10.dp)),
                        color = ApexDarkSurface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${calculated.calories}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ApexCalories
                                )
                                Text("kcal", fontSize = 10.sp, color = ApexTextMuted)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${calculated.protein}g",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ApexProtein
                                )
                                Text("Proteine", fontSize = 10.sp, color = ApexTextMuted)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${calculated.carbs}g",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ApexCarbs
                                )
                                Text("Carbo", fontSize = 10.sp, color = ApexTextMuted)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${calculated.fat}g",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ApexFats
                                )
                                Text("Grassi", fontSize = 10.sp, color = ApexTextMuted)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Add button with exact macros
                    Button(
                        onClick = {
                            val ingredient = food.toIngredient(customQuantityStr)
                            onIngredientSelected(ingredient)
                            selectedFood = null
                            searchQuery = ""
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("add_exact_macro_food_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ApexNeonLime,
                            contentColor = ApexBlack
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Aggiungi con Macro Esatti",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
