package ro.daydreamstalgia.duelmastersinventory.nav

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import ro.daydreamstalgia.duelmastersinventory.features.actors.ui.screens.ActorEditScreen
import ro.daydreamstalgia.duelmastersinventory.features.actors.ui.screens.ActorViewScreen
import ro.daydreamstalgia.duelmastersinventory.features.actors.ui.screens.ActorsScreen
import ro.daydreamstalgia.duelmastersinventory.features.actors.ui.screens.ActorsScreenTopBarStats
import ro.daydreamstalgia.duelmastersinventory.features.cards.ui.screens.CardViewScreen
import ro.daydreamstalgia.duelmastersinventory.features.cards.ui.screens.CardsListScreen
import ro.daydreamstalgia.duelmastersinventory.features.cards.ui.screens.CardsListScreenTopBarStats
import ro.daydreamstalgia.duelmastersinventory.features.export.ui.screens.ExportToSpreadsheetsScreen
import ro.daydreamstalgia.duelmastersinventory.features.export.ui.screens.SpreadsheetConfigEditScreen
import ro.daydreamstalgia.duelmastersinventory.features.export.ui.screens.SpreadsheetConfigViewScreen
import ro.daydreamstalgia.duelmastersinventory.features.onboarding.ui.screens.OnboardingScreen
import ro.daydreamstalgia.duelmastersinventory.features.settings.ui.screens.SettingsScreen
import ro.daydreamstalgia.duelmastersinventory.features.sets.ui.screens.SetsScreen
import ro.daydreamstalgia.duelmastersinventory.features.sets.ui.screens.SetsScreenTopBarActions
import ro.daydreamstalgia.duelmastersinventory.features.statistics.ui.screens.StatisticsScreen
import ro.daydreamstalgia.duelmastersinventory.features.transactions.ui.screens.CardScanScreen
import ro.daydreamstalgia.duelmastersinventory.features.transactions.ui.screens.TransactedCardsEditScreen
import ro.daydreamstalgia.duelmastersinventory.features.transactions.ui.screens.TransactionEditScreen
import ro.daydreamstalgia.duelmastersinventory.features.transactions.ui.screens.TransactionViewScreen
import ro.daydreamstalgia.duelmastersinventory.features.transactions.ui.screens.TransactionsScreen
import ro.daydreamstalgia.duelmastersinventory.shared.ui.scaffolds.BackScaffold
import ro.daydreamstalgia.duelmastersinventory.shared.ui.scaffolds.DrawerScaffold
import ro.daydreamstalgia.duelmastersinventory.shared.utils.constants.FeatureFlags

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Routes.CardList.route
) {
    @Composable fun NavDrawerScaffold(
        title: String,
        actions: (@Composable RowScope.() -> Unit)? = null,
        content: @Composable () -> Unit,
    ) {
        DrawerScaffold(title, navController, actions, content)
    }

    NavHost(navController = navController, startDestination = startDestination) {

        val ScaffoldType = object  {
            val NONE = 0;
            val DRAWER = 1;
            val BACK = 2;
        }

        fun navComposable(
            route: Routes,
            title: String,
            scaffold: Int = ScaffoldType.NONE,
            drawerActions: (@Composable RowScope.() -> Unit)? = null,
            screen: @Composable (screenConfig: ScreenConfig?)->Unit
        ) {
            composable(
                route = route.route,
                arguments = route.args.map{ navArgument(it.key) {
                    type= it.value.first

                    if(it.value.second) {
                        if(type == NavType.IntType) {
                            defaultValue = -1
                        } else {
                            nullable = true
                            defaultValue = null
                        }
                    }
                } }.toList(),
            ) {
                val screenConfig = remember { ScreenConfig() }

                val wrapper: @Composable (@Composable () -> Unit) -> Unit = when (scaffold) {
                    ScaffoldType.DRAWER -> { content ->
                        NavDrawerScaffold(title = title, actions = drawerActions) {
                            content()
                        }
                    }
                    ScaffoldType.BACK -> { content ->
                        val dropdownActions = screenConfig.actions.collectAsState()
                        val onBackOverride = screenConfig.onBack.collectAsState()
                        BackScaffold(
                            title = title,
                            onBack = { onBackOverride.value?.invoke() ?: navController.navigateUp() },
                            dropdownActions = dropdownActions.value,
                            ) {
                            content()
                        }
                    }

                    else -> { content -> content() }
                }


                wrapper { screen(screenConfig) }
            }
        }

        navComposable(
            Routes.CardList,
            "Collection",
            ScaffoldType.DRAWER,
            drawerActions = { CardsListScreenTopBarStats() },
        ) {
            CardsListScreen(navController = navController)
        }

        navComposable(Routes.CardView, "Card View", ScaffoldType.BACK) {
             CardViewScreen(navController = navController)
        }

        navComposable(Routes.TransactionsList, "Transactions", ScaffoldType.DRAWER) {
            TransactionsScreen(navController = navController)
        }

        navComposable(Routes.TransactionView, "Transaction", ScaffoldType.BACK) { screenConfig ->
            TransactionViewScreen(navController = navController, screenConfig = screenConfig)
        }

        navComposable(Routes.TransactionCreate, "Create Transaction", ScaffoldType.BACK) {
            TransactionEditScreen(navController = navController)
        }

        navComposable(Routes.TransactionEdit, "Edit Transaction", ScaffoldType.BACK) {
            TransactionEditScreen(navController = navController)
        }
        
        navComposable(Routes.TransactedCardsEdit, "Transacted Cards", ScaffoldType.BACK) { screenConfig ->
            TransactedCardsEditScreen(navController = navController, screenConfig = screenConfig)
        }

        if (FeatureFlags.CARD_SCAN) {
            navComposable(Routes.CardScan, "Scan card", ScaffoldType.NONE) {
                CardScanScreen(navController = navController)
            }
        }

        if (FeatureFlags.ACTORS) {
            navComposable(
                Routes.ActorsList,
                "Actors",
                ScaffoldType.DRAWER,
                drawerActions = { ActorsScreenTopBarStats() },
            ) {
                ActorsScreen(navController = navController)
            }

            navComposable(Routes.ActorView, "Actor", ScaffoldType.BACK) { screenConfig ->
                ActorViewScreen(navController = navController, screenConfig = screenConfig)
            }

            navComposable(Routes.ActorCreate, "Create Actor", ScaffoldType.BACK) {
                ActorEditScreen(navController = navController)
            }

            navComposable(Routes.ActorEdit, "Edit Actor", ScaffoldType.BACK) {
                ActorEditScreen(navController = navController)
            }
        }

        if (FeatureFlags.GOOGLE_SHEETS) {
            navComposable(Routes.Spreadsheets, "Spreadsheets", ScaffoldType.DRAWER) {
                ExportToSpreadsheetsScreen(navController = navController)
            }

            navComposable(Routes.SpreadsheetConfigCreate, "Create Spreadsheet Config", ScaffoldType.BACK) {
                SpreadsheetConfigEditScreen(navController = navController)
            }

            navComposable(Routes.SpreadsheetConfigEdit, "Edit Spreadsheet Config", ScaffoldType.BACK) {
                SpreadsheetConfigEditScreen(navController = navController)
            }

            navComposable(Routes.SpreadsheetConfigView, "Spreadsheet", ScaffoldType.BACK) { screenConfig ->
                SpreadsheetConfigViewScreen(navController = navController, screenConfig = screenConfig)
            }
        }

        navComposable(Routes.Statistics, "Statistics", ScaffoldType.DRAWER) {
            StatisticsScreen(navController = navController)
        }

        navComposable(
            Routes.Sets,
            "Sets",
            ScaffoldType.DRAWER,
            drawerActions = { SetsScreenTopBarActions() },
        ) {
            SetsScreen(navController = navController)
        }

        navComposable(Routes.Settings, "Settings", ScaffoldType.DRAWER) {
            SettingsScreen(navController = navController)
        }

        navComposable(Routes.Onboarding, "Welcome", ScaffoldType.NONE) {
            OnboardingScreen(navController = navController)
        }
    }
}

