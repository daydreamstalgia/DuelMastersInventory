package ro.ddnostalgia.duelmastersinventory.features.export.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import ro.ddnostalgia.duelmastersinventory.nav.Routes
import ro.ddnostalgia.duelmastersinventory.shared.ui.components.core.InputField
import ro.ddnostalgia.duelmastersinventory.shared.ui.google.rememberGoogleAuthContext

/** Same leading-name parse as SpreadsheetConfigViewScreen's RECORD_NAME_REGEX, kept local since
 * this screen only needs it for the read-only display pill. */
private val RECORD_NAME_REGEX = Regex("""(\w+)\(""")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpreadsheetConfigEditScreen(
    navController: NavController,
    viewModel: SpreadsheetConfigEditScreenViewModel = hiltViewModel()
) {
    val form by viewModel.form.collectAsState()

    LaunchedEffect(form.id) {
        viewModel.loadSpreadsheetConfig()
    }

    @Composable
    fun Form(modifier: Modifier = Modifier, onSubmit: () -> Unit) {
        val inputModifier = Modifier.fillMaxWidth().padding(top = 9.dp)

        Column(modifier) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                if (form.id != null) {
                    InfoRow("SPREADSHEET CONFIG ID", "${form.id}")
                }
                InfoRow("GOOGLE ACCOUNT", form.email ?: "—", modifier = Modifier.padding(top = 10.dp))
            }

            InputField(
                value = form.title ?: "",
                onValueChange = { viewModel.updateForm(title = it) },
                label = "Title",
                modifier = inputModifier.padding(top = 18.dp),
            )

            InputField(
                value = form.description ?: "",
                onValueChange = { viewModel.updateForm(description = it) },
                label = "Description",
                modifier = inputModifier,
            )

            if (form.id != null) {
                // Record type / columns / groups: shown read-only, parsed verbatim from the
                // loaded config. The mockup renders these as an editable segmented control and
                // drag/remove chips, but SpreadsheetConfigEditScreenViewModel.saveChanges doesn't
                // wire any of the three back to persistence yet (see its comment) — rendering
                // interactive-looking controls here would silently discard edits, so instead
                // these use the same dimmed/disabled treatment as ActorViewScreen's
                // "Delete unavailable" state, with a caption explaining why.
                SectionLabel("RECORD TYPE")
                DimmedPill(recordTypeLabel(form.recordType))

                val columnList = remember(form.columnsDisplay) {
                    form.columnsDisplay.split(",").map { it.trim() }.filter { it.isNotBlank() }
                }
                SectionLabel("COLUMNS")
                if (columnList.isEmpty()) {
                    DimmedText("—")
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        columnList.forEach { DimmedChip(it) }
                    }
                }

                val groupNames = remember(form.groupsDisplay) {
                    Regex("\"([^\"]+)\"").findAll(form.groupsDisplay).map { it.groupValues[1] }.toList()
                }
                SectionLabel("GROUP BY")
                if (groupNames.isEmpty()) {
                    DimmedText("—")
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        groupNames.forEach { DimmedChip(it) }
                    }
                }

                Text(
                    "Not editable from this screen yet — record type, columns, and groups are shown as stored.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                )
            }

            Button(
                onClick = onSubmit,
                enabled = form.email != null && form.title != null,
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier.fillMaxWidth().height(52.dp).padding(top = 20.dp),
            ) {
                Text("Save", style = MaterialTheme.typography.labelLarge)
            }
        }
    }

    val auth = rememberGoogleAuthContext()

    val sheetsService = auth.sheetsService

    var submitTriggered by remember{ mutableStateOf(false) }

    LaunchedEffect(sheetsService, submitTriggered) {
        Log.d("SCREEN", "Trigger $sheetsService, $submitTriggered")
        if(submitTriggered && sheetsService!=null) {
            viewModel.saveChanges(sheetsService)
            navController.navigate(Routes.Spreadsheets.route)
        }
    }

    Column(
        modifier= Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Form(Modifier.fillMaxWidth().padding(16.dp)) {
            submitTriggered = true
        }
    }
}

private fun recordTypeLabel(recordType: String?): String {
    if (recordType.isNullOrBlank()) return "—"
    return RECORD_NAME_REGEX.find(recordType)?.groupValues?.get(1)?.ifBlank { null } ?: recordType
}

@Composable
private fun InfoRow(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 3.dp))
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
private fun DimmedText(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
}

@Composable
private fun DimmedPill(label: String) {
    Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(17.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
private fun DimmedChip(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}
