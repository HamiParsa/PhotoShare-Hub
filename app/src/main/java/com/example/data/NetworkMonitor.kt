package com.example.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import com.example.model.NetworkStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

class NetworkMonitor(private val context: Context) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    private val _networkStatus = MutableStateFlow(getCurrentNetworkStatus())
    val networkStatus: StateFlow<NetworkStatus> = _networkStatus.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            updateStatus()
        }

        override fun onLost(network: Network) {
            updateStatus()
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            updateStatus()
        }
    }

    init {
        registerCallback()
    }

    private fun registerCallback() {
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager?.registerNetworkCallback(request, networkCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register network callback: ${e.message}")
        }
    }

    fun updateStatus() {
        scope.launch {
            val status = getCurrentNetworkStatus()
            _networkStatus.value = status
        }
    }

    fun getCurrentNetworkStatus(): NetworkStatus {
        val cm = connectivityManager ?: return NetworkStatus()
        val activeNetwork = cm.activeNetwork ?: return NetworkStatus(ipAddress = getLocalIpAddress())
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return NetworkStatus(ipAddress = getLocalIpAddress())

        val isConnected = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val isWifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        val isCellular = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)

        var ssid: String? = null
        if (isWifi) {
            try {
                val wifiInfo = wifiManager?.connectionInfo
                val rawSsid = wifiInfo?.ssid
                if (rawSsid != null && rawSsid != "<unknown ssid>" && rawSsid.isNotEmpty()) {
                    ssid = rawSsid.removeSurrounding("\"")
                }
            } catch (e: Exception) {
                // Ignore permission restrictions for SSID
            }
        }

        val ip = getLocalIpAddress()

        return NetworkStatus(
            isConnected = isConnected,
            isWifi = isWifi,
            isCellular = isCellular,
            ssid = ssid,
            ipAddress = ip
        )
    }

    companion object {
        private const val TAG = "NetworkMonitor"

        /**
         * Resolves the primary local IPv4 address of the device (Wi-Fi / Ethernet / hotspot).
         */
        fun getLocalIpAddress(): String {
            try {
                val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
                
                // First try to find active wlan / eth interfaces
                for (intf in interfaces) {
                    if (intf.isLoopback || !intf.isUp) continue
                    val name = intf.name.lowercase()
                    if (name.startsWith("wlan") || name.startsWith("eth") || name.startsWith("ap") || name.startsWith("rndis")) {
                        val addresses = Collections.list(intf.inetAddresses)
                        for (addr in addresses) {
                            if (!addr.isLoopbackAddress && addr is Inet4Address) {
                                val ip = addr.hostAddress
                                if (!ip.isNullOrBlank() && !ip.startsWith("127.")) {
                                    return ip
                                }
                            }
                        }
                    }
                }

                // Fallback to any active non-loopback IPv4 interface
                for (intf in interfaces) {
                    if (intf.isLoopback || !intf.isUp) continue
                    val addresses = Collections.list(intf.inetAddresses)
                    for (addr in addresses) {
                        if (!addr.isLoopbackAddress && addr is Inet4Address) {
                            val ip = addr.hostAddress
                            if (!ip.isNullOrBlank() && !ip.startsWith("127.")) {
                                return ip
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error resolving local IP: ${e.message}")
            }
            return "127.0.0.1"
        }
    }
}
