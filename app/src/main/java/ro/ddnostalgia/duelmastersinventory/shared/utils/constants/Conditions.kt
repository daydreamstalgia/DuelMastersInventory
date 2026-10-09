package ro.ddnostalgia.duelmastersinventory.shared.utils.constants

import androidx.compose.ui.graphics.Color

val Conditions = listOf(
    "NM",
    "LP",
    "MP",
    "HP",
    "D"
)

val ConditionColors = mapOf(
    "NM" to Color(0xFF7FD69B),
    "LP" to Color(0xFFF2C230),
    "MP" to Color(0xFFE8A33D),
    "HP" to Color(0xFFF19087),
    "D" to Color(0xFFB0A8B8),
)

fun conditionColor(condition: String?): Color =
    ConditionColors[condition] ?: Color(0xFFB0A8B8)
