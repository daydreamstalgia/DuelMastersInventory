package ro.ddnostalgia.duelmastersinventory.features.transactions.ui.utils

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import ro.ddnostalgia.duelmastersinventory.nav.Routes
import ro.ddnostalgia.duelmastersinventory.nav.ScreenConfig
import ro.ddnostalgia.duelmastersinventory.shared.data.transactions.model.Transaction
import ro.ddnostalgia.duelmastersinventory.shared.ui.components.cards.CardPrintImage
import ro.ddnostalgia.duelmastersinventory.shared.ui.components.cards.CardPrototypePrintsFilterBox
import ro.ddnostalgia.duelmastersinventory.shared.ui.components.core.AmountText
import ro.ddnostalgia.duelmastersinventory.shared.ui.components.core.AppConfirmDialog
import ro.ddnostalgia.duelmastersinventory.shared.ui.components.core.ConditionChip
import ro.ddnostalgia.duelmastersinventory.shared.ui.components.transactions.INBOUND_COLOR
import ro.ddnostalgia.duelmastersinventory.shared.ui.components.transactions.INBOUND_ON_COLOR
import ro.ddnostalgia.duelmastersinventory.shared.ui.components.transactions.OUTBOUND_ON_COLOR
import ro.ddnostalgia.duelmastersinventory.shared.utils.constants.Conditions
import ro.ddnostalgia.duelmastersinventory.shared.utils.constants.conditionColor


@Composable
fun InboundTransactedCardsEditScreen(
    navController: NavController,
    transaction: Transaction,
    screenConfig: ScreenConfig? = null,
    viewModel: InboundTransactedCardsEditScreenViewModel = hiltViewModel()
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
        TabRow(showingTabName, uiState.currentCardsCount, uiState.removedCardsCount, tab1Label = "Add") {
            coroutineScope.launch {
                viewModel.saveChanges()
                navController.navigateUp()
            }
        }

        @Composable
        fun DisplayItem(
            item: InboundTransactedCardsEditScreenViewModel.EditableItem,
            editableCondition: Boolean = false,
            onConditionChanged: ((String?)->Unit)? = null,
            pendingCaption: String? = null,
            dimmed: Boolean = false,
            strikethrough: Boolean = false,
            input: @Composable RowScope.(item: InboundTransactedCardsEditScreenViewModel.EditableItem) -> Unit
        ) {
            TransactedCardRow(
                imagePrint = item.key.print,
                title = "${item.key.prototype.name}",
                subtitle = item.key.print.displayId(),
                condition = item.key.condition,
                editableCondition = editableCondition,
                onConditionChanged = onConditionChanged,
                pendingCaption = pendingCaption,
                dimmed = dimmed,
                strikethrough = strikethrough,
                input = { input(this, item) }
            )
        } // DisplayItem()

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

            LazyColumn(
                state = currentListState,
                modifier=Modifier.padding(horizontal = 12.dp)
            ) {
                items(items = uiState.currentCards, key = { it.key.keyId }) {
                    DisplayItem(
                        it,
                        pendingCaption = "+${it.addedCards.size} pending".takeIf { _ -> it.addedCards.isNotEmpty() },
                        dimmed = !it.key.owned,
                    ) {
                        if(!it.key.owned) {
                            Text(
                                "x${it.transactedCards.size} outbound",
                                style = MaterialTheme.typography.bodySmall,
                                color = INBOUND_ON_COLOR,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                            return@DisplayItem
                        }

                        SmallIconButton(
                            onClick = { viewModel.removeOneTransactedCardFrom(it) },
                            containerColor = INBOUND_COLOR.copy(alpha = 0.16f),
                        ) {
                            Icon(Icons.Filled.Delete, "Remove", tint = INBOUND_ON_COLOR)
                        }

                        AmountText(
                            text = "${it.transactedCards.size + it.addedCards.size}",
                            fontSize = 16.sp,
                            modifier = Modifier.align(Alignment.CenterVertically).width(26.dp)
                        )

                        AddCirclePillButton(onClick = { viewModel.addOneTransactedCardTo(it) }, size = 28.dp)
                    }

                } //  items

                item {
                    PendingChangesBanner(uiState.currentCards.sumOf { it.addedCards.size })
                }
            } // LazyColumn
        } // if

        if(showingTabName.value == "removed") {
            FilterBox()

            LazyColumn(
                state = removedListState,
                modifier=Modifier.padding(horizontal = 12.dp)
            ) {
                item {
                    RemovedWarningPanel("They leave your collection when you save. Restore any of them with ↩.")
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f)) {
                    FilterBox()
                }
                Box(
                    modifier = Modifier
                        .padding(end = 10.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable {
                            navController.navigate(Routes.CardScan.createRoute(transaction.id))
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.CameraAlt,
                        contentDescription = "Scan card",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            LazyColumn(
                state = removedListState,
                modifier=Modifier.padding(horizontal = 12.dp)
            ) {
                item {
                    SectionLabel("Matching prints")
                }

                items(items = uiState.searchCards, key = { it.key.keyId }) {
                    var condition by remember { mutableStateOf<String?>(null) }

                    DisplayItem(
                        it,
                        editableCondition = true,
                        onConditionChanged = { condition = it },
                    ) {
                        AddCirclePillButton(onClick = {
                            val card = it.templateTransactedCard
                                ?.card
                                ?.copy(condition=condition) ?: return@AddCirclePillButton

                            val templateCard = it.templateTransactedCard.copy(card=card)

                            val item = InboundTransactedCardsEditScreenViewModel.EditableItem
                                .from(templateCard)

                            viewModel.addOneTransactedCardTo(item)
                        })
                    }
                }
            } // LazyColumn

        }

    } // Column
}

@Composable
internal fun TabRow(
    tabState: MutableState<String>,
    currentCount: Int,
    removedCount: Int,
    tab1Label: String,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    onAccentColor: Color = MaterialTheme.colorScheme.onPrimary,
    onSave: () -> Unit,
) {
    Row(
        modifier = Modifier.padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TabButton("add", tab1Label, tabState, accentColor, Modifier.weight(1f))
        TabButton("edit", "Current · $currentCount", tabState, accentColor, Modifier.weight(1f))
        TabButton("removed", "Removed · $removedCount", tabState, INBOUND_ON_COLOR, Modifier.weight(1f))

        Row(
            modifier = Modifier
                .padding(start = 6.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(accentColor)
                .clickable(onClick = onSave)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = onAccentColor, modifier = Modifier.size(14.dp))
            Text("Save", style = MaterialTheme.typography.labelLarge, color = onAccentColor)
        }
    }
}

@Composable
private fun TabButton(
    tabName: String,
    text: String,
    tabState: MutableState<String>,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val selected = tabState.value == tabName
    val color = if (selected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = modifier
            .clickable { tabState.value = tabName }
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text, style = if (selected) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelMedium, color = color)
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .fillMaxWidth()
                .size(width = 0.dp, height = 2.dp)
                .background(if (selected) accentColor else Color.Transparent)
        )
    }
}

/** "+N pending · nothing saved until you tap Save" banner shown under a Current tab's list. Fixed green accent regardless of inbound/outbound, matching the mockup. */
@Composable
internal fun PendingChangesBanner(count: Int) {
    if (count <= 0) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 8.dp)
            .background(OUTBOUND_ON_COLOR.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .border(1.dp, OUTBOUND_ON_COLOR.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(horizontal = 13.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(OUTBOUND_ON_COLOR))
        Text(
            "+$count pending · nothing saved until you tap Save",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
        )
    }
}

/** Warning panel shown atop a Removed tab's list. [message] differs between inbound/outbound. */
@Composable
internal fun RemovedWarningPanel(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .background(INBOUND_ON_COLOR.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .border(1.dp, INBOUND_ON_COLOR.copy(alpha = 0.28f), RoundedCornerShape(12.dp))
            .padding(horizontal = 13.dp, vertical = 12.dp),
    ) {
        Text("Removed from this transaction", style = MaterialTheme.typography.labelLarge, color = INBOUND_ON_COLOR)
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/** Uppercase micro-label above a list section, e.g. "Matching prints" / "Duplicates first · safe to sell". */
@Composable
internal fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f),
        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
    )
}

@Composable
internal fun TransactedCardRow(
    imagePrint: ro.ddnostalgia.duelmastersinventory.shared.data.cards.model.CardPrint,
    title: String,
    subtitle: String,
    condition: String?,
    editableCondition: Boolean = false,
    onConditionChanged: ((String?) -> Unit)? = null,
    pendingCaption: String? = null,
    pendingCaptionColor: Color? = null,
    dimmed: Boolean = false,
    strikethrough: Boolean = false,
    input: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .alpha(if (dimmed) 0.75f else 1f)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(13.dp))
            .padding(11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CardPrintImage(
            imagePrint,
            modifier = Modifier
                .width(36.dp)
                .aspectRatio(40f / 60f)
                .clip(RoundedCornerShape(3.dp))
                .alpha(if (strikethrough) 0.55f else 1f)
        )

        Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                textDecoration = if (strikethrough) TextDecoration.LineThrough else null,
                color = if (strikethrough) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f) else Color.Unspecified,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (strikethrough) 0.7f else 1f),
                modifier = Modifier.padding(top = 3.dp),
            )

            Row(modifier = Modifier.padding(top = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (!editableCondition) {
                    if (condition != null) {
                        ConditionChip(condition)
                    }
                } else {
                    var localCondition by remember { mutableStateOf<String?>(null) }
                    ConditionPillDropdown(
                        condition = localCondition,
                        onConditionChanged = {
                            localCondition = it
                            onConditionChanged?.invoke(it)
                        },
                    )
                }
                if (pendingCaption != null) {
                    Text(
                        pendingCaption,
                        style = MaterialTheme.typography.bodySmall,
                        color = pendingCaptionColor ?: MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            input()
        }
    }
}

/** Compact "NM ▾" condition selector chip (styles.css `.condition-chip` + dropdown arrow), replacing the full-width [ro.ddnostalgia.duelmastersinventory.shared.ui.components.core.Dropdown] text field for inline use inside a card row. */
@Composable
private fun ConditionPillDropdown(
    condition: String?,
    onConditionChanged: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val color = conditionColor(condition)

    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.35f))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                .clickable { expanded = true }
                .padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(condition ?: "-", style = MaterialTheme.typography.labelMedium, color = color)
            Text("▾", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.4f))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Conditions.forEach { cond ->
                DropdownMenuItem(
                    text = { Text(cond) },
                    onClick = {
                        onConditionChanged(cond)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
internal fun AddCirclePillButton(
    onClick: () -> Unit,
    size: Dp = 30.dp,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(containerColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Add, "Add", tint = contentColor, modifier = Modifier.size(16.dp))
    }
}

@Composable
internal fun SmallIconButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    size: Dp = 28.dp,
    containerColor: Color = Color.Transparent,
    borderColor: Color? = null,
    content: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(containerColor)
            .let { m -> if (borderColor != null) m.border(1.dp, borderColor, CircleShape) else m },
    ) {
        content()
    }
}
