package f.cking.software

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.util.Base64
import android.util.TypedValue
import android.view.Display
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.osmdroid.api.IGeoPoint
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.Projection
import timber.log.Timber
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.concurrent.atomic.AtomicInteger
import java.util.regex.PatternSyntaxException
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract


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

fun Long.dateTimeStringFormatLocalized(formatStyle: FormatStyle = FormatStyle.SHORT, timeZone: ZoneId = ZoneId.systemDefault()): String {
    return LocalDateTime.of(toLocalDate(timeZone), toLocalTime(timeZone))
        .format(DateTimeFormatter.ofLocalizedDateTime(formatStyle))
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
    val webpage: Uri = url.toUri()
    val intent = Intent(Intent.ACTION_VIEW, webpage)
    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(this, getString(R.string.cannot_open_the_url), Toast.LENGTH_SHORT).show()
    }
}

fun Context.dpToPx(value: Float): Int =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics).toInt()

@Composable
fun Float.toPx(): Int {
    return LocalContext.current.dpToPx(this)
}

fun Context.pxToDp(value: Float): Float = value / resources.displayMetrics.density

fun <T> List<T>.splitToBatches(batchSize: Int): List<List<T>> {
    if (size <= batchSize) return listOf(this)

    val result = mutableListOf<List<T>>()
    var fromIndex = 0

    do {
        val rangeEnd = fromIndex + (batchSize - 1)
        val toIndex = if (rangeEnd <= lastIndex) rangeEnd else lastIndex
        result.add(this.subList(fromIndex, toIndex + 1))
        fromIndex = toIndex + 1
    } while (fromIndex <= lastIndex)

    return result
}

fun <T> List<T>.splitToBatchesEqual(batchCount: Int): List<List<T>> {
    val batches = Array(batchCount) { mutableListOf<T>() }

    for (i in 0..lastIndex) {
        val batchIndex = i % batchCount
        batches[batchIndex].add(this[i])
    }

    return batches.toList()
}

fun Context.isDarkModeOn(): Boolean {
    val nightModeFlags = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
    return nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES
}

fun String.checkRegexSafe(pattern: String): Boolean {
    return try {
        this.contains(pattern.toRegex())
    } catch (e: PatternSyntaxException) {
        false
    } catch (e: Throwable) {
         Timber.e(e, "Unexpected regex failure")
        false
    }
}

@OptIn(ExperimentalContracts::class)
inline fun <T> T.letIf(condition: () -> Boolean, block: (T) -> T): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return if (condition.invoke()) block(this) else this
}

@OptIn(ExperimentalContracts::class)
inline fun <T> T.letIf(condition: Boolean, block: (T) -> T): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return if (condition) block(this) else this
}

fun <T> Flow<T>.collectAsState(scope: CoroutineScope, initialValue: T): State<T> {
    val state = mutableStateOf(initialValue)

    onEach { state.value = it }
        .launchIn(scope)

    return state
}

fun ByteArray.toBase64(): String {
    return Base64.encodeToString(this, Base64.NO_WRAP)
}

fun String.fromBase64(): ByteArray {
    return Base64.decode(this, Base64.NO_WRAP)
}

suspend fun <T, R> List<T>.mapParallel(transform: suspend (T) -> R): List<R> {
    return coroutineScope {
        map { async { transform(it) } }.awaitAll()
    }
}

fun extract16BitUuid(fullUuid: String): String? {
    val regex = Regex("^0000([0-9a-fA-F]{4})-0000-1000-8000-00805f9b34fb$")
    return regex.find(fullUuid)?.groupValues?.get(1)
}

operator fun AtomicInteger.getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>): Int =
    this.get()

operator fun AtomicInteger.setValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>, value: Int) =
    this.set(value)

fun IGeoPoint.toLocation(): Location {
    return Location("").apply {
        latitude = this@toLocation.latitude
        longitude = this@toLocation.longitude
    }
}

fun Projection.topLeft(): IGeoPoint {
    return GeoPoint(boundingBox.latNorth, boundingBox.lonWest)
}

fun Projection.bottomRight(): IGeoPoint {
    return GeoPoint(boundingBox.latSouth, boundingBox.lonEast)
}