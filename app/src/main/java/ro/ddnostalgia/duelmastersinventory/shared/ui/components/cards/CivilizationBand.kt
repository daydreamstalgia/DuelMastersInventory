package ro.ddnostalgia.duelmastersinventory.shared.ui.components.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ro.ddnostalgia.duelmastersinventory.shared.utils.types.Civilization

@Composable
fun CivilizationBand(
    civilization: String?,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 5.dp,
) {
    val colors = Civilization.fromCombo(civilization).map { it.color }

    Row(modifier = modifier.fillMaxWidth().height(height)) {
        colors.forEach {
            Box(
                modifier = Modifier.fillMaxSize().weight(1f)
                    .background(it)
            )
        }
    }
}
