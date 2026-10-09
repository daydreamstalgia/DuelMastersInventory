package ro.ddnostalgia.duelmastersinventory.features.transactions.ui.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import ro.ddnostalgia.duelmastersinventory.shared.data.actors.model.ActorWithAliases
import ro.ddnostalgia.duelmastersinventory.shared.data.actors.utils.displayName
import ro.ddnostalgia.duelmastersinventory.shared.ui.components.core.AmountText
import ro.ddnostalgia.duelmastersinventory.shared.ui.components.core.AppBottomSheet
import ro.ddnostalgia.duelmastersinventory.shared.ui.components.core.ChannelIcon

private data class ActorSelection(
    val actorId: Int,
    val channel: String?,
    val label: String
)

private fun ActorWithAliases.toSelections(): List<ActorSelection> {
    val name = listOfNotNull(actor.firstName, actor.lastName).joinToString(" ").trim()
    if (name.isNotEmpty()) {
        return listOf(ActorSelection(actor.id, null, name))
    }

    if (aliases.isNotEmpty()) {
        return aliases.map {
            ActorSelection(actor.id, it.platform, "${it.platform}/${it.username}")
        }
    }

    return listOf(ActorSelection(actor.id, null, "(unnamed actor)"))
}

/** Initials for the row avatar, same derivation as ActorViewScreen's hero avatar. */
private fun initialsOf(text: String): String =
    text.split(" ", "/").filter { it.isNotBlank() }
        .take(2).joinToString("") { it.first().uppercase() }
        .ifBlank { "?" }

/** "Who?" picker (spec `3c`) — search, pick an actor, or create one inline without leaving the sheet. */
@Composable
fun ActorSelector(
    value: Int?,
    onValueChanged: (actorId: Int?, channel: String?) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    channel: String? = null,
    viewModel: ActorSelectorViewModel = hiltViewModel()
) {
    var showSheet by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(value) {
        viewModel.loadSelectedActor(value)
    }

    val selectedActor by viewModel.selectedActor.collectAsState()

    Box(modifier = modifier) {
        TextField(
            value = selectedActor.displayName(channel),
            onValueChange = {},
            readOnly = true,
            label = label?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { showSheet = true }
        )
    }

    if (showSheet) {
        val search by viewModel.search.collectAsState()

        LaunchedEffect(search) {
            viewModel.loadActors()
        }

        val actors by viewModel.actors.collectAsState()
        val dealCounts by viewModel.dealCounts.collectAsState()
        val selections = remember(actors) { actors.flatMap { it.toSelections() } }

        AppBottomSheet(onDismissRequest = { showSheet = false }) {
            Text("Who?", style = MaterialTheme.typography.titleLarge)
            Text(
                "Pick an actor, or create one without leaving this sheet",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp, bottom = 14.dp),
            )

            TextField(
                value = search ?: "",
                onValueChange = { viewModel.setSearch(it.ifBlank { null }) },
                label = { Text("Search") },
                singleLine = true,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                items(items = selections, key = { "${it.actorId}/${it.label}" }) {
                    ActorSelectionRow(
                        selection = it,
                        selected = it.actorId == value,
                        dealsCount = dealCounts[it.actorId] ?: 0,
                        onClick = {
                            onValueChanged(it.actorId, it.channel)
                            showSheet = false
                        },
                    )
                }

                if (!search.isNullOrBlank()) {
                    item {
                        val name = search!!
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 1.dp, bottom = 7.dp)
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.34f),
                                    RoundedCornerShape(12.dp),
                                )
                                .clickable {
                                    coroutineScope.launch {
                                        val newActorId = viewModel.createActor(name)
                                        onValueChanged(newActorId, null)
                                        showSheet = false
                                    }
                                }
                                .padding(horizontal = 13.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text("+", color = MaterialTheme.colorScheme.primary)
                            Text(
                                "Create actor \"$name\"",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }

            Text(
                "Clear selection",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clickable {
                        onValueChanged(null, null)
                        showSheet = false
                    },
            )
        }
    }
}

/** One actor-row card in the "Who?" sheet (spec `3c`): avatar, name, alias+channel icon, deal count. */
@Composable
private fun ActorSelectionRow(
    selection: ActorSelection,
    selected: Boolean,
    dealsCount: Int,
    onClick: () -> Unit,
) {
    // A selection built from an alias carries "platform/username" as its label; split it back
    // apart so the username reads as the row's name and the platform icon leads the alias line.
    val name = if (selection.channel != null) selection.label.substringAfter("/") else selection.label

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 7.dp)
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.05f),
                RoundedCornerShape(12.dp),
            )
            .border(
                1.dp,
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f) else Color.Transparent,
                RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(initialsOf(name), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary)
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (selection.channel != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    ChannelIcon(selection.channel)
                    Text(
                        name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        if (dealsCount > 0) {
            Column(horizontalAlignment = Alignment.End) {
                AmountText(text = "$dealsCount", fontSize = 13.sp)
                Text(
                    "deals",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}
