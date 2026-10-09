package ro.daydreamstalgia.duelmastersinventory.features.transactions.ui.screens

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import ro.daydreamstalgia.duelmastersinventory.shared.data.actors.repository.ActorRepository
import ro.daydreamstalgia.duelmastersinventory.shared.data.actors.utils.displayName
import ro.daydreamstalgia.duelmastersinventory.shared.data.transactions.model.Transaction
import ro.daydreamstalgia.duelmastersinventory.shared.data.transactions.model.TransactionType
import ro.daydreamstalgia.duelmastersinventory.shared.data.transactions.repository.TransactionRepository
import ro.daydreamstalgia.duelmastersinventory.shared.utils.constants.FeatureFlags
import ro.daydreamstalgia.duelmastersinventory.shared.utils.types.BaseRouteViewModel
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class TransactionEditScreenViewModel @Inject constructor (
    private val transactionRepository: TransactionRepository,
    private val actorRepository: ActorRepository,
    savedStateHandle: SavedStateHandle,
): BaseRouteViewModel(savedStateHandle) {
    private val transactionId = routeArgIntOrNull("id")

    private val _transaction = MutableStateFlow(EditableTransaction())

    val form = _transaction.stateInViewModelScope(EditableTransaction())

    // Completed-steps summary (spec `3c`) shows the actor's real name/alias, not a raw id.
    @OptIn(ExperimentalCoroutinesApi::class)
    val actorDisplayName = _transaction
        .flatMapLatest { t ->
            val id = t.actorId
            if (id == null) flowOf(null) else actorRepository.getWithAliasesById(id).map { it.displayName(t.channel) }
        }
        .stateInViewModelScope()

    suspend fun loadTransaction() {
        val id = transactionId ?: return

        val transaction = transactionRepository
            .getById(id)
            .first() ?: return

        _transaction.update {
            EditableTransaction(
                id = transaction.id,
                type = transaction.type,
                actorId = transaction.actorId,
                channel = transaction.channel,
                date = transaction.date,
                costEuro = transaction.costEuro,
                description = transaction.description,
                tradeReferenceId = transaction.tradeReferenceId,
                parcelTrackingNumber = transaction.parcelTrackingNumber,
            )
        }
    }

    fun updateForm(form: EditableTransaction) {
        _transaction.update { form }
    }

    suspend fun saveChanges(): Int? {
        val t = form.value

        val transaction = Transaction(
            id = t.id ?: 0,
            type = t.type ?: return null,
            actorId = if (FeatureFlags.ACTORS) t.actorId ?: return null else t.actorId,
            channel = t.channel,
            date = t.date ?: return null,
            costEuro = t.costEuro ?: return null,
            description = t.description ?: "",
            tradeReferenceId = t.tradeReferenceId,
            parcelTrackingNumber = t.parcelTrackingNumber
        )

        var result: Int? = null

        if(transactionId==null) {
            val insertId = transactionRepository.insert(transaction)
            result = insertId
        } else {
            Log.d("UPDATE", transaction.toString())
            val updatedRows = transactionRepository.update(transaction)
            Log.d("UPDATE", updatedRows.toString())
            if(updatedRows > 0) {
                result = transactionId
            }
        }

        if(result==null) {
            return null
        }

        if(transaction.tradeReferenceId!=null) {
            val pairedTransaction = transactionRepository
                .getById(transaction.tradeReferenceId)
                .first()

            if(pairedTransaction!=null) {
                if (pairedTransaction.tradeReferenceId == null) {
                    transactionRepository.update(
                        pairedTransaction.copy(tradeReferenceId = transaction.id)
                    )
                }
            }
        }

        return result
    }

    data class EditableTransaction(
        val id: Int? = null,
        val type: TransactionType? = null,
        val actorId: Int? = null,
        val channel: String? = null,
        val date: LocalDate? = null,
        val costEuro: Double? = null,
        val description: String? = null,
        val tradeReferenceId: Int? = null,
        val parcelTrackingNumber: String? = null,
    )
}