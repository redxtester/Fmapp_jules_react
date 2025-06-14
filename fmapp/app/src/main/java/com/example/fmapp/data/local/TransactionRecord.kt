package com.example.fmapp.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

enum class TransactionType {
    INCOME_CREDIT,
    EXPENSE_DEBIT
}

@Entity(
    tableName = "transaction_records",
    foreignKeys = [
        ForeignKey(
            entity = FinancialAccount::class,
            parentColumns = ["id"],
            childColumns = ["affectedAccountId"],
            onDelete = ForeignKey.CASCADE // If an account is deleted, its transactions are also deleted
        ),
        ForeignKey(
            entity = FinancialAccount::class,
            parentColumns = ["id"],
            childColumns = ["counterpartyAccountId"], // For internal transfers
            onDelete = ForeignKey.SET_NULL // If counterparty account is deleted, link is just removed
        )
    ],
    indices = [
        Index(value = ["affectedAccountId"]),
        Index(value = ["counterpartyAccountId"]),
        Index(value = ["userId", "transactionDate"]) // For querying by user and date
    ]
)
data class TransactionRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val affectedAccountId: Int, // The primary account this transaction belongs to
    val transactionDate: Date,
    val amount: Double,
    val transactionType: TransactionType, // Income or Expense for the affectedAccount
    val currency: String = "ETB", // Default currency
    val descriptionNotes: String?,
    val payerSenderRaw: String?, // Raw text of payer/sender (e.g., from OCR or manual entry)
    val payeeReceiverRaw: String?, // Raw text of payee/receiver
    val referenceNumber: String?, // Transaction ID from bank/service

    val isInternalTransfer: Boolean = false,
    val counterpartyAccountId: Int?, // If internal transfer, the other account owned by the user

    val receiptFileLink: String?, // Link to stored receipt file (e.g., Supabase Storage path)
    val ocrExtractedRawText: String?, // Full raw text from OCR if used

    val userId: String // To associate transaction with a user
)
