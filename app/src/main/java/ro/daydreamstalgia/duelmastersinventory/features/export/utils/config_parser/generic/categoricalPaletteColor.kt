package ro.daydreamstalgia.duelmastersinventory.features.export.utils.config_parser.generic

import androidx.compose.ui.graphics.Color

val colors = listOf(
    Color(0xffdd7e6b),
    Color(0xffea9999),
    Color(0xffFFD580),
    Color(0xffFFFFE0),
    Color(0xff90EE90),
    Color(0xffd1eeee),
)


fun categoricalPaletteColor(map:MutableMap<String, Color>, value:String): Color {
    if(!map.containsKey(value)) {
        map[value] = colors[map.size % colors.size]
    }
    return map[value]!!
}