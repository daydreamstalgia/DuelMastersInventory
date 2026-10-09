package ro.ddnostalgia.duelmastersinventory.shared.utils.types

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import ro.ddnostalgia.duelmastersinventory.shared.utils.constants.TIMEOUT_MILLIS

open class BaseViewModel : ViewModel() {

    protected fun <T> Flow<T>.stateInViewModelScope(
        initialValue: T,
        timeoutMillis: Long = TIMEOUT_MILLIS
    ): StateFlow<T> = this.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(timeoutMillis),
        initialValue = initialValue
    )

    protected fun <T> Flow<List<T>>.stateInViewModelScope(
        initialValue: List<T> = listOf(),
        timeoutMillis: Long = TIMEOUT_MILLIS
    ): StateFlow<List<T>> = this.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(timeoutMillis),
        initialValue = initialValue
    )

    protected fun <K,V> Flow<Map<K,V>>.stateInViewModelScope(
        initialValue: Map<K,V> = mapOf(),
        timeoutMillis: Long = TIMEOUT_MILLIS
    ): StateFlow<Map<K,V>> = this.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(timeoutMillis),
        initialValue = initialValue
    )


    protected fun <T> Flow<T?>.stateInViewModelScope(
        timeoutMillis: Long = TIMEOUT_MILLIS
    ): StateFlow<T?> = this.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(timeoutMillis),
        initialValue = null
    )

}