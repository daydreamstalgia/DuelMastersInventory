package ro.daydreamstalgia.duelmastersinventory.shared.data.cards.model

/** How the cards grid groups rows: not at all, or by print language then set. */
enum class CardsGroupBy { NONE, LANGUAGE_SET }

/** How cards are ordered within a group (or within the whole grid when ungrouped). */
enum class CardsSortBy { NONE, RARITY, POWER }
