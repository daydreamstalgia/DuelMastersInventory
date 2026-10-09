package ro.ddnostalgia.duelmastersinventory.features.statistics.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import ro.ddnostalgia.duelmastersinventory.shared.data.statistics.model.LabeledCost
import ro.ddnostalgia.duelmastersinventory.shared.ui.components.core.AmountText
import ro.ddnostalgia.duelmastersinventory.shared.ui.components.transactions.INBOUND_COLOR
import ro.ddnostalgia.duelmastersinventory.shared.ui.components.transactions.INBOUND_ON_COLOR
import ro.ddnostalgia.duelmastersinventory.shared.ui.components.transactions.OUTBOUND_COLOR
import ro.ddnostalgia.duelmastersinventory.shared.ui.components.transactions.OUTBOUND_ON_COLOR
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun StatisticsScreen(
    navController: NavController
) {
    val viewModel: StatisticsScreenViewModel = hiltViewModel()

    val costsByMonth by viewModel.costsByMonth.collectAsState()
    val totalCost by viewModel.totalCost.collectAsState()
    val monthlyAverageCost by viewModel.monthlyAverageCost.collectAsState()
    val nextBuyMonthSpan by viewModel.nextBuyMonthSpan.collectAsState()

    val targetYearMonth = remember(nextBuyMonthSpan) {
        YearMonth.now().plusMonths(nextBuyMonthSpan?.toLong() ?: 0)
    }

    val formattedMonth = remember(targetYearMonth) {
        val monthName = targetYearMonth.month
            .getDisplayName(TextStyle.SHORT, Locale.getDefault())
        "$monthName ${targetYearMonth.year}"
    }

    val maxMonthly = remember(costsByMonth) {
        costsByMonth.maxOfOrNull { maxOf(it.inboundCost, it.outboundCost) }?.takeIf { it > 0 } ?: 1.0
    }

    LazyColumn(Modifier.padding(16.dp)) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SummaryTile(
                    label = "INBOUND",
                    amount = totalCost.inboundCost,
                    backgroundColor = INBOUND_COLOR.copy(alpha = 0.12f),
                    borderColor = INBOUND_COLOR.copy(alpha = 0.3f),
                    labelColor = INBOUND_ON_COLOR.copy(alpha = 0.8f),
                    valueColor = INBOUND_ON_COLOR,
                    modifier = Modifier.weight(1f),
                )
                SummaryTile(
                    label = "OUTBOUND",
                    amount = totalCost.outboundCost,
                    backgroundColor = OUTBOUND_COLOR.copy(alpha = 0.12f),
                    borderColor = OUTBOUND_COLOR.copy(alpha = 0.3f),
                    labelColor = OUTBOUND_ON_COLOR.copy(alpha = 0.8f),
                    valueColor = OUTBOUND_ON_COLOR,
                    modifier = Modifier.weight(1f),
                )
                SummaryTile(
                    label = "NET",
                    amount = totalCost.cost,
                    backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    labelColor = MaterialTheme.colorScheme.primary,
                    valueColor = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                )
            }

            SectionHeaderRow(title = "COST BY MONTH", meta = "EUR")
        }

        items(costsByMonth) { item ->
            MonthRow(item, maxMonthly)
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(13.dp)),
            ) {
                Column(modifier = Modifier.weight(1f).padding(14.dp)) {
                    Text("MONTHLY AVERAGE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    AmountText(text = "%.2f".format(monthlyAverageCost.cost), fontSize = 19.sp, modifier = Modifier.padding(top = 9.dp))
                }
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .padding(vertical = 14.dp)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
                )
                Column(modifier = Modifier.weight(1f).padding(14.dp)) {
                    Text("NEXT BUY (~300 EUR)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "in ${nextBuyMonthSpan ?: 0} months · $formattedMonth",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 9.dp),
                    )
                }
            }
        }
    }
}

/** Section-header title + gradient rule + trailing meta, matching the pattern established by
 * ActorViewScreen's private SectionHeaderRow / TransactionsList's MonthHeaderRow. */
@Composable
private fun SectionHeaderRow(title: String, meta: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 10.dp),
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
            Text(meta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SummaryTile(
    label: String,
    amount: Double,
    backgroundColor: Color,
    borderColor: Color,
    labelColor: Color,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(11.dp))
            .border(1.dp, borderColor, RoundedCornerShape(11.dp))
            .padding(horizontal = 11.dp, vertical = 10.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = labelColor)
        AmountText(text = "%.2f".format(amount), color = valueColor, fontSize = 17.sp, modifier = Modifier.padding(top = 7.dp))
    }
}

@Composable
private fun MonthRow(item: LabeledCost, maxMonthly: Double) {
    val displayLabel = remember(item.label) { formatMonthLabel(item.label) }
    val totalColor = if (item.cost >= 0) OUTBOUND_COLOR else INBOUND_COLOR

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 11.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(displayLabel, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(72.dp))
            Box(modifier = Modifier.weight(1f))
            // Plain ASCII hyphen, not U+2212 MINUS SIGN — the custom DMNumbers display
            // font (numbers.ttf) only covers digits/comma/period, so the real minus
            // sign glyph falls back to tofu that reads as a stray leading digit.
            AmountText(text = "-%.2f".format(item.inboundCost), color = INBOUND_ON_COLOR, fontSize = 11.sp)
            AmountText(
                text = "+%.2f".format(item.outboundCost),
                color = OUTBOUND_ON_COLOR,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 10.dp),
            )
            AmountText(
                text = "%.2f".format(item.cost),
                color = totalColor,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 10.dp),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp).height(4.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(barFraction(item.inboundCost, maxMonthly))
                    .fillMaxHeight()
                    .background(INBOUND_COLOR, RoundedCornerShape(2.dp))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(barFraction(item.outboundCost, maxMonthly))
                    .fillMaxHeight()
                    .background(OUTBOUND_COLOR, RoundedCornerShape(2.dp))
            )
        }
    }
}

/** "2026-01" (StatisticsDao's `strftime('%Y-%m', date)` grouping key) -> "Jan 2026", display-only. */
private fun formatMonthLabel(label: String): String {
    val yearMonth = runCatching { YearMonth.parse(label) }.getOrNull() ?: return label
    val monthName = yearMonth.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    return "$monthName ${yearMonth.year}"
}

private fun barFraction(value: Double, max: Double): Float {
    if (value <= 0.0) return 0f
    return (value / max).coerceIn(0.02, 1.0).toFloat()
}
