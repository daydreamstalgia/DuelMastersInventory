package ro.ddnostalgia.duelmastersinventory.shared.ui.components.utils

import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import ro.ddnostalgia.duelmastersinventory.shared.utils.types.Civilization

@Composable
fun CivilizationIcon(
    civilization: String?,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.inverseSurface
) {
    val iconRes = Civilization.fromName(civilization)?.iconRes

    if (iconRes != null) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = civilization,
            modifier = modifier.size(AssistChipDefaults.IconSize),
            tint = color,
        )
    }
}
