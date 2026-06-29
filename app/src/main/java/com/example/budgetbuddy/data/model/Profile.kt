package com.example.budgetbuddy.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String = "",
    @SerialName("full_name") val fullName: String = "",
    val email: String = "",
    @SerialName("monthly_budget") val monthlyBudget: Double = 0.0,
    @SerialName("created_at") val createdAt: String = ""
)