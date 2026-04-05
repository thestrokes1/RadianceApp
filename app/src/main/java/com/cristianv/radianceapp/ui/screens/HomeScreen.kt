package com.cristianv.radianceapp.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.grid.items
import com.cristianv.radianceapp.ui.components.ShimmerProductCard
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.cristianv.radianceapp.R
import com.cristianv.radianceapp.data.SampleData
import com.cristianv.radianceapp.model.Product
import com.cristianv.radianceapp.ui.components.ProductCard
import com.cristianv.radianceapp.viewmodel.HomeViewModel
import com.cristianv.radianceapp.viewmodel.ProductsState
import com.cristianv.radianceapp.viewmodel.WishlistViewModel

enum class SortOption {
    DEFAULT, PRICE_LOW_HIGH, PRICE_HIGH_LOW, RATING, NEWEST
}

@Composable
fun localizedCategory(category: String): String = when (category) {
    "All" -> stringResource(R.string.category_all)
    "Women" -> stringResource(R.string.category_women)
    "Men" -> stringResource(R.string.category_men)
    "Kids" -> stringResource(R.string.category_kids)
    "Accessories" -> stringResource(R.string.category_accessories)
    "Sale" -> stringResource(R.string.category_sale)
    else -> category
}

@Composable
fun SortOption.localizedLabel(): String = when (this) {
    SortOption.DEFAULT -> stringResource(R.string.sort_default)
    SortOption.PRICE_LOW_HIGH -> stringResource(R.string.sort_price_low_high)
    SortOption.PRICE_HIGH_LOW -> stringResource(R.string.sort_price_high_low)
    SortOption.RATING -> stringResource(R.string.sort_rating)
    SortOption.NEWEST -> stringResource(R.string.sort_newest)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onProductClick: (Product) -> Unit = {},
    onSearchClick: () -> Unit = {},
    wishlistViewModel: WishlistViewModel = viewModel(),
    homeViewModel: HomeViewModel = viewModel()
) {
    val productsState by homeViewModel.productsState.collectAsState()
    val selectedCategory by homeViewModel.selectedCategory.collectAsState()
    val isRefreshing by homeViewModel.isRefreshing.collectAsState()

    var sortOption by remember { mutableStateOf(SortOption.DEFAULT) }
    var appliedMinPrice by remember { mutableStateOf(0f) }
    var appliedMaxPrice by remember { mutableStateOf(200f) }
    var showFilterSheet by remember { mutableStateOf(false) }

    var pendingSortOption by remember { mutableStateOf(SortOption.DEFAULT) }
    var pendingMinPrice by remember { mutableStateOf(0f) }
    var pendingMaxPrice by remember { mutableStateOf(200f) }
    var pendingCategory by remember { mutableStateOf(selectedCategory) }

    val isFiltered = sortOption != SortOption.DEFAULT ||
            appliedMinPrice > 0f || appliedMaxPrice < 200f || selectedCategory != "All"

    val filteredProducts = when (val state = productsState) {
        is ProductsState.Success -> {
            var list = state.products
            if (selectedCategory != "All") list = list.filter { it.category == selectedCategory }
            list = list.filter { it.price >= appliedMinPrice && it.price <= appliedMaxPrice }
            list = when (sortOption) {
                SortOption.PRICE_LOW_HIGH -> list.sortedBy { it.price }
                SortOption.PRICE_HIGH_LOW -> list.sortedByDescending { it.price }
                SortOption.RATING -> list.sortedByDescending { it.rating }
                SortOption.NEWEST -> list.sortedByDescending { it.isNew }
                SortOption.DEFAULT -> list
            }
            list
        }
        else -> emptyList()
    }

    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(selectedCategory) { gridState.animateScrollToItem(0) }

    val pullToRefreshState = rememberPullToRefreshState()

    // Filter bottom sheet
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.home_filter_sort),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = {
                            pendingSortOption = SortOption.DEFAULT
                            pendingMinPrice = 0f
                            pendingMaxPrice = 200f
                            pendingCategory = "All"
                        }) {
                            Text(text = stringResource(R.string.filter_reset), color = MaterialTheme.colorScheme.error)
                        }
                        IconButton(onClick = { showFilterSheet = false }) {
                            Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.filter_close))
                        }
                    }
                }

                HorizontalDivider()

                // Sort by
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = stringResource(R.string.filter_sort_by), style = MaterialTheme.typography.titleMedium)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SortOption.values().forEach { option ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { pendingSortOption = option }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = option.localizedLabel(),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (pendingSortOption == option)
                                        MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                                )
                                RadioButton(
                                    selected = pendingSortOption == option,
                                    onClick = { pendingSortOption = option },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Price range
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = stringResource(R.string.filter_price_range), style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "$${pendingMinPrice.toInt()} – $${pendingMaxPrice.toInt()}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    RangeSlider(
                        value = pendingMinPrice..pendingMaxPrice,
                        onValueChange = { range ->
                            pendingMinPrice = range.start
                            pendingMaxPrice = range.endInclusive
                        },
                        valueRange = 0f..200f,
                        steps = 0,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "$0", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "$200", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                HorizontalDivider()

                // Category
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = stringResource(R.string.filter_category), style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SampleData.categories.forEach { category ->
                            val isSelected = category == pendingCategory
                            Surface(
                                modifier = Modifier.clip(RoundedCornerShape(20.dp)),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(20.dp),
                                onClick = { pendingCategory = category }
                            ) {
                                Text(
                                    text = localizedCategory(category),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        sortOption = pendingSortOption
                        appliedMinPrice = pendingMinPrice
                        appliedMaxPrice = pendingMaxPrice
                        homeViewModel.setCategory(pendingCategory)
                        showFilterSheet = false
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(text = stringResource(R.string.filter_apply), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.Black),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.home_greeting),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { onSearchClick() }) {
                        Icon(Icons.Outlined.Search, contentDescription = stringResource(R.string.home_search))
                    }
                    IconButton(onClick = {
                        pendingSortOption = sortOption
                        pendingMinPrice = appliedMinPrice
                        pendingMaxPrice = appliedMaxPrice
                        pendingCategory = selectedCategory
                        showFilterSheet = true
                    }) {
                        if (isFiltered) {
                            BadgedBox(badge = { Badge(containerColor = MaterialTheme.colorScheme.primary) }) {
                                Icon(Icons.Filled.FilterList, contentDescription = stringResource(R.string.home_filter_sort), tint = MaterialTheme.colorScheme.primary)
                            }
                        } else {
                            Icon(Icons.Filled.FilterList, contentDescription = stringResource(R.string.home_filter_sort))
                        }
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Outlined.Notifications, contentDescription = stringResource(R.string.home_notifications))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { homeViewModel.refresh() },
            state = pullToRefreshState,
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // Banner
                Surface(
                    onClick = {
                        homeViewModel.setCategory("Women")
                        coroutineScope.launch { gridState.animateScrollToItem(0) }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        val circleColor = Color.White.copy(alpha = 0.06f)
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val cx = size.width - 56.dp.toPx()
                            val cy = size.height / 2f
                            drawCircle(color = circleColor, radius = 45.dp.toPx(), center = Offset(cx, cy))
                            drawCircle(color = circleColor, radius = 31.dp.toPx(), center = Offset(cx, cy))
                            drawCircle(color = circleColor, radius = 18.dp.toPx(), center = Offset(cx, cy))
                        }
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.home_new_collection),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = stringResource(R.string.home_season),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.secondary) {
                                Text(
                                    text = stringResource(R.string.home_shop_now),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // Category chips
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SampleData.categories.forEach { category ->
                        val isSelected = category == selectedCategory
                        Surface(
                            modifier = Modifier.clip(RoundedCornerShape(20.dp)),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(20.dp),
                            onClick = { homeViewModel.setCategory(category) }
                        ) {
                            Text(
                                text = localizedCategory(category),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Section title
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isFiltered) stringResource(R.string.home_results, filteredProducts.size)
                        else stringResource(R.string.home_featured),
                        style = MaterialTheme.typography.titleLarge
                    )
                    TextButton(onClick = { homeViewModel.loadProducts() }) {
                        Text(
                            text = stringResource(R.string.home_refresh),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                // Products
                when (val state = productsState) {
                    is ProductsState.Loading -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) { items(6) { ShimmerProductCard() } }
                    }
                    is ProductsState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(text = stringResource(R.string.home_error), style = MaterialTheme.typography.titleMedium)
                                Text(text = state.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Button(onClick = { homeViewModel.loadProducts() }) {
                                    Text(stringResource(R.string.home_try_again))
                                }
                            }
                        }
                    }
                    is ProductsState.Success -> {
                        if (filteredProducts.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(text = stringResource(R.string.no_products_found), style = MaterialTheme.typography.titleMedium)
                                    Text(text = stringResource(R.string.filter_adjust_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    TextButton(onClick = {
                                        sortOption = SortOption.DEFAULT
                                        appliedMinPrice = 0f
                                        appliedMaxPrice = 200f
                                        homeViewModel.setCategory("All")
                                    }) {
                                        Text(stringResource(R.string.filter_clear))
                                    }
                                }
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                state = gridState,
                                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(bottom = 80.dp)
                            ) {
                                items(filteredProducts) { product ->
                                    ProductCard(product = product, onClick = { onProductClick(product) }, wishlistViewModel = wishlistViewModel)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
