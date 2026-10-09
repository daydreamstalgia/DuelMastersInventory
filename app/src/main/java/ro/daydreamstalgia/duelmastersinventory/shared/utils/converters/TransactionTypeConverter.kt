package ro.daydreamstalgia.duelmastersinventory.shared.utils.converters

import androidx.room.TypeConverter
import ro.daydreamstalgia.duelmastersinventory.shared.data.transactions.model.TransactionType

object TransactionTypeConverter {
    @TypeConverter
    fun fromEnum(type: TransactionType): String = type.name

    @TypeConverter
    fun toEnum(value: String): TransactionType = TransactionType.valueOf(value)
}