package com.tridivroy.streamly.core.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * The icon set from the design handoff (Phosphor regular), built from the prototype's own path data
 * so the strokes match it exactly. Defining them here also keeps the large material-icons-extended
 * artifact out of the build.
 *
 * Most are strokes — [stroked] — at the prototype's 1.7–1.8 width with round caps and joins.
 */
object StreamlyIcons {

    /** House outline. */
    val Home: ImageVector by lazy {
        stroked("Home", "M4 10.5 12 4l8 6.5V20h-5v-6H9v6H4z")
    }

    /** The logo's bolt glyph, as a tab icon. Filled — it is a brand mark, not a UI stroke. */
    val Bolt: ImageVector by lazy {
        filled(
            name = "Bolt",
            pathData = "M14.5 3C12.5 5.8 9.5 9.5 7.5 13H12L9.5 21C12 17.2 14.6 13.8 17 11H12.5C13.2 8.3 14.5 5.5 16 3Z",
        )
    }

    /** Arrow into a tray. */
    val Download: ImageVector by lazy {
        stroked("Download", "M12 4v11M7.5 10.5 12 15l4.5-4.5M5 20h14")
    }

    val Search: ImageVector by lazy {
        stroked("Search", "M17.5 11a6.5 6.5 0 1 1-13 0 6.5 6.5 0 0 1 13 0Zm2.5 9-4.2-4.2")
    }

    val Bell: ImageVector by lazy {
        stroked("Bell", "M6 16v-5a6 6 0 0 1 12 0v5l1.5 2h-15zM10 20.5h4")
    }

    val Heart: ImageVector by lazy {
        stroked("Heart", HEART_PATH)
    }

    val HeartFilled: ImageVector by lazy {
        filled("HeartFilled", HEART_PATH)
    }

    val Comment: ImageVector by lazy {
        stroked("Comment", "M5 19V6.5A1.5 1.5 0 0 1 6.5 5h11A1.5 1.5 0 0 1 19 6.5v8a1.5 1.5 0 0 1-1.5 1.5H8.5z")
    }

    val Share: ImageVector by lazy {
        stroked("Share", "m14 5 6 6-6 6M20 11h-8.5A6.5 6.5 0 0 0 5 17.5V19")
    }

    val Save: ImageVector by lazy {
        stroked("Save", SAVE_PATH)
    }

    val SaveFilled: ImageVector by lazy {
        filled("SaveFilled", SAVE_PATH)
    }

    val Trash: ImageVector by lazy {
        stroked("Trash", "M5 7h14M10 7V5h4v2M7 7l1 12h8l1-12")
    }

    val Check: ImageVector by lazy {
        stroked("Check", "m5 12 5 5 9-10", strokeWidth = 2.4f)
    }

    val ChevronDown: ImageVector by lazy {
        stroked("ChevronDown", "m6 9 6 6 6-6", strokeWidth = 2f)
    }

    val ChevronUp: ImageVector by lazy {
        stroked("ChevronUp", "m6 15 6-6 6 6", strokeWidth = 2f)
    }

    /** Overflow menu — three dots. */
    val MoreHorizontal: ImageVector by lazy {
        filled(
            name = "MoreHorizontal",
            pathData = "M6.7 12a1.7 1.7 0 1 1-3.4 0 1.7 1.7 0 0 1 3.4 0Z" +
                "M13.7 12a1.7 1.7 0 1 1-3.4 0 1.7 1.7 0 0 1 3.4 0Z" +
                "M20.7 12a1.7 1.7 0 1 1-3.4 0 1.7 1.7 0 0 1 3.4 0Z",
        )
    }

    /** Up Next list layout. */
    val LayoutRows: ImageVector by lazy {
        stroked("LayoutRows", "M4 6h16M4 12h16M4 18h16", strokeWidth = 1.8f)
    }

    /** Up Next carousel layout. */
    val LayoutColumns: ImageVector by lazy {
        stroked("LayoutColumns", "M4 6h6v12H4zM14 6h6v12h-6z", strokeWidth = 1.8f)
    }

    val Play: ImageVector by lazy {
        filled("Play", "M8 5.5v13a.8.8 0 0 0 1.2.7l10.3-6.5a.8.8 0 0 0 0-1.4L9.2 4.8A.8.8 0 0 0 8 5.5z")
    }

    val Pause: ImageVector by lazy {
        filled(
            name = "Pause",
            pathData = "M7.7 5h1.2a1.2 1.2 0 0 1 1.2 1.2v11.6A1.2 1.2 0 0 1 8.9 19H7.7a1.2 1.2 0 0 1-1.2-1.2V6.2A1.2 1.2 0 0 1 7.7 5Z" +
                "M15.1 5h1.2a1.2 1.2 0 0 1 1.2 1.2v11.6a1.2 1.2 0 0 1-1.2 1.2h-1.2a1.2 1.2 0 0 1-1.2-1.2V6.2A1.2 1.2 0 0 1 15.1 5Z",
        )
    }

    /** The Shorts sound row. */
    val MusicNote: ImageVector by lazy {
        stroked("MusicNote", "M9 18V5l11-2v13M9 18a2.5 2.5 0 1 1-5 0 2.5 2.5 0 0 1 5 0Zm11-2a2.5 2.5 0 1 1-5 0 2.5 2.5 0 0 1 5 0Z")
    }

    val Close: ImageVector by lazy {
        stroked("Close", "M6 6l12 12M18 6 6 18", strokeWidth = 1.8f)
    }

    /** Four corner brackets pointing out — enter fullscreen. */
    val Fullscreen: ImageVector by lazy {
        stroked(
            name = "Fullscreen",
            pathData = "M4 9V5a1 1 0 0 1 1-1h4M15 4h4a1 1 0 0 1 1 1v4M20 15v4a1 1 0 0 1-1 1h-4M9 20H5a1 1 0 0 1-1-1v-4",
            strokeWidth = 1.8f,
        )
    }

    /** The same brackets pointing in — leave fullscreen. */
    val FullscreenExit: ImageVector by lazy {
        stroked(
            name = "FullscreenExit",
            pathData = "M9 4v3a2 2 0 0 1-2 2H4M15 4v3a2 2 0 0 0 2 2h3M20 15h-3a2 2 0 0 0-2 2v3M4 15h3a2 2 0 0 1 2 2v3",
            strokeWidth = 1.8f,
        )
    }

    val Settings: ImageVector by lazy {
        stroked(
            name = "Settings",
            pathData = "M14.5 12a2.5 2.5 0 1 1-5 0 2.5 2.5 0 0 1 5 0Z" +
                "M12 3.5l1 2.2 2.4-.5 1 2.2 2.1 1.2-.6 2.4.6 2.4-2.1 1.2-1 2.2-2.4-.5-1 2.2-1-2.2-2.4.5-1-2.2-2.1-1.2.6-2.4-.6-2.4 2.1-1.2 1-2.2 2.4.5z",
        )
    }

    val Refresh: ImageVector by lazy {
        stroked("Refresh", "M19 12a7 7 0 1 1-2.1-5M19 4v4h-4", strokeWidth = 1.8f)
    }

    private const val HEART_PATH =
        "M12 20s-7.5-4.6-7.5-10.2A4.2 4.2 0 0 1 12 7.2a4.2 4.2 0 0 1 7.5 2.6C19.5 15.4 12 20 12 20z"

    private const val SAVE_PATH = "M7 4h10v16l-5-3.6L7 20z"

    /** Builds a 24dp stroke icon from SVG path data. */
    private fun stroked(name: String, pathData: String, strokeWidth: Float = 1.7f): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                pathData = PathParser().parsePathString(pathData).toNodes(),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = strokeWidth,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }.build()

    /** Builds a 24dp filled icon from SVG path data. */
    private fun filled(name: String, pathData: String): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                pathData = PathParser().parsePathString(pathData).toNodes(),
                fill = SolidColor(Color.Black),
            )
        }.build()
}
