package com.local.fatateer.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult

import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.local.fatateer.data.Categories
import com.local.fatateer.data.ImageStorage
import com.local.fatateer.data.Item
import com.local.fatateer.ui.locale.AppLanguage
import com.local.fatateer.ui.locale.AppStrings
import com.local.fatateer.ui.locale.LocalAppStrings
import com.local.fatateer.ui.locale.LocalSettingsController
import com.local.fatateer.ui.locale.ThemeMode
import com.local.fatateer.ui.locale.displayLabel
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FatateerApp(
    isDark: Boolean = false,
    vm: StockViewModel = viewModel()
) {
    val settings = LocalSettingsController.current
    val s = LocalAppStrings.current
    val state by vm.state.collectAsStateWithLifecycle()
    var editor by remember { mutableStateOf<Item?>(null) }
    var showNew by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<Item?>(null) }
    var itemToSell by remember { mutableStateOf<Item?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var showLowStock by remember { mutableStateOf(false) }
    var showLog by remember { mutableStateOf(false) }

    // التحكم في زر الرجوع
    BackHandler(enabled = true) {
        when {
            showSettings -> showSettings = false
            showLowStock -> showLowStock = false
            editor != null -> editor = null
            showNew -> showNew = false
            else -> {
                if (currentTab != MainTab.HOME) {
                    goToTab(MainTab.HOME)
                }
            }
        }
    }

    var drawerOpen by remember { mutableStateOf(false) }
    val drawerScope = rememberCoroutineScope()

    // تنقل بالسحب بين التبويبات الثلاثة (زي فيسبوك)
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

    CompositionLocalProvider(LocalLayoutDirection provides settings.language.layoutDirection) {
        ModalNavigationDrawer(
            drawerState = rememberDrawerState(initialValue = DrawerValue.Closed),
            gesturesEnabled = true,
            drawerContent = {
                ModalDrawerSheet {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "القائمة الرئيسية",
                            modifier = Modifier.padding(bottom = 16.dp, start = 12.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        
                        NavigationDrawerItem(
                            label = { Text(s.home) },
                            selected = currentTab == MainTab.HOME,
                            onClick = { 
                                goToTab(MainTab.HOME)
                                drawerScope.launch { // Close drawer (will need to manage state) } 
                            },
                            icon = { Icon(Icons.Default.Home, null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        NavigationDrawerItem(
                            label = { Text(s.spareParts) },
                            selected = currentTab == MainTab.SPARE,
                            onClick = { goToTab(MainTab.SPARE) },
                            icon = { Icon(Icons.Default.Build, null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        NavigationDrawerItem(
                            label = { Text(s.sales) },
                            selected = currentTab == MainTab.SALES,
                            onClick = { goToTab(MainTab.SALES) },
                            icon = { Icon(Icons.Default.ShoppingCart, null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(Modifier.height(20.dp))
                        Divider()
                        Spacer(Modifier.height(20.dp))
                        
                        NavigationDrawerItem(
                            label = { Text(s.settingsTitle) },
                            selected = showSettings,
                            onClick = { showSettings = true },
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
                                IconButton(onClick = { drawerScope.launch { /*’ Open Drawer logic handled by ModalNavigationDrawer state’ */ } }) {
                                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                                }
                            } else {
                                IconButton(onClick = { 
                                    if(showSettings) showSettings = false 
                                    if(showLowStock) showLowStock = false 
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.cancel)
                                }
                            }
                        },
                        actions = {
                            if (!showSettings && !showLowStock) {
                                if (currentTab != MainTab.HOME) {
                                    var menuOpen by remember { mutableStateOf(false) }
                                    IconButton(onClick = { menuOpen = true }) {
                                        Icon(Icons.Default.Menu, contentDescription = s.categories)
                                    }
                                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                        DropdownMenuItem(
                                            text = { Text(s.allCategories, fontWeight = if (state.selectedCategory == null) FontWeight.Bold else FontWeight.Normal) },
                                            onClick = { vm.setCategory(null); menuOpen = false }
                                        )
                                        chipCats.forEach { cat ->
                                            DropdownMenuItem(
                                                text = { Text(displayLabel(cat, s), fontWeight = if (state.selectedCategory == cat) FontWeight.Bold else FontWeight.Normal) },
                                                onClick = { vm.setCategory(cat); menuOpen = false }
                                            )
                                        }
                                    }
                                }
                            }
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
                            NavigationBarItem(
                                selected = currentTab == MainTab.HOME,
                                onClick = { goToTab(MainTab.HOME) },
                                icon = { Icon(Icons.Default.Home, null) },
                                label = { Text(s.home) }
                            )
                            NavigationBarItem(
                                selected = currentTab == MainTab.SPARE,
                                onClick = { goToTab(MainTab.SPARE) },
                                icon = { Icon(Icons.Default.Build, null) },
                                label = { Text(s.spareParts) }
                            )
                            NavigationBarItem(
                                selected = currentTab == MainTab.SALES,
                                onClick = { goToTab(MainTab.SALES) },
                                icon = { Icon(Icons.Default.ShoppingCart, null) },
                                label = { Text(s.sales) }
                            )
                        }
                    }
                },
                floatingActionButton = {
                    if (!showSettings && !showLowStock && currentTab != MainTab.HOME) {
                        FloatingActionButton(onClick = { showNew = true }) {
                            Icon(Icons.Default.Add, contentDescription = s.addItem)
                        }
                    }
                }
            ) { padding ->
                // Rest of the content (Pager/Settings/LowStock) as before
                when {
                    showSettings -> SettingsScreen(
                        modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                        onOpenLog = { showLog = true }
                    )
                    showLowStock -> LowStockScreen(
                        items = state.neededItems,
                        onPlus = vm::plus,
                        onMinus = vm::minus,
                        onEdit = { editor = it },
                        onDelete = { toDelete = it },
                        modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                    else -> HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize().padding(padding)
                    ) { page ->
                        when (MainTab.values()[page]) {
                            MainTab.HOME -> HomeScreen(
                                state = state,
                                modifier = Modifier.fillMaxSize().padding(16.dp),
                                onOpenSpare = { goToTab(MainTab.SPARE) },
                                onOpenSales = { goToTab(MainTab.SALES) },
                                onOpenLowStock = { showLowStock = true }
                            )
                            MainTab.SPARE, MainTab.SALES -> InventoryScreen(
                                state = state,
                                chipCats = if (MainTab.values()[page] == MainTab.SPARE) Categories.spareParts else Categories.sales,
                                onQuery = vm::setQuery,
                                onCategory = vm::setCategory,
                                onPlus = vm::plus,
                                onMinus = vm::minus,
                                onEdit = { editor = it },
                                onDelete = { toDelete = it },
                                onSell = { itemToSell = it },
                                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

        if (showNew) {
            val startCat = state.selectedCategory ?: defaultCategory
            ItemEditorDialog(
                title = s.addItem,
                initial = Item(
                    name = "",
                    category = startCat,
                    subCategory = "",
                    brand = "",
                    quantity = 0,
                    minQuantity = 1
                ),
                allowedCategories = Categories.all,
                askMainSection = true,
                onDismiss = { showNew = false },
                onSave = {
                    vm.save(it)
                    showNew = false
                }
            )
        }
        editor?.let { current ->
            ItemEditorDialog(
                title = s.edit,
                initial = current,
                allowedCategories = Categories.all,
                askMainSection = true,
                onDismiss = { editor = null },
                onSave = {
                    vm.save(it)
                    editor = null
                }
            )
        }
        toDelete?.let { item ->
            AlertDialog(
                onDismissRequest = { toDelete = null },
                title = { Text(s.deleteConfirmTitle) },
                text = { Text(s.deleteConfirmBody.replace("%s", item.name)) },
                confirmButton = {
                    TextButton(onClick = {
                        vm.delete(item)
                        toDelete = null
                    }) { Text(s.delete) }
                },
                dismissButton = {
                    TextButton(onClick = { toDelete = null }) { Text(s.cancel) }
                }
            )
        }
    }
}

@Composable
private fun SettingsScreen(
    modifier: Modifier = Modifier,
    onOpenLog: () -> Unit
) {
    val settings = LocalSettingsController.current
    val s = LocalAppStrings.current

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Theme
        Text(s.themeTitle, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(8.dp)) {
                ThemeModeRow(
                    label = s.themeSystem,
                    icon = Icons.Default.BrightnessAuto,
                    selected = settings.themeMode == ThemeMode.SYSTEM,
                    onClick = { settings.setThemeMode(ThemeMode.SYSTEM) }
                )
                ThemeModeRow(
                    label = s.themeDark,
                    icon = Icons.Default.DarkMode,
                    selected = settings.themeMode == ThemeMode.DARK,
                    onClick = { settings.setThemeMode(ThemeMode.DARK) }
                )
                ThemeModeRow(
                    label = s.themeLight,
                    icon = Icons.Default.LightMode,
                    selected = settings.themeMode == ThemeMode.LIGHT,
                    onClick = { settings.setThemeMode(ThemeMode.LIGHT) }
                )
            }
        }

        // Language
        Text(s.languageTitle, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(8.dp)) {
                ThemeModeRow(
                    label = s.languageArabic,
                    icon = Icons.Default.Language,
                    selected = settings.language == AppLanguage.ARABIC,
                    onClick = { settings.setLanguage(AppLanguage.ARABIC) }
                )
                ThemeModeRow(
                    label = s.languageEnglish,
                    icon = Icons.Default.Language,
                    selected = settings.language == AppLanguage.ENGLISH,
                    onClick = { settings.setLanguage(AppLanguage.ENGLISH) }
                )
            }
        }
        
        // Sale Log
        Text("إدارة المبيعات", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ThemeModeRow(
                label = "سجل المبيعات",
                icon = Icons.Default.ShoppingCart,
                selected = false,
                onClick = onOpenLog
            )
        }
    }
}

@Composable
private fun ThemeModeRow(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(label, modifier = Modifier.weight(1f), fontSize = 15.sp)
        RadioButton(selected = selected, onClick = onClick)
    }
}

@Composable
private fun HomeScreen(
    state: StockUiState,
    modifier: Modifier = Modifier,
    onOpenSpare: () -> Unit,
    onOpenSales: () -> Unit,
    onOpenLowStock: () -> Unit
) {
    val s = LocalAppStrings.current
    Column(
        modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(s.shopSummary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniStat(s.spareParts, "${state.spareCount}", Modifier.weight(1f), onClick = onOpenSpare)
            MiniStat(s.sales, "${state.salesCount}", Modifier.weight(1f), onClick = onOpenSales)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniStat(s.totalItems, "${state.totalQty}", Modifier.weight(1f))
            MiniStat(
                s.lowStock,
                "${state.neededCount}",
                Modifier.weight(1f),
                alert = state.neededCount > 0,
                onClick = if (state.neededCount > 0) onOpenLowStock else null
            )
        }
        if (state.neededCount > 0) {
            Text(
                s.lowStockHint.replace("%d", state.neededCount.toString()),
                fontSize = 13.sp,
                color = lowStockContent()
            )
        } else {
            Text(
                s.noLowStock,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun MiniStat(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    alert: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Card(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (alert) lowStockContainer() else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                title,
                fontSize = 12.sp,
                color = if (alert) lowStockContent()
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                value,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = if (alert) lowStockContent() else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun InventoryScreen(
    state: StockUiState,
    chipCats: List<String>,
    onQuery: (String) -> Unit,
    onCategory: (String?) -> Unit,
    onPlus: (Item) -> Unit,
    onMinus: (Item) -> Unit,
    onEdit: (Item) -> Unit,
    onDelete: (Item) -> Unit,
    onSell: (Item) -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalAppStrings.current
    Column(modifier) {
        if (state.selectedCategory == null && state.query.isBlank()) {
            Text(s.categories, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                s.lowCountLabel.replace("%d", state.filtered.count { it.quantity <= it.minQuantity }.toString()),
                fontWeight = FontWeight.SemiBold,
                color = if (state.filtered.any { it.quantity <= it.minQuantity })
                    lowStockContent() else MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(12.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 88.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                gridItems(chipCats, key = { it }) { cat ->
                    val inCat = state.filtered.filter { it.category == cat }
                    val needed = inCat.count { it.quantity <= it.minQuantity }
                    Card(
                        onClick = { onCategory(cat) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (needed > 0) lowStockContainer()
                            else MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.height(128.dp)
                    ) {
                        Column(
                            Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = categoryIcon(cat),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                displayLabel(cat, s),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                maxLines = 2,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        } else {
            Text(
                state.selectedCategory?.let { displayLabel(it, s) } ?: s.search,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.query,
                onValueChange = onQuery,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(s.search) },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { onCategory(null); onQuery("") }) {
                Text(s.backToOverview)
            }
            Spacer(Modifier.height(8.dp))
            if (state.filtered.isEmpty()) {
                Text(
                    s.emptyItems,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 88.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    val flat = state.filtered
                    gridItems(flat, key = { it.id }) { item ->
                        ItemCard(
                            item = item,
                            onPlus = { onPlus(item) },
                            onMinus = { onMinus(item) },
                            onEdit = { onEdit(item) },
                            onDelete = { onDelete(item) },
                            onSell = { onSell(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LowStockScreen(
    items: List<Item>,
    onPlus: (Item) -> Unit,
    onMinus: (Item) -> Unit,
    onEdit: (Item) -> Unit,
    onDelete: (Item) -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalAppStrings.current
    Column(modifier) {
        if (items.isEmpty()) {
            Text(
                s.noLowStockItems,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 88.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                gridItems(items, key = { it.id }) { item ->
                    ItemCard(
                        item = item,
                        onPlus = { onPlus(item) },
                        onMinus = { onMinus(item) },
                        onEdit = { onEdit(item) },
                        onDelete = { onDelete(item) }
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
        else -> Icons.Default.Category
    }
}

@Composable
private fun ItemThumbnail(item: Item, modifier: Modifier = Modifier) {
    val path = item.imagePath
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
        contentAlignment = Alignment.Center
    ) {
        if (!path.isNullOrBlank() && File(path).exists()) {
            AsyncImage(
                model = File(path),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = categoryIcon(item.category),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun lowStockContainer(): Color =
    if (MaterialTheme.colorScheme.background.red < 0.2f)
        Color(0xFF3B1F1F)
    else
        Color(0xFFFFEBEE)

@Composable
private fun lowStockContent(): Color =
    if (MaterialTheme.colorScheme.background.red < 0.2f)
        Color(0xFFFF8A80)
    else
        Color(0xFFC44536)

@Composable
private fun ItemCard(
    item: Item,
    onPlus: () -> Unit,
    onMinus: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSell: () -> Unit
) {
    val s = LocalAppStrings.current
    val low = item.quantity <= item.minQuantity
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (low) lowStockContainer() else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    ItemThumbnail(item, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        item.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        maxLines = 2,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = s.edit, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = s.delete, modifier = Modifier.size(16.dp))
                    }
                }
            }
            if (item.brand.isNotBlank() || item.subCategory.isNotBlank()) {
                val sub = listOfNotNull(
                    item.subCategory.takeIf { it.isNotBlank() }?.let { displayLabel(it, s) },
                    item.brand.takeIf { it.isNotBlank() }?.let { displayLabel(it, s) }
                ).joinToString(" · ")
                Text(
                    sub,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "${item.quantity}",
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.primary
            )
            if (low) {
                Text(
                    if (item.quantity <= 0) s.outOfStock else s.almostOut,
                    fontSize = 11.sp,
                    color = lowStockContent(),
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onMinus,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = s.minus, modifier = Modifier.size(18.dp))
                }
                IconButton(
                    onClick = onPlus,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = s.plus, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.weight(1f))
                IconButton(
                    onClick = onSell,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = "بيع", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemEditorDialog(
    title: String,
    initial: Item,
    allowedCategories: List<String>,
    askMainSection: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (Item) -> Unit
) {
    val s = LocalAppStrings.current
    val initialMain = if (initial.category in Categories.sales) "البيع" else "قطع الغيار"
    var mainSection by remember { mutableStateOf(initialMain) }
    var name by remember { mutableStateOf(initial.name) }
    var category by remember {
        mutableStateOf(
            if (initial.category in allowedCategories) initial.category
            else allowedCategories.firstOrNull().orEmpty()
        )
    }
    var subCategory by remember { mutableStateOf(initial.subCategory) }
    var brand by remember { mutableStateOf(initial.brand) }
    var qty by remember {
        mutableStateOf(if (initial.id == 0L && initial.quantity == 0) "" else initial.quantity.toString())
    }
    var minQty by remember { mutableStateOf(initial.minQuantity.toString()) }
    var priceMin by remember { mutableStateOf(initial.priceMin) }
    var priceMax by remember { mutableStateOf(initial.priceMax) }
    var notes by remember { mutableStateOf(initial.notes) }
    var error by remember { mutableStateOf<String?>(null) }
    var mainExpanded by remember { mutableStateOf(false) }
    var catExpanded by remember { mutableStateOf(false) }
    var subExpanded by remember { mutableStateOf(false) }
    var brandExpanded by remember { mutableStateOf(false) }

    // صورة المنتج — اختيارية، لو مفيش صورة بتتعرض أيقونة التصنيف بدلًا منها
    val context = LocalContext.current
    val imageScope = rememberCoroutineScope()
    val initialImagePath = initial.imagePath
    var imagePath by remember { mutableStateOf(initial.imagePath) }
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            imageScope.launch {
                val saved = ImageStorage.copyToAppStorage(context, uri)
                if (saved != null) imagePath = saved
            }
        }
    }

    val sectionCats = if (askMainSection) {
        if (mainSection == "البيع") Categories.sales else Categories.spareParts
    } else allowedCategories

    val isRemote = category == "ريموتات"
    val isTv = category == "تلفزيونات"
    val isDishLens = category == "عدسات دش"
    val isDigitalLens = category == "عدسات رقمية"

    val brandOptions: List<String> = when {
        isRemote && subCategory == "HD" -> Categories.remoteHdBrands
        isRemote && subCategory == "SD" -> Categories.remoteSdTypes
        isRemote && subCategory == "تلفزيون" -> Categories.remoteTvBrands
        isTv -> Categories.brandsTv
        isDishLens -> Categories.dishLensTypes
        isDigitalLens -> Categories.digitalLensBrands
        else -> emptyList()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                        contentAlignment = Alignment.Center
                    ) {
                        val path = imagePath
                        if (!path.isNullOrBlank() && File(path).exists()) {
                            AsyncImage(
                                model = File(path),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                Icons.Default.AddAPhoto,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        TextButton(onClick = {
                            pickImageLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }) {
                            Text(if (imagePath.isNullOrBlank()) s.addPhoto else s.changePhoto)
                        }
                        if (!imagePath.isNullOrBlank()) {
                            TextButton(onClick = { imagePath = null }) {
                                Text(s.removePhoto, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
                OutlinedTextField(
                    name, { name = it },
                    label = { Text(s.name) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (askMainSection) {
                    ExposedDropdownMenuBox(
                        expanded = mainExpanded,
                        onExpandedChange = { mainExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = displayLabel(mainSection, s),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(s.mainSection) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(mainExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = mainExpanded,
                            onDismissRequest = { mainExpanded = false }
                        ) {
                            listOf("قطع الغيار", "البيع").forEach { m ->
                                DropdownMenuItem(
                                    text = { Text(displayLabel(m, s)) },
                                    onClick = {
                                        mainSection = m
                                        mainExpanded = false
                                        val list = if (m == "البيع") Categories.sales else Categories.spareParts
                                        if (category !in list) {
                                            category = list.first()
                                            subCategory = ""
                                            brand = ""
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                ExposedDropdownMenuBox(
                    expanded = catExpanded,
                    onExpandedChange = { catExpanded = it }
                ) {
                    OutlinedTextField(
                        value = displayLabel(category, s),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(s.section) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(catExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = catExpanded,
                        onDismissRequest = { catExpanded = false }
                    ) {
                        sectionCats.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(displayLabel(c, s)) },
                                onClick = {
                                    category = c
                                    catExpanded = false
                                    subCategory = ""
                                    brand = ""
                                }
                            )
                        }
                    }
                }
                if (isRemote) {
                    ExposedDropdownMenuBox(
                        expanded = subExpanded,
                        onExpandedChange = { subExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = if (subCategory.isBlank()) "" else displayLabel(subCategory, s),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(s.remoteType) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(subExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = subExpanded,
                            onDismissRequest = { subExpanded = false }
                        ) {
                            Categories.remoteGroups.forEach { g ->
                                DropdownMenuItem(
                                    text = { Text(displayLabel(g, s)) },
                                    onClick = {
                                        subCategory = g
                                        subExpanded = false
                                        brand = ""
                                    }
                                )
                            }
                        }
                    }
                }
                if (brandOptions.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = brandExpanded,
                        onExpandedChange = { brandExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = if (brand.isBlank()) "" else displayLabel(brand, s),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(s.brandOrType) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(brandExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = brandExpanded,
                            onDismissRequest = { brandExpanded = false }
                        ) {
                            brandOptions.forEach { b ->
                                DropdownMenuItem(
                                    text = { Text(displayLabel(b, s)) },
                                    onClick = {
                                        brand = b
                                        brandExpanded = false
                                        if (name.isBlank()) name = b
                                    }
                                )
                            }
                        }
                    }
                }
                OutlinedTextField(
                    qty, { qty = it },
                    label = { Text(s.qtyInShop) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    minQty, { minQty = it },
                    label = { Text(s.reorderLevel) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        priceMin, { priceMin = it },
                        label = { Text("السعر من") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        priceMax, { priceMax = it },
                        label = { Text("السعر إلى") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    notes, { notes = it },
                    label = { Text(s.note) },
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val q = qty.trim().ifEmpty { "0" }.toIntOrNull()
                val m = minQty.trim().ifEmpty { "0" }.toIntOrNull()
                when {
                    name.isBlank() -> error = s.errName
                    category.isBlank() -> error = s.errSection
                    isRemote && subCategory.isBlank() -> error = s.errRemoteType
                    q == null || q < 0 -> error = s.errQty
                    m == null || m < 0 -> error = s.errMinQty
                    else -> {
                        if (!initialImagePath.isNullOrBlank() && initialImagePath != imagePath) {
                            ImageStorage.delete(initialImagePath)
                        }
                        onSave(
                            initial.copy(
                                name = name.trim(),
                                category = category,
                                subCategory = if (isRemote) subCategory else "",
                                brand = brand.trim(),
                                quantity = q,
                                minQuantity = m,
                                notes = notes.trim(),
                                imagePath = imagePath,
                                priceMin = priceMin.trim(),
                                priceMax = priceMax.trim()
                            )
                        )
                    }
                }
            }) { Text(s.save) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(s.cancel) }
        }
    )
}
