package com.tridivroy.streamly.presentation.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tridivroy.streamly.core.theme.LogoSizeSplash
import com.tridivroy.streamly.core.theme.SageMint
import com.tridivroy.streamly.core.theme.SlateCharcoal
import com.tridivroy.streamly.core.theme.StreamlyLogoTile
import com.tridivroy.streamly.core.theme.StreamlyTheme
import com.tridivroy.streamly.core.theme.StreamlyType
import com.tridivroy.streamly.core.theme.TextMuted
import com.tridivroy.streamly.core.theme.TextPrimary
import com.tridivroy.streamly.core.theme.logoCornerRadius
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The Compose half of the launch sequence. The Android 12 SplashScreen API (see
 * `Theme.Streamly.Starting`) holds the static bolt on the Slate Charcoal ground; this picks up from
 * that same frame and plays the brand animation out, then fades.
 *
 * Timings are the handoff's, verbatim: the tile scales 0.82 → 1 over 700ms, the bolt strikes in from
 * (5, −14) at 0.7 after a 500ms delay, a 2dp mint ring expands and fades at 620ms, the wordmark
 * rises 8dp at 900ms, and the whole thing fades out at 2.1s.
 *
 * [onFinished] fires once, whether the animation completed or the user tapped to skip.
 */
@Composable
fun StreamlySplash(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val glowAlpha = remember { Animatable(0f) }
    val tileScale = remember { Animatable(START_TILE_SCALE) }
    val tileAlpha = remember { Animatable(0f) }
    val boltProgress = remember { Animatable(0f) }
    val ringProgress = remember { Animatable(0f) }
    val wordmarkProgress = remember { Animatable(0f) }
    val exitAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        launch { glowAlpha.animateTo(1f, tween(durationMillis = 1200, easing = LinearEasing)) }
        launch { tileAlpha.animateTo(1f, tween(durationMillis = 420, easing = LinearEasing)) }
        launch { tileScale.animateTo(1f, tween(durationMillis = 700, easing = BrandEasing)) }
        launch {
            delay(500)
            boltProgress.animateTo(1f, tween(durationMillis = 500, easing = StrikeEasing))
        }
        launch {
            delay(620)
            ringProgress.animateTo(1f, tween(durationMillis = 700, easing = LinearEasing))
        }
        launch {
            delay(900)
            wordmarkProgress.animateTo(1f, tween(durationMillis = 600, easing = LinearEasing))
        }
        delay(EXIT_DELAY_MS)
        exitAlpha.animateTo(0f, tween(durationMillis = 450, easing = LinearEasing))
        onFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .alpha(exitAlpha.value)
            .background(SlateCharcoal),
        contentAlignment = Alignment.Center,
    ) {
        // 260dp radial mint glow at 16%, behind everything.
        Box(
            Modifier
                .size(260.dp)
                .alpha(glowAlpha.value)
                .background(
                    Brush.radialGradient(
                        colors = listOf(SageMint.copy(alpha = 0.16f), SageMint.copy(alpha = 0f)),
                    ),
                ),
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(Modifier.size(LogoSizeSplash), contentAlignment = Alignment.Center) {
                // scale and alpha go to the composable, not to Modifier.scale/Modifier.alpha: those
                // would scale a rasterised layer and soften the mark while it animates.
                StreamlyLogoTile(
                    boltOffset = Offset(
                        x = BOLT_FROM_X * (1f - boltProgress.value),
                        y = BOLT_FROM_Y * (1f - boltProgress.value),
                    ),
                    boltScale = START_BOLT_SCALE + (1f - START_BOLT_SCALE) * boltProgress.value,
                    // The bolt is opaque for the last third of its flight, as `boltStrike` reaches 1 at 70%.
                    boltAlpha = (boltProgress.value / 0.7f).coerceAtMost(1f),
                    scale = tileScale.value,
                    alpha = tileAlpha.value,
                    modifier = Modifier.fillMaxSize(),
                )

                // Mint ring: expands 1 → 1.6 and fades out.
                //
                // The expansion is computed into the rect rather than applied as a layer scale, so the
                // stroke stays exactly 2dp and sharp instead of being magnified to 3.2dp of blur.
                if (ringProgress.value > 0f) {
                    Canvas(Modifier.fillMaxSize()) {
                        val ringScale = 1f + 0.6f * ringProgress.value
                        val radius = logoCornerRadius(LogoSizeSplash).toPx() * ringScale
                        val grown = size * ringScale
                        val inset = Offset((size.width - grown.width) / 2f, (size.height - grown.height) / 2f)
                        drawRoundRect(
                            color = SageMint,
                            alpha = 0.9f * (1f - ringProgress.value),
                            topLeft = inset,
                            size = grown,
                            cornerRadius = CornerRadius(radius, radius),
                            style = Stroke(width = 2.dp.toPx()),
                        )
                    }
                }
            }

            Spacer(Modifier.height(22.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    alpha = wordmarkProgress.value
                    translationY = 8.dp.toPx() * (1f - wordmarkProgress.value)
                },
            ) {
                Text(text = "streamly", style = StreamlyType.WordmarkLarge, color = TextPrimary)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "INSTANT · ANYWHERE · OFFLINE",
                    style = StreamlyType.Tagline,
                    color = TextMuted,
                )
            }
        }
    }
}

/** `cubic-bezier(.2,.8,.2,1)` — the tile settling. */
private val BrandEasing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

/** `cubic-bezier(.2,.9,.2,1.2)` — the bolt's slight overshoot. */
private val StrikeEasing = CubicBezierEasing(0.2f, 0.9f, 0.2f, 1.2f)

private const val START_TILE_SCALE = 0.82f
private const val START_BOLT_SCALE = 0.7f

/** The bolt's starting offset, in the logo's 48-unit viewport (prototype: `translate(5px, -14px)`). */
private const val BOLT_FROM_X = 5f
private const val BOLT_FROM_Y = -14f

private const val EXIT_DELAY_MS = 2_100L

@Preview(showBackground = true, backgroundColor = 0xFF121820)
@Composable
private fun StreamlySplashPreview() {
    StreamlyTheme(darkTheme = true) {
        StreamlySplash(onFinished = {})
    }
}
