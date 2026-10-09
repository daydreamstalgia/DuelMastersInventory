package ro.ddnostalgia.duelmastersinventory.shared.ui.components.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlin.math.roundToInt
import ro.ddnostalgia.duelmastersinventory.R
import ro.ddnostalgia.duelmastersinventory.shared.data.cards.model.PrototypePrintsFilterParams
import ro.ddnostalgia.duelmastersinventory.shared.ui.components.core.AppBottomSheet
import ro.ddnostalgia.duelmastersinventory.shared.ui.components.utils.CivilizationIcon
import ro.ddnostalgia.duelmastersinventory.shared.ui.components.utils.RarityIcon
import ro.ddnostalgia.duelmastersinventory.shared.utils.types.Civilization

/** Flag emoji shown left of each language filter chip's code. EN is North America's English TCG printing, hence 🇺🇸 not 🇬🇧. */
private val LANGUAGE_FLAGS = mapOf(
    "EN" to "🇺🇸",
    "JP" to "🇯🇵",
    "DE" to "🇩🇪",
    "FR" to "🇫🇷",
    "IT" to "🇮🇹",
    "KO" to "🇰🇷",
    "CN" to "🇨🇳",
    "PT" to "🇵🇹",
    "MX" to "🇲🇽",
)

@Composable
fun CardPrototypePrintsFilterBox(
    value: PrototypePrintsFilterParams,
    onValueChange: (PrototypePrintsFilterParams)->Unit,
    modifier:Modifier = Modifier,
    filter: Boolean = true,
    viewModel: CardPrototypePrintsFilterBoxViewModel = hiltViewModel(),
    userContent: (@Composable RowScope.()->Unit)? = null
) {
    val openFilterDialog = remember { mutableStateOf(false) }

    val sets by viewModel.sets.collectAsState()

    Row(
        modifier = modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextField(
            value = value.search ?: "",
            onValueChange = { onValueChange(value.copy(search=it)) },
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            singleLine = true,
            placeholder = {
                Text(
                    "Search cards",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                )
            },
            textStyle = MaterialTheme.typography.bodyMedium,
            shape = RoundedCornerShape(20.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White.copy(alpha = 0.07f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.07f),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.primary,
            ),
        )

        if(filter) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), CircleShape)
                    .clickable { openFilterDialog.value = true },
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_filter),
                    contentDescription = "Filter",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        userContent?.invoke(this)

        when{
            openFilterDialog.value -> {
                CardPrototypePrintsFilterDialog(
                    onDismissRequest = {
                        openFilterDialog.value = false
                    },
                    options = value,
                    onApplyOptions = {
                        onValueChange(it)
                        openFilterDialog.value = false
                    },
                    sets = sets
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CardPrototypePrintsFilterDialog(
    onDismissRequest: () -> Unit,
    options: PrototypePrintsFilterParams,
    onApplyOptions: (PrototypePrintsFilterParams)->Unit,
    sets: List<String>
) {
    val countGreaterThanState = remember { mutableStateOf(options.countGreaterThan) }
    val countLowerThanState = remember { mutableStateOf(options.countLowerThan) }

    val civLightFlag = remember{ mutableStateOf(options.civilizations.contains("Light"))}
    val civFireFlag = remember{ mutableStateOf(options.civilizations.contains("Fire"))}
    val civWaterFlag = remember{ mutableStateOf(options.civilizations.contains("Water"))}
    val civDarknessFlag = remember{ mutableStateOf(options.civilizations.contains("Darkness"))}
    val civNatureFlag = remember{ mutableStateOf(options.civilizations.contains("Nature"))}
    val civMulticolorFlag = remember{ mutableStateOf(options.multicolor)}

    // Multicolor is exclusive with the single-civilization toggles: picking one clears the other.
    fun onSingleCivToggled() { civMulticolorFlag.value = false }
    fun onMulticolorToggled() {
        civLightFlag.value = false
        civFireFlag.value = false
        civWaterFlag.value = false
        civDarknessFlag.value = false
        civNatureFlag.value = false
    }

    val rarCommonFlag = remember { mutableStateOf(options.rarities.contains("Common")) }
    val rarUncommonFlag = remember { mutableStateOf(options.rarities.contains("Uncommon")) }
    val rarRareFlag = remember { mutableStateOf(options.rarities.contains("Rare")) }
    val rarVeryRareFlag = remember { mutableStateOf(options.rarities.contains("Very Rare")) }
    val rarSuperRareFlag = remember { mutableStateOf(options.rarities.contains("Super Rare")) }

    val typeCreatureFlag = remember { mutableStateOf(options.types.contains("Creature")) }
    val typeSpellFlag = remember { mutableStateOf(options.types.contains("Spell")) }
    val typeEvolutionCreatureFlag = remember { mutableStateOf(options.types.contains("Evolution Creature")) }

    // OCG-only card types (never printed in an EN/DE/FR/IT set to date) - hidden behind the
    // "Show OCG" toggle so the default Type row stays short. See
    // http://duelmasters.fandom.com/wiki/Card_Type for the full type list this is drawn from;
    // Castle/Fortress/Psychic Creature aren't in any imported pack yet but are real OCG types,
    // kept here so their filter exists once a pack with one lands.
    val ocgTypes = listOf(
        "Cross Gear", "Castle", "Fortress", "Psychic Creature",
        "Tamaseed", "Dream Creature", "Neo Creature", "G-Neo Creature", "D2 Field",
    )
    val ocgTypeFlags = remember { mutableStateMapOf<String, MutableState<Boolean>>().apply {
            ocgTypes.forEach { type ->
                this[type] = mutableStateOf(options.types.contains(type))
            }
        }
    }
    val showOcgTypes = remember { mutableStateOf(ocgTypeFlags.values.any { it.value }) }

    val setFlags = remember { mutableStateMapOf<String, MutableState<Boolean>>().apply {
        sets.forEach { set ->
                this[set] = mutableStateOf(options.sets.contains(set))
            }
        }
    }

    val languages = listOf("EN", "JP", "DE", "FR", "IT", "KO", "CN", "PT", "MX")

    val langFlags = remember { mutableStateMapOf<String, MutableState<Boolean>>().apply {
            languages.forEach { lang ->
                this[lang] = mutableStateOf(options.languages.contains(lang))
            }
        }
    }

    fun applyFilters() {
        val civs = listOfNotNull(
            "Light".takeIf { civLightFlag.value },
            "Fire".takeIf { civFireFlag.value },
            "Water".takeIf { civWaterFlag.value },
            "Darkness".takeIf { civDarknessFlag.value },
            "Nature".takeIf { civNatureFlag.value }
        )

        val rars = listOfNotNull(
            "Common".takeIf { rarCommonFlag.value },
            "Uncommon".takeIf { rarUncommonFlag.value },
            "Rare".takeIf { rarRareFlag.value },
            "Very Rare".takeIf { rarVeryRareFlag.value },
            "Super Rare".takeIf { rarSuperRareFlag.value }
        )

        val types = listOfNotNull(
            "Creature".takeIf { typeCreatureFlag.value },
            "Spell".takeIf { typeSpellFlag.value },
            "Evolution Creature".takeIf { typeEvolutionCreatureFlag.value },
        ) + ocgTypeFlags.filter { it.value.value }.keys

        val setsParam = setFlags
            .filter { item -> item.value.value }
            .keys
            .toList()

        val langParam = langFlags
            .filter { it.value.value }
            .keys
            .toList()

        onApplyOptions(
            options.copy(
                civilizations = civs,
                multicolor = civMulticolorFlag.value,
                rarities = rars,
                types = types,
                sets = setsParam,
                languages = langParam,
                countLowerThan = countLowerThanState.value,
                countGreaterThan = countGreaterThanState.value,
            )
        )
    }

    fun resetFilters() {
        onApplyOptions(PrototypePrintsFilterParams(
            search=options.search
        ))
    }

    AppBottomSheet(onDismissRequest = onDismissRequest) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Filter",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Reset",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { resetFilters() },
            )
        }

        val screenHeight = LocalConfiguration.current.screenHeightDp.dp
        Column(
            modifier = Modifier
                .heightIn(max = screenHeight * 0.6f)
                .verticalScroll(rememberScrollState())
        ) {

            SectionHeader("Civilization")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CivToggleTile(Civilization.LIGHT, civLightFlag, Modifier.weight(1f), onToggle = ::onSingleCivToggled)
                CivToggleTile(Civilization.FIRE, civFireFlag, Modifier.weight(1f), onToggle = ::onSingleCivToggled)
                CivToggleTile(Civilization.WATER, civWaterFlag, Modifier.weight(1f), onToggle = ::onSingleCivToggled)
                CivToggleTile(Civilization.NATURE, civNatureFlag, Modifier.weight(1f), onToggle = ::onSingleCivToggled)
                CivToggleTile(Civilization.DARKNESS, civDarknessFlag, Modifier.weight(1f), onToggle = ::onSingleCivToggled)
                MulticolorToggleTile(civMulticolorFlag, Modifier.weight(1f), onToggle = ::onMulticolorToggled)
            }

            SectionHeader("Rarity")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RarityToggleTile("Common", rarCommonFlag, Modifier.weight(1f))
                RarityToggleTile("Uncommon", rarUncommonFlag, Modifier.weight(1f))
                RarityToggleTile("Rare", rarRareFlag, Modifier.weight(1f))
                RarityToggleTile("Very Rare", rarVeryRareFlag, Modifier.weight(1f))
                RarityToggleTile("Super Rare", rarSuperRareFlag, Modifier.weight(1f))
            }

            SectionHeader("Copies owned")

            CountRangeSlider(countGreaterThanState, countLowerThanState)

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(0.6f)) {
                    SectionHeader("Type")

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                        ToggleButton("Creature", typeCreatureFlag)
                        ToggleButton("Spell", typeSpellFlag)
                        ToggleButton("Evolution Creature", typeEvolutionCreatureFlag)
                    }

                    OcgToggleChip(showOcgTypes)

                    if (showOcgTypes.value) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                            ocgTypes.forEach { type ->
                                ToggleButton(type, ocgTypeFlags.getValue(type))
                            }
                        }
                    }
                }

                Column(modifier = Modifier.weight(0.4f)) {
                    SectionHeader("Language")

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                        langFlags
                            .toSortedMap()
                            .forEach {
                                ToggleButton("${LANGUAGE_FLAGS[it.key] ?: ""} ${it.key}", it.value)
                            }
                    }
                }
            }

            if(sets.isNotEmpty()) {

                SectionHeader("Set")

                FlowRow {
                    sets.forEach { set ->
                        if (setFlags[set] == null) {
                            setFlags[set] = mutableStateOf(false)
                        }
                        ToggleButton(set, remember { setFlags[set]!! })
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = { applyFilters() },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text("Apply filters", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
    )
}

/** Civilization filter tile: icon tile colored by [Civilization.color] when selected. */
@Composable
private fun CivToggleTile(
    civ: Civilization,
    state: MutableState<Boolean>,
    modifier: Modifier = Modifier,
    onToggle: (() -> Unit)? = null,
) {
    var value by state
    val tint = if (value) civ.color else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
    val background = if (value) civ.color.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.05f)
    val border = if (value) civ.color else Color.White.copy(alpha = 0.1f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .border(1.dp, border, RoundedCornerShape(12.dp))
            .clickable { value = !value; onToggle?.invoke() }
            .padding(6.dp),
    ) {
        CivilizationIcon(civ.displayName, color = tint)
        Spacer(Modifier.height(6.dp))
        Text(
            civ.displayName.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            maxLines = 1,
        )
    }
}

/** "Multicolor" filter tile: same visual language as [CivToggleTile] but for 2+-civilization cards, with no single [Civilization] backing it. */
@Composable
private fun MulticolorToggleTile(
    state: MutableState<Boolean>,
    modifier: Modifier = Modifier,
    onToggle: (() -> Unit)? = null,
) {
    var value by state
    val tint = if (value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
    val background = if (value) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.05f)
    val border = if (value) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .border(1.dp, border, RoundedCornerShape(12.dp))
            .clickable { value = !value; onToggle?.invoke() }
            .padding(6.dp),
    ) {
        Text("◐", style = MaterialTheme.typography.titleMedium, color = tint)
        Spacer(Modifier.height(6.dp))
        Text(
            "MULTI",
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            maxLines = 1,
        )
    }
}

/** Rarity filter tile: fixed glyph button, reusing [RarityIcon]'s existing glyph mapping. */
@Composable
private fun RarityToggleTile(rarity: String, state: MutableState<Boolean>, modifier: Modifier = Modifier) {
    var value by state
    val tint = if (value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
    val background = if (value) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.05f)
    val border = if (value) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .border(1.dp, border, RoundedCornerShape(12.dp))
            .clickable { value = !value },
    ) {
        RarityIcon(rarity, color = tint)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CountRangeSlider(
    lowerState: MutableState<Int?>,
    upperState: MutableState<Int?>,
    modifier: Modifier = Modifier,
    minScaleMax: Int = 10
) {
    var lower by lowerState
    var upper by upperState

    // Scale grows past minScaleMax if an incoming value is already higher, and keeps growing
    // as either thumb is dragged into the top numeric step, so the scale never hard-caps the
    // reachable value. It shrinks back toward the current selection once a drag ends, so the
    // track doesn't stay coarse forever after one excursion to a high value.
    var scaleMax by remember { mutableStateOf(maxOf(minScaleMax, lower ?: 0, upper ?: 0)) }

    // Track positions: 0 = lower thumb "off" (no lower bound), 1..scaleMax = the value itself,
    // scaleMax + 1 = the ∞ tick (no upper bound, i.e. "at least" when paired with a lower bound).
    // The two thumbs read/write independently now — dragging one no longer forces the other along.
    val infinityPosition = scaleMax + 1
    val lowerPosition = (lower ?: 0).toFloat()
    val upperPosition = upper?.toFloat() ?: infinityPosition.toFloat()

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "More than: ${if (lower == null) "Off" else "≥$lower"}   ·   Less than: ${if (upper == null) "Off" else "≤$upper"}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        )
        RangeSlider(
            value = lowerPosition..upperPosition,
            onValueChange = { range ->
                val lowerStep = range.start.roundToInt()
                val upperStep = range.endInclusive.roundToInt()

                if (lowerStep >= scaleMax || upperStep == scaleMax) {
                    scaleMax += minScaleMax
                }

                // Reaching/crossing the ∞ tick degrades that side to "no bound". The lower thumb
                // can only get there by being dragged past the upper one, which pushes both to
                // ∞ — equivalent to no filter at all, which is already a valid state.
                lower = if (lowerStep <= 0 || lowerStep >= infinityPosition) null else lowerStep
                upper = if (upperStep >= infinityPosition) null else upperStep
            },
            onValueChangeFinished = {
                // Re-fit the scale to the current selection (ignoring an open/∞ bound), rounded
                // up to a half-minScaleMax grid so there's still a bit of headroom past the
                // selection, floored at minScaleMax.
                val higherBound = maxOf(lower ?: 0, upper ?: 0)
                val gridStep = maxOf(1, minScaleMax / 2)
                val refit = ((higherBound + gridStep) / gridStep) * gridStep
                scaleMax = minOf(scaleMax, maxOf(minScaleMax, refit))
            },
            valueRange = 0f..infinityPosition.toFloat(),
            steps = scaleMax,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = Color.White.copy(alpha = 0.12f),
            ),
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Off",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "∞",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
            )
        }
    }
}

@Composable
private fun ToggleButton(
    text: String,
    state: MutableState<Boolean>,
    modifier: Modifier = Modifier
) {
    var value by state

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .wrapContentWidth()
            .padding(4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (value) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.05f))
            .border(
                1.dp,
                if (value) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.1f),
                RoundedCornerShape(14.dp),
            )
            .clickable { value = !value }
            .padding(horizontal = 11.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
        )
    }
}

/** Always-primary-tinted chip (unlike [ToggleButton]'s selected-state tint) that gates the OCG-only type row - a mode switch, not a filter selection itself. */
@Composable
private fun OcgToggleChip(state: MutableState<Boolean>, modifier: Modifier = Modifier) {
    var expanded by state

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .wrapContentWidth()
            .padding(top = 4.dp, bottom = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            .clickable { expanded = !expanded }
            .padding(horizontal = 11.dp, vertical = 6.dp),
    ) {
        Text(
            text = if (expanded) "Hide OCG" else "Show OCG",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
