package ro.daydreamstalgia.duelmastersinventory.shared.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import ro.daydreamstalgia.duelmastersinventory.shared.utils.types.Fonts

/**
 * Codex type system: Perceval for screen/card titles, Officina for
 * everything else (varying weight/tracking covers the spec's uppercase
 * micro-labels via [labelSmall]). Two roles deliberately sit outside
 * Typography because they're used in only a handful of places and would
 * otherwise leak into default Material components:
 *  - Armada (civilization/race bylines, e.g. "ANGEL COMMAND") -> [RaceLabelStyle]
 *  - DMNumbers (mana/power/counts/money) -> AmountText composable
 *    (shared/ui/components/core/AmountText.kt)
 */
val Typography = Typography(
    headlineSmall = TextStyle(
        fontFamily = Fonts.percevalBoldFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        lineHeight = 24.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = Fonts.percevalBoldFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 19.sp,
        lineHeight = 22.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Fonts.percevalBoldFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 21.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = Fonts.percevalBoldFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 18.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = Fonts.officinaSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Fonts.officinaSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = Fonts.officinaSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 10.5.sp,
        lineHeight = 15.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = Fonts.officinaSansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.02.em,
    ),
    labelMedium = TextStyle(
        fontFamily = Fonts.officinaSansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
    ),
    // The uppercase, letter-spaced section-header micro-label that appears
    // throughout the spec ("CARDS", "COPIES OWNED", "RARITY", ...). Callers
    // are responsible for uppercasing the string.
    labelSmall = TextStyle(
        fontFamily = Fonts.officinaSansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 9.5.sp,
        lineHeight = 12.sp,
        letterSpacing = 0.12.em,
    ),
)

/** Armada, for civilization/race bylines only (e.g. "ANGEL COMMAND"). */
val RaceLabelStyle = TextStyle(
    fontFamily = Fonts.armadaBoldFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 11.sp,
    letterSpacing = 0.14.em,
)
