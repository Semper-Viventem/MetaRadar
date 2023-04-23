package f.cking.software.data.database

import androidx.room.TypeConverter

class Converters() {

    @TypeConverter
    fun fromStringList(list: List<String>): String {
        return list.joinToString(separator = ",")
    }

    @TypeConverter
    fun toStringList(string: String): List<String> {
        return if (string.isBlank()) {
            emptyList()
        } else {
            string.split(",")
        }
    }
}