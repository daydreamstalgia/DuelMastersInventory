package ro.ddnostalgia.duelmastersinventory.shared.data.cards.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ro.ddnostalgia.duelmastersinventory.shared.data.cards.model.AbilityKeyword
import ro.ddnostalgia.duelmastersinventory.shared.data.cards.model.CardPrototypeAbilityKeyword

@Dao
interface AbilityKeywordDao {
    /** One-shot snapshot of the registry - set-pack import parses against this, not a subscription. */
    @Query("SELECT * FROM AbilityKeyword")
    fun getAllOnce(): List<AbilityKeyword>

    @Query(
        """
        SELECT k.* FROM AbilityKeyword k
        INNER JOIN CardPrototypeAbilityKeyword link ON link.abilityKeywordId = k.id
        WHERE link.cardPrototypeId = :cardPrototypeId
        """
    )
    fun getForPrototype(cardPrototypeId: Int): Flow<List<AbilityKeyword>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertAssociation(association: CardPrototypeAbilityKeyword)
}
