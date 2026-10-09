package ro.daydreamstalgia.duelmastersinventory.nav

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ScreenConfig {
    private var _actions = MutableStateFlow<List<Pair<String, ()->Unit>>?>(null)

    val actions = _actions.asStateFlow()

    fun setDropdownAction(actions: List<Pair<String, ()->Unit>>) {
        _actions.update { actions }
    }

    // Lets a screen intercept the BackScaffold back button/system back (e.g. to confirm
    // discarding unsaved changes) instead of navigating up immediately.
    private var _onBack = MutableStateFlow<(() -> Unit)?>(null)

    val onBack = _onBack.asStateFlow()

    fun setOnBack(onBack: (() -> Unit)?) {
        _onBack.update { onBack }
    }
}