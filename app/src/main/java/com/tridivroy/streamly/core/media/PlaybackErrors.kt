package com.tridivroy.streamly.core.media

import androidx.media3.common.PlaybackException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

private val NETWORK_ERROR_CODES = setOf(
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
    PlaybackException.ERROR_CODE_TIMEOUT,
)

/**
 * `true` when playback failed because the network couldn't be reached, as opposed to a bad stream or
 * a server error. Media3 reports some DNS failures as an unspecified IO error, so the cause chain is
 * checked too.
 */
fun PlaybackException.isNetworkError(): Boolean =
    errorCode in NETWORK_ERROR_CODES ||
        generateSequence(cause) { it.cause }.any {
            it is UnknownHostException || it is ConnectException || it is SocketTimeoutException
        }
