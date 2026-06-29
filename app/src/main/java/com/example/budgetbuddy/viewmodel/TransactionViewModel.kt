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

data class TransactionUiState(
    val isLoading: Boolean = false,
    val transactions: List<Transaction> = emptyList(),
    val errorMessage: String? = null
)

class TransactionViewModel : ViewModel() {

    private val authRepository = AuthRepository()
    private val transactionRepository = TransactionRepository()

    private val _uiState = MutableStateFlow(TransactionUiState())
    val uiState: StateFlow<TransactionUiState> = _uiState.asStateFlow()

    init { loadTransactions() }

    fun loadTransactions() {
        val userId = authRepository.currentUserId()
        android.util.Log.d("TransactionVM", "loadTransactions userId: $userId")
        if (userId == null) {
            _uiState.update { it.copy(errorMessage = "Not logged in") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            transactionRepository.getTransactions(userId)
                .onSuccess { transactions ->
                    android.util.Log.d("TransactionVM", "Loaded ${transactions.size} transactions")
                    _uiState.update { it.copy(isLoading = false, transactions = transactions) }
                }
                .onFailure { e ->
                    android.util.Log.e("TransactionVM", "Load error: ${e.message}", e)
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
                }
        }
    }

    fun addTransaction(amount: Double, description: String, type: String, date: String, categoryId: Int) {
        val userId = authRepository.currentUserId() ?: return
        viewModelScope.launch {
            val transaction = Transaction(
                userId = userId,
                categoryId = categoryId,
                amount = amount,
                description = description,
                type = type,
                transactionDate = date
            )
            transactionRepository.addTransaction(transaction)
                .onSuccess {
                    _uiState.update { it.copy(errorMessage = null) }
                    loadTransactions()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(errorMessage = e.message) }
                }
        }
    }
}