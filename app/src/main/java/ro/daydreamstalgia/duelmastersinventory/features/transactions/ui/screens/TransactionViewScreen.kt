package ro.daydreamstalgia.duelmastersinventory.features.transactions.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import ro.daydreamstalgia.duelmastersinventory.nav.Routes
import ro.daydreamstalgia.duelmastersinventory.nav.ScreenConfig
import ro.daydreamstalgia.duelmastersinventory.shared.data.transactions.model.TransactionType
import ro.daydreamstalgia.duelmastersinventory.shared.ui.components.cards.CardPrototypePrintsFilterBox
import ro.daydreamstalgia.duelmastersinventory.shared.ui.components.cards.CardsList
import ro.daydreamstalgia.duelmastersinventory.shared.ui.components.core.AmountText
import ro.daydreamstalgia.duelmastersinventory.shared.ui.components.core.ChannelIcon
import ro.daydreamstalgia.duelmastersinventory.shared.utils.constants.FeatureFlags
import ro.daydreamstalgia.duelmastersinventory.shared.ui.components.transactions.INBOUND_ON_COLOR
import ro.daydreamstalgia.duelmastersinventory.shared.ui.components.transactions.OUTBOUND_ON_COLOR

@Composable
fun TransactionViewScreen(
    navController: NavController,
    screenConfig: ScreenConfig? = null,
    viewModel: TransactionViewScreenViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val transaction by viewModel.transaction.collectAsState()
    val actorDisplay by viewModel.actorDisplay.collectAsState()
    val records by viewModel.records.collectAsState()

    val cardFilterParams by viewModel.cardFilterParams.collectAsState()

    if(transaction==null) {
        return
    }

    val t = transaction!!
    val typeColor = if (t.type == TransactionType.INBOUND) INBOUND_ON_COLOR else OUTBOUND_ON_COLOR

    screenConfig?.setDropdownAction(listOfNotNull(
        "Edit transaction" to {
            navController.navigate(Routes.TransactionEdit.createRoute(t.id))
        },
        "Edit cards" to {
            navController.navigate(Routes.TransactedCardsEdit.createRoute(t.id))
        },
        t.parcelTrackingNumber?.let {
            "Track package" to {
                val url = "https://parcelsapp.com/en/tracking/$it"
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        }
    ))

    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.colorScheme.background)
                    ),
                    RoundedCornerShape(16.dp),
                )
                .border(1.dp, typeColor.copy(alpha = 0.28f), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text(
                            t.type.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = typeColor,
                            modifier = Modifier
                                .background(typeColor.copy(alpha = 0.18f), RoundedCornerShape(11.dp))
                                .padding(horizontal = 9.dp, vertical = 4.dp),
                        )
                        Text(t.date.toString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 10.dp),
                    ) {
                        ChannelIcon(t.channel)
                        Text(actorDisplay, style = MaterialTheme.typography.titleMedium)
                    }
                    // Without actors the headline above already is the channel.
                    if (FeatureFlags.ACTORS && !t.channel.isNullOrBlank()) {
                        Text(
                            t.channel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 5.dp),
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    AmountText(text = "%.2f".format(t.costEuro), color = typeColor, fontSize = 28.sp)
                    Text("EUR", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (t.description.isNotBlank()) {
                Text(
                    t.description,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }

        if (t.tradeReferenceId != null || t.parcelTrackingNumber != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                t.tradeReferenceId?.let { tradeRef ->
                    InfoTile("TRADE REFERENCE", "#$tradeRef ›", Modifier.weight(1f).clickable {
                        navController.navigate(Routes.TransactionView.createRoute(tradeRef))
                    })
                }
                t.parcelTrackingNumber?.let { tracking ->
                    InfoTile("PARCEL", tracking, Modifier.weight(1.5f))
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("CARDS IN THIS TRANSACTION", style = MaterialTheme.typography.titleSmall)
            Box(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), CircleShape)
                    .clickable { navController.navigate(Routes.TransactedCardsEdit.createRoute(t.id)) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = "Edit cards",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp),
                )
            }
        }

        CardPrototypePrintsFilterBox(
            value = cardFilterParams,
            onValueChange = { viewModel.setCardFilter(it) },
            filter = false,
            modifier = Modifier.fillMaxWidth()
        )

        CardsList(
            items = records,
            key = { it.card.id },
            cardPrintTransform = { it.print },
            onItemClick = {
                navController.navigate(Routes.CardView.createRoute(
                    it.print.cardPrototypeId,
                    it.print.id
                ))
            }
        ) {
            AmountText(text = "${it.count}", fontSize = 11.sp)
        }
    }
}

@Composable
private fun InfoTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 11.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 6.dp))
    }
}
