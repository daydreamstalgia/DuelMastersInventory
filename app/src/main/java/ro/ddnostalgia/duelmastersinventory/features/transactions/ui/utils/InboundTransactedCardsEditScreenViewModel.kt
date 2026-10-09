package ro.ddnostalgia.duelmastersinventory.features.transactions.ui.utils

import android.util.Log
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ro.ddnostalgia.duelmastersinventory.shared.data.cards.model.CardPrint
import ro.ddnostalgia.duelmastersinventory.shared.data.cards.model.CardPrototype
import ro.ddnostalgia.duelmastersinventory.shared.data.cards.model.PrototypePrintsFilterParams
import ro.ddnostalgia.duelmastersinventory.shared.data.cards.repository.CardPrintRepository
import ro.ddnostalgia.duelmastersinventory.shared.data.transactions.model.TransactedCard
import ro.ddnostalgia.duelmastersinventory.shared.data.transactions.model.TransactedCardWithPrint
import ro.ddnostalgia.duelmastersinventory.shared.data.transactions.repository.TransactedCardRepository
import ro.ddnostalgia.duelmastersinventory.shared.utils.types.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class InboundTransactedCardsEditScreenViewModel @Inject constructor(
    private val transactedCardRepository: TransactedCardRepository,
    cardPrintRepository: CardPrintRepository
) : BaseViewModel() {

    private val _transactionId = MutableStateFlow(0)

    private val transactedCards = _transactionId
        .map {
            transactedCardRepository
                .getCardsByTransactionId(it)
                .first()
        }

    private val currentCardsFlow = transactedCards
        .map { list ->
            list
                .groupBy {
                    TransactedCardsGroupKey.from(it)
                }
                .map {
                    EditableItem(it.key, it.value)
                }
                .associateBy { it.key.keyId }
                .toSortedMap()
        }
        .stateInViewModelScope(mapOf())

    private val searchCardsFlow = cardPrintRepository
        .getAllWithPrototype()
        .combine(_transactionId) { list, transactionId ->
            list to transactionId
        }
        .map { values ->
            val list = values.first
            val transactionId = values.second
            list.map {
                val transactedCard = TransactedCardWithPrint(
                    card = TransactedCard(
                        cardPrintId = it.print.id,
                        inTransactionId = transactionId,
                    ),
                    printWithPrototype = it,
                )

                EditableItem(
                    key = TransactedCardsGroupKey.from(transactedCard),
                    templateTransactedCard = transactedCard
                )
            }
        }

    private val _userAddedCardsState = MutableStateFlow(mapOf<String, EditableItem>())
    private val _userRemovedCardsState = MutableStateFlow(mapOf<String, EditableItem>())

    private val _cardsSearch = MutableStateFlow("")

    private val cardsFilterParams = _cardsSearch
        .map {
            PrototypePrintsFilterParams(
                search = it
            )
        }
        .stateInViewModelScope(PrototypePrintsFilterParams())


    private val userInputState = combine(_userAddedCardsState, _userRemovedCardsState
    ) { addedCards, removedCards ->
        UserInputState(
            addedCards,
            removedCards
        )
    }


    private val dataState = combine(currentCardsFlow, userInputState) { currentCards, userInput ->
        DataState(
            currentCards = mergeItems(
                currentCards,
                userInput.addedCards,
                userInput.removedCards
            ).filter {
                it.value.transactedCards.size + it.value.addedCards.size > 0
            }.values.toList(),
            addedCards = userInput.addedCards.values.toList(),
            removedCards = userInput.removedCards.values.toList()
        )
    }.stateInViewModelScope(DataState())

    val isDirty = dataState
        .map { it.addedCards.isNotEmpty() || it.removedCards.isNotEmpty() }
        .stateInViewModelScope(false)

    val uiState = combine(dataState, cardsFilterParams, searchCardsFlow) { data, filter, searchCards ->
        UIState(
            cardFilterParams = filter,
            currentCards = data.currentCards.filter { filterCard(filter, it.templateTransactedCard) },
            addedCards = data.addedCards.filter { filterCard(filter, it.templateTransactedCard) },
            removedCards = data.removedCards.filter { filterCard(filter, it.templateTransactedCard) },
            searchCards = searchCards.filter { filterCard(filter, it.templateTransactedCard) },
            currentCardsCount = data.currentCardsCount,
            removedCardsCount = data.removedCardsCount
        )
    }.stateInViewModelScope(UIState())

    private fun filterCard(
        filter: PrototypePrintsFilterParams,
        card: TransactedCardWithPrint?
    ): Boolean {
        val search = filter.search?.lowercase() ?: return true

        if(search=="") {
            return true
        }

        val prototype = card?.cardPrototype ?: return false
        val print = card.print

        return (prototype.name?.lowercase()?.contains(search) ?: false) ||
                (prototype.text?.lowercase()?.contains(search) ?: false) ||
                print.displayId().lowercase().contains(search)
    }

    fun setCardFilter(filter: PrototypePrintsFilterParams) {
        this._cardsSearch.update {  filter.search ?: "" }
    }

    fun setTransactionId(id:Int) {
        _transactionId.value = id
    }

    /** Whether transaction [id] currently has no cards - queried directly rather than derived from [uiState] so the caller can decide the initial tab before the combined flow has settled. */
    suspend fun isEmptyTransaction(id: Int): Boolean =
        transactedCardRepository.getCardsByTransactionId(id).first().isEmpty()

    fun addOneTransactedCardTo(item: EditableItem) {
        viewModelScope.launch {

            var cardToAdd = item.templateTransactedCard
            var removedExists = false

            _userRemovedCardsState.update {
                val card = getRemovedTransactedCard(it, item.key.keyId)

                if (card != null) {
                    cardToAdd = card
                    removedExists = true
                    val map = removeTransactedCardFromAdded(it, item.key.keyId, card)
                    map.filter { it.value.addedCards.isNotEmpty() }
                } else
                    it
            }

            _userAddedCardsState.update {
                if (cardToAdd != null)
                    addTransactedCard(
                        it, cardToAdd!!,
                        to = if (removedExists) "transacted" else "added"
                    )
                else
                    it
            }
        }
    }

    fun restoreTransactedCard(item:EditableItem) {
        viewModelScope.launch {
            _userRemovedCardsState.update {
                val card = getRemovedTransactedCard(it, item.key.keyId)

                if (card != null) {
                    val map = removeTransactedCardFromAdded(it, item.key.keyId, card)
                    map.filter { it.value.addedCards.isNotEmpty() }
                } else
                    it
            }
        }
    }

    fun removeOneTransactedCardFrom(item: EditableItem) {
        viewModelScope.launch {
            if (item.addedCards.isNotEmpty()) {
                _userAddedCardsState.update {
                    removeTransactedCardFromAdded(it, item.key.keyId)
                }
            } else if (item.transactedCards.isNotEmpty()) {
                _userRemovedCardsState.update {
                    val cardToRemove = getNotRemovedTransactedCard(
                        currentCardsFlow.value, it, item.key.keyId
                    )

                    val result = if (cardToRemove != null)
                        addTransactedCard(it, cardToRemove)
                    else
                        it

                    result
                }
            }
        }
    }

    suspend fun saveChanges() {
        val addedCards = dataState.value.addedCards.map {
            it.addedCards
        }.flatten().map {
            it.card
        }

        val removedCards = dataState.value.removedCards.map {
            it.addedCards
        }.flatten().map {
            it.card
        }

        transactedCardRepository.deleteAll(removedCards)
        transactedCardRepository.insertAll(addedCards)

    }

    private fun getRemovedTransactedCard(
        removed: Map<String, EditableItem>,
        keyId: String
    ) : TransactedCardWithPrint? {
        val removedItem = removed[keyId] ?: return null
        return removedItem.addedCards.lastOrNull()
    }

    private fun getNotRemovedTransactedCard(
        current: Map<String, EditableItem>,
        removed: Map<String, EditableItem>,
        keyId: String
    ) : TransactedCardWithPrint? {
        val currentItem = current[keyId] ?: return null

        val removedItem = removed[keyId]

        val currentCards = currentItem.transactedCards

        val availableCards = if (removedItem != null) {
            currentCards - removedItem.addedCards
        } else {
            currentCards
        }

        return availableCards.firstOrNull()
    }

    private fun removeTransactedCardFromAdded(
        items: Map<String, EditableItem>,
        keyId: String,
        card: TransactedCardWithPrint? = null,
    ): Map<String, EditableItem> {
        if(items.containsKey(keyId)) {
            var item = items[keyId]!!
            if(card == null || card.card.id==0) {
                item = item.copy(
                    addedCards = item.addedCards.take(item.addedCards.size - 1)
                )
            }
            else {
                item = item.copy(
                    addedCards = item.addedCards.filter { it.card.id != card.card.id }
                )
            }
            return items + (keyId to item)
        }
        return items
    }

    private fun addTransactedCard(
        items: Map<String, EditableItem>,
        card: TransactedCardWithPrint,
        to: String = "added",
    ) : Map<String, EditableItem> {
        val key = TransactedCardsGroupKey.from(card)
        val keyId = key.keyId

        if(!items.containsKey(keyId)) {
            val item = EditableItem(
                key = key,
                transactedCards = if(to=="transacted") listOf(card) else listOf(),
                addedCards = if(to=="added") listOf(card) else listOf(),
                templateTransactedCard = card.copyWithoutId()
            )

            return items + (keyId to item)
        }
        else {
            var item = items[keyId]!!
            item = item.copy(
                transactedCards = if(to=="transacted") item.transactedCards + card else item.transactedCards,
                addedCards = if(to=="added") item.addedCards + card else item.addedCards,
            )

            return items + (keyId to item)
        }
    }

    private fun mergeItems(
        current: Map<String, EditableItem>,
        added: Map<String, EditableItem>,
        removed: Map<String, EditableItem>
    ): Map<String, EditableItem> {
        val mergedMap = current.toMutableMap()

        // Merge user-added cards
        for ((keyId, addedItem) in added) {
            val existingItem = mergedMap[keyId]
            if (existingItem != null) {
                // Key exists, merge addedCards
                mergedMap[keyId] = existingItem.copy(
                    addedCards = existingItem.addedCards + addedItem.addedCards
                )
            } else {
                // Key doesn't exist, just insert
                mergedMap[keyId] = addedItem
            }
        }

        // Apply removals
        for ((keyId, removedItem) in removed) {
            val existingItem = mergedMap[keyId]
            if (existingItem != null) {
                val remainingTransactedCards = existingItem.transactedCards.filter { currentCard ->
                    removedItem.addedCards.none { it.card.id == currentCard.card.id }
                }

                mergedMap[keyId] = existingItem.copy(
                    transactedCards = remainingTransactedCards
                )
            }
        }

        return mergedMap
    }

    data class TransactedCardsGroupKey(
        val print: CardPrint,
        val prototype: CardPrototype,
        val condition: String?,
        val owned: Boolean
    ) {
        val keyId: String
            get() = "${print.id.toString().padStart(6, '0')}" +
                    "-${condition ?: "NONE"}" +
                    "-$owned"

        companion object {
            fun from(item: TransactedCardWithPrint): TransactedCardsGroupKey {
                return TransactedCardsGroupKey(
                    item.print,
                    item.cardPrototype,
                    item.card.condition,
                    owned = item.card.outTransactionId == null
                )
            }
        }
    }

    data class EditableItem(
        val key: TransactedCardsGroupKey,
        val transactedCards: List<TransactedCardWithPrint> = listOf(),
        val addedCards: List<TransactedCardWithPrint> = listOf(),
        val templateTransactedCard: TransactedCardWithPrint? = transactedCards
            .firstOrNull()
            ?.copyWithoutId(),
    ) {
        companion object {
            fun from(template: TransactedCardWithPrint): EditableItem {
                return EditableItem(
                    TransactedCardsGroupKey.from(template),
                    templateTransactedCard = template
                )
            }
        }
    }

    data class UserInputState(
        val addedCards: Map<String, EditableItem> = mapOf(),
        val removedCards: Map<String, EditableItem> = mapOf(),
    )

    data class DataState(
        val currentCards: List<EditableItem> = listOf(),
        val addedCards: List<EditableItem> = listOf(),
        val removedCards: List<EditableItem> = listOf(),

        val currentCardsCount: Int = currentCards.sumOf {
                it.addedCards.size + it.transactedCards.size
            },

        val removedCardsCount: Int = removedCards.sumOf { it.addedCards.size },
    )

    data class UIState(
        val currentCards: List<EditableItem> = listOf(),
        val addedCards: List<EditableItem> = listOf(),
        val removedCards: List<EditableItem> = listOf(),
        val searchCards: List<EditableItem> = listOf(),

        val cardFilterParams: PrototypePrintsFilterParams = PrototypePrintsFilterParams(),

        val currentCardsCount: Int = currentCards.sumOf {
            it.addedCards.size + it.transactedCards.size
        },

        val removedCardsCount: Int = removedCards.sumOf { it.addedCards.size },
    )

}