package ro.daydreamstalgia.duelmastersinventory.shared.ui.scaffolds

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackScaffold(
    title: String,
    onBack: () -> Unit,
    actions: (@Composable RowScope.() -> Unit)? = null,
    dropdownActions: List<Pair<String, () -> Unit>>? = null,
    content: @Composable () -> Unit,
) {
    var showActionsMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            // Gradient from a slightly-lighter panel tone down to the theme background, matching
            // the detail-screen header treatment across the Codex spec (styles.css .topbar.gradient).
            Box(
                modifier = Modifier.background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surfaceContainer,
                            MaterialTheme.colorScheme.background,
                        )
                    )
                ),
            ) {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        actions?.invoke(this)
                        if (dropdownActions?.isNotEmpty() == true) {
                            Box {
                                IconButton(onClick = { showActionsMenu = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                                }
                                DropdownMenu(
                                    expanded = showActionsMenu,
                                    onDismissRequest = { showActionsMenu = false },
                                    shape = RoundedCornerShape(16.dp),
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)),
                                ) {
                                    dropdownActions.forEach { (text, action) ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = MaterialTheme.colorScheme.onBackground,
                                                )
                                            },
                                            onClick = {
                                                showActionsMenu = false
                                                action()
                                            },
                                            colors = MenuDefaults.itemColors(
                                                textColor = MaterialTheme.colorScheme.onBackground,
                                            ),
                                            contentPadding = PaddingValues(horizontal = 14.dp),
                                        )
                                    }
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                    ),
                )
            }
        }
    ) { innerPadding ->
        // consumeWindowInsets + imePadding: on Android 15+ the app is drawn edge-to-edge and the
        // window no longer resizes for the keyboard, so content shrinks itself to stay above it
        // (without double-counting the nav-bar inset already in innerPadding). No-op on older versions.
        Box(modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding).imePadding()) {
            content()
        }
    }
}
