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

enum class AccentColor(val color: Color, val nameRes: String) {
    BLUE(Color(0xFF2196F3), "ar_blue"),
    GREEN(Color(0xFF4CAF50), "ar_green"),
    PURPLE(Color(0xFF9C27B0), "ar_purple"),
    RED(Color(0xFFFF4081), "ar_red"),
    ORANGE(Color(0xFFFF5722), "ar_orange"),
    YELLOW(Color(0xFFFFC107), "ar_yellow"),
    PINK(Color(0xFFE91E63), "ar_pink"),
    TEAL(Color(0xFF009688), "ar_teal"),
    DEEP_PURPLE(Color(0xFF673AB7), "ar_deep_purple"),
    BROWN(Color(0xFF795548), "ar_brown");

    companion object {
        fun fromPrefs(v: String?): AccentColor = when (v) {
            "ar_green" -> GREEN
            "ar_purple" -> PURPLE
            "ar_red" -> RED
            "ar_orange" -> ORANGE
            "ar_yellow" -> YELLOW
            "ar_pink" -> PINK
            "ar_teal" -> TEAL
            "ar_deep_purple" -> DEEP_PURPLE
            "ar_brown" -> BROWN
            else -> BLUE // Default
        }
    }
}

class SettingsController(context: Context) {
    private val prefs = context.getSharedPreferences("fatateer_settings", Context.MODE_PRIVATE)

    private var _language by mutableStateOf(
        when (prefs.getString("lang", "ar")) {
            "en" -> AppLanguage.ENGLISH
            else -> AppLanguage.ARABIC
        }
    )
    val language: AppLanguage get() = _language

    private var _themeMode by mutableStateOf(ThemeMode.fromPrefs(prefs.getString("theme_mode", "system")))
    val themeMode: ThemeMode get() = _themeMode

    private var _accentColor by mutableStateOf(AccentColor.fromPrefs(prefs.getString("accent_color", "blue")))
    val accentColor: AccentColor get() = _accentColor

    val strings: AppStrings
        get() = if (language == AppLanguage.ENGLISH) AppStrings.En else AppStrings.Ar

    fun setLanguage(lang: AppLanguage) {
        _language = lang
        prefs.edit().putString("lang", lang.code).apply()
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode = mode
        prefs.edit().putString("theme_mode", mode.prefsValue).apply()
    }

    fun setAccentColor(color: AccentColor) {
        _accentColor = color
        prefs.edit().putString("accent_color", color.nameRes).apply()
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
    abstract val themeAppColor: String
    abstract val appColorTitle: String
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
    abstract val nameRequired: String
    abstract val qtyRequired: String
    abstract val salesLogTitle: String
    abstract val noSalesLogs: String
    abstract val clearLogs: String
    abstract val customerName: String
    abstract val customerPhone: String
    abstract val price: String
    abstract val quantity: String
    abstract val notes: String
    abstract val brand: String
    abstract val subCategory: String
    abstract val category: String
    abstract val minQuantity: String
    abstract val priceMin: String
    abstract val priceMax: String

    // Theme accent color names
    abstract val ar_blue: String
    abstract val ar_green: String
    abstract val ar_purple: String
    abstract val ar_red: String
    abstract val ar_orange: String
    abstract val ar_yellow: String
    abstract val ar_pink: String
    abstract val ar_teal: String
    abstract val ar_deep_purple: String
    abstract val ar_brown: String

    // Sales / dialogs
    abstract val sell: String
    abstract val priceMustBePositive: String
    abstract val priceTooHigh: String
    abstract val qtyMustBePositive: String
    abstract val qtyExceedsStock: String
    abstract val collapse: String
    abstract val expand: String
    abstract val operations: String
    abstract val share: String
    abstract val shareVia: String
    abstract val date: String
    abstract val item: String
    abstract val customer: String
    abstract val phone: String
    abstract val totalDay: String
    abstract val backupRestore: String
    abstract val backupRestoreTitle: String
    abstract val export: String
    abstract val restore: String
    abstract val exportBackup: String
    abstract val restoreBackup: String
    abstract val close: String
    abstract val daily: String
    abstract val monthly: String
    abstract val back: String

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
        override val themeAppColor = "لون التطبيق"
        override val appColorTitle = "🎨 الثيم"
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
        override val nameRequired = "الاسم مطلوب"
        override val qtyRequired = "الكمية مطلوبة"
        override val salesLogTitle = "سجل المبيعات"
        override val noSalesLogs = "لا يوجد سجل مبيعات"
        override val clearLogs = "مسح السجل"
        override val customerName = "اسم الزبون"
        override val customerPhone = "رقم الهاتف"
        override val price = "السعر"
        override val quantity = "الكمية"
        override val notes = "ملاحظات"
        override val brand = "الماركة"
        override val subCategory = "القسم الفرعي"
        override val category = "القسم"
        override val minQuantity = "حد الطلب"
        override val priceMin = "أقل سعر"
        override val priceMax = "أعلى سعر"

        override val ar_blue = "أزرق"
        override val ar_green = "أخضر"
        override val ar_purple = "بنفسجي"
        override val ar_red = "أحمر"
        override val ar_orange = "برتقالي"
        override val ar_yellow = "أصفر"
        override val ar_pink = "وردي"
        override val ar_teal = "تركواز"
        override val ar_deep_purple = "بنفسجي غامق"
        override val ar_brown = "بني"

        override val sell = "بيع"
        override val priceMustBePositive = "السعر يجب أن يكون أكبر من صفر"
        override val priceTooHigh = "السعر مرتفع جدًا"
        override val qtyMustBePositive = "الكمية يجب أن تكون أكبر من صفر"
        override val qtyExceedsStock = "الكمية أكبر من المتوفر"
        override val collapse = "طي"
        override val expand = "توسيع"
        override val operations = "عمليات"
        override val share = "مشاركة"
        override val shareVia = "مشاركة عبر"
        override val date = "التاريخ"
        override val item = "الصنف"
        override val customer = "الزبون"
        override val phone = "الهاتف"
        override val totalDay = "إجمالي اليوم"
        override val backupRestore = "نسخ احتياطي / استعادة"
        override val backupRestoreTitle = "النسخ الاحتياطي"
        override val export = "تصدير"
        override val restore = "استعادة"
        override val exportBackup = "تصدير نسخة احتياطية"
        override val restoreBackup = "استعادة نسخة احتياطية"
        override val close = "إغلاق"
        override val daily = "يومي"
        override val monthly = "شهري"
        override val back = "رجوع"

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
        override val themeAppColor = "App Color"
        override val appColorTitle = "🎨 Theme"
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
        override val nameRequired = "Name required"
        override val qtyRequired = "Quantity required"
        override val salesLogTitle = "Sales Log"
        override val noSalesLogs = "No sales logs found"
        override val clearLogs = "Clear logs"
        override val customerName = "Customer Name"
        override val customerPhone = "Phone Number"
        override val price = "Price"
        override val quantity = "Quantity"
        override val notes = "Notes"
        override val brand = "Brand"
        override val subCategory = "Sub-category"
        override val category = "Category"
        override val minQuantity = "Min Quantity"
        override val priceMin = "Min Price"
        override val priceMax = "Max Price"

        override val ar_blue = "Blue"
        override val ar_green = "Green"
        override val ar_purple = "Purple"
        override val ar_red = "Red"
        override val ar_orange = "Orange"
        override val ar_yellow = "Yellow"
        override val ar_pink = "Pink"
        override val ar_teal = "Teal"
        override val ar_deep_purple = "Deep Purple"
        override val ar_brown = "Brown"

        override val sell = "Sell"
        override val priceMustBePositive = "Price must be positive"
        override val priceTooHigh = "Price is too high"
        override val qtyMustBePositive = "Quantity must be positive"
        override val qtyExceedsStock = "Quantity exceeds stock"
        override val collapse = "Collapse"
        override val expand = "Expand"
        override val operations = "ops"
        override val share = "Share"
        override val shareVia = "Share via"
        override val date = "Date"
        override val item = "Item"
        override val customer = "Customer"
        override val phone = "Phone"
        override val totalDay = "Day total"
        override val backupRestore = "Backup / Restore"
        override val backupRestoreTitle = "Backup"
        override val export = "Export"
        override val restore = "Restore"
        override val exportBackup = "Export backup"
        override val restoreBackup = "Restore backup"
        override val close = "Close"
        override val daily = "Daily"
        override val monthly = "Monthly"
        override val back = "Back"

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
            "سماعات" to "Speakers",
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
