package ro.daydreamstalgia.duelmastersinventory

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import ro.daydreamstalgia.duelmastersinventory.nav.AppNavHost
import ro.daydreamstalgia.duelmastersinventory.nav.Routes
import ro.daydreamstalgia.duelmastersinventory.shared.data.DuelMastersInventoryDatabase
import ro.daydreamstalgia.duelmastersinventory.shared.data.preferences.ThemeMode
import ro.daydreamstalgia.duelmastersinventory.shared.data.preferences.UserPreferencesRepository
import ro.daydreamstalgia.duelmastersinventory.shared.ui.theme.DuelMastersInventoryTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    @Inject
    lateinit var database: DuelMastersInventoryDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val themeMode by userPreferencesRepository.themeMode.collectAsState(ThemeMode.SYSTEM)
            val dynamicColor by userPreferencesRepository.dynamicColor.collectAsState(true)
            // null = not yet resolved - hold off rendering the nav graph rather than guessing a
            // start destination (specs/0006). A catalog that already has data (an install from
            // before this flag existed, e.g. an in-place update on a real device) counts as
            // already onboarded even if the flag was never set - onboarding is for a truly empty
            // catalog, not something to force on an existing user with real data.
            val startDestination by produceState<String?>(initialValue = null) {
                val alreadyOnboarded = userPreferencesRepository.onboardingCompleted.first()
                val catalogHasData = withContext(Dispatchers.IO) { database.cardPrototypeDao().count() > 0 }
                if (!alreadyOnboarded && catalogHasData) {
                    userPreferencesRepository.setOnboardingCompleted()
                }
                value = if (alreadyOnboarded || catalogHasData) Routes.CardList.route else Routes.Onboarding.route
            }
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            DuelMastersInventoryTheme(darkTheme = darkTheme, dynamicColor = dynamicColor) {
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    startDestination?.let { destination ->
                        AppNavHost(startDestination = destination)
                    }
                }
            }
        }
    }
}