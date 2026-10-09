package ro.ddnostalgia.duelmastersinventory.features.cards.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import ro.ddnostalgia.duelmastersinventory.nav.Routes
import ro.ddnostalgia.duelmastersinventory.shared.data.cards.model.CardsGroupBy
import ro.ddnostalgia.duelmastersinventory.shared.data.cards.model.CardsSortBy
import ro.ddnostalgia.duelmastersinventory.shared.data.cards.model.PrototypePrintsFilterParams
import ro.ddnostalgia.duelmastersinventory.shared.data.preferences.GRID_COLUMNS_MAX
import ro.ddnostalgia.duelmastersinventory.shared.data.preferences.GRID_COLUMNS_MIN
import ro.ddnostalgia.duelmastersinventory.shared.ui.components.cards.CardPrototypePrintsFilterBox
import ro.ddnostalgia.duelmastersinventory.shared.ui.components.cards.CardsGrid
import ro.ddnostalgia.duelmastersinventory.shared.ui.components.core.AppBottomSheet
import ro.ddnostalgia.duelmastersinventory.shared.utils.types.Civilization

@Composable
fun CardsListScreen(
    navController: NavHostController,
    viewModel: CardsListScreenViewModel = hiltViewModel()
) {
    val cards by viewModel.cards.collectAsState()
    val filterParams by viewModel.filterParams.collectAsState()
    val gridColumns by viewModel.gridColumns.collectAsState()
    val groupBy by viewModel.groupBy.collectAsState()
    val sortBy by viewModel.sortBy.collectAsState()
    val sortAscending by viewModel.sortAscending.collectAsState()

    var showViewOptions by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        CardPrototypePrintsFilterBox(
            value = filterParams,
            onValueChange = { viewModel.setFilterParams(it) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            ViewOptionsButton(onClick = { showViewOptions = true })
        }

        ActiveFilterChips(
            value = filterParams,
            onValueChange = { viewModel.setFilterParams(it) },
        )

        CardsGrid(
            cards = cards,
            columns = gridColumns,
            groupBy = groupBy,
            sortBy = sortBy,
            sortAscending = sortAscending,
            onItemClick = {
                val route = Routes.CardView
                    .createRoute(it.prototype.id, it.prints.firstOrNull()?.id)
                navController.navigate(route)
            },
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
        )
    }

    if (showViewOptions) {
        CardsViewOptionsSheet(
            onDismissRequest = { showViewOptions = false },
            columns = gridColumns,
            onColumnsChange = { viewModel.setGridColumns(it) },
            groupBy = groupBy,
            onGroupByChange = { viewModel.setGroupBy(it) },
            sortBy = sortBy,
            onSortByChange = { viewModel.setSortBy(it) },
            sortAscending = sortAscending,
            onSortAscendingChange = { viewModel.setSortAscending(it) },
        )
    }
}

/**
 * "N cards / M unique" read-out shown in the DrawerScaffold top bar for this screen (spec `2a`).
 * Backed by the same real totals as the drawer's own CARDS/UNIQUE stat tiles (unaffected by the
 * current search/filter selection). Rendered from [AppNavHost] via [DrawerScaffold]'s `actions`
 * slot; declared here (rather than inline in the nav graph) so the stat-fetching stays with the
 * screen it describes. Resolves to the same [CardsListScreenViewModel] instance as the rest of
 * this screen: both live inside the same NavBackStackEntry-scoped composable subtree.
 */
@Composable
fun CardsListScreenTopBarStats(viewModel: CardsListScreenViewModel = hiltViewModel()) {
    val stats by viewModel.stats.collectAsState()
    Column(
        horizontalAlignment = Alignment.End,
        modifier = Modifier.padding(end = 14.dp),
    ) {
        Text(
            "${stats.totalCount} cards",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
        )
        Text(
            "${stats.uniqueCount} unique",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
        )
    }
}

/**
 * Removable chips summarizing the currently-active filters, below the search/density/filter row
 * (spec `2a`). Purely a visual read of [PrototypePrintsFilterParams] already held by the
 * screen's filter state - clearing a chip just re-applies the same params with that one field
 * reset, via the existing [onValueChange] callback already wired to
 * [CardPrototypePrintsFilterBoxViewModel]/the filter dialog. No new state.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActiveFilterChips(
    value: PrototypePrintsFilterParams,
    onValueChange: (PrototypePrintsFilterParams) -> Unit,
) {
    val hasAny = value.civilizations.isNotEmpty() || value.multicolor || value.rarities.isNotEmpty() ||
        value.types.isNotEmpty() || value.languages.isNotEmpty() || value.sets.isNotEmpty() ||
        value.countGreaterThan != null || value.countLowerThan != null

    if (!hasAny) return

    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        value.civilizations.forEach { civName ->
            val tint = Civilization.fromName(civName)?.color ?: MaterialTheme.colorScheme.primary
            FilterChip(civName, tint) {
                onValueChange(value.copy(civilizations = value.civilizations - civName))
            }
        }
        if (value.multicolor) {
            FilterChip("Multicolor", MaterialTheme.colorScheme.primary) {
                onValueChange(value.copy(multicolor = false))
            }
        }
        value.rarities.forEach { rarity ->
            FilterChip(rarity, MaterialTheme.colorScheme.primary) {
                onValueChange(value.copy(rarities = value.rarities - rarity))
            }
        }
        value.types.forEach { type ->
            FilterChip(type, MaterialTheme.colorScheme.primary) {
                onValueChange(value.copy(types = value.types - type))
            }
        }
        value.languages.forEach { lang ->
            FilterChip(lang, MaterialTheme.colorScheme.primary) {
                onValueChange(value.copy(languages = value.languages - lang))
            }
        }
        value.sets.forEach { set ->
            FilterChip(set, MaterialTheme.colorScheme.primary) {
                onValueChange(value.copy(sets = value.sets - set))
            }
        }
        if (value.countGreaterThan != null || value.countLowerThan != null) {
            val label = buildString {
                append("Copies")
                value.countGreaterThan?.let { append(" ≥$it") }
                value.countLowerThan?.let { append(" ≤$it") }
            }
            FilterChip(label, MaterialTheme.colorScheme.primary) {
                onValueChange(value.copy(countGreaterThan = null, countLowerThan = null))
            }
        }
    }
}

@Composable
private fun FilterChip(label: String, tint: Color, onClear: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(tint.copy(alpha = 0.16f))
            .border(1.dp, tint.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClear)
            .padding(horizontal = 11.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = tint)
        Text(
            "✕",
            style = MaterialTheme.typography.labelMedium,
            color = tint,
            modifier = Modifier.padding(start = 5.dp),
        )
    }
}

/** Opens [CardsViewOptionsSheet]; replaces the old inline 3-or-5-per-row density toggle. */
@Composable
private fun ViewOptionsButton(onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(end = 10.dp)
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.07f))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.16f), CircleShape)
            .clickable(onClick = onClick),
    ) {
        Icon(
            Icons.Filled.GridView,
            contentDescription = "View options",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
    }
}

/**
 * Bottom sheet for the cards grid's view options: how many cards per row, whether to group
 * by print language+set, and how to sort cards within each group. Every change applies
 * immediately (backed by [ro.ddnostalgia.duelmastersinventory.shared.data.preferences.UserPreferencesRepository]),
 * so there's no separate "Apply" step.
 */
@Composable
private fun CardsViewOptionsSheet(
    onDismissRequest: () -> Unit,
    columns: Int,
    onColumnsChange: (Int) -> Unit,
    groupBy: CardsGroupBy,
    onGroupByChange: (CardsGroupBy) -> Unit,
    sortBy: CardsSortBy,
    onSortByChange: (CardsSortBy) -> Unit,
    sortAscending: Boolean,
    onSortAscendingChange: (Boolean) -> Unit,
) {
    AppBottomSheet(onDismissRequest = onDismissRequest) {
        Text(
            text = "View",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 6.dp, bottom = 4.dp),
        )

        ViewOptionsSectionHeader("Cards per row")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (GRID_COLUMNS_MIN..GRID_COLUMNS_MAX).forEach { count ->
                ViewOptionChip(
                    label = "$count",
                    selected = columns == count,
                    modifier = Modifier.weight(1f),
                    onClick = { onColumnsChange(count) },
                )
            }
        }

        ViewOptionsSectionHeader("Group by")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ViewOptionChip(
                label = "None",
                selected = groupBy == CardsGroupBy.NONE,
                modifier = Modifier.weight(1f),
                onClick = { onGroupByChange(CardsGroupBy.NONE) },
            )
            ViewOptionChip(
                label = "Language & Set",
                selected = groupBy == CardsGroupBy.LANGUAGE_SET,
                modifier = Modifier.weight(1f),
                onClick = { onGroupByChange(CardsGroupBy.LANGUAGE_SET) },
            )
        }

        ViewOptionsSectionHeader("Sort by")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ViewOptionChip(
                label = "None",
                selected = sortBy == CardsSortBy.NONE,
                modifier = Modifier.weight(1f),
                onClick = { onSortByChange(CardsSortBy.NONE) },
            )
            ViewOptionChip(
                label = "Rarity",
                selected = sortBy == CardsSortBy.RARITY,
                modifier = Modifier.weight(1f),
                onClick = { onSortByChange(CardsSortBy.RARITY) },
            )
            ViewOptionChip(
                label = "Power",
                selected = sortBy == CardsSortBy.POWER,
                modifier = Modifier.weight(1f),
                onClick = { onSortByChange(CardsSortBy.POWER) },
            )
        }

        ViewOptionsSectionHeader("Sort direction")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ViewOptionChip(
                label = "Ascending",
                selected = sortAscending,
                modifier = Modifier.weight(1f),
                onClick = { onSortAscendingChange(true) },
            )
            ViewOptionChip(
                label = "Descending",
                selected = !sortAscending,
                modifier = Modifier.weight(1f),
                onClick = { onSortAscendingChange(false) },
            )
        }

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun ViewOptionsSectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
    )
}

@Composable
private fun ViewOptionChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
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
            .padding(horizontal = 8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
            maxLines = 1,
        )
    }
}
