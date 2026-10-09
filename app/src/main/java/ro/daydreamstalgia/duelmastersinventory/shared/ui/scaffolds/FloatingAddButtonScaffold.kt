package ro.daydreamstalgia.duelmastersinventory.shared.ui.scaffolds

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * FAB scaffold shared by list screens. When [label] is supplied, renders the
 * Codex spec's pill "+ Label" FAB (styles.css `.fab`); otherwise keeps the
 * plain round FAB used by screens outside this redesign slice.
 */
@Composable
fun FloatingAddButtonScaffold(
    onClick: (()->Unit)?,
    label: String? = null,
    content: @Composable ()->Unit
) {
    Scaffold(
        floatingActionButton = {
            if (label != null) {
                ExtendedFloatingActionButton(
                    onClick = { onClick?.invoke() },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(18.dp),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                        )
                    },
                    text = { Text(label, style = MaterialTheme.typography.labelLarge) },
                )
            } else {
                FloatingActionButton(
                    onClick = { onClick?.invoke() },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add"
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues.let { PaddingValues(0.dp) })) {
            content()
        }
    }
}