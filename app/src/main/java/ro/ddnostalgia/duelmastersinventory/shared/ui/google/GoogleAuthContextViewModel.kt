package ro.ddnostalgia.duelmastersinventory.shared.ui.google

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.google.api.services.sheets.v4.Sheets
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ro.ddnostalgia.duelmastersinventory.shared.service.GoogleSheetsHelper
import ro.ddnostalgia.duelmastersinventory.shared.utils.types.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class GoogleAuthContextViewModel @Inject constructor(
    val helper: GoogleSheetsHelper
): BaseViewModel() {
    var email: String? by mutableStateOf(null)
        private set

    var sheetsService: Sheets? by mutableStateOf(null)
        private set

    var error: String? by mutableStateOf(null)
        private set

    val isSignedIn: Boolean
        get() = email != null && sheetsService != null

    private var currentAccountId: String? = null

    fun signIn(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount) {
        if (currentAccountId == account.id && sheetsService != null) return

        viewModelScope.launch {
            try {
                val service = withContext(Dispatchers.IO) {
                    helper.buildSheetsService(account)
                }
                email = account.email
                sheetsService = service
                currentAccountId = account.id
            } catch (e: Exception) {
                error = e.message
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            helper.signOut()
            email = null
            sheetsService = null
            error = null
        }
    }

}