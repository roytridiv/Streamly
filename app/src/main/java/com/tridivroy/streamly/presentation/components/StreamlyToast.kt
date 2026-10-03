package com.tridivroy.streamly.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.tridivroy.streamly.core.theme.StreamlyBrand
import kotlinx.coroutines.delay

/**
 * The handoff's toast: a SurfaceRaised pill with a mint edge, up for 1.8s.
 *
 * Material's Snackbar is the wrong shape here — it spans the width and carries its own elevation and
 * action slot. This is a plain centred pill, which is what "Link copied" is meant to look like.
 */
class ToastState internal constructor() {
    internal var message by mutableStateOf<String?>(null)
        private set

    /** Shows [message]; a new call replaces whatever is up and restarts the timer. */
    fun show(message: String) {
        this.message = message
    }

    internal fun dismiss() {
        message = null
    }
}

@Composable
fun rememberToastState(): ToastState = remember { ToastState() }

@Composable
fun StreamlyToastHost(
    state: ToastState,
    modifier: Modifier = Modifier,
) {
    val message = state.message

    LaunchedEffect(message) {
        if (message != null) {
            delay(VISIBLE_MS)
            state.dismiss()
        }
    }

    Box(modifier.padding(bottom = 96.dp), contentAlignment = Alignment.Center) {
        AnimatedVisibility(
            visible = message != null,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
        ) {
            Box(
                Modifier
                    .clip(SHAPE)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(1.dp, StreamlyBrand.GlowEdge.copy(alpha = 0.3f), SHAPE)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(
                    // Keeps the last message through the exit animation, so it does not blank first.
                    text = message ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
            }
        }
    }
}

private val SHAPE = RoundedCornerShape(12.dp)
private const val VISIBLE_MS = 1_800L
