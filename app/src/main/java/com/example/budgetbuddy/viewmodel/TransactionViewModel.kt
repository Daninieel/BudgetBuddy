package com.example.budgetbuddy.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetbuddy.data.model.Transaction
import com.example.budgetbuddy.data.repository.AuthRepository
import com.example.budgetbuddy.data.repository.BudgetRepository
import com.example.budgetbuddy.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TransactionUiState(
    val isLoading: Boolean = false,
    val transactions: List<Transaction> = emptyList(),
    val errorMessage: String? = null
)

class TransactionViewModel : ViewModel() {

    private val authRepository = AuthRepository()
    private val transactionRepository = TransactionRepository()
    private val budgetRepository = BudgetRepository()

    private val _uiState = MutableStateFlow(TransactionUiState())
    val uiState: StateFlow<TransactionUiState> = _uiState.asStateFlow()

    init { loadTransactions() }

    fun loadTransactions() {
        val userId = authRepository.currentUserId()
        if (userId == null) {
            _uiState.update { it.copy(errorMessage = "Not logged in") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            transactionRepository.getTransactions(userId)
                .onSuccess { transactions ->
                    _uiState.update { it.copy(isLoading = false, transactions = transactions, errorMessage = null) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
                }
        }
    }

    /**
     * Validates a new or edited EXPENSE against:
     *  1. A budget must exist for the category.
     *  2. New category spending must not exceed the category's budget limit.
     *  3. New total expenses must not exceed total income (balance can't go negative).
     *
     * excludingTransactionId: when editing, pass the transaction's own id so its
     * current amount is excluded from "spent so far" before checking.
     *
     * Returns null if valid, or an error message string if blocked.
     */
    private suspend fun validateExpense(
        userId: String,
        categoryId: Int,
        newAmount: Double,
        excludingTransactionId: Int? = null
    ): String? {
        val transactionsResult = transactionRepository.getTransactions(userId)
        val budgetsResult = budgetRepository.getBudgets(userId)

        if (transactionsResult.isFailure) return "Could not verify your balance. Try again."
        if (budgetsResult.isFailure) return "Could not verify your budget. Try again."

        val allTransactions = transactionsResult.getOrDefault(emptyList())
            .filterNot { it.id == excludingTransactionId }

        val currentMonth = LocalDate.now().monthValue
        val currentYear = LocalDate.now().year

        val budget = budgetsResult.getOrDefault(emptyList())
            .firstOrNull {
                it.categoryId == categoryId && it.month == currentMonth && it.year == currentYear
            }

        if (budget == null) {
            return "No budget set for this category yet. Set one in Budget Manager first."
        }

        val categorySpent = allTransactions
            .filter { it.type == "expense" && it.categoryId == categoryId }
            .sumOf { it.amount }

        if (categorySpent + newAmount > budget.budgetLimit) {
            val remaining = (budget.budgetLimit - categorySpent).coerceAtLeast(0.0)
            return "This exceeds your category budget. ₱%.2f remaining.".format(remaining)
        }

        val totalIncome = allTransactions.filter { it.type == "income" }.sumOf { it.amount }
        val totalExpenses = allTransactions.filter { it.type == "expense" }.sumOf { it.amount }

        if (totalExpenses + newAmount > totalIncome) {
            val remainingBalance = (totalIncome - totalExpenses).coerceAtLeast(0.0)
            return "This would exceed your available balance. ₱%.2f remaining.".format(remainingBalance)
        }

        return null
    }

    fun addTransaction(
        amount: Double,
        description: String,
        type: String,
        date: String,
        categoryId: Int,
        onBlocked: (String) -> Unit = {},
        onSuccess: () -> Unit = {}
    ) {
        val userId = authRepository.currentUserId() ?: return
        viewModelScope.launch {
            if (type == "expense") {
                val error = validateExpense(userId, categoryId, amount)
                if (error != null) {
                    onBlocked(error)
                    return@launch
                }
            }

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
                    onSuccess()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(errorMessage = e.message) }
                    onBlocked(e.message ?: "Failed to save transaction.")
                }
        }
    }

    fun updateTransaction(
        id: Int,
        amount: Double,
        description: String,
        type: String,
        date: String,
        categoryId: Int,
        onBlocked: (String) -> Unit = {},
        onSuccess: () -> Unit = {}
    ) {
        val userId = authRepository.currentUserId() ?: return
        viewModelScope.launch {
            if (type == "expense") {
                val error = validateExpense(userId, categoryId, amount, excludingTransactionId = id)
                if (error != null) {
                    onBlocked(error)
                    return@launch
                }
            }

            val transaction = Transaction(
                id = id,
                userId = userId,
                categoryId = categoryId,
                amount = amount,
                description = description,
                type = type,
                transactionDate = date
            )
            transactionRepository.updateTransaction(transaction)
                .onSuccess {
                    _uiState.update { it.copy(errorMessage = null) }
                    loadTransactions()
                    onSuccess()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(errorMessage = e.message) }
                    onBlocked(e.message ?: "Failed to save transaction.")
                }
        }
    }

    fun deleteTransaction(id: Int) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(id)
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