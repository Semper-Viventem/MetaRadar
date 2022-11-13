package f.cking.software

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