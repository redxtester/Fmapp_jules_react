package com.example.fmapp.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fmapp.data.local.* // Import all local data classes
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date

// Data class to hold account with its calculated current balance
data class AccountWithBalance(
    val account: FinancialAccount,
    val currentBalance: Double
)

class TransactionViewModel(application: Application) : AndroidViewModel(application) {

    private val transactionDao: TransactionDao = AppDatabase.getDatabase(application).transactionDao()
    private val financialAccountDao: FinancialAccountDao = AppDatabase.getDatabase(application).financialAccountDao()

    private val _currentUserId = MutableStateFlow<String?>(null)

    val allFinancialAccounts: StateFlow<List<FinancialAccount>> = _currentUserId.flatMapLatest { userId ->
        if (userId != null) {
            financialAccountDao.getAllAccounts(userId)
        } else {
            emptyFlow()
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Combines accounts with their dynamically calculated balances
    val accountsWithBalances: StateFlow<List<AccountWithBalance>> = allFinancialAccounts.flatMapLatest { accounts ->
        val userId = _currentUserId.value
        if (userId == null || accounts.isEmpty()) {
            flowOf(emptyList())
        } else {
            val flows: List<Flow<AccountWithBalance>> = accounts.map { account ->
                transactionDao.getTotalTransactionEffectOnBalance(account.id, userId)
                    .map { transactionEffect ->
                        val currentBalance = account.initialBalance + (transactionEffect ?: 0.0)
                        AccountWithBalance(account, currentBalance)
                    }
            }
            combine(flows) { it.toList() } // Combine individual account balance flows
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())


    // Transactions for a selected account, or all transactions if no account is selected
    private val _selectedAccountId = MutableStateFlow<Int?>(null)
    val transactions: StateFlow<List<TransactionRecord>> = combine(_currentUserId, _selectedAccountId) { userId, accountId ->
        Pair(userId, accountId)
    }.flatMapLatest { (userId, accountId) ->
        if (userId != null) {
            if (accountId != null) {
                transactionDao.getTransactionsForAccount(accountId, userId)
            } else {
                transactionDao.getAllTransactions(userId)
            }
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

    fun selectAccount(accountId: Int?) {
        _selectedAccountId.value = accountId
    }

    fun addTransaction(
        affectedAccountId: Int,
        amountStr: String,
        transactionType: TransactionType,
        description: String?,
        payerSender: String?,
        payeeReceiver: String?,
        referenceNumber: String?,
        isInternalTransfer: Boolean = false,
        counterpartyAccountId: Int? = null,
        transactionDate: Date = Date() // Allow overriding for back-dated entries
    ) {
        val userId = _currentUserId.value
        if (userId == null) {
            _error.value = "User not logged in."
            return
        }
        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _error.value = "Invalid amount."
            return
        }
        if (isInternalTransfer && counterpartyAccountId == null) {
            _error.value = "Counterparty account must be selected for internal transfers."
            return
        }
        if (isInternalTransfer && counterpartyAccountId == affectedAccountId) {
            _error.value = "Affected account and counterparty account cannot be the same for an internal transfer."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val transaction = TransactionRecord(
                    affectedAccountId = affectedAccountId,
                    transactionDate = transactionDate,
                    amount = amount,
                    transactionType = transactionType,
                    descriptionNotes = description,
                    payerSenderRaw = payerSender,
                    payeeReceiverRaw = payeeReceiver,
                    referenceNumber = referenceNumber,
                    isInternalTransfer = isInternalTransfer,
                    counterpartyAccountId = if(isInternalTransfer) counterpartyAccountId else null,
                    userId = userId
                    // receiptFileLink and ocrExtractedRawText will be handled later
                )
                transactionDao.insert(transaction)

                // If it's an internal transfer, create the corresponding transaction for the other account
                if (isInternalTransfer && counterpartyAccountId != null) {
                    val counterPartyTransactionType = if (transactionType == TransactionType.EXPENSE_DEBIT) {
                        TransactionType.INCOME_CREDIT
                    } else {
                        TransactionType.EXPENSE_DEBIT
                    }
                    val counterTransaction = transaction.copy(
                        id = 0, // New transaction
                        affectedAccountId = counterpartyAccountId,
                        transactionType = counterPartyTransactionType,
                        counterpartyAccountId = affectedAccountId, // Link back to original
                        descriptionNotes = description ?: "Internal Transfer"
                    )
                    transactionDao.insert(counterTransaction)
                }
                _toastMessage.emit("Transaction added successfully!")

            } catch (e: Exception) {
                _error.value = "Failed to add transaction: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteTransaction(transaction: TransactionRecord) {
         val userId = _currentUserId.value
         if (userId == null || transaction.userId != userId) {
            _error.value = "Cannot delete transaction: User mismatch or not logged in."
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null // Clear previous error before new operation
            try {
                transactionDao.delete(transaction)
                // If it was an internal transfer, attempt to find and delete the counterpart.
                if (transaction.isInternalTransfer && transaction.counterpartyAccountId != null) {
                    // This relies on finding a matching transaction. A dedicated shared transfer_id would be more robust.
                    // Fetch all transactions for the user to search for the counterpart.
                    val allUserTransactions = transactionDao.getAllTransactions(userId).firstOrNull() ?: emptyList()
                    val counterTransaction = allUserTransactions.find {
                        it.isInternalTransfer &&
                        it.counterpartyAccountId == transaction.affectedAccountId &&
                        it.affectedAccountId == transaction.counterpartyAccountId &&
                        it.amount == transaction.amount &&
                        (it.transactionDate.time - transaction.transactionDate.time).let { diff -> diff >= -5000 && diff < 5000 } && // Check if dates are very close
                        it.id != transaction.id
                    }
                    counterTransaction?.let { transactionDao.delete(it) }
                }
                _toastMessage.emit("Transaction deleted.")
            } catch (e: Exception) {
                 _error.value = "Failed to delete transaction: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
