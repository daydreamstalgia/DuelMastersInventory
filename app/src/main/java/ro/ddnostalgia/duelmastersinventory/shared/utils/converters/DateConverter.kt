package ro.ddnostalgia.duelmastersinventory.shared.utils.converters

import androidx.room.TypeConverter
import java.time.LocalDate

object DateConverter{
    @TypeConverter
    fun toLocalDate(dateStr:String?): LocalDate? = LocalDate.parse(dateStr);

    @TypeConverter
    fun fromLocalDate(date: LocalDate?):String? = date?.toString()
}