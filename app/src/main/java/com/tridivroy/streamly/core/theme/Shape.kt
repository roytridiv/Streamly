package com.tridivroy.streamly.core.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shapes from the design handoff. Material's [Shapes] carries the generic scale; the named values
 * below are the ones the handoff specifies per component.
 */
val StreamlyShapes = Shapes(
    extraSmall = RoundedCornerShape(7.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp),
)

object StreamlyShape {

    /** Cards and 16:9 thumbnails. */
    val Card = RoundedCornerShape(16.dp)

    /** The player's floating viewport. */
    val Viewport = RoundedCornerShape(12.dp)

    /** Small thumbnails (Up Next rows, Downloads rows, mini-player). */
    val SmallThumbnail = RoundedCornerShape(12.dp)

    /** Action-bar buttons and the tab container. */
    val ActionButton = RoundedCornerShape(14.dp)

    /** Duration badges on thumbnails. */
    val DurationBadge = RoundedCornerShape(7.dp)

    /** Icon buttons in the top bar / player header. */
    val IconButton = RoundedCornerShape(12.dp)

    /** Frosted squares in the Shorts right rail. */
    val FrostedTile = RoundedCornerShape(18.dp)

    /**
     * The brand's signature shape — one corner cut short so the pill reads as a forward-leaning
     * tag rather than a capsule. Used for category chips, Subscribe/Follow, the nav indicator,
     * the Offline Ready tag and Key Moment chips.
     *
     * The handoff specifies 17/6/17/17 at a 34dp height; [asymmetricPill] scales those radii for
     * elements of other heights so the proportion holds.
     */
    val AsymmetricPill = asymmetricPill(34.dp)

    /** [AsymmetricPill] scaled to [height], keeping the handoff's 17:6 ratio at 34dp. */
    fun asymmetricPill(height: Dp): RoundedCornerShape {
        val large = height / 2f
        val small = large * (6f / 17f)
        return RoundedCornerShape(
            topStart = large,
            topEnd = small,
            bottomEnd = large,
            bottomStart = large,
        )
    }
}
