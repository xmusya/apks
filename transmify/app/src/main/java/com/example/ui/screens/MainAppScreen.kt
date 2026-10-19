package com.example.ui.screens

import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.SearchHistoryEntity
import com.example.ui.TransmifyViewModel
import com.example.ui.components.AddTorrentDialog
import com.example.ui.components.TabletOverviewPane
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.RadiantRed
import com.example.ui.theme.VibrantAmber

enum class NavTab(
    val titleRu: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector
) {
    TORRENTS(
        "Торренты",
        Icons.AutoMirrored.Filled.List,
        Icons.Outlined.List
    ),
    RUTRACKER(
        "RuTracker",
        Icons.Default.Search,
        Icons.Outlined.Search
    ),
    STATS(
        "Статистика",
        Icons.Default.Equalizer,
        Icons.Outlined.Equalizer
    ),
    SETTINGS(
        "Настройки",
        Icons.Default.Settings,
        Icons.Outlined.Settings
    )
}

@Composable
fun MainAppScreen(
    viewModel: TransmifyViewModel,
    initialAddMagnetOrFile: String? = null
) {
    var selectedTab by remember { mutableStateOf(NavTab.TORRENTS) }
    var showAddDialog by remember { mutableStateOf(false) }

    // Screen configuration for tablet adaptation
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600
    val isSplitPane = configuration.screenWidthDp >= 720

    // Collect states
    val activeServer by viewModel.activeServer.collectAsState()
    val servers by viewModel.servers.collectAsState()
    val torrents by viewModel.torrents.collectAsState()
    val sessionStats by viewModel.sessionStats.collectAsState()
    val sessionInfo by viewModel.sessionInfo.collectAsState()
    val selectedTorrent by viewModel.selectedTorrent.collectAsState()

    val activeFilter by viewModel.activeFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val activeSort by viewModel.activeSort.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val connectionError by viewModel.connectionError.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()

    val themeMode by viewModel.themeMode.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()

    val rutrackerTopics by viewModel.rutrackerTopics.collectAsState()
    val rutrackerQuery by viewModel.rutrackerQuery.collectAsState()
    val rutrackerCategory by viewModel.rutrackerCategory.collectAsState()
    val rutrackerLoading by viewModel.rutrackerLoading.collectAsState()
    val rutrackerMirror by viewModel.rutrackerMirror.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Handle user messages in Snackbar
    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    // Handle external intent (e.g. magnet link opened from browser)
    LaunchedEffect(initialAddMagnetOrFile) {
        if (!initialAddMagnetOrFile.isNullOrEmpty()) {
            if (initialAddMagnetOrFile.startsWith("magnet:") || initialAddMagnetOrFile.startsWith("http")) {
                viewModel.addTorrent(filenameOrMagnet = initialAddMagnetOrFile)
            } else {
                showAddDialog = true
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (!isTablet) {
                // Phone NavigationBar
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavTab.entries.forEach { tab ->
                        val isSelected = selectedTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { selectedTab = tab },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.titleRu,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = tab.titleRu,
                                    fontSize = 11.sp
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        if (isTablet) {
            // Tablet layout with NavigationRail on start edge
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    header = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(top = 16.dp, bottom = 12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = "Transmify",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Transmify",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            FloatingActionButton(
                                onClick = { showAddDialog = true },
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Добавить торрент")
                            }
                        }
                    }
                ) {
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            NavTab.entries.forEach { tab ->
                                val isSelected = selectedTab == tab
                                NavigationRailItem(
                                    selected = isSelected,
                                    onClick = { selectedTab = tab },
                                    icon = {
                                        Icon(
                                            imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.titleRu,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.titleRu,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }

                        // Footer actions in NavigationRail
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            val altSpeedActive = sessionInfo.altSpeedEnabled
                            IconButton(
                                onClick = { viewModel.toggleAltSpeed() }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = "Ограничение скорости (Alt Speed)",
                                    tint = if (altSpeedActive) VibrantAmber else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (connectionError == null && activeServer != null) NeonEmerald else RadiantRed)
                            )
                        }
                    }
                }

                VerticalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )

                // Main tablet content pane
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    TabletScreenContent(
                        selectedTab = selectedTab,
                        isSplitPane = isSplitPane,
                        viewModel = viewModel,
                        activeServer = activeServer,
                        servers = servers,
                        torrents = torrents,
                        sessionStats = sessionStats,
                        sessionInfo = sessionInfo,
                        selectedTorrent = selectedTorrent,
                        activeFilter = activeFilter,
                        searchQuery = searchQuery,
                        activeSort = activeSort,
                        isRefreshing = isRefreshing,
                        connectionError = connectionError,
                        themeMode = themeMode,
                        notificationsEnabled = notificationsEnabled,
                        rutrackerTopics = rutrackerTopics,
                        rutrackerQuery = rutrackerQuery,
                        rutrackerCategory = rutrackerCategory,
                        rutrackerLoading = rutrackerLoading,
                        rutrackerMirror = rutrackerMirror,
                        recentSearches = recentSearches,
                        onOpenAddDialog = { showAddDialog = true },
                        onNavigateToRutracker = { selectedTab = NavTab.RUTRACKER },
                        onNavigateToServers = { selectedTab = NavTab.SETTINGS }
                    )
                }
            }
        } else {
            // Standard Phone Content Layout
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                when (selectedTab) {
                    NavTab.TORRENTS -> {
                        TorrentListScreen(
                            torrents = torrents,
                            activeServer = activeServer,
                            sessionStats = sessionStats,
                            sessionInfo = sessionInfo,
                            activeFilter = activeFilter,
                            searchQuery = searchQuery,
                            activeSort = activeSort,
                            isRefreshing = isRefreshing,
                            connectionError = connectionError,
                            onFilterChange = viewModel::setFilter,
                            onSearchChange = viewModel::setSearchQuery,
                            onSortChange = viewModel::setSort,
                            onRefresh = { viewModel.refreshData(silent = false) },
                            onTorrentClick = viewModel::selectTorrent,
                            onStartTorrent = viewModel::startTorrent,
                            onStopTorrent = viewModel::stopTorrent,
                            onRemoveTorrent = viewModel::removeTorrent,
                            onStartAll = viewModel::startAllTorrents,
                            onStopAll = viewModel::stopAllTorrents,
                            onToggleAltSpeed = viewModel::toggleAltSpeed,
                            onOpenAddDialog = { showAddDialog = true },
                            onNavigateToRutracker = { selectedTab = NavTab.RUTRACKER },
                            onNavigateToServers = { selectedTab = NavTab.SETTINGS },
                            selectedTorrentId = selectedTorrent?.id
                        )
                    }
                    NavTab.RUTRACKER -> {
                        RutrackerScreen(
                            topics = rutrackerTopics,
                            query = rutrackerQuery,
                            category = rutrackerCategory,
                            isLoading = rutrackerLoading,
                            mirrorUrl = rutrackerMirror,
                            recentSearches = recentSearches,
                            onSearch = viewModel::searchRutracker,
                            onDownloadTopic = viewModel::downloadRutrackerTopic,
                            onAddTorrentLink = { link ->
                                viewModel.addTorrentFromRutracker(url = link)
                            },
                            onUpdateMirror = viewModel::setRutrackerMirror
                        )
                    }
                    NavTab.STATS -> {
                        SeedingStatsScreen(
                            sessionStats = sessionStats,
                            sessionInfo = sessionInfo,
                            torrents = torrents,
                            activeServer = activeServer,
                            onToggleAltSpeed = viewModel::toggleAltSpeed,
                            onRefresh = { viewModel.refreshData(silent = false) }
                        )
                    }
                    NavTab.SETTINGS -> {
                        ServersSettingsScreen(
                            servers = servers,
                            activeServer = activeServer,
                            themeMode = themeMode,
                            notificationsEnabled = notificationsEnabled,
                            rutrackerMirror = rutrackerMirror,
                            onSelectServer = viewModel::switchActiveServer,
                            onSaveServer = viewModel::saveServer,
                            onDeleteServer = viewModel::deleteServer,
                            onTestConnection = viewModel::testConnection,
                            onThemeModeChange = viewModel::setThemeMode,
                            onNotificationsChange = viewModel::setNotificationsEnabled,
                            onSendTestNotification = viewModel::testNotification,
                            onUpdateRutrackerMirror = viewModel::setRutrackerMirror
                        )
                    }
                }
            }
        }
    }

    // Torrent detail sheet (shown on Phone or when split pane is not active)
    if (!isSplitPane && selectedTorrent != null) {
        selectedTorrent?.let { torrent ->
            TorrentDetailSheet(
                torrent = torrent,
                onDismiss = { viewModel.selectTorrent(null) },
                onStart = viewModel::startTorrent,
                onStop = viewModel::stopTorrent,
                onRemove = viewModel::removeTorrent,
                onToggleFileWanted = viewModel::toggleFileWanted
            )
        }
    }

    // Add torrent dialog
    if (showAddDialog) {
        AddTorrentDialog(
            onDismiss = { showAddDialog = false },
            onAddMagnetOrUrl = { url, dir, paused ->
                viewModel.addTorrent(filenameOrMagnet = url, downloadDir = dir, paused = paused)
            },
            onAddFile = { uri, dir, paused ->
                viewModel.addTorrentFromUri(uri = uri, downloadDir = dir, paused = paused)
            },
            defaultDownloadDir = sessionInfo.downloadDir
        )
    }
}

@Composable
private fun TabletScreenContent(
    selectedTab: NavTab,
    isSplitPane: Boolean,
    viewModel: TransmifyViewModel,
    activeServer: com.example.data.db.ServerEntity?,
    servers: List<com.example.data.db.ServerEntity>,
    torrents: List<com.example.data.model.TorrentItem>,
    sessionStats: com.example.data.model.SessionStats,
    sessionInfo: com.example.data.model.SessionInfo,
    selectedTorrent: com.example.data.model.TorrentItem?,
    activeFilter: com.example.ui.TorrentFilter,
    searchQuery: String,
    activeSort: com.example.ui.TorrentSort,
    isRefreshing: Boolean,
    connectionError: String?,
    themeMode: com.example.ui.theme.ThemeMode,
    notificationsEnabled: Boolean,
    rutrackerTopics: List<com.example.data.model.RutrackerTopic>,
    rutrackerQuery: String,
    rutrackerCategory: Int,
    rutrackerLoading: Boolean,
    rutrackerMirror: String,
    recentSearches: List<SearchHistoryEntity>,
    onOpenAddDialog: () -> Unit,
    onNavigateToRutracker: () -> Unit,
    onNavigateToServers: () -> Unit
) {
    when (selectedTab) {
        NavTab.TORRENTS -> {
            if (isSplitPane) {
                // Canonical Master-Detail layout
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(0.44f)
                            .fillMaxHeight()
                    ) {
                        TorrentListScreen(
                            torrents = torrents,
                            activeServer = activeServer,
                            sessionStats = sessionStats,
                            sessionInfo = sessionInfo,
                            activeFilter = activeFilter,
                            searchQuery = searchQuery,
                            activeSort = activeSort,
                            isRefreshing = isRefreshing,
                            connectionError = connectionError,
                            onFilterChange = viewModel::setFilter,
                            onSearchChange = viewModel::setSearchQuery,
                            onSortChange = viewModel::setSort,
                            onRefresh = { viewModel.refreshData(silent = false) },
                            onTorrentClick = viewModel::selectTorrent,
                            onStartTorrent = viewModel::startTorrent,
                            onStopTorrent = viewModel::stopTorrent,
                            onRemoveTorrent = viewModel::removeTorrent,
                            onStartAll = viewModel::startAllTorrents,
                            onStopAll = viewModel::stopAllTorrents,
                            onToggleAltSpeed = viewModel::toggleAltSpeed,
                            onOpenAddDialog = onOpenAddDialog,
                            onNavigateToRutracker = onNavigateToRutracker,
                            onNavigateToServers = onNavigateToServers,
                            selectedTorrentId = selectedTorrent?.id
                        )
                    }

                    VerticalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )

                    // Right Inspector / Overview Pane
                    Box(
                        modifier = Modifier
                            .weight(0.56f)
                            .fillMaxHeight()
                    ) {
                        if (selectedTorrent != null) {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                TorrentDetailContent(
                                    torrent = selectedTorrent,
                                    onStart = viewModel::startTorrent,
                                    onStop = viewModel::stopTorrent,
                                    onRemove = viewModel::removeTorrent,
                                    onToggleFileWanted = viewModel::toggleFileWanted,
                                    onClose = { viewModel.selectTorrent(null) },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(top = 16.dp, bottom = 16.dp)
                                )
                            }
                        } else {
                            TabletOverviewPane(
                                activeServer = activeServer,
                                sessionStats = sessionStats,
                                sessionInfo = sessionInfo,
                                torrents = torrents,
                                connectionError = connectionError,
                                onOpenAddDialog = onOpenAddDialog,
                                onNavigateToRutracker = onNavigateToRutracker,
                                onStartAll = viewModel::startAllTorrents,
                                onStopAll = viewModel::stopAllTorrents,
                                onToggleAltSpeed = viewModel::toggleAltSpeed
                            )
                        }
                    }
                }
            } else {
                // Portrait tablet
                TorrentListScreen(
                    torrents = torrents,
                    activeServer = activeServer,
                    sessionStats = sessionStats,
                    sessionInfo = sessionInfo,
                    activeFilter = activeFilter,
                    searchQuery = searchQuery,
                    activeSort = activeSort,
                    isRefreshing = isRefreshing,
                    connectionError = connectionError,
                    onFilterChange = viewModel::setFilter,
                    onSearchChange = viewModel::setSearchQuery,
                    onSortChange = viewModel::setSort,
                    onRefresh = { viewModel.refreshData(silent = false) },
                    onTorrentClick = viewModel::selectTorrent,
                    onStartTorrent = viewModel::startTorrent,
                    onStopTorrent = viewModel::stopTorrent,
                    onRemoveTorrent = viewModel::removeTorrent,
                    onStartAll = viewModel::startAllTorrents,
                    onStopAll = viewModel::stopAllTorrents,
                    onToggleAltSpeed = viewModel::toggleAltSpeed,
                    onOpenAddDialog = onOpenAddDialog,
                    onNavigateToRutracker = onNavigateToRutracker,
                    onNavigateToServers = onNavigateToServers,
                    selectedTorrentId = selectedTorrent?.id
                )
            }
        }
        NavTab.RUTRACKER -> {
            RutrackerScreen(
                topics = rutrackerTopics,
                query = rutrackerQuery,
                category = rutrackerCategory,
                isLoading = rutrackerLoading,
                mirrorUrl = rutrackerMirror,
                recentSearches = recentSearches,
                onSearch = viewModel::searchRutracker,
                onDownloadTopic = viewModel::downloadRutrackerTopic,
                onAddTorrentLink = { link ->
                    viewModel.addTorrentFromRutracker(url = link)
                },
                onUpdateMirror = viewModel::setRutrackerMirror
            )
        }
        NavTab.STATS -> {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(modifier = Modifier.widthIn(max = 960.dp).fillMaxSize()) {
                    SeedingStatsScreen(
                        sessionStats = sessionStats,
                        sessionInfo = sessionInfo,
                        torrents = torrents,
                        activeServer = activeServer,
                        onToggleAltSpeed = viewModel::toggleAltSpeed,
                        onRefresh = { viewModel.refreshData(silent = false) }
                    )
                }
            }
        }
        NavTab.SETTINGS -> {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(modifier = Modifier.widthIn(max = 900.dp).fillMaxSize()) {
                    ServersSettingsScreen(
                        servers = servers,
                        activeServer = activeServer,
                        themeMode = themeMode,
                        notificationsEnabled = notificationsEnabled,
                        rutrackerMirror = rutrackerMirror,
                        onSelectServer = viewModel::switchActiveServer,
                        onSaveServer = viewModel::saveServer,
                        onDeleteServer = viewModel::deleteServer,
                        onTestConnection = viewModel::testConnection,
                        onThemeModeChange = viewModel::setThemeMode,
                        onNotificationsChange = viewModel::setNotificationsEnabled,
                        onSendTestNotification = viewModel::testNotification,
                        onUpdateRutrackerMirror = viewModel::setRutrackerMirror
                    )
                }
            }
        }
    }
}
