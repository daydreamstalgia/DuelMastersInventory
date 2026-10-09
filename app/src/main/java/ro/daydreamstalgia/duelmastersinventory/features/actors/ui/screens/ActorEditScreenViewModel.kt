package ro.daydreamstalgia.duelmastersinventory.features.actors.ui.screens

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import ro.daydreamstalgia.duelmastersinventory.shared.data.actors.model.Actor
import ro.daydreamstalgia.duelmastersinventory.shared.data.actors.model.ActorAlias
import ro.daydreamstalgia.duelmastersinventory.shared.data.actors.repository.ActorRepository
import ro.daydreamstalgia.duelmastersinventory.shared.utils.types.BaseRouteViewModel
import javax.inject.Inject

@HiltViewModel
class ActorEditScreenViewModel @Inject constructor(
    private val actorRepository: ActorRepository,
    savedStateHandle: SavedStateHandle,
) : BaseRouteViewModel(savedStateHandle) {
    private val actorId = routeArgIntOrNull("id")

    private val _actor = MutableStateFlow(EditableActor())

    val form = _actor.stateInViewModelScope(EditableActor())

    suspend fun loadActor() {
        val id = actorId ?: return

        val actor = actorRepository.getById(id).first() ?: return
        val aliases = actorRepository.getAliasesByActorId(id).first()

        _actor.update {
            EditableActor(
                id = actor.id,
                firstName = actor.firstName,
                lastName = actor.lastName,
                aliases = aliases.map { EditableActorAlias(it.id, it.platform, it.username, it.url) }
            )
        }
    }

    fun updateForm(form: EditableActor) {
        _actor.update { form }
    }

    fun addAlias() {
        updateForm(form.value.copy(aliases = form.value.aliases + EditableActorAlias()))
    }

    fun updateAlias(index: Int, alias: EditableActorAlias) {
        updateForm(form.value.copy(aliases = form.value.aliases.toMutableList().apply { set(index, alias) }))
    }

    fun removeAlias(index: Int) {
        updateForm(form.value.copy(aliases = form.value.aliases.toMutableList().apply { removeAt(index) }))
    }

    suspend fun saveChanges(): Int? {
        val f = form.value

        val actor = Actor(
            id = f.id ?: 0,
            firstName = f.firstName?.ifBlank { null },
            lastName = f.lastName?.ifBlank { null },
        )

        val aliases = f.aliases
            .filter { it.platform.isNotBlank() && it.username.isNotBlank() }
            .map { ActorAlias(actorId = actor.id, platform = it.platform, username = it.username, url = it.url?.ifBlank { null }) }

        val saved = actorRepository.save(actor, aliases)

        return saved.id
    }

    data class EditableActor(
        val id: Int? = null,
        val firstName: String? = null,
        val lastName: String? = null,
        val aliases: List<EditableActorAlias> = listOf(),
    )

    data class EditableActorAlias(
        val id: Int? = null,
        val platform: String = "",
        val username: String = "",
        val url: String? = null,
    )
}
