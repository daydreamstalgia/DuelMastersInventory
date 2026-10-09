package ro.daydreamstalgia.duelmastersinventory.shared.ui.components.transactions

import androidx.compose.foundation.background
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ro.daydreamstalgia.duelmastersinventory.shared.data.transactions.model.Transaction
import ro.daydreamstalgia.duelmastersinventory.shared.data.transactions.model.TransactionType
import ro.daydreamstalgia.duelmastersinventory.shared.ui.components.core.AmountText
import ro.daydreamstalgia.duelmastersinventory.shared.ui.components.core.ChannelIcon
import ro.daydreamstalgia.duelmastersinventory.shared.utils.constants.FeatureFlags

// Shared inbound/outbound accents (spec `--inbound`/`--inbound-edge`/`--outbound`/`--outbound-edge`).
// Reused by TransactionsScreen and TransactionViewScreen instead of forking their own copies.
val INBOUND_COLOR get() = Color(0xFFE4483A)
val INBOUND_ON_COLOR get() = Color(0xFFF19087)
val OUTBOUND_COLOR get() = Color(0xFF4FA45B)
val OUTBOUND_ON_COLOR get() = Color(0xFF7FD69B)

private sealed class TxRow {
    data class MonthHeader(val label: String, val netCost: Double) : TxRow()
    data class Item(val transaction: Transaction) : TxRow()
}

/** Transaction list grouped by month, with a signed in/out chip per row (spec `3b`). */
@Composable
fun TransactionsList(
    transactions: List<Transaction>,
    onTransactionClick: ((Transaction)->Unit)?,
    actorDisplay: (Transaction)->String = { it.channel ?: "" }
) {
    val rows = remember(transactions) {
        transactions
            .sortedByDescending { it.date }
            .groupBy { "${it.date.year}-${it.date.monthValue.toString().padStart(2, '0')}" }
            .flatMap { (_, group) ->
                val label = group.first().date.month.name + " " + group.first().date.year
                // Same sign convention as StatisticsDao.getTotalCost/netCostByActorId: INBOUND +, OUTBOUND -.
                val netCost = group.sumOf { if (it.type == TransactionType.INBOUND) it.costEuro else -it.costEuro }
                listOf(TxRow.MonthHeader(label, netCost)) + group.map { TxRow.Item(it) }
            }
    }

    LazyColumn(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        items(rows, key = {
            when (it) {
                is TxRow.MonthHeader -> "header-${it.label}"
                is TxRow.Item -> it.transaction.id
            }
        }) { row ->
            when (row) {
                is TxRow.MonthHeader -> MonthHeaderRow(row.label, row.netCost)
                is TxRow.Item -> TransactionRow(row.transaction, onTransactionClick, actorDisplay)
            }
        }
    }
}

/** Month divider: title, gradient rule, and the month's net total (spec `3b` `.section-header-row`). */
@Composable
private fun MonthHeaderRow(label: String, netCost: Double) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(label.uppercase(), style = MaterialTheme.typography.titleSmall)
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
        Text(
            "%+.2f".format(netCost),
            style = MaterialTheme.typography.labelMedium,
            color = INBOUND_ON_COLOR,
        )
    }
}

@Composable
private fun TransactionRow(
    transaction: Transaction,
    onTransactionClick: ((Transaction) -> Unit)?,
    actorDisplay: (Transaction) -> String,
) {
    val edgeColor = when (transaction.type) {
        TransactionType.INBOUND -> INBOUND_COLOR
        TransactionType.OUTBOUND -> OUTBOUND_COLOR
    }
    val amountColor = when (transaction.type) {
        TransactionType.INBOUND -> INBOUND_ON_COLOR
        TransactionType.OUTBOUND -> OUTBOUND_ON_COLOR
    }
    // INBOUND = cards acquired (+), OUTBOUND = cards disposed of (−) — matches
    // Transaction's domain meaning (see kb/domain/glossary.md), previously swapped here.
    val sign = when (transaction.type) {
        TransactionType.INBOUND -> "+"
        TransactionType.OUTBOUND -> "−"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(13.dp))
            .clickable { onTransactionClick?.invoke(transaction) }
            .padding(start = 3.dp, end = 12.dp, top = 11.dp, bottom = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(4.dp, 34.dp)
                .background(edgeColor, RoundedCornerShape(2.dp))
        )
        Box(
            modifier = Modifier
                .padding(start = 10.dp)
                .size(34.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(edgeColor.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(sign, color = amountColor, style = MaterialTheme.typography.titleMedium)
        }
        Column(modifier = Modifier.weight(1f).padding(start = 11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                ChannelIcon(transaction.channel)
                Text(
                    actorDisplay(transaction),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "#${transaction.id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                // Without actors the headline already is the channel, so don't repeat it here.
                transaction.description.ifBlank { if (FeatureFlags.ACTORS) transaction.channel ?: "" else "" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
        AmountText(text = "%.2f".format(transaction.costEuro), color = amountColor, fontSize = 15.sp)
    }
}
