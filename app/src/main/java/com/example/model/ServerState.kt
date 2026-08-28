package com.example.model

enum class ServerStatus {
    STOPPED,
    STARTING,
    RUNNING,
    ERROR
}

data class ServerInfo(
    val status: ServerStatus = ServerStatus.STOPPED,
    val port: Int = 8080,
    val ipAddress: String = "127.0.0.1",
    val activeClients: Int = 0,
    val totalRequests: Long = 0,
    val totalBytesTransferred: Long = 0,
    val startTimeMs: Long = 0L,
    val error: String? = null
) {
    val fullUrl: String
        get() = if (status == ServerStatus.RUNNING && ipAddress.isNotBlank() && ipAddress != "127.0.0.1") {
            "http://$ipAddress:$port"
        } else {
            "http://127.0.0.1:$port"
        }
}
