package ro.daydreamstalgia.duelmastersinventory.features.transactions.ui.screens

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import ro.daydreamstalgia.duelmastersinventory.nav.Routes
import ro.daydreamstalgia.duelmastersinventory.shared.data.transactions.repository.TransactionRepository
import ro.daydreamstalgia.duelmastersinventory.shared.utils.types.RouteViewModel
import javax.inject.Inject

@HiltViewModel
class TransactedCardsEditScreenViewModel @Inject constructor(
    transactionRepository: TransactionRepository,
    savedStateHandle: SavedStateHandle
) : RouteViewModel<Routes.TransactedCardsEdit>(
    Routes.TransactedCardsEdit,
    savedStateHandle
) {
    private val transactionId = routeArgInt(route.transactionIdArg)

    val transaction = transactionRepository
        .getById(transactionId)
        .stateInViewModelScope()
}