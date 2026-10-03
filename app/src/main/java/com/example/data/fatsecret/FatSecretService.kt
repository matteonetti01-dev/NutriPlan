package com.example.data.fatsecret

import android.util.Base64
import android.util.Log
import com.example.data.entity.FoodItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class FatSecretService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) {
    private val TAG = "FatSecretService"
    private val TOKEN_URL = "https://oauth.fatsecret.com/connect/token"
    private val API_URL = "https://platform.fatsecret.com/rest/server.api"

    @Volatile
    private var cachedToken: String? = null
    @Volatile
    private var tokenExpiryTimeMs: Long = 0

    suspend fun getAccessToken(clientId: String, clientSecret: String): Result<String> {
        val cleanId = clientId.trim()
        val cleanSecret = clientSecret.trim()

        if (cleanId.isBlank() || cleanSecret.isBlank()) {
            return Result.failure(IllegalArgumentException("Client ID e Client Secret non configurati."))
        }

        // Return cached token if still valid (with 60s buffer)
        val now = System.currentTimeMillis()
        if (cachedToken != null && now < tokenExpiryTimeMs - 60_000) {
            return Result.success(cachedToken!!)
        }

        return withContext(Dispatchers.IO) {
            try {
                val credentials = "$cleanId:$cleanSecret"
                val basicAuth = "Basic " + Base64.encodeToString(credentials.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

                val body = FormBody.Builder()
                    .add("grant_type", "client_credentials")
                    .add("scope", "basic")
                    .build()

                val request = Request.Builder()
                    .url(TOKEN_URL)
                    .post(body)
                    .header("Authorization", basicAuth)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    Log.e(TAG, "OAuth failed with code ${response.code}: $responseBody")
                    val errorMsg = try {
                        val obj = JSONObject(responseBody)
                        obj.optString("error_description", obj.optString("error", "Errore di autenticazione"))
                    } catch (e: Exception) {
                        "Errore HTTP ${response.code}: $responseBody"
                    }
                    return@withContext Result.failure(Exception("Autenticazione FatSecret fallita: $errorMsg"))
                }

                val json = JSONObject(responseBody)
                val token = json.getString("access_token")
                val expiresInSec = json.optLong("expires_in", 86400)

                cachedToken = token
                tokenExpiryTimeMs = System.currentTimeMillis() + (expiresInSec * 1000)

                Result.success(token)
            } catch (e: Exception) {
                Log.e(TAG, "Token request failed", e)
                Result.failure(e)
            }
        }
    }

    suspend fun searchFoods(
        query: String,
        clientId: String,
        clientSecret: String,
        maxResults: Int = 30
    ): Result<List<FoodItem>> {
        val q = query.trim()
        if (q.isBlank()) return Result.success(emptyList())

        val tokenResult = getAccessToken(clientId, clientSecret)
        if (tokenResult.isFailure) {
            return Result.failure(tokenResult.exceptionOrNull() ?: Exception("Impossibile ottenere token FatSecret"))
        }
        val token = tokenResult.getOrThrow()

        return withContext(Dispatchers.IO) {
            try {
                val url = "$API_URL?method=foods.search&search_expression=${java.net.URLEncoder.encode(q, "UTF-8")}&format=json&max_results=$maxResults"
                val request = Request.Builder()
                    .url(url)
                    .get()
                    .header("Authorization", "Bearer $token")
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Errore ricerca FatSecret: HTTP ${response.code}"))
                }

                val json = JSONObject(responseBody)
                if (json.has("error")) {
                    val err = json.getJSONObject("error")
                    return@withContext Result.failure(Exception(err.optString("message", "Errore FatSecret API")))
                }

                val foodsObj = json.optJSONObject("foods") ?: return@withContext Result.success(emptyList())
                val results = mutableListOf<FoodItem>()

                val foodElement = foodsObj.opt("food")
                if (foodElement is JSONArray) {
                    for (i in 0 until foodElement.length()) {
                        parseFoodJson(foodElement.getJSONObject(i))?.let { results.add(it) }
                    }
                } else if (foodElement is JSONObject) {
                    parseFoodJson(foodElement)?.let { results.add(it) }
                }

                Result.success(results)
            } catch (e: Exception) {
                Log.e(TAG, "Search foods failed", e)
                Result.failure(e)
            }
        }
    }

    suspend fun testConnection(clientId: String, clientSecret: String): Result<String> {
        val tokenRes = getAccessToken(clientId, clientSecret)
        if (tokenRes.isFailure) {
            return Result.failure(tokenRes.exceptionOrNull() ?: Exception("Connessione fallita"))
        }

        // Test actual food search
        val searchRes = searchFoods("mela", clientId, clientSecret, maxResults = 3)
        if (searchRes.isFailure) {
            return Result.failure(searchRes.exceptionOrNull() ?: Exception("Ricerca di test fallita"))
        }

        val items = searchRes.getOrThrow()
        val summary = if (items.isNotEmpty()) {
            "Connessione stabilita con successo! Trovati ${items.size} risultati di test ('${items.first().name}')."
        } else {
            "Connessione stabilita con successo con FatSecret Platform API!"
        }
        return Result.success(summary)
    }

    private fun parseFoodJson(obj: JSONObject): FoodItem? {
        val id = obj.optString("food_id", "")
        val name = obj.optString("food_name", "").trim()
        if (id.isBlank() || name.isBlank()) return null

        val brand = obj.optString("brand_name", null)?.takeIf { it.isNotBlank() }
        val description = obj.optString("food_description", "")

        // FatSecret food_description format:
        // "Per 100g - Calories: 355kcal | Fat: 1.50g | Carbs: 72.00g | Protein: 12.50g"
        // or "Per 1 serving (125g) - Calories: 150kcal | Fat: 2.50g | Carbs: 20.00g | Protein: 8.00g"
        val parsed = parseFoodDescription(description)

        return FoodItem(
            id = "fs_$id",
            name = name,
            brand = brand,
            servingDescription = parsed.servingDesc,
            servingAmount = parsed.servingGrams,
            servingUnit = "g",
            caloriesPer100g = parsed.caloriesPer100g,
            proteinPer100g = parsed.proteinPer100g,
            carbsPer100g = parsed.carbsPer100g,
            fatPer100g = parsed.fatPer100g,
            source = "fatsecret"
        )
    }

    private data class ParsedDescription(
        val servingDesc: String,
        val servingGrams: Double,
        val caloriesPer100g: Double,
        val proteinPer100g: Double,
        val carbsPer100g: Double,
        val fatPer100g: Double
    )

    private fun parseFoodDescription(desc: String): ParsedDescription {
        // Defaults
        var servingDesc = "100g"
        var servingGrams = 100.0
        var calories = 0.0
        var fat = 0.0
        var carbs = 0.0
        var protein = 0.0

        val parts = desc.split(" - ")
        if (parts.isNotEmpty()) {
            servingDesc = parts[0].removePrefix("Per ").trim()
            val gramsMatch = """([0-9]+(?:[.,][0-9]+)?)\s*g""".toRegex(RegexOption.IGNORE_CASE).find(servingDesc)
            if (gramsMatch != null) {
                servingGrams = gramsMatch.groupValues[1].replace(',', '.').toDoubleOrNull() ?: 100.0
            }
        }

        val calMatch = """(?:Calories|Calorie):\s*([0-9]+(?:[.,][0-9]+)?)""".toRegex(RegexOption.IGNORE_CASE).find(desc)
        if (calMatch != null) {
            calories = calMatch.groupValues[1].replace(',', '.').toDoubleOrNull() ?: 0.0
        }

        val fatMatch = """(?:Fat|Grassi):\s*([0-9]+(?:[.,][0-9]+)?)""".toRegex(RegexOption.IGNORE_CASE).find(desc)
        if (fatMatch != null) {
            fat = fatMatch.groupValues[1].replace(',', '.').toDoubleOrNull() ?: 0.0
        }

        val carbsMatch = """(?:Carbs|Carbohydrate|Carboidrati):\s*([0-9]+(?:[.,][0-9]+)?)""".toRegex(RegexOption.IGNORE_CASE).find(desc)
        if (carbsMatch != null) {
            carbs = carbsMatch.groupValues[1].replace(',', '.').toDoubleOrNull() ?: 0.0
        }

        val proteinMatch = """(?:Protein|Proteine):\s*([0-9]+(?:[.,][0-9]+)?)""".toRegex(RegexOption.IGNORE_CASE).find(desc)
        if (proteinMatch != null) {
            protein = proteinMatch.groupValues[1].replace(',', '.').toDoubleOrNull() ?: 0.0
        }

        val factor = if (servingGrams > 0) 100.0 / servingGrams else 1.0

        return ParsedDescription(
            servingDesc = servingDesc,
            servingGrams = servingGrams,
            caloriesPer100g = calories * factor,
            proteinPer100g = protein * factor,
            carbsPer100g = carbs * factor,
            fatPer100g = fat * factor
        )
    }

    fun clearCache() {
        cachedToken = null
        tokenExpiryTimeMs = 0
    }
}
