package ro.ddnostalgia.duelmastersinventory.shared.data.cards.setimport

/** Mirrors a set pack's `data.json` (see "dynamic sets" spec). Field names match the JSON keys exactly - parsed with Gson's default field-name mapping, no `@SerializedName` needed. */
data class SetPackData(
    val set_language: String,
    val set_code: String,
    val set_name: String,
    val cards: List<SetPackCard>,
)

data class SetPackCard(
    val set_number: String,
    val set_count: String,
    val rarity: String?,
    val name: String?,
    val civilization: String?,
    val races: String?,
    val mana: Int?,
    val power: String?,
    val type: String?,
    val text: String?,
    val translated_name: String?,
    val translated_text: String?,
)

/** The Discover feed's response shape (a GET against a CDN URL). `target` must equal "DMInventory" - anything else is rejected as not being a DMInventory feed. */
data class DiscoverCatalog(
    val target: String?,
    val content: List<DiscoverSetEntry>?,
)

data class DiscoverSetEntry(
    val set_language: String,
    val set_code: String,
    val set_name: String,
    val url: String,
)
