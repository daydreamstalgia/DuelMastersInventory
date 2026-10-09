package ro.daydreamstalgia.duelmastersinventory.features.export.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import ro.daydreamstalgia.duelmastersinventory.nav.Routes
import ro.daydreamstalgia.duelmastersinventory.nav.ScreenConfig
import ro.daydreamstalgia.duelmastersinventory.shared.service.OperationService
import ro.daydreamstalgia.duelmastersinventory.shared.ui.components.core.HyperlinkText
import ro.daydreamstalgia.duelmastersinventory.shared.ui.google.rememberGoogleAuthContext

/**
 * `groups` is stored as a mini-DSL, e.g. `"set"=groupByField(set),"civilization"=groupByField(civilization)`
 * (see SpreadsheetConfigDataProvider.parseGroups). Reused here, read-only, purely for display —
 * name to its raw grouping pattern, no persistence change.
 */
private val GROUPS_REGEX = Regex("\"([^\"]+)\"=([^,]+\\([^)]*\\))")

/** `recordType` is stored as `recordName(filters...)` — just the leading name for the pill display. */
private val RECORD_NAME_REGEX = Regex("""(\w+)\(""")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpreadsheetConfigViewScreen(
    navController: NavController,
    screenConfig: ScreenConfig? = null,
) {
    val viewModel: SpreadsheetConfigViewScreenViewModel = hiltViewModel()
    val spreadsheetConfig by viewModel.spreadsheetConfig.collectAsState()

    if (spreadsheetConfig == null) {
        return
    }

    val form = spreadsheetConfig!!
    val context = LocalContext.current

    LaunchedEffect(form.id) {
        screenConfig?.setDropdownAction(listOf(
            "Edit spreadsheet" to {
                navController.navigate(Routes.SpreadsheetConfigEdit.createRoute(form.id, form.email))
            },
            "Share" to {
                val url = "https://docs.google.com/spreadsheets/d/${form.spreadsheetId}"
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, url)
                    type = "text/plain"
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share via"))
            },
        ))
    }

    // Reuses ExportToSpreadsheetsScreenViewModel's exportToSpreadsheets/OperationService flow
    // verbatim — same sync mechanics as the spreadsheets list screen's per-card Sync button,
    // just also reachable from this detail view's footer.
    val exportViewModel: ExportToSpreadsheetsScreenViewModel = hiltViewModel()
    val auth = rememberGoogleAuthContext()
    val sheetsService = auth.sheetsService
    var syncing by remember { mutableStateOf(false) }

    LaunchedEffect(sheetsService, syncing) {
        if (syncing && sheetsService != null) {
            OperationService.runInNotifications(context) {
                exportViewModel.exportToSpreadsheets(sheetsService, form)
                syncing = false
            }
        }
    }

    val recordTypeLabel = remember(form.recordType) {
        RECORD_NAME_REGEX.find(form.recordType)?.groupValues?.get(1)?.ifBlank { null } ?: form.recordType.ifBlank { "—" }
    }
    val columnList = remember(form.columns) {
        form.columns.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }
    val groupRows = remember(form.groups) {
        GROUPS_REGEX.findAll(form.groups).map { it.groupValues[1] to it.groupValues[2].trim() }.toList()
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // hero card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.colorScheme.background)
                    ),
                    RoundedCornerShape(16.dp),
                )
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Text(form.title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Text(
                    "#${form.id}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (form.description.isNotBlank()) {
                Text(
                    form.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(top = 14.dp, bottom = 12.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(24.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        form.email.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                Text(
                    form.email,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f).padding(start = 9.dp),
                )
                HyperlinkText(
                    url = "https://docs.google.com/spreadsheets/d/${form.spreadsheetId}",
                    text = "Open in Sheets",
                )
            }
        }

        SectionLabel("RECORD TYPE")
        RecordTypePill(recordTypeLabel)

        SectionLabel("COLUMNS · IN ORDER")
        if (columnList.isEmpty()) {
            EmptyValueText()
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                columnList.forEachIndexed { index, column ->
                    ColumnChip(index + 1, column)
                }
            }
        }

        SectionLabel("GROUPS · ONE SHEET EACH")
        if (groupRows.isEmpty()) {
            EmptyValueText()
        } else {
            Column {
                groupRows.forEach { (name, pattern) ->
                    GroupRow(name, pattern)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 16.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(13.dp))
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Sync now", style = MaterialTheme.typography.labelLarge)
                Text(
                    "Writes the current collection data to this spreadsheet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Text(
                if (syncing) "Syncing…" else "⟳ Sync",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = if (syncing) 0.5f else 1f),
                        RoundedCornerShape(16.dp),
                    )
                    .clickable(enabled = !syncing) { syncing = true }
                    .padding(horizontal = 16.dp, vertical = 9.dp),
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
    )
}

@Composable
private fun EmptyValueText() {
    Text("—", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun RecordTypePill(label: String) {
    Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f), RoundedCornerShape(17.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
private fun ColumnChip(ordinal: Int, name: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(horizontal = 11.dp, vertical = 7.dp),
    ) {
        Text(
            "$ordinal",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
        Text(
            name,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

@Composable
private fun GroupRow(name: String, pattern: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(11.dp))
            .padding(horizontal = 12.dp, vertical = 11.dp),
    ) {
        Text(name, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
        Text(pattern, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
