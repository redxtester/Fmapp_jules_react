package com.example.fmapp.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fmapp.data.local.AccountType
import com.example.fmapp.data.local.AppDatabase
import com.example.fmapp.data.local.FinancialAccount
import com.example.fmapp.data.local.FinancialAccountDao
import com.example.fmapp.data.local.SimCard
import com.example.fmapp.data.local.SimCardDao
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date

class FinancialAccountViewModel(application: Application) : AndroidViewModel(application) {

    private val financialAccountDao: FinancialAccountDao = AppDatabase.getDatabase(application).financialAccountDao()
    private val simCardDao: SimCardDao = AppDatabase.getDatabase(application).simCardDao() // For fetching SIMs to link

    private val _currentUserId = MutableStateFlow<String?>(null)

    val allSimCards: StateFlow<List<SimCard>> = _currentUserId.flatMapLatest { userId ->
        if (userId != null) {
            simCardDao.getAllSimCards(userId)
        } else {
            emptyFlow()
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val financialAccounts: StateFlow<List<FinancialAccount>> = _currentUserId.flatMapLatest { userId ->
        if (userId != null) {
            financialAccountDao.getAllAccounts(userId)
        } else {
            emptyFlow()
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()


    fun setCurrentUserId(userId: String?) {
        _currentUserId.value = userId
    }

    fun addFinancialAccount(
        accountName: String,
        accountIdentifier: String,
        accountType: AccountType,
        initialBalanceStr: String,
        linkedSimId: Int?
    ) {
        val userId = _currentUserId.value
        if (userId == null) {
            _error.value = "User not logged in."
            return
        }
        if (accountName.isBlank() || accountIdentifier.isBlank() || initialBalanceStr.isBlank()) {
            _error.value = "Account name, identifier, and initial balance cannot be empty."
            return
        }
        val initialBalance = initialBalanceStr.toDoubleOrNull()
        if (initialBalance == null) {
            _error.value = "Invalid initial balance format."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val existingAccount = financialAccountDao.getAccountByIdentifier(accountIdentifier, userId).firstOrNull()
                if (existingAccount != null) {
                    _error.value = "Account with this identifier already exists."
                    _isLoading.value = false
                    return@launch
                }

                val newAccount = FinancialAccount(
                    accountName = accountName,
                    accountIdentifier = accountIdentifier,
                    accountType = accountType,
                    linkedSimId = linkedSimId,
                    initialBalance = initialBalance,
                    dateAdded = Date(), // Current date
                    userId = userId
                )
                financialAccountDao.insert(newAccount)
                _toastMessage.emit("Account added successfully!")
            } catch (e: Exception) {
                _error.value = "Failed to add account: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteFinancialAccount(account: FinancialAccount) {
        val userId = _currentUserId.value
        if (userId == null || account.userId != userId) {
            _error.value = "Cannot delete account: User mismatch or not logged in."
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                financialAccountDao.delete(account)
                 _toastMessage.emit("Account deleted successfully!")
            } catch (e: Exception) {
                _error.value = "Failed to delete account: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
