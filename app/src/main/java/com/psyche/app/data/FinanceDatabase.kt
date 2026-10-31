package com.psyche.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        LedgerEntryEntity::class,
        BudgetEntity::class,
        AllocationEntity::class,
        GoalEntity::class,
        QuickTransactionEntity::class,
        SettingEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class FinanceDatabase : RoomDatabase() {
    abstract fun financeDao(): FinanceDao

    companion object {
        private val feedDefaultAccount = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                db.execSQL(
                    """
                    INSERT INTO accounts
                        (name, type, openingBalanceCents, creditLimitCents, lastFour, sortOrder, archived, isDefault)
                    VALUES
                        ('Cash', 'cash', 0, 0, NULL, 0, 0, 1)
                    """.trimIndent(),
                )
            }
        }

        private val migration1To2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE accounts ADD COLUMN isDefault INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    """
                    UPDATE accounts
                    SET isDefault = 1
                    WHERE id = (
                        SELECT id FROM accounts
                        WHERE type = 'cash' AND archived = 0
                        ORDER BY sortOrder ASC, id ASC
                        LIMIT 1
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO accounts
                        (name, type, openingBalanceCents, creditLimitCents, lastFour, sortOrder, archived, isDefault)
                    SELECT
                        'Cash', 'cash', 0, 0, NULL, 0, 0, 1
                    WHERE NOT EXISTS (
                        SELECT 1 FROM accounts WHERE isDefault = 1
                    )
                    """.trimIndent(),
                )
            }
        }

        private val migration2To3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `quick_transactions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `label` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `amountCents` INTEGER NOT NULL,
                        `accountId` INTEGER NOT NULL,
                        `categoryId` INTEGER,
                        `note` TEXT NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        `archived` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    DELETE FROM app_settings
                    WHERE key IN (
                        'notifications_enabled',
                        'show_amount_on_lock_screen',
                        'notification_prompt_seen'
                    )
                    """.trimIndent(),
                )
            }
        }

        fun build(context: Context): FinanceDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                FinanceDatabase::class.java,
                "psyche-finance.db",
            )
                .addMigrations(migration1To2, migration2To3)
                .addCallback(feedDefaultAccount)
                .build()
    }
}
