package com.tridivroy.streamly.domain.repository

import kotlinx.coroutines.flow.StateFlow

/** Live device connectivity, so screens can tell "no internet" apart from other failures. */
interface ConnectivityObserver {
    /** `true` while the device has a network that offers internet access. Always holds the current value. */
    val isOnline: StateFlow<Boolean>
}
