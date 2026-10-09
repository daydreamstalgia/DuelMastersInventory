package ro.daydreamstalgia.duelmastersinventory.features.actors.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import ro.daydreamstalgia.duelmastersinventory.nav.Routes
import ro.daydreamstalgia.duelmastersinventory.shared.ui.components.core.InputField
import ro.daydreamstalgia.duelmastersinventory.shared.ui.components.transactions.INBOUND_ON_COLOR

@Composable
fun ActorEditScreen(
    navController: NavController,
    viewModel: ActorEditScreenViewModel = hiltViewModel()
) {
    val form by viewModel.form.collectAsState()

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(viewModel) {
        viewModel.loadActor()
    }

    Column(
        modifier = Modifier
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            InputField(
                value = form.firstName,
                onValueChange = { viewModel.updateForm(form.copy(firstName = it)) },
                label = "First name",
                modifier = Modifier.weight(1f),
            )
            InputField(
                value = form.lastName,
                onValueChange = { viewModel.updateForm(form.copy(lastName = it)) },
                label = "Last name",
                modifier = Modifier.weight(1f),
            )
        }

        Text(
            "Leave both blank and the actor is shown by its first alias — the displayName() fallback.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f), RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("ALIASES", style = MaterialTheme.typography.titleSmall)
            Box(modifier = Modifier.weight(1f))
            Text(
                "platform / username",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        form.aliases.forEachIndexed { index, alias ->
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    InputField(
                        value = alias.platform,
                        onValueChange = { viewModel.updateAlias(index, alias.copy(platform = it ?: "")) },
                        label = "Platform",
                        modifier = Modifier.weight(1f),
                    )
                    InputField(
                        value = alias.username,
                        onValueChange = { viewModel.updateAlias(index, alias.copy(username = it ?: "")) },
                        label = "Username",
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = { viewModel.removeAlias(index) },
                        colors = IconButtonDefaults.iconButtonColors(contentColor = INBOUND_ON_COLOR),
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "Remove alias")
                    }
                }
                InputField(
                    value = alias.url ?: "",
                    onValueChange = { viewModel.updateAlias(index, alias.copy(url = it)) },
                    label = "Link (optional, e.g. Vinted profile)",
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .height(44.dp)
                .dashedBorder(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                .clickable { viewModel.addAlias() },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("+", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "Add alias",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        Button(
            onClick = {
                coroutineScope.launch {
                    val savedId = viewModel.saveChanges()
                    savedId?.apply {
                        navController.navigate(Routes.ActorView.createRoute(this)) {
                            popUpTo(navController.currentDestination?.id ?: return@navigate) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    }
                }
            },
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            modifier = Modifier.fillMaxWidth().height(52.dp).padding(top = 20.dp),
        ) {
            Text("Save actor", style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Dashed rounded-rect outline for the "Add alias" affordance (spec `actor-edit` `.chip.dashed`). */
private fun Modifier.dashedBorder(color: androidx.compose.ui.graphics.Color) = this.drawBehind {
    val strokeWidthPx = 1.dp.toPx()
    val cornerPx = 12.dp.toPx()
    drawRoundRect(
        color = color,
        style = Stroke(
            width = strokeWidthPx,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f),
        ),
        cornerRadius = CornerRadius(cornerPx, cornerPx),
    )
}
