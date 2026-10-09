package ro.daydreamstalgia.duelmastersinventory.shared.ui.components.core

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Gold when wishlisted, dim white outline otherwise - matches the Codex spec's card-view star
// (styles.css topbar star: #F2C230 filled / rgba(255,255,255,.4) outline).
private val WishlistGold = Color(0xFFF2C230)
private val WishlistDim = Color.White.copy(alpha = 0.4f)

@Composable
fun ShadowedStarIcon(
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: (()->Unit)? = null,
) {
    Box(
        modifier = modifier
            .shadow(3.dp, shape = CircleShape) // shadow
            .size(64.dp), // size of the button
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = { onClick?.invoke() }) {
            Icon(
                if (isSelected) Icons.Filled.Star else Icons.Filled.StarBorder,
                contentDescription = "Wishlist",
                tint = if (isSelected) WishlistGold else WishlistDim,
            )
        }
    }
}