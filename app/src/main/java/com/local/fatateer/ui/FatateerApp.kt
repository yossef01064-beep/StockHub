package com.local.fatateer.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.local.fatateer.data.Categories
import com.local.fatateer.data.ImageStorage
import com.local.fatateer.data.Item
import com.local.fatateer.ui.locale.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FatateerApp(
    isDark: Boolean = false,
    vm: StockViewModel = viewModel()
) {
    val settings = LocalSettingsController.current
    val s = LocalAppStrings.current
    val state by vm.state.collectAsStateWithLifecycle()
    val logs by vm.salesLogs.collectAsStateWithLifecycle(initialValue = emptyList())
    var editor by remember { mutableStateOf<Item?>(null) }
    var showNew by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<Item?>(null) }
    var itemToSell by remember { mutableStateOf<Item?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var showLowStock by remember { mutableStateOf(false) }
    var showLog by remember { mutableStateOf(false) }

    val pagerState = rememberPagerState(pageCount = { 3 })
    val pagerScope = rememberCoroutineScope()
    val currentTab = MainTab.values().getOrElse(pagerState.currentPage) { MainTab.HOME }

    LaunchedEffect(pagerState.currentPage) {
        vm.setTab(MainTab.values()[pagerState.currentPage])
    }

    fun goToTab(tab: MainTab) {
        showLowStock = false
        pagerScope.launch { pagerState.animateScrollToPage(tab.ordinal) }
    }

    val title = when {
        showSettings -> s.settingsTitle
        showLowStock -> s.lowStockListTitle
        currentTab == MainTab.HOME -> s.home
        currentTab == MainTab.SPARE -> s.spareParts
        else -> s.sales
    }

    val chipCats = when (currentTab) {
        MainTab.HOME -> emptyList()
        MainTab.SPARE -> Categories.spareParts
        MainTab.SALES -> Categories.sales
    }

    val defaultCategory = when (currentTab) {
        MainTab.SPARE -> Categories.spareParts.first()
        MainTab.SALES -> Categories.sales.first()
        MainTab.HOME -> Categories.all.first()
    }

    BackHandler(enabled = true) {
        when {
            showSettings -> showSettings = false
            showLowStock -> showLowStock = false
            editor != null -> editor = null
            showNew -> showNew = false
            else -> {
                if (currentTab != MainTab.HOME) goToTab(MainTab.HOME)
            }
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides settings.language.layoutDirection) {
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val drawerScope = rememberCoroutineScope()

        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = true,
            drawerContent = {
                ModalDrawerSheet {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("القائمة الرئيسية", modifier = Modifier.padding(bottom = 16.dp, start = 12.dp), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        NavigationDrawerItem(
                            label = { Text(s.home) },
                            selected = currentTab == MainTab.HOME,
                            onClick = { goToTab(MainTab.HOME); drawerScope.launch { drawerState.close() } },
                            icon = { Icon(Icons.Default.Home, null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        NavigationDrawerItem(
                            label = { Text(s.spareParts) },
                            selected = currentTab == MainTab.SPARE,
                            onClick = { goToTab(MainTab.SPARE); drawerScope.launch { drawerState.close() } },
                            icon = { Icon(Icons.Default.Build, null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        NavigationDrawerItem(
                            label = { Text(s.sales) },
                            selected = currentTab == MainTab.SALES,
                            onClick = { goToTab(MainTab.SALES); drawerScope.launch { drawerState.close() } },
                            icon = { Icon(Icons.Default.ShoppingCart, null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        NavigationDrawerItem(
                            label = { Text(s.salesLogTitle) },
                            selected = showLog,
                            onClick = { showLog = true; drawerScope.launch { drawerState.close() } },
                            icon = { Icon(Icons.Default.ListAlt, null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(20.dp))
                        Divider()
                        Spacer(Modifier.height(20.dp))
                        NavigationDrawerItem(
                            label = { Text(s.settingsTitle) },
                            selected = showSettings,
                            onClick = { showSettings = true; drawerScope.launch { drawerState.close() } },
                            icon = { Icon(Icons.Default.Settings, null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(title, fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            if (!showSettings && !showLowStock) {
                                IconButton(onClick = { drawerScope.launch { drawerState.open() } }) {
                                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                                }
                            } else {
                                IconButton(onClick = { showSettings = false; showLowStock = false }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.cancel)
                                }
                            }
                        },
                        actions = {
                            // Removed the secondary categories menu as it was redundant
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            titleContentColor = MaterialTheme.colorScheme.onPrimary,
                            actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                },
                bottomBar = {
                    if (!showSettings && !showLowStock) {
                        NavigationBar {
                            NavigationBarItem(selected = currentTab == MainTab.HOME, onClick = { goToTab(MainTab.HOME) }, icon = { Icon(Icons.Default.Home, null) }, label = { Text(s.home) })
                            NavigationBarItem(selected = currentTab == MainTab.SPARE, onClick = { goToTab(MainTab.SPARE) }, icon = { Icon(Icons.Default.Build, null) }, label = { Text(s.spareParts) })
                            NavigationBarItem(selected = currentTab == MainTab.SALES, onClick = { goToTab(MainTab.SALES) }, icon = { Icon(Icons.Default.ShoppingCart, null) }, label = { Text(s.sales) })
                        }
                    }
                },
                floatingActionButton = {
                    if (!showSettings && !showLowStock && currentTab != MainTab.HOME) {
                        FloatingActionButton(onClick = { showNew = true }) { Icon(Icons.Default.Add, contentDescription = s.addItem) }
                    }
                }
            ) { padding ->
                Box(modifier = Modifier.padding(padding)) {
    if (showLog) {
        SaleLogPage(
            logs = logs,
            onBack = { showLog = false },
            onDeleteLog = { vm.deleteSaleLog(it) }
        )
    } else if (showSettings) {
        SettingsScreen(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            onOpenLog = { showLog = true }
        )
    } else if (showLowStock) {
                        LowStockScreen(items = state.neededItems, onPlus = vm::plus, onMinus = vm::minus, onEdit = { editor = it }, onDelete = { toDelete = it }, onSell = { itemToSell = it }, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp))
                    } else {
                        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                            when (MainTab.values()[page]) {
                                MainTab.HOME -> HomeScreen(state = state, modifier = Modifier.fillMaxSize().padding(16.dp), onOpenSpare = { goToTab(MainTab.SPARE) }, onOpenSales = { goToTab(MainTab.SALES) }, onOpenLowStock = { showLowStock = true })
                                MainTab.SPARE, MainTab.SALES -> InventoryScreen(
                                    state = state,
                                    chipCats = if (MainTab.values()[page] == MainTab.SPARE) Categories.spareParts else Categories.sales,
                                    onQuery = vm::setQuery, onCategory = vm::setCategory, onPlus = vm::plus, onMinus = vm::minus, onEdit = { editor = it }, onDelete = { toDelete = it }, onSell = { itemToSell = it },
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)
                                )
                            }
                        }
                    }
                    itemToSell?.let { currentItem ->
                        SaleDialog(item = currentItem, onDismiss = { itemToSell = null }, onConfirm = { qty, custName, custPhone, pricePerUnit -> 
                            vm.recordSale(currentItem, qty, pricePerUnit, custName, custPhone)
                            itemToSell = null 
                        })
                    }
                    toDelete?.let { item ->
                        AlertDialog(
                            onDismissRequest = { toDelete = null },
                            title = { Text("تأكيد الحذف") },
                            text = { Text("هل أنت متأكد من حذف ${item.name}؟") },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        vm.delete(item)
                                        toDelete = null
                                    }
                                ) { Text("حذف", color = MaterialTheme.colorScheme.error) }
                            },
                            dismissButton = {
                                TextButton(onClick = { toDelete = null }) { Text("إلغاء") }
                            }
                        )
                    }
                    editor?.let { itemToEdit ->
                        ItemEditorDialog(
                            title = "${s.edit}: ${itemToEdit.name}",
                            initial = itemToEdit,
                            allowedCategories = if (chipCats.isNotEmpty()) chipCats else Categories.all,
                            onDismiss = { editor = null },
                            onSave = { updatedItem ->
                                vm.save(updatedItem)
                                editor = null
                            }
                        )
                    }
                    if (showNew) {
                        ItemEditorDialog(
                            title = s.addItem,
                            initial = Item(id = 0L, name = "", category = defaultCategory, quantity = 0),
                            allowedCategories = chipCats,
                            onDismiss = { showNew = false },
                            onSave = { newItem ->
                                vm.save(newItem)
                                showNew = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(modifier: Modifier = Modifier, onOpenLog: () -> Unit) {
    val settings = LocalSettingsController.current
    val s = LocalAppStrings.current
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(s.themeTitle, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(8.dp)) {
                ThemeModeRow(label = s.themeSystem, icon = Icons.Default.BrightnessAuto, selected = settings.themeMode == ThemeMode.SYSTEM, onClick = { settings.setThemeMode(ThemeMode.SYSTEM) })
                ThemeModeRow(label = s.themeDark, icon = Icons.Default.DarkMode, selected = settings.themeMode == ThemeMode.DARK, onClick = { settings.setThemeMode(ThemeMode.DARK) })
                ThemeModeRow(label = s.themeLight, icon = Icons.Default.LightMode, selected = settings.themeMode == ThemeMode.LIGHT, onClick = { settings.setThemeMode(ThemeMode.LIGHT) })
            }
        }
        Text(s.languageTitle, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(8.dp)) {
                ThemeModeRow(label = s.languageArabic, icon = Icons.Default.Language, selected = settings.language == AppLanguage.ARABIC, onClick = { settings.setLanguage(AppLanguage.ARABIC) })
                ThemeModeRow(label = s.languageEnglish, icon = Icons.Default.Language, selected = settings.language == AppLanguage.ENGLISH, onClick = { settings.setLanguage(AppLanguage.ENGLISH) })
            }
        }
    }
}

@Composable
private fun ThemeModeRow(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, modifier = Modifier.weight(1f), fontSize = 15.sp)
        RadioButton(selected = selected, onClick = onClick)
    }
}

@Composable
private fun HomeScreen(state: StockUiState, modifier: Modifier = Modifier, onOpenSpare: () -> Unit, onOpenSales: () -> Unit, onOpenLowStock: () -> Unit) {
    val s = LocalAppStrings.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(s.shopSummary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniStat(s.spareParts, "${state.spareCount}", Modifier.weight(1f), onClick = onOpenSpare)
            MiniStat(s.sales, "${state.salesCount}", Modifier.weight(1f), onClick = onOpenSales)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniStat(s.totalItems, "${state.totalQty}", Modifier.weight(1f))
            MiniStat(s.lowStock, "${state.neededCount}", Modifier.weight(1f), alert = state.neededCount > 0, onClick = if (state.neededCount > 0) onOpenLowStock else null)
        }
        if (state.neededCount > 0) {
            Text(s.lowStockHint.replace("%d", state.neededCount.toString()), fontSize = 13.sp, color = lowStockContent())
        } else {
            Text(s.noLowStock, fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun MiniStat(title: String, value: String, modifier: Modifier = Modifier, alert: Boolean = false, onClick: (() -> Unit)? = null) {
    Card(onClick = { onClick?.invoke() }, enabled = onClick != null, modifier = modifier, shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = if (alert) lowStockContainer() else MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text(title, fontSize = 12.sp, color = if (alert) lowStockContent() else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Spacer(Modifier.height(4.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = if (alert) lowStockContent() else MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun InventoryScreen(state: StockUiState, chipCats: List<String>, onQuery: (String) -> Unit, onCategory: (String?) -> Unit, onPlus: (Item) -> Unit, onMinus: (Item) -> Unit, onEdit: (Item) -> Unit, onDelete: (Item) -> Unit, onSell: (Item) -> Unit, modifier: Modifier = Modifier) {
    val s = LocalAppStrings.current

    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            Text(if (state.selectedCategory == null && state.query.isBlank()) s.categories else (state.selectedCategory?.let { displayLabel(it, s) } ?: s.search), fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        if (state.selectedCategory == null && state.query.isBlank()) {
            Text(s.lowCountLabel.replace("%d", state.filtered.count { it.quantity <= it.minQuantity }.toString()), fontWeight = FontWeight.SemiBold, color = if (state.filtered.any { it.quantity <= it.minQuantity }) lowStockContent() else MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(12.dp))
            LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 88.dp), modifier = Modifier.fillMaxSize()) {
                gridItems(chipCats, key = { it }) { cat ->
                    val inCat = state.filtered.filter { it.category == cat }
                    val needed = inCat.count { it.quantity <= it.minQuantity }
                    Card(onClick = { onCategory(cat) }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = if (needed > 0) lowStockContainer() else MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), modifier = Modifier.height(128.dp)) {
                        Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(imageVector = categoryIcon(cat), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                            Spacer(Modifier.height(10.dp))
                            Text(displayLabel(cat, s), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 2, lineHeight = 16.sp)
                        }
                    }
                }
            }
        } else {
            OutlinedTextField(value = state.query, onValueChange = onQuery, modifier = Modifier.fillMaxWidth(), placeholder = { Text(s.search) }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true)
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { onCategory(null); onQuery("") }) { Text(s.backToOverview) }
            Spacer(Modifier.height(8.dp))
            if (state.filtered.isEmpty()) {
                Text(s.emptyItems, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
            } else {
                LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 88.dp), modifier = Modifier.fillMaxSize()) {
                    gridItems(state.filtered, key = { it.id }) { item ->
                        ItemCard(
                            item = item, onPlus = { onPlus(item) }, onMinus = { onMinus(item) }, onEdit = { onEdit(item) }, onDelete = { onDelete(item) }, onSell = { onSell(item) },
                            isSelected = false,
                            onSelect = {}
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LowStockScreen(items: List<Item>, onPlus: (Item) -> Unit, onMinus: (Item) -> Unit, onEdit: (Item) -> Unit, onDelete: (Item) -> Unit, onSell: (Item) -> Unit, modifier: Modifier = Modifier) {
    val s = LocalAppStrings.current
    Column(modifier) {
        if (items.isEmpty()) {
            Text(s.noLowStockItems, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        } else {
            LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 88.dp), modifier = Modifier.fillMaxSize()) {
                gridItems(items, key = { it.id }) { item ->
                    ItemCard(
                        item = item, 
                        onPlus = { onPlus(item) }, 
                        onMinus = { onMinus(item) }, 
                        onEdit = { onEdit(item) }, 
                        onDelete = { onDelete(item) }, 
                        onSell = { onSell(item) },
                        onSelect = {}
                    )
                }
            }
        }
    }
}

@Composable
private fun categoryIcon(category: String): ImageVector {
    return when (category) {
        "IC الصوت" -> Icons.Default.DeveloperBoard
        "IC TV" -> Icons.Default.Memory
        "المكثفات" -> Icons.Default.ElectricalServices
        "الدوائر الكاملة" -> Icons.Default.DeveloperBoard
        "IC فرتكال" -> Icons.Default.Tv
        "قطع سماعات" -> Icons.Default.Speaker
        "ريموتات" -> Icons.Default.SettingsRemote
        "رسيفرات" -> Icons.Default.Wifi
        "تلفزيونات" -> Icons.Default.Tv
        "عدسات دش" -> Icons.Default.SatelliteAlt
        "عدسات رقمية" -> Icons.Default.SatelliteAlt
        "كابلات" -> Icons.Default.Cable
        "أدابتر 12V" -> Icons.Default.Power
        "لفات سلاك دش" -> Icons.Default.Cable
        "أطباق دش" -> Icons.Default.SatelliteAlt
        "فلانشات طبق" -> Icons.Default.Category
        "إكسسوار دش" -> Icons.Default.Inventory2
        "بطاريات قلم 1.5V" -> Icons.Default.BatteryStd
        "سماعات" -> Icons.Default.Speaker
        else -> Icons.Default.Category
    }
}

@Composable
private fun ItemThumbnail(item: Item, modifier: Modifier = Modifier) {
    val path = item.imagePath
    Box(modifier = modifier.clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)), contentAlignment = Alignment.Center) {
        if (!path.isNullOrBlank() && File(path).exists()) {
            AsyncImage(model = File(path), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Icon(imageVector = categoryIcon(item.category), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun lowStockContainer(): Color = if (MaterialTheme.colorScheme.background.red < 0.2f) Color(0xFF3B1F1F) else Color(0xFFFFEBEE)

@Composable
private fun lowStockContent(): Color = if (MaterialTheme.colorScheme.background.red < 0.2f) Color(0xFFFF8A80) else Color(0xFFC44536)

@Composable
private fun ItemCard(item: Item, onPlus: () -> Unit, onMinus: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit, onSell: () -> Unit, isSelected: Boolean = false, onSelect: () -> Unit) {
    val s = LocalAppStrings.current
    val low = item.quantity <= item.minQuantity
    Card(
        onClick = { onSelect() },
        shape = RoundedCornerShape(14.dp), 
        colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else if (low) lowStockContainer() else MaterialTheme.colorScheme.surface), 
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), 
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    ItemThumbnail(item, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(item.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 2, modifier = Modifier.weight(1f))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Edit, contentDescription = s.edit, modifier = Modifier.size(16.dp)) }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Delete, contentDescription = s.delete, modifier = Modifier.size(16.dp)) }
                    if (isSelected) {
                        Checkbox(checked = true, onCheckedChange = { onSelect() })
                    }
                }
            }
            if (item.brand.isNotBlank() || item.subCategory.isNotBlank()) {
                val sub = listOfNotNull(item.subCategory.takeIf { it.isNotBlank() }?.let { displayLabel(it, s) }, item.brand.takeIf { it.isNotBlank() }?.let { displayLabel(it, s) }).joinToString(" · ")
                Text(sub, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
            }
            Spacer(Modifier.height(6.dp))
            Text("${item.quantity}", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = MaterialTheme.colorScheme.primary)
            if (low) {
                Text(if (item.quantity <= 0) s.outOfStock else s.almostOut, fontSize = 11.sp, color = lowStockContent(), fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onMinus, colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)), modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Remove, contentDescription = s.minus, modifier = Modifier.size(18.dp)) }
                IconButton(onClick = onPlus, colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White), modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Add, contentDescription = s.plus, modifier = Modifier.size(18.dp)) }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onSell, colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = Color.White), modifier = Modifier.size(36.dp)) { Icon(Icons.Default.ShoppingCart, contentDescription = "بيع", modifier = Modifier.size(18.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemEditorDialog(title: String, initial: Item, allowedCategories: List<String>, askMainSection: Boolean = false, onDismiss: () -> Unit, onSave: (Item) -> Unit) {
    val s = LocalAppStrings.current
    var name by remember { mutableStateOf(initial.name) }
    var category by remember { mutableStateOf(if (initial.category in allowedCategories) initial.category else allowedCategories.firstOrNull().orEmpty()) }
    var subCategory by remember { mutableStateOf(initial.subCategory) }
    var brand by remember { mutableStateOf(initial.brand) }
    var qty by remember { mutableStateOf(if (initial.id == 0L && initial.quantity == 0) "" else initial.quantity.toString()) }
    var minQty by remember { mutableStateOf(initial.minQuantity.toString()) }
    var priceMin by remember { mutableStateOf(initial.priceMin) }
    var priceMax by remember { mutableStateOf(initial.priceMax) }
    var notes by remember { mutableStateOf(initial.notes) }
    var imagePath by remember { mutableStateOf(initial.imagePath) }
    var error by remember { mutableStateOf<String?>(null) }
    var showImagePicker by remember { mutableStateOf(false) }

    var expandedCat by remember { mutableStateOf(false) }
    var expandedSub by remember { mutableStateOf(false) }
    var expandedBrand by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { 
            scope.launch {
                val newPath = ImageStorage.copyToAppStorage(context, it)
                if (newPath != null) {
                    // Delete old image if replacing
                    if (!imagePath.isNullOrBlank()) {
                        ImageStorage.delete(imagePath)
                    }
                    imagePath = newPath
                }
            }
        }
    }

    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val capturedUri = cameraImageUri
        if (success && capturedUri != null) {
            scope.launch {
                val newPath = ImageStorage.copyToAppStorage(context, capturedUri)
                if (newPath != null) {
                    // Delete old image if replacing
                    if (!imagePath.isNullOrBlank()) {
                        ImageStorage.delete(imagePath)
                    }
                    imagePath = newPath
                }
                
                // Cleanup temporary file
                try {
                    capturedUri.path?.let { path ->
                        File(path).delete()
                    }
                } catch (_: Exception) {}
            }
        }
    }


    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(title)
                IconButton(onClick = { showImagePicker = true }) {
                    Icon(Icons.Default.AddAPhoto, contentDescription = "Add Photo")
                }
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (imagePath.isNullOrBlank()) {
                    // handle empty case if needed
                } else {
                    Box(Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(8.dp))) {
                        AsyncImage(model = File(imagePath), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        IconButton(onClick = { imagePath = "" }, modifier = Modifier.align(Alignment.TopEnd).background(Color.Black.copy(alpha = 0.5f), CircleShape)) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(s.name) }, modifier = Modifier.fillMaxWidth(), singleLine = true)

                // --- Category Dropdown ---
                Box {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(s.category) },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = { IconButton(onClick = { expandedCat = true }) { Icon(Icons.Default.ArrowDropDown, null) } }
                    )
                    DropdownMenu(expanded = expandedCat, onDismissRequest = { expandedCat = false }, modifier = Modifier.fillMaxWidth(0.9f)) {
                        allowedCategories.forEach { cat ->
                            DropdownMenuItem(text = { Text(cat) }, onClick = { category = cat; expandedCat = false })
                        }
                    }
                }

                // --- SubCategory Dropdown (Dynamic based on category) ---
                val subOptions = when (category) {
                    "ريموتات" -> Categories.remoteGroups
                    "عدسات رقمية" -> Categories.digitalLensBrands
                    "عدسات دش" -> Categories.dishLensTypes
                    else -> emptyList()
                }
                Box {
                    OutlinedTextField(
                        value = subCategory,
                        onValueChange = { subCategory = it },
                        label = { Text(s.subCategory) },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = { 
                            if (subOptions.isNotEmpty()) {
                                IconButton(onClick = { expandedSub = true }) { Icon(Icons.Default.ArrowDropDown, null) }
                            }
                        }
                    )
                    DropdownMenu(expanded = expandedSub, onDismissRequest = { expandedSub = false }, modifier = Modifier.fillMaxWidth(0.9f)) {
                        subOptions.forEach { sub ->
                            DropdownMenuItem(text = { Text(sub) }, onClick = { subCategory = sub; expandedSub = false })
                        }
                    }
                }

                // --- Brand Dropdown (Dynamic based on subCategory) ---
                val brandOptions = when (subCategory) {
                    "HD" -> Categories.remoteHdBrands
                    "SD" -> Categories.remoteSdTypes
                    "تلفزيون" -> Categories.remoteTvBrands
                    else -> if (category == "تلفزيونات") Categories.brandsTv else emptyList()
                }
                Box {
                    OutlinedTextField(
                        value = brand,
                        onValueChange = { brand = it },
                        label = { Text(s.brand) },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = { 
                            if (brandOptions.isNotEmpty()) {
                                IconButton(onClick = { expandedBrand = true }) { Icon(Icons.Default.ArrowDropDown, null) }
                            }
                        }
                    )
                    DropdownMenu(expanded = expandedBrand, onDismissRequest = { expandedBrand = false }, modifier = Modifier.fillMaxWidth(0.9f)) {
                        brandOptions.forEach { br ->
                            DropdownMenuItem(text = { Text(br) }, onClick = { brand = br; expandedBrand = false })
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = qty, onValueChange = { if (it.all { c -> c.isDigit() }) qty = it }, label = { Text(s.quantity) }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                    OutlinedTextField(value = minQty, onValueChange = { if (it.all { c -> c.isDigit() }) minQty = it }, label = { Text(s.minQuantity) }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = priceMin, onValueChange = { priceMin = it }, label = { Text(s.priceMin) }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = priceMax, onValueChange = { priceMax = it }, label = { Text(s.priceMax) }, modifier = Modifier.weight(1f), singleLine = true)
                }
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text(s.notes) }, modifier = Modifier.fillMaxWidth())
                if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank()) { error = s.nameRequired }
                else if (qty.isBlank()) { error = s.qtyRequired }
                else {
                    onSave(initial.copy(name = name, category = category, subCategory = subCategory, brand = brand, quantity = qty.toIntOrNull() ?: 0, minQuantity = minQty.toIntOrNull() ?: 1, priceMin = priceMin, priceMax = priceMax, notes = notes, imagePath = imagePath))
                }
            }) { Text(s.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } }
    )

    if (showImagePicker) {
        AlertDialog(
            onDismissRequest = { showImagePicker = false },
            title = { Text("إضافة صورة") },
            text = { Text("كيف تريد إضافة صورة العنصر؟") },
            confirmButton = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { 
                        showImagePicker = false
                        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) 
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("من الاستوديو (التخزين)")
                    }
                    Button(onClick = {
                        showImagePicker = false
                        val photoFile = File(context.cacheDir, "camera_${UUID.randomUUID()}.jpg")
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
                        cameraImageUri = uri
                        cameraLauncher.launch(uri)
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("بالكاميرا (مباشر)")
                    }
                }
            },
            dismissButton = { TextButton(onClick = { showImagePicker = false }) { Text("إلغاء") } }
        )
    }
}

@Composable
fun SaleDialog(item: Item, onDismiss: () -> Unit, onConfirm: (Int, String, String, Double) -> Unit) {
    val s = LocalAppStrings.current
    var qty by remember { mutableStateOf("1") }
    var custName by remember { mutableStateOf("") }
    var custPhone by remember { mutableStateOf("") }
    
    val isFixedPrice = item.priceMin == item.priceMax
    val defaultPrice = if (isFixedPrice) item.priceMax.toDoubleOrNull() ?: 0.0 else item.priceMax.toDoubleOrNull() ?: 0.0
    var salePrice by remember { mutableStateOf(defaultPrice.toString()) }
    
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("عملية بيع: ${item.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("الكمية المتوفرة: ${item.quantity}", fontSize = 14.sp)
                OutlinedTextField(value = qty, onValueChange = { if (it.all { c -> c.isDigit() }) qty = it }, label = { Text("الكمية المباعة") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                
                if (!isFixedPrice) {
                    OutlinedTextField(value = salePrice, onValueChange = { salePrice = it }, label = { Text("سعر البيع (للقطعة)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                } else {
                    Text("السعر الثابت: ${item.priceMax} ج.م", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                
                OutlinedTextField(value = custName, onValueChange = { custName = it }, label = { Text("اسم الزبون") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = custPhone, onValueChange = { custPhone = it }, label = { Text("رقم الهاتف") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true)
                if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val q = qty.toIntOrNull() ?: 0
                val p = salePrice.toDoubleOrNull() ?: if (isFixedPrice) item.priceMax.toDoubleOrNull() ?: 0.0 else 0.0
                if (q <= 0) { error = "الكمية يجب أن تكون أكبر من 0" }
                else if (q > item.quantity) { error = "الكمية المطلوبة أكبر من المتوفر" }
                else if (!isFixedPrice && p <= 0.0) { error = "السعر يجب أن يكون أكبر من 0" }
                else { onConfirm(q, custName, custPhone, p) }
            }) { Text("تأكيد البيع") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } }
    )
}

enum class FilterMode { DAILY, MONTHLY }

@Composable
private fun SaleLogPage(logs: List<com.local.fatateer.data.SaleLog>, onBack: () -> Unit, onDeleteLog: (com.local.fatateer.data.SaleLog) -> Unit) {

    val s = LocalAppStrings.current
    var filterMode by remember { mutableStateOf(FilterMode.DAILY) }
    var showDetails by remember { mutableStateOf<com.local.fatateer.data.SaleLog?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
            Text(s.salesLogTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(48.dp))
        }

        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.Center) {
            SingleChoiceSegmentedButtonRow {
                SegmentedButton(selected = filterMode == FilterMode.DAILY, onClick = { filterMode = FilterMode.DAILY }, shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)) {
                    Text("يومي")
                }
                SegmentedButton(selected = filterMode == FilterMode.MONTHLY, onClick = { filterMode = FilterMode.MONTHLY }, shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)) {
                    Text("شهري")
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            if (logs.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(s.noSalesLogs) }
            } else {
                val dateFormat = remember { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US) }
                val monthFormat = remember { java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.US) }
                val grouped = if (filterMode == FilterMode.DAILY) logs.groupBy { dateFormat.format(java.util.Date(it.timestamp)) } else logs.groupBy { monthFormat.format(java.util.Date(it.timestamp)) }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    grouped.forEach { (period, periodLogs) ->
                        val total = periodLogs.sumOf { 
                            val p = it.price.toDoubleOrNull() ?: 0.0
                            p * it.quantity 
                        }
                        item {
                            TimelineGroup(
                                period = period, 
                                logs = periodLogs, 
                                total = total, 
                                onLogClick = { log -> 
                                    showDetails = log 
                                },
                                onDeleteLog = onDeleteLog
                            )
                        }
                    }
                }
            }
        }
    }

    showDetails?.let { log ->
        SaleDetailDialog(log = log, onDismiss = { showDetails = null })
    }
}

@Composable
private fun TimelineGroup(
    period: String, 
    logs: List<com.local.fatateer.data.SaleLog>, 
    total: Double, 
    onLogClick: (com.local.fatateer.data.SaleLog) -> Unit,
    onDeleteLog: (com.local.fatateer.data.SaleLog) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
            Box(Modifier.size(12.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(period, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Column(modifier = Modifier.padding(start = 10.dp)) {
            logs.forEach { log ->
                Card(
                    onClick = { onLogClick(log) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${log.itemName} (${log.quantity})", fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        Text("${log.price} ج.م", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = { onDeleteLog(log) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("إجمالي الفترة:", fontWeight = FontWeight.Bold)
                    Text("${total} ج.م", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
    }
}

@Composable
private fun SaleDetailDialog(log: com.local.fatateer.data.SaleLog, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تفاصيل عملية البيع") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DetailRow("الصنف", log.itemName)
                DetailRow("الكمية", "${log.quantity}")
                DetailRow("السعر الإجمالي", "${log.price} ج.م")
                DetailRow("الزبون", log.customerName)
                DetailRow("الهاتف", log.customerPhone)
                DetailRow("التاريخ", java.text.DateFormat.getDateTimeInstance().format(java.util.Date(log.timestamp)))
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("إغلاق") } }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Bold)
    }
}
