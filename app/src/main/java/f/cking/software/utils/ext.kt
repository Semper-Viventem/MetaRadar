package f.cking.software

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.TypedValue
import android.view.Display
import android.view.WindowManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter


fun Long.getTimePeriodStr(context: Context): String {
    val sec = this / (1000)
    val min = this / (1000 * 60)
    val hours = this / (1000 * 60 * 60)
    val days = this / (1000 * 60 * 60 * 24)

    return when {
        days >= 1L -> context.resources.getQuantityString(R.plurals.day, days.toInt(), days.toInt())
        hours >= 1L -> context.resources.getQuantityString(R.plurals.hour, hours.toInt(), hours.toInt())
        min >= 1L -> context.resources.getQuantityString(R.plurals.min, min.toInt(), min.toInt())
        else -> context.resources.getQuantityString(R.plurals.sec, sec.toInt(), sec.toInt())
    }
}

fun Context.frameRate(): Float {
    val display: Display = ContextCompat.getSystemService(this, WindowManager::class.java)!!.defaultDisplay
    return display.getRefreshRate()
}

@ExperimentalUnsignedTypes // just to make it clear that the experimental unsigned types are used
fun ByteArray.toHexUByteString() = asUByteArray().joinToString("") { it.toString(16).padStart(2, '0') }
fun ByteArray.toHexString() = joinToString("") { it.toHexString() }
fun Byte.toHexString() = "%02x".format(this)
fun Int.toHexString() = "%04x".format(this)

fun Long.toLocalDate(timeZone: ZoneId = ZoneId.systemDefault()) = Instant.ofEpochMilli(this).atZone(timeZone).toLocalDate()
fun Long.toLocalTime(timeZone: ZoneId = ZoneId.systemDefault()) = Instant.ofEpochMilli(this).atZone(timeZone).toLocalTime()
fun timeFromDateTime(date: LocalDate, time: LocalTime): Long =
    LocalDateTime.of(date, time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

fun Long.dateTimeStringFormat(format: String, timeZone: ZoneId = ZoneId.systemDefault()): String {
    return LocalDateTime.of(toLocalDate(timeZone), toLocalTime(timeZone))
        .format(DateTimeFormatter.ofPattern(format))
}

fun LocalTime.dateTimeFormat(format: String): String {
    return format(DateTimeFormatter.ofPattern(format))
}

fun LocalDate.dateTimeFormat(format: String): String {
    return format(DateTimeFormatter.ofPattern(format))
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

fun Context.openUrl(url: String) {
    val webpage: Uri = Uri.parse(url)
    val intent = Intent(Intent.ACTION_VIEW, webpage)
    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(this, getString(R.string.cannot_open_the_url), Toast.LENGTH_SHORT).show()
    }
}

fun Context.dpToPx(value: Float): Int =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics).toInt()

fun Context.pxToDp(value: Float): Float = value / resources.displayMetrics.density

fun <T> List<T>.splitToBatches(batchSize: Int): List<List<T>> {
    if (size <= batchSize) return listOf(this)

    val result = mutableListOf<List<T>>()
    var fromIndex = 0

    do {
        val rangeEnd = fromIndex + (batchSize - 1)
        val toIndex = if (rangeEnd <= lastIndex) rangeEnd else lastIndex
        result.add(this.subList(fromIndex, toIndex))
        fromIndex = toIndex + 1
    } while (fromIndex < lastIndex)

    return result
}