package ro.ddnostalgia.duelmastersinventory.shared.data.transactions.model

import java.time.LocalDate

/**
 * One owned physical copy of a print, together with the (inbound)
 * transaction it was acquired in. Backs the card-detail "your copies" list,
 * which is read-only and links each copy back to its origin transaction —
 * inventory is never edited directly, only through transactions.
 */
data class OwnedCopyProvenance(
    val transactedCardId: Int,
    val cardPrintId: Int,
    val condition: String?,
    val transactionId: Int,
    val date: LocalDate,
    val channel: String?,
    val actorId: Int,
)
