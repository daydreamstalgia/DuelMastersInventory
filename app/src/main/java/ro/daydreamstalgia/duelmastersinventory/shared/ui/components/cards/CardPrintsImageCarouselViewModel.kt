package ro.daydreamstalgia.duelmastersinventory.shared.ui.components.cards

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.model.CardPrintUserData
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.repository.CardPrintRepository
import ro.daydreamstalgia.duelmastersinventory.shared.utils.types.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class CardPrintsImageCarouselViewModel @Inject constructor(
    private val cardPrintRepository: CardPrintRepository
): BaseViewModel() {

    private val _cardPrintIds = MutableStateFlow(listOf<Int>())
    private val _cachedUserData = MutableStateFlow(mapOf<Int, CardPrintUserData>())

    val userData = _cardPrintIds.map {
        cardPrintRepository
            .getUserDataByIds(it)
            .map { it.associateBy { it.cardPrintId } }
            .first()
    }.combine(_cachedUserData) { userData, cachedUserData ->
        userData + cachedUserData
    }.stateInViewModelScope()


    fun setCardPrintIds(ids: List<Int>) {
        _cachedUserData.update { mapOf() }
        _cardPrintIds.update { ids }
    }

    suspend fun updateUserData(userData: CardPrintUserData) {
        withContext(Dispatchers.IO) {
            val newUserData = cardPrintRepository.setUserData(userData)
            _cachedUserData.update { it + (newUserData.cardPrintId to newUserData) }
        }
    }

}