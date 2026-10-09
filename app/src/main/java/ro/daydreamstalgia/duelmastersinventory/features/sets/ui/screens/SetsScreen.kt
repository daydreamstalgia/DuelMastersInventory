package ro.daydreamstalgia.duelmastersinventory.features.sets.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.repository.CardSetSummary
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.setimport.AssetDiffKind
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.setimport.ConflictResolution
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.setimport.DiscoverSetEntry
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.setimport.SetImportConflict
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.setimport.SetImportPlan
import ro.daydreamstalgia.duelmastersinventory.shared.ui.components.core.AppConfirmDialog
import ro.daydreamstalgia.duelmastersinventory.shared.ui.components.utils.LazyImage

@Composable
fun SetsScreen(navController: NavController) {
    val viewModel: SetsScreenViewModel = hiltViewModel()

    val summaries by viewModel.setSummaries.collectAsState()
    val discoverState by viewModel.discoverState.collectAsState()
    val setImportState by viewModel.setImportState.collectAsState()
    val deleteState by viewModel.deleteState.collectAsState()

    if (summaries.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No sets loaded yet. Use the menu above to load one.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(summaries, key = { "${it.language}_${it.setCode}" }) { summary ->
                SetRow(summary, onDelete = { viewModel.requestDelete(summary) })
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
            }
        }
    }

    DiscoverDialog(
        state = discoverState,
        onDismiss = viewModel::dismissDiscover,
        onImportSelected = viewModel::importFromDiscover,
    )

    SetImportProgressDialog(state = setImportState, onDismiss = viewModel::dismissSetImportResult)

    (setImportState as? SetImportUiState.Conflicts)?.let { conflictsState ->
        ConflictsDialog(
            plan = conflictsState.plan,
            onConfirm = viewModel::resolveConflicts,
            onCancel = viewModel::cancelConflicts,
        )
    }

    DeleteSetDialogs(
        state = deleteState,
        onDismiss = viewModel::dismissDelete,
        onConfirm = viewModel::confirmDelete,
    )
}

/** Overflow menu for the top bar - shares [SetsScreenViewModel] with [SetsScreen] via the same NavBackStackEntry, same pattern as CardsListScreenTopBarStats. */
@Composable
fun SetsScreenTopBarActions() {
    val viewModel: SetsScreenViewModel = hiltViewModel()
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }

    val openSetPackLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.importFromFileUri(context, it) }
    }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "Menu")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Load from filesystem") },
                onClick = {
                    expanded = false
                    openSetPackLauncher.launch("*/*")
                },
            )
            DropdownMenuItem(
                text = { Text("Discover") },
                onClick = {
                    expanded = false
                    viewModel.discover()
                },
            )
        }
    }
}

@Composable
private fun SetRow(summary: CardSetSummary, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${summary.language} ${summary.setCode}" + (summary.displayName?.let { " — $it" } ?: ""),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${summary.ownedCount}/${summary.totalPrints} owned",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (summary.isDynamic) {
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete set",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DiscoverDialog(
    state: DiscoverUiState,
    onDismiss: () -> Unit,
    onImportSelected: (List<DiscoverSetEntry>) -> Unit,
) {
    when (state) {
        is DiscoverUiState.Idle -> return
        is DiscoverUiState.Loading -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                confirmButton = {},
                title = { Text("Discover") },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Text("Fetching available sets...", modifier = Modifier.padding(start = 12.dp))
                    }
                },
            )
        }
        is DiscoverUiState.Error -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
                title = { Text("Discover failed") },
                text = { Text(state.message) },
            )
        }
        is DiscoverUiState.Loaded -> {
            var selected by remember(state.entries) { mutableStateOf(setOf<DiscoverSetEntry>()) }
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Available sets") },
                text = {
                    if (state.entries.isEmpty()) {
                        Text("No sets available.")
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                            items(state.entries) { entry ->
                                val owned = (entry.set_language to entry.set_code) in state.alreadyOwned
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = !owned) {
                                            selected = if (entry in selected) selected - entry else selected + entry
                                        }
                                        .padding(vertical = 4.dp),
                                ) {
                                    Checkbox(
                                        checked = owned || entry in selected,
                                        onCheckedChange = { checked ->
                                            selected = if (checked) selected + entry else selected - entry
                                        },
                                        enabled = !owned,
                                    )
                                    Column {
                                        Text(
                                            "${entry.set_language} ${entry.set_code} - ${entry.set_name}",
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                        if (owned) {
                                            Text(
                                                "Already loaded",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        enabled = selected.isNotEmpty(),
                        onClick = { onImportSelected(selected.toList()) },
                    ) { Text("Import selected (${selected.size})") }
                },
                dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
            )
        }
    }
}

@Composable
private fun SetImportProgressDialog(state: SetImportUiState, onDismiss: () -> Unit) {
    when (state) {
        is SetImportUiState.Idle, is SetImportUiState.Conflicts -> return
        is SetImportUiState.Progress -> {
            AlertDialog(
                onDismissRequest = {},
                confirmButton = {},
                title = { Text("Importing set") },
                text = {
                    Column {
                        val fraction = state.fraction
                        if (fraction != null) {
                            LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
                        } else {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        Text(state.message, modifier = Modifier.padding(top = 10.dp), style = MaterialTheme.typography.bodySmall)
                    }
                },
            )
        }
        is SetImportUiState.Error -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
                title = { Text("Import failed") },
                text = { Text(state.message) },
            )
        }
        is SetImportUiState.Done -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
                title = { Text("Import complete") },
                text = {
                    Column {
                        state.results.forEach { result ->
                            Text(
                                "${result.setLanguage} ${result.setCode} - ${result.setName}: " +
                                    "${result.cardsAdded} added, ${result.cardsUpdated} updated, " +
                                    "${result.cardsSkipped} unchanged, ${result.prototypesCreated} new cards",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(vertical = 2.dp),
                            )
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun ConflictsDialog(
    plan: SetImportPlan,
    onConfirm: (Map<Int, ConflictResolution>) -> Unit,
    onCancel: () -> Unit,
) {
    var resolutions by remember(plan) {
        mutableStateOf(plan.conflicts.associate { it.existingPrintId to ConflictResolution.TAKE_NEW })
    }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Review changes — ${plan.pack.set_language} ${plan.pack.set_code}") },
        text = {
            Column {
                Text(
                    "${plan.conflicts.size} card(s) changed since this set was last loaded. " +
                        "Tap OLD or NEW to choose which one to keep for each.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LazyColumn(
                    modifier = Modifier
                        .heightIn(max = 420.dp)
                        .padding(top = 10.dp),
                ) {
                    items(plan.conflicts, key = { it.existingPrintId }) { conflict ->
                        ConflictRow(
                            conflict = conflict,
                            resolution = resolutions[conflict.existingPrintId] ?: ConflictResolution.TAKE_NEW,
                            onPick = { resolution ->
                                resolutions = resolutions + (conflict.existingPrintId to resolution)
                            },
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(resolutions) }) { Text("Import") } },
        dismissButton = { TextButton(onClick = onCancel) { Text("Keep all as-is") } },
    )
}

@Composable
private fun ConflictRow(
    conflict: SetImportConflict,
    resolution: ConflictResolution,
    onPick: (ConflictResolution) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        LazyImage(
            filename = "card_images_low/${conflict.existingPrintId}.jpg",
            modifier = Modifier.width(44.dp),
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                conflict.displayName,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${conflict.setNumber}/${conflict.setCount}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ConflictCell(
                    label = "OLD",
                    lines = conflictSideLines(conflict, isNew = false),
                    selected = resolution == ConflictResolution.KEEP_OLD,
                    modifier = Modifier.weight(1f),
                    onClick = { onPick(ConflictResolution.KEEP_OLD) },
                )
                ConflictCell(
                    label = "NEW",
                    lines = conflictSideLines(conflict, isNew = true),
                    selected = resolution == ConflictResolution.TAKE_NEW,
                    modifier = Modifier.weight(1f),
                    onClick = { onPick(ConflictResolution.TAKE_NEW) },
                )
            }
        }
    }
}

private fun conflictSideLines(conflict: SetImportConflict, isNew: Boolean): List<String> {
    val fieldLines = conflict.fieldDiffs.map { diff ->
        "${diff.label}: ${(if (isNew) diff.new else diff.old) ?: "—"}"
    }
    val assetLine = when (conflict.assetDiff) {
        null -> null
        AssetDiffKind.ADDED -> if (isNew) "Image added" else "No image"
        AssetDiffKind.CHANGED -> if (isNew) "Image changed" else "Current image"
    }
    return if (assetLine != null) fieldLines + assetLine else fieldLines
}

@Composable
private fun ConflictCell(
    label: String,
    lines: List<String>,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f),
            )
            .border(
                1.dp,
                if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onClick)
            .padding(8.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        lines.forEach { line ->
            Text(
                line,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun DeleteSetDialogs(
    state: DeleteSetUiState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    when (state) {
        is DeleteSetUiState.Idle -> return
        is DeleteSetUiState.Deleting -> {
            AlertDialog(
                onDismissRequest = {},
                confirmButton = {},
                title = { Text("Deleting set") },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Text("Removing cards and images...", modifier = Modifier.padding(start = 12.dp))
                    }
                },
            )
        }
        is DeleteSetUiState.Confirm -> {
            val summary = state.summary
            AppConfirmDialog(
                title = "Delete set?",
                message = "Delete ${summary.language} ${summary.setCode}" +
                    (summary.displayName?.let { " — $it" } ?: "") +
                    "? This removes ${summary.totalPrints} card(s) and their stored images. This can't be undone.",
                confirmText = "Delete",
                onConfirm = onConfirm,
                onDismiss = onDismiss,
            )
        }
        is DeleteSetUiState.Blocked -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
                title = { Text("Can't delete set") },
                text = {
                    Text(
                        "${state.transactedCount} card(s) from this set are referenced by transactions. " +
                            "Remove those from their transactions first.",
                    )
                },
            )
        }
    }
}
