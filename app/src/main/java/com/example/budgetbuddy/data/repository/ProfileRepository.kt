package com.example.budgetbuddy.data.repository

import com.example.budgetbuddy.data.model.Profile
import com.example.budgetbuddy.data.remote.supabase
import io.github.jan.supabase.postgrest.postgrest

class ProfileRepository {

    suspend fun getProfile(userId: String): Result<Profile?> = try {
        val result = supabase.postgrest["profiles"]
            .select { filter { eq("id", userId) } }
            .decodeSingleOrNull<Profile>()
        Result.success(result)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun upsertProfile(profile: Profile): Result<Unit> = try {
        supabase.postgrest["profiles"].upsert(profile)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
