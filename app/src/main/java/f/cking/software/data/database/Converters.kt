package f.cking.software.data.database

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromStringList(list: List<String>): String = list.joinToString(separator = ",")

    @TypeConverter
    fun toStringList(string: String): List<String> =
        if (string.isBlank()) {
            emptyList()
        } else {
            string.split(",")
        }
}
