package ro.daydreamstalgia.duelmastersinventory.shared.utils.constants

import ro.daydreamstalgia.duelmastersinventory.BuildConfig

/**
 * Build-time feature flags (see app/build.gradle.kts `featureFlag`). All default to off so a
 * public build ships without them; enable per machine in local.properties.
 */
object FeatureFlags {
    /**
     * Actors: tracking the real-world people/accounts behind each transaction. When off,
     * transactions carry no actor and the free-text description is the only "who" info.
     */
    const val ACTORS: Boolean = BuildConfig.FEATURE_ACTORS

    /** Google Sign-In plus the Google Sheets export (Spreadsheets screen). */
    const val GOOGLE_SHEETS: Boolean = BuildConfig.FEATURE_GOOGLE_SHEETS

    /** Settings > "Reset database": wipes all data and sends the app back through onboarding. */
    const val RESET_DATABASE: Boolean = BuildConfig.FEATURE_RESET_DATABASE
}
