package ro.ddnostalgia.duelmastersinventory.features.transactions.ui.screens

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import ro.ddnostalgia.duelmastersinventory.shared.data.actors.model.ActorWithAliases
import ro.ddnostalgia.duelmastersinventory.shared.data.actors.repository.ActorRepository
import ro.ddnostalgia.duelmastersinventory.shared.data.actors.utils.displayName
import ro.ddnostalgia.duelmastersinventory.shared.data.cards.model.CardPrototypeWithPrints
import ro.ddnostalgia.duelmastersinventory.shared.data.cards.model.FilterMode
import ro.ddnostalgia.duelmastersinventory.shared.data.cards.model.PrototypePrintsFilterParams
import ro.ddnostalgia.duelmastersinventory.shared.data.statistics.model.LabeledCost
import ro.ddnostalgia.duelmastersinventory.shared.data.statistics.repository.StatisticsRepository
import ro.ddnostalgia.duelmastersinventory.shared.data.transactions.model.Transaction
import ro.ddnostalgia.duelmastersinventory.shared.data.transactions.model.TransactionFilterParams
import ro.ddnostalgia.duelmastersinventory.shared.data.transactions.repository.TransactionRepository
import ro.ddnostalgia.duelmastersinventory.shared.utils.types.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class TransactionsScreenViewModel @Inject constructor (
    private val transactionRepository: TransactionRepository,
    private val actorRepository: ActorRepository,
    private val statisticsRepository: StatisticsRepository,
) : BaseViewModel() {

    private val _filterParams = MutableStateFlow(TransactionFilterParams())

    val filterParams = _filterParams.asStateFlow()

    val transactions = getTransactionsFlow()
        .stateInViewModelScope()

    val actorsById = actorRepository
        .getAllWithAliases()
        .map { list -> list.associateBy { actor -> actor.actor.id } }
        .stateInViewModelScope(mapOf<Int, ActorWithAliases>())

    val totalCost = statisticsRepository
        .getTotalCost()
        .stateInViewModelScope(LabeledCost())

    fun setFilter(params: TransactionFilterParams) {
        _filterParams.value = params
    }

    private fun getTransactionsFlow() : Flow<List<Transaction>> {
        return filterParams.map {
            transactionRepository
                .filter(it)
                .first()
        }
    }


}