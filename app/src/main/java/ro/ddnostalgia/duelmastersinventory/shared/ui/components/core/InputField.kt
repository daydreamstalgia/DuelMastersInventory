package ro.ddnostalgia.duelmastersinventory.shared.ui.components.core

import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun <T> InputField(
    value: T?,
    onValueChange: ((T?)->Unit)? = null,
    label: String? = null,
    readOnly: Boolean = false,
    modifier: Modifier = Modifier
) {
    when(value) {
        is String? -> {
            TextField(
                value = value.orEmpty(),
                onValueChange = { onValueChange?.invoke(it as T?) },
                readOnly = readOnly,
                label = { label?.let{ Text(it) } },
                modifier = modifier
            )
        }

        else -> {
            Text("Unsupported input type ${value!!::class.simpleName}")
        }
    }
}