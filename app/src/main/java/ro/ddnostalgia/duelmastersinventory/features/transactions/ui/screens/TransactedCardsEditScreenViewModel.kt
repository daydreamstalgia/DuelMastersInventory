package ro.ddnostalgia.duelmastersinventory.features.transactions.ui.screens

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import ro.ddnostalgia.duelmastersinventory.nav.Routes
import ro.ddnostalgia.duelmastersinventory.shared.data.transactions.repository.TransactionRepository
import ro.ddnostalgia.duelmastersinventory.shared.utils.types.RouteViewModel
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