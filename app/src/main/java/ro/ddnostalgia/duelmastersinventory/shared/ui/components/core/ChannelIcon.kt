package ro.ddnostalgia.duelmastersinventory.shared.ui.components.core

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import ro.ddnostalgia.duelmastersinventory.shared.utils.types.ChannelIcons

/**
 * Renders the known-platform icon for [channel] sized like an inline emoji —
 * matching the font size of whatever text style is ambient at the call site
 * — so it looks consistent whether it's next to an actor name or inside a
 * dropdown row. Renders nothing if the channel isn't a known platform.
 */
@Composable
fun ChannelIcon(channel: String?, modifier: Modifier = Modifier) {
    val icon = ChannelIcons.iconFor(channel) ?: return
    val fontSize = LocalTextStyle.current.fontSize.takeIf { it.isSpecified } ?: 16.sp
    val sizeDp = with(LocalDensity.current) { fontSize.toDp() }

    Icon(
        painter = painterResource(icon),
        contentDescription = null,
        tint = Color.Unspecified,
        modifier = modifier.size(sizeDp)
    )
}
