package com.local.fatateer.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Item::class, SaleLog::class, OrderRequest::class], version = 9, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
    abstract fun saleLogDao(): SaleLogDao
    abstract fun orderRequestDao(): OrderRequestDao
    abstract fun orderRequestDao(): OrderRequestDao

    companion object {
        /** الاسم الفعلي لملف قاعدة البيانات على القرص - يُستخدم أيضًا في Export/Restore */
        const val DB_FILE_NAME = "fatateer.db"

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

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `order_requests` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `itemName` TEXT NOT NULL,
                    `itemImagePath` TEXT,
                    `deviceName` TEXT NOT NULL,
                    `customerName` TEXT NOT NULL,
                    `customerPhone` TEXT NOT NULL,
                    `timestamp` INTEGER NOT NULL
                )")
            }
        }

        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_FILE_NAME
                )
                    .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                    .build()
                    .also { INSTANCE = it }
            }
        }

        /**
         * يغلق الاتصال الحالي بقاعدة البيانات ويمسح الـsingleton، بحيث يمكن استبدال
         * ملف القاعدة على القرص بأمان (يُستخدم أثناء Restore) ثم إعادة فتحه عبر get().
         */
        @Synchronized
        fun closeAndClearInstance() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}
