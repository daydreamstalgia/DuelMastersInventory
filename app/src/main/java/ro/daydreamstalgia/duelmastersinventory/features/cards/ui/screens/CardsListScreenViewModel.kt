package ro.daydreamstalgia.duelmastersinventory.features.cards.ui.screens

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.model.CardPrototypeWithPrintsAndCount
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.model.CardsGroupBy
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.model.CardsSortBy
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.model.FilterMode
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.model.PrototypePrintsFilterParams
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.repository.CardPrototypeRepository
import ro.daydreamstalgia.duelmastersinventory.shared.data.preferences.GRID_COLUMNS_COMPACT
import ro.daydreamstalgia.duelmastersinventory.shared.data.preferences.UserPreferencesRepository
import ro.daydreamstalgia.duelmastersinventory.shared.data.transactions.repository.TransactedCardRepository
import ro.daydreamstalgia.duelmastersinventory.shared.utils.types.BaseViewModel
import ro.daydreamstalgia.duelmastersinventory.shared.utils.types.Civilization
import javax.inject.Inject

/** Real totals backing the "N cards / M unique" top-bar readout (spec `2a`), unaffected by
 *  the current search/filter selection - same underlying counts as [ro.daydreamstalgia.duelmastersinventory.shared.ui.scaffolds.DrawerStats]. */
data class CardsListStats(
    val totalCount: Int = 0,
    val uniqueCount: Int = 0,
)

@HiltViewModel
class CardsListScreenViewModel @Inject constructor(
    private val cardPrototypeRepository: CardPrototypeRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    transactedCardRepository: TransactedCardRepository,
): BaseViewModel() {

    private val _filterParams = MutableStateFlow(PrototypePrintsFilterParams())

    val filterParams = _filterParams.asStateFlow()


    val cards = getCardsFlow()
        .stateInViewModelScope()

    val gridColumns = userPreferencesRepository.gridColumns
        .stateInViewModelScope(GRID_COLUMNS_COMPACT)

    val groupBy = userPreferencesRepository.cardsGroupBy
        .stateInViewModelScope(CardsGroupBy.LANGUAGE_SET)

    val sortBy = userPreferencesRepository.cardsSortBy
        .stateInViewModelScope(CardsSortBy.NONE)

    val sortAscending = userPreferencesRepository.cardsSortAscending
        .stateInViewModelScope(true)

    val stats = combine(
        transactedCardRepository.getOwnedCardsCount(),
        cardPrototypeRepository.getOwnedUniquePrototypeCount(),
    ) { totalCount, uniqueCount ->
        CardsListStats(totalCount = totalCount, uniqueCount = uniqueCount)
    }.stateInViewModelScope(CardsListStats())

    fun setFilterParams(value: PrototypePrintsFilterParams) {
        _filterParams.value = value
    }

    fun setGridColumns(columns: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setGridColumns(columns)
        }
    }

    fun setGroupBy(groupBy: CardsGroupBy) {
        viewModelScope.launch {
            userPreferencesRepository.setCardsGroupBy(groupBy)
        }
    }

    fun setSortBy(sortBy: CardsSortBy) {
        viewModelScope.launch {
            userPreferencesRepository.setCardsSortBy(sortBy)
        }
    }

    fun setSortAscending(ascending: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setCardsSortAscending(ascending)
        }
    }

    @OptIn(FlowPreview::class)
    private fun getCardsFlow() : Flow<List<CardPrototypeWithPrintsAndCount>> {
        return filterParams
            .debounce(SEARCH_DEBOUNCE_MILLIS)
            .distinctUntilChanged()
            .map {
                cardPrototypeRepository
                    .filterCardPrototypeWithPrintsAndOwnedCount(it, FilterMode.STRICT)
                    .map { list ->
                        var iter = (it.countGreaterThan?.let { bound ->
                            list.filter { it.count >= bound }
                        } ?: list)

                        iter = (it.countLowerThan?.let { bound ->
                            iter.filter { it.count <= bound }
                        } ?: iter)

                        iter = if (it.multicolor) {
                            iter.filter { card -> Civilization.fromCombo(card.prototype.civilization).size >= 2 }
                        } else {
                            iter
                        }

                        iter
                    }
                    .first()
            }
    }

    companion object {
        // The search box has no client-side debounce of its own; this keeps
        // semantic/fuzzy scoring (heavier than the old plain LIKE query) from
        // running on every keystroke.
        private const val SEARCH_DEBOUNCE_MILLIS = 250L
    }

}