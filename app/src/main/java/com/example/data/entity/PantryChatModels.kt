package com.example.data.entity

import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val mealProposal: GeneratedMealProposal? = null
)

data class GeneratedMealProposal(
    val name: String,
    val targetSlotName: String? = null,
    val calories: Int = 0,
    val protein: Int = 0,
    val carbs: Int = 0,
    val fat: Int = 0,
    val notes: String = "",
    val ingredients: List<Ingredient> = emptyList()
)

data class PantryChefResponse(
    val replyText: String,
    val mealProposal: GeneratedMealProposal? = null
)
