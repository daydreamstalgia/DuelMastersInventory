package ro.daydreamstalgia.duelmastersinventory.shared.ui.components.core

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
inline fun <reified T> EnumDropdown(
    value: T?,
    crossinline onValueChanged: (T?)-> Unit,
    label: String,
    modifier: Modifier = Modifier,
    allowNullValue: Boolean = false
) where T: Enum<T> {
    Dropdown(
        options = enumValues<T>().map { it.name },
        selectedOption = value?.name ?: "",
        allowNullValue = allowNullValue,
        onOptionSelected = { selectedName ->
            selectedName?.let {
                val selectedValue = enumValueOf<T>(it)
                if(selectedValue != value) {
                    onValueChanged(selectedValue)
                }
            }
        },
        label = label,
        modifier = modifier
    )
}