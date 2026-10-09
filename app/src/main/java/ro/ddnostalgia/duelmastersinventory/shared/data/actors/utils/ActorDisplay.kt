package ro.ddnostalgia.duelmastersinventory.shared.data.actors.utils

import ro.ddnostalgia.duelmastersinventory.shared.data.actors.model.ActorWithAliases

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
