package ro.daydreamstalgia.duelmastersinventory.shared.data.statistics.repository

import ro.daydreamstalgia.duelmastersinventory.shared.data.DuelMastersInventoryDatabase
import javax.inject.Inject


class StatisticsRepository @Inject constructor(
    db: DuelMastersInventoryDatabase
) {
    val dao = db.statisticsDao()

    fun getCostByMonth() = dao.getCostByMonth()
    fun getTotalCost() = dao.getTotalCost()
    fun getMonthlyAverageCost() = dao.getMonthlyAverageCost()

    fun getNextBuyMonthSpan(targetAverage: Int) =
        dao.getNextBuyMonthSpan(targetAverage)

}