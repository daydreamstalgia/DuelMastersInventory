package ro.daydreamstalgia.duelmastersinventory.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.ui.graphics.vector.ImageVector
import ro.daydreamstalgia.duelmastersinventory.shared.utils.constants.FeatureFlags

class DrawerItem(
    val label: String,
    val route: String,
    val icon: ImageVector
)

val DrawerItems = listOfNotNull(
    DrawerItem("Cards", Routes.CardList.route, Icons.Filled.Style),
    DrawerItem("Transactions", Routes.TransactionsList.route, Icons.Filled.SwapHoriz),
    if (FeatureFlags.ACTORS) DrawerItem("Actors", Routes.ActorsList.route, Icons.Filled.People) else null,
    if (FeatureFlags.GOOGLE_SHEETS) DrawerItem("Spreadsheets", Routes.Spreadsheets.route, Icons.Filled.TableChart) else null,
    if (FeatureFlags.STATISTICS) DrawerItem("Statistics", Routes.Statistics.route, Icons.Filled.BarChart) else null,
    DrawerItem("Sets", Routes.Sets.route, Icons.Filled.Inventory2),
    DrawerItem("Settings", Routes.Settings.route, Icons.Filled.Settings),
)
