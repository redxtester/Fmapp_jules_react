package com.example.fmapp.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fmapp.data.local.AppDatabase
import com.example.fmapp.data.local.SimCard
import com.example.fmapp.data.local.SimCardDao
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SimCardViewModel(application: Application) : AndroidViewModel(application) {

    private val simCardDao: SimCardDao = AppDatabase.getDatabase(application).simCardDao()
    // Assuming AuthViewModel provides the current user's ID.
    // This needs to be passed or observed from AuthViewModel.
    // For now, let's hardcode a placeholder or assume it's passed.
    // A better approach would be to have a UserRepository or SessionManager.
    private val _currentUserId = MutableStateFlow<String?>(null) // Placeholder

    // This would be populated from AuthViewModel's currentUser state
    fun setCurrentUserId(userId: String?) {
        _currentUserId.value = userId
    }

    val simCards: StateFlow<List<SimCard>> = _currentUserId.flatMapLatest { userId ->
        if (userId != null) {
            simCardDao.getAllSimCards(userId)
        } else {
            emptyFlow()
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _isLoading = MutableStateFlow(false) // For add/delete operations
    val isLoading: StateFlow<Boolean> = _isLoading

    fun addSimCard(phoneNumber: String, nickname: String, provider: String, registeredName: String?) {
        val userId = _currentUserId.value
        if (userId == null) {
            _error.value = "User not logged in."
            return
        }
        if (phoneNumber.isBlank() || nickname.isBlank() || provider.isBlank()) {
            _error.value = "Phone number, nickname, and provider cannot be empty."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // Check if SIM with this phone number already exists for this user
                val existingSim = simCardDao.getSimCardByPhoneNumber(phoneNumber, userId).firstOrNull()
                if (existingSim != null) {
                    _error.value = "SIM card with this phone number already exists."
                    _isLoading.value = false
                    return@launch
                }

                val newSimCard = SimCard(
                    phoneNumber = phoneNumber,
                    simNickname = nickname,
                    telecomProvider = provider,
                    officialRegisteredName = registeredName,
                    userId = userId
                )
                simCardDao.insert(newSimCard)
            } catch (e: Exception) {
                _error.value = "Failed to add SIM card: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteSimCard(simCard: SimCard) {
         val userId = _currentUserId.value
         if (userId == null || simCard.userId != userId) {
            _error.value = "Cannot delete SIM card: User mismatch or not logged in."
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                simCardDao.delete(simCard)
            } catch (e: Exception) {
                _error.value = "Failed to delete SIM card: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
