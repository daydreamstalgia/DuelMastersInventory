package ro.daydreamstalgia.duelmastersinventory.features.actors.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import ro.daydreamstalgia.duelmastersinventory.R
import ro.daydreamstalgia.duelmastersinventory.nav.Routes
import ro.daydreamstalgia.duelmastersinventory.shared.data.actors.model.ActorsSortBy
import ro.daydreamstalgia.duelmastersinventory.shared.data.actors.utils.displayName
import ro.daydreamstalgia.duelmastersinventory.shared.ui.components.core.AmountText
import ro.daydreamstalgia.duelmastersinventory.shared.ui.components.core.AppBottomSheet
import ro.daydreamstalgia.duelmastersinventory.shared.ui.components.list.LazyList
import ro.daydreamstalgia.duelmastersinventory.shared.ui.components.transactions.INBOUND_ON_COLOR
import ro.daydreamstalgia.duelmastersinventory.shared.ui.components.transactions.OUTBOUND_ON_COLOR
import ro.daydreamstalgia.duelmastersinventory.shared.ui.scaffolds.FloatingAddButtonScaffold

/** Initials for the row/hero avatar, same derivation used by ActorViewScreen and ActorSelector. */
private fun initialsOf(text: String): String =
    text.split(" ", "/").filter { it.isNotBlank() }
        .take(2).joinToString("") { it.first().uppercase() }
        .ifBlank { "?" }

@Composable
fun ActorsScreen(
    navController: NavController,
    viewModel: ActorsScreenViewModel = hiltViewModel()
) {
    val actors by viewModel.displayedActors.collectAsState()
    val search by viewModel.search.collectAsState()
    val sortBy by viewModel.sortBy.collectAsState()
    val sortAscending by viewModel.sortAscending.collectAsState()
    val platformFilter by viewModel.platformFilter.collectAsState()
    val availablePlatforms by viewModel.availablePlatforms.collectAsState()
    val dealsSummaryByActorId by viewModel.dealsSummaryByActorId.collectAsState()

    var showSortFilterSheet by remember { mutableStateOf(false) }

    FloatingAddButtonScaffold(onClick = {
        navController.navigate(Routes.ActorCreate.route)
    }) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextField(
                    value = search ?: "",
                    onValueChange = { viewModel.setSearch(it.ifBlank { null }) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = {
                        Text(
                            "Name or alias…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyMedium,
                    shape = RoundedCornerShape(20.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.07f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.07f),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        cursorColor = MaterialTheme.colorScheme.primary,
                    ),
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), CircleShape)
                        .clickable { showSortFilterSheet = true },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_filter),
                        contentDescription = "Sort & filter",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            if (platformFilter != null) {
                ActorsActiveFilterChips(platformFilter, onClearPlatform = { viewModel.setPlatformFilter(null) })
            }

            Box(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                LazyList(
                    items = actors,
                    key = { it.actor.id },
                    emptyTemplate = {
                        Box(it) {
                            Text(
                                "No actors",
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }
                ) { item, modifier ->
                    val name = item.displayName()
                    val initials = initialsOf(name)
                    val primaryAlias = item.aliases.firstOrNull()
                    val extraAliases = item.aliases.size - 1
                    val summary = dealsSummaryByActorId[item.actor.id]
                    val netCost = summary?.netCost ?: 0.0
                    val dealsCount = summary?.dealsCount ?: 0

                    val amountColor = when {
                        netCost > 0 -> INBOUND_ON_COLOR
                        netCost < 0 -> OUTBOUND_ON_COLOR
                        else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    }

                    Row(
                        modifier = modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(13.dp))
                            .clickable {
                                navController.navigate(Routes.ActorView.createRoute(item.actor.id))
                            }
                            .padding(11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(initials, color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelLarge)
                        }
                        Column(modifier = Modifier.weight(1f).padding(start = 11.dp)) {
                            Text(name, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (primaryAlias != null) {
                                Text(
                                    buildString {
                                        append(primaryAlias.username)
                                        if (extraAliases > 0) append(" · +$extraAliases more")
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 3.dp),
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            AmountText(text = "%.0f".format(netCost), color = amountColor, fontSize = 14.sp)
                            Text(
                                "$dealsCount deal${if (dealsCount == 1) "" else "s"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 5.dp),
                            )
                        }
                    }
                }
            }
        }

        if (showSortFilterSheet) {
            ActorsSortFilterSheet(
                onDismissRequest = { showSortFilterSheet = false },
                sortBy = sortBy,
                onSortByChange = { viewModel.setSortBy(it) },
                sortAscending = sortAscending,
                onSortAscendingChange = { viewModel.setSortAscending(it) },
                platformFilter = platformFilter,
                availablePlatforms = availablePlatforms,
                onPlatformFilterChange = { viewModel.setPlatformFilter(it) },
            )
        }
    }
}

/**
 * "N people" read-out shown in the DrawerScaffold top bar for this screen, matching the
 * CardsListScreenTopBarStats pattern. Just the size of the already-loaded actors list, not a
 * separate count query.
 */
@Composable
fun ActorsScreenTopBarStats(viewModel: ActorsScreenViewModel = hiltViewModel()) {
    val actors by viewModel.actors.collectAsState()
    Text(
        "${actors.size} people",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
        modifier = Modifier.padding(end = 14.dp),
    )
}

/** Removable chip for the active platform filter, same visual convention as CardsListScreen's ActiveFilterChips. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActorsActiveFilterChips(platformFilter: String?, onClearPlatform: () -> Unit) {
    if (platformFilter == null) return

    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                .clickable(onClick = onClearPlatform)
                .padding(horizontal = 11.dp),
        ) {
            Text(platformFilter, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(
                "✕",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 5.dp),
            )
        }
    }
}

/** Sort + platform-filter bottom sheet, following CardsListScreen's CardsViewOptionsSheet chip pattern. */
@Composable
private fun ActorsSortFilterSheet(
    onDismissRequest: () -> Unit,
    sortBy: ActorsSortBy,
    onSortByChange: (ActorsSortBy) -> Unit,
    sortAscending: Boolean,
    onSortAscendingChange: (Boolean) -> Unit,
    platformFilter: String?,
    availablePlatforms: List<String>,
    onPlatformFilterChange: (String?) -> Unit,
) {
    AppBottomSheet(onDismissRequest = onDismissRequest) {
        Text(
            text = "Sort & filter",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 6.dp, bottom = 4.dp),
        )

        Text(
            "SORT BY",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
            modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SortFilterChip("Name", sortBy == ActorsSortBy.NAME, Modifier.weight(1f)) { onSortByChange(ActorsSortBy.NAME) }
            SortFilterChip("Deals", sortBy == ActorsSortBy.DEALS_COUNT, Modifier.weight(1f)) { onSortByChange(ActorsSortBy.DEALS_COUNT) }
            SortFilterChip("Net EUR", sortBy == ActorsSortBy.NET_EUR, Modifier.weight(1f)) { onSortByChange(ActorsSortBy.NET_EUR) }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            SortFilterChip("Ascending", sortAscending, Modifier.weight(1f)) { onSortAscendingChange(true) }
            SortFilterChip("Descending", !sortAscending, Modifier.weight(1f)) { onSortAscendingChange(false) }
        }

        if (availablePlatforms.isNotEmpty()) {
            Text(
                "PLATFORM",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                availablePlatforms.forEach { platform ->
                    SortFilterChip(
                        label = platform,
                        selected = platformFilter == platform,
                        onClick = { onPlatformFilterChange(if (platformFilter == platform) null else platform) },
                    )
                }
            }
        }

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun SortFilterChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.05f))
            .border(
                1.dp,
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.1f),
                RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
            maxLines = 1,
        )
    }
}
