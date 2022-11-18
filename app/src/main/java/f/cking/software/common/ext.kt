package f.cking.software

import java.time.*
import java.util.*

fun getTimePeriodStr(timeMs: Long): String {
    val sec = timeMs / (1000L)
    val min = timeMs / (1000L * 60L)
    val hours = timeMs / (1000L * 60L * 60L)
    val days = timeMs / (1000L * 60L * 60L * 24L)

    return when {
        days > 1L -> "$days days"
        days == 1L -> "$days day"
        hours > 1L -> "$hours hours"
        hours == 1L -> "$hours hour"
        min > 0 -> "$min min"
        else -> "$sec sec"
    }
}

@ExperimentalUnsignedTypes // just to make it clear that the experimental unsigned types are used
fun ByteArray.toHexUByteString() = asUByteArray().joinToString("") { it.toString(16).padStart(2, '0') }
fun ByteArray.toHexString() = joinToString("") { it.toHexString() }
fun Byte.toHexString() = "%02x".format(this)
fun Int.toHexString() = "%04x".format(this)
fun <T> Optional<T>.orNull(): T? = if (isPresent) get() else null

fun Long.toLocalDate() = Instant.ofEpochMilli(this).atZone(ZoneId.of("GMT")).toLocalDate()
fun Long.toLocalTime() = Instant.ofEpochMilli(this).atZone(ZoneId.of("GMT")).toLocalTime()
fun timeFromDateTime(date: LocalDate, time: LocalTime): Long =
    LocalDateTime.of(date, time).atZone(ZoneId.of("GMT")).toInstant().toEpochMilli()

fun LocalTime.toMilliseconds() = (hour * 60L * 60L * 1000L) + (minute * 60L * 1000L)

fun concatTwoBytes(firstByte: Byte, secondByte: Byte): Int {
    return (firstByte.toUByte().toInt() shl 8) or secondByte.toUByte().toInt()
}