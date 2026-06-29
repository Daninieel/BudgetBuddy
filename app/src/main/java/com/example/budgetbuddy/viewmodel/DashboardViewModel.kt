package com.example.budgetbuddy.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetbuddy.data.model.Transaction
import com.example.budgetbuddy.data.repository.AuthRepository
import com.example.budgetbuddy.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = false,
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val balance: Double = 0.0,
    val monthlySavings: Double = 0.0,
    val recentTransactions: List<Transaction> = emptyList(),
    val errorMessage: String? = null
)

class DashboardViewModel : ViewModel() {

    private val authRepository = AuthRepository()
    private val transactionRepository = TransactionRepository()

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        val userId = authRepository.currentUserId() ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            transactionRepository.getTransactions(userId)
                .onSuccess { transactions ->
                    val income = transactions
                        .filter { it.type == "income" }
                        .sumOf { it.amount }
                    val expenses = transactions
                        .filter { it.type == "expense" }
                        .sumOf { it.amount }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            totalIncome = income,
                            totalExpenses = expenses,
                            balance = income - expenses,
                            monthlySavings = income - expenses,
                            recentTransactions = transactions.take(5)
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
                }
        }
    }
}