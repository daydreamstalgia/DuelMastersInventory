package ro.ddnostalgia.duelmastersinventory.shared.data.actors.repository

import androidx.room.withTransaction
import kotlinx.coroutines.flow.first
import ro.ddnostalgia.duelmastersinventory.shared.data.DuelMastersInventoryDatabase
import ro.ddnostalgia.duelmastersinventory.shared.data.actors.model.Actor
import ro.ddnostalgia.duelmastersinventory.shared.data.actors.model.ActorAlias
import javax.inject.Inject

class ActorRepository @Inject constructor(
    private val db: DuelMastersInventoryDatabase
) {
    private val actorDao = db.actorDao()
    private val actorAliasDao = db.actorAliasDao()
    private val transactionDao = db.transactionDao()

    fun getById(id: Int) = actorDao.getById(id)

    fun getAll() = actorDao.getAll()

    fun getWithAliasesById(id: Int) = actorDao.getWithAliasesById(id)

    fun getAllWithAliases() = actorDao.getAllWithAliases()

    fun filter(search: String?) = actorDao.filter(search)

    fun filterWithAliases(search: String?) = actorDao.filterWithAliases(search)

    fun getAliasesByActorId(actorId: Int) = actorAliasDao.getByActorId(actorId)

    fun getDistinctAliasPlatforms() = actorAliasDao.getDistinctPlatforms()

    suspend fun save(actor: Actor, aliases: List<ActorAlias>): Actor {
        val savedActor = if (actor.id == 0) {
            actor.copy(id = actorDao.insert(actor).toInt())
        } else {
            actorDao.update(actor)
            actor
        }

        actorAliasDao.deleteByActorId(savedActor.id)
        if (aliases.isNotEmpty()) {
            actorAliasDao.insertAll(aliases.map { it.copy(actorId = savedActor.id) })
        }

        return savedActor
    }

    suspend fun delete(actor: Actor) {
        actorDao.delete(actor)
    }

    // Reparents the source actor's aliases onto the target (skipping ones the
    // target already has), repoints every Transaction from source to target,
    // then removes the now-unreferenced source actor.
    suspend fun mergeInto(sourceActorId: Int, targetActorId: Int) {
        require(sourceActorId != targetActorId) { "Cannot merge an actor into itself" }

        db.withTransaction {
            val sourceActor = checkNotNull(actorDao.getById(sourceActorId).first())
            val sourceAliases = actorAliasDao.getByActorId(sourceActorId).first()
            val targetAliases = actorAliasDao.getByActorId(targetActorId).first()

            val existingKeys = targetAliases
                .map { it.platform.lowercase() to it.username.lowercase() }
                .toSet()

            sourceAliases.forEach { alias ->
                val key = alias.platform.lowercase() to alias.username.lowercase()
                if (key in existingKeys) {
                    actorAliasDao.delete(alias)
                } else {
                    actorAliasDao.reparent(alias.id, targetActorId)
                }
            }

            transactionDao.reassignActor(sourceActorId, targetActorId)

            actorDao.delete(sourceActor)
        }
    }
}
