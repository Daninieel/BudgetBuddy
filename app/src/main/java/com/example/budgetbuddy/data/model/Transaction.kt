package com.example.budgetbuddy.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Transaction(
    val id: Int = 0,
    @SerialName("user_id") val userId: String = "",
    @SerialName("category_id") val categoryId: Int = 0,
    val amount: Double = 0.0,
    val description: String = "",
    val type: String = "",
    @SerialName("transaction_date") val transactionDate: String = "",
    @SerialName("created_at") val createdAt: String = ""
)