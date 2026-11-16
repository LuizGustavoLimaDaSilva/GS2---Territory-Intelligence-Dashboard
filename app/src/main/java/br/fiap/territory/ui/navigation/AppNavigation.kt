package br.fiap.territory.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import br.fiap.territory.domain.model.IndicatorType
import br.fiap.territory.ui.alert.AlertRoute
import br.fiap.territory.ui.dashboard.DashboardRoute
import br.fiap.territory.ui.history.HistoryRoute
import br.fiap.territory.ui.indicator.IndicatorRoute
import br.fiap.territory.ui.settings.SettingsRoute

/** Mapa de rotas. Só identificadores (texto) trafegam na rota, nunca objetos inteiros. */
object Routes {
    const val DASHBOARD = "dashboard"
    const val INDICATOR = "indicator/{territoryId}/{type}"
    const val ALERT = "alert/{alertId}"
    const val HISTORY = "history"
    const val SETTINGS = "settings"

    fun indicator(territoryId: String, type: IndicatorType) = "indicator/$territoryId/${type.name}"
    fun alert(alertId: String) = "alert/$alertId"
}

@Composable
fun AppNavigation(
    onRequestNotification: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.DASHBOARD) {
        composable(Routes.DASHBOARD) {
            DashboardRoute(
                onOpenIndicator = { territoryId, type -> nav.navigate(Routes.indicator(territoryId, type)) },
                onOpenAlert = { nav.navigate(Routes.alert(it)) },
                onOpenHistory = { nav.navigate(Routes.HISTORY) },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                onRequestNotification = onRequestNotification,
                onOpenNotificationSettings = onOpenNotificationSettings,
            )
        }
        composable(
            route = Routes.INDICATOR,
            arguments = listOf(
                navArgument("territoryId") { type = NavType.StringType },
                navArgument("type") { type = NavType.StringType },
            ),
        ) {
            IndicatorRoute(
                onBack = { nav.popBackStack() },
                onOpenAlert = { nav.navigate(Routes.alert(it)) },
            )
        }
        composable(
            route = Routes.ALERT,
            arguments = listOf(navArgument("alertId") { type = NavType.StringType }),
        ) {
            AlertRoute(
                onBack = { nav.popBackStack() },
                onOpenHistory = { nav.navigate(Routes.HISTORY) },
            )
        }
        composable(Routes.HISTORY) {
            HistoryRoute(
                onBack = { nav.popBackStack() },
                onOpenAlert = { nav.navigate(Routes.alert(it)) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsRoute(
                onBack = { nav.popBackStack() },
                onRequestNotification = onRequestNotification,
                onOpenNotificationSettings = onOpenNotificationSettings,
            )
        }
    }
}
