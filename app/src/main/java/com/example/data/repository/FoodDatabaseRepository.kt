package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.BuildConfig
import com.example.data.dao.NutritionDao
import com.example.data.entity.CachedFoodEntity
import com.example.data.entity.FoodItem
import com.example.data.fatsecret.FatSecretService
import com.example.data.fatsecret.VerifiedFoodDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class FoodDatabaseRepository(
    private val context: Context,
    private val dao: NutritionDao,
    private val fatSecretService: FatSecretService = FatSecretService()
) {
    private val TAG = "FoodDatabaseRepo"
    private val PREFS_NAME = "fatsecret_prefs"
    private val KEY_CLIENT_ID = "client_id"
    private val KEY_CLIENT_SECRET = "client_secret"

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _clientId = MutableStateFlow(getStoredClientId())
    val clientId: StateFlow<String> = _clientId.asStateFlow()

    private val _clientSecret = MutableStateFlow(getStoredClientSecret())
    val clientSecret: StateFlow<String> = _clientSecret.asStateFlow()

    private val _isConfigured = MutableStateFlow(checkIfConfigured(_clientId.value, _clientSecret.value))
    val isConfigured: StateFlow<Boolean> = _isConfigured.asStateFlow()

    private fun getStoredClientId(): String {
        val saved = prefs.getString(KEY_CLIENT_ID, "") ?: ""
        if (saved.isNotBlank()) return saved
        return try {
            val buildVal = BuildConfig.FATSECRET_CLIENT_ID
            if (buildVal.isNotBlank() && !buildVal.contains("MY_FATSECRET")) buildVal else ""
        } catch (e: Throwable) {
            ""
        }
    }

    private fun getStoredClientSecret(): String {
        val saved = prefs.getString(KEY_CLIENT_SECRET, "") ?: ""
        if (saved.isNotBlank()) return saved
        return try {
            val buildVal = BuildConfig.FATSECRET_CLIENT_SECRET
            if (buildVal.isNotBlank() && !buildVal.contains("MY_FATSECRET")) buildVal else ""
        } catch (e: Throwable) {
            ""
        }
    }

    private fun checkIfConfigured(id: String, secret: String): Boolean {
        return id.isNotBlank() && secret.isNotBlank() &&
                !id.contains("MY_FATSECRET") && !secret.contains("MY_FATSECRET")
    }

    fun saveCredentials(newClientId: String, newClientSecret: String) {
        val cleanId = newClientId.trim()
        val cleanSecret = newClientSecret.trim()

        prefs.edit()
            .putString(KEY_CLIENT_ID, cleanId)
            .putString(KEY_CLIENT_SECRET, cleanSecret)
            .apply()

        fatSecretService.clearCache()
        _clientId.value = cleanId
        _clientSecret.value = cleanSecret
        _isConfigured.value = checkIfConfigured(cleanId, cleanSecret)
    }

    suspend fun testConnection(testId: String, testSecret: String): Result<String> {
        return fatSecretService.testConnection(testId.trim(), testSecret.trim())
    }

    fun searchFoods(query: String): Flow<List<FoodItem>> = flow {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) {
            emit(VerifiedFoodDatabase.items.take(15))
            return@flow
        }

        // 1. Emit instant local search from verified db + Room cache
        val localVerified = VerifiedFoodDatabase.search(cleanQuery)
        val roomCached = try {
            dao.searchCachedFoods(cleanQuery).map { it.toFoodItem() }
        } catch (e: Exception) {
            emptyList()
        }

        val combinedLocal = mergeFoodLists(roomCached, localVerified)
        emit(combinedLocal)

        // 2. If FatSecret API credentials are available, fetch from API
        val currentId = _clientId.value
        val currentSecret = _clientSecret.value

        if (checkIfConfigured(currentId, currentSecret)) {
            val apiResult = fatSecretService.searchFoods(cleanQuery, currentId, currentSecret, maxResults = 30)
            if (apiResult.isSuccess) {
                val apiFoods = apiResult.getOrThrow()
                if (apiFoods.isNotEmpty()) {
                    // Cache to Room
                    withContext(Dispatchers.IO) {
                        try {
                            dao.insertCachedFoods(apiFoods.map { CachedFoodEntity.fromFoodItem(it) })
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed caching foods to Room", e)
                        }
                    }
                    // Combine API foods with local staples
                    val mergedFinal = mergeFoodLists(apiFoods, localVerified)
                    emit(mergedFinal)
                }
            } else {
                Log.w(TAG, "FatSecret API search error: ${apiResult.exceptionOrNull()?.message}")
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun cacheCustomFood(item: FoodItem) {
        withContext(Dispatchers.IO) {
            try {
                dao.insertCachedFood(CachedFoodEntity.fromFoodItem(item))
            } catch (e: Exception) {
                Log.e(TAG, "Error inserting custom food into cache", e)
            }
        }
    }

    private fun mergeFoodLists(primary: List<FoodItem>, secondary: List<FoodItem>): List<FoodItem> {
        val seen = mutableSetOf<String>()
        val result = mutableListOf<FoodItem>()

        for (item in primary) {
            val key = "${item.name.lowercase()}_${item.brand?.lowercase() ?: ""}"
            if (seen.add(key)) {
                result.add(item)
            }
        }

        for (item in secondary) {
            val key = "${item.name.lowercase()}_${item.brand?.lowercase() ?: ""}"
            if (seen.add(key)) {
                result.add(item)
            }
        }

        return result
    }
}
