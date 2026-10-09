package ro.ddnostalgia.duelmastersinventory.shared.ui.components.cards

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import ro.ddnostalgia.duelmastersinventory.shared.data.cards.model.CardPrint
import ro.ddnostalgia.duelmastersinventory.shared.data.cards.model.CardPrototypeWithPrints
import ro.ddnostalgia.duelmastersinventory.shared.data.cards.model.CardPrototypeWithPrintsAndCount
import ro.ddnostalgia.duelmastersinventory.shared.data.cards.model.CardSetOrder
import ro.ddnostalgia.duelmastersinventory.shared.data.cards.model.CardsGroupBy
import ro.ddnostalgia.duelmastersinventory.shared.data.cards.model.CardsSortBy
import ro.ddnostalgia.duelmastersinventory.shared.ui.components.core.AmountText
import ro.ddnostalgia.duelmastersinventory.shared.ui.components.list.LazyGridWithHeaders
import ro.ddnostalgia.duelmastersinventory.shared.ui.components.list.LazyList

@Composable
fun CardsList(
    cards: List<CardPrototypeWithPrints>,
    onCardClick: ((CardPrototypeWithPrints) -> Unit)? = null,
) {
    LazyList(
        items = cards,
        key = { it.prints.firstOrNull()?.displayId() ?: "" },
        gridView = true,
    ) { card, modifier ->
        card.prints.firstOrNull()?.let {cardPrint ->
            CardPrintImage(
                cardPrint,
                modifier.clickable { onCardClick?.invoke(card)  }
            ) {
                if(card.prints.size > 1) {
                    Text(
                        text = "+${card.prints.size-1} prints",
                        color = Color(0.0f, 0.7f, 1.0f, 1.0f),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
fun <T> CardsList(
    items: List<T>,
    key: ((T)->Any)?,
    cardPrintTransform: (T)->CardPrint?,
    onItemClick: ((T) -> Unit)? = null,
    customInfo: (@Composable (T)->Unit)? = null,
) {
    LazyList(
        items = items,
        key = key,
        gridView = true,
    ) { item, modifier ->
        cardPrintTransform(item)?.let {cardPrint ->
            CardPrintImage(
                cardPrint,
                modifier.clickable { onItemClick?.invoke(item)  },
                customInfo = customInfo?.let { { customInfo(item) } }
            )
        }
    }
}

private sealed class CardGridRow {
    data class GroupHeader(
        val code: String,
        val name: String?,
        val owned: Int,
        val total: Int,
        val collapsed: Boolean,
    ) : CardGridRow()
    data class Item(val card: CardPrototypeWithPrintsAndCount) : CardGridRow()
}

private data class CardGroup(
    val code: String,
    val name: String?,
    val owned: Int,
    val total: Int,
    val items: List<CardPrototypeWithPrintsAndCount>,
)

private val RARITY_ORDER = listOf("Common", "Uncommon", "Rare", "Very Rare", "Super Rare")

private fun rarityRank(rarity: String?): Int {
    val idx = RARITY_ORDER.indexOf(rarity)
    return if (idx >= 0) idx else RARITY_ORDER.size
}

private fun powerValue(power: String?): Int = power?.filter { it.isDigit() }?.toIntOrNull() ?: Int.MAX_VALUE

private fun setGroupCode(language: String, set: String): String =
    if (language.isEmpty() && set.isEmpty()) "" else "$language $set"

private fun sortComparatorFor(sortBy: CardsSortBy): Comparator<CardPrototypeWithPrintsAndCount>? = when (sortBy) {
    CardsSortBy.NONE -> null
    CardsSortBy.RARITY -> compareBy { rarityRank(it.prints.firstOrNull()?.rarity) }
    CardsSortBy.POWER -> compareBy { powerValue(it.prototype.power) }
}

/**
 * Density-aware card grid, optionally grouped by print language+set with a full-span
 * "LANG SET ... owned/total" header row separating each group (spec `2a`), and optionally
 * sorted by rarity or power within each group (or across the whole grid when ungrouped).
 */
@Composable
fun CardsGrid(
    cards: List<CardPrototypeWithPrintsAndCount>,
    columns: Int,
    onItemClick: (CardPrototypeWithPrintsAndCount) -> Unit,
    modifier: Modifier = Modifier,
    groupBy: CardsGroupBy = CardsGroupBy.LANGUAGE_SET,
    sortBy: CardsSortBy = CardsSortBy.NONE,
    sortAscending: Boolean = true,
) {
    val groups = remember(cards, groupBy, sortBy, sortAscending) {
        val comparator = sortComparatorFor(sortBy)?.let { if (sortAscending) it else it.reversed() }

        val raw: List<Triple<String, String?, List<CardPrototypeWithPrintsAndCount>>> = when (groupBy) {
            CardsGroupBy.NONE -> listOf(Triple("", null, cards))
            CardsGroupBy.LANGUAGE_SET -> cards
                .groupBy { card ->
                    card.prints.firstOrNull()?.let { it.language to it.set } ?: ("" to "")
                }
                .toList()
                .sortedWith(compareBy(CardSetOrder.comparator) { it.first })
                .map { (key, group) ->
                    Triple(setGroupCode(key.first, key.second), CardSetOrder.displayName(key.first, key.second), group)
                }
        }

        raw.map { (code, name, group) ->
            val sorted = comparator?.let { group.sortedWith(it) } ?: group
            CardGroup(code, name, group.count { it.count > 0 }, group.size, sorted)
        }
    }

    // rememberSaveable (not remember) so both the collapse state and the grid's own scroll
    // position (gridState below) survive navigating away and back, e.g. tapping a card into
    // CardViewScreen and pressing back - Navigation-Compose keeps a per-destination saved-state
    // holder for exactly this, but only for state saved through rememberSaveable.
    var collapsedGroups by rememberSaveable(
        stateSaver = listSaver(
            save = { it.toList() },
            restore = { it.toSet() },
        )
    ) { mutableStateOf(emptySet<String>()) }

    val gridState = rememberLazyGridState()

    val rows = remember(groups, collapsedGroups) {
        groups.flatMap { group ->
            val collapsed = group.code in collapsedGroups
            val header = if (groupBy == CardsGroupBy.NONE) {
                emptyList()
            } else {
                listOf(CardGridRow.GroupHeader(group.code, group.name, group.owned, group.total, collapsed))
            }
            val items = if (collapsed) emptyList() else group.items.map { CardGridRow.Item(it) }
            header + items
        }
    }

    LazyGridWithHeaders(
        rows = rows,
        columns = columns,
        modifier = modifier,
        state = gridState,
        key = { row ->
            when (row) {
                is CardGridRow.GroupHeader -> "header-${row.code}"
                is CardGridRow.Item -> row.card.prints.firstOrNull()?.displayId() ?: row.card.prototype.id
            }
        },
        isHeader = { it is CardGridRow.GroupHeader },
        headerTemplate = { row ->
            val header = row as CardGridRow.GroupHeader
            GroupHeaderRow(header.code, header.name, header.owned, header.total, header.collapsed) {
                collapsedGroups = if (header.collapsed) {
                    collapsedGroups - header.code
                } else {
                    collapsedGroups + header.code
                }
            }
        },
    ) { row, itemModifier ->
        val item = (row as CardGridRow.Item).card
        item.prints.firstOrNull()?.let { cardPrint ->
            // Denser grids get a smaller, tighter count badge (spec `2a`: 20dp/11sp at 3-column,
            // 16dp/9sp at 5-column).
            val dense = columns > GRID_COLUMNS_DENSITY_THRESHOLD
            val badgeSize = if (dense) 16.dp else 20.dp
            val badgeFontSize = if (dense) 9.sp else 11.sp
            val stripeHeight = 2.dp

            Box(
                modifier = itemModifier
                    .aspectRatio(0.7f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .clickable { onItemClick(item) },
            ) {
                // Low columns => bigger cells; use the full-res asset so the card stays sharp.
                CardPrintImage(cardPrint, Modifier.fillMaxSize(), lowRes = dense)

                CivilizationBand(
                    item.prototype.civilization,
                    modifier = Modifier.align(Alignment.TopCenter),
                    height = stripeHeight,
                )

                if (item.count > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(3.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.85f))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                RoundedCornerShape(50),
                            )
                            .padding(horizontal = 4.dp)
                            .height(badgeSize),
                        contentAlignment = Alignment.Center,
                    ) {
                        AmountText(text = "${item.count}", fontSize = badgeFontSize, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

// Density threshold separating the compact (3-column) badge size from the dense (5-column) one;
// matches GRID_COLUMNS_COMPACT/GRID_COLUMNS_DENSE from UserPreferencesRepository.
private const val GRID_COLUMNS_DENSITY_THRESHOLD = 3

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GroupHeaderRow(code: String, name: String?, owned: Int, total: Int, collapsed: Boolean, onToggle: () -> Unit) {
    val rotation by animateFloatAsState(if (collapsed) -90f else 0f, label = "groupHeaderChevron")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 2.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.KeyboardArrowDown,
            contentDescription = if (collapsed) "Expand group" else "Collapse group",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(18.dp).rotate(rotation),
        )
        Text(
            text = code.uppercase(),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 2.dp),
        )
        if (name != null) {
            // Bus-sign marquee: name only scrolls if it doesn't fit the space left after
            // the fixed code/counter, so short names just sit still.
            Text(
                text = name.uppercase(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp)
                    .basicMarquee(iterations = Int.MAX_VALUE),
            )
        } else {
            Box(modifier = Modifier.weight(1f))
        }
        Text(text = "$owned", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        Text(
            text = "/$total",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f),
        )
    }
}
