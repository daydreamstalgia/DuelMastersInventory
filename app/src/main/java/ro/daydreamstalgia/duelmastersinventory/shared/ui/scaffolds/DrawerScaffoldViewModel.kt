package ro.daydreamstalgia.duelmastersinventory.shared.ui.scaffolds

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.repository.CardPrototypeRepository
import ro.daydreamstalgia.duelmastersinventory.shared.data.statistics.repository.StatisticsRepository
import ro.daydreamstalgia.duelmastersinventory.shared.data.transactions.repository.TransactedCardRepository
import ro.daydreamstalgia.duelmastersinventory.shared.utils.types.BaseViewModel
import javax.inject.Inject

/** Real numbers backing the drawer panel's CARDS / UNIQUE / NET EUR stat chips. */
data class DrawerStats(
    val ownedCardsCount: Int = 0,
    val uniquePrototypeCount: Int = 0,
    val netEuro: Double = 0.0,
)

@HiltViewModel
class DrawerScaffoldViewModel @Inject constructor(
    transactedCardRepository: TransactedCardRepository,
    cardPrototypeRepository: CardPrototypeRepository,
    statisticsRepository: StatisticsRepository,
) : BaseViewModel() {

    val stats = combine(
        transactedCardRepository.getOwnedCardsCount(),
        cardPrototypeRepository.getOwnedUniquePrototypeCount(),
        statisticsRepository.getTotalCost(),
    ) { ownedCardsCount, uniquePrototypeCount, totalCost ->
        DrawerStats(
            ownedCardsCount = ownedCardsCount,
            uniquePrototypeCount = uniquePrototypeCount,
            netEuro = totalCost.cost,
        )
    }.stateInViewModelScope(DrawerStats())
}
