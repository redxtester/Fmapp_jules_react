package com.example.fmapp.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FinancialAccountDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(financialAccount: FinancialAccount): Long

    @Update
    suspend fun update(financialAccount: FinancialAccount)

    @Delete
    suspend fun delete(financialAccount: FinancialAccount)

    @Query("SELECT * FROM financial_accounts WHERE id = :id AND userId = :userId")
    fun getAccountById(id: Int, userId: String): Flow<FinancialAccount?>

    @Query("SELECT * FROM financial_accounts WHERE userId = :userId ORDER BY accountName ASC")
    fun getAllAccounts(userId: String): Flow<List<FinancialAccount>>

    @Query("SELECT * FROM financial_accounts WHERE linkedSimId = :simId AND userId = :userId")
    fun getAccountsBySimId(simId: Int, userId: String): Flow<List<FinancialAccount>>

    @Query("SELECT * FROM financial_accounts WHERE accountIdentifier = :identifier AND userId = :userId")
    fun getAccountByIdentifier(identifier: String, userId: String): Flow<FinancialAccount?>
}
