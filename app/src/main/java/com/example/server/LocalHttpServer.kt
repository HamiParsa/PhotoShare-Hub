package com.example.server

import android.os.Build
import android.util.Log
import com.example.data.GalleryRepository
import com.example.data.ServerLogRepository
import com.example.model.PhotoItem
import com.example.model.ServerInfo
import com.example.model.ServerStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class LocalHttpServer(
    private val galleryRepository: GalleryRepository
) {
    private val serverJob = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + serverJob)

    private var serverSocket: ServerSocket? = null
    private var listeningJob: Job? = null

    private val _serverInfo = MutableStateFlow(ServerInfo())
    val serverInfo: StateFlow<ServerInfo> = _serverInfo.asStateFlow()

    private val activeClientsCounter = AtomicInteger(0)
    private val totalRequestsCounter = AtomicLong(0)
    private val totalBytesCounter = AtomicLong(0)

    val isRunning: Boolean
        get() = _serverInfo.value.status == ServerStatus.RUNNING

    fun start(port: Int, localIp: String): Boolean {
        if (isRunning) {
            Log.d(TAG, "Server already running on port ${_serverInfo.value.port}")
            return true
        }

        _serverInfo.value = _serverInfo.value.copy(
            status = ServerStatus.STARTING,
            port = port,
            ipAddress = localIp,
            error = null
        )

        return try {
            val socket = ServerSocket(port)
            socket.reuseAddress = true
            serverSocket = socket

            _serverInfo.value = ServerInfo(
                status = ServerStatus.RUNNING,
                port = port,
                ipAddress = localIp,
                activeClients = 0,
                totalRequests = totalRequestsCounter.get(),
                totalBytesTransferred = totalBytesCounter.get(),
                startTimeMs = System.currentTimeMillis()
            )

            ServerLogRepository.addLog(
                clientIp = localIp,
                method = "SYSTEM",
                path = "http://$localIp:$port",
                statusCode = 200,
                details = "Server started successfully on port $port"
            )

            listeningJob = scope.launch {
                listenForConnections(socket)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start HTTP server: ${e.message}", e)
            _serverInfo.value = _serverInfo.value.copy(
                status = ServerStatus.ERROR,
                error = e.localizedMessage ?: "Failed to bind port $port"
            )
            ServerLogRepository.addLog(
                clientIp = localIp,
                method = "ERROR",
                path = ":$port",
                statusCode = 500,
                details = "Failed to start server: ${e.message}",
                isError = true
            )
            false
        }
    }

    fun stop() {
        try {
            listeningJob?.cancel()
            listeningJob = null
            serverSocket?.close()
            serverSocket = null
        } catch (e: Exception) {
            Log.e(TAG, "Error closing server socket: ${e.message}")
        } finally {
            _serverInfo.value = _serverInfo.value.copy(
                status = ServerStatus.STOPPED,
                activeClients = 0
            )
            ServerLogRepository.addLog(
                clientIp = "127.0.0.1",
                method = "SYSTEM",
                path = "/",
                statusCode = 200,
                details = "Server stopped"
            )
        }
    }

    fun updateIpAddress(newIp: String) {
        if (_serverInfo.value.ipAddress != newIp) {
            _serverInfo.value = _serverInfo.value.copy(ipAddress = newIp)
        }
    }

    private suspend fun listenForConnections(socket: ServerSocket) = withContext(Dispatchers.IO) {
        while (isActive && !socket.isClosed) {
            try {
                val clientSocket = socket.accept()
                activeClientsCounter.incrementAndGet()
                updateStateMetrics()

                scope.launch {
                    handleClient(clientSocket)
                }
            } catch (e: Exception) {
                if (socket.isClosed) break
                Log.e(TAG, "Socket accept error: ${e.message}")
            }
        }
    }

    private fun handleClient(socket: Socket) {
        val clientIp = socket.inetAddress?.hostAddress ?: "unknown"
        var requestPath = ""
        var requestMethod = "GET"
        var statusCode = 200

        try {
            socket.soTimeout = 15000
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val outputStream = BufferedOutputStream(socket.getOutputStream())

            val requestLine = reader.readLine()
            if (requestLine.isNullOrBlank()) {
                socket.close()
                return
            }

            totalRequestsCounter.incrementAndGet()

            val parts = requestLine.split(" ")
            if (parts.size >= 2) {
                requestMethod = parts[0].uppercase()
                requestPath = parts[1]
            }

            // Consume rest of headers
            val headers = mutableMapOf<String, String>()
            var headerLine: String?
            while (reader.readLine().also { headerLine = it } != null) {
                if (headerLine.isNullOrBlank()) break
                val colonIdx = headerLine!!.indexOf(':')
                if (colonIdx != -1) {
                    val key = headerLine!!.substring(0, colonIdx).trim().lowercase()
                    val value = headerLine!!.substring(colonIdx + 1).trim()
                    headers[key] = value
                }
            }

            statusCode = dispatchRoute(
                method = requestMethod,
                fullPath = requestPath,
                headers = headers,
                output = outputStream,
                clientIp = clientIp
            )

        } catch (e: Exception) {
            statusCode = 500
            Log.e(TAG, "Error handling client $clientIp for $requestPath: ${e.message}")
        } finally {
            try {
                socket.close()
            } catch (e: Exception) {
                // Ignore
            }
            activeClientsCounter.decrementAndGet()
            updateStateMetrics()

            ServerLogRepository.addLog(
                clientIp = clientIp,
                method = requestMethod,
                path = requestPath,
                statusCode = statusCode,
                isError = statusCode >= 400
            )
        }
    }

    private fun dispatchRoute(
        method: String,
        fullPath: String,
        headers: Map<String, String>,
        output: OutputStream,
        clientIp: String
    ): Int {
        if (method == "OPTIONS") {
            writeResponseHeaders(output, 204, "No Content", "text/plain", 0)
            output.flush()
            return 204
        }

        val decodedPath = try {
            URLDecoder.decode(fullPath, "UTF-8")
        } catch (e: Exception) {
            fullPath
        }

        val uriParts = decodedPath.split("?", limit = 2)
        val path = uriParts[0]
        val query = if (uriParts.size > 1) uriParts[1] else ""

        return when {
            path == "/" || path == "/index.html" -> {
                serveHtml(output)
            }
            path == "/api/photos" -> {
                servePhotosJson(output)
            }
            path == "/api/albums" -> {
                serveAlbumsJson(output)
            }
            path == "/api/status" -> {
                serveStatusJson(output)
            }
            path == "/api/ping" -> {
                serveJson(output, """{"status":"ok","pong":true}""")
            }
            path.startsWith("/api/photo/") -> {
                val idStr = path.substringAfter("/api/photo/")
                servePhotoFile(idStr.toLongOrNull(), isDownload = false, output = output)
            }
            path.startsWith("/api/thumb/") -> {
                val idStr = path.substringAfter("/api/thumb/")
                servePhotoThumbnail(idStr.toLongOrNull(), output = output)
            }
            path.startsWith("/api/download/") -> {
                val idStr = path.substringAfter("/api/download/")
                servePhotoFile(idStr.toLongOrNull(), isDownload = true, output = output)
            }
            path == "/api/zip" -> {
                val album = getQueryParam(query, "album")
                serveZipArchive(album, output)
            }
            else -> {
                serve404(output, path)
            }
        }
    }

    private fun serveHtml(output: OutputStream): Int {
        val html = WebClientHtml.getHtml()
        val bytes = html.toByteArray(StandardCharsets.UTF_8)
        writeResponseHeaders(output, 200, "OK", "text/html; charset=utf-8", bytes.size.toLong())
        output.write(bytes)
        output.flush()
        totalBytesCounter.addAndGet(bytes.size.toLong())
        return 200
    }

    private fun servePhotosJson(output: OutputStream): Int {
        val photos = galleryRepository.photos.value
        val jsonArray = JSONArray()

        for (p in photos) {
            val obj = JSONObject().apply {
                put("id", p.id)
                put("name", p.displayName)
                put("size", p.sizeBytes)
                put("sizeFormatted", p.getFormattedSize())
                put("date", p.getFormattedDate())
                put("album", p.bucketName)
                put("mime", p.mimeType)
                put("width", p.width)
                put("height", p.height)
                put("resolution", p.getResolutionString())
                put("url", "/api/photo/${p.id}")
                put("thumbUrl", "/api/thumb/${p.id}")
                put("downloadUrl", "/api/download/${p.id}")
            }
            jsonArray.put(obj)
        }

        val jsonStr = jsonArray.toString()
        val bytes = jsonStr.toByteArray(StandardCharsets.UTF_8)
        writeResponseHeaders(output, 200, "OK", "application/json; charset=utf-8", bytes.size.toLong())
        output.write(bytes)
        output.flush()
        totalBytesCounter.addAndGet(bytes.size.toLong())
        return 200
    }

    private fun serveAlbumsJson(output: OutputStream): Int {
        val albums = galleryRepository.albums.value
        val jsonArray = JSONArray()

        for (a in albums) {
            val obj = JSONObject().apply {
                put("name", a.name)
                put("count", a.photoCount)
            }
            jsonArray.put(obj)
        }

        val jsonStr = jsonArray.toString()
        val bytes = jsonStr.toByteArray(StandardCharsets.UTF_8)
        writeResponseHeaders(output, 200, "OK", "application/json; charset=utf-8", bytes.size.toLong())
        output.write(bytes)
        output.flush()
        totalBytesCounter.addAndGet(bytes.size.toLong())
        return 200
    }

    private fun serveStatusJson(output: OutputStream): Int {
        val obj = JSONObject().apply {
            put("status", _serverInfo.value.status.name)
            put("port", _serverInfo.value.port)
            put("ip", _serverInfo.value.ipAddress)
            put("photosCount", galleryRepository.photos.value.size)
            put("deviceName", "${Build.MANUFACTURER} ${Build.MODEL}")
            put("uptimeMs", if (_serverInfo.value.startTimeMs > 0) System.currentTimeMillis() - _serverInfo.value.startTimeMs else 0)
            put("totalRequests", totalRequestsCounter.get())
        }
        val bytes = obj.toString().toByteArray(StandardCharsets.UTF_8)
        writeResponseHeaders(output, 200, "OK", "application/json; charset=utf-8", bytes.size.toLong())
        output.write(bytes)
        output.flush()
        totalBytesCounter.addAndGet(bytes.size.toLong())
        return 200
    }

    private fun serveJson(output: OutputStream, jsonString: String): Int {
        val bytes = jsonString.toByteArray(StandardCharsets.UTF_8)
        writeResponseHeaders(output, 200, "OK", "application/json; charset=utf-8", bytes.size.toLong())
        output.write(bytes)
        output.flush()
        totalBytesCounter.addAndGet(bytes.size.toLong())
        return 200
    }

    private fun servePhotoFile(photoId: Long?, isDownload: Boolean, output: OutputStream): Int {
        if (photoId == null) return serve404(output, "Invalid ID")

        val photo = galleryRepository.getPhotoById(photoId) ?: return serve404(output, "Photo not found")
        val inputStream = galleryRepository.openPhotoInputStream(photo) ?: return serve404(output, "File stream unreadable")

        return try {
            val mime = photo.mimeType.ifBlank { "image/jpeg" }
            val extraHeaders = mutableMapOf<String, String>()
            if (isDownload) {
                extraHeaders["Content-Disposition"] = "attachment; filename=\"${photo.displayName}\""
            } else {
                extraHeaders["Content-Disposition"] = "inline; filename=\"${photo.displayName}\""
            }
            extraHeaders["Cache-Control"] = "public, max-age=31536000"

            writeResponseHeaders(output, 200, "OK", mime, photo.sizeBytes, extraHeaders)

            val buffer = ByteArray(32 * 1024)
            var bytesRead: Int
            var totalWritten = 0L
            inputStream.use { stream ->
                while (stream.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    totalWritten += bytesRead
                }
            }
            output.flush()
            totalBytesCounter.addAndGet(totalWritten)
            200
        } catch (e: Exception) {
            Log.e(TAG, "Error streaming photo file: ${e.message}")
            500
        }
    }

    private fun servePhotoThumbnail(photoId: Long?, output: OutputStream): Int {
        if (photoId == null) return serve404(output, "Invalid ID")

        val photo = galleryRepository.getPhotoById(photoId) ?: return serve404(output, "Photo not found")
        val thumbBytes = galleryRepository.getThumbnailBytes(photo) ?: return serve404(output, "Thumbnail unavailable")

        val extraHeaders = mapOf("Cache-Control" to "public, max-age=86400")
        writeResponseHeaders(output, 200, "OK", "image/jpeg", thumbBytes.size.toLong(), extraHeaders)
        output.write(thumbBytes)
        output.flush()
        totalBytesCounter.addAndGet(thumbBytes.size.toLong())
        return 200
    }

    private fun serveZipArchive(album: String?, output: OutputStream): Int {
        val fileName = if (!album.isNullOrBlank()) "${album}_photos.zip" else "all_photos.zip"
        val extraHeaders = mapOf(
            "Content-Disposition" to "attachment; filename=\"$fileName\"",
            "Transfer-Encoding" to "chunked"
        )
        // Chunked / streamed headers (length -1)
        writeResponseHeaders(output, 200, "OK", "application/zip", -1, extraHeaders)

        return try {
            kotlinx.coroutines.runBlocking {
                galleryRepository.streamZipArchive(album, output)
            }
            200
        } catch (e: Exception) {
            Log.e(TAG, "Error streaming zip: ${e.message}")
            500
        }
    }

    private fun serve404(output: OutputStream, path: String): Int {
        val body = "404 Not Found: $path"
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        writeResponseHeaders(output, 404, "Not Found", "text/plain", bytes.size.toLong())
        output.write(bytes)
        output.flush()
        return 404
    }

    private fun writeResponseHeaders(
        output: OutputStream,
        code: Int,
        statusText: String,
        contentType: String,
        contentLength: Long,
        extraHeaders: Map<String, String> = emptyMap()
    ) {
        val sb = StringBuilder()
        sb.append("HTTP/1.1 $code $statusText\r\n")
        sb.append("Content-Type: $contentType\r\n")
        if (contentLength >= 0) {
            sb.append("Content-Length: $contentLength\r\n")
        }
        sb.append("Access-Control-Allow-Origin: *\r\n")
        sb.append("Access-Control-Allow-Methods: GET, HEAD, OPTIONS\r\n")
        sb.append("Access-Control-Allow-Headers: *\r\n")
        sb.append("Connection: close\r\n")

        for ((k, v) in extraHeaders) {
            sb.append("$k: $v\r\n")
        }
        sb.append("\r\n")

        output.write(sb.toString().toByteArray(StandardCharsets.UTF_8))
    }

    private fun getQueryParam(query: String, paramName: String): String? {
        if (query.isBlank()) return null
        val pairs = query.split("&")
        for (pair in pairs) {
            val parts = pair.split("=")
            if (parts.size == 2 && parts[0] == paramName) {
                return try {
                    URLDecoder.decode(parts[1], "UTF-8")
                } catch (e: Exception) {
                    parts[1]
                }
            }
        }
        return null
    }

    private fun updateStateMetrics() {
        _serverInfo.value = _serverInfo.value.copy(
            activeClients = activeClientsCounter.get().coerceAtLeast(0),
            totalRequests = totalRequestsCounter.get(),
            totalBytesTransferred = totalBytesCounter.get()
        )
    }

    companion object {
        private const val TAG = "LocalHttpServer"
    }
}
