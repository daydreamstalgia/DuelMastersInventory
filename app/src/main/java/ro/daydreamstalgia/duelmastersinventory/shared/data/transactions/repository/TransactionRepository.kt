package ro.daydreamstalgia.duelmastersinventory.shared.data.transactions.repository

import ro.daydreamstalgia.duelmastersinventory.shared.data.DuelMastersInventoryDatabase
import ro.daydreamstalgia.duelmastersinventory.shared.data.transactions.model.Transaction
import ro.daydreamstalgia.duelmastersinventory.shared.data.transactions.model.TransactionFilterParams
import javax.inject.Inject

class TransactionRepository  @Inject constructor(
    db: DuelMastersInventoryDatabase
) {
    private val dao = db.transactionDao()

    fun getAll() = dao.getAll()

    fun getById(id:Int) = dao.getById(id)

    fun filter(params: TransactionFilterParams) = dao
        .filter(
            params.search
        )

    fun getDistinctChannels() = dao.getDistinctChannels()

    fun countByActorId(actorId: Int) = dao.countByActorId(actorId)

    fun netCostByActorId(actorId: Int) = dao.netCostByActorId(actorId)

    fun getDealsSummaryByActor() = dao.getDealsSummaryByActor()

    fun getRecentByActorId(actorId: Int, limit: Int = 3) = dao.getRecentByActorId(actorId, limit)

    suspend fun insert(transaction: Transaction) : Int {
        return dao.insert(transaction).toInt()
    }

    suspend fun update(transaction: Transaction) : Int {
        return dao.update(transaction)
    }
}