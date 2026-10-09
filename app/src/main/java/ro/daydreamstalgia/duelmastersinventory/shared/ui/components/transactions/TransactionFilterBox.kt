package ro.daydreamstalgia.duelmastersinventory.shared.ui.components.transactions

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ro.daydreamstalgia.duelmastersinventory.shared.data.transactions.model.TransactionFilterParams
import ro.daydreamstalgia.duelmastersinventory.shared.utils.constants.FeatureFlags

/**
 * Search-only field for the transactions list (spec `3b`). No filter dialog
 * exists for transactions yet — this only restyles the existing search field
 * to match [ro.daydreamstalgia.duelmastersinventory.shared.ui.components.cards.CardPrototypePrintsFilterBox]'s
 * already-restyled pill search look.
 */
@Composable
fun TransactionFilterBox(
    value: TransactionFilterParams,
    onValueChange: (TransactionFilterParams)->Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth()) {
        TextField(
            value = value.search ?: "",
            onValueChange = { onValueChange(value.copy(search=it)) },
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            singleLine = true,
            placeholder = {
                Text(
                    if (FeatureFlags.ACTORS) "Actor, description, #id…" else "Description, channel, #id…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                )
            },
            textStyle = MaterialTheme.typography.bodyMedium,
            shape = RoundedCornerShape(20.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White.copy(alpha = 0.07f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.07f),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.primary,
            ),
        )
    }
}