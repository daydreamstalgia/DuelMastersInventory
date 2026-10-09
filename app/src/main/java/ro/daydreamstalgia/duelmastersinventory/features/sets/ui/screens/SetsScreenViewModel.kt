package ro.daydreamstalgia.duelmastersinventory.features.sets.ui.screens

import android.content.Context
import android.net.Uri
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.repository.CardSetRepository
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.repository.CardSetSummary
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.setimport.CardSetImportRepository
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.setimport.ConflictResolution
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.setimport.DiscoverRepository
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.setimport.DiscoverSetEntry
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.setimport.ImportProgress
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.setimport.SetImportPlan
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.setimport.SetImportResult
import ro.daydreamstalgia.duelmastersinventory.shared.utils.types.BaseViewModel
import javax.inject.Inject

sealed interface DiscoverUiState {
    data object Idle : DiscoverUiState
    data object Loading : DiscoverUiState
    data class Loaded(val entries: List<DiscoverSetEntry>, val alreadyOwned: Set<Pair<String, String>>) : DiscoverUiState
    data class Error(val message: String) : DiscoverUiState
}

sealed interface SetImportUiState {
    data object Idle : SetImportUiState
    data class Progress(val message: String, val fraction: Float?) : SetImportUiState
    data class Conflicts(val plan: SetImportPlan) : SetImportUiState
    data class Done(val results: List<SetImportResult>) : SetImportUiState
    data class Error(val message: String) : SetImportUiState
}

sealed interface DeleteSetUiState {
    data object Idle : DeleteSetUiState
    data class Confirm(val summary: CardSetSummary) : DeleteSetUiState
    data class Blocked(val summary: CardSetSummary, val transactedCount: Int) : DeleteSetUiState
    data object Deleting : DeleteSetUiState
}

private sealed interface PackSource {
    data class FileUri(val context: Context, val uri: Uri) : PackSource
    data class Discover(val entry: DiscoverSetEntry) : PackSource
}

@HiltViewModel
class SetsScreenViewModel @Inject constructor(
    private val cardSetRepository: CardSetRepository,
    private val cardSetImportRepository: CardSetImportRepository,
    private val discoverRepository: DiscoverRepository,
) : BaseViewModel() {

    val setSummaries = cardSetRepository.getSetSummaries().stateInViewModelScope(emptyList())

    private val _discoverState = MutableStateFlow<DiscoverUiState>(DiscoverUiState.Idle)
    val discoverState: StateFlow<DiscoverUiState> = _discoverState

    private val _setImportState = MutableStateFlow<SetImportUiState>(SetImportUiState.Idle)
    val setImportState: StateFlow<SetImportUiState> = _setImportState

    private val _deleteState = MutableStateFlow<DeleteSetUiState>(DeleteSetUiState.Idle)
    val deleteState: StateFlow<DeleteSetUiState> = _deleteState

    private var pendingResolution: CompletableDeferred<Map<Int, ConflictResolution>>? = null

    fun dismissDiscover() {
        _discoverState.value = DiscoverUiState.Idle
    }

    fun dismissSetImportResult() {
        _setImportState.value = SetImportUiState.Idle
    }

    fun discover() {
        viewModelScope.launch {
            _discoverState.value = DiscoverUiState.Loading
            try {
                val entries = discoverRepository.fetchCatalog()
                val owned = setSummaries.value.map { it.language to it.setCode }.toSet()
                _discoverState.value = DiscoverUiState.Loaded(entries, owned)
            } catch (e: Exception) {
                _discoverState.value = DiscoverUiState.Error(e.message ?: "Discover failed")
            }
        }
    }

    fun importFromFileUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _discoverState.value = DiscoverUiState.Idle
            runImportQueue(listOf(PackSource.FileUri(context, uri)))
        }
    }

    fun importFromDiscover(entries: List<DiscoverSetEntry>) {
        viewModelScope.launch {
            _discoverState.value = DiscoverUiState.Idle
            runImportQueue(entries.map { PackSource.Discover(it) })
        }
    }

    /** Resolves the pack currently paused on [SetImportUiState.Conflicts] and lets the import queue continue. */
    fun resolveConflicts(resolutions: Map<Int, ConflictResolution>) {
        pendingResolution?.complete(resolutions)
        pendingResolution = null
    }

    /** Cancelling the picker keeps every conflicting card as-is (same effect as the old silent skip) rather than aborting the whole pack. */
    fun cancelConflicts() {
        val plan = (_setImportState.value as? SetImportUiState.Conflicts)?.plan
        val allOld = plan?.conflicts?.associate { it.existingPrintId to ConflictResolution.KEEP_OLD }.orEmpty()
        pendingResolution?.complete(allOld)
        pendingResolution = null
    }

    private suspend fun runImportQueue(sources: List<PackSource>) {
        val results = mutableListOf<SetImportResult>()
        try {
            for (source in sources) {
                val plan = scanSource(source)

                val resolutions = if (plan.conflicts.isEmpty()) {
                    emptyMap()
                } else {
                    val deferred = CompletableDeferred<Map<Int, ConflictResolution>>()
                    pendingResolution = deferred
                    _setImportState.value = SetImportUiState.Conflicts(plan)
                    deferred.await()
                }

                _setImportState.value = SetImportUiState.Progress("Applying changes...", null)
                val result = cardSetImportRepository.applyPlan(plan, resolutions) { emitProgress(it) }
                results.add(result)
            }
            _setImportState.value = SetImportUiState.Done(results)
        } catch (e: Exception) {
            _setImportState.value = SetImportUiState.Error(e.message ?: "Import failed")
        }
    }

    private suspend fun scanSource(source: PackSource): SetImportPlan = withContext(Dispatchers.IO) {
        when (source) {
            is PackSource.FileUri -> {
                val stream = source.context.contentResolver.openInputStream(source.uri)
                    ?: throw IllegalStateException("Could not open selected file")
                cardSetImportRepository.scanZip(stream) { emitProgress(it) }
            }
            is PackSource.Discover -> {
                val zipFile = discoverRepository.downloadZip(source.entry) { emitProgress(it) }
                try {
                    zipFile.inputStream().use { cardSetImportRepository.scanZip(it) { p -> emitProgress(p) } }
                } finally {
                    zipFile.delete()
                }
            }
        }
    }

    private fun emitProgress(progress: ImportProgress) {
        _setImportState.value = SetImportUiState.Progress(progress.message, progress.fraction)
    }

    fun requestDelete(summary: CardSetSummary) {
        _deleteState.value = if (summary.transactedCount > 0) {
            DeleteSetUiState.Blocked(summary, summary.transactedCount)
        } else {
            DeleteSetUiState.Confirm(summary)
        }
    }

    fun dismissDelete() {
        _deleteState.value = DeleteSetUiState.Idle
    }

    fun confirmDelete() {
        val confirm = _deleteState.value as? DeleteSetUiState.Confirm ?: return
        viewModelScope.launch {
            _deleteState.value = DeleteSetUiState.Deleting
            when (val result = cardSetRepository.deleteSet(confirm.summary.language, confirm.summary.setCode)) {
                is CardSetRepository.DeleteResult.Deleted ->
                    _deleteState.value = DeleteSetUiState.Idle
                is CardSetRepository.DeleteResult.Blocked ->
                    _deleteState.value = DeleteSetUiState.Blocked(confirm.summary, result.transactedCount)
            }
        }
    }
}
