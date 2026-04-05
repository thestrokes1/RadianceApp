package com.cristianv.radianceapp.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.cristianv.radianceapp.R
import com.cristianv.radianceapp.model.Order
import com.cristianv.radianceapp.model.Product
import com.cristianv.radianceapp.viewmodel.AdminViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    navController: NavController,
    adminViewModel: AdminViewModel = viewModel()
) {
    val products by adminViewModel.products.collectAsState()
    val isLoading by adminViewModel.isLoading.collectAsState()
    val errorMessage by adminViewModel.errorMessage.collectAsState()
    val successMessage by adminViewModel.successMessage.collectAsState()
    val orders by adminViewModel.orders.collectAsState()
    val isLoadingOrders by adminViewModel.isLoadingOrders.collectAsState()
    val isUploadingImage by adminViewModel.isUploadingImage.collectAsState()

    var selectedTabIndex by remember { mutableStateOf(0) }

    var isAddFormExpanded by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newBrand by remember { mutableStateOf("") }
    var newPrice by remember { mutableStateOf("") }
    var newCategory by remember { mutableStateOf("") }
    var newImageUrl by remember { mutableStateOf("") }
    var newDescription by remember { mutableStateOf("") }

    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var editName by remember { mutableStateOf("") }
    var editBrand by remember { mutableStateOf("") }
    var editPrice by remember { mutableStateOf("") }
    var editOriginalPrice by remember { mutableStateOf("") }
    var editCategory by remember { mutableStateOf("") }
    var editImageUrl by remember { mutableStateOf("") }
    var editDescription by remember { mutableStateOf("") }
    var editIsNew by remember { mutableStateOf(false) }
    var editSizes by remember { mutableStateOf("") }
    var editColors by remember { mutableStateOf("") }

    LaunchedEffect(editingProduct) {
        editingProduct?.let { p ->
            editName = p.name
            editBrand = p.brand
            editPrice = p.price.toString()
            editOriginalPrice = p.originalPrice?.toString() ?: ""
            editCategory = p.category
            editImageUrl = p.imageUrl
            editDescription = p.description
            editIsNew = p.isNew
            editSizes = p.sizes.joinToString(", ")
            editColors = p.colors.joinToString(", ")
        }
    }

    var selectedOrder by remember { mutableStateOf<Order?>(null) }

    var imagePickTarget by remember { mutableStateOf("add") }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            adminViewModel.uploadProductImage(it) { url ->
                url?.let { downloadUrl ->
                    if (imagePickTarget == "add") newImageUrl = downloadUrl
                    else editImageUrl = downloadUrl
                }
            }
        }
    }

    LaunchedEffect(selectedTabIndex) {
        if (selectedTabIndex == 1) adminViewModel.loadOrders()
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            adminViewModel.clearMessages()
        }
    }

    LaunchedEffect(successMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(it)
            adminViewModel.clearMessages()
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val tabTitles = listOf(stringResource(R.string.admin_tab_products), stringResource(R.string.admin_tab_orders))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.admin_panel), style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->

        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTabIndex) {
                // ── Products Tab ──────────────────────────────────────────
                0 -> {
                    if (isLoading && products.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Text(
                                    text = stringResource(R.string.admin_products_count, products.size),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }

                            if (products.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().height(80.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = stringResource(R.string.no_products_found),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else {
                                items(products) { product ->
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        elevation = CardDefaults.cardElevation(0.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(text = product.name, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                                                Text(
                                                    text = "${product.brand} — $${String.format("%.2f", product.price)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            IconButton(onClick = { editingProduct = product }) {
                                                Icon(
                                                    Icons.Outlined.Edit,
                                                    contentDescription = stringResource(R.string.admin_edit),
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            IconButton(onClick = { adminViewModel.deleteProduct(product.id) }) {
                                                Icon(
                                                    Icons.Outlined.Delete,
                                                    contentDescription = stringResource(R.string.admin_delete),
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Add product card
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(0.dp)
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = stringResource(R.string.admin_add_product_title), style = MaterialTheme.typography.titleMedium)
                                            IconButton(onClick = { isAddFormExpanded = !isAddFormExpanded }) {
                                                Icon(
                                                    imageVector = if (isAddFormExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                                                    contentDescription = if (isAddFormExpanded) stringResource(R.string.admin_collapse) else stringResource(R.string.admin_expand)
                                                )
                                            }
                                        }

                                        if (isAddFormExpanded) {
                                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline)
                                            Column(
                                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                                verticalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                OutlinedTextField(
                                                    value = newName, onValueChange = { newName = it },
                                                    label = { Text(stringResource(R.string.admin_product_name)) },
                                                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true,
                                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline)
                                                )
                                                OutlinedTextField(
                                                    value = newBrand, onValueChange = { newBrand = it },
                                                    label = { Text(stringResource(R.string.admin_brand)) },
                                                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true,
                                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline)
                                                )
                                                OutlinedTextField(
                                                    value = newPrice, onValueChange = { newPrice = it },
                                                    label = { Text(stringResource(R.string.admin_price)) },
                                                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true,
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline)
                                                )
                                                OutlinedTextField(
                                                    value = newCategory, onValueChange = { newCategory = it },
                                                    label = { Text(stringResource(R.string.admin_category)) },
                                                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true,
                                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline)
                                                )
                                                OutlinedTextField(
                                                    value = newImageUrl, onValueChange = { newImageUrl = it },
                                                    label = { Text(stringResource(R.string.admin_image_url)) },
                                                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true,
                                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline)
                                                )
                                                OutlinedButton(
                                                    onClick = { imagePickTarget = "add"; imagePickerLauncher.launch("image/*") },
                                                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                                                    enabled = !isUploadingImage
                                                ) {
                                                    if (isUploadingImage && imagePickTarget == "add") {
                                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(stringResource(R.string.admin_uploading))
                                                    } else {
                                                        Text(stringResource(R.string.admin_pick_image))
                                                    }
                                                }
                                                OutlinedTextField(
                                                    value = newDescription, onValueChange = { newDescription = it },
                                                    label = { Text(stringResource(R.string.admin_description)) },
                                                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                                                    minLines = 2, maxLines = 4,
                                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline)
                                                )
                                                Button(
                                                    onClick = {
                                                        adminViewModel.addProduct(
                                                            name = newName, brand = newBrand, price = newPrice,
                                                            category = newCategory, imageUrl = newImageUrl, description = newDescription
                                                        )
                                                        newName = ""; newBrand = ""; newPrice = ""; newCategory = ""; newImageUrl = ""; newDescription = ""
                                                        isAddFormExpanded = false
                                                    },
                                                    modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp),
                                                    enabled = newName.isNotBlank() && newBrand.isNotBlank() && newPrice.isNotBlank() && newCategory.isNotBlank(),
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                                ) {
                                                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(text = stringResource(R.string.admin_add_product_btn), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimary)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            item { Spacer(modifier = Modifier.height(16.dp)) }
                        }
                    }
                }

                // ── Orders Tab ────────────────────────────────────────────
                1 -> {
                    if (isLoadingOrders && orders.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (orders.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = stringResource(R.string.admin_no_orders), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Text(text = stringResource(R.string.admin_orders_count, orders.size), style = MaterialTheme.typography.titleMedium)
                            }

                            items(orders) { order ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(0.dp),
                                    onClick = { selectedOrder = order }
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(text = "#${order.orderId.take(8).uppercase()}", style = MaterialTheme.typography.titleSmall)
                                                Text(text = order.userEmail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                                Text(text = dateFormat.format(Date(order.createdAt)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(text = "$${String.format("%.2f", order.total)}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                                Surface(
                                                    color = if (order.status == "completed") Color(0xFF4CAF50) else Color(0xFFFFC107),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = order.status.uppercase(),
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (order.status == "completed") Color.White else Color(0xFF212121)
                                                    )
                                                }
                                            }
                                        }

                                        if (order.status == "pending") {
                                            Button(
                                                onClick = { adminViewModel.markOrderCompleted(order.orderId) },
                                                modifier = Modifier.fillMaxWidth().height(40.dp),
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                                            ) {
                                                Text(text = stringResource(R.string.admin_mark_complete), style = MaterialTheme.typography.labelLarge, color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }

                            item { Spacer(modifier = Modifier.height(16.dp)) }
                        }
                    }
                }
            }
        }
    }

    // ── Edit Product Bottom Sheet ─────────────────────────────────────────
    if (editingProduct != null) {
        ModalBottomSheet(
            onDismissRequest = { editingProduct = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = stringResource(R.string.admin_edit_product), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 4.dp))

                OutlinedTextField(
                    value = editName, onValueChange = { editName = it },
                    label = { Text(stringResource(R.string.admin_product_name)) },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline)
                )
                OutlinedTextField(
                    value = editBrand, onValueChange = { editBrand = it },
                    label = { Text(stringResource(R.string.admin_brand)) },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline)
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editPrice, onValueChange = { editPrice = it },
                        label = { Text(stringResource(R.string.admin_price)) },
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline)
                    )
                    OutlinedTextField(
                        value = editOriginalPrice, onValueChange = { editOriginalPrice = it },
                        label = { Text(stringResource(R.string.admin_original_price)) },
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline)
                    )
                }
                OutlinedTextField(
                    value = editCategory, onValueChange = { editCategory = it },
                    label = { Text(stringResource(R.string.admin_category)) },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline)
                )
                OutlinedTextField(
                    value = editImageUrl, onValueChange = { editImageUrl = it },
                    label = { Text(stringResource(R.string.admin_image_url)) },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline)
                )
                OutlinedButton(
                    onClick = { imagePickTarget = "edit"; imagePickerLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                    enabled = !isUploadingImage
                ) {
                    if (isUploadingImage && imagePickTarget == "edit") {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.admin_uploading))
                    } else {
                        Text(stringResource(R.string.admin_pick_image))
                    }
                }
                OutlinedTextField(
                    value = editDescription, onValueChange = { editDescription = it },
                    label = { Text(stringResource(R.string.admin_description)) },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                    minLines = 2, maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline)
                )
                OutlinedTextField(
                    value = editSizes, onValueChange = { editSizes = it },
                    label = { Text(stringResource(R.string.admin_sizes_hint)) },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline)
                )
                OutlinedTextField(
                    value = editColors, onValueChange = { editColors = it },
                    label = { Text(stringResource(R.string.admin_colors_hint)) },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline)
                )
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = editIsNew, onCheckedChange = { editIsNew = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.admin_mark_new), style = MaterialTheme.typography.bodyMedium)
                }
                Button(
                    onClick = {
                        editingProduct?.let { p ->
                            adminViewModel.updateProduct(
                                productId = p.id, name = editName, brand = editBrand,
                                price = editPrice, originalPrice = editOriginalPrice,
                                category = editCategory, imageUrl = editImageUrl,
                                description = editDescription, isNew = editIsNew,
                                sizes = editSizes, colors = editColors
                            )
                            editingProduct = null
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp),
                    enabled = editName.isNotBlank() && editBrand.isNotBlank() && editPrice.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(text = stringResource(R.string.admin_save_changes), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }

    // ── Order Detail Dialog ───────────────────────────────────────────────
    selectedOrder?.let { order ->
        val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }

        AlertDialog(
            onDismissRequest = { selectedOrder = null },
            title = {
                Text(
                    text = stringResource(R.string.admin_order_dialog_title, order.orderId.take(8).uppercase()),
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = order.userEmail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = dateFormat.format(Date(order.createdAt)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (order.shippingAddress.isNotEmpty()) {
                        Text(text = stringResource(R.string.admin_ship_to, order.shippingAddress), style = MaterialTheme.typography.bodySmall)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(text = stringResource(R.string.checkout_items_label), style = MaterialTheme.typography.labelLarge)
                    order.items.forEach { item ->
                        val name = item["productName"]?.toString() ?: ""
                        val qty = item["quantity"]?.toString() ?: "1"
                        val price = (item["price"] as? Double)?.let { String.format("%.2f", it) } ?: ""
                        Text(
                            text = "• $name × $qty${if (price.isNotEmpty()) "  $$price" else ""}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.cart_subtotal), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$${String.format("%.2f", order.subtotal)}", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.checkout_shipping_label), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$${String.format("%.2f", order.shipping)}", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.cart_total), style = MaterialTheme.typography.labelLarge)
                        Text("$${String.format("%.2f", order.total)}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedOrder = null }) {
                    Text(stringResource(R.string.close))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}
