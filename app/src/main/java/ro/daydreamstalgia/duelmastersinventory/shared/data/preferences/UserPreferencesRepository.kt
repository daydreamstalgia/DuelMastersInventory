package ro.daydreamstalgia.duelmastersinventory.shared.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.model.CardsGroupBy
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.model.CardsSortBy

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

enum class ThemeMode { LIGHT, DARK, SYSTEM }

/** Grid density on the cards screen: how many card columns per row. */
const val GRID_COLUMNS_COMPACT = 3
const val GRID_COLUMNS_MIN = 2
const val GRID_COLUMNS_MAX = 6

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val GRID_COLUMNS = intPreferencesKey("grid_columns")
        val CARDS_GROUP_BY = stringPreferencesKey("cards_group_by")
        val CARDS_SORT_BY = stringPreferencesKey("cards_sort_by")
        val CARDS_SORT_ASCENDING = booleanPreferencesKey("cards_sort_ascending")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    val gridColumns: Flow<Int> = context.dataStore.data.map { it[Keys.GRID_COLUMNS] ?: GRID_COLUMNS_COMPACT }

    suspend fun setGridColumns(columns: Int) {
        context.dataStore.edit { it[Keys.GRID_COLUMNS] = columns }
    }

    val cardsGroupBy: Flow<CardsGroupBy> = context.dataStore.data.map { prefs ->
        prefs[Keys.CARDS_GROUP_BY]?.let { raw -> runCatching { CardsGroupBy.valueOf(raw) }.getOrNull() }
            ?: CardsGroupBy.LANGUAGE_SET
    }

    suspend fun setCardsGroupBy(groupBy: CardsGroupBy) {
        context.dataStore.edit { it[Keys.CARDS_GROUP_BY] = groupBy.name }
    }

    val cardsSortBy: Flow<CardsSortBy> = context.dataStore.data.map { prefs ->
        prefs[Keys.CARDS_SORT_BY]?.let { raw -> runCatching { CardsSortBy.valueOf(raw) }.getOrNull() }
            ?: CardsSortBy.NONE
    }

    suspend fun setCardsSortBy(sortBy: CardsSortBy) {
        context.dataStore.edit { it[Keys.CARDS_SORT_BY] = sortBy.name }
    }

    val cardsSortAscending: Flow<Boolean> = context.dataStore.data.map { it[Keys.CARDS_SORT_ASCENDING] ?: true }

    suspend fun setCardsSortAscending(ascending: Boolean) {
        context.dataStore.edit { it[Keys.CARDS_SORT_ASCENDING] = ascending }
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        prefs[Keys.THEME_MODE]?.let { raw -> runCatching { ThemeMode.valueOf(raw) }.getOrNull() }
            ?: ThemeMode.SYSTEM
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    val dynamicColor: Flow<Boolean> = context.dataStore.data.map { it[Keys.DYNAMIC_COLOR] ?: true }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    /** specs/0006 - gates the one-time first-run onboarding flow (no bundled catalog assets). */
    val onboardingCompleted: Flow<Boolean> = context.dataStore.data.map { it[Keys.ONBOARDING_COMPLETED] ?: false }

    suspend fun setOnboardingCompleted() {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = true }
    }

    suspend fun resetOnboardingCompleted() {
        context.dataStore.edit { it.remove(Keys.ONBOARDING_COMPLETED) }
    }
}
