package ro.daydreamstalgia.duelmastersinventory.features.onboarding.ui.screens

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.setimport.CardSetImportRepository
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.setimport.DiscoverRepository
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.setimport.DiscoverSetEntry
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.setimport.ImportProgress
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.setimport.SetImportResult
import ro.daydreamstalgia.duelmastersinventory.shared.data.preferences.UserPreferencesRepository
import ro.daydreamstalgia.duelmastersinventory.shared.utils.types.BaseViewModel
import javax.inject.Inject

/** TCG DM-01..DM-12 vs. everything else JP-language classified as OCG - client-side, no CDN metadata, see specs/0006. */
private val TCG_SET_CODES = (1..12).map { "DM-%02d".format(it) }.toSet()

enum class JapaneseLine { TCG, OCG }

sealed interface OnboardingStep {
    data object OptIn : OnboardingStep
    data object Focus : OnboardingStep
    data class SetPicker(val entries: List<DiscoverSetEntry>) : OnboardingStep
}

sealed interface CatalogLoadState {
    data object Idle : CatalogLoadState
    data object Loading : CatalogLoadState
    data class Loaded(val entries: List<DiscoverSetEntry>) : CatalogLoadState
    data class Error(val message: String) : CatalogLoadState
}

sealed interface OnboardingImportState {
    data object Idle : OnboardingImportState
    data class Progress(val message: String, val fraction: Float?) : OnboardingImportState
    data class Done(val results: List<SetImportResult>) : OnboardingImportState
    data class Error(val message: String) : OnboardingImportState
}

@HiltViewModel
class OnboardingScreenViewModel @Inject constructor(
    private val discoverRepository: DiscoverRepository,
    private val cardSetImportRepository: CardSetImportRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) : BaseViewModel() {

    private val _step = MutableStateFlow<OnboardingStep>(OnboardingStep.OptIn)
    val step: StateFlow<OnboardingStep> = _step

    private val _catalogState = MutableStateFlow<CatalogLoadState>(CatalogLoadState.Idle)
    val catalogState: StateFlow<CatalogLoadState> = _catalogState

    private val _importState = MutableStateFlow<OnboardingImportState>(OnboardingImportState.Idle)
    val importState: StateFlow<OnboardingImportState> = _importState

    /** True once every exit path (skip, import done, import backed-out) has been taken - AppNavHost pops to CardList on this. */
    private val _finished = MutableStateFlow(false)
    val finished: StateFlow<Boolean> = _finished

    private var fullCatalog: List<DiscoverSetEntry> = emptyList()

    fun skip() {
        finish()
    }

    fun startFocus() {
        _step.value = OnboardingStep.Focus
        if (_catalogState.value !is CatalogLoadState.Loaded) loadCatalog()
    }

    private fun loadCatalog() {
        viewModelScope.launch {
            _catalogState.value = CatalogLoadState.Loading
            try {
                fullCatalog = discoverRepository.fetchCatalog()
                _catalogState.value = CatalogLoadState.Loaded(fullCatalog)
            } catch (e: Exception) {
                _catalogState.value = CatalogLoadState.Error(e.message ?: "Could not reach the set catalog")
            }
        }
    }

    fun retryLoadCatalog() = loadCatalog()

    fun dismissImportError() {
        _importState.value = OnboardingImportState.Idle
    }

    /** [languages] are the raw set_language values chosen; [japaneseLines] filters JP-language entries further. */
    fun confirmFocus(languages: Set<String>, japaneseLines: Set<JapaneseLine>) {
        val filtered = fullCatalog.filter { entry ->
            if (entry.set_language !in languages) return@filter false
            if (entry.set_language != "JP") return@filter true
            val isTcg = entry.set_code in TCG_SET_CODES
            (isTcg && JapaneseLine.TCG in japaneseLines) || (!isTcg && JapaneseLine.OCG in japaneseLines)
        }
        _step.value = OnboardingStep.SetPicker(filtered)
    }

    fun backToFocus() {
        _step.value = OnboardingStep.Focus
    }

    /** Distinct languages available across the whole catalog, for the Focus step's language picker. */
    fun availableLanguages(): List<String> = fullCatalog.map { it.set_language }.distinct().sorted()

    fun importSelected(entries: List<DiscoverSetEntry>) {
        if (entries.isEmpty()) {
            finish()
            return
        }
        // Ignore repeat taps while an import is already running.
        if (_importState.value is OnboardingImportState.Progress) return
        _importState.value = OnboardingImportState.Progress("Starting download...", null)
        viewModelScope.launch {
            val results = mutableListOf<SetImportResult>()
            try {
                for (entry in entries) {
                    val zipFile = discoverRepository.downloadZip(entry) { emitProgress(it) }
                    val plan = try {
                        zipFile.inputStream().use { stream ->
                            cardSetImportRepository.scanZip(stream) { emitProgress(it) }
                        }
                    } finally {
                        zipFile.delete()
                    }
                    // A brand-new catalog can't have reimport conflicts - plan.conflicts is always
                    // empty here, so an empty resolutions map (defaults every entry to TAKE_NEW,
                    // moot since there's nothing to resolve) matches specs/0004's applyPlan contract.
                    val result = cardSetImportRepository.applyPlan(plan, emptyMap()) { emitProgress(it) }
                    results.add(result)
                }
                _importState.value = OnboardingImportState.Done(results)
            } catch (e: Exception) {
                _importState.value = OnboardingImportState.Error(e.message ?: "Import failed")
            }
        }
    }

    private fun emitProgress(progress: ImportProgress) {
        _importState.value = OnboardingImportState.Progress(progress.message, progress.fraction)
    }

    fun finish() {
        viewModelScope.launch {
            userPreferencesRepository.setOnboardingCompleted()
            _finished.value = true
        }
    }
}
