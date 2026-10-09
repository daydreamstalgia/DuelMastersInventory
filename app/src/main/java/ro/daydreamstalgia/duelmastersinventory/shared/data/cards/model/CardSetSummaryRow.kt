package ro.daydreamstalgia.duelmastersinventory.shared.data.cards.model

/** Raw per-(language, set) aggregate from [ro.daydreamstalgia.duelmastersinventory.shared.data.cards.dao.CardPrintDao.getSetSummaries]. */
data class CardSetSummaryRow(
    val language: String,
    val setCode: String,
    val totalPrints: Int,
    val ownedCount: Int,
    val transactedCount: Int,
)
