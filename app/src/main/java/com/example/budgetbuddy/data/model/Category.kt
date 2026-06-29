package com.example.budgetbuddy.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Category(
    val id: Int = 0,
    val name: String = "",
    val icon: String = "",
    val type: String = ""
)