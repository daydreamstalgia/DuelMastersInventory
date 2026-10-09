package ro.daydreamstalgia.duelmastersinventory.features.actors.ui.screens

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import ro.daydreamstalgia.duelmastersinventory.shared.data.actors.model.ActorWithAliases
import ro.daydreamstalgia.duelmastersinventory.shared.data.actors.model.ActorsSortBy
import ro.daydreamstalgia.duelmastersinventory.shared.data.actors.repository.ActorRepository
import ro.daydreamstalgia.duelmastersinventory.shared.data.actors.utils.displayName
import ro.daydreamstalgia.duelmastersinventory.shared.data.transactions.dao.ActorDealsSummary
import ro.daydreamstalgia.duelmastersinventory.shared.data.transactions.repository.TransactionRepository
import ro.daydreamstalgia.duelmastersinventory.shared.utils.types.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class ActorsScreenViewModel @Inject constructor(
    private val actorRepository: ActorRepository,
    transactionRepository: TransactionRepository,
) : BaseViewModel() {

    private val _search = MutableStateFlow<String?>(null)
    val search = _search.asStateFlow()

    private val _sortBy = MutableStateFlow(ActorsSortBy.NAME)
    val sortBy = _sortBy.asStateFlow()

    // NAME defaults ascending (A-Z); the numeric sorts default descending (highest first) -
    // matches this screen's pre-existing default ordering before the ascending/descending
    // toggle was added. Reset to that per-field default whenever the sort field itself
    // changes, same as clicking a different column header in a table.
    private val _sortAscending = MutableStateFlow(defaultAscendingFor(ActorsSortBy.NAME))
    val sortAscending = _sortAscending.asStateFlow()

    private val _platformFilter = MutableStateFlow<String?>(null)
    val platformFilter = _platformFilter.asStateFlow()

    val availablePlatforms = actorRepository
        .getDistinctAliasPlatforms()
        .stateInViewModelScope()

    // Same imperative-search convention as CardsListScreenViewModel/TransactionsScreenViewModel:
    // re-run the DAO filter (LOWER(...) LIKE query, not a live-observed join) whenever search changes.
    val actors = _search
        .map { actorRepository.filterWithAliases(it).first() }
        .stateInViewModelScope()

    // Per-actor deal count + net EUR for the list row trailing text (spec `actors-list`).
    // Reuses a single grouped query rather than one aggregate per row.
    val dealsSummaryByActorId = transactionRepository
        .getDealsSummaryByActor()
        .map { list -> list.associateBy { it.actorId } }
        .stateInViewModelScope(mapOf<Int, ActorDealsSummary>())

    // Platform filter and sort applied client-side over the already-loaded, already-searched
    // list, same pattern as CardsListScreenViewModel's client-side count filters.
    val displayedActors = combine(
        actors, dealsSummaryByActorId, _sortBy, _sortAscending, _platformFilter
    ) { actorList, summaries, sort, ascending, platform ->
        val filtered = if (platform == null) {
            actorList
        } else {
            actorList.filter { it.aliases.any { alias -> alias.platform.equals(platform, ignoreCase = true) } }
        }

        val comparator = when (sort) {
            ActorsSortBy.NAME -> compareBy<ActorWithAliases> { it.displayName().lowercase() }
            ActorsSortBy.DEALS_COUNT -> compareBy { summaries[it.actor.id]?.dealsCount ?: 0 }
            ActorsSortBy.NET_EUR -> compareBy { summaries[it.actor.id]?.netCost ?: 0.0 }
        }

        filtered.sortedWith(if (ascending) comparator else comparator.reversed())
    }.stateInViewModelScope(listOf<ActorWithAliases>())

    fun setSearch(value: String?) {
        _search.value = value
    }

    fun setSortBy(value: ActorsSortBy) {
        _sortBy.value = value
        _sortAscending.value = defaultAscendingFor(value)
    }

    fun setSortAscending(value: Boolean) {
        _sortAscending.value = value
    }

    fun setPlatformFilter(value: String?) {
        _platformFilter.value = value
    }

    companion object {
        private fun defaultAscendingFor(sortBy: ActorsSortBy) = when (sortBy) {
            ActorsSortBy.NAME -> true
            ActorsSortBy.DEALS_COUNT, ActorsSortBy.NET_EUR -> false
        }
    }
}
