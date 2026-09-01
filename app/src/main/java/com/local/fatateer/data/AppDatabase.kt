package com.local.fatateer.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Item::class, SaleLog::class], version = 8, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
    abstract fun saleLogDao(): SaleLogDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        /** يضيف عمود صورة المنتج دون فقدان البيانات الحالية */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE items ADD COLUMN imagePath TEXT")
            }
        }

        /** يضيف حقول الأسعار دون فقدان البيانات */
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE items ADD COLUMN priceMin TEXT DEFAULT ''")
                db.execSQL("ALTER TABLE items ADD COLUMN priceMax TEXT DEFAULT ''")
            }
        }

        /** يضيف جدول سجل المبيعات */
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `sale_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `itemName` TEXT NOT NULL, `category` TEXT NOT NULL, `price` TEXT NOT NULL, `quantity` INTEGER NOT NULL, `customerName` TEXT NOT NULL DEFAULT '', `customerPhone` TEXT NOT NULL DEFAULT '', `timestamp` INTEGER NOT NULL)")
            }
        }

        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fatateer.db"
                )
                    .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
