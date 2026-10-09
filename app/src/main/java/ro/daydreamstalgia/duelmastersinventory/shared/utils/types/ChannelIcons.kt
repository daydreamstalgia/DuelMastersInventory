package ro.daydreamstalgia.duelmastersinventory.shared.utils.types

import ro.daydreamstalgia.duelmastersinventory.R

object ChannelIcons {
    private val icons = mapOf(
        "ebay" to R.drawable.ic_channel_ebay,
        "vinted" to R.drawable.ic_channel_vinted,
    )

    val knownChannels: List<String> = icons.keys.sorted()

    fun iconFor(channel: String?): Int? = channel?.trim()?.lowercase()?.let { icons[it] }
}
