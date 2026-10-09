package ro.daydreamstalgia.duelmastersinventory.features.actors.ui.screens

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import ro.daydreamstalgia.duelmastersinventory.nav.Routes
import ro.daydreamstalgia.duelmastersinventory.shared.data.actors.repository.ActorRepository
import ro.daydreamstalgia.duelmastersinventory.shared.data.transactions.repository.TransactedCardRepository
import ro.daydreamstalgia.duelmastersinventory.shared.data.transactions.repository.TransactionRepository
import ro.daydreamstalgia.duelmastersinventory.shared.utils.types.RouteViewModel
import javax.inject.Inject

@HiltViewModel
class ActorViewScreenViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val actorRepository: ActorRepository,
    transactionRepository: TransactionRepository,
    transactedCardRepository: TransactedCardRepository,
) : RouteViewModel<Routes.ActorView>(Routes.ActorView, savedStateHandle) {
    private val actorId: Int = routeArgInt(route.actorIdArg)

    val actor = actorRepository
        .getWithAliasesById(actorId)
        .stateInViewModelScope()

    val dealsCount = transactionRepository
        .countByActorId(actorId)
        .stateInViewModelScope(0)

    val canDelete = dealsCount
        .map { it == 0 }
        .stateInViewModelScope(false)

    val netCost = transactionRepository
        .netCostByActorId(actorId)
        .stateInViewModelScope(0.0)

    val cardsInCount = transactedCardRepository
        .cardsInCountByActorId(actorId)
        .stateInViewModelScope(0)

    // A few recent deals for the "TRANSACTIONS" summary section (spec `actor-view`).
    val recentTransactions = transactionRepository
        .getRecentByActorId(actorId, RECENT_TRANSACTIONS_LIMIT)
        .stateInViewModelScope()

    suspend fun deleteActor() {
        val current = actorRepository.getById(actorId).first() ?: return
        actorRepository.delete(current)
    }

    suspend fun mergeInto(targetActorId: Int) {
        actorRepository.mergeInto(actorId, targetActorId)
    }

    companion object {
        private const val RECENT_TRANSACTIONS_LIMIT = 3
    }
}
