package com.melato.shop.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.melato.shop.ui.components.MelatoBottomBar
import com.melato.shop.ui.screens.*
import com.melato.shop.ui.theme.NearBlack
import com.melato.shop.vm.ShopViewModel

@Composable
fun AppNav() {
    val navController = rememberNavController()
    val vm: ShopViewModel = viewModel()
    val cart by vm.cart.collectAsState()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    val bottomBarRoutes = setOf(Screen.Home.route, Screen.Shop.route, Screen.Cart.route, Screen.Profile.route)
    val showBottomBar = currentRoute in bottomBarRoutes

    Scaffold(
        containerColor = NearBlack,
        bottomBar = {
            if (showBottomBar) {
                MelatoBottomBar(
                    currentRoute = currentRoute,
                    cartCount = vm.cartCount(),
                    onNavigate = { screen ->
                        navController.navigate(screen.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Splash.route) {
                SplashScreen(onDone = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                })
            }
            composable(Screen.Home.route) {
                HomeScreen(
                    cartCount = vm.cartCount(),
                    onProductClick = { id -> navController.navigate(Screen.ProductDetail.route(id)) },
                    onCartClick = { navController.navigate(Screen.Cart.route) },
                    onShopClick = {
                        navController.navigate(Screen.Shop.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                    onCategoryClick = { catId ->
                        navController.navigate(Screen.Shop.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Screen.Shop.route) {
                ShopScreen(onProductClick = { id -> navController.navigate(Screen.ProductDetail.route(id)) })
            }
            composable(Screen.ProductDetail.route) { backStackEntry ->
                val productId = backStackEntry.arguments?.getString("productId") ?: return@composable
                val product = vm.getProduct(productId) ?: return@composable
                ProductDetailScreen(
                    product = product,
                    onBack = { navController.popBackStack() },
                    onAddToCart = { size, color -> vm.addToCart(product, size, color) }
                )
            }
            composable(Screen.Cart.route) {
                CartScreen(
                    cartItems = cart,
                    total = vm.cartTotal(),
                    onRemove = { vm.removeFromCart(it) },
                    onQuantityChange = { item, delta -> vm.updateQuantity(item, delta) }
                )
            }
            composable(Screen.Profile.route) {
                ProfileScreen(onFaqClick = { navController.navigate(Screen.Faq.route) })
            }
            composable(Screen.Faq.route) {
                FaqScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
