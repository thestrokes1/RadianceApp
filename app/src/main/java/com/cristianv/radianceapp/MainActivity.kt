package com.cristianv.radianceapp

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cristianv.radianceapp.model.Product
import com.cristianv.radianceapp.ui.screens.*
import com.cristianv.radianceapp.ui.theme.RadianceAppTheme
import com.cristianv.radianceapp.viewmodel.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private val android.content.Context.mainDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "main_prefs")

private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
private val LANGUAGE_KEY = stringPreferencesKey("language")

class MainActivity : AppCompatActivity() {

    // Stored in attachBaseContext so onCreate can reuse without a second DataStore read.
    private var savedLanguage = "es"

    override fun attachBaseContext(newBase: android.content.Context) {
        // attachBaseContext is called before onCreate and before any resources are
        // inflated. This is the only reliable place to force the locale on every
        // launch, including first install on English-system devices.
        savedLanguage = runBlocking {
            newBase.mainDataStore.data.first()[LANGUAGE_KEY] ?: "es"
        }
        val locale = java.util.Locale(savedLanguage)
        java.util.Locale.setDefault(locale)
        val config = android.content.res.Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Reinforce the locale via both APIs for maximum compatibility across
        // API levels. savedLanguage was already read in attachBaseContext.
        val locale = java.util.Locale(savedLanguage)
        java.util.Locale.setDefault(locale)
        val config = android.content.res.Configuration()
        config.setLocale(locale)
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)

        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(savedLanguage)
        )

        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current
            val activity = context as? android.app.Activity
            val coroutineScope = rememberCoroutineScope()

            var isDarkTheme by rememberSaveable { mutableStateOf(false) }
            val currentLanguage = savedLanguage

            LaunchedEffect(Unit) {
                context.mainDataStore.data
                    .map { prefs -> prefs[DARK_MODE_KEY] ?: false }
                    .collect { isDarkTheme = it }
            }

            RadianceAppTheme(darkTheme = isDarkTheme) {
                RadianceApp(
                    isDarkTheme = isDarkTheme,
                    onToggleDarkTheme = {
                        coroutineScope.launch {
                            val newValue = !isDarkTheme
                            context.mainDataStore.edit { prefs ->
                                prefs[DARK_MODE_KEY] = newValue
                            }
                            isDarkTheme = newValue
                        }
                    },
                    currentLanguage = currentLanguage,
                    onLanguageChange = { lang ->
                        coroutineScope.launch {
                            // 1. Persist so the next attachBaseContext reads the right value
                            context.mainDataStore.edit { prefs ->
                                prefs[LANGUAGE_KEY] = lang
                            }
                            // 2. Java Locale — affects anything not yet recomposed
                            java.util.Locale.setDefault(java.util.Locale(lang))
                            // 3. AppCompat per-app locale API
                            AppCompatDelegate.setApplicationLocales(
                                LocaleListCompat.forLanguageTags(lang)
                            )
                            // 4. Recreate so attachBaseContext runs again with the new locale
                            activity?.recreate()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun RadianceApp(
    isDarkTheme: Boolean = false,
    onToggleDarkTheme: () -> Unit = {},
    currentLanguage: String = "es",
    onLanguageChange: (String) -> Unit = {}
) {
    val navController = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route

    val authViewModel: AuthViewModel = viewModel()
    val cartViewModel: CartViewModel = viewModel()
    val wishlistViewModel: WishlistViewModel = viewModel()
    val homeViewModel: HomeViewModel = viewModel()
    val onboardingViewModel: OnboardingViewModel = viewModel()

    val hasSeenOnboarding by onboardingViewModel.hasSeenOnboarding.collectAsState()

    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Cart.route,
        Screen.Wishlist.route,
        Screen.Profile.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    bottomNavItems.forEach { item ->
                        val isSelected = currentRoute == item.screen.route
                        val badgeCount = when (item.screen) {
                            Screen.Cart -> cartViewModel.totalItems
                            Screen.Wishlist -> wishlistViewModel.totalItems
                            else -> 0
                        }
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(Screen.Home.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                if (badgeCount > 0) {
                                    BadgedBox(
                                        badge = { Badge { Text(badgeCount.toString()) } }
                                    ) {
                                        Icon(
                                            imageVector = if (isSelected) item.selectedIcon
                                            else item.unselectedIcon,
                                            contentDescription = stringResource(item.labelRes)
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = if (isSelected) item.selectedIcon
                                        else item.unselectedIcon,
                                        contentDescription = stringResource(item.labelRes)
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = stringResource(item.labelRes),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(bottom = paddingValues.calculateBottomPadding()),
            enterTransition = {
                fadeIn(animationSpec = tween(300)) + slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                fadeOut(animationSpec = tween(300)) + slideOutHorizontally(
                    targetOffsetX = { -it },
                    animationSpec = tween(300)
                )
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(300)) + slideInHorizontally(
                    initialOffsetX = { -it },
                    animationSpec = tween(300)
                )
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(300)) + slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(300)
                )
            }
        ) {
            composable(Screen.Splash.route) {
                SplashScreen(
                    onSplashComplete = {
                        val seen = hasSeenOnboarding
                        if (seen == null) {
                            navController.navigate("onboarding") {
                                popUpTo(Screen.Splash.route) { inclusive = true }
                            }
                        } else if (seen) {
                            navController.navigate(Screen.Auth.route) {
                                popUpTo(Screen.Splash.route) { inclusive = true }
                            }
                        } else {
                            navController.navigate("onboarding") {
                                popUpTo(Screen.Splash.route) { inclusive = true }
                            }
                        }
                    }
                )
            }
            composable("onboarding") {
                OnboardingScreen(
                    onFinished = {
                        navController.navigate(Screen.Auth.route) {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    },
                    onboardingViewModel = onboardingViewModel
                )
            }
            composable(Screen.Auth.route) {
                AuthScreen(
                    onAuthSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Auth.route) { inclusive = true }
                        }
                    },
                    authViewModel = authViewModel
                )
            }
            composable(Screen.Home.route) {
                HomeScreen(
                    onProductClick = { product ->
                        navController.navigate("detail/${product.id}")
                    },
                    onSearchClick = {
                        navController.navigate(Screen.Search.route)
                    },
                    wishlistViewModel = wishlistViewModel,
                    homeViewModel = homeViewModel
                )
            }
            composable("detail/{productId}") { backStackEntry ->
                val productId = backStackEntry.arguments?.getString("productId")
                val productsState by homeViewModel.productsState.collectAsState()

                var product = when (val state = productsState) {
                    is ProductsState.Success -> state.products.find { it.id == productId }
                    else -> null
                }
                if (product == null) {
                    product = wishlistViewModel.wishlistItems.find { it.id == productId }
                }
                if (product == null) {
                    product = cartViewModel.cartItems.find { it.product.id == productId }?.product
                }

                if (product != null) {
                    ProductDetailScreen(
                        product = product,
                        onBackClick = { navController.popBackStack() },
                        cartViewModel = cartViewModel,
                        wishlistViewModel = wishlistViewModel
                    )
                } else {
                    if (productsState is ProductsState.Loading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) { CircularProgressIndicator() }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(stringResource(R.string.no_products_found))
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = { navController.popBackStack() }) {
                                    Text(stringResource(R.string.back))
                                }
                            }
                        }
                    }
                }
            }
            composable(Screen.Search.route) {
                SearchScreen(
                    onBackClick = { navController.popBackStack() },
                    onProductClick = { product ->
                        navController.navigate("detail/${product.id}")
                    },
                    wishlistViewModel = wishlistViewModel,
                    homeViewModel = homeViewModel
                )
            }
            composable(Screen.Cart.route) {
                CartScreen(
                    cartViewModel = cartViewModel,
                    navController = navController
                )
            }
            composable(Screen.Wishlist.route) {
                WishlistScreen(
                    wishlistViewModel = wishlistViewModel,
                    onProductClick = { product ->
                        navController.navigate("detail/${product.id}")
                    }
                )
            }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate(Screen.Auth.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    authViewModel = authViewModel,
                    wishlistViewModel = wishlistViewModel,
                    cartViewModel = cartViewModel,
                    isDarkTheme = isDarkTheme,
                    onToggleDarkTheme = onToggleDarkTheme,
                    currentLanguage = currentLanguage,
                    onLanguageChange = onLanguageChange,
                    navController = navController
                )
            }
            composable("checkout") {
                CheckoutScreen(
                    navController = navController,
                    cartViewModel = cartViewModel
                )
            }
            composable("admin") {
                AdminScreen(navController = navController)
            }
        }
    }
}
