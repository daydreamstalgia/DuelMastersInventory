package ro.ddnostalgia.duelmastersinventory.shared.ui.components.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.intellij.lang.annotations.JdkConstants.HorizontalAlignment
import ro.ddnostalgia.duelmastersinventory.shared.data.cards.model.CardPrint
import ro.ddnostalgia.duelmastersinventory.shared.ui.components.utils.LazyImage

@Composable
fun CardPrintImage(
    cardPrint: CardPrint,
    modifier: Modifier = Modifier,
    lowRes: Boolean = true,
    customInfo : (@Composable () -> Unit)? = null,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomEnd
    ) {
        val path = if (lowRes) "card_images_low" else "card_images"

        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val boxRatio = maxWidth / maxHeight
            val imageRatio = 0.7f

            var imageModifier = Modifier
                .align(Alignment.Center)
                .clip(RoundedCornerShape(percent = 5))

            imageModifier = if (imageRatio > boxRatio) {
                imageModifier.fillMaxWidth()
            } else {
                imageModifier.fillMaxHeight()
            }

            LazyImage(
                filename = "$path/${cardPrint.id}.jpg",
                modifier = imageModifier
            )
        }

        customInfo?.let {info->
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .background(Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                info()
            }
        }
    }
}