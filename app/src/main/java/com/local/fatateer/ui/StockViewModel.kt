package com.local.fatateer.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.local.fatateer.data.AppDatabase
import com.local.fatateer.data.Categories
import com.local.fatateer.data.ImageStorage
import com.local.fatateer.data.Item
import com.local.fatateer.data.SaleLog
import com.local.fatateer.data.SaleLogDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

enum class MainTab { HOME, SPARE, SALES }

data class StockUiState(
    val items: List<Item> = emptyList(),
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

    private val itemsFlow: Flow<List<Item>> = dbVersionFlow.flatMapLatest { dao.observeAll() }
    private val logsFlow: Flow<List<SaleLog>> = dbVersionFlow.flatMapLatest { logDao.observeAll() }

    val state = combine(
        itemsFlow,
        queryFlow,
        categoryFlow,
        tabFlow
    ) { list, query, cat, tab ->
        StockUiState(items = list, query = query, selectedCategory = cat, tab = tab)
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

    fun exportBackup(uri: Uri, context: Application, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch(Dispatchers.IO) {
            val imageDir = File(context.filesDir, "item_images")

            try {
                // تفريغ الـWAL أولًا حتى يحتوي ملف القاعدة الرئيسي على كل البيانات المؤكدة
                try {
                    db.query("PRAGMA wal_checkpoint(FULL)", null).close()
                } catch (_: Exception) {
                    // بعض إصدارات SQLite/الأجهزة قد لا تدعم هذا - نتابع بدونه
                }

                val dbFile = context.getDatabasePath(AppDatabase.DB_FILE_NAME)
                if (!dbFile.exists()) {
                    withContext(Dispatchers.Main) { onResult(false, "تعذّر العثور على قاعدة البيانات") }
                    return@launch
                }

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    ZipOutputStream(outputStream).use { zipOutputStream ->
                        // Add database to zip (بالاسم الفعلي الحالي لملف القاعدة)
                        zipOutputStream.putNextEntry(ZipEntry(AppDatabase.DB_FILE_NAME))
                        dbFile.inputStream().use { input -> input.copyTo(zipOutputStream) }
                        zipOutputStream.closeEntry()

                        // Add images to zip
                        imageDir.listFiles()?.forEach { file ->
                            zipOutputStream.putNextEntry(ZipEntry(file.name))
                            file.inputStream().use { input -> input.copyTo(zipOutputStream) }
                            zipOutputStream.closeEntry()
                        }
                    }
                } ?: run {
                    withContext(Dispatchers.Main) { onResult(false, "تعذّر فتح الملف المحدد للكتابة") }
                    return@launch
                }

                withContext(Dispatchers.Main) { onResult(true, null) }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { onResult(false, "حدث خطأ أثناء إنشاء النسخة الاحتياطية") }
            }
        }
    }

    fun restoreBackup(uri: Uri, context: Application, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch(Dispatchers.IO) {
            val tempDir = File(context.cacheDir, "restore_tmp_${System.currentTimeMillis()}").apply { mkdirs() }
            try {
                // 1) نسخ الملف المختار إلى مساحة داخلية مؤقتة
                val zipFile = File(tempDir, "backup.stockhub")
                val copiedOk = context.contentResolver.openInputStream(uri)?.use { input ->
                    zipFile.outputStream().use { output -> input.copyTo(output) }
                    true
                } ?: false

                if (!copiedOk || !zipFile.exists() || zipFile.length() == 0L) {
                    withContext(Dispatchers.Main) { onResult(false, "تعذّرت قراءة الملف المحدد") }
                    return@launch
                }

                // 2) فك الأرشيف إلى مجلد مؤقت أولًا (بدون لمس بيانات التطبيق الحالية بعد)
                val extractDir = File(tempDir, "extracted").apply { mkdirs() }
                var dbEntryFile: File? = null
                val imageEntryFiles = mutableListOf<File>()

                try {
                    ZipInputStream(zipFile.inputStream()).use { zipInputStream ->
                        val buffer = ByteArray(8192)
                        var entry: ZipEntry? = zipInputStream.nextEntry
                        while (entry != null) {
                            val rawName = entry.name
                            if (!entry.isDirectory && rawName.isNotBlank()) {
                                // إزالة أي مسار فرعي/محاولة اجتياز مسارات (path traversal) - نأخذ اسم الملف فقط
                                val safeName = File(rawName).name
                                if (safeName.isNotBlank()) {
                                    val outFile = File(extractDir, safeName)
                                    FileOutputStream(outFile).use { output ->
                                        zipInputStream.copyTo(output, buffer.size)
                                    }
                                    if (safeName == AppDatabase.DB_FILE_NAME || safeName in legacyDbEntryNames) {
                                        dbEntryFile = outFile
                                    } else {
                                        imageEntryFiles.add(outFile)
                                    }
                                }
                            }
                            zipInputStream.closeEntry()
                            entry = zipInputStream.nextEntry
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) { onResult(false, "ملف النسخة الاحتياطية تالف أو غير مقروء") }
                    tempDir.deleteRecursively()
                    return@launch
                }

                val validatedDbFile = dbEntryFile
                if (validatedDbFile == null || !validatedDbFile.exists()) {
                    withContext(Dispatchers.Main) { onResult(false, "الملف المحدد لا يحتوي على نسخة قاعدة بيانات صالحة") }
                    tempDir.deleteRecursively()
                    return@launch
                }

                // 3) تحقق سريع أن الملف المستخرج فعلًا قاعدة بيانات SQLite صالحة قبل أي تعديل حقيقي
                val isValidSqlite = validatedDbFile.length() >= 16 && validatedDbFile.inputStream().use { ins ->
                    val header = ByteArray(16)
                    ins.read(header) == 16 && String(header, Charsets.US_ASCII).startsWith("SQLite format 3")
                }
                if (!isValidSqlite) {
                    withContext(Dispatchers.Main) { onResult(false, "ملف قاعدة البيانات داخل النسخة الاحتياطية غير صالح") }
                    tempDir.deleteRecursively()
                    return@launch
                }

                // من هنا فصاعدًا: الملف تحقّقنا من صلاحيته، آمن نبدأ الاستبدال الفعلي
                // 4) إغلاق الاتصال الحالي بقاعدة البيانات لتحرير القفل قبل استبدال الملف
                db.close()
                AppDatabase.closeAndClearInstance()

                // 5) استبدال ملف القاعدة الفعلي + حذف أي -wal/-shm قديمة حتى يكون
                //    الملف الجديد هو المرجع الوحيد والكامل للبيانات
                val liveDbFile = context.getDatabasePath(AppDatabase.DB_FILE_NAME)
                liveDbFile.parentFile?.mkdirs()
                File(liveDbFile.path + "-wal").let { if (it.exists()) it.delete() }
                File(liveDbFile.path + "-shm").let { if (it.exists()) it.delete() }
                validatedDbFile.copyTo(liveDbFile, overwrite = true)

                // 6) استرداد صور المنتجات إلى مجلد الصور الداخلي الصحيح
                val imageDir = File(context.filesDir, "item_images").apply { mkdirs() }
                imageEntryFiles.forEach { src ->
                    src.copyTo(File(imageDir, src.name), overwrite = true)
                }

                // 7) إعادة فتح قاعدة البيانات على الملف الجديد
                db = AppDatabase.get(context)

                // 8) إعادة ربط مسارات الصور في سجلات المنتجات بالمسار الفعلي الحالي
                //    (المسار القديم المخزَّن كان absolute path على تركيب/جهاز آخر وقد لا يكون صالحًا الآن)
                try {
                    val restoredItems = db.itemDao().observeAll().first()
                    restoredItems.forEach { item ->
                        val storedPath = item.imagePath
                        if (!storedPath.isNullOrBlank()) {
                            val fileName = File(storedPath).name
                            val restoredImageFile = File(imageDir, fileName)
                            val newPath = if (restoredImageFile.exists()) restoredImageFile.absolutePath else null
                            if (newPath != item.imagePath) {
                                db.itemDao().update(item.copy(imagePath = newPath))
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    // فشل إعادة ربط الصور لا يعني فشل الاسترداد نفسه - البيانات الأساسية استُردت بنجاح
                }

                // 9) إجبار كل الـFlows على إعادة الاشتراك في قاعدة البيانات الجديدة
                withContext(Dispatchers.Main) {
                    dbVersionFlow.value += 1
                    onResult(true, null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { onResult(false, "حدث خطأ غير متوقع أثناء الاسترداد") }
            } finally {
                tempDir.deleteRecursively()
            }
        }
    }
}
