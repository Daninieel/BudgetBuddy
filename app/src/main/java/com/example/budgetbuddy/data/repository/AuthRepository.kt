package com.example.budgetbuddy.data.repository

import com.example.budgetbuddy.data.remote.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.Flow

class AuthRepository {

    /** Emits the current auth state: NotAuthenticated, Authenticated, etc. */
    val sessionStatus: Flow<SessionStatus>
        get() = supabase.auth.sessionStatus

    /** The currently logged-in user's id, or null if no one is logged in. */
    fun currentUserId(): String? = supabase.auth.currentUserOrNull()?.id

    suspend fun signUp(email: String, password: String): Result<Unit> = try {
        supabase.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
        // Note: insert the corresponding row into the "profiles" table (full_name,
        // monthly_budget) right after this succeeds — either here via a
        // ProfileRepository call, or with a Postgres trigger on auth.users.
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun signIn(email: String, password: String): Result<Unit> = try {
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun signOut(): Result<Unit> = try {
        supabase.auth.signOut()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> = try {
        supabase.auth.resetPasswordForEmail(email = email)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}