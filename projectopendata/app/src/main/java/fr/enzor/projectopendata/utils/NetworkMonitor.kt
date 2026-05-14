package fr.enzor.projectopendata.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import fr.enzor.projectopendata.vue.isNetworkAvailable

class NetworkMonitor(private val appContext: Context) {
    private val connectivityManager =
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isConnected = mutableStateOf(isNetworkAvailable(appContext))
    val isConnected: State<Boolean> get() = _isConnected

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isConnected.value = true
        }

        override fun onLost(network: Network) {
            _isConnected.value = isNetworkAvailable(appContext)
        }

        override fun onUnavailable() {
            _isConnected.value = false
        }
    }

    init {
        connectivityManager.registerDefaultNetworkCallback(callback)
    }
}
