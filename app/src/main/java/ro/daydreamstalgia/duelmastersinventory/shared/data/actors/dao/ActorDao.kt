package ro.daydreamstalgia.duelmastersinventory.shared.data.actors.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ro.daydreamstalgia.duelmastersinventory.shared.data.actors.model.Actor
import ro.daydreamstalgia.duelmastersinventory.shared.data.actors.model.ActorWithAliases

@Dao
interface ActorDao {
    @Query("SELECT * FROM Actor WHERE id=:id")
    fun getById(id: Int): Flow<Actor?>

    @Query("SELECT * FROM Actor")
    fun getAll(): Flow<List<Actor>>

    @Transaction
    @Query("SELECT * FROM Actor WHERE id=:id")
    fun getWithAliasesById(id: Int): Flow<ActorWithAliases?>

    @Transaction
    @Query("SELECT * FROM Actor")
    fun getAllWithAliases(): Flow<List<ActorWithAliases>>

    @Query(
        """
        SELECT * FROM Actor
        WHERE
            (:search IS NULL
             OR LOWER(firstName) LIKE '%' || LOWER(:search) || '%'
             OR LOWER(lastName) LIKE '%' || LOWER(:search) || '%'
             OR id IN (
                SELECT actorId FROM ActorAlias
                WHERE LOWER(username) LIKE '%' || LOWER(:search) || '%'
             ))
        ORDER BY lastName, firstName
    """
    )
    fun filter(search: String?): Flow<List<Actor>>

    @Transaction
    @Query(
        """
        SELECT * FROM Actor
        WHERE
            (:search IS NULL
             OR LOWER(firstName) LIKE '%' || LOWER(:search) || '%'
             OR LOWER(lastName) LIKE '%' || LOWER(:search) || '%'
             OR id IN (
                SELECT actorId FROM ActorAlias
                WHERE LOWER(username) LIKE '%' || LOWER(:search) || '%'
             ))
        ORDER BY lastName, firstName
    """
    )
    fun filterWithAliases(search: String?): Flow<List<ActorWithAliases>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(actor: Actor): Long

    @Update
    suspend fun update(actor: Actor): Int

    @Delete
    suspend fun delete(actor: Actor)
}
