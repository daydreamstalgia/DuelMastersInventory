package ro.daydreamstalgia.duelmastersinventory.shared.ui.components.core

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import ro.daydreamstalgia.duelmastersinventory.shared.ui.components.transactions.INBOUND_ON_COLOR

/**
 * Shared yes/no confirmation dialog for destructive/irreversible actions (delete, merge, discard
 * unsaved changes). Same visuals as the delete-actor dialog this replaces (ActorViewScreen).
 */
@Composable
fun AppConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissText: String = "Cancel",
    destructive: Boolean = true,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (destructive) INBOUND_ON_COLOR else MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary),
            ) {
                Text(dismissText)
            }
        },
    )
}
