package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.ServerEntity
import com.example.data.model.SessionInfo
import com.example.data.model.SessionStats
import com.example.data.model.TorrentItem
import com.example.data.model.TorrentStatus
import com.example.data.model.formatSpeed
import com.example.ui.TorrentFilter
import com.example.ui.TorrentSort
import com.example.ui.theme.CyberViolet
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.RadiantRed
import com.example.ui.theme.SkyBlue
import com.example.ui.theme.VibrantAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TorrentListScreen(
    torrents: List<TorrentItem>,
    activeServer: ServerEntity?,
    sessionStats: SessionStats,
    sessionInfo: SessionInfo,
    activeFilter: TorrentFilter,
    searchQuery: String,
    activeSort: TorrentSort,
    isRefreshing: Boolean,
    connectionError: String? = null,
    onFilterChange: (TorrentFilter) -> Unit,
    onSearchChange: (String) -> Unit,
    onSortChange: (TorrentSort) -> Unit,
    onRefresh: () -> Unit,
    onTorrentClick: (TorrentItem) -> Unit,
    onStartTorrent: (Long) -> Unit,
    onStopTorrent: (Long) -> Unit,
    onRemoveTorrent: (Long, Boolean) -> Unit,
    onStartAll: () -> Unit,
    onStopAll: () -> Unit,
    onToggleAltSpeed: () -> Unit,
    onOpenAddDialog: () -> Unit,
    onNavigateToRutracker: () -> Unit,
    onNavigateToServers: () -> Unit,
    selectedTorrentId: Long? = null
) {
    var showMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var torrentToDelete by remember { mutableStateOf<TorrentItem?>(null) }
    var deleteWithData by remember { mutableStateOf(false) }

    // Filter and sort the torrent list
    val displayedTorrents = remember(torrents, activeFilter, searchQuery, activeSort) {
        var list = torrents

        // 1. Filter by status
        list = when (activeFilter) {
            TorrentFilter.ALL -> list
            TorrentFilter.DOWNLOADING -> list.filter { it.status.isDownloading }
            TorrentFilter.SEEDING -> list.filter { it.status.isSeeding }
            TorrentFilter.PAUSED -> list.filter { it.status == TorrentStatus.STOPPED }
            TorrentFilter.FINISHED -> list.filter { it.isFinished || it.percentDone >= 1.0f }
        }

        // 2. Search query filter
        if (searchQuery.isNotBlank()) {
            list = list.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }

        // 3. Sorting
        when (activeSort) {
            TorrentSort.DATE -> list.sortedByDescending { it.addedDate }
            TorrentSort.NAME -> list.sortedBy { it.name.lowercase() }
            TorrentSort.PROGRESS -> list.sortedByDescending { it.percentDone }
            TorrentSort.SPEED_DOWN -> list.sortedByDescending { it.rateDownload }
            TorrentSort.SPEED_UP -> list.sortedByDescending { it.rateUpload }
            TorrentSort.SIZE -> list.sortedByDescending { it.totalSize }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { onNavigateToServers() }
                        ) {
                            Icon(
                                Icons.Default.Dns,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = activeServer?.name ?: "Нет активного сервера",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = if (activeServer?.isDemoMode == true) "Песочница • Локальная эмуляция" else activeServer?.displayUrl ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    // Turtle mode (Alt speed limit) toggle button
                    IconButton(
                        onClick = onToggleAltSpeed,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (sessionInfo.altSpeedEnabled) VibrantAmber.copy(alpha = 0.2f)
                                else Color.Transparent
                            )
                    ) {
                        Text(
                            text = "🐢",
                            fontSize = 20.sp
                        )
                    }

                    // Refresh Button
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Обновить")
                    }

                    // Overflow Menu
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Меню")
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Запустить все") },
                            leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                            onClick = {
                                onStartAll()
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Приостановить все") },
                            leadingIcon = { Icon(Icons.Default.Pause, contentDescription = null) },
                            onClick = {
                                onStopAll()
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Сортировка...") },
                            leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                showSortMenu = true
                            }
                        )
                    }

                    // Sort menu
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        TorrentSort.entries.forEach { sort ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = sort.titleRu,
                                        fontWeight = if (activeSort == sort) FontWeight.Bold else FontWeight.Normal,
                                        color = if (activeSort == sort) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    onSortChange(sort)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onOpenAddDialog,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Добавить торрент", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Speed Banner Ticker
            SpeedBanner(sessionStats = sessionStats, torrents = torrents)

            // Server connection error banner if Transmission doesn't respond
            if (!connectionError.isNullOrBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = RadiantRed.copy(alpha = 0.15f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RadiantRed.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = RadiantRed,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Сервер Transmission не отвечает",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Проверьте IP-адрес (${activeServer?.displayUrl ?: "не указан"}), порт и статус демона.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onNavigateToServers,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RadiantRed),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("Настроить IP", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Search Bar
            PaddingSearchField(
                query = searchQuery,
                onQueryChange = onSearchChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Filter Chips Horizontal Row
            FilterChipsRow(
                activeFilter = activeFilter,
                onFilterSelected = onFilterChange,
                counts = mapOf(
                    TorrentFilter.ALL to torrents.size,
                    TorrentFilter.DOWNLOADING to torrents.count { it.status.isDownloading },
                    TorrentFilter.SEEDING to torrents.count { it.status.isSeeding },
                    TorrentFilter.PAUSED to torrents.count { it.status == TorrentStatus.STOPPED },
                    TorrentFilter.FINISHED to torrents.count { it.isFinished || it.percentDone >= 1.0f }
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Torrents List
            if (displayedTorrents.isEmpty()) {
                EmptyTorrentsView(
                    isSearchActive = searchQuery.isNotBlank(),
                    onAddClick = onOpenAddDialog,
                    onRutrackerClick = onNavigateToRutracker
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(displayedTorrents, key = { it.id }) { torrent ->
                        TorrentItemCard(
                            torrent = torrent,
                            onClick = { onTorrentClick(torrent) },
                            onStart = { onStartTorrent(torrent.id) },
                            onStop = { onStopTorrent(torrent.id) },
                            onDelete = { torrentToDelete = torrent },
                            isSelected = torrent.id == selectedTorrentId
                        )
                    }
                }
            }
        }
    }

    // Delete single torrent dialog
    torrentToDelete?.let { torrent ->
        AlertDialog(
            onDismissRequest = { torrentToDelete = null },
            title = { Text("Удалить торрент?") },
            text = {
                Column {
                    Text(
                        text = torrent.name,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { deleteWithData = !deleteWithData },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = deleteWithData,
                            onCheckedChange = { deleteWithData = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Удалить также скачанные файлы с диска",
                            color = RadiantRed,
                            fontSize = 14.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRemoveTorrent(torrent.id, deleteWithData)
                        torrentToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RadiantRed)
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { torrentToDelete = null }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun SpeedBanner(sessionStats: SessionStats, torrents: List<TorrentItem>) {
    val totalDl = if (sessionStats.downloadSpeed > 0) sessionStats.downloadSpeed else torrents.filter { it.status == TorrentStatus.DOWNLOAD }.sumOf { it.rateDownload }
    val totalUl = if (sessionStats.uploadSpeed > 0) sessionStats.uploadSpeed else torrents.sumOf { it.rateUpload }
    val activeCount = torrents.count { it.status.isActive }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Download Speed
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = SkyBlue,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = formatSpeed(totalDl),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Upload Speed
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.ArrowUpward,
                    contentDescription = null,
                    tint = NeonEmerald,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = formatSpeed(totalUl),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Active Torrents count
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (activeCount > 0) NeonEmerald else Color.Gray)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "$activeCount акт.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PaddingSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Поиск среди торрентов...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Очистить")
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = modifier
    )
}

@Composable
private fun FilterChipsRow(
    activeFilter: TorrentFilter,
    onFilterSelected: (TorrentFilter) -> Unit,
    counts: Map<TorrentFilter, Int>
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TorrentFilter.entries.forEach { filter ->
            val count = counts[filter] ?: 0
            val isSelected = activeFilter == filter

            FilterChip(
                selected = isSelected,
                onClick = { onFilterSelected(filter) },
                label = {
                    Text(
                        text = "${filter.titleRu} ($count)",
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                shape = RoundedCornerShape(14.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

@Composable
fun TorrentItemCard(
    torrent: TorrentItem,
    onClick: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onDelete: () -> Unit,
    isSelected: Boolean = false
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = if (isSelected) BorderStroke(2.dp, borderColor) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Name and Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = torrent.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                StatusBadge(status = torrent.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { torrent.percentDone },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (torrent.percentDone >= 1.0f) NeonEmerald else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Progress & Size Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${(torrent.percentDone * 100).toInt()}% • ${torrent.formattedDownloaded} / ${torrent.formattedTotalSize}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (torrent.status == TorrentStatus.DOWNLOAD) {
                    Text(
                        text = "Осталось: ${torrent.formattedEta}",
                        fontSize = 12.sp,
                        color = VibrantAmber,
                        fontWeight = FontWeight.Medium
                    )
                } else if (torrent.uploadRatio > 0f) {
                    Text(
                        text = "Рейтинг: ${torrent.formattedRatio}",
                        fontSize = 12.sp,
                        color = CyberViolet,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom Actions & Speeds Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Live Speeds
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (torrent.rateDownload > 0) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = SkyBlue, modifier = Modifier.size(14.dp))
                        Text(torrent.formattedDownloadSpeed, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SkyBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    if (torrent.rateUpload > 0) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(14.dp))
                        Text(torrent.formattedUploadSpeed, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonEmerald)
                    }
                    if (torrent.rateDownload == 0L && torrent.rateUpload == 0L) {
                        Text("Неактивен", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    }
                    if (torrent.peersConnected > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "👥 ${torrent.peersSendingToUs} / ${torrent.peersGettingFromUs} (${torrent.peersConnected})",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Quick Play/Pause & Delete
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (torrent.status == TorrentStatus.STOPPED) {
                        IconButton(onClick = onStart, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Запустить", tint = ElectricCyan)
                        }
                    } else {
                        IconButton(onClick = onStop, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Pause, contentDescription = "Пауза", tint = VibrantAmber)
                        }
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = RadiantRed.copy(alpha = 0.8f))
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyTorrentsView(
    isSearchActive: Boolean,
    onAddClick: () -> Unit,
    onRutrackerClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isSearchActive) Icons.Default.Search else Icons.Default.CloudDownload,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isSearchActive) "Ничего не найдено" else "Список торрентов пуст",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isSearchActive) "Попробуйте изменить поисковый запрос"
                else "Добавьте magnet-ссылку, .torrent файл или найдите раздачу на RuTracker",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            if (!isSearchActive) {
                Spacer(modifier = Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onAddClick,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Color.Black)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Добавить", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onRutrackerClick,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                    ) {
                        Text("RuTracker", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
