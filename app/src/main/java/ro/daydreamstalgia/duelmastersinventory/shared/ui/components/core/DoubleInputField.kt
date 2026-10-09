package ro.daydreamstalgia.duelmastersinventory.shared.ui.components.core

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun DoubleInputField(
    value: Double?,
    onValueChange: (Double?) -> Unit,
    label: String = "",
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: String = ""
) {
    var text by remember(value) { mutableStateOf(value?.toString().orEmpty()) }

    TextField(
        value = text,
        onValueChange = { newText ->
            text = newText
            val parsed = newText.toDoubleOrNull()
            onValueChange(parsed)
        },
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        modifier = modifier,
        placeholder = { if (placeholder.isNotEmpty()) Text(placeholder) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}
