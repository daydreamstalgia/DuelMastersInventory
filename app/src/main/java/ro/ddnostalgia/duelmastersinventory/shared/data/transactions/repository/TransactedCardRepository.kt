package ro.ddnostalgia.duelmastersinventory.shared.data.transactions.repository

import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import kotlinx.coroutines.flow.Flow
import ro.ddnostalgia.duelmastersinventory.shared.data.DuelMastersInventoryDatabase
import ro.ddnostalgia.duelmastersinventory.shared.data.transactions.model.TransactedCard
import ro.ddnostalgia.duelmastersinventory.shared.data.transactions.model.TransactedCardWithPrint
import javax.inject.Inject

class TransactedCardRepository @Inject constructor(
    db: DuelMastersInventoryDatabase
) {
    private val dao = db.transactedCardDao()

    fun getCardsByTransactionId(
        transactionId: Int
    ) = dao.getCardsByTransactionId(transactionId)

    fun getCardsByTransactionIdWithCount(
        transactionId: Int
    ) = dao.getCardsByTransactionIdWithCount(transactionId)

    fun getOwnedTransactedCards() = dao.getOwnedTransactedCards()

    fun getOwnedCopiesWithTransactionByPrototypeId(prototypeId: Int) =
        dao.getOwnedCopiesWithTransactionByPrototypeId(prototypeId)

    fun cardsInCountByActorId(actorId: Int) = dao.cardsInCountByActorId(actorId)

    fun getOwnedCardsCount() = dao.getOwnedCardsCount()

    suspend fun insertAll(items: List<TransactedCard>) {
        dao.insertAll(items)
    }

    suspend fun deleteAll(items: List<TransactedCard>) {
        dao.deleteAll(items)
    }

    suspend fun updateOutTransactionIds(items: List<TransactedCard>) {
        dao.updateOutTransactionIds(items)
    }

}