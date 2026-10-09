package ro.ddnostalgia.duelmastersinventory.shared.ui.components.core

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import ro.ddnostalgia.duelmastersinventory.shared.utils.types.Fonts

/**
 * Plain (non-outlined) DMNumbers text for mana/power/counts/money figures.
 * See [ro.ddnostalgia.duelmastersinventory.shared.ui.components.cards.NumberText]
 * for the outlined variant used over card art.
 */
@Composable
fun AmountText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 18.sp,
    color: Color = LocalContentColor.current,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        style = TextStyle(
            fontFamily = Fonts.numbersFamily,
            fontSize = fontSize,
        ),
    )
}
