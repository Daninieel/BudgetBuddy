package com.example.budgetbuddy.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetbuddy.data.repository.AuthRepository
import com.example.budgetbuddy.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AnalyticsUiState(
    val isLoading: Boolean = false,
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val categoryBreakdown: Map<String, Double> = emptyMap(),
    val highestCategory: String = "",
    val highestAmount: Double = 0.0,
    val errorMessage: String? = null
)

class AnalyticsViewModel : ViewModel() {

    private val authRepository = AuthRepository()
    private val transactionRepository = TransactionRepository()

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init { loadAnalytics() }

    fun loadAnalytics() {
        val userId = authRepository.currentUserId() ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            transactionRepository.getTransactions(userId)
                .onSuccess { transactions ->
                    val income = transactions.filter { it.type == "income" }.sumOf { it.amount }
                    val expenses = transactions.filter { it.type == "expense" }.sumOf { it.amount }

                    val breakdown = transactions
                        .filter { it.type == "expense" }
                        .groupBy { it.categoryId.toString() }
                        .mapValues { entry -> entry.value.sumOf { it.amount } }

                    val highest = breakdown.maxByOrNull { it.value }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            totalIncome = income,
                            totalExpenses = expenses,
                            categoryBreakdown = breakdown,
                            highestCategory = highest?.key ?: "",
                            highestAmount = highest?.value ?: 0.0
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
                }
        }
    }
}