package ro.ddnostalgia.duelmastersinventory.shared.data.actors.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ro.ddnostalgia.duelmastersinventory.shared.data.actors.model.ActorAlias

@Dao
interface ActorAliasDao {
    @Query("SELECT * FROM ActorAlias WHERE actorId=:actorId")
    fun getByActorId(actorId: Int): Flow<List<ActorAlias>>

    @Query("SELECT DISTINCT platform FROM ActorAlias ORDER BY platform")
    fun getDistinctPlatforms(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(aliases: List<ActorAlias>)

    @Query("DELETE FROM ActorAlias WHERE actorId=:actorId")
    suspend fun deleteByActorId(actorId: Int)

    @Delete
    suspend fun delete(alias: ActorAlias)

    @Query("UPDATE ActorAlias SET actorId=:targetActorId WHERE id=:aliasId")
    suspend fun reparent(aliasId: Int, targetActorId: Int)
}
