package ro.ddnostalgia.duelmastersinventory.features.transactions.ui.utils

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import ro.ddnostalgia.duelmastersinventory.nav.ScreenConfig
import ro.ddnostalgia.duelmastersinventory.shared.data.transactions.model.Transaction
import ro.ddnostalgia.duelmastersinventory.shared.ui.components.cards.CardPrototypePrintsFilterBox
import ro.ddnostalgia.duelmastersinventory.shared.ui.components.core.AmountText
import ro.ddnostalgia.duelmastersinventory.shared.ui.components.core.AppConfirmDialog
import ro.ddnostalgia.duelmastersinventory.shared.ui.components.transactions.INBOUND_COLOR
import ro.ddnostalgia.duelmastersinventory.shared.ui.components.transactions.INBOUND_ON_COLOR
import ro.ddnostalgia.duelmastersinventory.shared.ui.components.transactions.OUTBOUND_ON_COLOR

// Dark-on-green text color for the outbound accent (mockup `.pill-btn.outbound`/owned-cards "+"
// button, styles.css literal #0c2e19) - the outbound counterpart of MaterialTheme's onPrimary,
// which has no equivalent theme token since the green accent itself is ad hoc per decision 0006.
private val OUTBOUND_ON_ACCENT_COLOR = Color(0xFF0C2E19)
// "Last copy" warning tint (styles.css `--civ-light`/`--cond-lp` gold, reused elsewhere in the
// app for the same hex - see Civilization.LIGHT / Conditions.kt's "LP" entry).
private val LAST_COPY_WARNING_COLOR = Color(0xFFF2C230)

@Composable
fun OutboundTransactedCardsEditScreen(
    navController: NavController,
    transaction: Transaction,
    screenConfig: ScreenConfig? = null,
    viewModel: OutboundTransactedCardsEditScreenViewModel = hiltViewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    val showingTabName = remember { mutableStateOf("edit") }

    val uiState by viewModel.uiState.collectAsState()
    val isDirty by viewModel.isDirty.collectAsState()

    val currentListState = rememberLazyListState()
    val removedListState = rememberLazyListState()

    var showExitConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(transaction) {
        viewModel.setTransactionId(transaction.id)
        // Nothing to review on an empty transaction - land straight on the tab that actually
        // does something.
        if (viewModel.isEmptyTransaction(transaction.id)) showingTabName.value = "add"
    }

    LaunchedEffect(isDirty) {
        screenConfig?.setOnBack {
            if (isDirty) showExitConfirm = true else navController.navigateUp()
        }
    }

    BackHandler(enabled = isDirty) {
        showExitConfirm = true
    }

    if (showExitConfirm) {
        AppConfirmDialog(
            title = "Discard changes?",
            message = "You have unsaved card changes. Leaving now will discard them.",
            confirmText = "Discard",
            onConfirm = {
                showExitConfirm = false
                navController.navigateUp()
            },
            onDismiss = { showExitConfirm = false },
        )
    }

    Column {
        TabRow(
            showingTabName,
            uiState.currentCardsCount,
            uiState.removedCardsCount,
            tab1Label = "Owned cards",
            accentColor = OUTBOUND_ON_COLOR,
            onAccentColor = OUTBOUND_ON_ACCENT_COLOR,
        ) {
            coroutineScope.launch {
                viewModel.saveChanges()
                navController.navigateUp()
            }
        }

        @Composable
        fun DisplayItem(
            item: OutboundTransactedCardsEditScreenViewModel.EditableItem,
            pendingCaption: String? = null,
            pendingCaptionColor: Color? = null,
            strikethrough: Boolean = false,
            input: @Composable androidx.compose.foundation.layout.RowScope.(item: OutboundTransactedCardsEditScreenViewModel.EditableItem) -> Unit
        ) {
            TransactedCardRow(
                imagePrint = item.key.print,
                title = "${item.key.prototype.name}",
                subtitle = "${item.key.print.language} ${item.key.print.displayId()}",
                condition = item.key.condition,
                pendingCaption = pendingCaption,
                pendingCaptionColor = pendingCaptionColor,
                strikethrough = strikethrough,
                input = { input(this, item) }
            )
        }

        @Composable
        fun FilterBox() {
            CardPrototypePrintsFilterBox(
                value = uiState.cardFilterParams,
                onValueChange = { viewModel.setCardFilter(it) },
                filter = false
            )
        }


        if (showingTabName.value == "edit") {
            FilterBox()

            LazyColumn(state = currentListState, modifier = Modifier.padding(horizontal = 12.dp)) {
                items(items = uiState.currentCards, key = { it.key.keyId }) {
                    DisplayItem(it) {
                        AmountText(
                            text = "${it.transactedCards.size + it.addedCards.size}",
                            fontSize = 16.sp,
                            modifier = Modifier.align(Alignment.CenterVertically),
                        )

                        SmallIconButton(
                            onClick = { viewModel.removeOneTransactedCardFrom(it) },
                            enabled = it.addedCards.size + it.transactedCards.size > 0,
                            containerColor = INBOUND_COLOR.copy(alpha = 0.16f),
                        ) {
                            Icon(Icons.Filled.Delete, "Remove", tint = INBOUND_ON_COLOR)
                        }
                    }
                } //  items

                item {
                    PendingChangesBanner(uiState.currentCards.sumOf { it.addedCards.size })
                }
            } // LazyColumn
        } // if

        if(showingTabName.value == "removed") {
            FilterBox()

            LazyColumn(state = removedListState, modifier = Modifier.padding(horizontal = 12.dp)) {
                item {
                    RemovedWarningPanel("They stay in your collection when you save. Restore any of them with ↩.")
                }

                items(items = uiState.removedCards, key = { it.key.keyId }) {
                    DisplayItem(it, strikethrough = true) {
                        AmountText(text = "${it.addedCards.size}", fontSize = 16.sp, modifier = Modifier.align(Alignment.CenterVertically).padding(end = 10.dp))

                        SmallIconButton(
                            onClick = { viewModel.restoreTransactedCard(it) },
                            enabled = it.addedCards.isNotEmpty(),
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                            borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Restore", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            } // LazyColumn
        } // if

        if(showingTabName.value=="add") {
            FilterBox()

            LazyColumn(state = removedListState, modifier = Modifier.padding(horizontal = 12.dp)) {
                item {
                    SectionLabel("Duplicates first · safe to sell")
                }

                items(items = uiState.searchCards, key = { it.key.keyId }) {
                    Column {
                        DisplayItem(
                            it,
                            pendingCaption = "${it.addedCards.size} owned",
                            pendingCaptionColor = if (it.addedCards.size > 1) OUTBOUND_ON_COLOR else LAST_COPY_WARNING_COLOR,
                        ) {
                            AddCirclePillButton(
                                onClick = { viewModel.moveOneOwnedCardToCurrent(it) },
                                containerColor = OUTBOUND_ON_COLOR,
                                contentColor = OUTBOUND_ON_ACCENT_COLOR,
                            )
                        }

                        if (it.addedCards.size == 1) {
                            Text(
                                "Selling your last copy of ${it.key.prototype.name}.",
                                style = MaterialTheme.typography.bodySmall,
                                color = LAST_COPY_WARNING_COLOR,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                                    .background(LAST_COPY_WARNING_COLOR.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                    .border(1.dp, LAST_COPY_WARNING_COLOR.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 13.dp, vertical = 11.dp),
                            )
                        }
                    }
                }
            } // LazyColumn

        }

    } // Column
}
