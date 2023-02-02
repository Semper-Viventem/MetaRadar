package f.cking.software

import android.content.Context
import android.util.TypedValue
import java.security.MessageDigest
import java.time.*
import java.time.format.DateTimeFormatter
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

fun Long.toLocalDate(timeZone: ZoneId = ZoneId.of("GMT")) = Instant.ofEpochMilli(this).atZone(timeZone).toLocalDate()
fun Long.toLocalTime(timeZone: ZoneId = ZoneId.of("GMT")) = Instant.ofEpochMilli(this).atZone(timeZone).toLocalTime()
fun timeFromDateTime(date: LocalDate, time: LocalTime): Long =
    LocalDateTime.of(date, time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

fun Long.dateTimeStringFormat(format: String): String {
    return LocalDateTime.of(toLocalDate(ZoneId.systemDefault()), toLocalTime(ZoneId.systemDefault()))
        .format(DateTimeFormatter.ofPattern(format))
}

fun LocalTime.toMilliseconds() = (hour * 60L * 60L * 1000L) + (minute * 60L * 1000L)

fun concatTwoBytes(firstByte: Byte, secondByte: Byte): Int {
    return (firstByte.toUByte().toInt() shl 8) or secondByte.toUByte().toInt()
}

val String.sha256: ByteArray
    get() {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = this.toByteArray()
        digest.update(bytes, 0, bytes.size)
        return digest.digest()
    }

object SHA256 {
    private val digest = MessageDigest.getInstance("SHA-256")

    fun fromString(string: String): ByteArray {
        val bytes = string.toByteArray()
        digest.update(bytes, 0, bytes.size)
        return digest.digest().apply {
            digest.reset()
        }
    }

    fun fromStringAirdrop(string: String): Int {
        return fromString(string).let { concatTwoBytes(it[0], it[1]) }
    }
}

fun Context.dpToPx(value: Float): Int =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics).toInt()