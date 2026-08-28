package com.example.model

data class NetworkStatus(
    val isConnected: Boolean = false,
    val isWifi: Boolean = false,
    val isCellular: Boolean = false,
    val ssid: String? = null,
    val ipAddress: String = "127.0.0.1",
    val networkInterfaceName: String = "wlan0"
) {
    val isLocalNetworkAvailable: Boolean
        get() = isConnected && ipAddress != "127.0.0.1" && ipAddress.isNotBlank()
}
