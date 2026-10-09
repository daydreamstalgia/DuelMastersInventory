package ro.ddnostalgia.duelmastersinventory.features.transactions.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import ro.ddnostalgia.duelmastersinventory.nav.Routes
import ro.ddnostalgia.duelmastersinventory.shared.data.actors.utils.displayName
import ro.ddnostalgia.duelmastersinventory.shared.ui.components.core.AmountText
import ro.ddnostalgia.duelmastersinventory.shared.ui.components.transactions.INBOUND_COLOR
import ro.ddnostalgia.duelmastersinventory.shared.ui.components.transactions.INBOUND_ON_COLOR
import ro.ddnostalgia.duelmastersinventory.shared.ui.components.transactions.OUTBOUND_COLOR
import ro.ddnostalgia.duelmastersinventory.shared.ui.components.transactions.OUTBOUND_ON_COLOR
import ro.ddnostalgia.duelmastersinventory.shared.ui.components.transactions.TransactionFilterBox
import ro.ddnostalgia.duelmastersinventory.shared.ui.components.transactions.TransactionsList
import ro.ddnostalgia.duelmastersinventory.shared.ui.scaffolds.FloatingAddButtonScaffold

@Composable
fun TransactionsScreen(
    navController: NavController,
    viewModel: TransactionsScreenViewModel = hiltViewModel()
) {
    val transactions by viewModel.transactions.collectAsState()
    val filterParams by viewModel.filterParams.collectAsState()
    val actorsById by viewModel.actorsById.collectAsState()
    val totalCost by viewModel.totalCost.collectAsState()

    FloatingAddButtonScaffold(
        label = "New",
        onClick = { navController.navigate(Routes.TransactionCreate.route) },
    ) {
        Column {
            // Gradient panel holding search + summary tiles (spec `3b`), matching the
            // gradient-header treatment BackScaffold already applies to detail screens.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceContainer,
                                MaterialTheme.colorScheme.background,
                            )
                        )
                    )
                    .padding(bottom = 10.dp)
            ) {
                TransactionFilterBox(
                    value = filterParams,
                    onValueChange = { viewModel.setFilter(it) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SummaryTile(
                        "INBOUND",
                        totalCost.inboundCost,
                        accentColor = INBOUND_COLOR,
                        onColor = INBOUND_ON_COLOR,
                        modifier = Modifier.weight(1f),
                    )
                    SummaryTile(
                        "OUTBOUND",
                        totalCost.outboundCost,
                        accentColor = OUTBOUND_COLOR,
                        onColor = OUTBOUND_ON_COLOR,
                        modifier = Modifier.weight(1f),
                    )
                    SummaryTile(
                        "NET",
                        totalCost.cost,
                        accentColor = MaterialTheme.colorScheme.primary,
                        onColor = MaterialTheme.colorScheme.primary,
                        valueColor = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            TransactionsList(
                transactions,
                onTransactionClick = {
                    navController.navigate(Routes.TransactionView.createRoute(it.id))
                },
                actorDisplay = { actorsById[it.actorId].displayName(it.channel) }
            )
        }
    }
}

/** Inbound/outbound/net summary tile (spec `3b` `.stat-tile`): tinted background + border in [accentColor], label/value in [onColor]/[valueColor]. */
@Composable
private fun SummaryTile(
    label: String,
    amount: Double,
    accentColor: Color,
    onColor: Color,
    modifier: Modifier = Modifier,
    valueColor: Color = onColor,
) {
    Column(
        modifier = modifier
            .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(11.dp))
            .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(11.dp))
            .padding(horizontal = 11.dp, vertical = 10.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = onColor.copy(alpha = 0.8f))
        AmountText(text = "%.2f".format(amount), color = valueColor, modifier = Modifier.padding(top = 6.dp))
    }
}
