package com.local.fatateer.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.local.fatateer.data.AppDatabase
import com.local.fatateer.data.Categories
import com.local.fatateer.data.ImageStorage
import com.local.fatateer.data.Item
import com.local.fatateer.data.SaleLog
import com.local.fatateer.data.SaleLogDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

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
    private val dao = AppDatabase.get(app).itemDao()
    private val logDao = AppDatabase.get(app).saleLogDao()
    private val queryFlow = MutableStateFlow("")
    private val categoryFlow = MutableStateFlow<String?>(null)
    private val tabFlow = MutableStateFlow(MainTab.HOME)

    val state = combine(
        dao.observeAll(),
        queryFlow,
        categoryFlow,
        tabFlow
    ) { list, query, cat, tab ->
        StockUiState(items = list, query = query, selectedCategory = cat, tab = tab)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StockUiState())

    val salesLogs: Flow<List<SaleLog>> = logDao.observeAll()

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

    fun exportBackup(uri: Uri, context: Application) {
        viewModelScope.launch {
            val dbFile = context.getDatabasePath("app_database")
            val imageDir = File(context.filesDir, "item_images")

            try {
                val zipOutputStream = ZipOutputStream(context.contentResolver.openOutputStream(uri))

                // Add database to zip
                zipOutputStream.putNextEntry(ZipEntry("app_database"))
                dbFile.copyTo(zipOutputStream, bufferSize = 8192)
                zipOutputStream.closeEntry()

                // Add images to zip
                imageDir.listFiles()?.forEach { file ->
                    zipOutputStream.putNextEntry(ZipEntry(file.name))
                    file.copyTo(zipOutputStream, bufferSize = 8192)
                    zipOutputStream.closeEntry()
                }

                zipOutputStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun restoreBackup(uri: Uri, context: Application) {
        viewModelScope.launch {
            val tempDir = File(context.cacheDir, "temp_backup").apply { mkdirs() }
            val zipFile = File(tempDir, "backup.zip")

            try {
                // Copy the backup file to cache
                context.contentResolver.openInputStream(uri)?.use { input ->
                    zipFile.outputStream().use { output -> input.copyTo(output) }
                }

                // Extract the zip file
                val zipInputStream = ZipInputStream(zipFile.inputStream())
                val buffer = ByteArray(8192)

                var entry: ZipEntry?
                while (zipInputStream.readEntry().also { entry = it } != null) {
                    if (entry?.name == "app_database") {
                        // Replace the database
                        val dbFile = context.getDatabasePath("app_database")
                        dbFile.parentFile?.mkdirs()
                        FileOutputStream(dbFile).use { output ->
                            zipInputStream.use { input -> input.copyTo(output, buffer) }
                        }
                    } else if (entry?.name != null) {
                        // Save images to internal storage
                        val imageDir = File(context.filesDir, "item_images")
                        imageDir.mkdirs()
                        val imageFile = File(imageDir, entry.name)
                        FileOutputStream(imageFile).use { output ->
                            zipInputStream.use { input -> input.copyTo(output, buffer) }
                        }
                    }
                    zipInputStream.closeEntry()
                }

                zipInputStream.close()
                zipFile.delete()

                // Refresh data
                state.value = state.value.copy()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
