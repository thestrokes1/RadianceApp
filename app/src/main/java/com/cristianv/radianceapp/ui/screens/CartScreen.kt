package com.cristianv.radianceapp.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.cristianv.radianceapp.R
import com.cristianv.radianceapp.viewmodel.CartItem
import com.cristianv.radianceapp.viewmodel.CartViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    cartViewModel: CartViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    navController: NavController? = null
) {
    val cartItems = cartViewModel.cartItems

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(R.string.cart_title), style = MaterialTheme.typography.headlineMedium)
                },
                actions = {
                    if (cartItems.isNotEmpty()) {
                        TextButton(onClick = { cartViewModel.clearCart() }) {
                            Text(text = stringResource(R.string.cart_clear_all), color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            if (cartItems.isNotEmpty()) {
                Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = stringResource(R.string.cart_subtotal), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "$${String.format("%.2f", cartViewModel.totalPrice)}", style = MaterialTheme.typography.titleMedium)
                        }
                        if (cartViewModel.totalSavings > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = stringResource(R.string.cart_you_save), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                                Text(text = "-$${String.format("%.2f", cartViewModel.totalSavings)}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = stringResource(R.string.cart_total), style = MaterialTheme.typography.titleLarge)
                            Text(text = "$${String.format("%.2f", cartViewModel.totalPrice)}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                        }
                        Button(
                            onClick = { navController?.navigate("checkout") },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(text = stringResource(R.string.cart_checkout), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->

        if (cartItems.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "🛒", style = MaterialTheme.typography.displayLarge)
                    Text(text = stringResource(R.string.cart_empty), style = MaterialTheme.typography.titleLarge)
                    Text(text = stringResource(R.string.cart_empty_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(cartItems) { cartItem ->
                    CartItemCard(
                        cartItem = cartItem,
                        onIncrease = { cartViewModel.increaseQuantity(cartItem) },
                        onDecrease = { cartViewModel.decreaseQuantity(cartItem) },
                        onRemove = { cartViewModel.removeFromCart(cartItem) }
                    )
                }
            }
        }
    }
}

@Composable
fun CartItemCard(
    cartItem: CartItem,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val displayImageUrl = if (cartItem.selectedColor != null && cartItem.product.colorImages.containsKey(cartItem.selectedColor)) {
                cartItem.product.colorImages[cartItem.selectedColor]!!
            } else {
                cartItem.product.imageUrl
            }

            AsyncImage(
                model = displayImageUrl,
                contentDescription = cartItem.product.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(90.dp).clip(RoundedCornerShape(12.dp))
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = cartItem.product.brand, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = cartItem.product.name, style = MaterialTheme.typography.titleMedium, maxLines = 2)

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (cartItem.selectedSize != null) {
                        Text(
                            text = stringResource(R.string.cart_item_size, cartItem.selectedSize),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (cartItem.selectedColor != null) {
                        Text(
                            text = stringResource(R.string.cart_item_color, cartItem.selectedColor),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(text = "$${cartItem.product.price}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onDecrease,
                        modifier = Modifier.size(32.dp).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    ) { Text("-", style = MaterialTheme.typography.titleMedium) }
                    Text(
                        text = cartItem.quantity.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.width(32.dp),
                        textAlign = TextAlign.Center
                    )
                    IconButton(
                        onClick = onIncrease,
                        modifier = Modifier.size(32.dp).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    ) { Text("+", style = MaterialTheme.typography.titleMedium) }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.cart_remove), tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}
