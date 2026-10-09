package ro.daydreamstalgia.duelmastersinventory.features.transactions.ui.utils

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import ro.daydreamstalgia.duelmastersinventory.shared.data.transactions.repository.TransactionRepository
import ro.daydreamstalgia.duelmastersinventory.shared.utils.types.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class ChannelSelectorViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : BaseViewModel() {

    private val _existingChannels = MutableStateFlow(listOf<String>())

    val existingChannels = _existingChannels.stateInViewModelScope()

    suspend fun loadExistingChannels() {
        val values = transactionRepository.getDistinctChannels().first()
        _existingChannels.update { values }
    }
}
