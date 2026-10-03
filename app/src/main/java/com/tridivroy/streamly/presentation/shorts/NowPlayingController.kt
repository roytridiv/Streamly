package com.tridivroy.streamly.presentation.shorts

/**
 * The app-wide player that the mini-player keeps running.
 *
 * Shorts only ever needs to silence it, so that is all this exposes. `NowPlayingStore` implements it.
 */
interface NowPlayingController {
    fun pause()
}
