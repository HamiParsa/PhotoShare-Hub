package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.AppLanguage
import com.example.ui.MainViewModel
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.GalleryScreen
import com.example.ui.screens.GuideScreen
import com.example.ui.screens.LogsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.CyanElectric
import com.example.ui.theme.PhotoShareTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val mainViewModel: MainViewModel = viewModel()
            val settings by mainViewModel.settings.collectAsState()

            val layoutDirection = if (settings.language == AppLanguage.FA) {
                LayoutDirection.Rtl
            } else {
                LayoutDirection.Ltr
            }

            PhotoShareTheme(themeMode = settings.themeMode) {
                CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                    PhotoShareApp(viewModel = mainViewModel)
                }
            }
        }
    }
}

@Composable
fun PhotoShareApp(viewModel: MainViewModel) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    val serverInfo by viewModel.serverInfo.collectAsState()
    val networkStatus by viewModel.networkStatus.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val photos by viewModel.photos.collectAsState()
    val filteredPhotos by viewModel.filteredPhotos.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val selectedAlbum by viewModel.selectedAlbum.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val storageFormatted by viewModel.totalStorageFormatted.collectAsState()

    // Request permissions on launch
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val grantedImages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.READ_MEDIA_IMAGES] == true
        } else {
            permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true
        }
        if (grantedImages) {
            viewModel.refreshGallery()
        }
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permissionLauncher.launch(permissionsToRequest.toTypedArray())
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                val navItemColors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )

                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_dashboard)) },
                    modifier = Modifier.testTag("nav_tab_dashboard"),
                    colors = navItemColors
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_gallery)) },
                    modifier = Modifier.testTag("nav_tab_gallery"),
                    colors = navItemColors
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_logs)) },
                    modifier = Modifier.testTag("nav_tab_logs"),
                    colors = navItemColors
                )

                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.MenuBook, contentDescription = null) },
                    label = { Text(if (settings.language == AppLanguage.FA) "راهنما" else "Guide") },
                    modifier = Modifier.testTag("nav_tab_guide"),
                    colors = navItemColors
                )

                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_settings)) },
                    modifier = Modifier.testTag("nav_tab_settings"),
                    colors = navItemColors
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> DashboardScreen(
                    serverInfo = serverInfo,
                    networkStatus = networkStatus,
                    settings = settings,
                    photosCount = photos.size,
                    storageFormatted = storageFormatted,
                    recentLogs = logs,
                    isSyncing = isSyncing,
                    onStartServer = { viewModel.startServer() },
                    onStopServer = { viewModel.stopServer() },
                    onRestartServer = { viewModel.restartServer() },
                    onToggleAutoSync = { viewModel.toggleAutoSync(it) },
                    onNavigateToGallery = { selectedTab = 1 },
                    onNavigateToLogs = { selectedTab = 2 }
                )
                1 -> GalleryScreen(
                    photos = filteredPhotos,
                    albums = albums,
                    selectedAlbum = selectedAlbum,
                    searchQuery = searchQuery,
                    isSyncing = isSyncing,
                    onSelectAlbum = { viewModel.selectAlbum(it) },
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    onRefreshGallery = { viewModel.refreshGallery() }
                )
                2 -> LogsScreen(
                    logs = logs,
                    onClearLogs = { viewModel.clearLogs() }
                )
                3 -> GuideScreen(
                    serverInfo = serverInfo,
                    language = settings.language
                )
                4 -> SettingsScreen(
                    settings = settings,
                    networkStatus = networkStatus,
                    onToggleAutoSync = { viewModel.toggleAutoSync(it) },
                    onSetWifiOnly = { viewModel.setWifiOnly(it) },
                    onSetPort = { viewModel.setPort(it) },
                    onSetThemeMode = { viewModel.setThemeMode(it) },
                    onSetLanguage = { viewModel.setLanguage(it) }
                )
            }
        }
    }
}
