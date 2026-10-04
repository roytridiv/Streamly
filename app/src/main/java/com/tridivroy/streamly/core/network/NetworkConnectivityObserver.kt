package com.tridivroy.streamly.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.core.content.getSystemService
import com.tridivroy.streamly.domain.repository.ConnectivityObserver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [ConnectivityObserver] over [ConnectivityManager]'s default-network callback, registered once for
 * the app's lifetime. The default network is the one apps actually use, so switching between Wi-Fi
 * and mobile data doesn't show up as a brief "offline".
 *
 * "Online" means the network *offers* internet ([NetworkCapabilities.NET_CAPABILITY_INTERNET]), not
 * that it has been validated: some networks never validate, and a captive portal still fails at
 * playback time, where the error is classified as a network error anyway.
 */
@Singleton
class NetworkConnectivityObserver @Inject constructor(
    @ApplicationContext context: Context,
) : ConnectivityObserver {

    private val connectivityManager = requireNotNull(context.getSystemService<ConnectivityManager>())

    private val _isOnline = MutableStateFlow(currentlyOnline())
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    init {
        connectivityManager.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                _isOnline.value = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            }

            override fun onLost(network: Network) {
                _isOnline.value = false
            }

            override fun onUnavailable() {
                _isOnline.value = false
            }
        })
    }

    private fun currentlyOnline(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
