package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.ServerEntity
import com.example.data.model.formatBytes
import com.example.data.model.formatSpeed
import com.example.data.model.SessionInfo
import com.example.data.model.SessionStats
import com.example.data.model.TorrentItem
import com.example.data.model.TorrentStatus
import com.example.ui.theme.CyberViolet
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.RadiantRed
import com.example.ui.theme.SkyBlue
import com.example.ui.theme.VibrantAmber

@Composable
fun TabletOverviewPane(
    activeServer: ServerEntity?,
    sessionStats: SessionStats,
    sessionInfo: SessionInfo,
    torrents: List<TorrentItem>,
    connectionError: String?,
    onOpenAddDialog: () -> Unit,
    onNavigateToRutracker: () -> Unit,
    onStartAll: () -> Unit,
    onStopAll: () -> Unit,
    onToggleAltSpeed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val downloadingCount = torrents.count { it.status.isDownloading }
    val seedingCount = torrents.count { it.status.isSeeding }
    val pausedCount = torrents.count { it.status == TorrentStatus.STOPPED }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Server header card
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(if (connectionError == null && activeServer != null) NeonEmerald.copy(alpha = 0.15f) else RadiantRed.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (connectionError == null && activeServer != null) Icons.Default.Dns else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (connectionError == null && activeServer != null) NeonEmerald else RadiantRed,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = activeServer?.name ?: "Transmission Демон",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (connectionError == null && activeServer != null) NeonEmerald.copy(alpha = 0.2f) else RadiantRed.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (connectionError == null && activeServer != null) "Онлайн" else "Офлайн",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (connectionError == null && activeServer != null) NeonEmerald else RadiantRed
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Хост: ${activeServer?.displayUrl ?: "Не настроен"} • Версия: ${sessionInfo.version.ifEmpty { "Transmission RPC" }}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Live Speed Metrics
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            MetricCard(
                title = "Скорость загрузки",
                value = formatSpeed(sessionStats.downloadSpeed),
                subtitle = "За сессию: ${formatBytes(sessionStats.sessionDownload)}",
                icon = Icons.Default.ArrowDownward,
                iconColor = SkyBlue,
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                title = "Скорость отдачи",
                value = formatSpeed(sessionStats.uploadSpeed),
                subtitle = "За сессию: ${formatBytes(sessionStats.sessionUpload)}",
                icon = Icons.Default.ArrowUpward,
                iconColor = NeonEmerald,
                modifier = Modifier.weight(1f)
            )
        }

        // Distribution stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            MetricCard(
                title = "Рейтинг раздачи (Ratio)",
                value = sessionStats.formattedSessionRatio,
                subtitle = "Всего торрентов: ${torrents.size}",
                icon = Icons.Default.Speed,
                iconColor = CyberViolet,
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                title = "Статус раздач",
                value = "$downloadingCount качается • $seedingCount раздается",
                subtitle = "$pausedCount на паузе",
                icon = Icons.Default.CheckCircle,
                iconColor = VibrantAmber,
                modifier = Modifier.weight(1f)
            )
        }

        // Quick Actions
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Быстрые действия",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onOpenAddDialog,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Добавить торрент", fontWeight = FontWeight.Bold)
                    }

                    FilledTonalButton(
                        onClick = onNavigateToRutracker,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Поиск на RuTracker")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onStartAll,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Запустить все")
                    }

                    OutlinedButton(
                        onClick = onStopAll,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Остановить все")
                    }
                }
            }
        }

        // Inspection Hint
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "💡", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Нажмите на любую раздачу в списке слева, чтобы здесь открылась подробная панель: прогресс, сидеры/пиры, рейтинг, управление файлами и приоритетами скачивания.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
