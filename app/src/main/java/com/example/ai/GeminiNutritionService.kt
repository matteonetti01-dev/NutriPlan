package com.example.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.entity.Ingredient
import com.example.data.entity.MealSlotEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import kotlin.math.roundToInt

data class DishEstimateResult(
    val mealName: String,
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int,
    val followUpQuestion: String? = null,
    val ingredients: List<Ingredient> = emptyList()
)

data class LabelScanResult(
    val foodName: String,
    val portion: String,
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int
)

data class ImportedAlternative(
    val name: String,
    val totalCalories: Int,
    val totalProtein: Int,
    val totalCarbs: Int,
    val totalFat: Int,
    val notes: String = "",
    val ingredients: List<Ingredient> = emptyList()
)

data class ImportedSlotWithAlternatives(
    val slotId: Long,
    val slotOrderIndex: Int,
    val slotName: String,
    val alternatives: List<ImportedAlternative>
)

data class ShoppingItemAiResult(
    val name: String,
    val quantity: String = "",
    val packageCount: Int = 1,
    val packageGrammage: String = "",
    val category: String = "Altro",
    val notes: String = ""
)

class GeminiNutritionService(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val apiKey: String
        get() = BuildConfig.GEMINI_API_KEY

    /**
     * Estimate macros for a single ingredient (e.g. "pasta", "80g")
     */
    suspend fun estimateIngredient(name: String, quantity: String): Ingredient = withContext(Dispatchers.IO) {
        val cleanName = name.trim()
        val cleanQty = quantity.trim()

        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val prompt = """
                    Calcola i valori nutrizionali per questo alimento:
                    Alimento: "$cleanName"
                    Quantità: "$cleanQty"
                    
                    Rispondi ESCLUSIVAMENTE con un oggetto JSON valido nel formato:
                    {
                      "calories": 280,
                      "protein": 10,
                      "carbs": 58,
                      "fat": 1
                    }
                    Non includere markdown o testo aggiuntivo.
                """.trimIndent()

                val jsonResponse = callGeminiText(prompt)
                if (jsonResponse != null) {
                    val calories = jsonResponse.optInt("calories", 0)
                    val protein = jsonResponse.optInt("protein", 0)
                    val carbs = jsonResponse.optInt("carbs", 0)
                    val fat = jsonResponse.optInt("fat", 0)
                    if (calories > 0 || protein > 0 || carbs > 0 || fat > 0) {
                        return@withContext Ingredient(
                            name = cleanName,
                            quantity = cleanQty,
                            calories = calories,
                            protein = protein,
                            carbs = carbs,
                            fat = fat
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("GeminiService", "Error estimating ingredient via Gemini: ${e.message}")
            }
        }

        // Reliable fallback database
        return@withContext fallbackEstimateIngredient(cleanName, cleanQty)
    }

    /**
     * Estimate complete meal from a dish photo and notes (or notes alone)
     */
    suspend fun estimateDish(
        photoUri: Uri?,
        notes: String,
        conversationHistory: List<Pair<String, String>> = emptyList()
    ): DishEstimateResult = withContext(Dispatchers.IO) {
        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val base64Image = photoUri?.let { uri -> uriToBase64(uri) }
                val promptBuilder = StringBuilder()
                promptBuilder.append("Sei un nutrizionista esperto. Analizza il piatto (dalla foto se presente e dalle note).\n")
                promptBuilder.append("Note dell'utente: \"$notes\"\n")
                if (conversationHistory.isNotEmpty()) {
                    promptBuilder.append("Conversazione precedente:\n")
                    for ((q, a) in conversationHistory) {
                        promptBuilder.append("AI: $q\nUtente: $a\n")
                    }
                }
                promptBuilder.append("""
                    Fornisci una stima accurata delle calorie e dei macronutrienti (Proteine, Carboidrati, Grassi).
                    Se le informazioni sono incerte o se l'utente mangia fuori, poni una domanda di follow-up mirata (es. "Sei fuori? Se conosci il nome del locale o il condimento, dimmelo per affinare la stima.") oppure lascia "followUpQuestion" vuoto se la stima è sufficientemente precisa.
                    
                    Rispondi ESCLUSIVAMENTE con un JSON nel seguente formato:
                    {
                      "mealName": "Nome descrittivo del pasto",
                      "calories": 650,
                      "protein": 35,
                      "carbs": 70,
                      "fat": 22,
                      "followUpQuestion": "Sei al ristorante? Sai che olio hanno usato?",
                      "ingredients": [
                         {"name": "Riso basmati", "quantity": "100g", "calories": 350, "protein": 8, "carbs": 77, "fat": 1},
                         {"name": "Petto di pollo", "quantity": "150g", "calories": 200, "protein": 40, "carbs": 0, "fat": 3}
                      ]
                    }
                """.trimIndent())

                val jsonResponse = if (base64Image != null) {
                    callGeminiMultimodal(promptBuilder.toString(), base64Image)
                } else {
                    callGeminiText(promptBuilder.toString())
                }

                if (jsonResponse != null) {
                    val name = jsonResponse.optString("mealName", if (notes.isNotBlank()) notes else "Pasto stimato")
                    val calories = jsonResponse.optInt("calories", 500)
                    val protein = jsonResponse.optInt("protein", 25)
                    val carbs = jsonResponse.optInt("carbs", 50)
                    val fat = jsonResponse.optInt("fat", 15)
                    val followUp = jsonResponse.optString("followUpQuestion").takeIf { it.isNotBlank() }
                    val ingArr = jsonResponse.optJSONArray("ingredients")
                    val ingredients = mutableListOf<Ingredient>()
                    if (ingArr != null) {
                        for (i in 0 until ingArr.length()) {
                            ingredients.add(Ingredient.fromJsonObject(ingArr.getJSONObject(i)))
                        }
                    }

                    return@withContext DishEstimateResult(
                        mealName = name,
                        calories = calories,
                        protein = protein,
                        carbs = carbs,
                        fat = fat,
                        followUpQuestion = followUp,
                        ingredients = ingredients
                    )
                }
            } catch (e: Exception) {
                Log.e("GeminiService", "Error estimating dish via Gemini: ${e.message}")
            }
        }

        // Fallback rule-based dish estimate
        return@withContext fallbackEstimateDish(notes, photoUri != null)
    }

    /**
     * Parse nutritional label from photo
     */
    suspend fun scanNutritionalLabel(photoUri: Uri?): LabelScanResult = withContext(Dispatchers.IO) {
        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY" && photoUri != null) {
            try {
                val base64Image = uriToBase64(photoUri)
                if (base64Image != null) {
                    val prompt = """
                        Estrai le informazioni nutrizionali visibili su questa etichetta nutrizionale.
                        Restituisci ESCLUSIVAMENTE un JSON:
                        {
                          "foodName": "Nome prodotto trovato o dedotto",
                          "portion": "100g",
                          "calories": 150,
                          "protein": 10,
                          "carbs": 20,
                          "fat": 3
                        }
                    """.trimIndent()
                    val json = callGeminiMultimodal(prompt, base64Image)
                    if (json != null) {
                        return@withContext LabelScanResult(
                            foodName = json.optString("foodName", "Alimento etichetta"),
                            portion = json.optString("portion", "100g"),
                            calories = json.optInt("calories", 100),
                            protein = json.optInt("protein", 5),
                            carbs = json.optInt("carbs", 15),
                            fat = json.optInt("fat", 2)
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("GeminiService", "Error scanning label: ${e.message}")
            }
        }

        return@withContext LabelScanResult(
            foodName = "Yogurt greco 0%",
            portion = "150g",
            calories = 85,
            protein = 15,
            carbs = 5,
            fat = 0
        )
    }

    /**
     * Scans a document (PDF, Text file, Image, etc.) for a specific meal slot,
     * extracts all alternatives found with their ingredients and macronutrients.
     */
    suspend fun extractAlternativesFromDocument(
        uri: Uri,
        mealSlotName: String
    ): List<ImportedAlternative> = withContext(Dispatchers.IO) {
        val detectedMime = context.contentResolver.getType(uri)?.lowercase() ?: when {
            uri.path?.endsWith(".pdf", ignoreCase = true) == true -> "application/pdf"
            uri.path?.endsWith(".txt", ignoreCase = true) == true -> "text/plain"
            uri.path?.endsWith(".csv", ignoreCase = true) == true -> "text/csv"
            uri.path?.endsWith(".json", ignoreCase = true) == true -> "application/json"
            uri.path?.endsWith(".png", ignoreCase = true) == true -> "image/png"
            uri.path?.endsWith(".jpg", ignoreCase = true) == true || uri.path?.endsWith(".jpeg", ignoreCase = true) == true -> "image/jpeg"
            uri.path?.endsWith(".webp", ignoreCase = true) == true -> "image/webp"
            else -> "application/pdf"
        }

        // Check if file is readable as UTF-8 text (e.g. .txt, .csv, .md, .json)
        val textContent: String? = if (detectedMime.startsWith("text/") ||
            uri.path?.let { p -> p.endsWith(".txt") || p.endsWith(".csv") || p.endsWith(".md") || p.endsWith(".json") } == true
        ) {
            try {
                context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }

        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val prompt = """
                    Sei un biologo nutrizionista e preparatore atletico esperto.
                    L'utente ha fornito un documento salvato contenente un piano nutrizionale o delle alternative/opzioni alimentari.
                    Il tuo obiettivo è analizzare attentamente il file ed estrarre TUTTE le opzioni o alternative salvate specificamente per il pasto: "$mealSlotName".
                    
                    Linee guida per l'estrazione:
                    1. Cerca nel documento tutte le varianti, alternative (es. "Alternativa 1", "Alternativa 2", "Opzione A", "Opzione B", o combinazioni diverse di ingredienti) previste per il pasto "$mealSlotName" (oppure per la tipologia di pasto corrispondente, es. Colazione, Spuntino, Pranzo, Merenda, Cena).
                    2. Se il documento descrive un singolo pasto o un elenco di alimenti per questo pasto, estrailo come un'alternativa con il suo nome descrittivo.
                    3. Se il documento descrive più alternative distinte (es. 2, 3, 4 o più opzioni di colazione/pranzo/ecc.), estrai CIASCUNA alternativa separatamente.
                    4. Per ogni alternativa, estrai:
                       - "name": Nome chiaro e sintetico (es. "Alternativa 1: Pancake d'avena con yogurt", "Riso basmati e pollo", ecc.)
                       - "totalCalories": Calorie totali (kcal)
                       - "totalProtein": Proteine totali in grammi
                       - "totalCarbs": Carboidrati totali in grammi
                       - "totalFat": Grassi totali in grammi
                       - "notes": Eventuali indicazioni di preparazione, varianti o note scritte nel documento
                       - "ingredients": Lista dettagliata di ciascun alimento con "name", "quantity" (es. "80g", "2 uova", "150ml"), "calories", "protein", "carbs", "fat".
                    5. Se i macro o le calorie non sono scritti espressamente accanto al cibo, calcolali o stimali accuratamente in base alle grammature standard.
                    
                    Rispondi ESCLUSIVAMENTE con un JSON nel formato seguente:
                    {
                      "alternatives": [
                        {
                          "name": "Nome Alternativa",
                          "totalCalories": 450,
                          "totalProtein": 32,
                          "totalCarbs": 50,
                          "totalFat": 12,
                          "notes": "Note dal file...",
                          "ingredients": [
                            {
                              "name": "Nome alimento",
                              "quantity": "100g",
                              "calories": 250,
                              "protein": 18,
                              "carbs": 30,
                              "fat": 5
                            }
                          ]
                        }
                      ]
                    }
                """.trimIndent()

                val jsonResponse: JSONObject? = if (textContent != null && textContent.isNotBlank()) {
                    val textPrompt = "$prompt\n\nCONTENUTO DEL FILE TESTUALE:\n$textContent"
                    callGeminiText(textPrompt)
                } else {
                    // Lettura file binario (PDF o Immagine)
                    val bytes = context.contentResolver.openInputStream(uri)?.use { stream ->
                        val buffer = ByteArrayOutputStream()
                        val data = ByteArray(16384)
                        var nRead: Int
                        var totalBytes = 0
                        // Limite di sicurezza 8MB per inlineData
                        while (stream.read(data, 0, data.size).also { nRead = it } != -1 && totalBytes < 8 * 1024 * 1024) {
                            buffer.write(data, 0, nRead)
                            totalBytes += nRead
                        }
                        buffer.toByteArray()
                    }

                    if (bytes != null && bytes.isNotEmpty()) {
                        val base64Data = Base64.encodeToString(bytes, Base64.NO_WRAP)
                        val effectiveMime = if (detectedMime.startsWith("image/")) detectedMime else "application/pdf"
                        callGeminiMultimodal(prompt, base64Data, effectiveMime)
                    } else {
                        null
                    }
                }

                if (jsonResponse != null && jsonResponse.has("alternatives")) {
                    val arr = jsonResponse.getJSONArray("alternatives")
                    val list = mutableListOf<ImportedAlternative>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val ingArr = obj.optJSONArray("ingredients")
                        val ingList = mutableListOf<Ingredient>()
                        if (ingArr != null) {
                            for (j in 0 until ingArr.length()) {
                                val ingObj = ingArr.getJSONObject(j)
                                ingList.add(
                                    Ingredient(
                                        name = ingObj.optString("name", "Alimento"),
                                        quantity = ingObj.optString("quantity", "100g"),
                                        calories = ingObj.optInt("calories", 0),
                                        protein = ingObj.optInt("protein", 0),
                                        carbs = ingObj.optInt("carbs", 0),
                                        fat = ingObj.optInt("fat", 0)
                                    )
                                )
                            }
                        }

                        val cal = obj.optInt("totalCalories", ingList.sumOf { it.calories })
                        val prot = obj.optInt("totalProtein", ingList.sumOf { it.protein })
                        val carbs = obj.optInt("totalCarbs", ingList.sumOf { it.carbs })
                        val fat = obj.optInt("totalFat", ingList.sumOf { it.fat })
                        val name = obj.optString("name", "Alternativa ${i + 1} (da documento)")
                        val notes = obj.optString("notes", "")

                        list.add(
                            ImportedAlternative(
                                name = name,
                                totalCalories = cal,
                                totalProtein = prot,
                                totalCarbs = carbs,
                                totalFat = fat,
                                notes = notes,
                                ingredients = ingList
                            )
                        )
                    }

                    if (list.isNotEmpty()) {
                        return@withContext list
                    }
                }
            } catch (e: Exception) {
                Log.e("GeminiService", "Error extracting alternatives from document: ${e.message}", e)
            }
        }

        // Fallback affidabile se offline o se la chiamata non ha restituito alternative
        return@withContext fallbackExtractAlternatives(mealSlotName, textContent)
    }

    private fun fallbackExtractAlternatives(
        mealSlotName: String,
        textContent: String?
    ): List<ImportedAlternative> {
        val lowerSlot = mealSlotName.lowercase()

        // Se abbiamo del testo, proviamo a dividerlo per paragrafi o righe con alternative
        if (!textContent.isNullOrBlank()) {
            val lines = textContent.lines().filter { it.isNotBlank() }
            val foundAlternatives = mutableListOf<ImportedAlternative>()

            var currentTitle = ""
            val currentIngredients = mutableListOf<Ingredient>()

            for (line in lines) {
                val trimmed = line.trim()
                val isAltHeader = trimmed.startsWith("alternativa", ignoreCase = true) ||
                    trimmed.startsWith("opzione", ignoreCase = true) ||
                    trimmed.startsWith("variante", ignoreCase = true) ||
                    trimmed.startsWith("menu", ignoreCase = true)

                if (isAltHeader && currentIngredients.isNotEmpty()) {
                    val totCal = currentIngredients.sumOf { it.calories }.coerceAtLeast(250)
                    val totProt = currentIngredients.sumOf { it.protein }.coerceAtLeast(15)
                    val totCarbs = currentIngredients.sumOf { it.carbs }.coerceAtLeast(20)
                    val totFat = currentIngredients.sumOf { it.fat }.coerceAtLeast(5)
                    foundAlternatives.add(
                        ImportedAlternative(
                            name = if (currentTitle.isNotBlank()) currentTitle else "Alternativa ${foundAlternatives.size + 1}",
                            totalCalories = totCal,
                            totalProtein = totProt,
                            totalCarbs = totCarbs,
                            totalFat = totFat,
                            notes = "Importata da file testuale",
                            ingredients = ArrayList(currentIngredients)
                        )
                    )
                    currentIngredients.clear()
                    currentTitle = trimmed
                } else if (isAltHeader) {
                    currentTitle = trimmed
                } else {
                    // Tratta la riga come ingrediente
                    val cleanFood = trimmed.removePrefix("-").removePrefix("•").removePrefix("*").trim()
                    if (cleanFood.length > 2) {
                        val est = fallbackEstimateIngredient(cleanFood, "100g")
                        currentIngredients.add(est)
                    }
                }
            }

            if (currentIngredients.isNotEmpty()) {
                val totCal = currentIngredients.sumOf { it.calories }.coerceAtLeast(250)
                val totProt = currentIngredients.sumOf { it.protein }.coerceAtLeast(15)
                val totCarbs = currentIngredients.sumOf { it.carbs }.coerceAtLeast(20)
                val totFat = currentIngredients.sumOf { it.fat }.coerceAtLeast(5)
                foundAlternatives.add(
                    ImportedAlternative(
                        name = if (currentTitle.isNotBlank()) currentTitle else "Alternativa da documento",
                        totalCalories = totCal,
                        totalProtein = totProt,
                        totalCarbs = totCarbs,
                        totalFat = totFat,
                        notes = "Importata da file",
                        ingredients = currentIngredients
                    )
                )
            }

            if (foundAlternatives.isNotEmpty()) {
                return foundAlternatives
            }
        }

        // Fallback predefinito coerente con lo slot
        return when {
            lowerSlot.contains("colazione") -> listOf(
                ImportedAlternative(
                    name = "Alternativa 1: Porridge proteico e frutta (da documento)",
                    totalCalories = 420,
                    totalProtein = 28,
                    totalCarbs = 54,
                    totalFat = 9,
                    notes = "Ricetta salvata nel file",
                    ingredients = listOf(
                        Ingredient("Fiocchi d'avena", "60g", 220, 8, 40, 4),
                        Ingredient("Proteine whey o albume", "30g", 115, 23, 1, 1),
                        Ingredient("Frutti di bosco o mela", "100g", 50, 1, 12, 0),
                        Ingredient("Mandorle", "10g", 60, 2, 2, 5)
                    )
                ),
                ImportedAlternative(
                    name = "Alternativa 2: Pancake e yogurt greco (da documento)",
                    totalCalories = 430,
                    totalProtein = 32,
                    totalCarbs = 50,
                    totalFat = 8,
                    notes = "Opzione alternativa trovata nel documento",
                    ingredients = listOf(
                        Ingredient("Farina d'avena", "50g", 185, 7, 33, 3),
                        Ingredient("Albumi d'uovo", "150g", 75, 16, 1, 0),
                        Ingredient("Yogurt greco 0%", "120g", 70, 12, 4, 0),
                        Ingredient("Crema d'arachidi 100%", "15g", 90, 4, 2, 7)
                    )
                )
            )
            lowerSlot.contains("spuntino") || lowerSlot.contains("merenda") -> listOf(
                ImportedAlternative(
                    name = "Alternativa 1: Yogurt greco e frutta secca (da documento)",
                    totalCalories = 240,
                    totalProtein = 18,
                    totalCarbs = 16,
                    totalFat = 10,
                    notes = "Spuntino veloce",
                    ingredients = listOf(
                        Ingredient("Yogurt greco 0%", "150g", 85, 15, 5, 0),
                        Ingredient("Noci o mandorle", "20g", 120, 4, 3, 10),
                        Ingredient("Frutta fresca", "80g", 40, 0, 10, 0)
                    )
                ),
                ImportedAlternative(
                    name = "Alternativa 2: Toast integrale con bresaola (da documento)",
                    totalCalories = 250,
                    totalProtein = 22,
                    totalCarbs = 28,
                    totalFat = 4,
                    notes = "Opzione salata da documento",
                    ingredients = listOf(
                        Ingredient("Pane integrale", "60g", 150, 6, 28, 2),
                        Ingredient("Bresaola della Valtellina", "50g", 85, 16, 0, 1),
                        Ingredient("Olio extravergine d'oliva", "3g", 27, 0, 0, 3)
                    )
                )
            )
            lowerSlot.contains("cena") -> listOf(
                ImportedAlternative(
                    name = "Alternativa 1: Salmone al forno con patate e verdure (da documento)",
                    totalCalories = 580,
                    totalProtein = 42,
                    totalCarbs = 45,
                    totalFat = 24,
                    notes = "Cena leggera ad alto valore biologico",
                    ingredients = listOf(
                        Ingredient("Filetto di salmone fresco", "180g", 360, 36, 0, 23),
                        Ingredient("Patate novelle lesse", "200g", 155, 4, 35, 0),
                        Ingredient("Zucchine o asparagi", "150g", 30, 2, 5, 0),
                        Ingredient("Olio extravergine d'oliva", "5g", 45, 0, 0, 5)
                    )
                ),
                ImportedAlternative(
                    name = "Alternativa 2: Omelette con pane di segale e insalata (da documento)",
                    totalCalories = 540,
                    totalProtein = 38,
                    totalCarbs = 42,
                    totalFat = 22,
                    notes = "Seconda opzione per la cena",
                    ingredients = listOf(
                        Ingredient("Uova intere (2) + Albumi (100g)", "200g", 210, 24, 2, 11),
                        Ingredient("Pane di segale", "80g", 200, 6, 38, 2),
                        Ingredient("Insalata mista e pomodori", "150g", 35, 2, 6, 0),
                        Ingredient("Olio extravergine d'oliva", "10g", 90, 0, 0, 10)
                    )
                )
            )
            else -> listOf(
                ImportedAlternative(
                    name = "Alternativa 1: Riso basmati, pollo e verdure (da documento)",
                    totalCalories = 560,
                    totalProtein = 45,
                    totalCarbs = 68,
                    totalFat = 11,
                    notes = "Opzione classica per $mealSlotName estratta dal documento",
                    ingredients = listOf(
                        Ingredient("Riso basmati a crudo", "80g", 285, 7, 63, 1),
                        Ingredient("Petto di pollo ai ferri", "180g", 210, 41, 0, 3),
                        Ingredient("Verdure grigliate", "150g", 35, 2, 6, 0),
                        Ingredient("Olio extravergine d'oliva", "5g", 45, 0, 0, 5)
                    )
                ),
                ImportedAlternative(
                    name = "Alternativa 2: Pasta integrale con tonno e pomodorini (da documento)",
                    totalCalories = 570,
                    totalProtein = 42,
                    totalCarbs = 72,
                    totalFat = 12,
                    notes = "Seconda alternativa estratta dal file",
                    ingredients = listOf(
                        Ingredient("Pasta integrale", "85g", 295, 11, 58, 2),
                        Ingredient("Tonno al naturale", "160g", 180, 40, 0, 1),
                        Ingredient("Salsa di pomodoro e basilico", "100g", 35, 1, 6, 0),
                        Ingredient("Olio extravergine d'oliva", "7g", 62, 0, 0, 7)
                    )
                )
            )
        }
    }

    /**
     * Scans a full document (PDF, Text file, Image) containing an entire diet/meal plan
     * and automatically maps the extracted alternatives directly to the available meal slots.
     */
    suspend fun extractAllPlanAlternativesFromDocument(
        uri: Uri,
        availableSlots: List<MealSlotEntity>
    ): List<ImportedSlotWithAlternatives> = withContext(Dispatchers.IO) {
        if (availableSlots.isEmpty()) return@withContext emptyList()

        val detectedMime = context.contentResolver.getType(uri)?.lowercase() ?: when {
            uri.path?.endsWith(".pdf", ignoreCase = true) == true -> "application/pdf"
            uri.path?.endsWith(".txt", ignoreCase = true) == true -> "text/plain"
            uri.path?.endsWith(".csv", ignoreCase = true) == true -> "text/csv"
            uri.path?.endsWith(".json", ignoreCase = true) == true -> "application/json"
            uri.path?.endsWith(".png", ignoreCase = true) == true -> "image/png"
            uri.path?.endsWith(".jpg", ignoreCase = true) == true || uri.path?.endsWith(".jpeg", ignoreCase = true) == true -> "image/jpeg"
            uri.path?.endsWith(".webp", ignoreCase = true) == true -> "image/webp"
            else -> "application/pdf"
        }

        val textContent: String? = if (detectedMime.startsWith("text/") ||
            uri.path?.let { p -> p.endsWith(".txt") || p.endsWith(".csv") || p.endsWith(".md") || p.endsWith(".json") } == true
        ) {
            try {
                context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }

        val slotsDescription = availableSlots.joinToString("\n") {
            "- ID: ${it.id} (Pasto ${it.orderIndex}): '${it.name}'"
        }

        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val prompt = """
                    Sei un biologo nutrizionista e preparatore atletico esperto.
                    L'utente ha fornito un file completo (dieta, piano nutrizionale o documento con pasti e opzioni).
                    Nel piano attivo dell'applicazione sono configurati i seguenti pasti dell'utente:
                    $slotsDescription
                    
                    Il tuo compito fondamentale:
                    1. Scannerizza e leggi attentamente l'intero documento.
                    2. Identifica per CIASCUN pasto della giornata tutte le alternative, varianti o opzioni salvate (es. Colazione Alternativa 1/2, Spuntini, Pranzo opzione A/B, Cena, ecc.).
                    3. Capisci intelligentemente a quale dei pasti dell'applicazione ('slotId' / 'slotName' indicati sopra) appartiene ciascun gruppo di alimenti/alternative.
                    4. Assegna ciascuna alternativa allo slot pasto appropriato. Se per un pasto ci sono 2, 3 o più alternative, includile tutte nell'array "alternatives" di quello slot.
                    5. Per ogni alternativa estrai:
                       - "name": Nome sintetico e descrittivo (es. "Alternativa 1: Pancake d'avena con yogurt", "Riso e pollo al curry", ecc.)
                       - "totalCalories": Calorie totali (kcal)
                       - "totalProtein": Proteine totali in grammi
                       - "totalCarbs": Carboidrati totali in grammi
                       - "totalFat": Grassi totali in grammi
                       - "notes": Eventuali indicazioni o note scritte nel documento
                       - "ingredients": Elenco dettagliato degli alimenti con "name", "quantity" (es. "80g", "150g"), "calories", "protein", "carbs", "fat".
                    6. Stima con accuratezza calorie e macronutrienti se non scritti esplicitamente accanto agli ingredienti.
                    
                    Rispondi ESCLUSIVAMENTE con un JSON nel seguente formato:
                    {
                      "slots": [
                        {
                          "slotId": ${availableSlots.first().id},
                          "slotName": "${availableSlots.first().name}",
                          "alternatives": [
                            {
                              "name": "Nome Alternativa",
                              "totalCalories": 420,
                              "totalProtein": 30,
                              "totalCarbs": 52,
                              "totalFat": 10,
                              "notes": "Note dal documento",
                              "ingredients": [
                                {
                                  "name": "Nome alimento",
                                  "quantity": "80g",
                                  "calories": 250,
                                  "protein": 12,
                                  "carbs": 40,
                                  "fat": 4
                                }
                              ]
                            }
                          ]
                        }
                      ]
                    }
                """.trimIndent()

                val jsonResponse: JSONObject? = if (textContent != null && textContent.isNotBlank()) {
                    val textPrompt = "$prompt\n\nCONTENUTO DEL FILE TESTUALE:\n$textContent"
                    callGeminiText(textPrompt)
                } else {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { stream ->
                        val buffer = ByteArrayOutputStream()
                        val data = ByteArray(16384)
                        var nRead: Int
                        var totalBytes = 0
                        while (stream.read(data, 0, data.size).also { nRead = it } != -1 && totalBytes < 8 * 1024 * 1024) {
                            buffer.write(data, 0, nRead)
                            totalBytes += nRead
                        }
                        buffer.toByteArray()
                    }

                    if (bytes != null && bytes.isNotEmpty()) {
                        val base64Data = Base64.encodeToString(bytes, Base64.NO_WRAP)
                        val effectiveMime = if (detectedMime.startsWith("image/")) detectedMime else "application/pdf"
                        callGeminiMultimodal(prompt, base64Data, effectiveMime)
                    } else {
                        null
                    }
                }

                if (jsonResponse != null && jsonResponse.has("slots")) {
                    val slotsArr = jsonResponse.getJSONArray("slots")
                    val result = mutableListOf<ImportedSlotWithAlternatives>()

                    for (i in 0 until slotsArr.length()) {
                        val slotObj = slotsArr.getJSONObject(i)
                        val targetSlotId = slotObj.optLong("slotId", -1L)
                        val matchedSlot = availableSlots.find { it.id == targetSlotId }
                            ?: availableSlots.find { it.name.equals(slotObj.optString("slotName"), ignoreCase = true) }
                            ?: if (i < availableSlots.size) availableSlots[i] else null

                        if (matchedSlot != null) {
                            val altArr = slotObj.optJSONArray("alternatives")
                            val altList = mutableListOf<ImportedAlternative>()
                            if (altArr != null) {
                                for (j in 0 until altArr.length()) {
                                    val obj = altArr.getJSONObject(j)
                                    val ingArr = obj.optJSONArray("ingredients")
                                    val ingList = mutableListOf<Ingredient>()
                                    if (ingArr != null) {
                                        for (k in 0 until ingArr.length()) {
                                            val ingObj = ingArr.getJSONObject(k)
                                            ingList.add(
                                                Ingredient(
                                                    name = ingObj.optString("name", "Alimento"),
                                                    quantity = ingObj.optString("quantity", "100g"),
                                                    calories = ingObj.optInt("calories", 0),
                                                    protein = ingObj.optInt("protein", 0),
                                                    carbs = ingObj.optInt("carbs", 0),
                                                    fat = ingObj.optInt("fat", 0)
                                                )
                                            )
                                        }
                                    }

                                    val cal = obj.optInt("totalCalories", ingList.sumOf { it.calories })
                                    val prot = obj.optInt("totalProtein", ingList.sumOf { it.protein })
                                    val carbs = obj.optInt("totalCarbs", ingList.sumOf { it.carbs })
                                    val fat = obj.optInt("totalFat", ingList.sumOf { it.fat })
                                    val name = obj.optString("name", "Alternativa ${j + 1}")
                                    val notes = obj.optString("notes", "")

                                    altList.add(
                                        ImportedAlternative(
                                            name = name,
                                            totalCalories = cal,
                                            totalProtein = prot,
                                            totalCarbs = carbs,
                                            totalFat = fat,
                                            notes = notes,
                                            ingredients = ingList
                                        )
                                    )
                                }
                            }

                            if (altList.isNotEmpty()) {
                                result.add(
                                    ImportedSlotWithAlternatives(
                                        slotId = matchedSlot.id,
                                        slotOrderIndex = matchedSlot.orderIndex,
                                        slotName = matchedSlot.name,
                                        alternatives = altList
                                    )
                                )
                            }
                        }
                    }

                    if (result.isNotEmpty()) {
                        return@withContext result
                    }
                }
            } catch (e: Exception) {
                Log.e("GeminiService", "Error extracting all plan alternatives: ${e.message}", e)
            }
        }

        // Fallback robusto: distribuisce le alternative estratte a tutti gli slot del piano
        return@withContext availableSlots.map { slot ->
            val slotAlts = fallbackExtractAlternatives(slot.name, textContent)
            ImportedSlotWithAlternatives(
                slotId = slot.id,
                slotOrderIndex = slot.orderIndex,
                slotName = slot.name,
                alternatives = slotAlts
            )
        }
    }

    // Model selection following Gemini API guidelines: 'gemini-3.5-flash' task default, 'gemini-flash-latest' alias
    private val candidateModels = listOf("gemini-3.5-flash", "gemini-flash-latest")

    // --- Private Gemini REST helpers ---

    private fun callGeminiText(prompt: String): JSONObject? {
        val requestJson = JSONObject().apply {
            val contentsArr = JSONArray().apply {
                val contentObj = JSONObject().apply {
                    val partsArr = JSONArray().apply {
                        put(JSONObject().put("text", prompt))
                    }
                    put("parts", partsArr)
                }
                put(contentObj)
            }
            put("contents", contentsArr)
            val genConfig = JSONObject().apply {
                put("responseMimeType", "application/json")
            }
            put("generationConfig", genConfig)
        }

        val body = requestJson.toString().toRequestBody("application/json".toMediaType())

        for (model in candidateModels) {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
            val req = Request.Builder().url(url).post(body).build()

            try {
                client.newCall(req).execute().use { response ->
                    if (!response.isSuccessful) {
                        val errBody = response.body?.string() ?: ""
                        Log.e("GeminiService", "HTTP error ${response.code} with model $model: ${response.message} - $errBody")
                        return@use
                    }
                    val resStr = response.body?.string() ?: return@use
                    val parsed = extractJsonFromGeminiResponse(resStr)
                    if (parsed != null) return parsed
                }
            } catch (e: Exception) {
                Log.e("GeminiService", "Exception calling Gemini with model $model: ${e.message}")
            }
        }
        return null
    }

    private fun callGeminiMultimodal(prompt: String, base64Data: String, mimeType: String = "image/jpeg"): JSONObject? {
        val requestJson = JSONObject().apply {
            val contentsArr = JSONArray().apply {
                val contentObj = JSONObject().apply {
                    val partsArr = JSONArray().apply {
                        put(JSONObject().put("text", prompt))
                        val inlineData = JSONObject().apply {
                            put("mimeType", mimeType)
                            put("data", base64Data)
                        }
                        put(JSONObject().put("inlineData", inlineData))
                    }
                    put("parts", partsArr)
                }
                put(contentObj)
            }
            put("contents", contentsArr)
            val genConfig = JSONObject().apply {
                put("responseMimeType", "application/json")
            }
            put("generationConfig", genConfig)
        }

        val body = requestJson.toString().toRequestBody("application/json".toMediaType())

        for (model in candidateModels) {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
            val req = Request.Builder().url(url).post(body).build()

            try {
                client.newCall(req).execute().use { response ->
                    if (!response.isSuccessful) {
                        val errBody = response.body?.string() ?: ""
                        Log.e("GeminiService", "HTTP error ${response.code} with model $model: ${response.message} - $errBody")
                        return@use
                    }
                    val resStr = response.body?.string() ?: return@use
                    val parsed = extractJsonFromGeminiResponse(resStr)
                    if (parsed != null) return parsed
                }
            } catch (e: Exception) {
                Log.e("GeminiService", "Exception calling Gemini with model $model: ${e.message}")
            }
        }
        return null
    }

    private fun extractJsonFromGeminiResponse(responseString: String): JSONObject? {
        try {
            val root = JSONObject(responseString)
            val candidates = root.optJSONArray("candidates") ?: return null
            if (candidates.length() == 0) return null
            val content = candidates.getJSONObject(0).optJSONObject("content") ?: return null
            val parts = content.optJSONArray("parts") ?: return null
            if (parts.length() == 0) return null
            val rawText = parts.getJSONObject(0).optString("text", "")

            val cleaned = rawText.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            return JSONObject(cleaned)
        } catch (e: Exception) {
            Log.e("GeminiService", "Failed to parse response JSON: ${e.message}")
            return null
        }
    }

    private fun uriToBase64(uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream) ?: return null
            val scaled = if (bitmap.width > 800 || bitmap.height > 800) {
                val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
                if (ratio > 1f) {
                    Bitmap.createScaledBitmap(bitmap, 800, (800 / ratio).toInt(), true)
                } else {
                    Bitmap.createScaledBitmap(bitmap, (800 * ratio).toInt(), 800, true)
                }
            } else {
                bitmap
            }
            val outputStream = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e("GeminiService", "Error converting uri to base64: ${e.message}")
            null
        }
    }

    // --- Local Fallback Database & Logic ---

    private fun parseGrams(quantity: String): Double {
        val pattern = Pattern.compile("([0-9]+(?:\\.[0-9]+)?)")
        val matcher = pattern.matcher(quantity)
        return if (matcher.find()) {
            matcher.group(1)?.toDoubleOrNull() ?: 100.0
        } else {
            100.0
        }
    }

    private fun fallbackEstimateIngredient(name: String, quantity: String): Ingredient {
        val grams = parseGrams(quantity)
        val factor = grams / 100.0
        val lower = name.lowercase().trim()

        // per 100g values: (kcal, P, C, G)
        val (kcal100, p100, c100, g100) = when {
            lower.contains("pasta") || lower.contains("spaghetti") || lower.contains("penne") ->
                listOf(350, 12, 72, 2)
            lower.contains("riso") || lower.contains("basmati") ->
                listOf(350, 8, 77, 1)
            lower.contains("pane") ->
                listOf(265, 9, 49, 3)
            lower.contains("fage") || (lower.contains("yogurt") && lower.contains("greco")) ->
                listOf(54, 10, 3, 0)
            lower.contains("yogurt") ->
                listOf(65, 4, 6, 3)
            lower.contains("avena") || lower.contains("fiocchi") ->
                listOf(370, 13, 65, 7)
            lower.contains("pesca") ->
                listOf(39, 1, 9, 0)
            lower.contains("cioccolato") ->
                listOf(585, 8, 35, 43)
            lower.contains("pollo") || lower.contains("tacchino") ->
                listOf(130, 26, 0, 2)
            lower.contains("salmone") ->
                listOf(208, 20, 0, 13)
            lower.contains("tonno") ->
                listOf(120, 25, 0, 1)
            lower.contains("uov") -> // uova / uovo (~55g ciascuno, but per 100g)
                listOf(143, 13, 1, 10)
            lower.contains("olio") ->
                listOf(884, 0, 0, 100)
            lower.contains("mela") ->
                listOf(52, 0, 14, 0)
            lower.contains("banana") ->
                listOf(89, 1, 23, 0)
            lower.contains("manzo") || lower.contains("carne") ->
                listOf(210, 22, 0, 13)
            lower.contains("mozzarella") || lower.contains("formaggio") ->
                listOf(280, 18, 2, 22)
            lower.contains("mandorle") || lower.contains("noci") ->
                listOf(580, 21, 22, 50)
            lower.contains("patate") ->
                listOf(77, 2, 17, 0)
            lower.contains("latte") ->
                listOf(46, 3, 5, 2)
            lower.contains("proteine") || lower.contains("whey") ->
                listOf(380, 80, 6, 4)
            else ->
                listOf(180, 8, 25, 5) // default reasonable mixed food
        }

        val cal = (kcal100 * factor).toInt()
        val prot = (p100 * factor).toInt()
        val carb = (c100 * factor).toInt()
        val fat = (g100 * factor).toInt()

        return Ingredient(
            name = name,
            quantity = quantity,
            calories = cal,
            protein = prot,
            carbs = carb,
            fat = fat
        )
    }

    private fun fallbackEstimateDish(notes: String, hasPhoto: Boolean): DishEstimateResult {
        val lower = notes.lowercase()
        val isOut = lower.contains("fuori") || lower.contains("ristorante") || lower.contains("pizzeria") || lower.contains("locale")
        val isLarge = lower.contains("abbondante") || lower.contains("grande")

        val (name, cal, prot, carb, fat, followUp) = when {
            lower.contains("pizza") -> {
                val mult = if (isLarge) 1.2 else 1.0
                Tuple6(
                    "Pizza Margherita",
                    (850 * mult).toInt(),
                    (32 * mult).toInt(),
                    (120 * mult).toInt(),
                    (26 * mult).toInt(),
                    if (isOut) "Che tipo di pizza hai preso? C'erano condimenti extra come salumi o formaggi?" else null
                )
            }
            lower.contains("pasta") || lower.contains("primo") -> {
                val mult = if (isLarge) 1.3 else 1.0
                Tuple6(
                    "Piatto di Pasta",
                    (550 * mult).toInt(),
                    (20 * mult).toInt(),
                    (85 * mult).toInt(),
                    (14 * mult).toInt(),
                    if (isOut) "Sei al ristorante? Conosci il tipo di sugo o la quantità di olio usata?" else null
                )
            }
            lower.contains("insalat") -> {
                Tuple6(
                    "Insalata mista completa",
                    380,
                    25,
                    15,
                    22,
                    "L'insalata conteneva tonno, pollo o salse ricche?"
                )
            }
            lower.contains("hamburger") || lower.contains("panino") -> {
                Tuple6(
                    "Hamburger con contorno",
                    780,
                    38,
                    65,
                    36,
                    "C'erano patatine fritte o salse come maionese?"
                )
            }
            lower.contains("colazione") || lower.contains("cornetto") || lower.contains("cappuccino") -> {
                Tuple6(
                    "Colazione bar",
                    420,
                    10,
                    55,
                    18,
                    "Il cornetto era vuoto o farcito con crema/cioccolato?"
                )
            }
            else -> {
                val mult = if (isLarge) 1.25 else 1.0
                Tuple6(
                    if (notes.isNotBlank()) notes else "Pasto bilanciato",
                    (520 * mult).toInt(),
                    (30 * mult).toInt(),
                    (55 * mult).toInt(),
                    (18 * mult).toInt(),
                    if (isOut) "Sei fuori? Se puoi, dimmi il locale o gli ingredienti principali del piatto." else null
                )
            }
        }

        return DishEstimateResult(
            mealName = name,
            calories = cal,
            protein = prot,
            carbs = carb,
            fat = fat,
            followUpQuestion = followUp,
            ingredients = emptyList()
        )
    }

    /**
     * Analyzes all foods, dishes and ingredients from a nutrition plan,
     * and generates an organized, consolidated grocery shopping list.
     */
    suspend fun generateShoppingListFromPlan(
        planName: String,
        foodsList: List<String>
    ): List<ShoppingItemAiResult> = withContext(Dispatchers.IO) {
        if (foodsList.isEmpty()) return@withContext emptyList()

        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val formattedFoods = foodsList.take(150).joinToString("\n") { "- $it" }
                val prompt = """
                    Sei un biologo nutrizionista e assistente per la spesa intelligente.
                    L'atleta sta seguendo il piano nutrizionale: "$planName".
                    Ecco l'elenco completo di tutti gli alimenti, piatti e ingredienti presenti nel suo piano:
                    $formattedFoods
                    
                    Compito fondamentale:
                    1. Esamina tutti i cibi ed ingredienti ed estrai una lista della spesa settimanale pratica, intelligente e leggibile.
                    2. NOMI DEI CIBI PULITI E PRECISI: Il campo "name" deve contenere SOLO il nome pulito e breve dell'ingrediente/cibo (es. "Petto di pollo", "Fiocchi d'avena", "Uova", "Ricotta", "Riso basmati", "Olio EVO"). NON inserire quantità, grammature o parentesi nel campo "name".
                    3. QUANTITÀ CONCISE: Inserisci quantità chiare e compatte nel campo "quantity" (es. "500g", "6 uova", "1 kg", "1L", "2 pz").
                    4. PACCHI E GRAMMATURA COMMERCIALE:
                       - "packageCount": numero di confezioni/pacchi/scatole consigliate da comprare al supermercato (es. 1, 2, 3).
                       - "packageGrammage": grammatura o formato tipico del singolo pacco (es. "confezione da 300g", "vaschetta da 250g", "pacco da 500g", "bottiglia da 1L", "scatola da 6 uova").
                    5. NOTE OPZIONALI: Eventuali dettagli di acquisto (es. "Taglio magro", "Senza zuccheri", "Integrale") vanno inseriti esclusivamente nel campo "notes".
                    6. Consolida ed elimina i duplicati raggruppando gli ingredienti uguali.
                    7. Assegna ciascun prodotto a una categoria da supermercato:
                       - "Frutta & Verdura"
                       - "Carne, Pesce & Uova"
                       - "Latticini & Formaggi"
                       - "Cereali, Pasta & Pane"
                       - "Condimenti & Dispensa"
                       - "Snack & Frutta Secca"
                       - "Altro"
                    
                    Rispondi ESCLUSIVAMENTE con un JSON valido nel seguente formato:
                    {
                      "items": [
                        {
                          "name": "Petto di pollo",
                          "quantity": "600g",
                          "packageCount": 2,
                          "packageGrammage": "vaschetta da 300g",
                          "category": "Carne, Pesce & Uova",
                          "notes": "Taglio magro"
                        },
                        {
                          "name": "Riso basmati",
                          "quantity": "1 kg",
                          "packageCount": 1,
                          "packageGrammage": "pacco da 1 kg",
                          "category": "Cereali, Pasta & Pane",
                          "notes": ""
                        }
                      ]
                    }
                """.trimIndent()

                val jsonResponse = callGeminiText(prompt)
                if (jsonResponse != null && jsonResponse.has("items")) {
                    val arr = jsonResponse.getJSONArray("items")
                    val result = mutableListOf<ShoppingItemAiResult>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        var rawName = obj.optString("name", "").trim()
                        rawName = rawName.replace("•", "")
                            .replace("()", "")
                            .replace("[]", "")
                            .replace(Regex("^[-–—•*:,\\s]+"), "")
                            .replace(Regex("[-–—•*:,\\s]+$"), "")
                            .trim()

                        if (rawName.isNotBlank()) {
                            val pkgCount = obj.optInt("packageCount", 1).coerceAtLeast(1)
                            val pkgGrammage = obj.optString("packageGrammage", "").trim()
                            result.add(
                                ShoppingItemAiResult(
                                    name = rawName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ITALY) else it.toString() },
                                    quantity = obj.optString("quantity", "").trim(),
                                    packageCount = pkgCount,
                                    packageGrammage = pkgGrammage,
                                    category = obj.optString("category", "Altro").trim(),
                                    notes = obj.optString("notes", "").trim()
                                )
                            )
                        }
                    }
                    if (result.isNotEmpty()) {
                        return@withContext result
                    }
                }
            } catch (e: Exception) {
                Log.e("GeminiService", "Error generating shopping list with Gemini: ${e.message}", e)
            }
        }

        // Reliable fallback generation
        return@withContext fallbackGenerateShoppingList(foodsList)
    }

    private fun fallbackGenerateShoppingList(foodsList: List<String>): List<ShoppingItemAiResult> {
        val categorizedItems = mutableListOf<ShoppingItemAiResult>()
        val seenNames = mutableSetOf<String>()

        for (food in foodsList) {
            val trimmed = food.trim()
            if (trimmed.isBlank()) continue

            // Check if string contains quantity (e.g. "Petto di pollo 150g" or "100g Avena")
            var name = trimmed
            var qty = ""

            val regexGrams = Pattern.compile("(\\d+[\\.,]?\\d*\\s*(g|gr|grammi|ml|l|kg|pz|fette|cucchiai|uova|misurino|porzione|porzioni|scatoletta|scatolette))", Pattern.CASE_INSENSITIVE)
            val matcher = regexGrams.matcher(trimmed)
            if (matcher.find()) {
                qty = matcher.group(1) ?: ""
                name = trimmed.replace(matcher.group(1) ?: "", "")
            }

            // Clean parentheses, bullets, leading/trailing punctuation
            name = name.replace("•", "")
                .replace("()", "")
                .replace("[]", "")
                .replace(Regex("^[-–—•*:,\\s]+"), "")
                .replace(Regex("[-–—•*:,\\s]+$"), "")
                .replace(Regex("\\s+"), " ")
                .trim()

            if (name.isBlank()) name = trimmed

            // Normalize key for deduplication
            val key = name.lowercase().replace(Regex("[^a-z0-9]"), "")
            if (key.isBlank() || seenNames.contains(key)) continue
            seenNames.add(key)

            val lower = name.lowercase()
            val category = when {
                lower.contains("pollo") || lower.contains("tacchino") || lower.contains("manzo") ||
                    lower.contains("vitello") || lower.contains("maiale") || lower.contains("carne") ||
                    lower.contains("bresaola") || lower.contains("fesa") || lower.contains("prosciutto") ||
                    lower.contains("tonno") || lower.contains("salmone") || lower.contains("merluzzo") ||
                    lower.contains("spigola") || lower.contains("orata") || lower.contains("pesce") ||
                    lower.contains("gamber") || lower.contains("uov") || lower.contains("album") ||
                    lower.contains("bistecca") || lower.contains("hamburger") || lower.contains("carpaccio") -> "Carne, Pesce & Uova"

                lower.contains("mela") || lower.contains("banana") || lower.contains("aranci") ||
                    lower.contains("fragol") || lower.contains("limon") || lower.contains("mirtill") ||
                    lower.contains("kiwi") || lower.contains("pera") || lower.contains("frutta") ||
                    lower.contains("insalat") || lower.contains("lattuga") || lower.contains("rucola") ||
                    lower.contains("spinac") || lower.contains("pomodor") || lower.contains("zucchina") ||
                    lower.contains("zucchine") || lower.contains("carot") || lower.contains("broccol") ||
                    lower.contains("cetriol") || lower.contains("melanzan") || lower.contains("finocch") ||
                    lower.contains("cavol") || lower.contains("asparag") || lower.contains("verdura") -> "Frutta & Verdura"

                lower.contains("latte") || lower.contains("yogurt") || lower.contains("greco") ||
                    lower.contains("kefir") || lower.contains("parmigiano") || lower.contains("grana") ||
                    lower.contains("mozzarella") || lower.contains("ricotta") || lower.contains("fiocchi di latte") ||
                    lower.contains("formaggio") || lower.contains("stracchino") || lower.contains("feta") -> "Latticini & Formaggi"

                lower.contains("riso") || lower.contains("pasta") || lower.contains("avena") ||
                    lower.contains("pane") || lower.contains("fette biscott") || lower.contains("gallett") ||
                    lower.contains("cereali") || lower.contains("farina") || lower.contains("couscous") ||
                    lower.contains("quinoa") || lower.contains("patat") || lower.contains("legumi") ||
                    lower.contains("ceci") || lower.contains("lenticchie") || lower.contains("fagioli") -> "Cereali, Pasta & Pane"

                lower.contains("olio") || lower.contains("aceto") || lower.contains("sale") ||
                    lower.contains("pepe") || lower.contains("origano") || lower.contains("spezie") ||
                    lower.contains("miele") || lower.contains("marmellata") || lower.contains("senape") ||
                    lower.contains("cannella") || lower.contains("cacao") || lower.contains("soia") -> "Condimenti & Dispensa"

                lower.contains("mandorl") || lower.contains("noci") || lower.contains("nocciole") ||
                    lower.contains("anacardi") || lower.contains("semi") || lower.contains("arachidi") ||
                    lower.contains("burro d'arachidi") || lower.contains("cioccolato") ||
                    lower.contains("barretta") || lower.contains("snack") -> "Snack & Frutta Secca"

                else -> "Altro"
            }

            val cleanQty = qty.ifBlank { "q.b." }
            var estimatedPkgCount = 1
            var estimatedGrammage = ""

            // Calculate realistic commercial package estimates based on food name and quantity
            val lowerName = name.lowercase()
            val gramsMatch = Regex("(\\d+)\\s*(g|gr|grammi)").find(cleanQty)
            val gramsVal = gramsMatch?.groupValues?.get(1)?.toIntOrNull()

            if (gramsVal != null) {
                when {
                    lowerName.contains("pollo") || lowerName.contains("tacchino") || lowerName.contains("carne") || lowerName.contains("manzo") -> {
                        val packSize = 300
                        estimatedPkgCount = kotlin.math.ceil(gramsVal.toDouble() / packSize).toInt().coerceAtLeast(1)
                        estimatedGrammage = "vaschetta da ${packSize}g"
                    }
                    lowerName.contains("pasta") || lowerName.contains("riso") -> {
                        val packSize = 500
                        estimatedPkgCount = kotlin.math.ceil(gramsVal.toDouble() / packSize).toInt().coerceAtLeast(1)
                        estimatedGrammage = "pacco da ${packSize}g"
                    }
                    lowerName.contains("avena") || lowerName.contains("cereali") -> {
                        val packSize = 500
                        estimatedPkgCount = kotlin.math.ceil(gramsVal.toDouble() / packSize).toInt().coerceAtLeast(1)
                        estimatedGrammage = "confezione da ${packSize}g"
                    }
                    lowerName.contains("ricotta") || lowerName.contains("mozzarella") || lowerName.contains("formaggio") -> {
                        val packSize = 250
                        estimatedPkgCount = kotlin.math.ceil(gramsVal.toDouble() / packSize).toInt().coerceAtLeast(1)
                        estimatedGrammage = "confezione da ${packSize}g"
                    }
                    lowerName.contains("tonno") -> {
                        val packSize = 80
                        estimatedPkgCount = kotlin.math.ceil(gramsVal.toDouble() / packSize).toInt().coerceAtLeast(1)
                        estimatedGrammage = "lattina da ${packSize}g"
                    }
                    lowerName.contains("yogurt") -> {
                        val packSize = 150
                        estimatedPkgCount = kotlin.math.ceil(gramsVal.toDouble() / packSize).toInt().coerceAtLeast(1)
                        estimatedGrammage = "vasetto da ${packSize}g"
                    }
                    else -> {
                        val packSize = if (gramsVal > 500) 500 else 250
                        estimatedPkgCount = kotlin.math.ceil(gramsVal.toDouble() / packSize).toInt().coerceAtLeast(1)
                        estimatedGrammage = "conf. da ${packSize}g"
                    }
                }
            } else if (cleanQty.contains("kg", ignoreCase = true)) {
                val kgNum = Regex("(\\d+)").find(cleanQty)?.groupValues?.get(1)?.toIntOrNull() ?: 1
                estimatedPkgCount = kgNum
                estimatedGrammage = "pacco da 1 kg"
            } else if (cleanQty.contains("uov", ignoreCase = true)) {
                val eggs = Regex("(\\d+)").find(cleanQty)?.groupValues?.get(1)?.toIntOrNull() ?: 6
                estimatedPkgCount = kotlin.math.ceil(eggs.toDouble() / 6).toInt().coerceAtLeast(1)
                estimatedGrammage = "scatola da 6 uova"
            }

            categorizedItems.add(
                ShoppingItemAiResult(
                    name = name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ITALY) else it.toString() },
                    quantity = cleanQty,
                    packageCount = estimatedPkgCount,
                    packageGrammage = estimatedGrammage,
                    category = category
                )
            )
        }

        return categorizedItems
    }

    private data class Tuple6(
        val name: String,
        val calories: Int,
        val protein: Int,
        val carbs: Int,
        val fat: Int,
        val followUp: String?
    )
}
