package ro.daydreamstalgia.duelmastersinventory.shared.ui.components.core

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ro.daydreamstalgia.duelmastersinventory.shared.utils.constants.conditionColor

/** Small colored pill for a card condition (NM/LP/MP/HP/D). */
@Composable
fun ConditionChip(
    condition: String?,
    modifier: Modifier = Modifier,
) {
    val color = conditionColor(condition)
    Text(
        text = condition ?: "?",
        modifier = modifier
            .background(color.copy(alpha = 0.16f), RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        color = color,
        style = MaterialTheme.typography.labelMedium,
    )
}
