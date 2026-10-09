package ro.ddnostalgia.duelmastersinventory.shared.utils.types

import androidx.compose.ui.graphics.Color
import ro.ddnostalgia.duelmastersinventory.R

enum class Civilization(val displayName: String, val color: Color, val iconRes: Int) {
    LIGHT("Light", Color(0xFFF2C230), R.drawable.ic_civ_light),
    FIRE("Fire", Color(0xFFE4483A), R.drawable.ic_civ_fire),
    WATER("Water", Color(0xFF35A7D8), R.drawable.ic_civ_water),
    DARKNESS("Darkness", Color(0xFF8E7FA8), R.drawable.ic_civ_darkness),
    NATURE("Nature", Color(0xFF4FA45B), R.drawable.ic_civ_nature);

    companion object {
        val ALL = entries.toList()

        fun fromName(name: String?): Civilization? =
            entries.find { it.displayName.equals(name?.trim(), ignoreCase = true) }

        /** Cards can have combo civilizations joined by '/', e.g. "Fire/Water". */
        fun fromCombo(civilization: String?): List<Civilization> =
            civilization?.split('/')?.mapNotNull { fromName(it) } ?: emptyList()
    }
}
