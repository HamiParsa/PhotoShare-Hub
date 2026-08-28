package com.example.data

import com.example.model.ServerLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentLinkedDeque

object ServerLogRepository {
    private const val MAX_LOGS = 120
    private val logsQueue = ConcurrentLinkedDeque<ServerLog>()
    private val _logsFlow = MutableStateFlow<List<ServerLog>>(emptyList())
    val logsFlow: StateFlow<List<ServerLog>> = _logsFlow.asStateFlow()

    fun addLog(log: ServerLog) {
        logsQueue.addFirst(log)
        while (logsQueue.size > MAX_LOGS) {
            logsQueue.pollLast()
        }
        _logsFlow.value = logsQueue.toList()
    }

    fun addLog(
        clientIp: String,
        method: String,
        path: String,
        statusCode: Int,
        details: String = "",
        isError: Boolean = false
    ) {
        addLog(
            ServerLog(
                clientIp = clientIp,
                method = method,
                path = path,
                statusCode = statusCode,
                details = details,
                isError = isError
            )
        )
    }

    fun clearLogs() {
        logsQueue.clear()
        _logsFlow.value = emptyList()
    }
}
