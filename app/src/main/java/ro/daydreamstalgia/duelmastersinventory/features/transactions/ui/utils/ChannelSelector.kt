package ro.daydreamstalgia.duelmastersinventory.features.transactions.ui.utils

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.hilt.navigation.compose.hiltViewModel
import ro.daydreamstalgia.duelmastersinventory.shared.ui.components.core.ChannelIcon
import ro.daydreamstalgia.duelmastersinventory.shared.utils.types.ChannelIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelSelector(
    value: String?,
    onValueChanged: (String?) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    viewModel: ChannelSelectorViewModel = hiltViewModel()
) {
    // Opened on focus (before any typing) rather than as a side effect of
    // onValueChange, so an expand-transition never coincides with a
    // keystroke.
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.loadExistingChannels()
    }

    val existingChannels by viewModel.existingChannels.collectAsState()
    val suggestions = remember(existingChannels) {
        (ChannelIcons.knownChannels + existingChannels).distinct().sorted()
    }

    val text = value ?: ""
    val filtered = remember(text, suggestions) {
        if (text.isBlank()) suggestions
        else suggestions.filter { it.contains(text, ignoreCase = true) }
    }

    ExposedDropdownMenuBox(
        expanded = expanded && filtered.isNotEmpty(),
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        TextField(
            value = text,
            onValueChange = { onValueChanged(it.ifBlank { null }) },
            label = label?.let { { Text(it) } },
            leadingIcon = ChannelIcons.iconFor(value)?.let { { ChannelIcon(value) } },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            // The deprecated no-arg menuAnchor() defaults to
            // MenuAnchorType.PrimaryNotEditable, which locks the field
            // against text input while the menu is showing - that was the
            // actual cause of keystrokes (including delete) going nowhere
            // once the dropdown opened, not a focus race.
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryEditable)
                .fillMaxWidth()
                .onFocusChanged { expanded = it.isFocused }
        )

        ExposedDropdownMenu(
            expanded = expanded && filtered.isNotEmpty(),
            onDismissRequest = { expanded = false }
        ) {
            filtered.forEach { suggestion ->
                DropdownMenuItem(
                    text = { Text(suggestion) },
                    leadingIcon = ChannelIcons.iconFor(suggestion)?.let { { ChannelIcon(suggestion) } },
                    onClick = {
                        onValueChanged(suggestion)
                        expanded = false
                    }
                )
            }
        }
    }
}
