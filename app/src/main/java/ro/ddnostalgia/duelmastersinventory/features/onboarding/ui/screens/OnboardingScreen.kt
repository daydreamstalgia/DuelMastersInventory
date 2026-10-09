package ro.ddnostalgia.duelmastersinventory.features.onboarding.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import ro.ddnostalgia.duelmastersinventory.nav.Routes
import ro.ddnostalgia.duelmastersinventory.shared.data.cards.setimport.DiscoverSetEntry

@Composable
fun OnboardingScreen(navController: NavController) {
    val viewModel: OnboardingScreenViewModel = hiltViewModel()

    val step by viewModel.step.collectAsState()
    val catalogState by viewModel.catalogState.collectAsState()
    val importState by viewModel.importState.collectAsState()
    val finished by viewModel.finished.collectAsState()

    LaunchedEffect(finished) {
        if (finished) {
            navController.navigate(Routes.CardList.route) {
                popUpTo(Routes.Onboarding.route) { inclusive = true }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        when (val current = step) {
            is OnboardingStep.OptIn -> OptInStep(onYes = viewModel::startFocus, onNo = viewModel::skip)
            is OnboardingStep.Focus -> FocusStep(
                catalogState = catalogState,
                onRetry = viewModel::retryLoadCatalog,
                availableLanguages = viewModel::availableLanguages,
                onConfirm = viewModel::confirmFocus,
            )
            is OnboardingStep.SetPicker -> SetPickerStep(
                entries = current.entries,
                onBack = viewModel::backToFocus,
                onImport = viewModel::importSelected,
            )
        }
    }

    OnboardingImportDialog(
        state = importState,
        onDone = viewModel::finish,
        onBackToPicker = viewModel::dismissImportError,
        onSkip = viewModel::finish,
    )
}

@Composable
private fun OptInStep(onYes: () -> Unit, onNo: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Text("Want to set up your list of desired collectibles?", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Pick sets to download now, or start with an empty catalog and add sets later from the Sets screen.",
            modifier = Modifier.padding(top = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(modifier = Modifier.padding(top = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onNo) { Text("No, skip") }
            Button(onClick = onYes) { Text("Yes, let's go") }
        }
    }
}

@Composable
private fun FocusStep(
    catalogState: CatalogLoadState,
    onRetry: () -> Unit,
    availableLanguages: () -> List<String>,
    onConfirm: (Set<String>, Set<JapaneseLine>) -> Unit,
) {
    when (catalogState) {
        is CatalogLoadState.Idle, is CatalogLoadState.Loading -> {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Text("Fetching available sets...", modifier = Modifier.padding(top = 12.dp))
            }
        }
        is CatalogLoadState.Error -> {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                Text("Couldn't reach the set catalog", style = MaterialTheme.typography.headlineSmall)
                Text(catalogState.message, modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) { Text("Retry") }
            }
        }
        is CatalogLoadState.Loaded -> {
            val languages = remember(catalogState) { availableLanguages() }
            var selectedLanguages by remember(languages) { mutableStateOf(setOf<String>()) }
            var selectedJapaneseLines by remember { mutableStateOf(setOf(JapaneseLine.TCG, JapaneseLine.OCG)) }

            Column(modifier = Modifier.fillMaxSize()) {
                Text("What is your collecting focus?", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Pick the language(s) you collect.",
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(modifier = Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    languages.forEach { language ->
                        FilterChip(
                            selected = language in selectedLanguages,
                            onClick = {
                                selectedLanguages = if (language in selectedLanguages) {
                                    selectedLanguages - language
                                } else {
                                    selectedLanguages + language
                                }
                            },
                            label = { Text(language) },
                        )
                    }
                }

                if ("JP" in selectedLanguages) {
                    Text(
                        "Japanese: TCG (DM-01..DM-12) or OCG (everything else)?",
                        modifier = Modifier.padding(top = 20.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        JapaneseLine.entries.forEach { line ->
                            FilterChip(
                                selected = line in selectedJapaneseLines,
                                onClick = {
                                    selectedJapaneseLines = if (line in selectedJapaneseLines) {
                                        selectedJapaneseLines - line
                                    } else {
                                        selectedJapaneseLines + line
                                    }
                                },
                                label = { Text(line.name) },
                            )
                        }
                    }
                }

                Button(
                    enabled = selectedLanguages.isNotEmpty(),
                    onClick = { onConfirm(selectedLanguages, selectedJapaneseLines) },
                    modifier = Modifier.padding(top = 24.dp),
                ) { Text("Continue") }
            }
        }
    }
}

@Composable
private fun SetPickerStep(
    entries: List<DiscoverSetEntry>,
    onBack: () -> Unit,
    onImport: (List<DiscoverSetEntry>) -> Unit,
) {
    var selected by remember(entries) { mutableStateOf(entries.toSet()) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Choose sets to import", style = MaterialTheme.typography.headlineSmall)

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = selected.size == entries.size && entries.isNotEmpty(),
                    onCheckedChange = { checked -> selected = if (checked) entries.toSet() else emptySet() },
                )
                Text("Select all")
            }
            Text("${selected.size}/${entries.size}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (entries.isEmpty()) {
            Text(
                "No sets match that focus.",
                modifier = Modifier.padding(top = 16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
                items(entries) { entry ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = entry in selected,
                            onCheckedChange = { checked ->
                                selected = if (checked) selected + entry else selected - entry
                            },
                        )
                        Text("${entry.set_language} ${entry.set_code} - ${entry.set_name}")
                    }
                }
            }
        }

        Row(modifier = Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onBack) { Text("Back") }
            Button(onClick = { onImport(selected.toList()) }) {
                Text(if (selected.isEmpty()) "Skip" else "Import selected (${selected.size})")
            }
        }
    }
}

@Composable
private fun OnboardingImportDialog(
    state: OnboardingImportState,
    onDone: () -> Unit,
    onBackToPicker: () -> Unit,
    onSkip: () -> Unit,
) {
    when (state) {
        is OnboardingImportState.Idle -> return
        is OnboardingImportState.Progress -> {
            AlertDialog(
                onDismissRequest = {},
                confirmButton = {},
                title = { Text("Setting up your catalog") },
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
        is OnboardingImportState.Error -> {
            AlertDialog(
                onDismissRequest = onBackToPicker,
                title = { Text("Import failed") },
                text = { Text(state.message) },
                confirmButton = { TextButton(onClick = onBackToPicker) { Text("Back") } },
                dismissButton = { TextButton(onClick = onSkip) { Text("Skip for now") } },
            )
        }
        is OnboardingImportState.Done -> {
            AlertDialog(
                onDismissRequest = onDone,
                title = { Text("Catalog ready") },
                text = {
                    Column {
                        state.results.forEach { result ->
                            Text(
                                "${result.setLanguage} ${result.setCode} - ${result.setName}: ${result.cardsAdded} cards",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(vertical = 2.dp),
                            )
                        }
                    }
                },
                confirmButton = { TextButton(onClick = onDone) { Text("Start") } },
            )
        }
    }
}
