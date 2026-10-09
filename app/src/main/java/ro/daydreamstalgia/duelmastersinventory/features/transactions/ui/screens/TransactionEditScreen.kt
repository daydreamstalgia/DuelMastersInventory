package ro.daydreamstalgia.duelmastersinventory.features.transactions.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import ro.daydreamstalgia.duelmastersinventory.features.transactions.ui.utils.ActorSelector
import ro.daydreamstalgia.duelmastersinventory.features.transactions.ui.utils.ChannelSelector
import ro.daydreamstalgia.duelmastersinventory.features.transactions.ui.utils.TransactionSelector
import ro.daydreamstalgia.duelmastersinventory.nav.Routes
import ro.daydreamstalgia.duelmastersinventory.shared.data.transactions.model.TransactionType
import ro.daydreamstalgia.duelmastersinventory.shared.ui.components.core.DatePickerField
import ro.daydreamstalgia.duelmastersinventory.shared.ui.components.core.DoubleInputField
import ro.daydreamstalgia.duelmastersinventory.shared.ui.components.core.EnumDropdown
import ro.daydreamstalgia.duelmastersinventory.shared.ui.components.core.InputField
import ro.daydreamstalgia.duelmastersinventory.shared.ui.components.transactions.OUTBOUND_ON_COLOR
import ro.daydreamstalgia.duelmastersinventory.shared.utils.constants.FeatureFlags

private enum class Step { TYPE, ACTOR, CHANNEL, DATE, COST, DETAILS }

// The actor step only exists with the actors feature on; otherwise the description (DETAILS
// step) is the only place to note who the transaction was with.
private val STEPS = Step.entries.filter { it != Step.ACTOR || FeatureFlags.ACTORS }
private val STEP_COUNT = STEPS.size

@Composable
fun TransactionEditScreen(
    navController: NavController,
    viewModel: TransactionEditScreenViewModel = hiltViewModel()
) {
    val form by viewModel.form.collectAsState()
    val actorDisplayName by viewModel.actorDisplayName.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var step by remember { mutableIntStateOf(0) }

    LaunchedEffect(viewModel) {
        viewModel.loadTransaction()
    }

    // Editing an existing transaction: every step is already filled in, so
    // jump straight to the last step instead of forcing a re-walk through
    // fields that are already set.
    LaunchedEffect(form.id) {
        if (form.id != null) {
            step = STEP_COUNT - 1
        }
    }

    val inputModifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)

    val currentStep = STEPS[step]
    fun isPast(s: Step) = STEPS.indexOf(s) in 0 until step

    fun canContinue(): Boolean = when (currentStep) {
        Step.TYPE -> form.type != null
        Step.ACTOR -> form.actorId != null
        Step.CHANNEL -> true // channel is optional
        Step.DATE -> form.date != null
        Step.COST -> form.costEuro != null
        Step.DETAILS -> true
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Gradient header: title, step counter, progress bar (spec `3c`), matching the
        // gradient-header treatment used across the redesign's other list/detail headers.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.colorScheme.background)
                    )
                )
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (form.id != null) "Edit transaction" else "New transaction",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    "step ${step + 1} of $STEP_COUNT",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                repeat(STEP_COUNT) { i ->
                    val filled = i <= step
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .background(
                                if (filled) MaterialTheme.colorScheme.primary
                                else Color.White.copy(alpha = 0.13f),
                                RoundedCornerShape(2.dp),
                            )
                    ) {}
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            // Summary of completed steps
            val doneFields = buildList {
                if (isPast(Step.TYPE) && form.type != null) add("TYPE" to form.type.toString())
                if (isPast(Step.ACTOR) && form.actorId != null) add("ACTOR" to (actorDisplayName ?: "#${form.actorId}"))
                if (isPast(Step.CHANNEL) && !form.channel.isNullOrBlank()) add("CHANNEL" to form.channel!!)
                if (isPast(Step.DATE) && form.date != null) add("DATE" to form.date.toString())
                if (isPast(Step.COST) && form.costEuro != null) add("COST" to "%.2f".format(form.costEuro))
            }
            if (doneFields.isNotEmpty()) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    doneFields.forEach { (label, value) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 13.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .background(OUTBOUND_ON_COLOR.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("✓", style = MaterialTheme.typography.labelMedium, color = OUTBOUND_ON_COLOR)
                            }
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(74.dp),
                            )
                            Text(
                                value,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.weight(1f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                            )
                        }
                    }
                }
            }

            // Active step: purple-tinted panel around whichever field is currently being filled
            // in (spec `3c`) — the field itself still reuses its existing component as-is.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.09f),
                        RoundedCornerShape(14.dp),
                    )
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.42f),
                        RoundedCornerShape(14.dp),
                    )
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
            when (currentStep) {
                Step.TYPE -> EnumDropdown<TransactionType>(
                    value = form.type,
                    onValueChanged = { viewModel.updateForm(form.copy(type = it)) },
                    label = "Transaction type",
                    modifier = inputModifier
                )

                Step.ACTOR -> ActorSelector(
                    value = form.actorId,
                    onValueChanged = { actorId, channel ->
                        viewModel.updateForm(form.copy(actorId = actorId, channel = channel ?: form.channel))
                    },
                    channel = form.channel,
                    label = "Actor",
                    modifier = inputModifier
                )

                Step.CHANNEL -> ChannelSelector(
                    value = form.channel,
                    onValueChanged = { viewModel.updateForm(form.copy(channel = it)) },
                    label = "Channel",
                    modifier = inputModifier
                )

                Step.DATE -> DatePickerField(
                    date = form.date,
                    onDateChange = { viewModel.updateForm(form.copy(date = it)) },
                    label = "Date",
                    modifier = inputModifier
                )

                Step.COST -> DoubleInputField(
                    value = form.costEuro,
                    onValueChange = { viewModel.updateForm(form.copy(costEuro = it)) },
                    label = "Cost (EUR)",
                    modifier = inputModifier
                )

                Step.DETAILS -> {
                    TextField(
                        value = form.description ?: "",
                        onValueChange = { viewModel.updateForm(form.copy(description = it)) },
                        label = { Text("Description") },
                        modifier = inputModifier
                    )

                    TransactionSelector(
                        value = form.tradeReferenceId,
                        onValueChanged = { viewModel.updateForm(form.copy(tradeReferenceId = it)) },
                        filter = { it.id != form.id },
                        label = "Trade reference",
                        modifier = inputModifier
                    )

                    InputField(
                        value = form.parcelTrackingNumber,
                        onValueChange = { viewModel.updateForm(form.copy(parcelTrackingNumber = it)) },
                        label = "Parcel Tracking Number",
                        modifier = inputModifier
                    )
                }
            }
            } // end active-step panel
        } // end scrollable content column

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (step > 0) {
                Button(
                    onClick = { step -= 1 },
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.08f),
                        contentColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    ),
                    modifier = Modifier.width(96.dp).height(52.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                ) {
                    Text("Back", style = MaterialTheme.typography.labelLarge)
                }
            }

            if (step < STEP_COUNT - 1) {
                Button(
                    onClick = { step += 1 },
                    enabled = canContinue(),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    modifier = Modifier.weight(1f).height(52.dp),
                ) {
                    Text("Continue", style = MaterialTheme.typography.labelLarge)
                }
            } else {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val insertId = viewModel.saveChanges()
                            insertId?.apply {
                                navController.navigate(Routes.TransactionView.createRoute(this)) {
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
                    modifier = Modifier.weight(1f).height(52.dp),
                ) {
                    Text("Save Transaction", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
