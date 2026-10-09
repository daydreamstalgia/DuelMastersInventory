package ro.ddnostalgia.duelmastersinventory.shared.data.transactions.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import ro.ddnostalgia.duelmastersinventory.shared.data.actors.model.Actor
import ro.ddnostalgia.duelmastersinventory.shared.utils.converters.DateConverter
import ro.ddnostalgia.duelmastersinventory.shared.utils.converters.TransactionTypeConverter
import java.time.LocalDate

@Entity(
    tableName = "Transaction",
    foreignKeys = [
        ForeignKey(
            entity = Transaction::class,
            parentColumns = ["id"],
            childColumns = ["tradeReferenceId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Actor::class,
            parentColumns = ["id"],
            childColumns = ["actorId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index(value = ["tradeReferenceId"]), Index(value = ["actorId"])]
)
@TypeConverters(DateConverter::class, TransactionTypeConverter::class)
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val type: TransactionType,
    val actorId: Int,
    val channel: String? = null,
    val date: LocalDate,
    val costEuro: Double,
    val description: String,
    val tradeReferenceId: Int? = null,
    val parcelTrackingNumber: String? = null,
)