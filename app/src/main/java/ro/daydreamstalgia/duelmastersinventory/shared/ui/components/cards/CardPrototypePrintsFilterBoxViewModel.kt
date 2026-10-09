package ro.daydreamstalgia.duelmastersinventory.shared.ui.components.cards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.repository.CardPrintRepository
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.repository.CardPrototypeRepository
import ro.daydreamstalgia.duelmastersinventory.shared.utils.constants.TIMEOUT_MILLIS
import ro.daydreamstalgia.duelmastersinventory.shared.utils.types.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class CardPrototypePrintsFilterBoxViewModel @Inject constructor(
    cardPrintRepository: CardPrintRepository
): BaseViewModel() {

    val sets = cardPrintRepository
        .getAllSets()
        .stateInViewModelScope()


}