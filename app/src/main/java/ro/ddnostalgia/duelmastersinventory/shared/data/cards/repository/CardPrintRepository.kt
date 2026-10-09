package ro.ddnostalgia.duelmastersinventory.shared.data.cards.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import ro.ddnostalgia.duelmastersinventory.shared.data.DuelMastersInventoryDatabase
import ro.ddnostalgia.duelmastersinventory.shared.data.cards.model.CardPrintUserData
import ro.ddnostalgia.duelmastersinventory.shared.data.cards.model.CardPrintWithPrototype
import javax.inject.Inject

class CardPrintRepository @Inject constructor(
    db: DuelMastersInventoryDatabase
) {
    private val dao = db.cardPrintDao()

    fun getAllSets() = dao.getAllSets()
    fun getAllWithPrototype() = dao.getAllWithPrototype()
    fun getUserDataById(cardPrintId: Int) = dao.getUserDataById(cardPrintId)
    fun getUserDataByIds(cardPrintIds: List<Int>) = dao.getUserDataByIds(cardPrintIds)

    suspend fun setUserData(
        userData: CardPrintUserData
    ): CardPrintUserData {
        dao.insertOrUpdateUserData(userData)
        return getUserDataById(userData.cardPrintId).first() ?: CardPrintUserData(
            cardPrintId=userData.cardPrintId,
            wishlist = false
        )
    }
}