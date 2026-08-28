package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GalleryRepository
import com.example.data.NetworkMonitor
import com.example.data.PreferencesManager
import com.example.data.ServerLogRepository
import com.example.model.AlbumInfo
import com.example.model.AppLanguage
import com.example.model.AppSettings
import com.example.model.AppThemeMode
import com.example.model.NetworkStatus
import com.example.model.PhotoItem
import com.example.model.ServerInfo
import com.example.model.ServerLog
import com.example.server.LocalHttpServer
import com.example.service.LocalSharingService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesManager = PreferencesManager(application)
    private val networkMonitor = NetworkMonitor(application)
    private val galleryRepository = GalleryRepository(application)
    private val localServer = LocalHttpServer(galleryRepository)

    val settings: StateFlow<AppSettings> = preferencesManager.settings
    val networkStatus: StateFlow<NetworkStatus> = networkMonitor.networkStatus
    val photos: StateFlow<List<PhotoItem>> = galleryRepository.photos
    val albums: StateFlow<List<AlbumInfo>> = galleryRepository.albums
    val isSyncing: StateFlow<Boolean> = galleryRepository.isSyncing
    val logs: StateFlow<List<ServerLog>> = ServerLogRepository.logsFlow

    private val _serverInfo = MutableStateFlow(ServerInfo())
    val serverInfo: StateFlow<ServerInfo> = _serverInfo.asStateFlow()

    private val _selectedAlbum = MutableStateFlow("All")
    val selectedAlbum: StateFlow<String> = _selectedAlbum.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredPhotos: StateFlow<List<PhotoItem>> = combine(
        photos,
        _selectedAlbum,
        _searchQuery
    ) { photoList, album, query ->
        photoList.filter { photo ->
            val matchAlbum = album == "All" || photo.bucketName.equals(album, ignoreCase = true)
            val matchQuery = query.isBlank() || photo.displayName.contains(query, ignoreCase = true) || photo.getFormattedDate().contains(query, ignoreCase = true)
            matchAlbum && matchQuery
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val totalStorageFormatted: StateFlow<String> = photos.map { photoList ->
        val totalBytes = photoList.sumOf { it.sizeBytes }
        if (totalBytes <= 0) "0 MB"
        else {
            val mb = totalBytes / (1024.0 * 1024.0)
            if (mb > 1024) String.format("%.2f GB", mb / 1024.0)
            else String.format("%.1f MB", mb)
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, "0 MB")

    init {
        viewModelScope.launch {
            // Monitor service or local server info
            localServer.serverInfo.collect { info ->
                _serverInfo.value = info
            }
        }

        viewModelScope.launch {
            // Auto sync gallery on init
            galleryRepository.refreshPhotos()
        }

        viewModelScope.launch {
            networkMonitor.networkStatus.collect { netStatus ->
                if (_serverInfo.value.ipAddress != netStatus.ipAddress && localServer.isRunning) {
                    localServer.updateIpAddress(netStatus.ipAddress)
                }

                // Check auto-start if service not running
                val currentSettings = preferencesManager.settings.value
                if (currentSettings.autoSync && netStatus.isLocalNetworkAvailable) {
                    if (currentSettings.wifiOnly && !netStatus.isWifi) {
                        // Do not auto start on cellular if wifi only
                    } else if (!_serverInfo.value.status.name.equals("RUNNING", ignoreCase = true)) {
                        startServer()
                    }
                }
            }
        }
    }

    fun startServer() {
        val app = getApplication<Application>()
        LocalSharingService.startService(app)
        // Also start local server instance if standalone
        val port = settings.value.port
        val ip = networkStatus.value.ipAddress
        viewModelScope.launch(Dispatchers.IO) {
            galleryRepository.refreshPhotos()
            localServer.start(port, ip)
        }
    }

    fun stopServer() {
        val app = getApplication<Application>()
        LocalSharingService.stopService(app)
        localServer.stop()
    }

    fun restartServer() {
        stopServer()
        startServer()
    }

    fun refreshGallery() {
        viewModelScope.launch(Dispatchers.IO) {
            galleryRepository.refreshPhotos()
        }
    }

    fun selectAlbum(album: String) {
        _selectedAlbum.value = album
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleAutoSync(enabled: Boolean) {
        preferencesManager.setAutoSync(enabled)
    }

    fun setWifiOnly(enabled: Boolean) {
        preferencesManager.setWifiOnly(enabled)
    }

    fun setPort(port: Int) {
        preferencesManager.setPort(port)
        if (localServer.isRunning) {
            restartServer()
        }
    }

    fun setThemeMode(themeMode: AppThemeMode) {
        preferencesManager.setThemeMode(themeMode)
    }

    fun setLanguage(language: AppLanguage) {
        preferencesManager.setLanguage(language)
    }

    fun clearLogs() {
        ServerLogRepository.clearLogs()
    }
}
