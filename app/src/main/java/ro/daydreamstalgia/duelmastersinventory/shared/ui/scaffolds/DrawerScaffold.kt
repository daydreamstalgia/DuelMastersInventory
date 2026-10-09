package ro.daydreamstalgia.duelmastersinventory.shared.ui.scaffolds

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.launch
import ro.daydreamstalgia.duelmastersinventory.nav.DrawerItem
import ro.daydreamstalgia.duelmastersinventory.nav.DrawerItems
import ro.daydreamstalgia.duelmastersinventory.shared.ui.components.core.AmountText
import ro.daydreamstalgia.duelmastersinventory.shared.ui.google.GoogleAuthContextData
import ro.daydreamstalgia.duelmastersinventory.shared.ui.google.rememberGoogleAuthContext
import ro.daydreamstalgia.duelmastersinventory.shared.utils.constants.FeatureFlags
import ro.daydreamstalgia.duelmastersinventory.shared.ui.theme.RaceLabelStyle

// Ad hoc accent for the NET EUR figure, matching the "inbound"/amount accent used across the
// app's other money read-outs (e.g. StatisticsScreen/TransactionsScreen's own local literal) -
// there's no dedicated theme token for it.
private val NetEuroColor = Color(0xFFF19087)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerScaffold(
    title: String,
    navController: NavController,
    actions: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.background,
                drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
            ) {
                DrawerPanelContent(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        scope.launch { drawerState.close() }
                        if (route != currentRoute) {
                            navController.navigate(route)
                        }
                    },
                )
            }
        }
    ) {

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                    actions = {
                        actions?.invoke(this)
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
        ) { innerPadding ->
            // consumeWindowInsets + imePadding: on Android 15+ the app is drawn edge-to-edge and the
            // window no longer resizes for the keyboard, so content shrinks itself to stay above it
            // (without double-counting the nav-bar inset already in innerPadding). No-op on older versions.
            Surface(
                modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding).imePadding(),
                color = MaterialTheme.colorScheme.background,
            ) {
                content()
            }
        }
    }
}

/** Drawer panel body: header block, 3-chip stats row, nav rows, account row pinned to bottom. */
@Composable
private fun DrawerPanelContent(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    viewModel: DrawerScaffoldViewModel = hiltViewModel(),
) {
    val stats by viewModel.stats.collectAsState()
    val auth = if (FeatureFlags.GOOGLE_SHEETS) rememberGoogleAuthContext() else null

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(300.dp)
            .padding(vertical = 10.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text(
                "Duel Masters",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                "INVENTORY",
                style = RaceLabelStyle,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DrawerStatTile("CARDS", stats.ownedCardsCount.toString(), Modifier.weight(1f))
            DrawerStatTile("UNIQUE", stats.uniquePrototypeCount.toString(), Modifier.weight(1f))
            DrawerStatTile(
                "NET EUR",
                "%.0f".format(stats.netEuro),
                Modifier.weight(1f),
                valueColor = NetEuroColor,
            )
        }

        Column(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .padding(top = 24.dp)
                .weight(1f),
        ) {
            DrawerItems.forEach { item: DrawerItem ->
                val selected = currentRoute == item.route
                DrawerNavRow(
                    label = item.label,
                    selected = selected,
                    onClick = { onNavigate(item.route) },
                )
            }
        }

        auth?.let { AccountRow(it) }
    }
}

@Composable
private fun DrawerStatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onBackground,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(11.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .padding(horizontal = 11.dp, vertical = 10.dp),
    ) {
        AmountText(text = value, fontSize = 18.sp, color = valueColor)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun DrawerNavRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .background(
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
                    shape = CircleShape,
                )
        )
        Spacer(Modifier.width(13.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
        )
    }
}

/**
 * Account row pinned to the bottom of the drawer. Real signed-in email via
 * [rememberGoogleAuthContext]; there's no cheap existing source for a "last synced" timestamp
 * (would need new sync-tracking plumbing outside this slice), so that line is simply omitted
 * rather than showing an invented value.
 */
@Composable
private fun AccountRow(auth: GoogleAuthContextData) {
    val email = auth.email

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = email?.take(1)?.uppercase() ?: "?",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = email ?: "Not signed in",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
