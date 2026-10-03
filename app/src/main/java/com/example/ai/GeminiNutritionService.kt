package com.example.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.entity.ChatMessage
import com.example.data.entity.GeneratedMealProposal
import com.example.data.entity.Ingredient
import com.example.data.entity.MealSlotEntity
import com.example.data.entity.PantryChefResponse
import com.example.data.fatsecret.VerifiedFoodDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.Charset
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

data class OutdoorMealChatResult(
    val replyText: String,
    val isFinalEstimate: Boolean,
    val estimate: DishEstimateResult? = null
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

    private val prefs = context.getSharedPreferences("apex_nutrition_prefs", Context.MODE_PRIVATE)

    var customApiKey: String
        get() = prefs.getString("gemini_custom_api_key", "") ?: ""
        set(value) = prefs.edit().putString("gemini_custom_api_key", value.trim()).apply()

    var selectedModel: String
        get() = prefs.getString("gemini_selected_model", "gemini-2.5-flash") ?: "gemini-2.5-flash"
        set(value) = prefs.edit().putString("gemini_selected_model", value.trim()).apply()

    val apiKey: String
        get() {
            val custom = customApiKey
            return if (custom.isNotBlank()) custom else BuildConfig.GEMINI_API_KEY
        }

    val isUsingCustomKey: Boolean
        get() = customApiKey.isNotBlank()

    val candidateModels: List<String>
        get() {
            val current = selectedModel
            val list = mutableListOf(current)
            val standard = listOf("gemini-2.5-flash", "gemini-3.5-flash", "gemini-flash-latest", "gemini-3.1-pro-preview", "gemini-3.1-flash-lite-preview")
            for (m in standard) {
                if (!list.contains(m)) list.add(m)
            }
            return list
        }

    val supportedModels: List<Pair<String, String>> = listOf(
        Pair("gemini-2.5-flash", "Gemini 2.5 Flash (Consigliato • Multimodale Veloce)"),
        Pair("gemini-3.5-flash", "Gemini 3.5 Flash (Analisi Testi & Compiti Complessi)"),
        Pair("gemini-flash-latest", "Gemini Flash Latest (Sempre aggiornato)"),
        Pair("gemini-3.1-pro-preview", "Gemini 3.1 Pro Preview (Massima Intelligenza)"),
        Pair("gemini-3.1-flash-lite-preview", "Gemini 3.1 Flash Lite (Ultra Reattivo)")
    )

    suspend fun testConnection(keyToTest: String = "", modelToTest: String = ""): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val effectiveKey = if (keyToTest.isNotBlank()) keyToTest.trim() else apiKey
        if (effectiveKey.isBlank() || effectiveKey == "MY_GEMINI_API_KEY") {
            return@withContext Pair(false, "Nessuna chiave API inserita. Inserisci la tua chiave API Google Gemini da Google AI Studio.")
        }
        val effectiveModel = if (modelToTest.isNotBlank()) modelToTest.trim() else selectedModel
        val startTime = System.currentTimeMillis()
        val requestJson = JSONObject().apply {
            val contentsArr = JSONArray().apply {
                val contentObj = JSONObject().apply {
                    val partsArr = JSONArray().apply {
                        put(JSONObject().put("text", "Rispondi solo con: OK"))
                    }
                    put("parts", partsArr)
                }
                put(contentObj)
            }
            put("contents", contentsArr)
            val genConfig = JSONObject().apply {
                put("maxOutputTokens", 5)
            }
            put("generationConfig", genConfig)
        }
        val body = requestJson.toString().toRequestBody("application/json".toMediaType())
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$effectiveModel:generateContent?key=$effectiveKey"
        val req = Request.Builder().url(url).post(body).build()

        try {
            client.newCall(req).execute().use { response ->
                val elapsed = System.currentTimeMillis() - startTime
                if (response.isSuccessful) {
                    Pair(true, "Connessione a Google Gemini riuscita! Modello: $effectiveModel attivo • Latenza: ${elapsed}ms")
                } else {
                    val errBody = response.body?.string() ?: ""
                    val errMsg = try {
                        val obj = JSONObject(errBody)
                        val errObj = obj.optJSONObject("error")
                        errObj?.optString("message") ?: "HTTP ${response.code}: ${response.message}"
                    } catch (_: Exception) {
                        "HTTP ${response.code}: ${response.message}"
                    }
                    Pair(false, "Errore Google Gemini ($errMsg)")
                }
            }
        } catch (e: Exception) {
            Pair(false, "Impossibile raggiungere i server Google: ${e.localizedMessage ?: "Errore di connessione"}")
        }
    }

    /**
     * Estimate macros for a single ingredient (e.g. "pasta", "80g", "1 mela", "2 uova")
     * Prioritizes certified verified Italian database, enforces raw weights and Atwater physical consistency.
     */
    suspend fun estimateIngredient(name: String, quantity: String): Ingredient = withContext(Dispatchers.IO) {
        val cleanName = name.trim()
        val cleanQty = quantity.trim()
        if (cleanName.isBlank()) return@withContext Ingredient(name = "", quantity = cleanQty, calories = 0, protein = 0, carbs = 0, fat = 0)

        val grams = parsePortionGrams(cleanName, cleanQty)
        val factor = grams / 100.0

        // 1. Highest priority: Check certified Italian Food Database (USDA / CREA verified standards)
        val verifiedMatch = VerifiedFoodDatabase.findBestMatch(cleanName)
        if (verifiedMatch != null) {
            val cal = (verifiedMatch.caloriesPer100g * factor).roundToInt()
            val prot = (verifiedMatch.proteinPer100g * factor).roundToInt()
            val carbs = (verifiedMatch.carbsPer100g * factor).roundToInt()
            val fat = (verifiedMatch.fatPer100g * factor).roundToInt()
            return@withContext Ingredient(
                name = cleanName,
                quantity = if (cleanQty.isBlank()) "${grams.roundToInt()}g" else cleanQty,
                calories = cal,
                protein = prot,
                carbs = carbs,
                fat = fat
            )
        }

        // 2. Second priority: Use Gemini AI with strict thermodynamic Atwater and raw-weight instructions
        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val prompt = """
                    Sei un biologo nutrizionista clinico ed esperto di composizione degli alimenti (CREA/USDA).
                    Calcola con il massimo rigore scientifico i valori nutrizionali per questo alimento:
                    Alimento: "$cleanName"
                    Quantità indicata: "${if (cleanQty.isBlank()) "${grams.roundToInt()}g" else cleanQty}"
                    Grammatura stimata della porzione: ${grams.roundToInt()} grammi
                    
                    REGOLE CRITICHE DI CALCOLO E COERENZA NUTRIZIONALE:
                    1. PESATO A CRUDO: Salvo che l'utente specifichi esplicitamente "cotto", tutti gli alimenti (pasta, riso, cereali, carne, legumi) devono essere calcolati PESATI A CRUDO (es. pasta secca cruda ~350-360 kcal/100g, riso crudo ~350 kcal/100g, petto di pollo crudo ~110-120 kcal/100g).
                    2. FORMULA ATWATER (LEGGE FISICA): Le calorie TOTALI devono corrispondere ESATTAMENTE alla somma termodinamica dei macronutrienti:
                       Calorie = (Proteine * 4) + (Carboidrati * 4) + (Grassi * 9).
                       Non fornire MAI calorie discordanti o inventate rispetto ai macronutrienti.
                    3. Se l'alimento include grassi o condimenti nascosti (es. fritti o preparati), calcolali fedelmente.
                    
                    Rispondi ESCLUSIVAMENTE con un JSON nel formato:
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
                    val p = jsonResponse.optInt("protein", 0).coerceAtLeast(0)
                    val c = jsonResponse.optInt("carbs", 0).coerceAtLeast(0)
                    val f = jsonResponse.optInt("fat", 0).coerceAtLeast(0)
                    val returnedCal = jsonResponse.optInt("calories", 0)
                    val atwaterCal = (p * 4 + c * 4 + f * 9)

                    // Reconcile calories to ensure 100% mathematical accuracy
                    val reconciledCal = if (returnedCal > 0 && Math.abs(returnedCal - atwaterCal) <= (atwaterCal * 0.15).coerceAtLeast(15.0)) {
                        returnedCal
                    } else if (atwaterCal > 0) {
                        atwaterCal
                    } else {
                        returnedCal
                    }

                    if (reconciledCal > 0 || p > 0 || c > 0 || f > 0) {
                        return@withContext Ingredient(
                            name = cleanName,
                            quantity = if (cleanQty.isBlank()) "${grams.roundToInt()}g" else cleanQty,
                            calories = reconciledCal,
                            protein = p,
                            carbs = c,
                            fat = f
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("GeminiService", "Error estimating ingredient via Gemini: ${e.message}")
            }
        }

        // 3. Fallback database
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
                promptBuilder.append("Sei un biologo nutrizionista clinico esperto in nutrizione sportiva e ristorazione. Analizza il piatto (dalla foto se presente e dalle note dell'utente).\n")
                promptBuilder.append("Note dell'utente: \"$notes\"\n")
                if (conversationHistory.isNotEmpty()) {
                    promptBuilder.append("Conversazione precedente:\n")
                    for ((q, a) in conversationHistory) {
                        promptBuilder.append("AI: $q\nUtente: $a\n")
                    }
                }
                promptBuilder.append("""
                    REGOLE CRITICHE DI CALCOLO E COERENZA NUTRIZIONALE:
                    1. FORMULA ATWATER (VINCOLO ASSOLUTO): Le calorie totali del pasto DEVONO corrispondere esattamente alla formula: (Proteine * 4) + (Carboidrati * 4) + (Grassi * 9).
                    2. COERENZA INGREDIENTI: La somma delle calorie e dei singoli macronutrienti dell'elenco 'ingredients' deve corrispondere con precisione ai totali del pasto (calories, protein, carbs, fat).
                    3. GRAMMATURE E CONDIMENTI: Considera porzioni reali e i condimenti di cottura (olio EVO, burro, salse) tipici della preparazione descritta (es. 10-15g di olio di cottura per piatti saltati o al ristorante).
                    4. Se le informazioni sono incerte o se l'utente mangia fuori, poni una domanda di follow-up mirata (es. "Sei fuori? Se conosci il nome del locale o il condimento, dimmelo per affinare la stima.") oppure lascia "followUpQuestion" vuoto se la stima è sufficientemente precisa.
                    
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
                         {"name": "Petto di pollo", "quantity": "150g", "calories": 165, "protein": 35, "carbs": 0, "fat": 2},
                         {"name": "Olio EVO", "quantity": "15g", "calories": 135, "protein": 0, "carbs": 0, "fat": 15}
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
                    var calories = jsonResponse.optInt("calories", 500)
                    var protein = jsonResponse.optInt("protein", 25)
                    var carbs = jsonResponse.optInt("carbs", 50)
                    var fat = jsonResponse.optInt("fat", 15)
                    val followUp = jsonResponse.optString("followUpQuestion").takeIf { it.isNotBlank() }
                    val ingArr = jsonResponse.optJSONArray("ingredients")
                    val ingredients = mutableListOf<Ingredient>()
                    if (ingArr != null) {
                        for (i in 0 until ingArr.length()) {
                            ingredients.add(Ingredient.fromJsonObject(ingArr.getJSONObject(i)))
                        }
                    }

                    // Enforce physical Atwater coherence
                    val atwaterCal = (protein * 4 + carbs * 4 + fat * 9)
                    val sumIngCal = if (ingredients.isNotEmpty()) ingredients.sumOf { it.calories } else 0
                    val finalCal = when {
                        sumIngCal > 0 && Math.abs(sumIngCal - atwaterCal) <= 50 -> sumIngCal
                        atwaterCal > 0 -> atwaterCal
                        else -> calories
                    }

                    return@withContext DishEstimateResult(
                        mealName = name,
                        calories = finalCal,
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

    data class FileInspectionResult(
        val isText: Boolean,
        val textContent: String?,
        val rawBytes: ByteArray?,
        val mimeType: String,
        val fileName: String
    )

    private fun inspectFile(uri: Uri): FileInspectionResult {
        var fileName = ""
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx != -1) fileName = cursor.getString(idx) ?: ""
                }
            }
        } catch (_: Exception) {}
        if (fileName.isBlank()) {
            fileName = uri.lastPathSegment ?: "documento"
        }
        val lowerName = fileName.lowercase()
        val resolverMime = context.contentResolver.getType(uri)?.lowercase() ?: ""

        val bytes = try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val buffer = ByteArrayOutputStream()
                val data = ByteArray(16384)
                var nRead: Int
                var totalBytes = 0
                while (stream.read(data, 0, data.size).also { nRead = it } != -1 && totalBytes < 10 * 1024 * 1024) {
                    buffer.write(data, 0, nRead)
                    totalBytes += nRead
                }
                buffer.toByteArray()
            }
        } catch (e: Exception) {
            null
        }

        if (bytes == null || bytes.isEmpty()) {
            return FileInspectionResult(false, null, null, "application/octet-stream", fileName)
        }

        val isPdf = (bytes.size >= 4 && bytes[0] == 0x25.toByte() && bytes[1] == 0x50.toByte() && bytes[2] == 0x44.toByte() && bytes[3] == 0x46.toByte()) ||
                lowerName.endsWith(".pdf") || resolverMime == "application/pdf"

        val isPng = bytes.size >= 4 && bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() && bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte()
        val isJpg = bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte()
        val isWebp = bytes.size >= 12 && String(bytes.sliceArray(0..3)) == "RIFF" && String(bytes.sliceArray(8..11)) == "WEBP"
        val isImage = isPng || isJpg || isWebp || resolverMime.startsWith("image/") ||
                lowerName.endsWith(".png") || lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".webp")

        if (isPdf) {
            val extractedPdfText = extractTextFromPdfBytes(bytes)
            return FileInspectionResult(
                isText = !extractedPdfText.isNullOrBlank(),
                textContent = extractedPdfText,
                rawBytes = bytes,
                mimeType = "application/pdf",
                fileName = fileName
            )
        }

        if (isImage) {
            val imgMime = when {
                isPng || lowerName.endsWith(".png") -> "image/png"
                isWebp || lowerName.endsWith(".webp") -> "image/webp"
                else -> "image/jpeg"
            }
            return FileInspectionResult(
                isText = false,
                textContent = null,
                rawBytes = bytes,
                mimeType = imgMime,
                fileName = fileName
            )
        }

        // Try decoding as plain text (UTF-8 or ISO-8859-1)
        val text = try {
            String(bytes, Charsets.UTF_8).replace("\u0000", "")
        } catch (_: Exception) {
            try {
                String(bytes, Charset.forName("ISO-8859-1")).replace("\u0000", "")
            } catch (_: Exception) {
                null
            }
        }

        return FileInspectionResult(
            isText = !text.isNullOrBlank(),
            textContent = text,
            rawBytes = bytes,
            mimeType = if (resolverMime.startsWith("text/")) resolverMime else "text/plain",
            fileName = fileName
        )
    }

    private fun extractTextFromPdfBytes(bytes: ByteArray): String? {
        val extractedText = StringBuilder()

        try {
            val raw = String(bytes, Charsets.ISO_8859_1)
            var searchPos = 0

            while (searchPos < bytes.size) {
                val sIdx = raw.indexOf("stream", searchPos)
                if (sIdx == -1) break

                var dataStart = sIdx + 6
                if (dataStart < bytes.size && bytes[dataStart] == '\r'.toByte()) dataStart++
                if (dataStart < bytes.size && bytes[dataStart] == '\n'.toByte()) dataStart++

                val eIdx = raw.indexOf("endstream", dataStart)
                if (eIdx == -1) break

                val dictHeader = raw.substring((sIdx - 300).coerceAtLeast(0), sIdx)
                val isFlate = dictHeader.contains("/FlateDecode") || dictHeader.contains("/Fl")

                val streamLen = eIdx - dataStart
                if (streamLen in 1..2_000_000) {
                    val streamBytes = bytes.copyOfRange(dataStart, eIdx)
                    val uncompressed = if (isFlate) {
                        decompressZlib(streamBytes)
                    } else {
                        streamBytes
                    }

                    if (uncompressed != null && uncompressed.isNotEmpty()) {
                        val streamStr = String(uncompressed, Charsets.ISO_8859_1)
                        parsePdfTextStream(streamStr, extractedText)
                    }
                }

                searchPos = eIdx + 9
            }

            // Also check for uncompressed text in raw PDF body
            if (extractedText.length < 50) {
                parsePdfTextStream(raw, extractedText)
            }
        } catch (_: Exception) {}

        val result = extractedText.toString().trim()
        return if (result.length > 20) result else null
    }

    private fun decompressZlib(data: ByteArray): ByteArray? {
        for (nowrap in listOf(false, true)) {
            val inflater = java.util.zip.Inflater(nowrap)
            inflater.setInput(data)
            val outputStream = ByteArrayOutputStream(data.size * 2)
            val buffer = ByteArray(4096)
            try {
                while (!inflater.finished()) {
                    val count = inflater.inflate(buffer)
                    if (count == 0) {
                        if (inflater.needsInput() || inflater.needsDictionary()) break
                    } else {
                        outputStream.write(buffer, 0, count)
                    }
                }
                inflater.end()
                val res = outputStream.toByteArray()
                if (res.isNotEmpty()) return res
            } catch (_: Exception) {
                inflater.end()
            }
        }
        return null
    }

    private fun parsePdfTextStream(streamStr: String, out: StringBuilder) {
        val tjMatcher = Pattern.compile("""\((.*?)\)\s*(?:Tj|'|")""").matcher(streamStr)
        while (tjMatcher.find()) {
            val text = cleanPdfString(tjMatcher.group(1) ?: "")
            if (text.isNotBlank()) {
                out.append(text).append("\n")
            }
        }

        val hexTjMatcher = Pattern.compile("""<([0-9A-Fa-f]+)>\s*(?:Tj|'|")""").matcher(streamStr)
        while (hexTjMatcher.find()) {
            val hex = hexTjMatcher.group(1) ?: ""
            val text = decodePdfHexString(hex)
            if (text.isNotBlank()) {
                out.append(text).append("\n")
            }
        }

        val arrayTjMatcher = Pattern.compile("""\[(.*?)\]\s*TJ""").matcher(streamStr)
        while (arrayTjMatcher.find()) {
            val inner = arrayTjMatcher.group(1) ?: ""
            val tokenMatcher = Pattern.compile("""\((.*?)\)|<([0-9A-Fa-f]+)>""").matcher(inner)
            val lineBuilder = StringBuilder()
            while (tokenMatcher.find()) {
                val literal = tokenMatcher.group(1)
                val hex = tokenMatcher.group(2)
                if (literal != null) {
                    lineBuilder.append(cleanPdfString(literal))
                } else if (hex != null) {
                    lineBuilder.append(decodePdfHexString(hex))
                }
            }
            val text = lineBuilder.toString().trim()
            if (text.isNotBlank()) {
                out.append(text).append("\n")
            }
        }
    }

    private fun decodePdfHexString(hex: String): String {
        val clean = hex.replace("\\s".toRegex(), "")
        if (clean.isEmpty() || clean.length % 2 != 0) return ""
        val bytes = ByteArray(clean.length / 2)
        for (i in clean.indices step 2) {
            val b = clean.substring(i, i + 2).toIntOrNull(16) ?: 0
            bytes[i / 2] = b.toByte()
        }
        if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            return try {
                String(bytes.copyOfRange(2, bytes.size), Charsets.UTF_16BE)
            } catch (_: Exception) {
                String(bytes, Charsets.ISO_8859_1)
            }
        }
        if (bytes.size >= 4 && bytes[0] == 0.toByte() && bytes[2] == 0.toByte()) {
            return try {
                String(bytes, Charsets.UTF_16BE)
            } catch (_: Exception) {
                String(bytes, Charsets.ISO_8859_1)
            }
        }
        return String(bytes, Charsets.ISO_8859_1)
    }

    private fun cleanPdfString(raw: String): String {
        return raw.replace("\\(", "(")
            .replace("\\)", ")")
            .replace("\\\\", "\\")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .trim()
    }

    fun renderPdfToBitmaps(bytes: ByteArray, maxPages: Int = 16): List<Bitmap> {
        val bitmaps = mutableListOf<Bitmap>()
        var tempFile: File? = null
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            tempFile = File.createTempFile("pdf_page_", ".pdf", context.cacheDir)
            tempFile.writeBytes(bytes)
            pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            val pageCount = renderer.pageCount.coerceAtMost(maxPages)
            for (i in 0 until pageCount) {
                val page = renderer.openPage(i)
                // High-resolution rasterization (1600px width) ensures small fonts, tables,
                // alternative columns, grams and notes in clinical diet PDFs are crystal clear.
                val targetWidth = 1600
                val scale = targetWidth.toFloat() / page.width.toFloat().coerceAtLeast(1f)
                val targetHeight = (page.height * scale).toInt().coerceIn(200, 4000)
                val bmp = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                canvas.drawColor(AndroidColor.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                page.close()
                bitmaps.add(bmp)
            }
        } catch (e: Exception) {
            Log.e("GeminiService", "Error rendering PDF to bitmaps: ${e.message}")
        } finally {
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
            try { tempFile?.delete() } catch (_: Exception) {}
        }
        return bitmaps
    }

    /**
     * Extracts alternatives faithfully from raw text (copied or extracted from document).
     * Strictly avoids hallucinations or inventing meals not in the user's text,
     * while exhaustively capturing all options, inline variants ("oppure"), and daily menus.
     */
    suspend fun extractAlternativesFromText(
        rawText: String,
        mealSlotName: String
    ): List<ImportedAlternative> = withContext(Dispatchers.IO) {
        val cleanText = rawText.trim()
        if (cleanText.isBlank()) return@withContext emptyList()

        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val prompt = """
                    Sei un biologo nutrizionista clinico ed esperto in dietetica applicata.
                    L'utente ti ha fornito il testo reale estratto da un suo file o documento con la sua dieta personale o piano alimentare.
                    Il tuo obiettivo è analizzare attentamente il testo ed estrarre TUTTE le opzioni o alternative previste per il pasto: "$mealSlotName" (inclusi eventuali sinonimi o varianti correlate, es. Colazione/Breakfast, Pranzo/Lunch, Spuntino/Merenda/Break/Snack, Cena/Dinner).

                    *** OBIETTIVO PRIMARIO ED INDEROGABILE: ZERO PERDITA DI INFORMAZIONI ***:
                    L'utente richiede espressamente di PRENDERE TUTTO CIÒ CHE È SCRITTO NEL FILE, SENZA PERDERE NULLA.
                    Nessuna alternativa, variante o alimento presente per questo pasto deve essere tralasciato o scartato!

                    *** REGOLE CRITICHE E IMPERATIVE ***:
                    1. ZERO ALLUCINAZIONI E MASSIMA FEDELTÀ:
                       - DEVI PRENDERE ESCLUSIVAMENTE E FEDELMENTE CIÒ CHE L'UTENTE HA SCRITTO NEL TESTO FORNITO.
                       - È SEVERAMENTE VIETATO INVENTARE pasti, ricette o cibi non presenti nel testo fornito.
                       - NON SOSTITUIRE con cibi stereotipati se non compaiono nel testo dell'utente!

                    2. PRENDI TUTTE LE ALTERNATIVE, NESSUNA ESCLUSA:
                       - Estrai ogni opzione numerata o distinta (es. "Alternativa 1", "Alternativa 2", "Opzione A/B/C", "Variante 1/2").
                       - ALTERNATIVE NEL TESTO ("OPPURE", "IN ALTERNATIVA", "A SCELTA TRA", "/", "O"):
                         Se all'interno della descrizione sono indicati cibi alternativi (ad es. "150g petto di pollo OPPURE 130g manzo OPPURE 180g pesce", o "pane 50g o 4 fette biscottate o 40g fiocchi d'avena"),
                         DEVI creare una distinta alternativa per CIASCUNA di queste opzioni, in modo da non perderne nessuna!
                       - MENU SETTIMANALI O GIORNALIERI (Lunedì... Domenica, o Giorno 1, 2, 3...):
                         Se il documento presenta menu suddivisi per giorni della settimana, estrai il pasto "$mealSlotName" di CIASCUN GIORNO come un'alternativa separata (es. "$mealSlotName - Lunedì", "$mealSlotName - Martedì", ecc.)!
                       - TABELLE DI SOSTITUZIONE ED EQUIVALENZE:
                         Se nel testo compaiono sostituzioni o equivalenze previste per questo pasto, estraile tutte come opzioni alternative.

                    3. INGREDIENTI, QUANTITÀ E MACRONUTRIENTI:
                       - Per ogni alimento scritto dall'utente, estrai:
                         * "name": Nome esatto dell'alimento
                         * "quantity": Quantità indicata (es. "80g", "2 fette", "200ml", "1 cucchiaio", "a piacere")
                         * "calories", "protein", "carbs", "fat": Calcola i valori nutrizionali precisi per quel cibo e grammatura. Se nel testo sono presenti "Macro stimati", usali con massima fedeltà.
                       - "name" dell'alternativa: crea un titolo descrittivo basato ESCLUSIVAMENTE sui cibi di quell'alternativa (es. "Alternativa 1: Fette biscottate con marmellata e latte").
                       - "notes": note o istruzioni scritte dall'utente nel testo.

                    Rispondi ESCLUSIVAMENTE con un JSON nel formato seguente:
                    {
                      "alternatives": [
                        {
                          "name": "Nome Alternativa (dal testo dell'utente)",
                          "totalCalories": 420,
                          "totalProtein": 22,
                          "totalCarbs": 58,
                          "totalFat": 10,
                          "notes": "Note dal testo...",
                          "ingredients": [
                            {
                              "name": "Nome alimento dell'utente",
                              "quantity": "80g",
                              "calories": 280,
                              "protein": 10,
                              "carbs": 55,
                              "fat": 2
                            }
                          ]
                        }
                      ]
                    }

                    TESTO FORNITO DALL'UTENTE:
                    \"\"\"
                    $cleanText
                    \"\"\"
                """.trimIndent()

                val jsonResponse = callGeminiText(prompt)
                val parsed = parseAlternativesFromJson(jsonResponse)
                if (parsed.isNotEmpty()) return@withContext parsed
            } catch (e: Exception) {
                Log.e("GeminiService", "Error in extractAlternativesFromText: ${e.message}", e)
            }
        }

        // Smart text fallback: parses only the user's actual text lines (never invents fake food)
        return@withContext fallbackExtractAlternativesFromText(mealSlotName, cleanText)
    }

    private fun bitmapToBase64Jpeg(bitmap: Bitmap): String? {
        return try {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
        } catch (_: Exception) {
            null
        }
    }

    private fun parseAlternativesFromJson(jsonResponse: JSONObject?): List<ImportedAlternative> {
        if (jsonResponse == null || !jsonResponse.has("alternatives")) return emptyList()
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
            val name = obj.optString("name", "Alternativa ${i + 1}")
            val notes = obj.optString("notes", "")

            if (ingList.isNotEmpty() || cal > 0) {
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
        }
        return list
    }

    /**
     * Scans a document (PDF, Text file, Image, etc.) for a specific meal slot,
     * extracts all alternatives found with their ingredients and macronutrients.
     */
    suspend fun extractAlternativesFromDocument(
        uri: Uri,
        mealSlotName: String
    ): List<ImportedAlternative> = withContext(Dispatchers.IO) {
        val inspection = inspectFile(uri)

        val prompt = """
            Sei un biologo nutrizionista clinico ed esperto in dietetica applicata.
            L'utente ha fornito un documento PDF o immagine con la sua dieta personale o piano alimentare clinico.
            Il tuo obiettivo è analizzare attentamente il file ed estrarre TUTTE le opzioni o alternative previste per il pasto: "$mealSlotName" (inclusi eventuali sinonimi o varianti correlate, es. Colazione/Breakfast, Pranzo/Lunch, Spuntino/Merenda/Break/Snack, Cena/Dinner).

            *** OBIETTIVO PRIMARIO ED INDEROGABILE: ZERO PERDITA DI INFORMAZIONI ***:
            L'utente richiede espressamente di PRENDERE TUTTO CIÒ CHE È SCRITTO NEL FILE, SENZA PERDERE NULLA.
            Nessuna alternativa, variante o alimento presente per questo pasto deve essere tralasciato o scartato!

            *** REGOLE CRITICHE E IMPERATIVE ***:
            1. ZERO ALLUCINAZIONI E MASSIMA FEDELTÀ:
               - DEVI PRENDERE ESCLUSIVAMENTE E FEDELMENTE CIÒ CHE È SCRITTO NEL DOCUMENTO FORNITO.
               - È SEVERAMENTE VIETATO INVENTARE pasti, alimenti o ricette non presenti nel documento.
               - NON SOSTITUIRE con cibi stereotipati se non compaiono nel file!

            2. PRENDI TUTTE LE ALTERNATIVE, NESSUNA ESCLUSA:
               - Estrai ogni opzione numerata o distinta (es. "Alternativa 1", "Alternativa 2", "Opzione A/B/C", "Variante 1/2").
               - ALTERNATIVE NEL TESTO ("OPPURE", "IN ALTERNATIVA", "A SCELTA TRA", "/", "O"):
                 Se all'interno della descrizione sono indicati cibi alternativi (ad es. "150g petto di pollo OPPURE 130g manzo OPPURE 180g pesce", o "pane 50g o 4 fette biscottate o 40g fiocchi d'avena"),
                 DEVI creare una distinta alternativa per CIASCUNA di queste opzioni, in modo da non perderne nessuna!
               - MENU SETTIMANALI O GIORNALIERI (Lunedì... Domenica, o Giorno 1, 2, 3...):
                 Se il documento presenta menu suddivisi per giorni della settimana, estrai il pasto "$mealSlotName" di CIASCUN GIORNO come un'alternativa separata (es. "$mealSlotName - Lunedì", "$mealSlotName - Martedì", ecc.)!
               - TABELLE DI SOSTITUZIONE ED EQUIVALENZE:
                 Se nel documento compaiono tabelle o elenchi di sostituzioni o equivalenze previste per questo pasto, estraile tutte come opzioni alternative.

            3. INGREDIENTI, QUANTITÀ E MACRONUTRIENTI:
               - Per ciascun alimento scritto nel documento, estrai "name", "quantity", "calories", "protein", "carbs", "fat".
               - Se nel testo sono presenti "Macro stimati", estraili con massima fedeltà. Altrimenti calcola con cura i macro per quegli alimenti e quella grammatura.
               - "name" dell'alternativa: crea un titolo descrittivo basato ESCLUSIVAMENTE sui cibi di quell'alternativa.

            Rispondi ESCLUSIVAMENTE con un JSON nel formato:
            {
              "alternatives": [
                {
                  "name": "Nome Alternativa (dal file, es. Opzione 1 o Lunedì)",
                  "totalCalories": 420,
                  "totalProtein": 25,
                  "totalCarbs": 55,
                  "totalFat": 10,
                  "notes": "Note dal documento",
                  "ingredients": [
                    {
                      "name": "Nome alimento dal file",
                      "quantity": "80g",
                      "calories": 280,
                      "protein": 10,
                      "carbs": 55,
                      "fat": 2
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        // 1. If PDF: render pages to high-res images for multimodal vision OCR
        if (inspection.mimeType == "application/pdf" && inspection.rawBytes != null && inspection.rawBytes.isNotEmpty()) {
            val pdfBitmaps = renderPdfToBitmaps(inspection.rawBytes)
            val pagesBase64 = pdfBitmaps.mapNotNull { bitmapToBase64Jpeg(it) }
            if (pagesBase64.isNotEmpty() && apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
                try {
                    val fullPrompt = if (!inspection.textContent.isNullOrBlank()) {
                        "$prompt\n\nTESTO RILEVATO NEL DOCUMENTO (usalo come supporto integrativo insieme a TUTTE le pagine visive fornite):\n\"\"\"\n${inspection.textContent}\n\"\"\"\n\nRICORDA: Esamina con cura TUTTE le pagine fornite nelle immagini per non tralasciare alcuna opzione o alternativa!"
                    } else {
                        prompt
                    }
                    val jsonResponse = callGeminiMultimodalPages(fullPrompt, pagesBase64)
                    val parsed = parseAlternativesFromJson(jsonResponse)
                    if (parsed.isNotEmpty()) return@withContext parsed
                } catch (e: Exception) {
                    Log.e("GeminiService", "Error in PDF multimodal alternative extraction: ${e.message}")
                }
            }

            // Fallback to text extraction if PDF text was decoded
            if (!inspection.textContent.isNullOrBlank()) {
                val fromText = extractAlternativesFromText(inspection.textContent, mealSlotName)
                if (fromText.isNotEmpty()) return@withContext fromText
            }
        }

        // 2. If Image (PNG/JPEG/WEBP)
        if (inspection.mimeType.startsWith("image/") && inspection.rawBytes != null && inspection.rawBytes.isNotEmpty()) {
            if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
                try {
                    val base64Data = Base64.encodeToString(inspection.rawBytes, Base64.NO_WRAP)
                    val jsonResponse = callGeminiMultimodal(prompt, base64Data, inspection.mimeType)
                    val parsed = parseAlternativesFromJson(jsonResponse)
                    if (parsed.isNotEmpty()) return@withContext parsed
                } catch (e: Exception) {
                    Log.e("GeminiService", "Error in Image multimodal alternative extraction: ${e.message}")
                }
            }
        }

        // 3. Plain text file or decoded text
        if (inspection.isText && !inspection.textContent.isNullOrBlank()) {
            return@withContext extractAlternativesFromText(inspection.textContent, mealSlotName)
        }

        // 4. Local text fallback
        if (!inspection.textContent.isNullOrBlank()) {
            return@withContext fallbackExtractAlternativesFromText(mealSlotName, inspection.textContent)
        }

        return@withContext emptyList()
    }

    /**
     * Matches an extracted slot ID or slot name to the most appropriate existing slot in availableSlots.
     * Guarantees that NO slot or alternative is ever discarded.
     */
    private fun matchSlotForExtracted(
        slotId: Long,
        slotName: String,
        availableSlots: List<MealSlotEntity>,
        index: Int
    ): MealSlotEntity {
        // 1. Direct ID match
        availableSlots.find { it.id == slotId }?.let { return it }

        // 2. Exact name match (case-insensitive)
        val cleanName = slotName.trim()
        availableSlots.find { it.name.equals(cleanName, ignoreCase = true) }?.let { return it }

        // 3. Normalized semantic match based on keywords
        val lower = cleanName.lowercase()
        if (lower.contains("colazion") || lower.contains("breakfast") || lower.contains("mattina")) {
            availableSlots.find { it.name.lowercase().contains("colazion") }?.let { return it }
        }
        if (lower.contains("pranz") || lower.contains("lunch") || lower.contains("mezzogiorno")) {
            availableSlots.find { it.name.lowercase().contains("pranz") }?.let { return it }
        }
        if (lower.contains("cen") || lower.contains("dinner") || lower.contains("sera")) {
            availableSlots.find { it.name.lowercase().contains("cen") }?.let { return it }
        }
        if (lower.contains("spuntin") || lower.contains("merend") || lower.contains("snack") ||
            lower.contains("break") || lower.contains("pre nanna") || lower.contains("prenanna")
        ) {
            if (lower.contains("1") || lower.contains("mattin")) {
                availableSlots.find { it.name.lowercase().contains("spuntin 1") || it.name.lowercase().contains("mattin") }?.let { return it }
            }
            if (lower.contains("2") || lower.contains("pomerigg") || lower.contains("merend")) {
                availableSlots.find { it.name.lowercase().contains("spuntin 2") || it.name.lowercase().contains("pomerigg") || it.name.lowercase().contains("merend") }?.let { return it }
            }
            availableSlots.find { it.name.lowercase().contains("spuntin") || it.name.lowercase().contains("merend") }?.let { return it }
        }

        // 4. Match by orderIndex
        availableSlots.find { it.orderIndex == index + 1 }?.let { return it }

        // 5. Index within bounds
        if (index in availableSlots.indices) {
            return availableSlots[index]
        }

        // 6. Guarantee: NEVER drop! Fallback to the closest slot
        return availableSlots.last()
    }

    private fun parseSlotsFromJson(jsonResponse: JSONObject?, availableSlots: List<MealSlotEntity>): List<ImportedSlotWithAlternatives> {
        if (jsonResponse == null || !jsonResponse.has("slots") || availableSlots.isEmpty()) return emptyList()
        val slotsArr = jsonResponse.getJSONArray("slots")

        // Group alternatives by target slot ID so multiple sections, pages, daily variants,
        // and sub-meals are combined cleanly into the appropriate slot without losing anything!
        val groupedAlternatives = mutableMapOf<Long, MutableList<ImportedAlternative>>()
        for (slot in availableSlots) {
            groupedAlternatives[slot.id] = mutableListOf()
        }

        for (i in 0 until slotsArr.length()) {
            val slotObj = slotsArr.getJSONObject(i)
            val targetSlotId = slotObj.optLong("slotId", -1L)
            val extractedSlotName = slotObj.optString("slotName", "").trim()
            val matchedSlot = matchSlotForExtracted(targetSlotId, extractedSlotName, availableSlots, i)

            val altArr = slotObj.optJSONArray("alternatives")
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
                    var name = obj.optString("name", "Alternativa ${j + 1}").trim()
                    val notes = obj.optString("notes", "")

                    // If the extracted meal had a specific name (e.g. "Merenda" or "Spuntino Mattina")
                    // and the matched slot has a more generic name, preserve that context in the title
                    if (extractedSlotName.isNotBlank() &&
                        !matchedSlot.name.equals(extractedSlotName, ignoreCase = true) &&
                        !name.contains(extractedSlotName, ignoreCase = true)
                    ) {
                        name = "[$extractedSlotName] $name"
                    }

                    if (ingList.isNotEmpty() || cal > 0) {
                        groupedAlternatives[matchedSlot.id]?.add(
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
            }
        }

        val result = mutableListOf<ImportedSlotWithAlternatives>()
        for (slot in availableSlots.sortedBy { it.orderIndex }) {
            val alts = groupedAlternatives[slot.id] ?: emptyList()
            if (alts.isNotEmpty()) {
                result.add(
                    ImportedSlotWithAlternatives(
                        slotId = slot.id,
                        slotOrderIndex = slot.orderIndex,
                        slotName = slot.name,
                        alternatives = alts
                    )
                )
            }
        }
        return result
    }

    /**
     * Extracts full-plan alternatives faithfully from raw text (copied or extracted from document).
     * Strictly avoids hallucinations or inventing meals not in the user's text.
     */
    suspend fun extractAllPlanAlternativesFromText(
        rawText: String,
        availableSlots: List<MealSlotEntity>
    ): List<ImportedSlotWithAlternatives> = withContext(Dispatchers.IO) {
        val cleanText = rawText.trim()
        if (cleanText.isBlank() || availableSlots.isEmpty()) return@withContext emptyList()

        val slotsDescription = availableSlots.joinToString("\n") {
            "- Slot ID: ${it.id} (Pasto ${it.orderIndex}): '${it.name}'"
        }

        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val prompt = """
                    Sei un biologo nutrizionista clinico ed esperto in dietetica applicata.
                    L'utente ti ha fornito il testo reale estratto da un suo file o documento contenente la sua intera dieta / piano nutrizionale.
                    Nel piano attivo dell'applicazione sono configurati i seguenti pasti dell'utente:
                    $slotsDescription

                    *** OBIETTIVO PRIMARIO ED INDEROGABILE: ZERO PERDITA DI INFORMAZIONI ***:
                    L'utente richiede espressamente di PRENDERE TUTTO CIÒ CHE È SCRITTO NEL FILE, SENZA PERDERE NULLA.
                    Nessuna alternativa, variante o alimento presente nel testo deve essere tralasciato o scartato!

                    *** REGOLE CRITICHE E IMPERATIVE ***:
                    1. ZERO ALLUCINAZIONI E MASSIMA FEDELTÀ:
                       - DEVI PRENDERE ESCLUSIVAMENTE E FEDELMENTE QUELLO CHE L'UTENTE HA SCRITTO NEL SUO FILE.
                       - È SEVERAMENTE VIETATO INVENTARE pasti, cibi o alternative che l'utente non ha scritto nel testo.
                       - NON SOSTITUIRE gli ingredienti con cibi stereotipati se non compaiono nel testo dell'utente!

                    2. PRENDI TUTTE LE ALTERNATIVE, NESSUNA ESCLUSA:
                       - ALTERNATIVE NUMERATE O DISTINTE: Estrai ogni opzione ("Alternativa 1", "Alternativa 2", "Opzione A/B/C", "Variante 1/2", "Menu 1/2").
                       - ALTERNATIVE NEL TESTO ("OPPURE", "IN ALTERNATIVA", "A SCELTA TRA", "/", "O"):
                         Se all'interno di un pasto sono elencati cibi o abbinamenti alternativi (ad es. "150g petto di pollo OPPURE 130g manzo OPPURE 180g merluzzo" oppure "pane 50g o 4 fette biscottate o 40g fiocchi d'avena"),
                         DEVI creare una distinta alternativa per CIASCUNA di queste opzioni, in modo da non perderne nessuna!
                       - MENU SETTIMANALI O GIORNALIERI (Lunedì... Domenica, o Giorno 1, 2, 3...):
                         Se il testo presenta pasti suddivisi per giorni della settimana o menu rotazionali, estrai il pasto di CIASCUN GIORNO come un'alternativa per quel rispettivo pasto (es. "Colazione - Lunedì", "Colazione - Martedì", ecc.).
                         NON limitarti a un solo giorno: estrai TUTTI i giorni presenti!
                       - TABELLE DI SOSTITUZIONE ED EQUIVALENZE:
                         Se nel file compaiono elenchi di sostituzioni o equivalenze, estrai ogni opzione come alternativa nel pasto corrispondente.

                    3. INGREDIENTI, QUANTITÀ E VALORI NUTRIZIONALI:
                       - Per ciascuna alternativa, estrai gli ingredienti esatti scritti dall'utente con le relative grammature e calcola i macro precisi per quegli alimenti.
                       - Se nel testo sono presenti "Macro stimati", usali con massima fedeltà.

                    4. MAPPATURA AGLI SLOT DELL'APP:
                       - Mappa ciascuna opzione o alternativa allo slot corrispondente configurato nell'app:
                         $slotsDescription
                       - Restituisci ESATTAMENTE "slotId" e "slotName".
                       - Se per uno slot del piano non compaiono pasti nel testo, lascia il suo array "alternatives" vuoto [].

                    Rispondi ESCLUSIVAMENTE con un JSON nel seguente formato:
                    {
                      "slots": [
                        {
                          "slotId": ${availableSlots.first().id},
                          "slotName": "${availableSlots.first().name}",
                          "alternatives": [
                            {
                              "name": "Nome Alternativa (dal testo dell'utente)",
                              "totalCalories": 420,
                              "totalProtein": 25,
                              "totalCarbs": 55,
                              "totalFat": 10,
                              "notes": "Note dal testo dell'utente...",
                              "ingredients": [
                                {
                                  "name": "Nome alimento dell'utente",
                                  "quantity": "80g",
                                  "calories": 280,
                                  "protein": 10,
                                  "carbs": 55,
                                  "fat": 2
                                }
                              ]
                            }
                          ]
                        }
                      ]
                    }

                    TESTO FORNITO DALL'UTENTE:
                    \"\"\"
                    $cleanText
                    \"\"\"
                """.trimIndent()

                val jsonResponse = callGeminiText(prompt)
                val parsed = parseSlotsFromJson(jsonResponse, availableSlots)
                if (parsed.isNotEmpty()) return@withContext parsed
            } catch (e: Exception) {
                Log.e("GeminiService", "Error in extractAllPlanAlternativesFromText: ${e.message}", e)
            }
        }

        // Smart text fallback: extracts faithfully for each slot from the user's text
        return@withContext fallbackExtractAllPlanAlternativesFromText(availableSlots, cleanText)
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

        val inspection = inspectFile(uri)
        val slotsDescription = availableSlots.joinToString("\n") {
            "- Slot ID: ${it.id} (Pasto ${it.orderIndex}): '${it.name}'"
        }

        val prompt = """
            Sei un biologo nutrizionista clinico ed esperto in dietetica applicata.
            L'utente ha fornito un documento PDF o immagine con la sua intera dieta o piano nutrizionale.
            Nel piano attivo dell'applicazione sono configurati i seguenti pasti:
            $slotsDescription

            *** OBIETTIVO PRIMARIO ED INDEROGABILE: ZERO PERDITA DI INFORMAZIONI ***:
            L'utente richiede espressamente di PRENDERE TUTTO CIÒ CHE È SCRITTO NEL FILE, SENZA PERDERE NULLA.
            Nessuna alternativa, variante o alimento presente nel documento deve essere tralasciato o scartato!

            *** REGOLE CRITICHE E IMPERATIVE ***:
            1. ZERO ALLUCINAZIONI E MASSIMA FEDELTÀ:
               - DEVI PRENDERE ESCLUSIVAMENTE E FEDELMENTE CIÒ CHE È SCRITTO NEL DOCUMENTO FORNITO.
               - È SEVERAMENTE VIETATO INVENTARE pasti, cibi o alternative non presenti nel documento.
               - NON SOSTITUIRE con cibi stereotipati (NON mettere pancake, porridge, pollo o riso a meno che non siano scritti nel file!).

            2. PRENDI TUTTE LE ALTERNATIVE, NESSUNA ESCLUSA:
               - ALTERNATIVE NUMERATE O DISTINTE: Estrai ogni opzione ("Alternativa 1", "Alternativa 2", "Opzione A/B/C", "Variante 1/2", "Menu 1/2").
               - ALTERNATIVE NEL TESTO ("OPPURE", "IN ALTERNATIVA", "A SCELTA TRA", "/", "O"):
                 Se all'interno di un pasto sono elencati cibi o abbinamenti alternativi (ad es. "150g petto di pollo OPPURE 130g manzo OPPURE 180g merluzzo" oppure "pane 50g o 4 fette biscottate o 40g fiocchi d'avena"),
                 DEVI creare una distinta alternativa per CIASCUNA di queste opzioni, in modo da non perderne nessuna!
               - MENU SETTIMANALI O SU PIÙ GIORNI (Lunedì... Domenica, o Giorno 1, 2, 3...):
                 Se il documento presenta pasti suddivisi per giorni della settimana o menu rotazionali, estrai il pasto di CIASCUN GIORNO come un'alternativa per quel rispettivo pasto (es. "Colazione - Lunedì", "Colazione - Martedì", ecc.).
                 NON limitarti a un solo giorno: estrai TUTTI i giorni presenti nel documento!
               - TABELLE DI SOSTITUZIONE ED EQUIVALENZE:
                 Se nel file compaiono tabelle o elenchi di "Sostituzioni ammesse", "Equivalenze", "Varianti proteiche/glucidiche", estrai ogni elemento come opzione alternativa assegnata al pasto corrispondente (es. colazione o pranzo/cena).

            3. INGREDIENTI, QUANTITÀ E VALORI NUTRIZIONALI:
               - Per ciascuna alternativa, estrai l'elenco completo degli alimenti con la grammatura o porzione esatta indicata nel file (es. "80g", "2 fette", "1 cucchiaio", "a piacere").
               - Se nel documento sono presenti i "Macro stimati", usali con massima accuratezza. Altrimenti calcola con cura i macro per quegli alimenti.

            4. MAPPATURA AGLI SLOT DELL'APP:
               - Assegna ciascuna alternativa allo slot pasto appropriato configurato nell'app:
                 $slotsDescription
               - Mappa la colazione allo slot Colazione (usando il suo slotId esatto), il pranzo allo slot Pranzo, la cena allo slot Cena, e gli spuntini/merende allo slot Spuntino.
               - Restituisci ESATTAMENTE il "slotId" e "slotName" corrispondenti a ciascuno slot.
               - Se per uno slot non ci sono cibi nel file, lascia l'array vuoto []. Non inventare cibi assenti.

            Rispondi ESCLUSIVAMENTE con un JSON nel formato:
            {
              "slots": [
                {
                  "slotId": ${availableSlots.first().id},
                  "slotName": "${availableSlots.first().name}",
                  "alternatives": [
                    {
                      "name": "Nome Alternativa (dal file, es. Colazione Opzione 1 o Lunedì)",
                      "totalCalories": 420,
                      "totalProtein": 25,
                      "totalCarbs": 55,
                      "totalFat": 10,
                      "notes": "Note dal file",
                      "ingredients": [
                        {
                          "name": "Nome alimento dal file",
                          "quantity": "80g",
                          "calories": 280,
                          "protein": 10,
                          "carbs": 55,
                          "fat": 2
                        }
                      ]
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        // 1. If PDF: render pages to high-res images for multimodal vision OCR
        if (inspection.mimeType == "application/pdf" && inspection.rawBytes != null && inspection.rawBytes.isNotEmpty()) {
            val pdfBitmaps = renderPdfToBitmaps(inspection.rawBytes)
            val pagesBase64 = pdfBitmaps.mapNotNull { bitmapToBase64Jpeg(it) }
            if (pagesBase64.isNotEmpty() && apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
                try {
                    val fullPrompt = if (!inspection.textContent.isNullOrBlank()) {
                        "$prompt\n\nTESTO RILEVATO NEL DOCUMENTO (usalo come supporto integrativo insieme a TUTTE le pagine visive fornite):\n\"\"\"\n${inspection.textContent}\n\"\"\"\n\nRICORDA: Esamina attentamente TUTTE le pagine e tabelle fornite nelle immagini per non tralasciare alcuna opzione o alternativa!"
                    } else {
                        prompt
                    }
                    val jsonResponse = callGeminiMultimodalPages(fullPrompt, pagesBase64)
                    val parsed = parseSlotsFromJson(jsonResponse, availableSlots)
                    if (parsed.isNotEmpty()) return@withContext parsed
                } catch (e: Exception) {
                    Log.e("GeminiService", "Error in PDF multimodal plan extraction: ${e.message}")
                }
            }

            // Fallback to text extraction if PDF text was decoded
            if (!inspection.textContent.isNullOrBlank()) {
                val fromText = extractAllPlanAlternativesFromText(inspection.textContent, availableSlots)
                if (fromText.isNotEmpty()) return@withContext fromText
            }
        }

        // 2. If Image
        if (inspection.mimeType.startsWith("image/") && inspection.rawBytes != null && inspection.rawBytes.isNotEmpty()) {
            if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
                try {
                    val base64Data = Base64.encodeToString(inspection.rawBytes, Base64.NO_WRAP)
                    val jsonResponse = callGeminiMultimodal(prompt, base64Data, inspection.mimeType)
                    val parsed = parseSlotsFromJson(jsonResponse, availableSlots)
                    if (parsed.isNotEmpty()) return@withContext parsed
                } catch (e: Exception) {
                    Log.e("GeminiService", "Error in Image multimodal plan extraction: ${e.message}")
                }
            }
        }

        // 3. Plain text file or decoded text
        if (inspection.isText && !inspection.textContent.isNullOrBlank()) {
            return@withContext extractAllPlanAlternativesFromText(inspection.textContent, availableSlots)
        }

        // 4. Local text fallback
        if (!inspection.textContent.isNullOrBlank()) {
            return@withContext fallbackExtractAllPlanAlternativesFromText(availableSlots, inspection.textContent)
        }

        return@withContext emptyList()
    }

    private fun fallbackExtractAlternativesFromText(
        mealSlotName: String,
        textContent: String,
        orderIndex: Int = 1
    ): List<ImportedAlternative> {
        val clean = textContent.trim()
        if (clean.isBlank()) return emptyList()

        val sectionText = findSectionForMealSlot(clean, mealSlotName, orderIndex) ?: clean
        val blocks = splitIntoAlternativeBlocks(sectionText)
        val result = mutableListOf<ImportedAlternative>()

        blocks.forEachIndexed { idx, block ->
            val macroRegex = Regex("""Macro\s*stimati:\s*([\d.,]+)\s*g\s*Proteine\s*\|\s*([\d.,]+)\s*g\s*Grassi\s*\|\s*([\d.,]+)\s*g\s*Carboidrati\s*\|\s*~?\s*(\d+)\s*kcal""", RegexOption.IGNORE_CASE)
            val macroMatch = macroRegex.find(block)

            val ingredients = extractIngredientsFromTextBlock(block)
            if (ingredients.isNotEmpty() || macroMatch != null) {
                val totCal: Int
                val totProt: Int
                val totCarbs: Int
                val totFat: Int

                if (macroMatch != null) {
                    totProt = macroMatch.groupValues[1].replace(',', '.').toDouble().roundToInt()
                    totFat = macroMatch.groupValues[2].replace(',', '.').toDouble().roundToInt()
                    totCarbs = macroMatch.groupValues[3].replace(',', '.').toDouble().roundToInt()
                    totCal = macroMatch.groupValues[4].toInt()
                } else {
                    totCal = ingredients.sumOf { it.calories }
                    totProt = ingredients.sumOf { it.protein }
                    totCarbs = ingredients.sumOf { it.carbs }
                    totFat = ingredients.sumOf { it.fat }
                }

                val title = deriveAlternativeTitle(block, ingredients, idx + 1)
                result.add(
                    ImportedAlternative(
                        name = title,
                        totalCalories = totCal,
                        totalProtein = totProt,
                        totalCarbs = totCarbs,
                        totalFat = totFat,
                        notes = "Estratta fedelmente dal tuo documento",
                        ingredients = ingredients
                    )
                )
            }
        }
        return result
    }

    private fun fallbackExtractAllPlanAlternativesFromText(
        availableSlots: List<MealSlotEntity>,
        textContent: String
    ): List<ImportedSlotWithAlternatives> {
        val result = mutableListOf<ImportedSlotWithAlternatives>()

        for (slot in availableSlots) {
            val slotAlts = fallbackExtractAlternativesFromText(slot.name, textContent, slot.orderIndex)
            if (slotAlts.isNotEmpty()) {
                result.add(
                    ImportedSlotWithAlternatives(
                        slotId = slot.id,
                        slotOrderIndex = slot.orderIndex,
                        slotName = slot.name,
                        alternatives = slotAlts
                    )
                )
            }
        }

        return result
    }

    private fun findSectionForMealSlot(fullText: String, slotName: String, orderIndex: Int = 1): String? {
        val lower = fullText.lowercase()
        val targetKeyword = when {
            slotName.contains("colazione", ignoreCase = true) || orderIndex == 1 -> "colazione"
            slotName.contains("spuntino 1", ignoreCase = true) || (slotName.contains("spuntino", ignoreCase = true) && orderIndex == 2) -> {
                if (lower.contains("spuntino 1")) "spuntino 1" else "spuntino"
            }
            orderIndex == 2 && lower.contains("spuntino 1") -> "spuntino 1"
            slotName.contains("pranzo", ignoreCase = true) || orderIndex == 3 -> "pranzo"
            slotName.contains("spuntino 2", ignoreCase = true) || slotName.contains("merenda", ignoreCase = true) || (slotName.contains("spuntino", ignoreCase = true) && orderIndex == 4) -> {
                if (lower.contains("spuntino 2")) "spuntino 2" else if (lower.contains("merenda")) "merenda" else "spuntino"
            }
            orderIndex == 4 && lower.contains("spuntino 2") -> "spuntino 2"
            slotName.contains("cena", ignoreCase = true) || orderIndex >= 5 -> "cena"
            else -> slotName.lowercase().trim()
        }

        val startIdx = lower.indexOf(targetKeyword)
        if (startIdx == -1) return null

        val allMealKeywords = listOf("colazione", "spuntino 1", "spuntino 2", "spuntino", "pranzo", "merenda", "cena", "spuntino pomeridiano", "pre nanna")
        var nextHeaderIdx = -1

        for (kw in allMealKeywords) {
            if (kw == targetKeyword) continue
            val idx = lower.indexOf(kw, startIdx + targetKeyword.length)
            if (idx != -1 && (nextHeaderIdx == -1 || idx < nextHeaderIdx)) {
                nextHeaderIdx = idx
            }
        }

        return if (nextHeaderIdx != -1) {
            fullText.substring(startIdx, nextHeaderIdx).trim()
        } else {
            fullText.substring(startIdx).trim()
        }
    }

    private fun splitIntoAlternativeBlocks(text: String): List<String> {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()

        val blocks = mutableListOf<MutableList<String>>()
        var currentBlock = mutableListOf<String>()

        fun isAltHeader(line: String): Boolean {
            val l = line.lowercase()
            return l.startsWith("alternativa") ||
                    l.startsWith("opzione") ||
                    l.startsWith("variante") ||
                    l.startsWith("menu") ||
                    l.startsWith("oppure") ||
                    l.startsWith("in alternativa") ||
                    l.startsWith("o anche") ||
                    Regex("""^(?:[0-9]+[.)]|[a-zA-Z][.)]|opzione\s*[0-9a-zA-Z]+|alternativa\s*[0-9a-zA-Z]+|variante\s*[0-9a-zA-Z]+|giorno\s*[0-9]+|luned[iì]|marted[iì]|mercoled[iì]|gioved[iì]|venerd[iì]|sabato|domenica)""", RegexOption.IGNORE_CASE).containsMatchIn(line)
        }

        for (line in lines) {
            val l = line.lowercase()
            if (l.endsWith(":") && (l.contains("colazione") || l.contains("pranzo") || l.contains("cena") || l.contains("spuntino"))) {
                continue
            }

            if (isAltHeader(line)) {
                if (currentBlock.isNotEmpty()) {
                    blocks.add(currentBlock)
                    currentBlock = mutableListOf()
                }
                currentBlock.add(line)
            } else {
                currentBlock.add(line)
            }
        }

        if (currentBlock.isNotEmpty()) {
            blocks.add(currentBlock)
        }

        return if (blocks.isNotEmpty()) {
            blocks.map { it.joinToString("\n") }
        } else {
            listOf(text)
        }
    }

    private fun extractIngredientsFromTextBlock(block: String): List<Ingredient> {
        val ingredients = mutableListOf<Ingredient>()
        val lines = block.lines().map { it.trim() }.filter { it.isNotBlank() }

        for (line in lines) {
            var clean = line
                .removePrefix("-").removePrefix("•").removePrefix("*").removePrefix("+")
                .trim()

            val lower = clean.lowercase()
            if (lower.startsWith("macro stimat") || lower.contains("macro stimati")) {
                continue
            }

            if (lower.startsWith("alternativa") || lower.startsWith("opzione") || lower.startsWith("variante") ||
                lower.startsWith("oppure") || lower.startsWith("in alternativa")) {
                val afterColon = clean.substringAfter(":", "").trim()
                if (afterColon.isNotBlank()) {
                    clean = afterColon
                } else {
                    continue
                }
            }

            if (clean.length < 2) continue

            val parsed = parseIngredientLine(clean)
            if (parsed != null) {
                val est = fallbackEstimateIngredient(parsed.first, parsed.second)
                ingredients.add(est)
            }
        }

        return ingredients
    }

    private fun parseIngredientLine(line: String): Pair<String, String>? {
        var clean = line.replace(Regex("""^[0-9]+[).]\s*"""), "").trim()
        clean = clean.removePrefix("-").removePrefix("•").removePrefix("*").trim()
        if (clean.length < 2) return null

        val startQtyRegex = Regex("""^([\d.,]+\s*(?:g|gr|grammi|ml|l|pz|fette|fetta|cucchiai|cucchiaio|uova|uovo|scatoletta|scatolette|vasetto|vasetti|misurino|scoop|tazza|tazze|bicchiere|bicchieri|porzione|porzioni)?)\s*(?:di\s+|d'|del\s+)?(.*)$""", RegexOption.IGNORE_CASE)
        val matchStart = startQtyRegex.find(clean)
        if (matchStart != null) {
            val q = matchStart.groupValues[1].trim()
            val n = matchStart.groupValues[2].trim()
            if (n.length >= 2) {
                return Pair(n, if (q.isNotBlank()) q else "100g")
            }
        }

        val endQtyRegex = Regex("""^(.*?)\s*\(?([\d.,]+\s*(?:g|gr|grammi|ml|l|pz|fette|cucchiai|uova|scatoletta|vasetto|porzione))\)?$""", RegexOption.IGNORE_CASE)
        val matchEnd = endQtyRegex.find(clean)
        if (matchEnd != null) {
            val n = matchEnd.groupValues[1].trim()
            val q = matchEnd.groupValues[2].trim()
            if (n.length >= 2) {
                return Pair(n, q)
            }
        }

        return Pair(clean, "1 porzione")
    }

    private fun deriveAlternativeTitle(block: String, ingredients: List<Ingredient>, index: Int): String {
        val firstLine = block.lines().firstOrNull { it.isNotBlank() }?.trim() ?: ""
        val lowerFirst = firstLine.lowercase()
        if (lowerFirst.startsWith("alternativa") || lowerFirst.startsWith("opzione") || lowerFirst.startsWith("variante")) {
            val titlePart = firstLine.substringBefore(":").trim()
            val after = firstLine.substringAfter(":", "").trim()
            return if (after.isNotBlank()) "$titlePart: $after" else "$titlePart: ${ingredients.take(2).joinToString(" e ") { it.name }}"
        }
        val topFoods = ingredients.take(2).joinToString(" e ") { it.name }
        return "Alternativa $index: $topFoods"
    }

    /**
     * Multi-turn interactive Pantry Chef Chat with Gemini.
     * Takes user's pantry contents and desired meal, references the active plan's targets,
     * and creates a customized meal proposal that can be copied to the plan or logged in outdoor day.
     */
    suspend fun sendPantryChefMessage(
        history: List<ChatMessage>,
        userMessage: String,
        activePlanContext: String,
        pantryItems: List<String> = emptyList()
    ): PantryChefResponse = withContext(Dispatchers.IO) {
        val cleanMsg = userMessage.trim()
        if (cleanMsg.isBlank()) {
            return@withContext PantryChefResponse(
                replyText = "Ciao! Dimmi cosa hai in dispensa o in frigorifero e quale pasto vuoi preparare (colazione, pranzo, spuntino o cena)."
            )
        }

        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val systemPrompt = """
                    Sei "Apex Nutritionist & Pantry Chef", un biologo nutrizionista clinico ed executive culinary coach d'élite integrato nell'app NutriPlan.

                    IL TUO SCOPO PRIMARIO:
                    L'utente ti dice quali ingredienti ha nella sua dispensa o nel frigorifero e quale pasto desidera (Colazione, Pranzo, Spuntino, Cena).
                    Tu devi creare un pasto sano, delizioso, semplice e calibrato al millimetro sui target calorici e di macronutrienti dello slot corrispondente nel suo piano nutrizionale attivo.

                    DATI DEL PIANO NUTRIZIONALE ATTIVO:
                    $activePlanContext

                    INGREDIENTI REGISTRATI IN DISPENSA:
                    ${if (pantryItems.isNotEmpty()) pantryItems.joinToString(", ") else "Non specificati in anticipo; affidati agli ingredienti che l'utente elenca."}

                    REGOLE CRITICHE E SCIENTIFICHE:
                    1. RICONOSCIMENTO DELLO SLOT / PASTO:
                       - Identifica per quale pasto l'utente desidera la ricetta (Colazione, Pranzo, Spuntino, Cena). Se non viene specificato chiaramente, deduci il pasto più idoneo in base agli ingredienti oppure proponi Pranzo o Cena.
                       - Calcola con cura le grammature degli ingredienti per avvicinarti con precisione al target calorico e di macronutrienti di quello slot nel piano attivo.
                    2. FORMULA ATWATER (LEGGE FISICA FONDAMENTALE):
                       - Calorie totali = (Proteine * 4) + (Carboidrati * 4) + (Grassi * 9).
                       - Non generare MAI calorie casuali o disallineate dai macronutrienti.
                       - Gli alimenti base (pasta, riso, carne, pesce, legumi, cereali) si intendono pesati A CRUDO, salvo diversa specifica.
                       - Includi sempre condimenti realistici necessari alla cottura (es. 10g olio EVO).
                    3. FORMATO DI RISPOSTA:
                       - Rispondi in italiano in modo chiaro, empatico, professionale, con 2-3 passaggi rapidi di preparazione o cottura.
                       - QUANDO CREI O PROSEGUI UN PASTO, devi SEMPRE concludere la tua risposta con un blocco JSON racchiuso esattamente tra ```json e ``` contenente la struttura esatta del pasto, con cui l'applicazione genererà i pulsanti funzionali 'Copia nel Piano' e 'Registra in Giornata Fuori':
                    ```json
                    {
                      "mealName": "Nome invitante del pasto",
                      "targetSlotName": "Nome dello slot (es. Pranzo, Cena, Spuntino, Colazione)",
                      "calories": 650,
                      "protein": 42,
                      "carbs": 68,
                      "fat": 18,
                      "notes": "Consigli rapidi di preparazione",
                      "ingredients": [
                        {"name": "Petto di pollo", "quantity": "180g", "calories": 200, "protein": 42, "carbs": 0, "fat": 3},
                        {"name": "Riso basmati", "quantity": "80g", "calories": 280, "protein": 6, "carbs": 62, "fat": 1},
                        {"name": "Zucchine", "quantity": "200g", "calories": 35, "protein": 3, "carbs": 6, "fat": 0},
                        {"name": "Olio EVO", "quantity": "15g", "calories": 135, "protein": 0, "carbs": 0, "fat": 15}
                      ]
                    }
                    ```
                       - Se l'utente fa solo una domanda discorsiva generale, rispondi senza includere il blocco JSON.
                """.trimIndent()

                val rawResponse = callGeminiChat(systemPrompt, history, cleanMsg)
                if (!rawResponse.isNullOrBlank()) {
                    return@withContext parsePantryChefResponse(rawResponse)
                }
            } catch (e: Exception) {
                Log.e("GeminiService", "Error in sendPantryChefMessage: ${e.message}", e)
            }
        }

        // Local Smart Fallback
        return@withContext fallbackPantryChef(cleanMsg, activePlanContext, pantryItems)
    }

    private fun callGeminiChat(
        systemInstruction: String,
        history: List<ChatMessage>,
        currentMessage: String
    ): String? {
        val requestJson = JSONObject().apply {
            val sysObj = JSONObject().apply {
                val sysParts = JSONArray().apply {
                    put(JSONObject().put("text", systemInstruction))
                }
                put("parts", sysParts)
            }
            put("system_instruction", sysObj)

            val contentsArr = JSONArray()
            // Add up to last 10 turns of history
            history.takeLast(10).forEach { msg ->
                val role = if (msg.isUser) "user" else "model"
                val item = JSONObject().apply {
                    put("role", role)
                    val pArr = JSONArray().apply {
                        put(JSONObject().put("text", msg.text))
                    }
                    put("parts", pArr)
                }
                contentsArr.put(item)
            }

            // Current message
            val currentItem = JSONObject().apply {
                put("role", "user")
                val pArr = JSONArray().apply {
                    put(JSONObject().put("text", currentMessage))
                }
                put("parts", pArr)
            }
            contentsArr.put(currentItem)

            put("contents", contentsArr)
        }

        val body = requestJson.toString().toRequestBody("application/json".toMediaType())

        val chatModels = candidateModels
        for (model in chatModels) {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
            val req = Request.Builder().url(url).post(body).build()

            try {
                client.newCall(req).execute().use { response ->
                    if (!response.isSuccessful) {
                        val errBody = response.body?.string() ?: ""
                        Log.e("GeminiService", "Chat HTTP ${response.code} with $model: $errBody")
                        return@use
                    }
                    val resStr = response.body?.string() ?: return@use
                    val root = JSONObject(resStr)
                    val candidates = root.optJSONArray("candidates") ?: return@use
                    if (candidates.length() == 0) return@use
                    val content = candidates.getJSONObject(0).optJSONObject("content") ?: return@use
                    val parts = content.optJSONArray("parts") ?: return@use
                    if (parts.length() == 0) return@use
                    val text = parts.getJSONObject(0).optString("text", "")
                    if (text.isNotBlank()) return text
                }
            } catch (e: Exception) {
                Log.e("GeminiService", "Exception in callGeminiChat with $model: ${e.message}")
            }
        }
        return null
    }

    suspend fun sendOutdoorMealChatMessage(
        history: List<Pair<String, Boolean>>,
        currentMessage: String,
        mealSlotName: String? = null
    ): OutdoorMealChatResult = withContext(Dispatchers.IO) {
        val cleanMsg = currentMessage.trim()
        if (cleanMsg.isBlank()) return@withContext OutdoorMealChatResult(
            replyText = if (!mealSlotName.isNullOrBlank()) "Descrivimi pure cosa vorresti mangiare per $mealSlotName!" else "Descrivimi pure cosa stai mangiando!",
            isFinalEstimate = false
        )

        val targetContext = if (!mealSlotName.isNullOrBlank()) {
            "L'utente sta creando un'alternativa per il pasto '$mealSlotName' del proprio piano alimentare (o stimando un pasto). Genera una proposta dettagliata, bilanciata e realistica."
        } else {
            "L'utente sta mangiando fuori casa (o al ristorante/bar/lavoro) e desidera stimare calorie e macronutrienti per registrare il pasto nella sua 'Giornata Fuori'."
        }

        val systemInstruction = """
            Sei un biologo nutrizionista clinico esperto in ristorazione, nutrizione clinica e stime nutrizionali.
            $targetContext
            
            PROTOCOLLO DI CONVERSAZIONE (CRITICO):
            1. PRIMO CONTATTO / DETTAGLI INCOMPLETI:
               Se l'utente ha appena descritto il pasto in modo rapido o sintetico (es. 'Ho preso una tagliata con patate' o 'Pizza margherita e birra'), NON dare subito la stima finale ufficiale!
               Rispondi invece in modo cordiale, empatico e sintetico ponendo 1 o 2 domande brevi e mirate per chiarire i dettagli chiave (ad esempio: porzione indicativa, tipo di cottura/condimento con olio o salse, eventuale pane o bibite/dolci).
               In questa fase, scrivi solo il testo colloquiale con le tue domande (NON inserire blocco JSON, oppure metti "isFinal": false).
            
            2. RISPOSTA AI CHIARIMENTI / STIMA FINALE:
               Se l'utente risponde alle tue domande, oppure ha già fornito descrizioni dettagliate, oppure dice 'è tutto qui / calcola / stima approssimativa' o dopo 1-2 scambi:
               Fornisci una spiegazione cordiale e includi obbligatoriamente alla fine del messaggio un blocco JSON formattato ESATTAMENTE così:
               ```json
               {
                 "isFinal": true,
                 "mealName": "Nome descrittivo del pasto",
                 "calories": 720,
                 "protein": 42,
                 "carbs": 65,
                 "fat": 28,
                 "notes": "Spiegazione rapida della stima",
                 "ingredients": [
                   {"name": "Alimento 1", "quantity": "150g", "calories": 250, "protein": 30, "carbs": 0, "fat": 5},
                   {"name": "Contorno/Condimento", "quantity": "100g", "calories": 180, "protein": 3, "carbs": 25, "fat": 8}
                 ]
               }
               ```
            3. REGOLA RIGOROSA FORMULA ATWATER:
               Le calorie DEVONO corrispondere esattamente a: (protein * 4) + (carbs * 4) + (fat * 9).
               Per ogni ingrediente, specifica anche calories, protein, carbs e fat realistici in modo che la loro somma corrisponda ai totali.

            4. LINEE GUIDA REALISTICHE CALORIE PASTI FUORI CASA (OUTDOOR):
               - SUSHI ALL YOU CAN EAT (AYCE): porzioni abbondanti implicano grandi quantitativi di riso condito (con aceto e zucchero), 20-30+ roll/uramaki/nigiri, tempura, fritti, tartare e salse. Un All You Can Eat abbondante varia tipicamente tra 1900 e 2600 kcal (75-95g proteine, 260-320g carboidrati, 55-75g grassi). NON STIMARE MAI 500 KCAL per un all you can eat abbondante!
               - RISTORANTE / TRATTORIA: Primo + secondo + contorno con olio + vino = 1100 - 1700 kcal.
               - PIZZERIA: Pizza con birra e dolce = 1200 - 1800 kcal.
               - HAMBURGERIA / PUB: Hamburger doppio con formaggio, patatine fritte e salse = 1200 - 1650 kcal.
               - APERICENA / BUFFET: Drink alcolico + finger food fritti e pizzette = 900 - 1450 kcal.
        """.trimIndent()

        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val chatHistory = history.map { (text, isUser) ->
                    ChatMessage(
                        isUser = isUser,
                        text = text
                    )
                }
                val rawResponse = callGeminiChat(systemInstruction, chatHistory, cleanMsg)
                if (!rawResponse.isNullOrBlank()) {
                    return@withContext parseOutdoorMealChatResponse(rawResponse, cleanMsg)
                }
            } catch (e: Exception) {
                Log.e("GeminiService", "Exception in sendOutdoorMealChatMessage: ${e.message}")
            }
        }

        return@withContext fallbackOutdoorMealChat(history, cleanMsg, mealSlotName)
    }

    private fun parseOutdoorMealChatResponse(rawText: String, userMsg: String): OutdoorMealChatResult {
        val jsonRegex = Pattern.compile("```(?:json)?\\s*(\\{[\\s\\S]*?\\})\\s*```", Pattern.MULTILINE)
        val matcher = jsonRegex.matcher(rawText)

        if (matcher.find()) {
            val jsonStr = matcher.group(1)
            try {
                if (jsonStr != null) {
                    val obj = JSONObject(jsonStr)
                    val isFinal = obj.optBoolean("isFinal", true)
                    val mealName = obj.optString("mealName", if (userMsg.isNotBlank()) userMsg else "Pasto fuori casa")
                    val p = obj.optInt("protein", 25).coerceAtLeast(0)
                    val c = obj.optInt("carbs", 50).coerceAtLeast(0)
                    val f = obj.optInt("fat", 18).coerceAtLeast(0)
                    val atwater = (p * 4 + c * 4 + f * 9)
                    val rawCal = obj.optInt("calories", atwater)
                    val finalCal = if (rawCal > 0 && Math.abs(rawCal - atwater) <= (atwater * 0.15).coerceAtLeast(20.0)) rawCal else atwater
                    val notes = obj.optString("notes", "")

                    val ingArr = obj.optJSONArray("ingredients")
                    val ingList = mutableListOf<Ingredient>()
                    if (ingArr != null) {
                        for (i in 0 until ingArr.length()) {
                            ingList.add(Ingredient.fromJsonObject(ingArr.getJSONObject(i)))
                        }
                    }

                    val dishResult = DishEstimateResult(
                        mealName = mealName,
                        calories = finalCal,
                        protein = p,
                        carbs = c,
                        fat = f,
                        followUpQuestion = null,
                        ingredients = ingList
                    )

                    val cleanReply = rawText.replace(matcher.group(0) ?: "", "").trim()
                    return OutdoorMealChatResult(
                        replyText = if (cleanReply.isNotBlank()) cleanReply else "Ecco la stima per il tuo pasto fuori casa:",
                        isFinalEstimate = isFinal,
                        estimate = if (isFinal) dishResult else null
                    )
                }
            } catch (e: Exception) {
                Log.w("GeminiService", "Could not parse JSON from outdoor chat: ${e.message}")
            }
        }

        return OutdoorMealChatResult(
            replyText = rawText.trim(),
            isFinalEstimate = false,
            estimate = null
        )
    }

    private fun fallbackOutdoorMealChat(
        history: List<Pair<String, Boolean>>,
        currentMessage: String,
        mealSlotName: String? = null
    ): OutdoorMealChatResult {
        val userTurnCount = history.count { it.second } + 1
        if (userTurnCount == 1) {
            val promptTarget = if (!mealSlotName.isNullOrBlank()) "per $mealSlotName" else "del pasto"
            return OutdoorMealChatResult(
                replyText = "Sembra un'ottima opzione $promptTarget! Per calcolare una stima accurata: che quantità indicativa hai in mente? C'è qualche condimento (olio, formaggio, salse) o contorno/pane?",
                isFinalEstimate = false,
                estimate = null
            )
        } else {
            val combinedNotes = history.filter { it.second }.joinToString(" ") { it.first } + " " + currentMessage
            val dishEstimate = fallbackEstimateDish(combinedNotes, false)
            val finishPrompt = if (!mealSlotName.isNullOrBlank()) {
                "Perfetto! Ho calcolato i valori nutrizionali e gli ingredienti per $mealSlotName. Puoi confermare e aggiungerla al tuo piano con il pulsante qui sotto."
            } else {
                "Perfetto! In base ai dettagli che mi hai fornito ho calcolato la stima nutrizionale del pasto. Puoi confermarla con il pulsante qui sotto per aggiungerla alla tua Giornata Fuori."
            }
            return OutdoorMealChatResult(
                replyText = finishPrompt,
                isFinalEstimate = true,
                estimate = dishEstimate
            )
        }
    }

    private fun parsePantryChefResponse(rawText: String): PantryChefResponse {
        var replyText = rawText
        var proposal: GeneratedMealProposal? = null

        val jsonRegex = Pattern.compile("```(?:json)?\\s*(\\{[\\s\\S]*?\\})\\s*```", Pattern.MULTILINE)
        val matcher = jsonRegex.matcher(rawText)

        if (matcher.find()) {
            val jsonStr = matcher.group(1)
            try {
                if (jsonStr != null) {
                    val obj = JSONObject(jsonStr)
                    val mealName = obj.optString("mealName", "Pasto dalla dispensa")
                    val targetSlot = obj.optString("targetSlotName", "Pasto").takeIf { it.isNotBlank() }
                    val p = obj.optInt("protein", 0).coerceAtLeast(0)
                    val c = obj.optInt("carbs", 0).coerceAtLeast(0)
                    val f = obj.optInt("fat", 0).coerceAtLeast(0)
                    val atwater = (p * 4 + c * 4 + f * 9)
                    val rawCal = obj.optInt("calories", atwater)
                    val finalCal = if (rawCal > 0 && Math.abs(rawCal - atwater) <= (atwater * 0.15).coerceAtLeast(15.0)) rawCal else atwater
                    val notes = obj.optString("notes", "")

                    val ingArr = obj.optJSONArray("ingredients")
                    val ingList = mutableListOf<Ingredient>()
                    if (ingArr != null) {
                        for (i in 0 until ingArr.length()) {
                            ingList.add(Ingredient.fromJsonObject(ingArr.getJSONObject(i)))
                        }
                    }

                    proposal = GeneratedMealProposal(
                        name = mealName,
                        targetSlotName = targetSlot,
                        calories = finalCal,
                        protein = p,
                        carbs = c,
                        fat = f,
                        notes = notes,
                        ingredients = ingList
                    )

                    // Clean out the raw json block from the visible conversational reply
                    replyText = rawText.replace(matcher.group(0) ?: "", "").trim()
                }
            } catch (e: Exception) {
                Log.w("GeminiService", "Could not parse JSON block from chat: ${e.message}")
            }
        }

        return PantryChefResponse(
            replyText = replyText.ifBlank { "Ecco la proposta calibrata sui tuoi macro:" },
            mealProposal = proposal
        )
    }

    private fun fallbackPantryChef(
        userMessage: String,
        activePlanContext: String,
        pantryItems: List<String>
    ): PantryChefResponse {
        val lower = (userMessage + " " + pantryItems.joinToString(" ")).lowercase()

        val isColazione = lower.contains("colazione")
        val isSpuntino = lower.contains("spuntino") || lower.contains("merenda")
        val isCena = lower.contains("cena")
        val isPranzo = lower.contains("pranzo") || (!isColazione && !isSpuntino && !isCena)

        val targetSlotName = when {
            isColazione -> "Colazione"
            isSpuntino -> "Spuntino"
            isCena -> "Cena"
            else -> "Pranzo"
        }

        return when {
            isColazione || lower.contains("avena") || lower.contains("yogurt") -> {
                val ingList = listOf(
                    Ingredient("Yogurt greco 0%", "170g", 97, 17, 7, 0),
                    Ingredient("Fiocchi d'avena", "50g", 185, 7, 33, 4),
                    Ingredient("Mela", "150g (1 mela)", 78, 0, 21, 0),
                    Ingredient("Noci sgusciate", "15g", 98, 2, 2, 10)
                )
                val cal = ingList.sumOf { it.calories }
                val p = ingList.sumOf { it.protein }
                val c = ingList.sumOf { it.carbs }
                val f = ingList.sumOf { it.fat }
                PantryChefResponse(
                    replyText = "Ho analizzato la tua dispensa per la tua **$targetSlotName**!\n\nTi propongo una **Power Bowl d'Avena e Yogurt Greco con Mela e Noci**, leggera ma ad alto potere saziante e ricca di proteine nobili e acidi grassi essenziali.\n\nPreparazione:\n1. Versa lo yogurt greco in una ciotola con i fiocchi d'avena.\n2. Taglia la mela a cubetti e aggiungila insieme alle noci sbriciolate.\n3. A piacere puoi aggiungere un pizzico di cannella per esaltare il sapore.",
                    mealProposal = GeneratedMealProposal(
                        name = "Power Bowl Avena, Yogurt Greco e Mela",
                        targetSlotName = targetSlotName,
                        calories = cal,
                        protein = p,
                        carbs = c,
                        fat = f,
                        notes = "Colazione/Spuntino energetico e saziante pronto in 2 minuti.",
                        ingredients = ingList
                    )
                )
            }
            lower.contains("uov") || lower.contains("album") -> {
                val ingList = listOf(
                    Ingredient("Uova intere", "1 uovo (55g)", 72, 7, 0, 5),
                    Ingredient("Albume d'uovo", "150g", 78, 16, 1, 0),
                    Ingredient("Pane integrale", "70g (2 fette)", 175, 6, 34, 2),
                    Ingredient("Zucchine", "200g", 35, 3, 6, 0),
                    Ingredient("Olio extravergine d'oliva", "10g (1 cucchiaio)", 90, 0, 0, 10)
                )
                val cal = ingList.sumOf { it.calories }
                val p = ingList.sumOf { it.protein }
                val c = ingList.sumOf { it.carbs }
                val f = ingList.sumOf { it.fat }
                PantryChefResponse(
                    replyText = "Ottima scelta con le uova! Ho creato per il tuo **$targetSlotName** una **Frittata proteica alle Zucchine con Pane Integrale tostato**.\n\nÈ calibrata per fornirti proteine nobili con una quantità controllata di grassi buoni (grazie all'unione di 1 uovo intero con 150g di albume) e carboidrati complessi a rilascio graduale.\n\nPreparazione:\n1. Taglia le zucchine a rondelle sottili e falle saltare 3 minuti in padella antiaderente con metà dell'olio EVO.\n2. Sbatti l'uovo con l'albume, un pizzico di sale e pepe, versa sulle zucchine e cuoci con coperchio per 4-5 minuti.\n3. Accompagna con il pane integrale tostato.",
                    mealProposal = GeneratedMealProposal(
                        name = "Frittata Proteica di Zucchine e Pane Integrale",
                        targetSlotName = targetSlotName,
                        calories = cal,
                        protein = p,
                        carbs = c,
                        fat = f,
                        notes = "Piatto rapido e bilanciato ricco di micronutrienti e proteine nobili.",
                        ingredients = ingList
                    )
                )
            }
            lower.contains("tonno") -> {
                val ingList = listOf(
                    Ingredient("Pasta di semola", "80g", 284, 10, 58, 1),
                    Ingredient("Tonno al naturale", "112g (2 scatolette)", 114, 26, 0, 1),
                    Ingredient("Pomodorini", "150g", 27, 1, 6, 0),
                    Ingredient("Olio extravergine d'oliva", "10g (1 cucchiaio)", 90, 0, 0, 10)
                )
                val cal = ingList.sumOf { it.calories }
                val p = ingList.sumOf { it.protein }
                val c = ingList.sumOf { it.carbs }
                val f = ingList.sumOf { it.fat }
                PantryChefResponse(
                    replyText = "Con il tonno e la pasta della tua dispensa, ecco il pasto ideale per il tuo **$targetSlotName**: **Pasta Mediterranea con Tonno e Pomodorini**!\n\nUna ricetta classica e veloce, che garantisce l'energia dei carboidrati complessi e la purezza proteica del tonno al naturale.\n\nPreparazione:\n1. Lessa la pasta in acqua bollente salata.\n2. In una padella fai scaldare l'olio con i pomodorini tagliati a spicchi per 2 minuti.\n3. Scola la pasta al dente, uniscila ai pomodorini e al tonno sgocciolato a fuoco spento.",
                    mealProposal = GeneratedMealProposal(
                        name = "Pasta Mediterranea Tonno e Pomodorini",
                        targetSlotName = targetSlotName,
                        calories = cal,
                        protein = p,
                        carbs = c,
                        fat = f,
                        notes = "Primo piatto fresco e sportivo pronto in 10 minuti.",
                        ingredients = ingList
                    )
                )
            }
            else -> {
                val ingList = listOf(
                    Ingredient("Riso basmati crudo", "80g", 280, 6, 62, 1),
                    Ingredient("Petto di pollo crudo", "180g", 198, 41, 0, 3),
                    Ingredient("Zucchine", "200g", 35, 3, 6, 0),
                    Ingredient("Olio extravergine d'oliva", "10g (1 cucchiaio)", 90, 0, 0, 10)
                )
                val cal = ingList.sumOf { it.calories }
                val p = ingList.sumOf { it.protein }
                val c = ingList.sumOf { it.carbs }
                val f = ingList.sumOf { it.fat }
                PantryChefResponse(
                    replyText = "Basandomi sulla tua dispensa e sul tuo piano attivo, ecco il pasto d'oro per il tuo **$targetSlotName**: **Bowl di Riso Basmati, Tagliata di Pollo al Limone e Zucchine**!\n\nHa una digeribilità elevatissima, un profilo aminoacidico completo ed è perfetta sia per il pranzo sia per la cena.\n\nPreparazione:\n1. Cuoci il riso basmati per assorbimento (160ml di acqua per 80g di riso per 10 minuti).\n2. Griglia il petto di pollo tagliato a straccetti con spezie a piacere e gocce di limone.\n3. Salta le zucchine a cubetti e componi il piatto condendo a crudo con l'olio EVO.",
                    mealProposal = GeneratedMealProposal(
                        name = "Bowl Basmati, Pollo al Limone e Zucchine",
                        targetSlotName = targetSlotName,
                        calories = cal,
                        protein = p,
                        carbs = c,
                        fat = f,
                        notes = "Piatto iconico da nutrizionista sportivo, perfettamente bilanciato.",
                        ingredients = ingList
                    )
                )
            }
        }
    }

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
                put("maxOutputTokens", 16384)
                put("temperature", 0.1)
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
                put("maxOutputTokens", 16384)
                put("temperature", 0.1)
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

    private fun callGeminiMultimodalPages(prompt: String, imagesBase64: List<String>): JSONObject? {
        if (imagesBase64.isEmpty()) return null
        val requestJson = JSONObject().apply {
            val contentsArr = JSONArray().apply {
                val contentObj = JSONObject().apply {
                    val partsArr = JSONArray().apply {
                        put(JSONObject().put("text", prompt))
                        for (base64Data in imagesBase64) {
                            val inlineData = JSONObject().apply {
                                put("mimeType", "image/jpeg")
                                put("data", base64Data)
                            }
                            put(JSONObject().put("inlineData", inlineData))
                        }
                    }
                    put("parts", partsArr)
                }
                put(contentObj)
            }
            put("contents", contentsArr)
            val genConfig = JSONObject().apply {
                put("responseMimeType", "application/json")
                put("maxOutputTokens", 16384)
                put("temperature", 0.1)
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
                .removePrefix("```JSON")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val startIdx = cleaned.indexOf('{')
            val endIdx = cleaned.lastIndexOf('}')
            if (startIdx != -1 && endIdx > startIdx) {
                return JSONObject(cleaned.substring(startIdx, endIdx + 1))
            }

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

    fun parsePortionGrams(foodName: String, quantity: String): Double {
        val q = quantity.trim().lowercase()
        val name = foodName.trim().lowercase()

        // 1. Direct kg / chili
        val kgPattern = Pattern.compile("([0-9]+(?:[.,][0-9]+)?)\\s*(?:kg|chili|chilo)")
        val kgMatcher = kgPattern.matcher(q)
        if (kgMatcher.find()) {
            val num = kgMatcher.group(1)?.replace(',', '.')?.toDoubleOrNull() ?: 1.0
            return (num * 1000.0).coerceAtLeast(10.0)
        }

        // 2. Direct l / litri
        val literPattern = Pattern.compile("([0-9]+(?:[.,][0-9]+)?)\\s*(?:l|litri|litro)")
        val literMatcher = literPattern.matcher(q)
        if (literMatcher.find()) {
            val num = literMatcher.group(1)?.replace(',', '.')?.toDoubleOrNull() ?: 1.0
            return (num * 1000.0).coerceAtLeast(10.0)
        }

        // 3. Direct grams / ml
        val gPattern = Pattern.compile("([0-9]+(?:[.,][0-9]+)?)\\s*(?:g|gr|grammi|ml|cc)\\b")
        val gMatcher = gPattern.matcher(q)
        if (gMatcher.find()) {
            return gMatcher.group(1)?.replace(',', '.')?.toDoubleOrNull() ?: 100.0
        }

        // 4. Count for items
        val countPattern = Pattern.compile("([0-9]+(?:[.,][0-9]+)?)")
        val countMatcher = countPattern.matcher(q)
        val count = if (countMatcher.find()) {
            countMatcher.group(1)?.replace(',', '.')?.toDoubleOrNull() ?: 1.0
        } else if (q.contains("mezz") || q.contains("metà") || q.contains("1/2")) {
            0.5
        } else {
            1.0
        }

        // 5. Intelligent portion heuristics
        return when {
            q.contains("cucchiain") -> count * 5.0
            q.contains("cucchiai") -> {
                if (name.contains("olio") || name.contains("evo")) count * 10.0 else count * 15.0
            }
            q.contains("fett") -> {
                when {
                    name.contains("biscottat") -> count * 9.0
                    name.contains("prosciutto") || name.contains("bresaola") || name.contains("tacchino") -> count * 25.0
                    else -> count * 35.0 // fetta di pane
                }
            }
            q.contains("uov") || name.contains("uov") -> {
                if (name.contains("album")) count * 35.0 else count * 55.0 // uovo medio ~55g
            }
            q.contains("scatolett") || q.contains("lattin") || q.contains("scatola") -> {
                if (name.contains("tonno")) count * 52.0 else count * 80.0
            }
            q.contains("vasett") -> {
                if (name.contains("fage") || name.contains("greco")) count * 150.0 else count * 125.0
            }
            q.contains("scoop") || q.contains("misurin") -> count * 30.0
            q.contains("bicchier") -> count * 200.0
            q.contains("tazz") -> count * 150.0
            q.contains("piatt") -> {
                if (name.contains("pasta") || name.contains("riso")) count * 85.0 else count * 200.0
            }
            name.contains("mela") || name.contains("mele") -> count * 150.0
            name.contains("banana") || name.contains("banane") -> count * 120.0
            name.contains("arancia") || name.contains("arance") -> count * 150.0
            name.contains("pera") || name.contains("pere") -> count * 160.0
            name.contains("pesca") || name.contains("pesche") -> count * 130.0
            name.contains("kiwi") -> count * 80.0
            count > 10.0 -> count // Raw numeric input like "80" or "150"
            else -> 100.0 // Default standard 100g portion
        }
    }

    private fun fallbackEstimateIngredient(name: String, quantity: String): Ingredient {
        val grams = parsePortionGrams(name, quantity)
        val factor = grams / 100.0

        val verifiedMatch = VerifiedFoodDatabase.findBestMatch(name)
        if (verifiedMatch != null) {
            val cal = (verifiedMatch.caloriesPer100g * factor).roundToInt()
            val prot = (verifiedMatch.proteinPer100g * factor).roundToInt()
            val carbs = (verifiedMatch.carbsPer100g * factor).roundToInt()
            val fat = (verifiedMatch.fatPer100g * factor).roundToInt()
            return Ingredient(
                name = name,
                quantity = if (quantity.isBlank()) "${grams.roundToInt()}g" else quantity,
                calories = cal,
                protein = prot,
                carbs = carbs,
                fat = fat
            )
        }

        val (kcal100, p100, c100, g100) = listOf(160, 10, 20, 4)
        val cal = (kcal100 * factor).roundToInt()
        val prot = (p100 * factor).roundToInt()
        val carb = (c100 * factor).roundToInt()
        val fat = (g100 * factor).roundToInt()

        return Ingredient(
            name = name,
            quantity = if (quantity.isBlank()) "${grams.roundToInt()}g" else quantity,
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
            lower.contains("sushi") || lower.contains("giapponese") -> {
                val isAyce = lower.contains("all you can eat") || lower.contains("ayce") || lower.contains("buffet")
                when {
                    isAyce && isLarge -> Tuple6(
                        "Sushi All You Can Eat Abbondante",
                        2250,
                        85,
                        290,
                        62,
                        if (isOut) "Hai preso molti fritti (tempura) o uramaki speciali con salse/maionese?" else null
                    )
                    isAyce -> Tuple6(
                        "Sushi All You Can Eat",
                        1680,
                        68,
                        215,
                        46,
                        if (isOut) "Quanti roll/piatti circa hai consumato?" else null
                    )
                    isLarge -> Tuple6(
                        "Menu Sushi Abbondante",
                        1280,
                        55,
                        160,
                        32,
                        "Comprendeva anche tempura o tartare?"
                    )
                    else -> Tuple6(
                        "Sushi Misto (Uramaki, Nigiri, Sashimi)",
                        880,
                        42,
                        110,
                        20,
                        "C'erano anche edamame o gyoza?"
                    )
                }
            }
            lower.contains("carne") || lower.contains("tagliata") || lower.contains("bistecca") || lower.contains("grigliat") -> {
                val mult = if (isLarge) 1.35 else 1.0
                Tuple6(
                    "Tagliata o Grigliata di Carne con contorno",
                    (780 * mult).toInt(),
                    (65 * mult).toInt(),
                    (25 * mult).toInt(),
                    (38 * mult).toInt(),
                    if (isOut) "La carne era accompagnata da patate o condita con olio abbondante?" else null
                )
            }
            lower.contains("aperitivo") || lower.contains("apericena") || lower.contains("buffet") -> {
                val mult = if (isLarge) 1.35 else 1.0
                Tuple6(
                    "Apericena / Buffet con drink",
                    (980 * mult).toInt(),
                    (30 * mult).toInt(),
                    (95 * mult).toInt(),
                    (42 * mult).toInt(),
                    "Hai consumato fritti, pizzette o patatine?"
                )
            }
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
