package shop.chobbar.app.ui.nav

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import shop.chobbar.app.R
import shop.chobbar.app.ui.components.AppIcons
import shop.chobbar.app.ui.screens.*
import shop.chobbar.app.ui.theme.LocalAppContainer
import shop.chobbar.app.ui.theme.LocalLowEnd

private val TABS = listOf("home", "shop", "search", "cart", "account")

@Composable
fun ChobbarRoot(initialDeepLink: String?, onLanguageChange: (String) -> Unit) {
    val nav = rememberNavController()
    val c = LocalAppContainer.current
    val snackbar = remember { SnackbarHostState() }
    val cart by c.cart.cart.collectAsState()
    val user by c.auth.user.collectAsState()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: "home"

    LaunchedEffect(Unit) {
        UiMessages.flow.collect { m ->
            val r = snackbar.showSnackbar(m.text, actionLabel = m.action, withDismissAction = m.action == null, duration = SnackbarDuration.Short)
            if (r == SnackbarResult.ActionPerformed) m.onAction?.invoke()
        }
    }
    LaunchedEffect(Unit) {
        initialDeepLink?.let { DeepLinks.route(it) }?.let { nav.navigate(it) }
        DeepLinks.flow.collect { url -> DeepLinks.route(url)?.let { nav.navigate(it) } }
    }
    // Realtime notifications only while the app is in the foreground and signed in (saves battery/data).
    LifecycleEventEffect(Lifecycle.Event.ON_START) { if (c.auth.isSignedIn) c.realtime.connect() }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { c.realtime.disconnect() }
    LaunchedEffect(user?.id) {
        if (user != null) c.realtime.connect() else c.realtime.disconnect()
        c.wishlist.refresh()
    }
    LaunchedEffect(Unit) {
        c.realtime.notifications.collect { n ->
            if (n.kind == "order" || n.kind == "payment") c.cart.refresh()
            val target = DeepLinks.route("https://chobbar.shop" + n.url)
            UiMessages.show(n.title, if (target != null) "→" else null) { target?.let { nav.navigate(it) } }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            AnimatedVisibility(route in TABS, enter = slideInVertically { it }, exit = slideOutVertically { it }) {
                NavigationBar(tonalElevation = 0.dp, containerColor = MaterialTheme.colorScheme.surface) {
                    TabItem(nav, route, "home", Icons.Outlined.Home, R.string.nav_home)
                    TabItem(nav, route, "shop", AppIcons.Grid, R.string.nav_shop)
                    TabItem(nav, route, "search", Icons.Outlined.Search, R.string.common_search)
                    TabItem(nav, route, "cart", AppIcons.Bag, R.string.nav_bag, badge = cart?.quantity ?: 0)
                    TabItem(nav, route, "account", Icons.Outlined.Person, R.string.nav_account)
                }
            }
        },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        val lowEnd = LocalLowEnd.current
        val d = if (lowEnd) 120 else 280
        NavHost(
            nav, startDestination = "home", modifier = Modifier.padding(padding),
            enterTransition = { if (initialState.isTab() && targetState.isTab()) fadeIn(tween(d)) else slideInHorizontally(tween(d)) { it / 6 } + fadeIn(tween(d)) },
            exitTransition = { fadeOut(tween(d / 2)) },
            popEnterTransition = { fadeIn(tween(d)) },
            popExitTransition = { slideOutHorizontally(tween(d)) { it / 6 } + fadeOut(tween(d)) },
        ) {
            composable("home") { HomeScreen(nav) }
            composable("shop") { ShopScreen(nav) }
            composable("search") { SearchScreen(nav) }
            composable("cart") { CartScreen(nav) }
            composable("account") { AccountScreen(nav, onLanguageChange) }
            composable("list/{kind}/{slug}") { b -> ProductListScreen(nav, b.arguments?.getString("kind") ?: "all", b.arguments?.getString("slug") ?: "") }
            composable("product/{slug}") { b -> ProductScreen(nav, b.arguments?.getString("slug")!!) }
            composable("checkout") { CheckoutScreen(nav) }
            composable("success/{id}") { b -> OrderSuccessScreen(nav, b.arguments?.getString("id")!!) }
            composable("login") { LoginScreen(nav) }
            composable("register") { RegisterScreen(nav) }
            composable("forgot") { ForgotPasswordScreen(nav) }
            composable("orders") { OrdersScreen(nav) }
            composable("order/{id}") { b -> OrderDetailScreen(nav, b.arguments?.getString("id")!!) }
            composable("wishlist") { WishlistScreen(nav) }
            composable("addresses") { AddressesScreen(nav) }
            composable("notifications") { NotificationsScreen(nav) }
            composable("settings") { SettingsScreen(nav, onLanguageChange) }
            composable("track") { TrackScreen(nav) }
            composable("faq") { FaqScreen(nav) }
            composable("page/{slug}") { b -> ContentPageScreen(nav, b.arguments?.getString("slug")!!) }
            composable("chats") { ChatListScreen(nav) }
            composable("chat/{id}") { b -> ChatScreen(nav, b.arguments?.getString("id")!!) }
        }
    }
}

private fun NavBackStackEntry.isTab() = destination.route in TABS

@Composable
private fun RowScope.TabItem(nav: NavHostController, current: String, route: String, icon: androidx.compose.ui.graphics.vector.ImageVector, label: Int, badge: Int = 0) {
    NavigationBarItem(
        selected = current == route,
        onClick = {
            if (current != route) nav.navigate(route) {
                popUpTo("home") { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        },
        icon = {
            BadgedBox(badge = { if (badge > 0) Badge { Text(if (badge > 99) "99+" else badge.toString()) } }) { Icon(icon, null) }
        },
        label = { Text(stringResource(label), style = MaterialTheme.typography.labelSmall) },
        colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primaryContainer, selectedIconColor = MaterialTheme.colorScheme.primary, selectedTextColor = MaterialTheme.colorScheme.primary),
    )
}

/** Navigate to a protected route, or to sign-in first. */
fun NavHostController.requireAuth(signedIn: Boolean, route: String) = navigate(if (signedIn) route else "login")
