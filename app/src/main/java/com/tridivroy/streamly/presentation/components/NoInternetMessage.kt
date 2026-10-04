package com.tridivroy.streamly.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tridivroy.streamly.R
import com.tridivroy.streamly.core.theme.StreamlyBrand
import com.tridivroy.streamly.core.theme.StreamlyIcons
import com.tridivroy.streamly.core.theme.StreamlyTheme
import kotlinx.coroutines.delay

/**
 * "No Internet Connection" with a Retry button, used instead of raw network/source error text.
 *
 * Retry shows "Checking…" for a moment on every tap. If the connection is back the caller swaps this
 * out; if not, the brief spinner is the feedback that the check ran, instead of a button that seems
 * to do nothing.
 *
 * @param contentColor text and icon colour: the theme's for a screen, [StreamlyBrand.OnMedia] over video.
 */
@Composable
fun NoInternetMessage(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.onBackground,
) {
    var isChecking by remember { mutableStateOf(false) }
    LaunchedEffect(isChecking) {
        if (isChecking) {
            delay(RETRY_FEEDBACK_MS)
            isChecking = false
        }
    }

    Column(
        modifier = modifier
            .padding(24.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = StreamlyIcons.WifiOff,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(44.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.no_internet_title),
            style = MaterialTheme.typography.titleMedium,
            color = contentColor,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.no_internet_message),
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor.copy(alpha = 0.75f),
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 320.dp),
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                isChecking = true
                onRetry()
            },
            enabled = !isChecking,
        ) {
            if (isChecking) {
                CircularProgressIndicator(
                    color = LocalContentColor.current,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.no_internet_checking))
            } else {
                Text(stringResource(R.string.no_internet_retry))
            }
        }
    }
}

/**
 * [NoInternetMessage] on a scrim, sized by the caller to cover a video surface. It also swallows
 * taps, so the play/pause and seek gestures underneath can't fire while playback is stalled.
 */
@Composable
fun NoInternetOverlay(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(StreamlyBrand.Scrim)
            .pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.Center,
    ) {
        NoInternetMessage(onRetry = onRetry, contentColor = StreamlyBrand.OnMedia)
    }
}

private const val RETRY_FEEDBACK_MS = 900L

@Preview
@Composable
private fun NoInternetOverlayPreview() {
    StreamlyTheme {
        NoInternetOverlay(onRetry = {})
    }
}
