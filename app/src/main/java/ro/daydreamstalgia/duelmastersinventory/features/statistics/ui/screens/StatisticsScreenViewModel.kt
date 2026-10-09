package ro.daydreamstalgia.duelmastersinventory.features.statistics.ui.screens

import dagger.hilt.android.lifecycle.HiltViewModel
import ro.daydreamstalgia.duelmastersinventory.shared.data.statistics.model.LabeledCost
import ro.daydreamstalgia.duelmastersinventory.shared.data.statistics.repository.StatisticsRepository
import ro.daydreamstalgia.duelmastersinventory.shared.data.transactions.repository.TransactionRepository
import ro.daydreamstalgia.duelmastersinventory.shared.utils.types.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class StatisticsScreenViewModel @Inject constructor(
    statisticsRepository: StatisticsRepository
): BaseViewModel() {

    val costsByMonth = statisticsRepository
        .getCostByMonth()
        .stateInViewModelScope()

    val totalCost = statisticsRepository
        .getTotalCost()
        .stateInViewModelScope(LabeledCost())

    val monthlyAverageCost = statisticsRepository
        .getMonthlyAverageCost()
        .stateInViewModelScope(LabeledCost())

    val nextBuyMonthSpan = statisticsRepository
        .getNextBuyMonthSpan(300)
        .stateInViewModelScope()

}