package com.example.fmapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.Date

// Converters class remains mostly the same, just add TransactionType
class Converters {
    @androidx.room.TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    @androidx.room.TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time

    @androidx.room.TypeConverter
    fun fromAccountTypeString(value: String?): AccountType? = value?.let { AccountType.valueOf(it) }

    @androidx.room.TypeConverter
    fun accountTypeToString(accountType: AccountType?): String? = accountType?.name

    @androidx.room.TypeConverter
    fun fromTransactionTypeString(value: String?): TransactionType? = value?.let { TransactionType.valueOf(it) }

    @androidx.room.TypeConverter
    fun transactionTypeToString(transactionType: TransactionType?): String? = transactionType?.name
}

@Database(
    entities = [SimCard::class, FinancialAccount::class, TransactionRecord::class], // Added TransactionRecord
    version = 3, // Incremented version
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun simCardDao(): SimCardDao
    abstract fun financialAccountDao(): FinancialAccountDao
    abstract fun transactionDao(): TransactionDao // Added DAO accessor

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // MIGRATION_1_2 remains unchanged from the prompt as it was already defined
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `financial_accounts` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `accountName` TEXT NOT NULL,
                        `accountIdentifier` TEXT NOT NULL,
                        `accountType` TEXT NOT NULL,
                        `linkedSimId` INTEGER,
                        `initialBalance` REAL NOT NULL,
                        `dateAdded` INTEGER NOT NULL,
                        `userId` TEXT NOT NULL,
                        FOREIGN KEY(`linkedSimId`) REFERENCES `sim_cards`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                """)
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_financial_accounts_accountIdentifier_userId` ON `financial_accounts` (`accountIdentifier`, `userId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_financial_accounts_linkedSimId` ON `financial_accounts` (`linkedSimId`)")
            }
        }

        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `transaction_records` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `affectedAccountId` INTEGER NOT NULL,
                        `transactionDate` INTEGER NOT NULL,
                        `amount` REAL NOT NULL,
                        `transactionType` TEXT NOT NULL,
                        `currency` TEXT NOT NULL DEFAULT 'ETB',
                        `descriptionNotes` TEXT,
                        `payerSenderRaw` TEXT,
                        `payeeReceiverRaw` TEXT,
                        `referenceNumber` TEXT,
                        `isInternalTransfer` INTEGER NOT NULL DEFAULT 0,
                        `counterpartyAccountId` INTEGER,
                        `receiptFileLink` TEXT,
                        `ocrExtractedRawText` TEXT,
                        `userId` TEXT NOT NULL,
                        FOREIGN KEY(`affectedAccountId`) REFERENCES `financial_accounts`(`id`) ON DELETE CASCADE,
                        FOREIGN KEY(`counterpartyAccountId`) REFERENCES `financial_accounts`(`id`) ON DELETE SET NULL
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transaction_records_affectedAccountId` ON `transaction_records` (`affectedAccountId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transaction_records_counterpartyAccountId` ON `transaction_records` (`counterpartyAccountId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transaction_records_userId_transactionDate` ON `transaction_records` (`userId`, `transactionDate`)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fmapp_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3) // Added new migration
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
