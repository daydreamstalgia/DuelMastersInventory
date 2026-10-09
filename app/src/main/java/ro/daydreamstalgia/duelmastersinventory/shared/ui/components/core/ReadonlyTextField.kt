package ro.daydreamstalgia.duelmastersinventory.shared.ui.components.core

import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ReadonlyTextField(
    value: String?,
    label: String? = null,
    modifier: Modifier = Modifier
) {
    TextField(
        value = value ?: "",
        onValueChange = {},
        label= label?.let { { Text(label) } },
        readOnly = true,
        modifier = modifier
    )
}