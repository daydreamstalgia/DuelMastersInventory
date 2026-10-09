package ro.ddnostalgia.duelmastersinventory.shared.ui.components.core

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Shared wrapper around Material3's [ModalBottomSheet] so every picker/filter
 * sheet in the app shares the same shape/scrim/drag-handle. This is the
 * redesign's replacement for the raw `Dialog(...)` pickers the app used
 * before (actor/transaction selectors, actor merge, card filter).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    content: @Composable () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        // Real nav-bar inset instead of a fixed guess — some devices (3-button, gesture pill,
        // OEM nav bars) reserve more/less space than a hardcoded padding accounts for.
        contentWindowInsets = { WindowInsets.navigationBars },
    ) {
        Column(
            modifier = modifier.padding(horizontal = 18.dp).padding(bottom = 8.dp)
        ) {
            content()
        }
    }
}
