package com.tridivroy.streamly.core.theme

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/*
 * The v2-bolt brand mark, drawn in Compose.
 *
 * `res/drawable/ic_streamly_logo.xml` is the same artwork and is what the launcher and the Android 12
 * splash use. The Compose version exists because the splash animates the play mark and the bolt
 * independently — the bolt strikes in after the tile settles — which a single vector drawable cannot
 * do. Both are generated from the same path data, so they stay identical.
 */

/** Path data from `logo/v2-bolt/`, in the artwork's own 48×48 viewport. */
private const val PLAY_PATH =
    "M19.1 11.9C17.4 10.9 15.3 12.1 15.3 14.1V33.9C15.3 35.9 17.4 37.1 19.1 36.1L36.5 25.9C38.2 24.9 38.2 23.1 36.5 22.1Z"

private const val BOLT_PATH =
    "M28.5 11.5C25.5 15.5 21.5 20.5 18.5 26H25L21.5 37.5C25 32 28.5 27 32 22.5H25.5C26.5 18.5 28.5 14.5 30.5 11.5Z"

private const val VIEWPORT = 48f

/** The bolt's pivot in the 48×48 viewport, as the prototype's `transform-origin: 25px 24px`. */
private val BOLT_PIVOT = Offset(25f, 24f)

private fun parse(pathData: String): Path = PathParser().parsePathString(pathData).toPath()

/**
 * The full logo tile: rounded square, mint play mark, and the bolt cut out of it.
 *
 * [boltOffset] and [boltScale] are in viewport units and exist for the splash animation; at their
 * defaults this renders the static mark. [outlined] draws the play mark as a stroke instead of a
 * fill, for the Downloads empty state.
 *
 * [scale] and [alpha] are taken as parameters rather than left to `Modifier.scale`/`Modifier.alpha`
 * on purpose: those go through `graphicsLayer`, which transforms a composited layer, so an animated
 * scale resamples an already-rasterised bitmap and the mark goes soft mid-animation. Applied here
 * they are part of the draw, so Skia re-tessellates the paths at the final size every frame.
 */
@Composable
fun StreamlyLogoTile(
    modifier: Modifier = Modifier,
    tileColor: Color = SoftCharcoal,
    markColor: Color = SageMint,
    outlineColor: Color = StorageOther,
    boltOffset: Offset = Offset.Zero,
    boltScale: Float = 1f,
    boltAlpha: Float = 1f,
    outlined: Boolean = false,
    showTile: Boolean = true,
    scale: Float = 1f,
    alpha: Float = 1f,
) {
    val playPath = remember { parse(PLAY_PATH) }
    val boltPath = remember { parse(BOLT_PATH) }

    Canvas(modifier) {
        val unit = size.minDimension / VIEWPORT
        // Outer scale is the animated one, around the centre; inner scale maps the artwork's 48-unit
        // viewport onto the measured size. Both are draw transforms, so geometry stays exact.
        scale(scale = scale, pivot = center) {
            scale(scale = unit, pivot = Offset.Zero) {
                if (showTile) {
                    drawRoundRect(
                        color = tileColor,
                        alpha = alpha,
                        size = Size(VIEWPORT, VIEWPORT),
                        cornerRadius = CornerRadius(13f, 13f),
                    )
                }
                if (outlined) {
                    drawPath(playPath, color = outlineColor, alpha = alpha, style = Stroke(width = 1.6f))
                } else {
                    drawPath(playPath, color = markColor, alpha = alpha)
                }
                drawBolt(
                    path = boltPath,
                    // The bolt is a cut through the play mark, so it is painted in the tile's colour —
                    // except when outlined, where it is the only solid shape left.
                    color = if (outlined) markColor else tileColor,
                    offset = boltOffset,
                    scale = boltScale,
                    alpha = boltAlpha * alpha,
                )
            }
        }
    }
}

/** Just the bolt glyph — the Shorts tab icon and the Shorts header. */
@Composable
fun StreamlyBoltGlyph(
    modifier: Modifier = Modifier,
    color: Color = SageMint,
) {
    val boltPath = remember { parse(BOLT_PATH) }
    Canvas(modifier) {
        // The glyph alone occupies roughly x 18.5–32, y 11.5–37.5 of the 48 viewport; this crops to it.
        val glyphSize = 27f
        val unit = size.minDimension / glyphSize
        scale(scale = unit, pivot = Offset.Zero) {
            translate(left = -17f, top = -10.5f) {
                drawPath(boltPath, color = color)
            }
        }
    }
}

private fun DrawScope.drawBolt(
    path: Path,
    color: Color,
    offset: Offset,
    scale: Float,
    alpha: Float,
) {
    translate(left = offset.x, top = offset.y) {
        scale(scale = scale, pivot = BOLT_PIVOT) {
            drawPath(path, color = color, alpha = alpha)
        }
    }
}

/** The tile's corner radius at a given rendered size, for glows and rings drawn around it. */
fun logoCornerRadius(tileSize: androidx.compose.ui.unit.Dp) = tileSize * (13f / VIEWPORT)

/** Default tile size in the Home top bar. */
val LogoSizeTopBar = 30.dp

/** Default tile size on the splash. */
val LogoSizeSplash = 104.dp
