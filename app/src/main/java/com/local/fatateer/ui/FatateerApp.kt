@Composable
fun TopSellingScreen(state: StockUiState, onBack: () -> Unit) {
    val s = LocalAppStrings.current
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.back)
            }
            Text(s.topSellingItems, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.topSellingItems) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${item.rank}.", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(8.dp))
                            Text(item.itemName, fontWeight = FontWeight.Medium)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("${item.totalQuantity} ${s.piecesSold}", color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
    }
}


@Composable
fun OrderRequestsScreen(state: StockUiState, vm: StockViewModel, onBack: () -> Unit) {
    val s = LocalAppStrings.current
    var showAddRequest by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.back)
            }
            Text(s.orderRequests, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            IconButton(onClick = { showAddRequest = true }) {
                Icon(Icons.Default.Add, contentDescription = s.add)
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.orderRequests) { request ->
                OrderRequestCard(request = request, onDelete = { vm.deleteOrderRequest(request) })
            }
        }
    }

    if (showAddRequest) {
        AddOrderRequestDialog(
            onDismiss = { showAddRequest = false },
            onSave = { request ->
                vm.saveOrderRequest(request)
                showAddRequest = false
            }
        )
    }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                state = state,
                modifier = Modifier.fillMaxSize().padding(16.dp),
                onOpenSpare = { goToTab(MainTab.SPARE) },
                onOpenSales = { goToTab(MainTab.SALES) },
                onOpenLowStock = { showLowStock = true },
                onOpenTopSelling = { goToTopSelling() },
                onOpenOrderRequests = { goToOrderRequests() }
            )
        }
        composable("top_selling") {
            TopSellingScreen(state = state, onBack = { navController.popBackStack() })
        }
        composable("order_requests") {
            OrderRequestsScreen(state = state, vm = vm, onBack = { navController.popBackStack() })
        }
    }
}


@Composable
private fun OrderRequestCard(request: OrderRequest, onDelete: () -> Unit) {
    val s = LocalAppStrings.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(request.itemName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            request.itemImagePath?.let {
                AsyncImage(
                    model = it,
                    contentDescription = s.itemImage,
                    modifier = Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(8.dp))
            }
            Text("${s.deviceName}: ${request.deviceName}")
            Text("${s.customerName}: ${request.customerName}")
            Text("${s.customerPhone}: ${request.customerPhone}")
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { onDelete() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(s.markAsReceived)
            }
        }
    }
}


@Composable
private fun AddOrderRequestDialog(onDismiss: () -> Unit, onSave: (OrderRequest) -> Unit) {
    val s = LocalAppStrings.current
    var itemName by remember { mutableStateOf("") }
    var deviceName by remember { mutableStateOf("") }
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var imagePath by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let {
            scope.launch {
                val newPath = ImageStorage.copyToAppStorage(context, it)
                if (newPath != null) {
                    imagePath = newPath
                }
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val capturedUri = cameraImageUri
        if (success && capturedUri != null) {
            scope.launch {
                val newPath = ImageStorage.copyToAppStorage(context, capturedUri)
                if (newPath != null) {
                    imagePath = newPath
                }
            }
        }
    }

    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.addOrderRequest) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = { Text(s.itemName) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = deviceName,
                    onValueChange = { deviceName = it },
                    label = { Text(s.deviceName) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text(s.customerName) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = customerPhone,
                    onValueChange = { customerPhone = it },
                    label = { Text(s.customerPhone) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(onClick = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                        Text(s.gallery)
                    }
                    Button(onClick = {
                        cameraImageUri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            File(context.cacheDir, "temp_camera_image.jpg")
                        )
                        cameraLauncher.launch(cameraImageUri!!)
                    }) {
                        Text(s.camera)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (itemName.isNotBlank() && deviceName.isNotBlank() && customerName.isNotBlank() && customerPhone.isNotBlank()) {
                        onSave(OrderRequest(
                            itemName = itemName,
                            itemImagePath = imagePath,
                            deviceName = deviceName,
                            customerName = customerName,
                            customerPhone = customerPhone
                        ))
                    }
                }
            ) {
                Text(s.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(s.cancel)
            }
        }
    )
}