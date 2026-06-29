package com.example.budgetbuddy.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetbuddy.data.model.Profile
import com.example.budgetbuddy.data.repository.AuthRepository
import com.example.budgetbuddy.data.repository.ProfileRepository
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val fullName: String = "",
    val monthlyBudget: String = "",
    val email: String = "",
    val saveMessage: String? = null,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null
)

class SettingsViewModel : ViewModel() {

    private val authRepository = AuthRepository()
    private val profileRepository = ProfileRepository()

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init { loadProfile() }

    private fun loadProfile() {
        val userId = authRepository.currentUserId() ?: return
        val email = com.example.budgetbuddy.data.remote.supabase.auth.currentUserOrNull()?.email ?: ""

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            profileRepository.getProfile(userId)
                .onSuccess { profile ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            email = email,
                            fullName = profile?.fullName ?: "",
                            monthlyBudget = profile?.monthlyBudget?.toString() ?: ""
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message, email = email) }
                }
        }
    }

    fun updateProfile(fullName: String, monthlyBudget: Double) {
        val userId = authRepository.currentUserId() ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveMessage = null) }

            val profile = Profile(
                id = userId,
                fullName = fullName,
                email = _uiState.value.email,
                monthlyBudget = monthlyBudget
            )

            profileRepository.upsertProfile(profile)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            saveSuccess = true,
                            saveMessage = "Profile updated successfully.",
                            fullName = fullName,
                            monthlyBudget = monthlyBudget.toString()
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            saveSuccess = false,
                            saveMessage = e.message ?: "Failed to update profile."
                        )
                    }
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }
}