package com.example.budgetbuddy.data.repository

import com.example.budgetbuddy.data.model.Category
import com.example.budgetbuddy.data.remote.supabase
import io.github.jan.supabase.postgrest.postgrest

class CategoryRepository {

    suspend fun getCategories(): Result<List<Category>> = try {
        val result = supabase.postgrest["categories"]
            .select()
            .decodeList<Category>()
        Result.success(result)
    } catch (e: Exception) {
        Result.failure(e)
    }
}