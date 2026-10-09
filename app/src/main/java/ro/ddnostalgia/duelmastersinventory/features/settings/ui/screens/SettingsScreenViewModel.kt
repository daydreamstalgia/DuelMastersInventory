package ro.ddnostalgia.duelmastersinventory.features.settings.ui.screens

import android.content.Context
import android.net.Uri
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ro.ddnostalgia.duelmastersinventory.shared.data.cards.repository.CardPrintReorderRepository
import ro.ddnostalgia.duelmastersinventory.shared.data.cards.repository.CardPrintRepository
import ro.ddnostalgia.duelmastersinventory.shared.data.preferences.ThemeMode
import ro.ddnostalgia.duelmastersinventory.shared.data.preferences.UserPreferencesRepository
import ro.ddnostalgia.duelmastersinventory.shared.data.transactions.repository.TransactedCardRepository
import ro.ddnostalgia.duelmastersinventory.shared.utils.types.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsScreenViewModel @Inject constructor(
    private val transactedCardRepository: TransactedCardRepository,
    private val cardPrintRepository: CardPrintRepository,
    private val cardPrintReorderRepository: CardPrintReorderRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
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

    suspend fun reorderCardPrintIds(): Int {
        return withContext(Dispatchers.IO) {
            cardPrintReorderRepository.reorderToCanonicalOrder()
        }
    }
}