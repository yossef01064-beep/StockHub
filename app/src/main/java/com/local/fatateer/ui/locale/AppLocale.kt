package com.local.fatateer.ui.locale

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.LayoutDirection

enum class AppLanguage(val code: String) {
    ARABIC("ar"),
    ENGLISH("en");

    val isRtl: Boolean get() = this == ARABIC
    val layoutDirection: LayoutDirection
        get() = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
}

/** null = follow system, true = dark, false = light */
enum class ThemeMode(val prefsValue: String) {
    SYSTEM("system"),
    DARK("dark"),
    LIGHT("light");

    companion object {
        fun fromPrefs(v: String?): ThemeMode = when (v) {
            "dark" -> DARK
            "light" -> LIGHT
            else -> SYSTEM
        }
    }
}

class SettingsController(context: Context) {
    private val prefs = context.getSharedPreferences("fatateer_settings", Context.MODE_PRIVATE)

    var language by mutableStateOf(
        when (prefs.getString("lang", "ar")) {
            "en" -> AppLanguage.ENGLISH
            else -> AppLanguage.ARABIC
        }
    )
        private set

    var themeMode by mutableStateOf(ThemeMode.fromPrefs(prefs.getString("theme_mode", "system")))
        private set

    val strings: AppStrings
        get() = if (language == AppLanguage.ENGLISH) AppStrings.En else AppStrings.Ar

    fun setLanguage(lang: AppLanguage) {
        language = lang
        prefs.edit().putString("lang", lang.code).apply()
    }

    fun setThemeMode(mode: ThemeMode) {
        themeMode = mode
        prefs.edit().putString("theme_mode", mode.prefsValue).apply()
    }

    fun isDark(systemDark: Boolean): Boolean = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> systemDark
    }
}

val LocalAppStrings = staticCompositionLocalOf { AppStrings.Ar as AppStrings }
val LocalSettingsController = staticCompositionLocalOf<SettingsController> {
    error("SettingsController not provided")
}

@Composable
fun rememberSettingsController(): SettingsController {
    val context = LocalContext.current
    return androidx.compose.runtime.remember { SettingsController(context) }
}

/**
 * Display label for a category / sub-category / brand value stored in the DB.
 * Keys stay in Arabic (or Latin brands) so existing data keeps working.
 */
fun displayLabel(key: String, s: AppStrings): String = s.categoryLabel(key)

sealed class AppStrings {
    abstract val home: String
    abstract val spareParts: String
    abstract val sales: String
    abstract val settings: String
    abstract val search: String
    abstract val categories: String
    abstract val allCategories: String
    abstract val lowStock: String
    abstract val outOfStock: String
    abstract val almostOut: String
    abstract val shopSummary: String
    abstract val totalItems: String
    abstract val neededOrder: String
    abstract val noLowStock: String
    abstract val lowStockHint: String
    abstract val emptyItems: String
    abstract val backToOverview: String
    abstract val addItem: String
    abstract val edit: String
    abstract val delete: String
    abstract val deleteConfirmTitle: String
    abstract val deleteConfirmBody: String
    abstract val save: String
    abstract val cancel: String
    abstract val name: String
    abstract val mainSection: String
    abstract val section: String
    abstract val remoteType: String
    abstract val brandOrType: String
    abstract val qtyInShop: String
    abstract val reorderLevel: String
    abstract val note: String
    abstract val errName: String
    abstract val errSection: String
    abstract val errRemoteType: String
    abstract val errQty: String
    abstract val errMinQty: String
    abstract val plus: String
    abstract val minus: String
    abstract val toggleTheme: String
    abstract val themeTitle: String
    abstract val themeSystem: String
    abstract val themeDark: String
    abstract val themeLight: String
    abstract val languageTitle: String
    abstract val languageArabic: String
    abstract val languageEnglish: String
    abstract val settingsTitle: String
    abstract val openSpare: String
    abstract val openSales: String
    abstract val neededCountLabel: String
    abstract val lowCountLabel: String
    abstract val lowStockListTitle: String
    abstract val noLowStockItems: String
    abstract val addPhoto: String
    abstract val changePhoto: String
    abstract val removePhoto: String

    abstract fun categoryLabel(key: String): String

    object Ar : AppStrings() {
        override val home = "الرئيسية"
        override val spareParts = "قطع الغيار"
        override val sales = "البيع"
        override val settings = "الإعدادات"
        override val search = "بحث"
        override val categories = "التصنيفات"
        override val allCategories = "كل الأقسام"
        override val lowStock = "ناقص"
        override val outOfStock = "نفد"
        override val almostOut = "قرب يخلص"
        override val shopSummary = "ملخص المحل"
        override val totalItems = "إجمالي الأصناف"
        override val neededOrder = "محتاج طلب"
        override val noLowStock = "مفيش أصناف ناقصة حالياً"
        override val lowStockHint = "في %d صنف محتاج طلب — استخدم الشريط تحت عشان تراجع"
        override val emptyItems = "مفيش أصناف هنا. اضغط + وأضف."
        override val backToOverview = "← رجوع للموجز"
        override val addItem = "إضافة صنف"
        override val edit = "تعديل"
        override val delete = "حذف"
        override val deleteConfirmTitle = "حذف؟"
        override val deleteConfirmBody = "هيتشال %s"
        override val save = "حفظ"
        override val cancel = "إلغاء"
        override val name = "الاسم"
        override val mainSection = "القسم الرئيسي"
        override val section = "القسم"
        override val remoteType = "نوع الريموت"
        override val brandOrType = "الماركة / النوع"
        override val qtyInShop = "المتبقي في المحل"
        override val reorderLevel = "حد الطلب"
        override val note = "ملاحظة"
        override val errName = "اكتب الاسم"
        override val errSection = "اختار القسم"
        override val errRemoteType = "اختار نوع الريموت"
        override val errQty = "كمية غلط"
        override val errMinQty = "حد الطلب غلط"
        override val plus = "زيادة"
        override val minus = "نقص"
        override val toggleTheme = "تبديل الثيم"
        override val themeTitle = "المظهر"
        override val themeSystem = "حسب النظام"
        override val themeDark = "داكن"
        override val themeLight = "فاتح"
        override val languageTitle = "اللغة"
        override val languageArabic = "العربية"
        override val languageEnglish = "English"
        override val settingsTitle = "الإعدادات"
        override val openSpare = "قطع الغيار"
        override val openSales = "البيع"
        override val neededCountLabel = "محتاج طلب"
        override val lowCountLabel = "ناقص: %d"
        override val lowStockListTitle = "الأصناف الناقصة"
        override val noLowStockItems = "مفيش أصناف ناقصة"
        override val addPhoto = "إضافة صورة"
        override val changePhoto = "تغيير الصورة"
        override val removePhoto = "إزالة الصورة"

        override fun categoryLabel(key: String): String = key
    }

    object En : AppStrings() {
        override val home = "Home"
        override val spareParts = "Spare parts"
        override val sales = "Sales"
        override val settings = "Settings"
        override val search = "Search"
        override val categories = "Categories"
        override val allCategories = "All categories"
        override val lowStock = "Low"
        override val outOfStock = "Out"
        override val almostOut = "Almost out"
        override val shopSummary = "Shop summary"
        override val totalItems = "Total items"
        override val neededOrder = "Need reorder"
        override val noLowStock = "No low-stock items right now"
        override val lowStockHint = "%d item(s) need reorder — use the bar below to review"
        override val emptyItems = "No items here. Tap + to add."
        override val backToOverview = "← Back to overview"
        override val addItem = "Add item"
        override val edit = "Edit"
        override val delete = "Delete"
        override val deleteConfirmTitle = "Delete?"
        override val deleteConfirmBody = "%s will be removed"
        override val save = "Save"
        override val cancel = "Cancel"
        override val name = "Name"
        override val mainSection = "Main section"
        override val section = "Category"
        override val remoteType = "Remote type"
        override val brandOrType = "Brand / type"
        override val qtyInShop = "Qty in shop"
        override val reorderLevel = "Reorder level"
        override val note = "Note"
        override val errName = "Enter a name"
        override val errSection = "Choose a category"
        override val errRemoteType = "Choose remote type"
        override val errQty = "Invalid quantity"
        override val errMinQty = "Invalid reorder level"
        override val plus = "Increase"
        override val minus = "Decrease"
        override val toggleTheme = "Toggle theme"
        override val themeTitle = "Appearance"
        override val themeSystem = "System"
        override val themeDark = "Dark"
        override val themeLight = "Light"
        override val languageTitle = "Language"
        override val languageArabic = "العربية"
        override val languageEnglish = "English"
        override val settingsTitle = "Settings"
        override val openSpare = "Spare parts"
        override val openSales = "Sales"
        override val neededCountLabel = "Need reorder"
        override val lowCountLabel = "Low: %d"
        override val lowStockListTitle = "Low-stock items"
        override val noLowStockItems = "No low-stock items"
        override val addPhoto = "Add photo"
        override val changePhoto = "Change photo"
        override val removePhoto = "Remove photo"

        private val map = mapOf(
            // Spare parts
            "IC الصوت" to "Sound IC",
            "IC TV" to "TV IC",
            "المكثفات" to "Capacitors",
            "الدوائر الكاملة" to "Complete boards",
            "IC فرتكال" to "Vertical IC",
            "قطع سماعات" to "Speaker parts",
            // Sales
            "ريموتات" to "Remotes",
            "رسيفرات" to "Receivers",
            "تلفزيونات" to "TVs",
            "عدسات دش" to "Dish LNBs",
            "عدسات رقمية" to "Digital LNBs",
            "كابلات" to "Cables",
            "أدابتر 12V" to "12V adapters",
            "لفات سلاك دش" to "Dish slack coils",
            "أطباق دش" to "Satellite dishes",
            "فلانشات طبق" to "Dish flanges",
            "إكسسوار دش" to "Dish accessories",
            "بطاريات قلم 1.5V" to "1.5V AA batteries",
            // Remote groups
            "HD" to "HD",
            "SD" to "SD",
            "تلفزيون" to "TV",
            // Remote SD types
            "صيني صف واحد" to "Chinese 1-row",
            "صيني بدون صف" to "Chinese no-row",
            "صيني 2 صف" to "Chinese 2-row",
            "تورمان" to "Turman",
            "STRONG" to "STRONG",
            "USTRA" to "USTRA",
            // Brands
            "توشيبا" to "Toshiba",
            "باناسونيك" to "Panasonic",
            "جولدي" to "Goldi",
            "صيني" to "Chinese",
            "أخرى" to "Other",
            "LG" to "LG",
            "Gold" to "Gold",
            "Goldstar" to "Goldstar",
            "TIMER" to "TIMER",
            "L/M" to "L/M",
            "GOTE" to "GOTE",
            "EPG" to "EPG",
            // Dish lens
            "1 مخرج" to "1 output",
            "2 مخرج" to "2 outputs",
            "4 مخرج" to "4 outputs",
            // Sections (used as values in editor)
            "قطع الغيار" to "Spare parts",
            "البيع" to "Sales"
        )

        override fun categoryLabel(key: String): String = map[key] ?: key
    }
}
