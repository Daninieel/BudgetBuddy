package com.example.budgetbuddy.data.repository

import com.example.budgetbuddy.data.model.Budget
import com.example.budgetbuddy.data.remote.supabase
import io.github.jan.supabase.postgrest.postgrest

class BudgetRepository {

    suspend fun getBudgets(userId: String): Result<List<Budget>> = try {
        val result = supabase.postgrest["budgets"]
            .select { filter { eq("user_id", userId) } }
            .decodeList<Budget>()
        Result.success(result)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun addBudget(budget: Budget): Result<Unit> = try {
        supabase.postgrest["budgets"].insert(budget)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun deleteBudget(id: Int): Result<Unit> = try {
        supabase.postgrest["budgets"]
            .delete { filter { eq("id", id) } }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}