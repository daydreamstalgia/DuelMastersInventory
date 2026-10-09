package ro.daydreamstalgia.duelmastersinventory.features.settings.ui.screens

import android.content.Context
import android.net.Uri
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ro.daydreamstalgia.duelmastersinventory.shared.data.DuelMastersInventoryDatabase
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.repository.CardPrintReorderRepository
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.repository.CardPrintRepository
import ro.daydreamstalgia.duelmastersinventory.shared.data.preferences.ThemeMode
import ro.daydreamstalgia.duelmastersinventory.shared.data.preferences.UserPreferencesRepository
import ro.daydreamstalgia.duelmastersinventory.shared.data.transactions.repository.TransactedCardRepository
import ro.daydreamstalgia.duelmastersinventory.shared.utils.types.BaseViewModel
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SettingsScreenViewModel @Inject constructor(
    private val transactedCardRepository: TransactedCardRepository,
    private val cardPrintRepository: CardPrintRepository,
    private val cardPrintReorderRepository: CardPrintReorderRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val database: DuelMastersInventoryDatabase,
) : BaseViewModel() {

    val themeMode = userPreferencesRepository.themeMode.stateInViewModelScope(ThemeMode.SYSTEM)
    val dynamicColor = userPreferencesRepository.dynamicColor.stateInViewModelScope(true)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { userPreferencesRepository.setThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setDynamicColor(enabled) }
    }

    fun exportDatabaseToUri(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dbFile = context.getDatabasePath("dminventory.db")

                if (!dbFile.exists()) return@launch

                context.contentResolver.openOutputStream(uri)?.use { output ->
                    dbFile.inputStream().use { input ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun removeOwnedCardsFromWishlist() {
        withContext(Dispatchers.IO) {
            val ownedCardPrintIds = transactedCardRepository
                .getOwnedTransactedCards()
                .first()
                .map { it.print.id }
                .toSet()
                .toList()

            val data = cardPrintRepository
                .getUserDataByIds(ownedCardPrintIds)
                .first()
                .filter { it.wishlist }
                .map { it.copy(wishlist = false) }

            data.forEach { cardPrintRepository.setUserData(it) }
        }
    }

    /**
     * Wipes every table plus the imported card images, and clears the onboarding flag so the
     * app starts over as a fresh install. Appearance preferences are kept.
     */
    suspend fun resetDatabase(context: Context) {
        withContext(Dispatchers.IO) {
            database.clearAllTables()
            File(context.filesDir, "card_images").deleteRecursively()
            File(context.filesDir, "card_images_low").deleteRecursively()
        }
        userPreferencesRepository.resetOnboardingCompleted()
    }

    suspend fun reorderCardPrintIds(): Int {
        return withContext(Dispatchers.IO) {
            cardPrintReorderRepository.reorderToCanonicalOrder()
        }
    }
}