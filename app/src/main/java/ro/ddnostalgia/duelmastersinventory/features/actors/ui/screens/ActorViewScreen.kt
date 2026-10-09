package ro.ddnostalgia.duelmastersinventory.features.actors.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch
import ro.ddnostalgia.duelmastersinventory.nav.Routes
import ro.ddnostalgia.duelmastersinventory.nav.ScreenConfig
import ro.ddnostalgia.duelmastersinventory.shared.data.actors.utils.displayName
import ro.ddnostalgia.duelmastersinventory.shared.data.transactions.model.Transaction
import ro.ddnostalgia.duelmastersinventory.shared.data.transactions.model.TransactionType
import ro.ddnostalgia.duelmastersinventory.shared.ui.components.core.AmountText
import ro.ddnostalgia.duelmastersinventory.shared.ui.components.core.AppBottomSheet
import ro.ddnostalgia.duelmastersinventory.shared.ui.components.core.AppConfirmDialog
import ro.ddnostalgia.duelmastersinventory.shared.ui.components.core.ChannelIcon
import ro.ddnostalgia.duelmastersinventory.shared.ui.components.transactions.INBOUND_COLOR
import ro.ddnostalgia.duelmastersinventory.shared.ui.components.transactions.INBOUND_ON_COLOR
import ro.ddnostalgia.duelmastersinventory.shared.ui.components.transactions.OUTBOUND_COLOR
import ro.ddnostalgia.duelmastersinventory.shared.ui.components.transactions.OUTBOUND_ON_COLOR

private val RECENT_TX_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)

/** Initials for the hero avatar, same derivation used by ActorsScreen/ActorSelector. */
private fun initialsOf(text: String): String =
    text.split(" ", "/").filter { it.isNotBlank() }
        .take(2).joinToString("") { it.first().uppercase() }
        .ifBlank { "?" }

@Composable
fun ActorViewScreen(
    navController: NavController,
    screenConfig: ScreenConfig? = null,
    viewModel: ActorViewScreenViewModel = hiltViewModel()
) {
    val actor by viewModel.actor.collectAsState()
    val canDelete by viewModel.canDelete.collectAsState()
    val dealsCount by viewModel.dealsCount.collectAsState()
    val netCost by viewModel.netCost.collectAsState()
    val cardsInCount by viewModel.cardsInCount.collectAsState()
    val recentTransactions by viewModel.recentTransactions.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showMergeSheet by remember { mutableStateOf(false) }
    var mergeTarget by remember { mutableStateOf<Pair<Int, String>?>(null) }

    if (actor == null) {
        return
    }

    LaunchedEffect(actor, canDelete) {
        val actions = mutableListOf<Pair<String, () -> Unit>>(
            "Edit actor" to {
                navController.navigate(Routes.ActorEdit.createRoute(actor!!.actor.id))
            },
            "Merge actor" to { showMergeSheet = true },
        )
        if (canDelete) {
            actions.add("Delete actor" to { showDeleteConfirm = true })
        }
        screenConfig?.setDropdownAction(actions)
    }

    val name = actor!!.displayName()
    val initials = initialsOf(name)

    // Positive net = more inbound (acquired) than outbound (disposed) EUR value, same sign
    // convention as TransactionsList's netCost calc - tinted red/inbound to match.
    val netTint = when {
        netCost > 0 -> INBOUND_COLOR
        netCost < 0 -> OUTBOUND_COLOR
        else -> null
    }
    val netOnTint = when {
        netCost > 0 -> INBOUND_ON_COLOR
        netCost < 0 -> OUTBOUND_ON_COLOR
        else -> MaterialTheme.colorScheme.onBackground
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.colorScheme.background)
                    ),
                    RoundedCornerShape(16.dp),
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(56.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(initials, color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleMedium)
            }
            Column(modifier = Modifier.padding(start = 14.dp)) {
                Text(name, style = MaterialTheme.typography.titleLarge)
                Text(
                    "${actor!!.aliases.size} alias${if (actor!!.aliases.size == 1) "" else "es"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatTile("DEALS", "$dealsCount", MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
            StatTile("CARDS IN", "$cardsInCount", MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
            StatTile(
                "NET EUR",
                "%.0f".format(netCost),
                netOnTint,
                Modifier.weight(1f),
                tintColor = netTint,
            )
        }

        if (actor!!.aliases.isNotEmpty()) {
            SectionHeaderRow("ALIASES")
            Column {
                actor!!.aliases.forEach { alias ->
                    val url = alias.url
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 7.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .then(
                                if (url != null) {
                                    Modifier.clickable {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    }
                                } else {
                                    Modifier
                                }
                            )
                            .padding(horizontal = 13.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ChannelIcon(alias.platform)
                        Column(modifier = Modifier.weight(1f).padding(start = 11.dp)) {
                            Text(alias.username, style = MaterialTheme.typography.labelLarge)
                            Text(alias.platform, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 3.dp))
                        }
                        if (url != null) {
                            Text(
                                "↗",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }

        if (recentTransactions.isNotEmpty()) {
            SectionHeaderRow("TRANSACTIONS", meta = "See all $dealsCount")
            Column {
                recentTransactions.forEach { transaction ->
                    RecentTransactionRow(transaction)
                }
            }
        }

    }

    if (showDeleteConfirm) {
        AppConfirmDialog(
            title = "Delete actor?",
            message = "This actor has no transactions and will be permanently removed.",
            confirmText = "Delete",
            onConfirm = {
                showDeleteConfirm = false
                coroutineScope.launch {
                    viewModel.deleteActor()
                    navController.navigate(Routes.ActorsList.route) {
                        popUpTo(navController.currentDestination?.id ?: return@navigate) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            },
            onDismiss = { showDeleteConfirm = false },
        )
    }

    if (showMergeSheet) {
        MergeActorSheet(
            excludeActorId = actor!!.actor.id,
            onDismiss = { showMergeSheet = false },
            onSelected = { targetActorId, targetName ->
                showMergeSheet = false
                mergeTarget = targetActorId to targetName
            }
        )
    }

    mergeTarget?.let { (targetActorId, targetName) ->
        AppConfirmDialog(
            title = "Merge into $targetName?",
            message = "$name's aliases and transactions will move to $targetName, and $name will be permanently removed. This can't be undone.",
            confirmText = "Merge",
            onConfirm = {
                mergeTarget = null
                coroutineScope.launch {
                    viewModel.mergeInto(targetActorId)
                    navController.navigate(Routes.ActorView.createRoute(targetActorId)) {
                        popUpTo(navController.currentDestination?.id ?: return@navigate) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            },
            onDismiss = { mergeTarget = null },
        )
    }
}

@Composable
private fun SectionHeaderRow(title: String, meta: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), Color.Transparent)
                    )
                ),
        )
        if (meta != null) {
            Text(meta, style = MaterialTheme.typography.labelMedium, color = INBOUND_ON_COLOR)
        }
    }
}

/**
 * One recent-deal row (spec `actor-view` TRANSACTIONS section): colored left edge + amount,
 * matching TransactionsList's row conventions. Not clickable to a filtered Transactions view -
 * TransactionFilterParams has no actorId field, and wiring that is out of scope for this pass.
 */
@Composable
private fun RecentTransactionRow(transaction: Transaction) {
    val edgeColor = when (transaction.type) {
        TransactionType.INBOUND -> INBOUND_COLOR
        TransactionType.OUTBOUND -> OUTBOUND_COLOR
    }
    val amountColor = when (transaction.type) {
        TransactionType.INBOUND -> INBOUND_ON_COLOR
        TransactionType.OUTBOUND -> OUTBOUND_ON_COLOR
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(13.dp))
            .padding(end = 12.dp, top = 11.dp, bottom = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .padding(start = 3.dp, end = 10.dp)
                .size(3.dp, 34.dp)
                .background(edgeColor, RoundedCornerShape(2.dp))
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "#${transaction.id} · ${transaction.description.ifBlank { transaction.type.toString() }}",
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${transaction.date.format(RECENT_TX_DATE_FORMAT)}${transaction.channel?.let { " · $it" } ?: ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
        AmountText(text = "%.0f".format(transaction.costEuro), color = amountColor, fontSize = 14.sp)
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
    tintColor: Color? = null,
) {
    Column(
        modifier = modifier
            .background(
                tintColor?.copy(alpha = 0.12f) ?: MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                RoundedCornerShape(12.dp),
            )
            .then(
                if (tintColor != null) {
                    Modifier.border(1.dp, tintColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 11.dp, vertical = 10.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        AmountText(text = value, color = valueColor, modifier = Modifier.padding(top = 7.dp))
    }
}

@Composable
private fun MergeActorSheet(
    excludeActorId: Int,
    onDismiss: () -> Unit,
    onSelected: (Int, String) -> Unit,
    viewModel: MergeActorViewModel = hiltViewModel()
) {
    val search by viewModel.search.collectAsState()

    LaunchedEffect(search) {
        viewModel.loadActors()
    }

    val actors by viewModel.actors.collectAsState()
    val candidates = remember(actors) { actors.filter { it.actor.id != excludeActorId } }

    AppBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = "Merge into…",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        TextField(
            value = search ?: "",
            onValueChange = { viewModel.setSearch(it.ifBlank { null }) },
            label = { Text("Search") },
            modifier = Modifier.fillMaxWidth()
        )

        LazyColumn(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
            items(items = candidates, key = { it.actor.id }) {
                Text(
                    it.displayName(),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelected(it.actor.id, it.displayName()) }
                        .padding(vertical = 12.dp)
                )
            }
        }
    }
}
