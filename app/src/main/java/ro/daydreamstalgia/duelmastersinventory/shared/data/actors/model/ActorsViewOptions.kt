package ro.daydreamstalgia.duelmastersinventory.shared.data.actors.model

/** How the actors list is ordered. Deals/net are client-side sorts over the already-loaded
 *  per-actor summary, same convention as CardsSortBy's client-side count filters. */
enum class ActorsSortBy { NAME, DEALS_COUNT, NET_EUR }
