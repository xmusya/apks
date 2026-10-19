package com.example.data.remote

import android.util.Base64
import com.example.data.db.ServerEntity
import com.example.data.model.SessionInfo
import com.example.data.model.SessionStats
import com.example.data.model.TorrentFileItem
import com.example.data.model.TorrentItem
import com.example.data.model.TorrentStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class TransmissionRpcClient(private val server: ServerEntity) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    companion object {
        @Volatile
        private var cachedSessionId: String? = null

        // In-memory demo state for instant testing
        private var demoInitialized = false
        private val demoTorrents = mutableListOf<TorrentItem>()
        private var demoAltSpeedEnabled = false
        private var demoSessionStats = SessionStats(
            activeTorrentCount = 4,
            downloadSpeed = 8_450_000L,
            uploadSpeed = 2_150_000L,
            torrentCount = 5,
            pausedTorrentCount = 1,
            cumulativeUpload = 2_340_000_000_000L, // 2.34 TB
            cumulativeDownload = 1_120_000_000_000L, // 1.12 TB
            cumulativeSecondsActive = 3_450_000L,
            sessionUpload = 14_500_000_000L, // 14.5 GB
            sessionDownload = 28_200_000_000L, // 28.2 GB
            sessionSecondsActive = 18_400L
        )

        private fun initDemoDataIfNeeded() {
            if (demoInitialized) return
            demoInitialized = true
            demoTorrents.addAll(
                listOf(
                    TorrentItem(
                        id = 1L,
                        name = "Ubuntu 24.04.1 LTS Desktop (Noble Numbat) x86_64 ISO",
                        status = TorrentStatus.DOWNLOAD,
                        totalSize = 6_120_000_000L,
                        percentDone = 0.84f,
                        rateDownload = 5_840_000L, // 5.8 MB/s
                        rateUpload = 820_000L,    // 820 KB/s
                        uploadRatio = 0.42f,
                        uploadedEver = 2_100_000_000L,
                        downloadedEver = 5_140_800_000L,
                        leftUntilDone = 979_200_000L,
                        eta = 168L, // 2m 48s
                        peersConnected = 42,
                        peersSendingToUs = 18,
                        peersGettingFromUs = 6,
                        addedDate = System.currentTimeMillis() - 3600_000L,
                        downloadDir = "/downloads/iso",
                        hashString = "e2b1f83c18b7654a9d701ef5b8c3498a1b2c3d4e",
                        files = listOf(
                            TorrentFileItem(0, "ubuntu-24.04.1-desktop-amd64.iso", 5_140_000_000L, 6_118_000_000L, true),
                            TorrentFileItem(1, "SHA256SUMS", 800_000L, 800_000L, true),
                            TorrentFileItem(2, "SHA256SUMS.gpg", 1_200_000L, 1_200_000L, true)
                        )
                    ),
                    TorrentItem(
                        id = 2L,
                        name = "Blender Open Movie - Charge (4K HDR Master 60fps AC3)",
                        status = TorrentStatus.SEED,
                        totalSize = 3_450_000_000L,
                        percentDone = 1.0f,
                        rateDownload = 0L,
                        rateUpload = 1_280_000L, // 1.28 MB/s
                        uploadRatio = 3.65f,
                        uploadedEver = 12_592_500_000L,
                        downloadedEver = 3_450_000_000L,
                        leftUntilDone = 0L,
                        eta = -1L,
                        peersConnected = 28,
                        peersSendingToUs = 0,
                        peersGettingFromUs = 14,
                        addedDate = System.currentTimeMillis() - 86400_000L * 3,
                        doneDate = System.currentTimeMillis() - 86400_000L * 3 + 1200_000L,
                        isFinished = true,
                        downloadDir = "/downloads/media",
                        hashString = "7a4d9e01bc5832a89047bf38ca2d91834e7f601a",
                        files = listOf(
                            TorrentFileItem(0, "Charge_4K_ProRes_Master.mov", 3_200_000_000L, 3_200_000_000L, true),
                            TorrentFileItem(1, "Production_Art_Book.pdf", 250_000_000L, 250_000_000L, true)
                        )
                    ),
                    TorrentItem(
                        id = 3L,
                        name = "Arch Linux 2026.09.01 x86_64 Minimal Installation Image",
                        status = TorrentStatus.SEED,
                        totalSize = 980_000_000L,
                        percentDone = 1.0f,
                        rateDownload = 0L,
                        rateUpload = 450_000L,
                        uploadRatio = 5.21f,
                        uploadedEver = 5_105_800_000L,
                        downloadedEver = 980_000_000L,
                        leftUntilDone = 0L,
                        eta = -1L,
                        peersConnected = 19,
                        peersSendingToUs = 0,
                        peersGettingFromUs = 7,
                        addedDate = System.currentTimeMillis() - 86400_000L * 7,
                        isFinished = true,
                        downloadDir = "/downloads/iso",
                        hashString = "9988aabbccddeeff00112233445566778899aabb"
                    ),
                    TorrentItem(
                        id = 4L,
                        name = "Debian 12.6.0 'Bookworm' Netinst x86_64",
                        status = TorrentStatus.DOWNLOAD,
                        totalSize = 650_000_000L,
                        percentDone = 0.45f,
                        rateDownload = 2_610_000L,
                        rateUpload = 50_000L,
                        uploadRatio = 0.08f,
                        uploadedEver = 23_400_000L,
                        downloadedEver = 292_500_000L,
                        leftUntilDone = 357_500_000L,
                        eta = 137L,
                        peersConnected = 14,
                        peersSendingToUs = 9,
                        peersGettingFromUs = 2,
                        addedDate = System.currentTimeMillis() - 900_000L,
                        downloadDir = "/downloads/iso",
                        hashString = "11223344556677889900aabbccddeeff00112233"
                    ),
                    TorrentItem(
                        id = 5L,
                        name = "LibreOffice 24.8.1 Multilingual Suite Full Package",
                        status = TorrentStatus.STOPPED,
                        totalSize = 420_000_000L,
                        percentDone = 0.62f,
                        rateDownload = 0L,
                        rateUpload = 0L,
                        uploadRatio = 0.15f,
                        uploadedEver = 39_060_000L,
                        downloadedEver = 260_400_000L,
                        leftUntilDone = 159_600_000L,
                        eta = -1L,
                        peersConnected = 0,
                        peersSendingToUs = 0,
                        peersGettingFromUs = 0,
                        addedDate = System.currentTimeMillis() - 14400_000L,
                        downloadDir = "/downloads/software",
                        hashString = "f0e1d2c3b4a5968778695a4b3c2d1e0f1a2b3c4d"
                    )
                )
            )
        }
    }

    private val rpcUrl: String
        get() {
            val scheme = if (server.useHttps) "https" else "http"
            val path = if (server.path.startsWith("/")) server.path else "/${server.path}"
            return "$scheme://${server.host}:${server.port}$path"
        }

    suspend fun fetchTorrents(): Result<List<TorrentItem>> = withContext(Dispatchers.IO) {
        if (server.isDemoMode) {
            initDemoDataIfNeeded()
            // Increment demo progress slightly for realistic live feeling
            synchronized(demoTorrents) {
                for (i in demoTorrents.indices) {
                    val t = demoTorrents[i]
                    if (t.status == TorrentStatus.DOWNLOAD && t.percentDone < 1.0f) {
                        val addedBytes = (t.rateDownload * 2).coerceAtLeast(1_000_000L)
                        val newDownloaded = (t.downloadedEver + addedBytes).coerceAtMost(t.totalSize)
                        val newPercent = (newDownloaded.toFloat() / t.totalSize.toFloat()).coerceIn(0f, 1f)
                        val newEta = if (t.rateDownload > 0) ((t.totalSize - newDownloaded) / t.rateDownload).coerceAtLeast(0L) else 0L

                        if (newPercent >= 1.0f) {
                            demoTorrents[i] = t.copy(
                                percentDone = 1.0f,
                                status = TorrentStatus.SEED,
                                rateDownload = 0L,
                                rateUpload = 650_000L,
                                isFinished = true,
                                eta = -1L,
                                doneDate = System.currentTimeMillis()
                            )
                        } else {
                            demoTorrents[i] = t.copy(
                                percentDone = newPercent,
                                downloadedEver = newDownloaded,
                                leftUntilDone = t.totalSize - newDownloaded,
                                eta = newEta
                            )
                        }
                    }
                }
                return@withContext Result.success(demoTorrents.toList())
            }
        }

        try {
            val body = JSONObject().apply {
                put("method", "torrent-get")
                put("arguments", JSONObject().apply {
                    put("fields", JSONArray().apply {
                        put("id")
                        put("name")
                        put("status")
                        put("totalSize")
                        put("percentDone")
                        put("rateDownload")
                        put("rateUpload")
                        put("uploadRatio")
                        put("uploadedEver")
                        put("downloadedEver")
                        put("leftUntilDone")
                        put("eta")
                        put("peersConnected")
                        put("peersSendingToUs")
                        put("peersGettingFromUs")
                        put("error")
                        put("errorString")
                        put("addedDate")
                        put("doneDate")
                        put("isFinished")
                        put("downloadDir")
                        put("hashString")
                        put("files")
                        put("fileStats")
                    })
                })
                put("tag", 1)
            }

            val response = executeRpcRequest(body.toString())
            val json = JSONObject(response)
            if (json.optString("result") != "success") {
                return@withContext Result.failure(Exception("RPC error: ${json.optString("result")}"))
            }

            val arguments = json.getJSONObject("arguments")
            val torrentsArray = arguments.getJSONArray("torrents")
            val list = mutableListOf<TorrentItem>()

            for (i in 0 until torrentsArray.length()) {
                val t = torrentsArray.getJSONObject(i)
                val filesList = mutableListOf<TorrentFileItem>()
                val filesArr = t.optJSONArray("files")
                val statsArr = t.optJSONArray("fileStats")
                if (filesArr != null) {
                    for (f in 0 until filesArr.length()) {
                        val fileObj = filesArr.getJSONObject(f)
                        val statObj = statsArr?.optJSONObject(f)
                        filesList.add(
                            TorrentFileItem(
                                index = f,
                                name = fileObj.optString("name", "File #$f"),
                                bytesCompleted = fileObj.optLong("bytesCompleted", 0L),
                                length = fileObj.optLong("length", 0L),
                                wanted = statObj?.optBoolean("wanted", true) ?: true,
                                priority = statObj?.optInt("priority", 0) ?: 0
                            )
                        )
                    }
                }

                list.add(
                    TorrentItem(
                        id = t.getLong("id"),
                        name = t.optString("name", "Unknown Torrent"),
                        status = TorrentStatus.fromCode(t.optInt("status", -1)),
                        totalSize = t.optLong("totalSize", 0L),
                        percentDone = t.optDouble("percentDone", 0.0).toFloat(),
                        rateDownload = t.optLong("rateDownload", 0L),
                        rateUpload = t.optLong("rateUpload", 0L),
                        uploadRatio = t.optDouble("uploadRatio", 0.0).toFloat(),
                        uploadedEver = t.optLong("uploadedEver", 0L),
                        downloadedEver = t.optLong("downloadedEver", 0L),
                        leftUntilDone = t.optLong("leftUntilDone", 0L),
                        eta = t.optLong("eta", -1L),
                        peersConnected = t.optInt("peersConnected", 0),
                        peersSendingToUs = t.optInt("peersSendingToUs", 0),
                        peersGettingFromUs = t.optInt("peersGettingFromUs", 0),
                        error = t.optInt("error", 0),
                        errorString = t.optString("errorString", ""),
                        addedDate = t.optLong("addedDate", 0L) * 1000L,
                        doneDate = t.optLong("doneDate", 0L) * 1000L,
                        isFinished = t.optBoolean("isFinished", false),
                        downloadDir = t.optString("downloadDir", ""),
                        hashString = t.optString("hashString", ""),
                        files = filesList
                    )
                )
            }

            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchSessionStats(): Result<SessionStats> = withContext(Dispatchers.IO) {
        if (server.isDemoMode) {
            initDemoDataIfNeeded()
            val active = demoTorrents.count { it.status.isActive }
            val paused = demoTorrents.count { it.status == TorrentStatus.STOPPED }
            val dlSpeed = demoTorrents.filter { it.status == TorrentStatus.DOWNLOAD }.sumOf { it.rateDownload }
            val ulSpeed = demoTorrents.sumOf { it.rateUpload }
            return@withContext Result.success(
                demoSessionStats.copy(
                    activeTorrentCount = active,
                    pausedTorrentCount = paused,
                    downloadSpeed = dlSpeed,
                    uploadSpeed = ulSpeed,
                    torrentCount = demoTorrents.size
                )
            )
        }

        try {
            val body = JSONObject().apply {
                put("method", "session-stats")
                put("tag", 2)
            }
            val response = executeRpcRequest(body.toString())
            val json = JSONObject(response)
            val args = json.getJSONObject("arguments")
            val cum = args.optJSONObject("cumulative-stats")
            val curr = args.optJSONObject("current-stats")

            val stats = SessionStats(
                activeTorrentCount = args.optInt("activeTorrentCount", 0),
                downloadSpeed = args.optLong("downloadSpeed", 0L),
                uploadSpeed = args.optLong("uploadSpeed", 0L),
                torrentCount = args.optInt("torrentCount", 0),
                pausedTorrentCount = args.optInt("pausedTorrentCount", 0),
                cumulativeUpload = cum?.optLong("uploadedBytes", 0L) ?: 0L,
                cumulativeDownload = cum?.optLong("downloadedBytes", 0L) ?: 0L,
                cumulativeSecondsActive = cum?.optLong("secondsActive", 0L) ?: 0L,
                sessionUpload = curr?.optLong("uploadedBytes", 0L) ?: 0L,
                sessionDownload = curr?.optLong("downloadedBytes", 0L) ?: 0L,
                sessionSecondsActive = curr?.optLong("secondsActive", 0L) ?: 0L
            )
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchSessionInfo(): Result<SessionInfo> = withContext(Dispatchers.IO) {
        if (server.isDemoMode) {
            return@withContext Result.success(
                SessionInfo(
                    version = "4.0.5 (Transmify Demo)",
                    rpcVersion = 17,
                    downloadDir = "/storage/downloads",
                    altSpeedEnabled = demoAltSpeedEnabled,
                    altSpeedDown = 500,
                    altSpeedUp = 200
                )
            )
        }

        try {
            val body = JSONObject().apply {
                put("method", "session-get")
                put("tag", 3)
            }
            val response = executeRpcRequest(body.toString())
            val json = JSONObject(response)
            val args = json.getJSONObject("arguments")

            val info = SessionInfo(
                version = args.optString("version", "Unknown"),
                rpcVersion = args.optInt("rpc-version", 17),
                downloadDir = args.optString("download-dir", "/downloads"),
                altSpeedEnabled = args.optBoolean("alt-speed-enabled", false),
                altSpeedDown = args.optLong("alt-speed-down", 50L),
                altSpeedUp = args.optLong("alt-speed-up", 50L),
                speedLimitDownEnabled = args.optBoolean("speed-limit-down-enabled", false),
                speedLimitDown = args.optLong("speed-limit-down", 0L),
                speedLimitUpEnabled = args.optBoolean("speed-limit-up-enabled", false),
                speedLimitUp = args.optLong("speed-limit-up", 0L)
            )
            Result.success(info)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun startTorrent(id: Long): Result<Unit> = withContext(Dispatchers.IO) {
        if (server.isDemoMode) {
            synchronized(demoTorrents) {
                val index = demoTorrents.indexOfFirst { it.id == id }
                if (index != -1) {
                    val current = demoTorrents[index]
                    val newStatus = if (current.percentDone >= 1.0f) TorrentStatus.SEED else TorrentStatus.DOWNLOAD
                    val dlSpeed = if (newStatus == TorrentStatus.DOWNLOAD) 3_200_000L else 0L
                    demoTorrents[index] = current.copy(
                        status = newStatus,
                        rateDownload = dlSpeed,
                        rateUpload = 450_000L
                    )
                }
            }
            return@withContext Result.success(Unit)
        }

        val body = JSONObject().apply {
            put("method", "torrent-start")
            put("arguments", JSONObject().apply {
                put("ids", JSONArray().put(id))
            })
        }
        executeSimpleCommand(body.toString())
    }

    suspend fun stopTorrent(id: Long): Result<Unit> = withContext(Dispatchers.IO) {
        if (server.isDemoMode) {
            synchronized(demoTorrents) {
                val index = demoTorrents.indexOfFirst { it.id == id }
                if (index != -1) {
                    demoTorrents[index] = demoTorrents[index].copy(
                        status = TorrentStatus.STOPPED,
                        rateDownload = 0L,
                        rateUpload = 0L
                    )
                }
            }
            return@withContext Result.success(Unit)
        }

        val body = JSONObject().apply {
            put("method", "torrent-stop")
            put("arguments", JSONObject().apply {
                put("ids", JSONArray().put(id))
            })
        }
        executeSimpleCommand(body.toString())
    }

    suspend fun startAllTorrents(): Result<Unit> = withContext(Dispatchers.IO) {
        if (server.isDemoMode) {
            synchronized(demoTorrents) {
                for (i in demoTorrents.indices) {
                    val t = demoTorrents[i]
                    val newStatus = if (t.percentDone >= 1.0f) TorrentStatus.SEED else TorrentStatus.DOWNLOAD
                    demoTorrents[i] = t.copy(
                        status = newStatus,
                        rateDownload = if (newStatus == TorrentStatus.DOWNLOAD) 2_500_000L else 0L,
                        rateUpload = 300_000L
                    )
                }
            }
            return@withContext Result.success(Unit)
        }
        val body = JSONObject().apply {
            put("method", "torrent-start")
        }
        executeSimpleCommand(body.toString())
    }

    suspend fun stopAllTorrents(): Result<Unit> = withContext(Dispatchers.IO) {
        if (server.isDemoMode) {
            synchronized(demoTorrents) {
                for (i in demoTorrents.indices) {
                    demoTorrents[i] = demoTorrents[i].copy(
                        status = TorrentStatus.STOPPED,
                        rateDownload = 0L,
                        rateUpload = 0L
                    )
                }
            }
            return@withContext Result.success(Unit)
        }
        val body = JSONObject().apply {
            put("method", "torrent-stop")
        }
        executeSimpleCommand(body.toString())
    }

    suspend fun removeTorrent(id: Long, deleteLocalData: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        if (server.isDemoMode) {
            synchronized(demoTorrents) {
                demoTorrents.removeAll { it.id == id }
            }
            return@withContext Result.success(Unit)
        }

        val body = JSONObject().apply {
            put("method", "torrent-remove")
            put("arguments", JSONObject().apply {
                put("ids", JSONArray().put(id))
                put("delete-local-data", deleteLocalData)
            })
        }
        executeSimpleCommand(body.toString())
    }

    suspend fun addTorrent(
        filenameOrMagnet: String? = null,
        metainfoBase64: String? = null,
        downloadDir: String? = null,
        paused: Boolean = false
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (server.isDemoMode) {
            initDemoDataIfNeeded()
            val newId = (demoTorrents.maxOfOrNull { it.id } ?: 0L) + 1L
            val name = when {
                !filenameOrMagnet.isNullOrEmpty() && filenameOrMagnet.contains("dn=") -> {
                    val dn = filenameOrMagnet.substringAfter("dn=").substringBefore("&")
                    java.net.URLDecoder.decode(dn, "UTF-8")
                }
                !filenameOrMagnet.isNullOrEmpty() -> filenameOrMagnet.substringAfterLast("/")
                else -> "Добавленный торрент #$newId"
            }

            val newTorrent = TorrentItem(
                id = newId,
                name = name,
                status = if (paused) TorrentStatus.STOPPED else TorrentStatus.DOWNLOAD,
                totalSize = 4_200_000_000L,
                percentDone = 0.05f,
                rateDownload = if (paused) 0L else 3_400_000L,
                rateUpload = if (paused) 0L else 120_000L,
                uploadRatio = 0.0f,
                uploadedEver = 0L,
                downloadedEver = 210_000_000L,
                leftUntilDone = 3_990_000_000L,
                eta = 1173L,
                peersConnected = 18,
                peersSendingToUs = 12,
                peersGettingFromUs = 1,
                addedDate = System.currentTimeMillis(),
                downloadDir = downloadDir ?: "/downloads"
            )
            synchronized(demoTorrents) {
                demoTorrents.add(0, newTorrent)
            }
            return@withContext Result.success(Unit)
        }

        val body = JSONObject().apply {
            put("method", "torrent-add")
            put("arguments", JSONObject().apply {
                if (!filenameOrMagnet.isNullOrEmpty()) {
                    put("filename", filenameOrMagnet)
                }
                if (!metainfoBase64.isNullOrEmpty()) {
                    put("metainfo", metainfoBase64)
                }
                if (!downloadDir.isNullOrEmpty()) {
                    put("download-dir", downloadDir)
                }
                put("paused", paused)
            })
        }
        executeSimpleCommand(body.toString())
    }

    suspend fun toggleAltSpeed(enabled: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        if (server.isDemoMode) {
            demoAltSpeedEnabled = enabled
            return@withContext Result.success(Unit)
        }

        val body = JSONObject().apply {
            put("method", "session-set")
            put("arguments", JSONObject().apply {
                put("alt-speed-enabled", enabled)
            })
        }
        executeSimpleCommand(body.toString())
    }

    suspend fun setFileWanted(torrentId: Long, fileIndex: Int, wanted: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        if (server.isDemoMode) {
            synchronized(demoTorrents) {
                val index = demoTorrents.indexOfFirst { it.id == torrentId }
                if (index != -1) {
                    val t = demoTorrents[index]
                    val updatedFiles = t.files.map {
                        if (it.index == fileIndex) it.copy(wanted = wanted) else it
                    }
                    demoTorrents[index] = t.copy(files = updatedFiles)
                }
            }
            return@withContext Result.success(Unit)
        }

        val field = if (wanted) "files-wanted" else "files-unwanted"
        val body = JSONObject().apply {
            put("method", "torrent-set")
            put("arguments", JSONObject().apply {
                put("ids", JSONArray().put(torrentId))
                put(field, JSONArray().put(fileIndex))
            })
        }
        executeSimpleCommand(body.toString())
    }

    suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) {
        if (server.isDemoMode) {
            return@withContext Result.success("Подключение к демо-серверу успешно! Версия: 4.0.5")
        }
        val infoRes = fetchSessionInfo()
        if (infoRes.isSuccess) {
            val info = infoRes.getOrNull()
            Result.success("Успешно! Transmission v${info?.version ?: "OK"}, RPC v${info?.rpcVersion}")
        } else {
            Result.failure(infoRes.exceptionOrNull() ?: Exception("Не удалось подключиться к серверу"))
        }
    }

    private fun executeSimpleCommand(jsonBody: String): Result<Unit> {
        return try {
            val response = executeRpcRequest(jsonBody)
            val json = JSONObject(response)
            if (json.optString("result") == "success") {
                Result.success(Unit)
            } else {
                Result.failure(Exception("RPC error: ${json.optString("result")}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun executeRpcRequest(jsonBody: String): String {
        var requestBuilder = Request.Builder()
            .url(rpcUrl)
            .post(jsonBody.toRequestBody(jsonMediaType))

        if (server.username.isNotEmpty() || server.password.isNotEmpty()) {
            val credentials = "${server.username}:${server.password}"
            val encoded = Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)
            requestBuilder = requestBuilder.header("Authorization", "Basic $encoded")
        }

        cachedSessionId?.let {
            requestBuilder = requestBuilder.header("X-Transmission-Session-Id", it)
        }

        var response = client.newCall(requestBuilder.build()).execute()

        // Handle 409 Conflict (Transmission Session ID CSRF handshake)
        if (response.code == 409) {
            val newSessionId = response.header("X-Transmission-Session-Id")
            response.close()
            if (!newSessionId.isNullOrEmpty()) {
                cachedSessionId = newSessionId
                requestBuilder = requestBuilder.header("X-Transmission-Session-Id", newSessionId)
                response = client.newCall(requestBuilder.build()).execute()
            }
        }

        if (!response.isSuccessful) {
            val code = response.code
            val msg = response.message
            response.close()
            throw IOException("HTTP $code: $msg")
        }

        val bodyString = response.body?.string() ?: throw IOException("Empty response from server")
        response.close()
        return bodyString
    }
}
