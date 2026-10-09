package ro.daydreamstalgia.duelmastersinventory.features.transactions.ui.screens

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import ro.daydreamstalgia.duelmastersinventory.nav.Routes
import ro.daydreamstalgia.duelmastersinventory.shared.data.actors.repository.ActorRepository
import ro.daydreamstalgia.duelmastersinventory.shared.data.actors.utils.counterpartyDisplay
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.model.PrototypePrintsFilterParams
import ro.daydreamstalgia.duelmastersinventory.shared.data.transactions.repository.*
import ro.daydreamstalgia.duelmastersinventory.shared.utils.types.RouteViewModel
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TransactionViewScreenViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    transactionRepository: TransactionRepository,
    transactedCardRepository: TransactedCardRepository,
    actorRepository: ActorRepository,
): RouteViewModel<Routes.TransactionView>(Routes.TransactionView,savedStateHandle) {
    private val transactionId: Int = routeArgInt(route.transactionIdArg)

    private val _cardFilterParams = MutableStateFlow(PrototypePrintsFilterParams())

    val cardFilterParams = _cardFilterParams.asStateFlow()

    val transaction = transactionRepository
        .getById(transactionId)
        .stateInViewModelScope()

    val actorDisplay = transaction
        .flatMapLatest { t ->
            val actorId = t?.actorId
            if (actorId == null) flowOf(null) else actorRepository.getWithAliasesById(actorId)
        }
        .combine(transaction) { actor, t -> t?.counterpartyDisplay(actor) ?: "" }
        .stateInViewModelScope("")

    val records = transactedCardRepository
        .getCardsByTransactionIdWithCount(transactionId)
        .map {
            it.groupBy {
                it.print.id
            }.map {
                val card = it.value.first()
                card.copy(count = it.value.sumOf { it.count })
            }
        }
        .stateInViewModelScope()

    fun setCardFilter(params: PrototypePrintsFilterParams) {
        _cardFilterParams.value = params
    }

}