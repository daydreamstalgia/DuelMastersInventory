package ro.daydreamstalgia.duelmastersinventory.features.cards.ui.screens

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import ro.daydreamstalgia.duelmastersinventory.nav.Routes
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.repository.CardPrototypeRepository
import ro.daydreamstalgia.duelmastersinventory.shared.data.transactions.repository.TransactedCardRepository
import ro.daydreamstalgia.duelmastersinventory.shared.utils.types.RouteViewModel
import javax.inject.Inject

@HiltViewModel
class CardViewScreenViewModel @Inject constructor (
    cardPrototypeRepository: CardPrototypeRepository,
    transactedCardRepository: TransactedCardRepository,
    savedStateHandle: SavedStateHandle
): RouteViewModel<Routes.CardView>(Routes.CardView, savedStateHandle) {
    private val cardId: Int = routeArgInt(route.cardIdArg)
    private val printId: Int = routeArgInt(route.printIdArg)

    val card = cardPrototypeRepository
        .getCardPrototypesWithPrintsById(cardId)
        .stateInViewModelScope()

    val ownedCards = cardPrototypeRepository
        .getPrintsAndOwnedCountByPrototypeId(cardId)
        .stateInViewModelScope()

    val ownedCopies = transactedCardRepository
        .getOwnedCopiesWithTransactionByPrototypeId(cardId)
        .stateInViewModelScope()

    val queryPrint = card
        .map {
            it?.prints?.firstOrNull { print -> print.id == printId }
        }
        .stateInViewModelScope()

    val abilityKeywords = cardPrototypeRepository
        .getAbilityKeywordsForPrototype(cardId)
        .stateInViewModelScope()

}