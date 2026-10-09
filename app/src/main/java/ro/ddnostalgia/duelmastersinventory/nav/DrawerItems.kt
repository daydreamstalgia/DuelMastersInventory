package ro.ddnostalgia.duelmastersinventory.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.ui.graphics.vector.ImageVector

class DrawerItem(
    val label: String,
    val route: String,
    val icon: ImageVector
)

val DrawerItems = listOf(
    DrawerItem("Cards", Routes.CardList.route, Icons.Filled.Style),
    DrawerItem("Transactions", Routes.TransactionsList.route, Icons.Filled.SwapHoriz),
    DrawerItem("Actors", Routes.ActorsList.route, Icons.Filled.People),
    DrawerItem("Spreadsheets", Routes.Spreadsheets.route, Icons.Filled.TableChart),
    DrawerItem("Statistics", Routes.Statistics.route, Icons.Filled.BarChart),
    DrawerItem("Sets", Routes.Sets.route, Icons.Filled.Inventory2),
    DrawerItem("Settings", Routes.Settings.route, Icons.Filled.Settings),
)
