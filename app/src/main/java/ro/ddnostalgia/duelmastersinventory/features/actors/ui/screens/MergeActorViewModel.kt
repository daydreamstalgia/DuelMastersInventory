package ro.ddnostalgia.duelmastersinventory.features.actors.ui.screens

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import ro.ddnostalgia.duelmastersinventory.shared.data.actors.model.ActorWithAliases
import ro.ddnostalgia.duelmastersinventory.shared.data.actors.repository.ActorRepository
import ro.ddnostalgia.duelmastersinventory.shared.utils.types.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class MergeActorViewModel @Inject constructor(
    private val actorRepository: ActorRepository
) : BaseViewModel() {

    private val _actors = MutableStateFlow(listOf<ActorWithAliases>())
    private val _search = MutableStateFlow<String?>(null)

    val actors = _actors.stateInViewModelScope()
    val search = _search.stateInViewModelScope(null)

    fun setSearch(search: String?) {
        _search.update { search }
    }

    suspend fun loadActors() {
        val values = actorRepository.filterWithAliases(_search.value).first()
        _actors.update { values }
    }
}
