package com.example.data.model

import java.util.Locale

enum class TorrentStatus(val code: Int, val labelRu: String, val labelEn: String) {
    STOPPED(0, "Приостановлен", "Paused"),
    CHECK_WAIT(1, "Ожидает проверки", "Wait to check"),
    CHECK(2, "Проверка данных", "Verifying"),
    DOWNLOAD_WAIT(3, "В очереди", "Queued download"),
    DOWNLOAD(4, "Загрузка", "Downloading"),
    SEED_WAIT(5, "Очередь раздачи", "Queued seed"),
    SEED(6, "Раздача", "Seeding"),
    UNKNOWN(-1, "Неизвестно", "Unknown");

    companion object {
        fun fromCode(code: Int): TorrentStatus =
            entries.find { it.code == code } ?: UNKNOWN
    }

    val isActive: Boolean
        get() = this == DOWNLOAD || this == SEED || this == CHECK

    val isSeeding: Boolean
        get() = this == SEED || this == SEED_WAIT

    val isDownloading: Boolean
        get() = this == DOWNLOAD || this == DOWNLOAD_WAIT
}

data class TorrentFileItem(
    val index: Int,
    val name: String,
    val bytesCompleted: Long,
    val length: Long,
    val wanted: Boolean = true,
    val priority: Int = 0 // -1: Low, 0: Normal, 1: High
) {
    val progress: Float
        get() = if (length > 0) (bytesCompleted.toFloat() / length.toFloat()).coerceIn(0f, 1f) else 0f

    val formattedSize: String
        get() = formatBytes(length)

    val formattedProgress: String
        get() = "${(progress * 100).toInt()}%"
}

data class TorrentItem(
    val id: Long,
    val name: String,
    val status: TorrentStatus,
    val totalSize: Long,
    val percentDone: Float,
    val rateDownload: Long, // bytes/sec
    val rateUpload: Long,   // bytes/sec
    val uploadRatio: Float,
    val uploadedEver: Long,
    val downloadedEver: Long,
    val leftUntilDone: Long,
    val eta: Long, // seconds (-1 or -2 if unknown)
    val peersConnected: Int,
    val peersSendingToUs: Int,
    val peersGettingFromUs: Int,
    val error: Int = 0,
    val errorString: String = "",
    val addedDate: Long = 0L,
    val doneDate: Long = 0L,
    val isFinished: Boolean = false,
    val downloadDir: String = "",
    val hashString: String = "",
    val files: List<TorrentFileItem> = emptyList()
) {
    val formattedTotalSize: String
        get() = formatBytes(totalSize)

    val formattedDownloaded: String
        get() = formatBytes((totalSize * percentDone).toLong())

    val formattedDownloadSpeed: String
        get() = formatSpeed(rateDownload)

    val formattedUploadSpeed: String
        get() = formatSpeed(rateUpload)

    val formattedRatio: String
        get() = String.format(Locale.US, "%.2f", uploadRatio)

    val formattedEta: String
        get() = formatEtaDuration(eta)
}

data class SessionStats(
    val activeTorrentCount: Int = 0,
    val downloadSpeed: Long = 0L,
    val uploadSpeed: Long = 0L,
    val torrentCount: Int = 0,
    val pausedTorrentCount: Int = 0,
    val cumulativeUpload: Long = 0L,
    val cumulativeDownload: Long = 0L,
    val cumulativeSecondsActive: Long = 0L,
    val sessionUpload: Long = 0L,
    val sessionDownload: Long = 0L,
    val sessionSecondsActive: Long = 0L
) {
    val cumulativeRatio: Float
        get() = if (cumulativeDownload > 0) cumulativeUpload.toFloat() / cumulativeDownload.toFloat() else 0f

    val sessionRatio: Float
        get() = if (sessionDownload > 0) sessionUpload.toFloat() / sessionDownload.toFloat() else 0f

    val formattedCumulativeRatio: String
        get() = String.format(Locale.US, "%.2f", cumulativeRatio)

    val formattedSessionRatio: String
        get() = String.format(Locale.US, "%.2f", sessionRatio)
}

data class SessionInfo(
    val version: String = "4.0.5",
    val rpcVersion: Int = 17,
    val downloadDir: String = "/downloads",
    val altSpeedEnabled: Boolean = false,
    val altSpeedDown: Long = 50, // KB/s
    val altSpeedUp: Long = 50,   // KB/s
    val speedLimitDownEnabled: Boolean = false,
    val speedLimitDown: Long = 0,
    val speedLimitUpEnabled: Boolean = false,
    val speedLimitUp: Long = 0
)

fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB", "PB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    val groupIndex = digitGroups.coerceIn(0, units.size - 1)
    val value = bytes / Math.pow(1024.0, groupIndex.toDouble())
    return String.format(Locale.US, "%.1f %s", value, units[groupIndex])
}

fun formatSpeed(bytesPerSec: Long): String {
    if (bytesPerSec <= 0) return "0 B/s"
    val units = arrayOf("B/s", "KB/s", "MB/s", "GB/s")
    val digitGroups = (Math.log10(bytesPerSec.toDouble()) / Math.log10(1024.0)).toInt()
    val groupIndex = digitGroups.coerceIn(0, units.size - 1)
    val value = bytesPerSec / Math.pow(1024.0, groupIndex.toDouble())
    return String.format(Locale.US, "%.1f %s", value, units[groupIndex])
}

fun formatEtaDuration(seconds: Long): String {
    if (seconds < 0) return "∞"
    if (seconds == 0L) return "0с"
    val days = seconds / 86400
    val hours = (seconds % 86400) / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60

    return when {
        days > 0 -> "${days}д ${hours}ч"
        hours > 0 -> "${hours}ч ${minutes}м"
        minutes > 0 -> "${minutes}м ${secs}с"
        else -> "${secs}с"
    }
}
