package ro.ddnostalgia.duelmastersinventory.shared.data.transactions.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName="TransactedCard",
    foreignKeys = [
        ForeignKey(
            entity = Transaction::class,
            parentColumns = ["id"],
            childColumns = ["inTransactionId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = Transaction::class,
            parentColumns = ["id"],
            childColumns = ["outTransactionId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index(value = ["inTransactionId"]), Index(value = ["outTransactionId"])]

)
data class TransactedCard (
    @PrimaryKey(autoGenerate = true)
    val id:Int = 0,
    val cardPrintId:Int,

    val inTransactionId: Int,
    val outTransactionId: Int? = null,
    val condition: String? = null
)