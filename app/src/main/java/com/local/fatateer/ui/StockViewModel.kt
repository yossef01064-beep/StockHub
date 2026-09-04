package com.local.fatateer.ui

import android.app.Application
import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.sqlite.db.SupportSQLiteDatabase
import com.local.fatateer.data.AppDatabase
import com.local.fatateer.data.Categories
import com.local.fatateer.data.ImageStorage
import com.local.fatateer.data.Item
import com.local.fatateer.data.SaleLog
import com.local.fatateer.data.OrderRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.security.MessageDigest
import java.util.LinkedHashMap
import java.util.LinkedHashSet
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

enum class MainTab { HOME, SPARE, SALES }

data class TopSellingItem(
    val itemName: String,
    val totalQuantity: Int,
    val rank: Int
)


data class StockUiState(
    val items: List<Item> = emptyList(),
    val logs: List<SaleLog> = emptyList(),
    val orderRequests: List<OrderRequest> = emptyList(),
    val query: String = "",
    val selectedCategory: String? = null,
    val tab: MainTab = MainTab.HOME
) {
    private val scopeCategories: List<String>?
        get() = when (tab) {
            MainTab.HOME -> null
            MainTab.SPARE -> Categories.spareParts
            MainTab.SALES -> Categories.sales
        }

    val filtered: List<Item>
        get() {
            var list = items
            scopeCategories?.let { allowed ->
                list = list.filter { it.category in allowed }
            }
            selectedCategory?.let { cat ->
                list = list.filter { it.category == cat }
            }
            val q = query.trim()
            if (q.isNotEmpty()) {
                list = list.filter {
                    it.name.contains(q, true) ||
                        it.notes.contains(q, true) ||
                        it.category.contains(q, true) ||
                        it.subCategory.contains(q, true) ||
                        it.brand.contains(q, true)
                }
            }
            return list
        }

    val grouped: Map<String, List<Item>>
        get() = filtered.groupBy { item ->
            if (item.subCategory.isNotBlank()) {
                "${item.category} › ${item.subCategory}"
            } else {
                item.category
            }
        }

    val neededItems: List<Item>
        get() = items.filter { it.quantity <= it.minQuantity }

    val neededCount: Int get() = neededItems.size
    val spareCount: Int get() = items.count { it.category in Categories.spareParts }
    val salesCount: Int get() = items.count { it.category in Categories.sales }
    val totalQty: Int get() = items.sumOf { it.quantity }
    val todayStart: Long get() = System.currentTimeMillis() - (System.currentTimeMillis() % (24 * 60 * 60 * 1000))
    
    // Expose the todayStart to StockUiState
    val todayStartForUi: Long get() = todayStart

    // إحصائيات جديدة
    val totalTodayIncome: Double get() = logs.filter { it.timestamp >= todayStart }.sumOf { (it.price.toDoubleOrNull() ?: 0.0) * it.quantity }
    val topSellingItems: List<TopSellingItem> get() = calculateTopSellingItems(logs)
    val lowStockSparePartsCount: Int get() = items.count { it.category in Categories.spareParts && it.quantity <= it.minQuantity }
    val lowStockSalesCount: Int get() = items.count { it.category in Categories.sales && it.quantity <= it.minQuantity }
    val orderRequestsCount: Int get() = orderRequests.size

    private fun calculateTopSellingItems(logs: List<SaleLog>): List<TopSellingItem> {
        val monthStart = System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L
        val monthLogs = logs.filter { it.timestamp >= monthStart && it.category in Categories.sales }
        val itemSalesMap = mutableMapOf<String, Int>()
        monthLogs.forEach { log ->
            itemSalesMap[log.itemName] = itemSalesMap.getOrDefault(log.itemName, 0) + log.quantity
        }
        return itemSalesMap.toList()
            .sortedByDescending { it.second }
            .take(5)
            .mapIndexed { index, (name, qty) -> TopSellingItem(name, qty, index + 1) }
    }
}

class StockViewModel(app: Application) : AndroidViewModel(app) {
    // مرجع قابل للاستبدال لقاعدة البيانات - يُعاد تعيينه بعد Restore حتى تعمل كل
    // القراءات/الـFlows على الملف الجديد دون الحاجة لإعادة تشغيل التطبيق.
    private var db = AppDatabase.get(app)
    private val dao get() = db.itemDao()
    private val logDao get() = db.saleLogDao()
    private val queryFlow = MutableStateFlow("")
    private val categoryFlow = MutableStateFlow<String?>(null)
    private val tabFlow = MutableStateFlow(MainTab.HOME)
    // يزداد كلما تم استبدال قاعدة البيانات (بعد Restore) لإجبار الـFlows على
    // إعادة الاشتراك في dao الجديد بدل الاتصال القديم المغلق.
    private val dbVersionFlow = MutableStateFlow(0)
    private val orderRequestDao get() = db.orderRequestDao()

    private val itemsFlow: Flow<List<Item>> = dbVersionFlow.flatMapLatest { dao.observeAll() }
    private val logsFlow: Flow<List<SaleLog>> = dbVersionFlow.flatMapLatest { logDao.observeAll() }
    private val orderRequestsFlow: Flow<List<OrderRequest>> = dbVersionFlow.flatMapLatest { orderRequestDao.observeAll() }

    val state = combine(
        combine(itemsFlow, logsFlow, orderRequestsFlow) { items: List<Item>, logs: List<SaleLog>, orderRequests: List<OrderRequest> ->
            Triple(items, logs, orderRequests)
        },
        queryFlow,
        categoryFlow,
        tabFlow
    ) { databaseState, query, cat, tab ->
        StockUiState(
            items = databaseState.first,
            logs = databaseState.second,
            orderRequests = databaseState.third,
            query = query,
            selectedCategory = cat,
            tab = tab
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StockUiState())

    val salesLogs: Flow<List<SaleLog>> = logsFlow

    fun setQuery(value: String) {
        queryFlow.value = value
    }

    fun setCategory(value: String?) {
        categoryFlow.value = value
    }

    fun setTab(tab: MainTab) {
        tabFlow.value = tab
        categoryFlow.value = null
        queryFlow.value = ""
    }

    fun save(item: Item) {
        viewModelScope.launch {
            if (item.id == 0L) dao.insert(item) else dao.update(item)
        }
    }

    fun delete(item: Item) {
        viewModelScope.launch {
            dao.delete(item)
            ImageStorage.delete(item.imagePath)
        }
    }

    fun plus(item: Item) {
        viewModelScope.launch { dao.changeQuantity(item.id, 1) }
    }

    fun minus(item: Item) {
        if (item.quantity <= 0) return
        viewModelScope.launch { dao.changeQuantity(item.id, -1) }
    }

    fun deleteSaleLog(log: SaleLog) {
        viewModelScope.launch { logDao.delete(log) }
    }

    fun deleteOrderRequest(request: OrderRequest) {
        viewModelScope.launch { orderRequestDao.delete(request) }
    }

    fun saveOrderRequest(request: OrderRequest) {
        viewModelScope.launch {
            if (request.id == 0L) {
                orderRequestDao.insert(request)
            } else {
                orderRequestDao.update(request)
            }
        }
    }

    fun addOrderRequest(order: OrderRequest) {
        viewModelScope.launch {
            orderRequestDao.insert(order)
        }
    }

    fun recordSale(item: Item, qty: Int, price: Double, customerName: String = "", customerPhone: String = "") {
        viewModelScope.launch {
            if (item.quantity < qty) {
                return@launch 
            }

            logDao.insert(SaleLog(
                itemName = item.name,
                category = item.category,
                price = price.toString(),
                quantity = qty,
                customerName = customerName,
                customerPhone = customerPhone
            ))
            dao.changeQuantity(item.id, -qty)
        }
    }

    fun updateLog(log: SaleLog) {
        viewModelScope.launch {
            logDao.update(log)
        }
    }

    fun clearLogs() {
        viewModelScope.launch { logDao.clearAll() }
    }

    /** أسماء بديلة قديمة كانت تُستخدم بالخطأ لمُدخل قاعدة البيانات داخل النسخ الاحتياطية السابقة */
    private val legacyDbEntryNames = setOf("app_database")

    private companion object {
        private const val BACKUP_FORMAT = "stockhub-backup"
        private const val BACKUP_FORMAT_VERSION = 1
        private const val BACKUP_SCHEMA_VERSION = 9
        private const val MANIFEST_FILE_NAME = "manifest.json"
        private const val MAX_BACKUP_BYTES = 256L * 1024L * 1024L
        private val ITEM_COLUMNS = listOf(
            "id", "name", "category", "subCategory", "brand", "quantity",
            "minQuantity", "notes", "imagePath", "priceMin", "priceMax"
        )
        private val SALE_LOG_COLUMNS = listOf(
            "id", "itemName", "category", "price", "quantity",
            "customerName", "customerPhone", "timestamp"
        )
        private val ORDER_REQUEST_COLUMNS = listOf(
            "id", "itemName", "itemImagePath", "deviceName",
            "customerName", "customerPhone", "timestamp"
        )
    }

    private data class BackupMetadata(
        val databaseFile: String,
        val databaseSha256: String,
        val databaseSize: Long,
        val schemaVersion: Int,
        val imageNames: Set<String>
    )

    private data class BackupSourceSchema(
        val itemColumns: Set<String>,
        val hasSaleLogs: Boolean,
        val hasOrderRequests: Boolean
    )

    private class BackupValidationException(message: String) : Exception(message)

    fun exportBackup(uri: Uri, context: Application, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch(Dispatchers.IO) {
            val snapshotFile = File(context.cacheDir, "export_snapshot_${System.currentTimeMillis()}.db")
            val imageSnapshotDir = File(context.cacheDir, "export_images_${System.currentTimeMillis()}")
            try {
                val imageDir = File(context.filesDir, "item_images")
                val dbFile = context.getDatabasePath(AppDatabase.DB_FILE_NAME)

                if (!dbFile.exists() || dbFile.length() == 0L) {
                    withContext(Dispatchers.Main) {
                        onResult(false, "تعذّر العثور على قاعدة البيانات")
                    }
                    return@launch
                }

                // لا ننسخ ملفًا قديمًا بينما توجد بيانات مؤكدة في WAL.
                db.query("PRAGMA wal_checkpoint(FULL)", null).close()
                dbFile.copyTo(snapshotFile, overwrite = true)

                val imageFiles = imageDir.listFiles()?.filter { it.isFile }.orEmpty()
                imageSnapshotDir.mkdirs()
                val snapshotImages = imageFiles.map { file ->
                    file.copyTo(File(imageSnapshotDir, file.name), overwrite = true)
                }
                val manifestImages = JSONArray()
                snapshotImages.forEach { file ->
                    manifestImages.put(JSONObject().apply {
                        put("name", file.name)
                        put("size", file.length())
                        put("sha256", sha256(file))
                    })
                }

                val manifest = JSONObject().apply {
                    put("format", BACKUP_FORMAT)
                    put("formatVersion", BACKUP_FORMAT_VERSION)
                    put("schemaVersion", BACKUP_SCHEMA_VERSION)
                    put("databaseFile", AppDatabase.DB_FILE_NAME)
                    put("databaseSize", snapshotFile.length())
                    put("databaseSha256", sha256(snapshotFile))
                    put("images", manifestImages)
                }

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    ZipOutputStream(outputStream).use { zipOutputStream ->
                        zipOutputStream.putNextEntry(ZipEntry(MANIFEST_FILE_NAME))
                        zipOutputStream.write(manifest.toString().toByteArray(Charsets.UTF_8))
                        zipOutputStream.closeEntry()

                        zipOutputStream.putNextEntry(ZipEntry(AppDatabase.DB_FILE_NAME))
                        snapshotFile.inputStream().use { input -> input.copyTo(zipOutputStream) }
                        zipOutputStream.closeEntry()

                        snapshotImages.forEach { file ->
                            zipOutputStream.putNextEntry(ZipEntry(file.name))
                            file.inputStream().use { input -> input.copyTo(zipOutputStream) }
                            zipOutputStream.closeEntry()
                        }
                    }
                } ?: run {
                    withContext(Dispatchers.Main) {
                        onResult(false, "تعذّر فتح الملف المحدد للكتابة")
                    }
                    return@launch
                }

                withContext(Dispatchers.Main) { onResult(true, null) }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onResult(false, "حدث خطأ أثناء إنشاء النسخة الاحتياطية")
                }
            } finally {
                snapshotFile.delete()
                imageSnapshotDir.deleteRecursively()
            }
        }
    }

    fun restoreBackup(uri: Uri, context: Application, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch(Dispatchers.IO) {
            val tempDir = File(context.cacheDir, "restore_tmp_${System.currentTimeMillis()}").apply { mkdirs() }
            var imageChangesStarted = false
            var transactionStarted = false
            var transactionDb: SupportSQLiteDatabase? = null
            var imageEntries = emptyMap<String, File>()
            var imageDir: File? = null
            var imageRollbackDir: File? = null
            var sourceDb: SQLiteDatabase? = null

            try {
                val zipFile = File(tempDir, "backup.stockhub")
                val copiedOk = context.contentResolver.openInputStream(uri)?.use { input ->
                    zipFile.outputStream().use { output -> copyWithLimit(input, output, 0L) }
                    true
                } ?: false

                if (!copiedOk || !zipFile.exists() || zipFile.length() == 0L) {
                    throw BackupValidationException("تعذّرت قراءة ملف النسخة الاحتياطية")
                }

                val extractDir = File(tempDir, "extracted").apply { mkdirs() }
                val extractedEntries = extractBackupEntries(zipFile, extractDir)
                val manifestFile = extractedEntries[MANIFEST_FILE_NAME]
                val metadata = manifestFile?.let { readAndValidateManifest(it, extractedEntries) }

                val databaseFileName = metadata?.databaseFile
                    ?: AppDatabase.DB_FILE_NAME.takeIf { extractedEntries.containsKey(it) }
                    ?: legacyDbEntryNames.firstOrNull { extractedEntries.containsKey(it) }
                    ?: throw BackupValidationException("النسخة لا تحتوي على ملف قاعدة بيانات")
                val backupDbFile = extractedEntries[databaseFileName]
                    ?: throw BackupValidationException("ملف قاعدة البيانات غير موجود داخل النسخة")

                imageEntries = extractedEntries
                    .filterKeys { it != MANIFEST_FILE_NAME && it != databaseFileName }
                if (metadata != null && imageEntries.keys != metadata.imageNames) {
                    throw BackupValidationException("محتوى الصور داخل النسخة لا يطابق ملف التحقق")
                }

                sourceDb = try {
                    openAndValidateBackupDatabase(backupDbFile, metadata?.schemaVersion)
                } catch (e: BackupValidationException) {
                    throw e
                } catch (e: Exception) {
                    throw BackupValidationException("ملف قاعدة البيانات داخل النسخة غير صالح أو غير مقروء")
                }

                try {
                    val schema = validateSourceSchema(sourceDb!!, metadata?.schemaVersion)
                    validateReferencedImages(sourceDb!!, schema, imageEntries)

                    imageDir = File(context.filesDir, "item_images")
                    imageRollbackDir = File(tempDir, "current_images").apply { mkdirs() }
                    backupCurrentImages(imageDir!!, imageEntries.keys, imageRollbackDir!!)

                    // لا يتم حذف الملف الحالي أو إغلاق Room؛ كل تغييرات البيانات تتم ذريًا.
                    transactionDb = db.openHelper.writableDatabase
                    transactionDb!!.beginTransaction()
                    transactionStarted = true

                    imageDir!!.mkdirs()
                    imageChangesStarted = imageEntries.isNotEmpty()
                    imageEntries.forEach { (name, sourceFile) ->
                        sourceFile.copyTo(File(imageDir, name), overwrite = true)
                    }

                    transactionDb!!.delete("order_requests", null, null)
                    transactionDb!!.delete("sale_logs", null, null)
                    transactionDb!!.delete("items", null, null)

                    importItems(sourceDb!!, transactionDb!!, schema, imageDir!!)
                    if (schema.hasSaleLogs) {
                        importSaleLogs(sourceDb!!, transactionDb!!)
                    }
                    if (schema.hasOrderRequests) {
                        importOrderRequests(sourceDb!!, transactionDb!!, imageDir!!)
                    }

                    transactionDb!!.setTransactionSuccessful()
                } catch (e: Exception) {
                    if (imageChangesStarted) {
                        restoreChangedImages(imageDir!!, imageEntries.keys, imageRollbackDir!!)
                    }
                    throw e
                } finally {
                    if (transactionStarted) {
                        transactionDb!!.endTransaction()
                    }
                }
                sourceDb!!.close()
                sourceDb = null

                withContext(Dispatchers.Main) {
                    // إجبار الـFlows على إعادة القراءة بعد اكتمال الـtransaction فقط.
                    dbVersionFlow.value += 1
                    onResult(true, null)
                }
            } catch (e: BackupValidationException) {
                withContext(Dispatchers.Main) {
                    onResult(false, e.message ?: "النسخة الاحتياطية غير صالحة ولم يتم تغيير البيانات")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onResult(false, "فشل الاسترداد ولم يتم تغيير البيانات الحالية")
                }
            } finally {
                sourceDb?.close()
                tempDir.deleteRecursively()
            }
        }
    }

    private fun extractBackupEntries(zipFile: File, extractDir: File): LinkedHashMap<String, File> {
        val entries = LinkedHashMap<String, File>()
        var totalBytes = 0L

        ZipInputStream(zipFile.inputStream()).use { input ->
            var entry = input.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val name = entry.name
                    if (name.isBlank() || name != File(name).name || name.contains('\\')) {
                        throw BackupValidationException("النسخة تحتوي على مسار ملف غير آمن")
                    }
                    if (!entries.containsKey(name)) {
                        val outputFile = File(extractDir, name)
                        outputFile.outputStream().use { output ->
                            totalBytes = copyWithLimit(input, output, totalBytes)
                        }
                        entries[name] = outputFile
                    } else {
                        throw BackupValidationException("النسخة تحتوي على ملفات مكررة")
                    }
                }
                input.closeEntry()
                entry = input.nextEntry
            }
        }

        if (!entries.containsKey(MANIFEST_FILE_NAME) && entries.isEmpty()) {
            throw BackupValidationException("ملف النسخة الاحتياطية فارغ")
        }
        return entries
    }

    private fun readAndValidateManifest(
        manifestFile: File,
        entries: Map<String, File>
    ): BackupMetadata {
        val json = try {
            JSONObject(manifestFile.readText(Charsets.UTF_8))
        } catch (_: Exception) {
            throw BackupValidationException("ملف التحقق داخل النسخة تالف")
        }

        if (json.optString("format") != BACKUP_FORMAT ||
            json.optInt("formatVersion", -1) != BACKUP_FORMAT_VERSION
        ) {
            throw BackupValidationException("صيغة النسخة الاحتياطية غير متوافقة")
        }

        val databaseFile = json.optString("databaseFile")
        val databaseSha256 = json.optString("databaseSha256")
        val databaseSize = json.optLong("databaseSize", -1L)
        val schemaVersion = json.optInt("schemaVersion", -1)
        if (!isSafeEntryName(databaseFile) ||
            databaseFile != AppDatabase.DB_FILE_NAME ||
            databaseSha256.isBlank() ||
            databaseSize < 1L ||
            schemaVersion != BACKUP_SCHEMA_VERSION
        ) {
            throw BackupValidationException("معلومات قاعدة البيانات داخل النسخة غير صالحة")
        }

        val databaseFileOnDisk = entries[databaseFile]
            ?: throw BackupValidationException("ملف قاعدة البيانات غير موجود داخل النسخة")
        if (databaseFileOnDisk.length() != databaseSize ||
            sha256(databaseFileOnDisk) != databaseSha256
        ) {
            throw BackupValidationException("قاعدة البيانات داخل النسخة لا تطابق ملف التحقق")
        }

        val images = json.optJSONArray("images")
            ?: throw BackupValidationException("قائمة صور النسخة غير موجودة")
        val imageNames = LinkedHashSet<String>()
        for (index in 0 until images.length()) {
            val image = images.optJSONObject(index)
                ?: throw BackupValidationException("بيانات صورة داخل النسخة غير صالحة")
            val name = image.optString("name")
            val size = image.optLong("size", -1L)
            val sha256 = image.optString("sha256")
            if (!isSafeEntryName(name) || name == databaseFile || name == MANIFEST_FILE_NAME ||
                !imageNames.add(name) || size < 0L || sha256.isBlank()
            ) {
                throw BackupValidationException("قائمة الصور داخل النسخة غير صالحة")
            }

            val imageFile = entries[name]
                ?: throw BackupValidationException("الصورة $name غير موجودة داخل النسخة")
            if (imageFile.length() != size || sha256(imageFile) != sha256) {
                throw BackupValidationException("الصورة $name لا تطابق ملف التحقق")
            }
        }

        return BackupMetadata(
            databaseFile = databaseFile,
            databaseSha256 = databaseSha256,
            databaseSize = databaseSize,
            schemaVersion = schemaVersion,
            imageNames = imageNames
        )
    }

    private fun openAndValidateBackupDatabase(file: File, expectedSchemaVersion: Int?): SQLiteDatabase {
        if (file.length() < 16L || file.inputStream().use { input ->
                val header = ByteArray(16)
                input.read(header) == 16 &&
                    String(header, Charsets.US_ASCII).startsWith("SQLite format 3")
            }.not()
        ) {
            throw BackupValidationException("ملف قاعدة البيانات داخل النسخة غير صالح")
        }

        val source = try {
            SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY)
        } catch (_: Exception) {
            throw BackupValidationException("تعذّر فتح قاعدة البيانات داخل النسخة")
        }

        try {
            source.rawQuery("PRAGMA quick_check", null).use { cursor ->
                if (!cursor.moveToFirst() || cursor.getString(0) != "ok") {
                    throw BackupValidationException("قاعدة البيانات داخل النسخة تالفة")
                }
            }

            val userVersion = source.rawQuery("PRAGMA user_version", null).use { cursor ->
                if (!cursor.moveToFirst()) -1 else cursor.getInt(0)
            }
            if (expectedSchemaVersion != null && userVersion != expectedSchemaVersion) {
                throw BackupValidationException("إصدار قاعدة البيانات داخل النسخة غير متوافق")
            }
            if (expectedSchemaVersion == null && userVersion !in 5..BACKUP_SCHEMA_VERSION) {
                throw BackupValidationException("إصدار قاعدة البيانات القديمة غير مدعوم")
            }
            return source
        } catch (e: BackupValidationException) {
            source.close()
            throw e
        } catch (e: Exception) {
            source.close()
            throw BackupValidationException("قاعدة البيانات داخل النسخة غير صالحة")
        }
    }

    private fun validateSourceSchema(
        source: SQLiteDatabase,
        expectedSchemaVersion: Int?
    ): BackupSourceSchema {
        val itemColumns = tableColumns(source, "items")
        val requiredItemColumns = setOf("id", "name", "category", "quantity")
        if (!itemColumns.containsAll(requiredItemColumns)) {
            throw BackupValidationException("جدول المنتجات داخل النسخة غير متوافق")
        }

        val hasSaleLogs = tableExists(source, "sale_logs")
        val hasOrderRequests = tableExists(source, "order_requests")
        if (expectedSchemaVersion != null && (!hasSaleLogs || !hasOrderRequests)) {
            throw BackupValidationException("النسخة لا تحتوي على كل جداول StockHub المطلوبة")
        }
        if (hasSaleLogs && !tableColumns(source, "sale_logs").containsAll(SALE_LOG_COLUMNS)) {
            throw BackupValidationException("جدول سجل المبيعات داخل النسخة غير متوافق")
        }
        if (hasOrderRequests && !tableColumns(source, "order_requests").containsAll(ORDER_REQUEST_COLUMNS)) {
            throw BackupValidationException("جدول الطلبات داخل النسخة غير متوافق")
        }

        return BackupSourceSchema(
            itemColumns = itemColumns,
            hasSaleLogs = hasSaleLogs,
            hasOrderRequests = hasOrderRequests
        )
    }

    private fun validateReferencedImages(
        source: SQLiteDatabase,
        schema: BackupSourceSchema,
        imageEntries: Map<String, File>
    ) {
        if ("imagePath" in schema.itemColumns) {
            source.query("items", arrayOf("imagePath"), null, null, null, null, null).use { cursor ->
                val column = cursor.getColumnIndexOrThrow("imagePath")
                while (cursor.moveToNext()) {
                    val path = if (cursor.isNull(column)) null else cursor.getString(column)
                    if (!path.isNullOrBlank() && imageEntries[File(path).name] == null) {
                        throw BackupValidationException("صورة مرتبطة بمنتج غير موجودة داخل النسخة")
                    }
                }
            }
        }
        if (schema.hasOrderRequests) {
            source.query("order_requests", arrayOf("itemImagePath"), null, null, null, null, null).use { cursor ->
                val column = cursor.getColumnIndexOrThrow("itemImagePath")
                while (cursor.moveToNext()) {
                    val path = if (cursor.isNull(column)) null else cursor.getString(column)
                    if (!path.isNullOrBlank() && imageEntries[File(path).name] == null) {
                        throw BackupValidationException("صورة مرتبطة بطلب غير موجودة داخل النسخة")
                    }
                }
            }
        }
    }

    private fun backupCurrentImages(imageDir: File, names: Set<String>, rollbackDir: File) {
        if (!imageDir.exists()) return
        names.forEach { name ->
            val currentFile = File(imageDir, name)
            if (currentFile.isFile) {
                currentFile.copyTo(File(rollbackDir, name), overwrite = true)
            }
        }
    }

    private fun restoreChangedImages(imageDir: File, names: Set<String>, rollbackDir: File) {
        imageDir.mkdirs()
        names.forEach { name ->
            val currentFile = File(imageDir, name)
            val previousFile = File(rollbackDir, name)
            if (previousFile.isFile) {
                previousFile.copyTo(currentFile, overwrite = true)
            } else {
                currentFile.delete()
            }
        }
    }

    private fun importItems(
        source: SQLiteDatabase,
        target: SupportSQLiteDatabase,
        schema: BackupSourceSchema,
        imageDir: File
    ) {
        val projection = ITEM_COLUMNS.filter { it in schema.itemColumns }.toTypedArray()
        source.query("items", projection, null, null, null, null, "id ASC").use { cursor ->
            while (cursor.moveToNext()) {
                val values = ContentValues()
                values.put("id", cursor.requiredLong("id"))
                values.put("name", cursor.requiredString("name"))
                values.put("category", cursor.requiredString("category"))
                values.put("subCategory", cursor.optionalString("subCategory") ?: "")
                values.put("brand", cursor.optionalString("brand") ?: "")
                values.put("quantity", cursor.requiredInt("quantity"))
                values.put("minQuantity", cursor.optionalInt("minQuantity") ?: 1)
                values.put("notes", cursor.optionalString("notes") ?: "")
                putRestoredImagePath(values, "imagePath", cursor.optionalString("imagePath"), imageDir)
                values.put("priceMin", cursor.optionalString("priceMin") ?: "")
                values.put("priceMax", cursor.optionalString("priceMax") ?: "")
                insertOrThrow(target, "items", values)
            }
        }
    }

    private fun importSaleLogs(source: SQLiteDatabase, target: SupportSQLiteDatabase) {
        source.query("sale_logs", SALE_LOG_COLUMNS.toTypedArray(), null, null, null, null, "id ASC").use { cursor ->
            while (cursor.moveToNext()) {
                val values = ContentValues()
                values.put("id", cursor.requiredLong("id"))
                values.put("itemName", cursor.requiredString("itemName"))
                values.put("category", cursor.requiredString("category"))
                values.put("price", cursor.requiredString("price"))
                values.put("quantity", cursor.requiredInt("quantity"))
                values.put("customerName", cursor.requiredString("customerName"))
                values.put("customerPhone", cursor.requiredString("customerPhone"))
                values.put("timestamp", cursor.requiredLong("timestamp"))
                insertOrThrow(target, "sale_logs", values)
            }
        }
    }

    private fun importOrderRequests(
        source: SQLiteDatabase,
        target: SupportSQLiteDatabase,
        imageDir: File
    ) {
        source.query("order_requests", ORDER_REQUEST_COLUMNS.toTypedArray(), null, null, null, null, "id ASC").use { cursor ->
            while (cursor.moveToNext()) {
                val values = ContentValues()
                values.put("id", cursor.requiredLong("id"))
                values.put("itemName", cursor.requiredString("itemName"))
                putRestoredImagePath(values, "itemImagePath", cursor.optionalString("itemImagePath"), imageDir)
                values.put("deviceName", cursor.requiredString("deviceName"))
                values.put("customerName", cursor.requiredString("customerName"))
                values.put("customerPhone", cursor.requiredString("customerPhone"))
                values.put("timestamp", cursor.requiredLong("timestamp"))
                insertOrThrow(target, "order_requests", values)
            }
        }
    }

    private fun putRestoredImagePath(
        values: ContentValues,
        columnName: String,
        originalPath: String?,
        imageDir: File
    ) {
        if (originalPath.isNullOrBlank()) {
            values.putNull(columnName)
        } else {
            values.put(columnName, File(imageDir, File(originalPath).name).absolutePath)
        }
    }

    private fun insertOrThrow(target: SupportSQLiteDatabase, table: String, values: ContentValues) {
        if (target.insert(table, SQLiteDatabase.CONFLICT_ABORT, values) == -1L) {
            throw IllegalStateException("تعذّر إدخال بيانات $table")
        }
    }

    private fun tableExists(database: SQLiteDatabase, table: String): Boolean =
        database.rawQuery(
            "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ? LIMIT 1",
            arrayOf(table)
        ).use { it.moveToFirst() }

    private fun tableColumns(database: SQLiteDatabase, table: String): Set<String> =
        database.rawQuery("PRAGMA table_info(`$table`)", null).use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            buildSet {
                while (cursor.moveToNext()) add(cursor.getString(nameIndex))
            }
        }

    private fun isSafeEntryName(name: String): Boolean =
        name.isNotBlank() && name == File(name).name && !name.contains('\\') &&
            name != "." && name != ".."

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var read = input.read(buffer)
            while (read >= 0) {
                if (read > 0) digest.update(buffer, 0, read)
                read = input.read(buffer)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun copyWithLimit(input: InputStream, output: OutputStream, alreadyCopied: Long): Long {
        val buffer = ByteArray(8192)
        var total = alreadyCopied
        var read = input.read(buffer)
        while (read >= 0) {
            if (read > 0) {
                total += read
                if (total > MAX_BACKUP_BYTES) {
                    throw BackupValidationException("حجم النسخة الاحتياطية أكبر من الحد المسموح")
                }
                output.write(buffer, 0, read)
            }
            read = input.read(buffer)
        }
        return total
    }

    private fun Cursor.requiredString(column: String): String =
        optionalString(column) ?: throw BackupValidationException("بيانات نصية مطلوبة مفقودة من النسخة")

    private fun Cursor.requiredLong(column: String): Long {
        val index = getColumnIndexOrThrow(column)
        if (isNull(index)) throw BackupValidationException("بيانات رقمية مطلوبة مفقودة من النسخة")
        return getLong(index)
    }

    private fun Cursor.requiredInt(column: String): Int = requiredLong(column).toInt()

    private fun Cursor.optionalString(column: String): String? {
        val index = getColumnIndex(column)
        return if (index < 0 || isNull(index)) null else getString(index)
    }

    private fun Cursor.optionalInt(column: String): Int? {
        val index = getColumnIndex(column)
        return if (index < 0 || isNull(index)) null else getInt(index)
    }
}
