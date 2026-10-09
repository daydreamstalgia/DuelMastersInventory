package ro.ddnostalgia.duelmastersinventory.shared.service

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GoogleSheetsHelper @Inject constructor (
    @ApplicationContext private val context: Context
) {

    lateinit var googleSignInClient: GoogleSignInClient
        private set

    fun initGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(SheetsScopes.SPREADSHEETS))
            .build()

        googleSignInClient = GoogleSignIn.getClient(context, gso)
    }

    /**
     * Builds a Sheets service with a fresh access token for the given account.
     */
    suspend fun buildSheetsService(account: GoogleSignInAccount): Sheets =
        withContext(Dispatchers.IO) {
            // Request OAuth access token with Sheets scope
            val scope = "oauth2:${SheetsScopes.SPREADSHEETS}"
            val token = GoogleAuthUtil.getToken(context, account.account!!, scope)

            val credential = GoogleCredential().setAccessToken(token)
            Sheets.Builder(
                com.google.api.client.http.javanet.NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            )
                .setApplicationName("Duel Masters Inventory")
                .build()
        }

    suspend fun signOut() = withContext(Dispatchers.Main) {
        googleSignInClient.signOut().addOnCompleteListener {
            // Optional: log or handle completion
            Log.d("GoogleSheetsHelper", "Signed out from Google")
        }
    }

}
