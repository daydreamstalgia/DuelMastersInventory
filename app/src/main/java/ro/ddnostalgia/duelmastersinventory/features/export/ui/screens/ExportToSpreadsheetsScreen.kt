package ro.ddnostalgia.duelmastersinventory.features.export.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import ro.ddnostalgia.duelmastersinventory.nav.Routes
import ro.ddnostalgia.duelmastersinventory.shared.data.export_sheet_configs.model.ExportSheetConfig
import ro.ddnostalgia.duelmastersinventory.shared.service.OperationService
import ro.ddnostalgia.duelmastersinventory.shared.ui.components.core.HyperlinkText
import ro.ddnostalgia.duelmastersinventory.shared.ui.components.list.LazyList
import ro.ddnostalgia.duelmastersinventory.shared.ui.google.GoogleAuthInfo
import ro.ddnostalgia.duelmastersinventory.shared.ui.google.rememberGoogleAuthContext
import ro.ddnostalgia.duelmastersinventory.shared.ui.scaffolds.FloatingAddButtonScaffold

@Composable
fun ExportToSpreadsheetsScreen(
    navController: NavController,
    viewModel: ExportToSpreadsheetsScreenViewModel = hiltViewModel()
) {
    val email by viewModel.email.collectAsState()

    val spreadsheetConfigs by viewModel.spreadsheetConfigs.collectAsState()

    val coroutineScope = rememberCoroutineScope()

    FloatingAddButtonScaffold(
        label = "New spreadsheet",
        onClick = {
            if(email!=null) {
                navController.navigate(Routes.SpreadsheetConfigCreate.createRoute(email!!))
            }
        },
    ) {
        val auth = rememberGoogleAuthContext(onSignedIn = { signInEmail, _ ->
            viewModel.setEmail(signInEmail)
        })

        val context = LocalContext.current
        val sheetsService = auth.sheetsService
        var syncingConfig by remember { mutableStateOf<ExportSheetConfig?>(null) }

        LaunchedEffect(sheetsService, syncingConfig) {
            if(sheetsService!=null && syncingConfig!=null) {
                OperationService.runInNotifications(context) {
                    viewModel.exportToSpreadsheets(sheetsService, syncingConfig!!)
                    syncingConfig = null
                }
            }
        }

        Column(Modifier.padding(horizontal = 12.dp)) {
            GoogleAuthInfo(
                auth,
                mode = "row",
                autoSignIn = true,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
            )

            LazyList(
                spreadsheetConfigs,
                key = { it.id },
                emptyTemplate = {
                    Box(it) {
                        Text(
                            "No spreadsheets",
                            modifier = Modifier
                                .align(Alignment.Center)
                        )
                    }
                }
            ) { item, modifier ->
                Column(
                    modifier = modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(13.dp))
                        .padding(horizontal = 14.dp, vertical = 13.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            HyperlinkText(
                                url = "https://docs.google.com/spreadsheets/d/${item.spreadsheetId}",
                                text = item.title,
                            )
                            if((item.description).isNotBlank()) {
                                Text(
                                    item.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 3.dp),
                                )
                            }
                        }
                        Text(
                            "#${item.id}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 11.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SyncButton(
                            syncing = syncingConfig != null,
                            onClick = {
                                coroutineScope.launch {
                                    if(syncingConfig==null) {
                                        syncingConfig = item
                                    }
                                }
                            },
                        )

                        RoundIconButton(
                            icon = Icons.Filled.Info,
                            contentDescription = "Info",
                            modifier = Modifier.padding(start = 7.dp),
                            onClick = {
                                navController.navigate(Routes.SpreadsheetConfigView.createRoute(item.id))
                            },
                        )

                        RoundIconButton(
                            icon = Icons.Filled.Share,
                            contentDescription = "Share",
                            modifier = Modifier.padding(start = 7.dp),
                            onClick = {
                                val url = "https://docs.google.com/spreadsheets/d/${item.spreadsheetId}"
                                val shareIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, url)
                                    type = "text/plain"
                                }
                                context.startActivity(
                                    Intent.createChooser(shareIntent, "Share via")
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

/** Small pill sync button (spec `.pill-btn.small`), disabled while any card's sync is in flight. */
@Composable
private fun SyncButton(syncing: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(15.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = if (syncing) 0.07f else 0.16f))
            .clickable(enabled = !syncing, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Icon(
            Icons.Filled.Refresh,
            contentDescription = "Sync",
            tint = MaterialTheme.colorScheme.primary.copy(alpha = if (syncing) 0.5f else 1f),
            modifier = Modifier.size(14.dp),
        )
        Text(
            "Sync",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary.copy(alpha = if (syncing) 0.5f else 1f),
            modifier = Modifier.padding(start = 5.dp),
        )
    }
}

/** Circular icon-only action button (spec `.icon-btn-circle`-adjacent row actions). */
@Composable
private fun RoundIconButton(
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable(onClick = onClick),
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(15.dp),
        )
    }
}
