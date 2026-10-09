package ro.daydreamstalgia.duelmastersinventory.features.cards.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import ro.daydreamstalgia.duelmastersinventory.nav.Routes
import ro.daydreamstalgia.duelmastersinventory.shared.data.transactions.model.OwnedCopyProvenance
import ro.daydreamstalgia.duelmastersinventory.shared.ui.brushes.CivilizationBrush
import ro.daydreamstalgia.duelmastersinventory.shared.ui.components.cards.CardPrintsImageCarousel
import ro.daydreamstalgia.duelmastersinventory.shared.ui.components.cards.CivilizationBand
import ro.daydreamstalgia.duelmastersinventory.shared.ui.components.core.AmountText
import ro.daydreamstalgia.duelmastersinventory.shared.ui.components.core.ChannelIcon
import ro.daydreamstalgia.duelmastersinventory.shared.ui.components.core.ConditionChip
import ro.daydreamstalgia.duelmastersinventory.shared.ui.components.utils.AbilityKeywordIcon
import ro.daydreamstalgia.duelmastersinventory.shared.ui.components.utils.RarityIcon
import ro.daydreamstalgia.duelmastersinventory.shared.ui.theme.RaceLabelStyle
import ro.daydreamstalgia.duelmastersinventory.shared.utils.types.Civilization

// Ad hoc "inbound" accent reused across the app's other money/meta read-outs (e.g.
// DrawerScaffold/TransactionViewScreen's own local literal) - there's no dedicated theme token.
private val InboundColor = Color(0xFFF19087)

@Composable
fun CardViewScreen(
    navController: NavController,
    viewModel: CardViewScreenViewModel = hiltViewModel()
) {
    val card by viewModel.card.collectAsState()
    val ownedCopies by viewModel.ownedCopies.collectAsState()
    val queryPrint by viewModel.queryPrint.collectAsState()
    val abilityKeywords by viewModel.abilityKeywords.collectAsState()

    if(card == null) {
        Text("Card not found")
        return
    }

    val prints = card!!.prints
    val prototype = card!!.prototype
    val rarity = queryPrint?.rarity ?: prints.firstOrNull()?.rarity

    val bgColor = MaterialTheme.colorScheme.background

    val brush = CivilizationBrush(prototype.civilization, bgColor)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val screenHeight = this.maxHeight;
        val maxHeight = screenHeight * 0.6f      // original image height
        val minHeight = screenHeight * 0.2f      // min shrunk height

        // Civilization glow is a header backdrop, not a full-page wash — bounded to the
        // (unshrunk) image zone so it doesn't bleed into the scrolled "YOUR COPIES" list
        // further down the same Column.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(maxHeight)
                .align(Alignment.TopCenter)
                .background(brush)
        )
        val maxShrink = with(LocalDensity.current) { (maxHeight - minHeight).toPx() }
        val scrollState = rememberScrollState()
        var shrinkOffset by remember { mutableStateOf(0f) }

        val nestedScrollConnection = remember {
            object : NestedScrollConnection {
                override fun onPreScroll(
                    available: Offset,
                    source: NestedScrollSource
                ): Offset {
                    val delta = available.y
                    val newOffset = (shrinkOffset - delta).coerceIn(0f, maxShrink)

                    val consumed = shrinkOffset - newOffset
                    shrinkOffset = newOffset
                    return Offset(0f, consumed) // consume scroll while shrinking
                }
            }
        }

        val imageHeight = with(LocalDensity.current) {
            (maxHeight.toPx() - shrinkOffset).toDp()
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection) // hook in
                .verticalScroll(scrollState)
                .padding(20.dp)
        ) {

            CardPrintsImageCarousel(
                prints,
                selectedPrint = queryPrint,
                modifier = Modifier.fillMaxWidth(),
                itemModifier = Modifier.height(imageHeight)
            )

            Text(
                prototype.name ?: "??",
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )

            if (prototype.type != "Spell" && !prototype.races.isNullOrBlank()) {
                val raceColor = Civilization.fromCombo(prototype.civilization).firstOrNull()?.color
                    ?: MaterialTheme.colorScheme.primary
                Text(
                    prototype.races.uppercase(),
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    style = RaceLabelStyle,
                    color = raceColor,
                    textAlign = TextAlign.Center
                )
            }

            Box(modifier = Modifier.padding(top = 10.dp)) {
                CivilizationBand(prototype.civilization)
            }

            // Mana / power / rarity tile row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.09f)),
                horizontalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                StatTile(modifier = Modifier.weight(1f)) {
                    AmountText(text = "${prototype.mana ?: "?"}", fontSize = 24.sp)
                    SectionLabel("MANA")
                }
                if (prototype.type != "Spell") {
                    StatTile(modifier = Modifier.weight(1f)) {
                        AmountText(text = "${prototype.power ?: "?"}", fontSize = 24.sp)
                        SectionLabel("POWER")
                    }
                }
                if (rarity != null) {
                    StatTile(modifier = Modifier.weight(1f)) {
                        RarityIcon(rarity, color = MaterialTheme.colorScheme.primary)
                        SectionLabel(rarity.uppercase())
                    }
                }
            }

            if (!prototype.text.isNullOrBlank()) {
                val abilityLines = prototype.text.split(';').map { it.trim() }.filter { it.isNotEmpty() }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(14.dp)
                ) {
                    SectionLabel("ABILITIES")
                    abilityLines.forEach { line ->
                        // Keyword icons ahead of the line they were matched in - see
                        // specs/0007-prototype-identity-normalization-ability-keywords.md.
                        val matchedKeywords = abilityKeywords.filter { line.contains(it.matchText, ignoreCase = true) }
                        Row(modifier = Modifier.padding(top = 6.dp)) {
                            matchedKeywords.forEach { keyword ->
                                Box(modifier = Modifier.padding(end = 6.dp)) {
                                    AbilityKeywordIcon(keyword.iconKey)
                                }
                            }
                            Text(line, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            val copyGroups = remember(ownedCopies) {
                ownedCopies
                    .groupBy { Triple(it.cardPrintId, it.condition, it.transactionId) }
                    .map { (_, copies) -> copies.first() to copies.size }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 4.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Text(
                    "YOUR COPIES".uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp)
                        .height(1.dp)
                        .background(
                            androidx.compose.ui.graphics.Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                    Color.Transparent,
                                )
                            )
                        ),
                )
                Text(
                    "${ownedCopies.size} across ${prints.size} print${if (prints.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = InboundColor,
                )
            }

            if (copyGroups.isEmpty()) {
                Text(
                    "None owned yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }

            copyGroups.forEach { (copy, count) ->
                val print = prints.firstOrNull { it.id == copy.cardPrintId }
                OwnedCopyRow(
                    print?.displayId() ?: "#${copy.cardPrintId}",
                    condition = copy.condition,
                    count = count,
                    copy = copy,
                    onClick = {
                        navController.navigate(Routes.TransactionView.createRoute(copy.transactionId))
                    },
                )
            }

            Box(modifier = Modifier.height(24.dp))
        } // Column
    } // BoxWithConstraints

}

@Composable
private fun StatTile(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.55f))
            .padding(vertical = 10.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
    ) {
        content()
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 5.dp),
    )
}

@Composable
private fun OwnedCopyRow(
    displayId: String,
    condition: String?,
    count: Int,
    copy: OwnedCopyProvenance,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .background(Color.White.copy(alpha = 0.055f), RoundedCornerShape(13.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        AmountText(
            text = "$count",
            fontSize = 19.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(24.dp),
        )
        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(displayId, style = MaterialTheme.typography.labelLarge)
                if (condition != null) {
                    Box(modifier = Modifier.padding(start = 6.dp)) {
                        ConditionChip(condition)
                    }
                }
            }
            Row(
                modifier = Modifier.padding(top = 4.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                ChannelIcon(copy.channel, modifier = Modifier.width(12.dp))
                Text(
                    "#${copy.transactionId} · ${copy.channel ?: "Unknown"} · ${copy.date}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 5.dp),
                )
            }
        }
        Text(
            "›",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

