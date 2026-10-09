package ro.daydreamstalgia.duelmastersinventory.shared.ui.google

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.api.services.sheets.v4.Sheets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ro.daydreamstalgia.duelmastersinventory.shared.service.GoogleSheetsHelper

@Composable
fun rememberGoogleAuthContext(
    onSignedIn: ((email: String, sheetsService: Sheets) -> Unit)? = null,
): GoogleAuthContextData {
    val viewModel: GoogleAuthContextViewModel = hiltViewModel()
    val context = LocalContext.current

    val helper = viewModel.helper

    SideEffect {
        Log.d("GoogleSheetsContext", "Initializing GoogleSignIn")
        helper.initGoogleSignIn()
    }

    LaunchedEffect(viewModel.email, viewModel.sheetsService) {
        val service = viewModel.sheetsService
        val email = viewModel.email
        if (service != null && email != null) {
            onSignedIn?.invoke(email, service)
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d("GoogleSheetsContext", "Activity result received: resultCode=${result.resultCode}")
        val data = result.data
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            Log.d("GoogleSheetsContext", "Got account=${account?.email}")

            if (account != null && account.email != null) {
                // Use ViewModel to handle signing in
                viewModel.signIn(account)
            } else {
                Log.w("GoogleSheetsContext", "Sign-in failed: no account")
            }
        } catch (e: Exception) {
            Log.e("GoogleSheetsContext", "Exception in sign-in flow", e)
        }
    }

    val signInWithLastAccount: () -> Boolean = {
        val lastAccount = GoogleSignIn.getLastSignedInAccount(context)
        if (lastAccount != null && lastAccount.email != null) {
            viewModel.signIn(lastAccount)
            true
        }
        else {
            false
        }
    }

    val signIn: (skipLastAccountSignInAttempt:Boolean) -> Unit = {skip ->
        if(skip || !signInWithLastAccount()) {
            launcher.launch(helper.googleSignInClient.signInIntent)
        }
    }

    val signOut: () -> Unit = {
        viewModel.signOut()
    }

    val coroutineScope = rememberCoroutineScope()
    SideEffect {
        coroutineScope.launch {
            signInWithLastAccount()
        }
    }


    return GoogleAuthContextData(
            email = viewModel.email,
            sheetsService = viewModel.sheetsService,
            signInWithLastAccount = signInWithLastAccount,
            signIn = signIn,
            signOut = signOut,
            isSignedIn = viewModel.isSignedIn,
            error = viewModel.error
        )
}

class GoogleAuthContextData(
    val email: String?,
    val sheetsService: Sheets?,
    val signInWithLastAccount: ()->Boolean,
    val signIn: (skipLastAccountSignInAttempt:Boolean) -> Unit,
    val signOut: () -> Unit,
    val isSignedIn: Boolean,
    val error: String?,
)
