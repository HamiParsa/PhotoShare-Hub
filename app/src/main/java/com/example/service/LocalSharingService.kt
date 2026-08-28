package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.GalleryRepository
import com.example.data.NetworkMonitor
import com.example.data.PreferencesManager
import com.example.model.ServerInfo
import com.example.model.ServerStatus
import com.example.server.LocalHttpServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LocalSharingService : Service() {

    private val serviceJob = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var galleryRepository: GalleryRepository
    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var httpServer: LocalHttpServer

    private var wakeLock: PowerManager.WakeLock? = null
    private var networkObserverJob: Job? = null
    private var serverStateJob: Job? = null

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): LocalSharingService = this@LocalSharingService
        fun getHttpServer(): LocalHttpServer = httpServer
        fun getGalleryRepository(): GalleryRepository = galleryRepository
        fun getNetworkMonitor(): NetworkMonitor = networkMonitor
        fun getPreferencesManager(): PreferencesManager = preferencesManager
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        galleryRepository = GalleryRepository(applicationContext)
        networkMonitor = NetworkMonitor(applicationContext)
        preferencesManager = PreferencesManager(applicationContext)
        httpServer = LocalHttpServer(galleryRepository)

        createNotificationChannel()
        acquireWakeLock()

        observeNetworkChanges()
        observeServerState()

        scope.launch(Dispatchers.IO) {
            galleryRepository.refreshPhotos()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        when (action) {
            ACTION_STOP_SERVER -> {
                stopSharingServer()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_RESTART_SERVER -> {
                restartSharingServer()
            }
            ACTION_START_SERVER, null -> {
                startSharingServer()
            }
        }

        val notification = buildNotification(httpServer.serverInfo.value)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        return START_STICKY
    }

    private fun startSharingServer() {
        val settings = preferencesManager.settings.value
        val netStatus = networkMonitor.networkStatus.value
        val ip = netStatus.ipAddress

        scope.launch(Dispatchers.IO) {
            galleryRepository.refreshPhotos()
            httpServer.start(settings.port, ip)
        }
    }

    fun stopSharingServer() {
        httpServer.stop()
    }

    fun restartSharingServer() {
        stopSharingServer()
        startSharingServer()
    }

    private fun observeNetworkChanges() {
        networkObserverJob?.cancel()
        networkObserverJob = scope.launch {
            networkMonitor.networkStatus.collectLatest { netStatus ->
                val settings = preferencesManager.settings.value
                val isWifiOnly = settings.wifiOnly

                if (httpServer.isRunning) {
                    httpServer.updateIpAddress(netStatus.ipAddress)
                    updateNotification(httpServer.serverInfo.value)
                }

                // Auto-sync trigger
                if (settings.autoSync) {
                    val allowed = if (isWifiOnly) netStatus.isWifi else netStatus.isConnected
                    if (allowed && netStatus.isLocalNetworkAvailable) {
                        if (!httpServer.isRunning) {
                            startSharingServer()
                        } else {
                            galleryRepository.refreshPhotos()
                        }
                    }
                }
            }
        }
    }

    private fun observeServerState() {
        serverStateJob?.cancel()
        serverStateJob = scope.launch {
            httpServer.serverInfo.collectLatest { info ->
                updateNotification(info)
            }
        }
    }

    private fun updateNotification(info: ServerInfo) {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val notification = buildNotification(info)
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(info: ServerInfo): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, LocalSharingService::class.java).apply {
            action = ACTION_STOP_SERVER
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isRunning = info.status == ServerStatus.RUNNING
        val photosCount = galleryRepository.photos.value.size

        val title = if (isRunning) {
            getString(R.string.foreground_service_title)
        } else {
            getString(R.string.server_stopped)
        }

        val content = if (isRunning) {
            getString(R.string.foreground_service_content, info.fullUrl, photosCount)
        } else {
            "Tap to configure and start local sharing"
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(isRunning)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        if (isRunning) {
            builder.addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.stop_server),
                stopPendingIntent
            )
        }

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notification_channel_name)
            val desc = getString(R.string.notification_channel_desc)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = desc
                setShowBadge(false)
            }
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "PhotoShare:ServerWakeLock"
            )?.apply {
                setReferenceCounted(false)
                acquire(3 * 60 * 60 * 1000L) // 3 hours max safe limit
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire wake lock: ${e.message}")
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
            wakeLock = null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release wake lock: ${e.message}")
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
        stopSharingServer()
        releaseWakeLock()
        scope.cancel()
    }

    companion object {
        private const val TAG = "LocalSharingService"
        const val CHANNEL_ID = "photoshare_server_channel"
        const val NOTIFICATION_ID = 9981

        const val ACTION_START_SERVER = "com.example.action.START_SERVER"
        const val ACTION_STOP_SERVER = "com.example.action.STOP_SERVER"
        const val ACTION_RESTART_SERVER = "com.example.action.RESTART_SERVER"

        @Volatile
        var instance: LocalSharingService? = null
            private set

        fun startService(context: Context) {
            val intent = Intent(context, LocalSharingService::class.java).apply {
                action = ACTION_START_SERVER
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, LocalSharingService::class.java).apply {
                action = ACTION_STOP_SERVER
            }
            context.startService(intent)
        }
    }
}
