package ro.ddnostalgia.duelmastersinventory.features.settings.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import ro.ddnostalgia.duelmastersinventory.shared.data.preferences.ThemeMode
import ro.ddnostalgia.duelmastersinventory.shared.service.OperationService
import ro.ddnostalgia.duelmastersinventory.shared.ui.google.GoogleAuthInfo
import ro.ddnostalgia.duelmastersinventory.shared.ui.google.rememberGoogleAuthContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    navController: NavController
) {
    val viewModel: SettingsScreenViewModel = hiltViewModel()
    val auth = rememberGoogleAuthContext()
    val context = LocalContext.current

    val themeMode by viewModel.themeMode.collectAsState()
    val dynamicColor by viewModel.dynamicColor.collectAsState()

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")) {
            uri: Uri? ->
            uri?.let {
                viewModel.exportDatabaseToUri(context, it)
            }

    }

    Column(
        modifier= Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        GoogleAuthInfo(
            data = auth,
            mode = "row",
            autoSignIn = false,
            modifier = Modifier.fillMaxWidth()
        )

        SectionLabel("APPEARANCE")
        SettingsCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ThemeOption("Light", themeMode == ThemeMode.LIGHT, Modifier.weight(1f)) { viewModel.setThemeMode(ThemeMode.LIGHT) }
                ThemeOption("Dark", themeMode == ThemeMode.DARK, Modifier.weight(1f)) { viewModel.setThemeMode(ThemeMode.DARK) }
                ThemeOption("System", themeMode == ThemeMode.SYSTEM, Modifier.weight(1f)) { viewModel.setThemeMode(ThemeMode.SYSTEM) }
            }

            HorizontalDivider(
                modifier = Modifier.padding(top = 14.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Use wallpaper colours", style = MaterialTheme.typography.labelLarge)
                    Text(
                        "Material You dynamic palette",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
                Switch(checked = dynamicColor, onCheckedChange = { viewModel.setDynamicColor(it) })
            }
        }

        SectionLabel("WISHLIST")
        SettingsCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Remove owned cards", style = MaterialTheme.typography.labelLarge)
                    Text(
                        "Clears wishlist entries you already own",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
                ActionPill("Clean up") {
                    OperationService.runInNotifications(context) {
                        viewModel.removeOwnedCardsFromWishlist()
                    }
                }
            }
        }

        SectionLabel("CARD CATALOG")
        SettingsCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Reorder print IDs", style = MaterialTheme.typography.labelLarge)
                    Text(
                        "Renumbers card prints into canonical set order (EN, DE, other languages, JP)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
                ActionPill("Reorder") {
                    OperationService.runInNotifications(context) {
                        viewModel.reorderCardPrintIds()
                    }
                }
            }
        }

        SectionLabel("EXPORT")
        SettingsCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Export database", style = MaterialTheme.typography.labelLarge)
                    Text(
                        "Saves a copy of your local database",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
                ActionPill("Export") {
                    // Built fresh on tap, not once when this screen composes - it must reflect
                    // the moment the save dialog actually opens, not whenever Settings was entered.
                    val exportFileName = "dbinventory-" + SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date()) + ".db"
                    createDocumentLauncher.launch(exportFileName)
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 22.dp, bottom = 8.dp),
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(13.dp))
            .padding(14.dp),
        content = content,
    )
}

@Composable
private fun ThemeOption(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier
            .background(
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                RoundedCornerShape(9.dp),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
    )
}

@Composable
private fun ActionPill(label: String, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f), RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}
