package dev.kosh.financetracker

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import dev.kosh.financetracker.feature.detail.TransactionDetailScreen
import dev.kosh.financetracker.feature.insights.InsightsScreen
import dev.kosh.financetracker.feature.overview.OverviewScreen
import dev.kosh.financetracker.feature.review.ReviewScreen
import dev.kosh.financetracker.feature.trail.TrailScreen
import dev.kosh.financetracker.navigation.AppShellViewModel
import dev.kosh.financetracker.navigation.KoshDestination
import dev.kosh.financetracker.ui.components.KoshBottomBar
import dev.kosh.financetracker.ui.theme.KoshTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Lets the warm hero gradient draw behind the status bar; light (white)
        // status bar icons since Home's background is dark all the way to the edge.
        // Forced explicitly — there's no day/night resource split for auto-detect to key off.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
        setContent {
            KoshTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    KoshApp()
                }
            }
        }
    }
}

@Composable
private fun KoshApp() {
    val navController = rememberNavController()
    val shellViewModel: AppShellViewModel = hiltViewModel()
    val needsReviewCount by shellViewModel.needsReviewCount.collectAsState()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        // No top inset here on purpose — Home's gradient bleeds behind the status
        // bar; each screen clears the status bar itself where it actually has
        // content up there (see OverviewScreen). Bottom bar still reserves its
        // own space via the padding this Scaffold hands back.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (currentRoute == null || currentRoute != KoshDestination.Detail.route) {
                KoshBottomBar(
                    currentRoute = currentRoute,
                    reviewBadgeCount = needsReviewCount,
                    onNavigate = { destination ->
                        navController.navigate(destination.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = KoshDestination.Overview.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(KoshDestination.Overview.route) {
                OverviewScreen(
                    onOpenTransaction = { id -> navController.navigate(KoshDestination.Detail.createRoute(id)) },
                    onOpenReview = { navController.navigate(KoshDestination.Review.route) },
                    onSeeAllActivity = { navController.navigate(KoshDestination.Trail.route) },
                    onOpenInsights = { navController.navigate(KoshDestination.Insights.route) },
                )
            }
            composable(KoshDestination.Trail.route) {
                TrailScreen(
                    onOpenTransaction = { id -> navController.navigate(KoshDestination.Detail.createRoute(id)) },
                )
            }
            composable(KoshDestination.Insights.route) {
                InsightsScreen()
            }
            composable(KoshDestination.Review.route) {
                ReviewScreen()
            }
            composable(KoshDestination.Detail.route) {
                TransactionDetailScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
