package com.example.budgetbuddy.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Budget(
    val id: Int = 0,
    @SerialName("user_id") val userId: String = "",
    @SerialName("category_id") val categoryId: Int = 0,
    @SerialName("budget_limit") val budgetLimit: Double = 0.0,
    val month: Int = 0,
    val year: Int = 0
)