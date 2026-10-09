package ro.ddnostalgia.duelmastersinventory.shared.data.cards.model

enum class FilterMode { STRICT, RELAXED }

data class PrototypePrintsFilterParams(
    val search: String? = null,
    val rarities: List<String> = emptyList(),
    val sets: List<String> = emptyList(),
    val types: List<String> = emptyList(),
    val languages: List<String> = emptyList(),
    val civilizations: List<String> = emptyList(),
    // Exclusive with civilizations: matches cards with 2+ civilizations, regardless of which.
    val multicolor: Boolean = false,

    val countGreaterThan: Int? = null,
    val countLowerThan: Int? = null,
)