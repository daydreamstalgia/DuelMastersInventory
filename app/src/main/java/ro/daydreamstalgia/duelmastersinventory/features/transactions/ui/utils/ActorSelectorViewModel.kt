package ro.daydreamstalgia.duelmastersinventory.features.transactions.ui.utils

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import ro.daydreamstalgia.duelmastersinventory.shared.data.actors.model.Actor
import ro.daydreamstalgia.duelmastersinventory.shared.data.actors.model.ActorWithAliases
import ro.daydreamstalgia.duelmastersinventory.shared.data.actors.repository.ActorRepository
import ro.daydreamstalgia.duelmastersinventory.shared.data.transactions.repository.TransactionRepository
import ro.daydreamstalgia.duelmastersinventory.shared.utils.types.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class ActorSelectorViewModel @Inject constructor(
    private val actorRepository: ActorRepository,
    private val transactionRepository: TransactionRepository,
) : BaseViewModel() {

    /** Creates a bare-bones actor from the search text, without leaving the picker sheet. */
    suspend fun createActor(name: String): Int {
        val saved = actorRepository.save(Actor(firstName = name, lastName = null), emptyList())
        return saved.id
    }

    private val _actors = MutableStateFlow(listOf<ActorWithAliases>())
    private val _selectedActor = MutableStateFlow<ActorWithAliases?>(null)
    private val _search = MutableStateFlow<String?>(null)
    private val _dealCounts = MutableStateFlow(mapOf<Int, Int>())

    val actors = _actors.stateInViewModelScope()
    val search = _search.stateInViewModelScope(null)

    val selectedActor = _selectedActor.stateInViewModelScope()

    // "N deals" trailing text on each row of the "Who?" picker (spec `3c`) — reuses the same
    // TransactionRepository.countByActorId query ActorViewScreenViewModel already uses per actor.
    val dealCounts = _dealCounts.stateInViewModelScope(mapOf())

    fun setSearch(search: String?) {
        _search.update { search }
    }

    suspend fun loadActors() {
        val values = actorRepository.filterWithAliases(_search.value).first()
        _actors.update { values }
        _dealCounts.update {
            values.associate { it.actor.id to transactionRepository.countByActorId(it.actor.id).first() }
        }
    }

    suspend fun loadSelectedActor(id: Int?) {
        if (id == null) {
            _selectedActor.update { null }
            return
        }

        val value = actorRepository.getWithAliasesById(id).first()
        _selectedActor.update { value }
    }
}
