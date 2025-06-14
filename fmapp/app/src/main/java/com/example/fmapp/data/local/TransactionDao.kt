package com.example.fmapp.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transactionRecord: TransactionRecord): Long

    @Update
    suspend fun update(transactionRecord: TransactionRecord)

    @Delete
    suspend fun delete(transactionRecord: TransactionRecord)

    @Query("SELECT * FROM transaction_records WHERE id = :id AND userId = :userId")
    fun getTransactionById(id: Int, userId: String): Flow<TransactionRecord?>

    @Query("SELECT * FROM transaction_records WHERE userId = :userId ORDER BY transactionDate DESC")
    fun getAllTransactions(userId: String): Flow<List<TransactionRecord>>

    @Query("SELECT * FROM transaction_records WHERE affectedAccountId = :accountId AND userId = :userId ORDER BY transactionDate DESC")
    fun getTransactionsForAccount(accountId: Int, userId: String): Flow<List<TransactionRecord>>

    // Query to calculate current balance for an account
    // This sums incomes and subtracts expenses from the initial balance.
    // Note: This is a more complex query and might be better handled in ViewModel logic
    // by fetching initial balance and then transactions separately for more flexibility,
    // especially if needing date-specific balances.
    // For now, a simplified version:
    @Query("""
        SELECT SUM(CASE
                     WHEN tr.transactionType = 'INCOME_CREDIT' THEN tr.amount
                     WHEN tr.transactionType = 'EXPENSE_DEBIT' THEN -tr.amount
                     ELSE 0
                   END)
        FROM transaction_records tr
        WHERE tr.affectedAccountId = :accountId AND tr.userId = :userId
    """)
    fun getTotalTransactionEffectOnBalance(accountId: Int, userId: String): Flow<Double?>
}
