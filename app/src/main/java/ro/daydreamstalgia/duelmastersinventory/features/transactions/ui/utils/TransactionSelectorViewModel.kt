package ro.daydreamstalgia.duelmastersinventory.features.transactions.ui.utils


import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import ro.daydreamstalgia.duelmastersinventory.shared.data.actors.model.ActorWithAliases
import ro.daydreamstalgia.duelmastersinventory.shared.data.actors.repository.ActorRepository
import ro.daydreamstalgia.duelmastersinventory.shared.data.actors.utils.displayName
import ro.daydreamstalgia.duelmastersinventory.shared.data.transactions.model.Transaction
import ro.daydreamstalgia.duelmastersinventory.shared.data.transactions.model.TransactionFilterParams
import ro.daydreamstalgia.duelmastersinventory.shared.data.transactions.repository.TransactionRepository
import ro.daydreamstalgia.duelmastersinventory.shared.utils.types.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class TransactionSelectorViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val actorRepository: ActorRepository,
): BaseViewModel() {

    private val _transactions = MutableStateFlow(listOf<Transaction>())
    private val _selectedTransaction = MutableStateFlow<Transaction?>(null)
    private val _filterParams = MutableStateFlow(TransactionFilterParams())

    val transactions = _transactions.stateInViewModelScope()
    val filterParams = _filterParams.stateInViewModelScope(TransactionFilterParams())

    val selectedTransaction = _selectedTransaction.stateInViewModelScope()

    val actorsById = actorRepository
        .getAllWithAliases()
        .map { list -> list.associateBy { actor -> actor.actor.id } }
        .stateInViewModelScope(mapOf<Int, ActorWithAliases>())

    fun setFilter(params: TransactionFilterParams) {
        _filterParams.update { params }
    }

    suspend fun loadTransactions(filter: (Transaction)->Boolean) {
        val values = transactionRepository
            .filter(_filterParams.value)
            .first()
            .filter(filter)
        _transactions.update { values }
    }

    suspend fun loadSelectedTransaction(id: Int?) {
        if(id==null) {
            _selectedTransaction.update { null }
            return
        }

        val value = transactionRepository
            .getById(id)
            .first()
        _selectedTransaction.update { value }
    }

}