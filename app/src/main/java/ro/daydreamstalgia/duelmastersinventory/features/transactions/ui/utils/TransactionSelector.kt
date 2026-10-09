package ro.daydreamstalgia.duelmastersinventory.features.transactions.ui.utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ro.daydreamstalgia.duelmastersinventory.shared.data.actors.utils.counterpartyDisplay
import ro.daydreamstalgia.duelmastersinventory.shared.data.transactions.model.Transaction
import ro.daydreamstalgia.duelmastersinventory.shared.utils.extensions.safeSubstring
import ro.daydreamstalgia.duelmastersinventory.shared.ui.components.core.AppBottomSheet
import ro.daydreamstalgia.duelmastersinventory.shared.ui.components.transactions.TransactionFilterBox

@Composable
fun TransactionSelector(
    value: Int?,
    onValueChanged: (Int?)->Unit,
    filter: (Transaction)->Boolean,
    modifier: Modifier = Modifier,
    label: String? = null,
    viewModel: TransactionSelectorViewModel = hiltViewModel()
) {
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(value) {
        viewModel.loadSelectedTransaction(value)
    }

    val selectedTransaction by viewModel.selectedTransaction.collectAsState()
    val actorsById by viewModel.actorsById.collectAsState()

    Box(modifier = modifier) {
        TextField(
            value = selectedTransaction?.let {
                "${it.id}. ${it.type}\n${it.counterpartyDisplay(actorsById[it.actorId])}\n${it.date}\n${it.description.safeSubstring(120)}"
            } ?: "",
            onValueChange = {},
            readOnly = true,
            label = label?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { showDialog = true }
        )
    }


    if (showDialog) {
        val filterParams by viewModel.filterParams.collectAsState()

        LaunchedEffect(filter, filterParams) {
            viewModel.loadTransactions(filter)
        }

        val transactions by viewModel.transactions.collectAsState()

        AppBottomSheet(onDismissRequest = { showDialog = false }) {
            SectionHeader("Select a transaction")

            TransactionFilterBox(
                value = filterParams,
                onValueChange = { viewModel.setFilter(it) }
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.6f)
            ) {
                items(items = transactions, key = { it.id }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onValueChanged(it.id)
                                showDialog = false
                            }
                    ) {
                        Column {
                            Text("${it.id}. ${it.type}")
                            Text(it.counterpartyDisplay(actorsById[it.actorId]))
                            Text(it.description.safeSubstring(120))
                            HorizontalDivider(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                            )
                        }
                    }
                }
            }

            Row {
                Button(
                    onClick = {
                        onValueChanged(null)
                        showDialog = false
                    }
                ) {
                    Text("Clear selection")
                }
            }
        }
    }

}

@Composable
private fun SectionHeader(text: String) {
    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium
    )
}