package com.tridivroy.streamly.core.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/*
 * Type from the design handoff: Inter for everything, JetBrains Mono for timestamps and durations.
 *
 * The handoff shipped no font files, so these resolve to the platform sans and mono faces. Inter is
 * close enough to the stock Android sans (Roboto) at these sizes that the layout holds; dropping
 * Inter and JetBrains Mono TTFs into `res/font` and pointing [InterFamily] / [MonoFamily] at them is
 * the only change needed to match the prototype exactly.
 *
 * Handoff rule: headings are Medium (500) and never bolder. SemiBold (600) is for badges and the
 * wordmark only.
 */
private val InterFamily = FontFamily.SansSerif
private val MonoFamily = FontFamily.Monospace

val Typography = Typography(
    /** Screen title — "Downloads". Handoff: 24, Medium, −2% tracking. */
    headlineSmall = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 29.sp,
        letterSpacing = (-0.48).sp,
    ),
    /** Player title. Handoff: 19, Medium, −1% tracking, line-height 1.3. */
    titleLarge = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 19.sp,
        lineHeight = 25.sp,
        letterSpacing = (-0.19).sp,
    ),
    /** Card title. Handoff: 15, Medium, line-height 1.3. */
    titleMedium = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 19.5.sp,
    ),
    /** Secondary titles — Up Next and Downloads rows. Handoff: 13–13.5, Medium. */
    titleSmall = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.5.sp,
        lineHeight = 17.5.sp,
    ),
    /** Body — descriptions, captions. Handoff: 13.5, line-height 1.55. */
    bodyLarge = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 21.sp,
    ),
    /** Meta — "Channel · 1.2M views · 3 days ago". Handoff: 12.5. */
    bodySmall = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.5.sp,
        lineHeight = 17.sp,
    ),
    /** Buttons and chips. Handoff: 13, Medium. */
    labelLarge = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 17.sp,
    ),
    /** Nav labels and small controls. Handoff: 11–12, Medium. */
    labelMedium = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.5.sp,
        lineHeight = 15.sp,
    ),
    /** Badges. Handoff: 10–11, SemiBold. */
    labelSmall = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
    ),
)

/** Styles outside the Material scale: the mono family and the wordmark. */
object StreamlyType {

    /** Duration badges on thumbnails. Handoff: mono, 11, SemiBold. */
    val Duration = TextStyle(
        fontFamily = MonoFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 11.sp,
    )

    /** Scrubber and player timestamps. Handoff: mono, 10.5, Medium. */
    val Timestamp = TextStyle(
        fontFamily = MonoFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.5.sp,
        lineHeight = 13.sp,
    )

    /** Tracked-out mono labels — "NOW PLAYING", the splash tagline. */
    val Eyebrow = TextStyle(
        fontFamily = MonoFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 13.sp,
        letterSpacing = 1.1.sp,
    )

    /** Storage figures — "40.8 / 64 GB". Handoff: mono, 12, Medium. */
    val MonoValue = TextStyle(
        fontFamily = MonoFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 15.sp,
    )

    /** "streamly" in the Home top bar. Handoff: 21, SemiBold, −3% tracking. */
    val Wordmark = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 21.sp,
        lineHeight = 25.sp,
        letterSpacing = (-0.63).sp,
    )

    /** "streamly" on the splash. Handoff: 26, SemiBold, −3% tracking. */
    val WordmarkLarge = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.78).sp,
    )

    /** The splash tagline. Handoff: mono, 10.5, 14% tracking. */
    val Tagline = TextStyle(
        fontFamily = MonoFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.5.sp,
        lineHeight = 13.sp,
        letterSpacing = 1.47.sp,
    )
}
