package com.cristianv.radianceapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.cristianv.radianceapp.R
import com.cristianv.radianceapp.data.FirebaseRepository
import com.cristianv.radianceapp.viewmodel.CartViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    navController: NavController,
    cartViewModel: CartViewModel
) {
    var currentStep by remember { mutableStateOf(1) }

    var fullName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }

    var isSavingOrder by remember { mutableStateOf(false) }
    var savedOrderId by remember { mutableStateOf("") }
    var savedTotal by remember { mutableStateOf(0.0) }
    var savedItemCount by remember { mutableStateOf(0) }
    var saveError by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val repository = remember { FirebaseRepository() }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(saveError) {
        saveError?.let {
            snackbarHostState.showSnackbar(it)
            saveError = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (currentStep) {
                            1 -> stringResource(R.string.checkout_step_shipping)
                            2 -> stringResource(R.string.checkout_step_summary)
                            else -> stringResource(R.string.checkout_step_confirm)
                        },
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                navigationIcon = {
                    if (currentStep < 3) {
                        IconButton(onClick = {
                            if (currentStep > 1) currentStep-- else navController.popBackStack()
                        }) {
                            Icon(Icons.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->

        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (currentStep < 3) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(2) { index ->
                        val stepNum = index + 1
                        val isActive = currentStep >= stepNum
                        Box(modifier = Modifier.weight(1f).height(4.dp), contentAlignment = Alignment.Center) {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(2.dp)
                            ) {}
                        }
                    }
                }
            }

            when (currentStep) {
                1 -> ShippingStep(
                    fullName = fullName, address = address, city = city, country = country,
                    onFullNameChange = { fullName = it }, onAddressChange = { address = it },
                    onCityChange = { city = it }, onCountryChange = { country = it },
                    onContinue = { currentStep = 2 }
                )
                2 -> OrderSummaryStep(
                    cartViewModel = cartViewModel,
                    fullName = fullName, address = address, city = city, country = country,
                    isLoading = isSavingOrder,
                    onPlaceOrder = {
                        scope.launch {
                            isSavingOrder = true
                            try {
                                val user = FirebaseAuth.getInstance().currentUser
                                val subtotal = cartViewModel.totalPrice
                                val shipping = 5.99
                                val total = subtotal + shipping
                                val itemCount = cartViewModel.totalItems
                                val orderData = mapOf(
                                    "userId" to (user?.uid ?: ""),
                                    "userEmail" to (user?.email ?: ""),
                                    "items" to cartViewModel.cartItems.map { item ->
                                        mapOf(
                                            "productId" to item.product.id,
                                            "productName" to item.product.name,
                                            "price" to item.product.price,
                                            "quantity" to item.quantity,
                                            "size" to (item.selectedSize ?: ""),
                                            "color" to (item.selectedColor ?: "")
                                        )
                                    },
                                    "subtotal" to subtotal,
                                    "shipping" to shipping,
                                    "total" to total,
                                    "address" to mapOf("street" to address, "city" to city, "country" to country),
                                    "status" to "pending",
                                    "createdAt" to System.currentTimeMillis()
                                )
                                savedOrderId = repository.saveOrder(orderData)
                                savedTotal = total
                                savedItemCount = itemCount
                                cartViewModel.clearCart()
                                currentStep = 3
                            } catch (e: Exception) {
                                saveError = e.message ?: "Failed to place order"
                            } finally {
                                isSavingOrder = false
                            }
                        }
                    }
                )
                3 -> OrderConfirmationStep(
                    orderId = savedOrderId,
                    fullName = fullName, city = city, country = country,
                    orderTotal = savedTotal, totalItems = savedItemCount,
                    onContinueShopping = {
                        navController.navigate("home") { popUpTo(0) { inclusive = true } }
                    }
                )
            }
        }
    }
}

@Composable
private fun ShippingStep(
    fullName: String, address: String, city: String, country: String,
    onFullNameChange: (String) -> Unit, onAddressChange: (String) -> Unit,
    onCityChange: (String) -> Unit, onCountryChange: (String) -> Unit,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = stringResource(R.string.checkout_deliver_question), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        OutlinedTextField(value = fullName, onValueChange = onFullNameChange, label = { Text(stringResource(R.string.checkout_full_name)) },
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline))

        OutlinedTextField(value = address, onValueChange = onAddressChange, label = { Text(stringResource(R.string.checkout_address)) },
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline))

        OutlinedTextField(value = city, onValueChange = onCityChange, label = { Text(stringResource(R.string.checkout_city)) },
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline))

        OutlinedTextField(value = country, onValueChange = onCountryChange, label = { Text(stringResource(R.string.checkout_country)) },
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline))

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(14.dp),
            enabled = fullName.isNotBlank() && address.isNotBlank() && city.isNotBlank() && country.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(text = stringResource(R.string.checkout_continue), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

@Composable
private fun OrderSummaryStep(
    cartViewModel: CartViewModel, fullName: String, address: String, city: String, country: String,
    isLoading: Boolean, onPlaceOrder: () -> Unit
) {
    val cartItems = cartViewModel.cartItems

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(0.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = stringResource(R.string.checkout_shipping_to), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = fullName, style = MaterialTheme.typography.titleMedium)
                Text(text = "$address, $city, $country", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Text(text = stringResource(R.string.checkout_items_count, cartItems.size), style = MaterialTheme.typography.titleMedium)

        cartItems.forEach { cartItem ->
            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(0.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = cartItem.product.name, style = MaterialTheme.typography.titleSmall, maxLines = 2)
                        Text(text = "Qty: ${cartItem.quantity}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(text = "$${String.format("%.2f", cartItem.product.price * cartItem.quantity)}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = stringResource(R.string.cart_subtotal), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = "$${String.format("%.2f", cartViewModel.totalPrice)}", style = MaterialTheme.typography.bodyMedium)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = stringResource(R.string.checkout_shipping_label), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = "$5.99", style = MaterialTheme.typography.bodyMedium)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = stringResource(R.string.cart_total), style = MaterialTheme.typography.titleLarge)
            Text(text = "$${String.format("%.2f", cartViewModel.totalPrice + 5.99)}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        }

        Button(
            onClick = onPlaceOrder,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(14.dp),
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
            } else {
                Text(text = stringResource(R.string.checkout_place_order), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

@Composable
private fun OrderConfirmationStep(
    orderId: String, fullName: String, city: String, country: String,
    orderTotal: Double, totalItems: Int, onContinueShopping: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(96.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = stringResource(R.string.checkout_success_title), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = stringResource(R.string.checkout_success_message, fullName), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(24.dp))

        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(0.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = stringResource(R.string.checkout_order_details), style = MaterialTheme.typography.titleMedium)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                if (orderId.isNotEmpty()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = stringResource(R.string.checkout_order_id), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "#${orderId.take(8).uppercase()}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = stringResource(R.string.checkout_delivering_to), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "$city, $country", style = MaterialTheme.typography.bodyMedium)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = stringResource(R.string.checkout_order_total), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "$${String.format("%.2f", orderTotal)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = stringResource(R.string.checkout_items_label), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "$totalItems", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onContinueShopping,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(text = stringResource(R.string.checkout_continue_shopping), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}
