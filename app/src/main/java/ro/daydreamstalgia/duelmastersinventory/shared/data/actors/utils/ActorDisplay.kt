package ro.daydreamstalgia.duelmastersinventory.shared.data.actors.utils

import ro.daydreamstalgia.duelmastersinventory.shared.data.actors.model.ActorWithAliases
import ro.daydreamstalgia.duelmastersinventory.shared.data.transactions.model.Transaction
import ro.daydreamstalgia.duelmastersinventory.shared.utils.constants.FeatureFlags

fun ActorWithAliases?.displayName(channel: String? = null): String {
    if (this == null) return "(unknown actor)"

    val name = listOfNotNull(actor.firstName, actor.lastName)
        .joinToString(" ")
        .trim()
    if (name.isNotEmpty()) return name

    val channelMatch = channel?.let { ch ->
        aliases.firstOrNull { it.platform.equals(ch, ignoreCase = true) }
    }
    val alias = channelMatch ?: aliases.firstOrNull()

    return alias?.let { "${it.platform}/${it.username}" } ?: "(unnamed actor)"
}

/**
 * Who a transaction was with, as shown in transaction lists/headers. With [FeatureFlags.ACTORS]
 * off there's no actor, so the channel stands in (the free-text description is shown separately).
 */
fun Transaction.counterpartyDisplay(actor: ActorWithAliases?): String =
    if (FeatureFlags.ACTORS) actor.displayName(channel) else channel ?: "No channel"
