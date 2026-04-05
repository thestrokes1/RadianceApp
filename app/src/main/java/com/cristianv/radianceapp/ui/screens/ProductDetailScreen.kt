package com.cristianv.radianceapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.cristianv.radianceapp.R
import com.cristianv.radianceapp.model.Product
import com.cristianv.radianceapp.viewmodel.CartViewModel
import com.cristianv.radianceapp.viewmodel.WishlistViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProductDetailScreen(
    product: Product,
    onBackClick: () -> Unit = {},
    cartViewModel: CartViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    wishlistViewModel: WishlistViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    var selectedSize by remember { mutableStateOf<String?>(null) }
    var selectedColor by remember(product.id) { mutableStateOf(product.colors.firstOrNull()) }
    var isFavorite by remember(product.id) { mutableStateOf(wishlistViewModel.isInWishlist(product.id)) }
    var quantity by remember { mutableIntStateOf(1) }

    val galleryImages = remember(product.id) {
        if (product.colorImages.isNotEmpty()) {
            product.colors.mapNotNull { product.colorImages[it] }.ifEmpty { listOf(product.imageUrl) }
        } else {
            listOf(product.imageUrl)
        }
    }

    val pagerState = rememberPagerState(pageCount = { galleryImages.size })
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage) {
        val color = product.colors.getOrNull(pagerState.currentPage)
        if (color != selectedColor) selectedColor = color
    }

    LaunchedEffect(selectedColor) {
        val idx = product.colors.indexOf(selectedColor)
        if (idx >= 0 && idx != pagerState.currentPage) pagerState.animateScrollToPage(idx)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        wishlistViewModel.toggleWishlist(product)
                        isFavorite = wishlistViewModel.isInWishlist(product.id)
                    }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = stringResource(R.string.product_favorite),
                            tint = if (isFavorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Outlined.Share, contentDescription = stringResource(R.string.product_share))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quantity selector
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        TextButton(
                            onClick = { if (quantity > 1) quantity-- },
                            modifier = Modifier.size(32.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) { Text("-", style = MaterialTheme.typography.titleLarge) }
                        Text(
                            text = quantity.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.width(24.dp),
                            textAlign = TextAlign.Center
                        )
                        TextButton(
                            onClick = { quantity++ },
                            modifier = Modifier.size(32.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) { Text("+", style = MaterialTheme.typography.titleLarge) }
                    }

                    // Add to cart button
                    Button(
                        onClick = {
                            cartViewModel.addToCart(
                                product = product,
                                quantity = quantity,
                                selectedSize = selectedSize,
                                selectedColor = selectedColor
                            )
                            onBackClick()
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(
                            text = stringResource(R.string.product_add_to_cart, String.format("%.2f", product.price * quantity)),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->

        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState())
        ) {

            // Image Gallery
            Box(modifier = Modifier.fillMaxWidth().height(380.dp)) {
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    AsyncImage(
                        model = galleryImages[page],
                        contentDescription = "${product.name} image ${page + 1}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                    )
                }
                if (galleryImages.size > 1) {
                    Row(
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(galleryImages.size) { index ->
                            val isSelected = index == pagerState.currentPage
                            Box(
                                modifier = Modifier
                                    .size(width = if (isSelected) 20.dp else 7.dp, height = 7.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                                    )
                                    .clickable { coroutineScope.launch { pagerState.animateScrollToPage(index) } }
                            )
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

                // Brand + name + rating
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = product.brand.uppercase(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                    Text(text = product.name, style = MaterialTheme.typography.headlineMedium)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(text = "★ ${product.rating}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                        Text(
                            text = stringResource(R.string.product_reviews, product.reviewCount),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Price
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = "$${product.price}", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                    if (product.originalPrice != null) {
                        Text(
                            text = "$${product.originalPrice}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = TextDecoration.LineThrough
                        )
                        val discount = ((1 - product.price / product.originalPrice) * 100).toInt()
                        Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.error) {
                            Text(
                                text = "-$discount%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outline)

                // Size selector
                if (product.sizes.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = stringResource(R.string.product_select_size), style = MaterialTheme.typography.titleMedium)
                            TextButton(onClick = {}) {
                                Text(text = stringResource(R.string.product_size_guide), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            product.sizes.forEach { size ->
                                val isSelected = size == selectedSize
                                Surface(
                                    onClick = { selectedSize = size },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                                    modifier = Modifier.size(width = 60.dp, height = 48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = size,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Color selector
                if (product.colors.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = if (selectedColor != null) stringResource(R.string.product_color_selected, selectedColor!!)
                            else stringResource(R.string.product_select_color),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            product.colors.forEach { colorStr ->
                                val isSelected = colorStr == selectedColor
                                val color = when (colorStr.lowercase()) {
                                    "black" -> androidx.compose.ui.graphics.Color(0xFF0A0A0A)
                                    "white" -> androidx.compose.ui.graphics.Color(0xFFF8F8F8)
                                    "blue" -> androidx.compose.ui.graphics.Color(0xFF1565C0)
                                    "navy" -> androidx.compose.ui.graphics.Color(0xFF0D1B4B)
                                    "red" -> androidx.compose.ui.graphics.Color(0xFFE53935)
                                    "green" -> androidx.compose.ui.graphics.Color(0xFF2E7D32)
                                    "grey", "gray" -> androidx.compose.ui.graphics.Color(0xFF757575)
                                    "beige" -> androidx.compose.ui.graphics.Color(0xFFF5F0E8)
                                    "pink" -> androidx.compose.ui.graphics.Color(0xFFEC407A)
                                    "camel" -> androidx.compose.ui.graphics.Color(0xFFC19A6B)
                                    "khaki" -> androidx.compose.ui.graphics.Color(0xFFBDB76B)
                                    "olive" -> androidx.compose.ui.graphics.Color(0xFF6B6B2A)
                                    "purple" -> androidx.compose.ui.graphics.Color(0xFF7B1FA2)
                                    else -> androidx.compose.ui.graphics.Color(0xFFCCCCCC)
                                }
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp).clip(CircleShape).background(color)
                                            .border(
                                                width = if (isSelected) 3.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                                shape = CircleShape
                                            )
                                            .clickable { selectedColor = colorStr }
                                    )
                                    if (isSelected) {
                                        Text(text = colorStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outline)

                // Description
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = stringResource(R.string.product_description), style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = product.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
