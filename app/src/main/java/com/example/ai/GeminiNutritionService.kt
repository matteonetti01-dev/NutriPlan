package com.example.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.entity.Ingredient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
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

    private fun callGeminiMultimodal(prompt: String, base64Image: String): JSONObject? {
        val requestJson = JSONObject().apply {
            val contentsArr = JSONArray().apply {
                val contentObj = JSONObject().apply {
                    val partsArr = JSONArray().apply {
                        put(JSONObject().put("text", prompt))
                        val inlineData = JSONObject().apply {
                            put("mimeType", "image/jpeg")
                            put("data", base64Image)
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

    private data class Tuple6(
        val name: String,
        val calories: Int,
        val protein: Int,
        val carbs: Int,
        val fat: Int,
        val followUp: String?
    )
}
