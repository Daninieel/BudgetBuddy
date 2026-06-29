package com.example.budgetbuddy.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetbuddy.data.model.Budget
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

data class BudgetWithSpending(
    val budget: Budget,
    val spent: Double,
    val percentage: Double,
    val isAlert: Boolean
)

data class BudgetUiState(
    val isLoading: Boolean = false,
    val budgetsWithSpending: List<BudgetWithSpending> = emptyList(),
    val errorMessage: String? = null
)

class BudgetViewModel : ViewModel() {

    private val authRepository = AuthRepository()
    private val budgetRepository = BudgetRepository()
    private val transactionRepository = TransactionRepository()

    private val _uiState = MutableStateFlow(BudgetUiState())
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()

    init { loadBudgets() }

    fun loadBudgets() {
        val userId = authRepository.currentUserId() ?: return
        val currentMonth = LocalDate.now().monthValue
        val currentYear = LocalDate.now().year

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val budgetsResult = budgetRepository.getBudgets(userId)
            val transactionsResult = transactionRepository.getTransactions(userId)

            if (budgetsResult.isSuccess && transactionsResult.isSuccess) {
                val budgets = budgetsResult.getOrDefault(emptyList())
                val transactions = transactionsResult.getOrDefault(emptyList())

                val monthlyExpenses = transactions.filter { it.type == "expense" }

                val budgetsWithSpending = budgets
                    .filter { it.month == currentMonth && it.year == currentYear }
                    .map { budget ->
                        val spent = monthlyExpenses
                            .filter { it.categoryId == budget.categoryId }
                            .sumOf { it.amount }
                        val percentage = if (budget.budgetLimit > 0) (spent / budget.budgetLimit * 100) else 0.0
                        BudgetWithSpending(
                            budget = budget,
                            spent = spent,
                            percentage = percentage,
                            isAlert = percentage >= 90.0
                        )
                    }

                _uiState.update {
                    it.copy(isLoading = false, budgetsWithSpending = budgetsWithSpending)
                }
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to load budgets") }
            }
        }
    }

    fun addBudget(categoryId: Int, limit: Double) {
        val userId = authRepository.currentUserId() ?: return
        val now = LocalDate.now()
        viewModelScope.launch {
            val budget = Budget(
                userId = userId,
                categoryId = categoryId,
                budgetLimit = limit,
                month = now.monthValue,
                year = now.year
            )
            budgetRepository.addBudget(budget)
                .onSuccess { loadBudgets() }
                .onFailure { e -> _uiState.update { it.copy(errorMessage = e.message) } }
        }
    }
}