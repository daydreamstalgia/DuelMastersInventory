package ro.ddnostalgia.duelmastersinventory.features.transactions.ui.screens

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import ro.ddnostalgia.duelmastersinventory.features.transactions.ui.utils.InboundTransactedCardsEditScreen
import ro.ddnostalgia.duelmastersinventory.features.transactions.ui.utils.OutboundTransactedCardsEditScreen
import ro.ddnostalgia.duelmastersinventory.nav.ScreenConfig
import ro.ddnostalgia.duelmastersinventory.shared.data.transactions.model.TransactionType

@Composable
fun TransactedCardsEditScreen(
    navController: NavController,
    screenConfig: ScreenConfig? = null,
    viewModel: TransactedCardsEditScreenViewModel = hiltViewModel()
) {
    val transaction by viewModel.transaction.collectAsState()

    when(val t=transaction) {
        null -> { Text("Loading") }
        else -> when (t.type) {
            TransactionType.INBOUND -> InboundTransactedCardsEditScreen(
                navController,
                t,
                screenConfig
            )
            TransactionType.OUTBOUND -> OutboundTransactedCardsEditScreen(
                navController,
                t,
                screenConfig
            )
        }
    }
}