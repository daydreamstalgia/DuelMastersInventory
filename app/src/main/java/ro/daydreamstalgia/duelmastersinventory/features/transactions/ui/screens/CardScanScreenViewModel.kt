package ro.daydreamstalgia.duelmastersinventory.features.transactions.ui.screens

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ro.daydreamstalgia.duelmastersinventory.nav.Routes
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.model.CardPrintWithPrototype
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.model.CardSetOrder
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.repository.CardImageMatch
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.repository.CardImageMatchRepository
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.repository.CardPrintRepository
import ro.daydreamstalgia.duelmastersinventory.shared.data.transactions.model.TransactedCard
import ro.daydreamstalgia.duelmastersinventory.shared.data.transactions.repository.TransactedCardRepository
import ro.daydreamstalgia.duelmastersinventory.shared.utils.image.ScannedCardStorage
import ro.daydreamstalgia.duelmastersinventory.shared.utils.types.RouteViewModel
import javax.inject.Inject

@HiltViewModel
class CardScanScreenViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val cardImageMatchRepository: CardImageMatchRepository,
    private val cardPrintRepository: CardPrintRepository,
    private val transactedCardRepository: TransactedCardRepository,
) : RouteViewModel<Routes.CardScan>(Routes.CardScan, savedStateHandle) {

    val transactionId: Int = routeArgInt(route.transactionIdArg)

    data class QueuedScan(
        val cardPrintId: Int,
        val condition: String?,
        val count: Int,
    )

    data class CandidateDetail(
        val match: CardImageMatch,
        val printWithPrototype: CardPrintWithPrototype?,
    )

    /** One selectable row of the set-filter slideout - a `(language, set)` pair loaded into this catalog. */
    data class ScanSetOption(
        val language: String,
        val setCode: String,
        val displayName: String?,
    )

    private val _printsById = MutableStateFlow<Map<Int, CardPrintWithPrototype>>(emptyMap())
    private val _queue = MutableStateFlow<List<QueuedScan>>(emptyList())
    val queue = _queue.asStateFlow()

    private val _candidates = MutableStateFlow<List<CardImageMatch>>(emptyList())
    private val _candidateIndex = MutableStateFlow(0)

    private val _matching = MutableStateFlow(false)
    val matching = _matching.asStateFlow()

    /** True once a capture comes back with zero confident candidates - the screen swaps the
     * confirm sheet for a search-and-add slideout instead of interrupting with a toast. Cleared
     * by [skipSearch] or once a result is added via [addSearchResult]. */
    private val _searchActive = MutableStateFlow(false)
    val searchActive = _searchActive.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    /** Prints matching [searchQuery] by name or `displayId()`, empty until the user types something. */
    val searchResults = kotlinx.coroutines.flow.combine(_printsById, _searchQuery) { printsById, query ->
        val q = query.trim().lowercase()
        if (q.isEmpty()) return@combine emptyList()
        printsById.values
            .filter {
                (it.prototype.name?.lowercase()?.contains(q) == true) ||
                    it.print.displayId().lowercase().contains(q)
            }
            .sortedBy { it.prototype.name }
            .take(40)
    }.stateInViewModelScope(emptyList())

    private val _matchVisualization = MutableStateFlow<Bitmap?>(null)
    /** Side-by-side ORB keypoint-match render for [currentCandidate], see [CardImageMatchRepository.visualizeMatch]. Recomputed whenever the candidate changes (paging via "Not it" included); null while it's still rendering or there's no candidate. */
    val matchVisualization = _matchVisualization.asStateFlow()

    private val _capturedBitmap = MutableStateFlow<Bitmap?>(null)
    /** The cropped frame that entered the match pipeline for the current capture - shown in the no-match search slideout so the user can see what they're searching for. */
    val capturedBitmap = _capturedBitmap.asStateFlow()

    /** Every `(language, set)` pair loaded into this catalog, for the set-filter slideout - derived from [_printsById] rather than a separate query since that's already the full loaded catalog. */
    val availableSets = _printsById.map { printsById ->
        printsById.values
            .map { it.print.language to it.print.set }
            .distinct()
            .sortedWith(CardSetOrder.comparator)
            .map { (language, set) -> ScanSetOption(language, set, CardSetOrder.displayName(language, set)) }
    }.stateInViewModelScope(emptyList())

    /** Selected `(language, set)` pairs to narrow ORB matching to - empty means "no filter, match every set" (spec 0008). In-memory only, like the scan screen's torch/condition state - doesn't survive leaving this screen. */
    private val _selectedSetFilter = MutableStateFlow<Set<Pair<String, String>>>(emptySet())
    val selectedSetFilter = _selectedSetFilter.asStateFlow()

    fun setSetFilter(selection: Set<Pair<String, String>>) {
        _selectedSetFilter.value = selection
    }

    val currentCandidate = kotlinx.coroutines.flow.combine(
        _candidates, _candidateIndex, _printsById
    ) { candidates, index, printsById ->
        candidates.getOrNull(index)?.let { CandidateDetail(it, printsById[it.cardPrintId]) }
    }.stateInViewModelScope()

    val hasMoreCandidates = kotlinx.coroutines.flow.combine(_candidates, _candidateIndex) { c, i -> i < c.size - 1 }
        .stateInViewModelScope(false)

    init {
        viewModelScope.launch {
            cardImageMatchRepository.ensureIndexBuilt()
        }
        viewModelScope.launch {
            val prints = cardPrintRepository.getAllWithPrototype().first()
            _printsById.value = prints.associateBy { it.print.id }
        }
        viewModelScope.launch(Dispatchers.Default) {
            currentCandidate.collectLatest { detail ->
                _matchVisualization.value = null
                val bitmap = _capturedBitmap.value
                if (detail != null && bitmap != null) {
                    _matchVisualization.value = cardImageMatchRepository.visualizeMatch(bitmap, detail.match.cardPrintId)
                }
            }
        }
    }

    /**
     * [bitmap] is a single capture frame - burst capture was dropped (see
     * decisions/0009) since matching every frame of a 4-shot burst against
     * the whole index made a scan press feel slow; one frame is enough now
     * that ORB (not a global hash) is doing the matching.
     */
    fun onCaptured(bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.Default) {
            _matching.value = true
            _capturedBitmap.value = bitmap
            val filter = _selectedSetFilter.value
            val allowedPrintIds = if (filter.isEmpty()) {
                null
            } else {
                _printsById.value.values
                    .filter { (it.print.language to it.print.set) in filter }
                    .map { it.print.id }
                    .toSet()
            }
            val matches = cardImageMatchRepository.findMatches(bitmap, allowedPrintIds = allowedPrintIds)
            _candidates.value = matches
            _candidateIndex.value = 0
            _matching.value = false
            _searchActive.value = matches.isEmpty()
            if (matches.isEmpty()) _searchQuery.value = ""
        }
    }

    /** "Not it" - try the next best candidate, or give up back to the camera. */
    fun rejectCandidate() {
        if (_candidateIndex.value < _candidates.value.size - 1) {
            _candidateIndex.update { it + 1 }
        } else {
            dismissCandidates()
        }
    }

    fun dismissCandidates() {
        _candidates.value = emptyList()
        _candidateIndex.value = 0
    }

    /** Give up on the no-match search too - back to camera/picked-image, nothing added. */
    fun skipSearch() {
        _searchActive.value = false
        _searchQuery.value = ""
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun queueCandidate(cardPrintId: Int, condition: String?) {
        addToQueue(cardPrintId, condition)
        persistCapture(cardPrintId, condition)
        dismissCandidates()
    }

    /** Add a card found via the no-match search slideout instead of a confident ORB candidate. */
    fun addSearchResult(cardPrintId: Int, condition: String?) {
        addToQueue(cardPrintId, condition)
        persistCapture(cardPrintId, condition)
        skipSearch()
    }

    private fun addToQueue(cardPrintId: Int, condition: String?) {
        _queue.update { current ->
            val existingIndex = current.indexOfFirst { it.cardPrintId == cardPrintId && it.condition == condition }
            if (existingIndex >= 0) {
                current.toMutableList().apply {
                    this[existingIndex] = this[existingIndex].copy(count = this[existingIndex].count + 1)
                }
            } else {
                current + QueuedScan(cardPrintId, condition, 1)
            }
        }
    }

    /** Saves the capture that entered the match pipeline alongside the confirmed print identity and condition, see [ScannedCardStorage]. Best-effort - failures are logged, not surfaced. */
    private fun persistCapture(cardPrintId: Int, condition: String?) {
        val bitmap = _capturedBitmap.value ?: return
        val print = _printsById.value[cardPrintId]?.print ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val identity = "${print.language}_${print.set}_${print.setNumber}_${print.setCount}"
            ScannedCardStorage.save(bitmap, identity, condition)
        }
    }

    fun printFor(cardPrintId: Int): CardPrintWithPrototype? = _printsById.value[cardPrintId]

    /** Writes every queued copy straight to the transaction - nothing is buffered upstream. */
    suspend fun commitQueue() {
        val cards = _queue.value.flatMap { queued ->
            List(queued.count) {
                TransactedCard(
                    cardPrintId = queued.cardPrintId,
                    inTransactionId = transactionId,
                    condition = queued.condition,
                )
            }
        }
        if (cards.isNotEmpty()) {
            transactedCardRepository.insertAll(cards)
        }
        _queue.value = emptyList()
    }
}
