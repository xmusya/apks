package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.model.TorrentFileItem
import com.example.data.model.TorrentItem
import com.example.data.model.TorrentStatus
import com.example.ui.theme.CyberViolet
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.RadiantRed
import com.example.ui.theme.SkyBlue
import com.example.ui.theme.VibrantAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TorrentDetailSheet(
    torrent: TorrentItem,
    onDismiss: () -> Unit,
    onStart: (Long) -> Unit,
    onStop: (Long) -> Unit,
    onRemove: (id: Long, deleteLocalData: Boolean) -> Unit,
    onToggleFileWanted: (torrentId: Long, fileIndex: Int, wanted: Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        TorrentDetailContent(
            torrent = torrent,
            onStart = onStart,
            onStop = onStop,
            onRemove = onRemove,
            onToggleFileWanted = onToggleFileWanted,
            onClose = onDismiss,
            modifier = Modifier.padding(bottom = 32.dp)
        )
    }
}

@Composable
fun TorrentDetailContent(
    torrent: TorrentItem,
    onStart: (Long) -> Unit,
    onStop: (Long) -> Unit,
    onRemove: (id: Long, deleteLocalData: Boolean) -> Unit,
    onToggleFileWanted: (torrentId: Long, fileIndex: Int, wanted: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteLocalData by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        // Title & Status Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = torrent.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                StatusBadge(torrent.status)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Delete Action
                IconButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Удалить торрент",
                        tint = RadiantRed
                    )
                }
                if (onClose != null) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.padding(start = 2.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Закрыть",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Progress bar and %
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${(torrent.percentDone * 100).toInt()}% завершено",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp
                )
                Text(
                    text = "${torrent.formattedDownloaded} из ${torrent.formattedTotalSize}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { torrent.percentDone },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (torrent.percentDone >= 1.0f) NeonEmerald else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Primary Quick Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (torrent.status == TorrentStatus.STOPPED) {
                Button(
                    onClick = { onStart(torrent.id) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Возобновить", fontWeight = FontWeight.Bold)
                }
            } else {
                FilledTonalButton(
                    onClick = { onStop(torrent.id) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = VibrantAmber.copy(alpha = 0.2f), contentColor = VibrantAmber)
                ) {
                    Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Поставить на паузу", fontWeight = FontWeight.Bold)
                }
            }

            OutlinedButton(
                onClick = { showDeleteDialog = true },
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp), tint = RadiantRed)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Удалить", color = RadiantRed)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tab row (Обзор / Файлы)
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.clip(RoundedCornerShape(16.dp))
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Статистика и обзор") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Файлы (${torrent.files.size})") }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            if (selectedTab == 0) {
                OverviewTab(torrent)
            } else {
                FilesTab(
                    torrent = torrent,
                    onToggleFileWanted = { index, wanted ->
                        onToggleFileWanted(torrent.id, index, wanted)
                    }
                )
            }
        }
    }

    // Confirmation Delete Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удаление торрента") },
            text = {
                Column {
                    Text("Вы действительно хотите удалить торрент \"${torrent.name}\"?")
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { deleteLocalData = !deleteLocalData },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = deleteLocalData,
                            onCheckedChange = { deleteLocalData = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Удалить также скачанные файлы с диска", color = RadiantRed, fontSize = 14.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRemove(torrent.id, deleteLocalData)
                        showDeleteDialog = false
                        onClose?.invoke()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RadiantRed)
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun StatusBadge(status: TorrentStatus) {
    val (bg, fg) = when (status) {
        TorrentStatus.DOWNLOAD, TorrentStatus.DOWNLOAD_WAIT -> SkyBlue.copy(alpha = 0.15f) to SkyBlue
        TorrentStatus.SEED, TorrentStatus.SEED_WAIT -> NeonEmerald.copy(alpha = 0.15f) to NeonEmerald
        TorrentStatus.STOPPED -> VibrantAmber.copy(alpha = 0.15f) to VibrantAmber
        TorrentStatus.CHECK, TorrentStatus.CHECK_WAIT -> CyberViolet.copy(alpha = 0.15f) to CyberViolet
        TorrentStatus.UNKNOWN -> Color.Gray.copy(alpha = 0.15f) to Color.Gray
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(fg)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = status.labelRu,
                color = fg,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun OverviewTab(torrent: TorrentItem) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Transfer Speed Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                title = "Скорость загрузки",
                value = torrent.formattedDownloadSpeed,
                icon = Icons.Default.ArrowDownward,
                iconColor = SkyBlue,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Скорость отдачи",
                value = torrent.formattedUploadSpeed,
                icon = Icons.Default.ArrowUpward,
                iconColor = NeonEmerald,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                title = "Сидеры (раздают)",
                value = "${torrent.peersSendingToUs} из ${torrent.peersConnected}",
                icon = Icons.Default.People,
                iconColor = NeonEmerald,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Личеры (качают)",
                value = "${torrent.peersGettingFromUs} из ${torrent.peersConnected}",
                icon = Icons.Default.Upload,
                iconColor = SkyBlue,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                title = "Рейтинг раздачи",
                value = torrent.formattedRatio,
                icon = Icons.Default.Speed,
                iconColor = CyberViolet,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Оставшееся время (ETA)",
                value = torrent.formattedEta,
                icon = Icons.Default.Schedule,
                iconColor = VibrantAmber,
                modifier = Modifier.weight(1f)
            )
        }

        // Details list
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailRow("Сидеры (источники)", "${torrent.peersSendingToUs} активно передают данные")
                DetailRow("Личеры (клиенты)", "${torrent.peersGettingFromUs} скачивают у нас")
                DetailRow("Всего подключено пиров", "${torrent.peersConnected}")
                DetailRow("Роздано всего", torrent.formattedTotalSize.let { com.example.data.model.formatBytes(torrent.uploadedEver) })
                DetailRow("Скачано всего", com.example.data.model.formatBytes(torrent.downloadedEver))
                if (torrent.downloadDir.isNotEmpty()) {
                    DetailRow("Путь сохранения", torrent.downloadDir)
                }
                if (torrent.addedDate > 0) {
                    val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(torrent.addedDate))
                    DetailRow("Дата добавления", dateStr)
                }
                if (torrent.hashString.isNotEmpty()) {
                    DetailRow("Хэш", torrent.hashString.take(16) + "...")
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun FilesTab(
    torrent: TorrentItem,
    onToggleFileWanted: (fileIndex: Int, wanted: Boolean) -> Unit
) {
    if (torrent.files.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Список файлов формируется...",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(torrent.files, key = { it.index }) { file ->
                FileItemRow(file = file, onToggleWanted = { wanted ->
                    onToggleFileWanted(file.index, wanted)
                })
            }
        }
    }
}

@Composable
private fun FileItemRow(
    file: TorrentFileItem,
    onToggleWanted: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = file.wanted,
                onCheckedChange = { onToggleWanted(it) },
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (file.wanted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = file.formattedSize,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = file.formattedProgress,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (file.progress >= 1f) NeonEmerald else ElectricCyan
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { file.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = if (file.progress >= 1f) NeonEmerald else ElectricCyan,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}
