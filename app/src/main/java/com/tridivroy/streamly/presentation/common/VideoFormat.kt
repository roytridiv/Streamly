package com.tridivroy.streamly.presentation.common

import kotlin.math.abs
import kotlin.math.roundToLong

/*
 * Formatting helpers.
 *
 * Padding and rounding are done by hand below rather than with `String.format`, so the output never
 * depends on the device locale.
 */

/** Formats seconds as `m:ss`, or `h:mm:ss` for an hour or longer. */
fun formatDuration(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "$hours:${minutes.pad2()}:${seconds.pad2()}"
    } else {
        "$minutes:${seconds.pad2()}"
    }
}

/**
 * Compact counts as the design shows them: `1.2M`, `640K`, `412`. A trailing `.0` is dropped, so
 * 2,000,000 reads "2M" rather than "2.0M".
 */
fun formatCount(count: Long): String = when {
    abs(count) >= 1_000_000 -> (count / 1_000_000.0).toFixed(1).removeSuffix(".0") + "M"
    abs(count) >= 1_000 -> (count / 1_000.0).toFixed(1).removeSuffix(".0") + "K"
    else -> count.toString()
}

/**
 * Upload age as "3 days ago". [epochSeconds] `null` — an API that did not say — yields `null` so
 * callers can leave the slot out rather than print a wrong "just now".
 */
fun formatRelativeAge(epochSeconds: Long?, nowSeconds: Long = System.currentTimeMillis() / 1000): String? {
    if (epochSeconds == null) return null
    val elapsed = nowSeconds - epochSeconds
    if (elapsed < 0) return null
    return when {
        elapsed < 60 -> "just now"
        elapsed < 3_600 -> plural(elapsed / 60, "minute")
        elapsed < 86_400 -> plural(elapsed / 3_600, "hour")
        elapsed < 2_592_000 -> plural(elapsed / 86_400, "day")
        elapsed < 31_536_000 -> plural(elapsed / 2_592_000, "month")
        else -> plural(elapsed / 31_536_000, "year")
    }
}

private fun plural(value: Long, unit: String): String =
    "$value $unit${if (value == 1L) "" else "s"} ago"

/** Bytes as "612 MB" / "1.8 GB", matching the mono size labels in Downloads. */
fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000_000 -> "${(bytes / 1_000_000_000.0).toFixed(1)} GB"
    bytes >= 1_000_000 -> "${(bytes / 1_000_000.0).toFixed(0)} MB"
    bytes >= 1_000 -> "${(bytes / 1_000.0).toFixed(0)} KB"
    else -> "$bytes B"
}

/** Two-digit, zero-padded — the `%02d` these strings used to rely on. */
private fun Long.pad2(): String = toString().padStart(2, '0')

/**
 * Rounds to [decimals] places and always prints exactly that many, which is what `%.1f` did.
 * Only 0 and 1 decimals are needed here, so this stays deliberately small.
 */
private fun Double.toFixed(decimals: Int): String {
    if (decimals == 0) return roundToLong().toString()
    val scale = 10.0
    val scaled = (this * scale).roundToLong()
    val whole = scaled / 10
    val fraction = (if (scaled < 0) -scaled else scaled) % 10
    return "$whole.$fraction"
}
