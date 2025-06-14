package com.example.fmapp.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

enum class AccountType {
    BANK_ACCOUNT,
    MOBILE_WALLET,
    ONLINE_MONEY // For virtual/internal tracking
}

@Entity(
    tableName = "financial_accounts",
    foreignKeys = [
        ForeignKey(
            entity = SimCard::class,
            parentColumns = ["id"],
            childColumns = ["linkedSimId"],
            onDelete = ForeignKey.SET_NULL // Or RESTRICT, depending on desired behavior
        )
    ],
    indices = [Index(value = ["accountIdentifier", "userId"], unique = true), Index(value = ["linkedSimId"])]
)
data class FinancialAccount(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val accountName: String, // e.g., "Personal Savings", "CBE Mobile Wallet"
    val accountIdentifier: String, // e.g., Account number, mobile number for wallet
    val accountType: AccountType,
    val linkedSimId: Int?, // Nullable if not directly linked to one SIM (e.g. some bank accounts)
    val initialBalance: Double,
    val dateAdded: Date,
    val userId: String // To associate account with a user
    // Current balance will be computed dynamically, not stored directly here
)
