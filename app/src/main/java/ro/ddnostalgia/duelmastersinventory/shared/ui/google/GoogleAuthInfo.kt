package ro.ddnostalgia.duelmastersinventory.shared.ui.google

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ro.ddnostalgia.duelmastersinventory.shared.ui.components.transactions.OUTBOUND_ON_COLOR

@Composable
fun GoogleAuthInfo(
    data: GoogleAuthContextData,
    mode: String = "card",
    autoSignIn: Boolean = false,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    SideEffect {
        coroutineScope.launch {
            if(!data.signInWithLastAccount() && autoSignIn) {
                delay(500)
                if (!data.isSignedIn) {
                    data.signIn(true)
                }
            }
        }
    }

    if(mode=="notification") {
        Column(modifier) {
            Row(modifier=Modifier.align(Alignment.End)) {
                Text("Google sign in: ")
                Text("●", color = if(data.isSignedIn) Color.Green else Color.Red)
            }
        }

    } else if (mode == "row") {
        // Avatar-circle account row (spec's shared account-row pattern, matching
        // DrawerScaffold's AccountRow/ActorsScreen's row look) — reused by both the
        // spreadsheets list and Settings screens instead of forking their own copies.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .clickable(enabled = !data.isSignedIn) { data.signIn(false) }
                .padding(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    data.email?.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                Text(
                    data.email ?: "Not signed in",
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (data.isSignedIn) "Signed in to Google" else "Tap to sign in",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (data.isSignedIn) OUTBOUND_ON_COLOR else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            if (data.isSignedIn) {
                Text(
                    "Sign out",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .clickable { data.signOut() }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            } else {
                Text(
                    "›",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f),
                )
            }
        }

    }else if(mode=="card") {
        Card(modifier) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(text = "Google sign-in", fontSize = 16.sp)

                if (data.error != null) {
                    Text(data.error, color = Color.Red)
                }

                if (data.isSignedIn) {
                    Text(data.email ?: "")
                    Button(
                        onClick = { data.signOut() },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Sign out")
                    }
                } else {
                    Text("Not signed in.")
                    Button(
                        onClick = { data.signIn(false) },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Sign in")
                    }
                }
            }
        }
    }
}