package com.example.budgetbuddy.data.repository

import com.example.budgetbuddy.data.model.Transaction
import com.example.budgetbuddy.data.remote.supabase
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order

class TransactionRepository {

    suspend fun getTransactions(userId: String): Result<List<Transaction>> = try {
        val result = supabase.postgrest["transactions"]
            .select {
                filter { eq("user_id", userId) }
                order("transaction_date", Order.DESCENDING)
            }
            .decodeList<Transaction>()
        Result.success(result)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun addTransaction(transaction: Transaction): Result<Unit> = try {
        supabase.postgrest["transactions"].insert(transaction)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun updateTransaction(transaction: Transaction): Result<Unit> = try {
        supabase.postgrest["transactions"].update(
            {
                set("amount", transaction.amount)
                set("description", transaction.description)
                set("type", transaction.type)
                set("category_id", transaction.categoryId)
                set("transaction_date", transaction.transactionDate)
            }
        ) {
            filter { eq("id", transaction.id) }
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun deleteTransaction(id: Int): Result<Unit> = try {
        supabase.postgrest["transactions"]
            .delete { filter { eq("id", id) } }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}