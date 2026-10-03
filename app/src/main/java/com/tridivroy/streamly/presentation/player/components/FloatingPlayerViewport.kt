package com.tridivroy.streamly.presentation.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.tridivroy.streamly.R
import com.tridivroy.streamly.core.theme.SageMint
import com.tridivroy.streamly.core.theme.StreamlyBrand
import com.tridivroy.streamly.core.theme.StreamlyIcons
import com.tridivroy.streamly.core.theme.StreamlyLogoTile
import com.tridivroy.streamly.core.theme.StreamlyShape
import com.tridivroy.streamly.core.theme.StreamlyType
import com.tridivroy.streamly.domain.model.Chapter
import com.tridivroy.streamly.presentation.common.PlaybackProgress
import com.tridivroy.streamly.presentation.common.formatDuration
import com.tridivroy.streamly.presentation.common.safeOverlayPadding
import kotlinx.coroutines.delay

/**
 * The player's floating viewport: a 16:9 pane inset from the screen edges, clipped to 12dp, lit by a
 * mint ambient glow, with the existing Media3 [PlayerView] inside it.
 *
 * Everything drawn on top — the watermark, the centre play/pause and the scrubber — is Compose over
 * the video surface, so the Media3 setup is untouched; only [onTogglePlay] and [onSeekTo] reach back
 * into the player.
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun FloatingPlayerViewport(
    player: Player,
    progress: PlaybackProgress,
    chapters: List<Chapter>,
    onTogglePlay: () -> Unit,
    onSeekTo: (positionMs: Long) -> Unit,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier,
    isFullscreen: Boolean = false,
    controls: PlayerControlsState = rememberPlayerControlsState(),
) {
    // Auto-hide: only while playing, and restarted by every interaction. A paused video keeps its
    // controls up — the play button is the way back out, so hiding it would strand the viewer.
    LaunchedEffect(controls.isVisible, controls.interactionCount, progress.isPlaying) {
        if (controls.isVisible && progress.isPlaying) {
            delay(CONTROLS_AUTO_HIDE_MS)
            controls.hide()
        }
    }

    // Pausing brings them back; resuming hands over to the countdown above.
    LaunchedEffect(progress.isPlaying) {
        if (!progress.isPlaying) controls.isVisible = true
    }

    Box(
        modifier = modifier
            // Fullscreen drops the floating treatment: a pane that fills the screen has no edges to
            // round and nothing to cast a glow against, and the border would read as a hairline seam.
            .then(
                if (isFullscreen) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        // Handoff glow: a 1dp mint edge plus a soft ambient drop — no heavy shadow.
                        .shadow(
                            elevation = 20.dp,
                            shape = StreamlyShape.Viewport,
                            ambientColor = StreamlyBrand.GlowAmbient,
                            spotColor = StreamlyBrand.GlowAmbient,
                        )
                        .clip(StreamlyShape.Viewport)
                        .border(1.dp, StreamlyBrand.GlowEdge, StreamlyShape.Viewport)
                },
            )
            .background(StreamlyBrand.Letterbox)
            // A tap anywhere on the surface toggles the controls. The play button and the scrubber
            // consume their own taps, so this only fires on the bare video.
            .pointerInput(Unit) { detectTapGestures { controls.toggle() } },
    ) {
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    // The Compose overlay owns the controls, so Media3's own chrome stays off.
                    useController = false
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                    keepScreenOn = true
                }
            },
            update = { view -> view.player = player },
            onRelease = { view -> view.player = null },
            modifier = Modifier.fillMaxSize(),
        )

        // Faint logo mark, top-right at 55%.
        StreamlyLogoTile(
            showTile = false,
            markColor = StreamlyBrand.OnMedia,
            modifier = Modifier
                .align(Alignment.TopEnd)
                // Fullscreen puts the mark at the real screen edge, where a cutout or the status bar
                // would otherwise clip it.
                .then(fullscreenInsets(isFullscreen, WindowInsetsSides.Top))
                .padding(top = 9.dp, end = 11.dp)
                .size(18.dp)
                .alpha(0.55f),
        )

        // Removed from composition when hidden, not just faded — a 0-alpha overlay would still
        // swallow the taps meant for the surface underneath it.
        AnimatedVisibility(
            visible = controls.isVisible,
            enter = fadeIn(tween(FADE_MS)),
            exit = fadeOut(tween(FADE_MS)),
            modifier = Modifier.align(Alignment.Center),
        ) {
            CentrePlayButton(
                isPlaying = progress.isPlaying,
                onClick = {
                    onTogglePlay()
                    controls.keepVisible()
                },
            )
        }

        AnimatedVisibility(
            visible = controls.isVisible,
            enter = fadeIn(tween(FADE_MS)),
            exit = fadeOut(tween(FADE_MS)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                // Fullscreen hides the bars but not the gesture strip, so the scrubber and the
                // fullscreen button would sit under the home indicator without this.
                .then(fullscreenInsets(isFullscreen, WindowInsetsSides.Bottom)),
        ) {
            ScrubberBar(
                progress = progress,
                chapters = chapters,
                isFullscreen = isFullscreen,
                onSeekTo = { positionMs ->
                    onSeekTo(positionMs)
                    controls.keepVisible()
                },
                // Holds the overlay up mid-drag, so the auto-hide cannot pull the bar from under the finger.
                onScrub = controls::keepVisible,
                onToggleFullscreen = {
                    onToggleFullscreen()
                    controls.keepVisible()
                },
            )
        }
    }
}

/** 54dp frosted circle over the video. */
@Composable
private fun CentrePlayButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(54.dp)
            .clip(CircleShape)
            .background(StreamlyBrand.FrostedSurface)
            .border(1.dp, StreamlyBrand.OnMedia.copy(alpha = 0.25f), CircleShape)
            // clickable, not pointerInput: it carries click semantics, so TalkBack can activate it and
            // it reports real bounds. It still consumes the tap, so the surface toggle below is unaffected.
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isPlaying) StreamlyIcons.Pause else StreamlyIcons.Play,
            contentDescription = stringResource(
                if (isPlaying) R.string.player_pause else R.string.player_play,
            ),
            tint = StreamlyBrand.OnMedia,
            modifier = Modifier.size(22.dp),
        )
    }
}

/**
 * 3dp track with a mint fill, a glow under it, an 11dp thumb, and a tick at each chapter. Tapping or
 * dragging anywhere on the 14dp-tall strip seeks, which is why the touch target is taller than the
 * line it draws.
 */
@Composable
private fun ScrubberBar(
    progress: PlaybackProgress,
    chapters: List<Chapter>,
    isFullscreen: Boolean,
    onSeekTo: (positionMs: Long) -> Unit,
    onScrub: () -> Unit,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    var trackWidthPx by remember { mutableIntStateOf(0) }

    // Where the finger is while dragging, or null when not scrubbing. The thumb, fill and label follow
    // it, and the player is only asked to seek once, on release — a seek per move event would make a
    // streamed video rebuffer the whole way along the drag.
    var scrubFraction by remember { mutableStateOf<Float?>(null) }
    val currentOnSeekTo by rememberUpdatedState(onSeekTo)
    val currentOnScrub by rememberUpdatedState(onScrub)
    val shown = scrubFraction?.let { progress.copy(positionMs = (progress.durationMs * it).toLong()) } ?: progress

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(Color.Transparent, StreamlyBrand.Letterbox.copy(alpha = 0.8f))),
            )
            .padding(start = 12.dp, end = 12.dp, top = 22.dp, bottom = 9.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .onSizeChanged { trackWidthPx = it.width }
                .pointerInput(progress.durationMs) {
                    detectTapGestures { offset ->
                        if (progress.durationMs > 0 && trackWidthPx > 0) {
                            val fraction = (offset.x / trackWidthPx).coerceIn(0f, 1f)
                            currentOnSeekTo((progress.durationMs * fraction).toLong())
                        }
                    }
                }
                .pointerInput(progress.durationMs) {
                    val durationMs = progress.durationMs
                    fun fractionAt(x: Float) = (x / trackWidthPx).coerceIn(0f, 1f)
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            if (durationMs > 0 && trackWidthPx > 0) {
                                scrubFraction = fractionAt(offset.x)
                                currentOnScrub()
                            }
                        },
                        onDragEnd = {
                            scrubFraction?.let { currentOnSeekTo((durationMs * it).toLong()) }
                            scrubFraction = null
                        },
                        onDragCancel = { scrubFraction = null },
                    ) { change, _ ->
                        if (scrubFraction != null) {
                            change.consume()
                            scrubFraction = fractionAt(change.position.x)
                            currentOnScrub()
                        }
                    }
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .drawBehind {
                        val radius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                        drawRoundRect(
                            color = StreamlyBrand.OnMedia.copy(alpha = 0.22f),
                            cornerRadius = radius,
                        )
                        val fillWidth = size.width * shown.fraction
                        if (fillWidth > 0f) {
                            drawRoundRect(
                                color = SageMint,
                                size = Size(fillWidth, size.height),
                                cornerRadius = radius,
                            )
                        }
                        // Chapter ticks, punched in the track's own colour so they read as gaps.
                        if (progress.durationMs > 0) {
                            chapters.forEach { chapter ->
                                val fraction = (chapter.positionSeconds * 1000f / progress.durationMs)
                                if (fraction in 0f..1f) {
                                    drawRect(
                                        color = StreamlyBrand.Letterbox,
                                        topLeft = Offset(size.width * fraction, -1.dp.toPx()),
                                        size = Size(2.dp.toPx(), 5.dp.toPx()),
                                    )
                                }
                            }
                        }
                    },
            )

            // 11dp thumb with a mint halo, positioned along the track.
            val thumbOffset = with(density) { (trackWidthPx * shown.fraction).toDp() }
            Box(
                Modifier
                    .padding(start = (thumbOffset - 5.5.dp).coerceAtLeast(0.dp))
                    .size(11.dp)
                    .clip(CircleShape)
                    .background(StreamlyBrand.OnMedia)
                    .border(3.dp, SageMint.copy(alpha = 0.35f), CircleShape),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = scrubberLabel(shown, chapters),
                style = StreamlyType.Timestamp,
                color = StreamlyBrand.OnMediaVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = formatDuration(progress.durationSeconds),
                style = StreamlyType.Timestamp,
                color = StreamlyBrand.OnMediaVariant,
            )
            FullscreenButton(isFullscreen = isFullscreen, onClick = onToggleFullscreen)
        }
    }
}

@Composable
private fun FullscreenButton(
    isFullscreen: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OverlayIconButton(
        icon = if (isFullscreen) StreamlyIcons.FullscreenExit else StreamlyIcons.Fullscreen,
        contentDescription = stringResource(
            if (isFullscreen) R.string.player_fullscreen_exit else R.string.player_fullscreen_enter,
        ),
        onClick = onClick,
        modifier = modifier,
    )
}

/**
 * Sits at the end of the scrubber's label row, so it fades in and out with the rest of the overlay
 * rather than needing its own visibility rule.
 */
@Composable
private fun OverlayIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(28.dp)
            .clip(StreamlyShape.DurationBadge)
            // clickable rather than pointerInput, for the same reason as the centre button: on device
            // this node reported [0,0][0,0] bounds and no click action to accessibility services.
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = StreamlyBrand.OnMedia,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** Cross-fade for the overlay. */
private const val FADE_MS = 200

/** "0:42 · Blue hour" — the position, plus the chapter it falls in when there is one. */
private fun scrubberLabel(progress: PlaybackProgress, chapters: List<Chapter>): String {
    val position = formatDuration(progress.positionSeconds)
    val chapter = chapters.lastOrNull { it.positionSeconds <= progress.positionSeconds }
    return if (chapter != null && chapter.label.isNotBlank()) "$position · ${chapter.label}" else position
}

/**
 * Inset padding for an overlay element, applied only in fullscreen.
 *
 * In the floating pane the viewport is already inset from the screen edges and the screen around it
 * pads for the system bars, so padding here would only push the controls in off the video.
 */
@Composable
private fun fullscreenInsets(isFullscreen: Boolean, side: WindowInsetsSides): Modifier =
    if (isFullscreen) Modifier.safeOverlayPadding(side + WindowInsetsSides.Horizontal) else Modifier
