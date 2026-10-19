package com.example.ui

import android.app.Application
import android.net.Uri
import android.util.Base64
import android.webkit.CookieManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.db.SearchHistoryEntity
import com.example.data.db.ServerEntity
import com.example.data.model.RutrackerTopic
import com.example.data.model.SessionInfo
import com.example.data.model.SessionStats
import com.example.data.model.TorrentItem
import com.example.data.model.TorrentStatus
import com.example.data.remote.RutrackerClient
import com.example.data.remote.TransmissionRpcClient
import com.example.notifications.DownloadNotificationManager
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

enum class TorrentFilter(val titleRu: String) {
    ALL("Все"),
    DOWNLOADING("Загружаются"),
    SEEDING("Раздаются"),
    PAUSED("На паузе"),
    FINISHED("Завершены")
}

enum class TorrentSort(val titleRu: String) {
    DATE("По дате добавления"),
    NAME("По имени (А-Я)"),
    PROGRESS("По прогрессу"),
    SPEED_DOWN("По скорости скачивания"),
    SPEED_UP("По скорости отдачи"),
    SIZE("По размеру")
}

class TransmifyViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val serverDao = db.serverDao()
    private val searchDao = db.searchDao()
    val notificationManager = DownloadNotificationManager(application)
    private val rutrackerClient = RutrackerClient()

    private val _servers = MutableStateFlow<List<ServerEntity>>(emptyList())
    val servers: StateFlow<List<ServerEntity>> = _servers.asStateFlow()

    private val _activeServer = MutableStateFlow<ServerEntity?>(null)
    val activeServer: StateFlow<ServerEntity?> = _activeServer.asStateFlow()

    private val _torrents = MutableStateFlow<List<TorrentItem>>(emptyList())
    val torrents: StateFlow<List<TorrentItem>> = _torrents.asStateFlow()

    private val _sessionStats = MutableStateFlow(SessionStats())
    val sessionStats: StateFlow<SessionStats> = _sessionStats.asStateFlow()

    private val _sessionInfo = MutableStateFlow(SessionInfo())
    val sessionInfo: StateFlow<SessionInfo> = _sessionInfo.asStateFlow()

    private val _selectedTorrent = MutableStateFlow<TorrentItem?>(null)
    val selectedTorrent: StateFlow<TorrentItem?> = _selectedTorrent.asStateFlow()

    // Filters & Sorting
    private val _activeFilter = MutableStateFlow(TorrentFilter.ALL)
    val activeFilter: StateFlow<TorrentFilter> = _activeFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _activeSort = MutableStateFlow(TorrentSort.DATE)
    val activeSort: StateFlow<TorrentSort> = _activeSort.asStateFlow()

    // UI Feedback
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _connectionError = MutableStateFlow<String?>(null)
    val connectionError: StateFlow<String?> = _connectionError.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    // Theme & Notification settings
    private val _themeMode = MutableStateFlow(ThemeMode.DARK)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    // RuTracker State
    private val _rutrackerTopics = MutableStateFlow<List<RutrackerTopic>>(emptyList())
    val rutrackerTopics: StateFlow<List<RutrackerTopic>> = _rutrackerTopics.asStateFlow()

    private val _rutrackerQuery = MutableStateFlow("")
    val rutrackerQuery: StateFlow<String> = _rutrackerQuery.asStateFlow()

    private val _rutrackerCategory = MutableStateFlow(0)
    val rutrackerCategory: StateFlow<Int> = _rutrackerCategory.asStateFlow()

    private val _rutrackerLoading = MutableStateFlow(false)
    val rutrackerLoading: StateFlow<Boolean> = _rutrackerLoading.asStateFlow()

    private val _rutrackerMirror = MutableStateFlow("https://rutracker.org")
    val rutrackerMirror: StateFlow<String> = _rutrackerMirror.asStateFlow()

    private val _recentSearches = MutableStateFlow<List<SearchHistoryEntity>>(emptyList())
    val recentSearches: StateFlow<List<SearchHistoryEntity>> = _recentSearches.asStateFlow()

    private var activeRpcClient: TransmissionRpcClient? = null
    private var pollJob: Job? = null
    private val previousCompletionState = mutableMapOf<Long, Boolean>()

    init {
        viewModelScope.launch {
            serverDao.getAllServers().collect { serverList ->
                _servers.value = serverList
                val currentActive = serverList.find { it.isActive } ?: serverList.firstOrNull()
                if (currentActive != _activeServer.value) {
                    _activeServer.value = currentActive
                    currentActive?.let { setupRpcClient(it) }
                }
            }
        }

        viewModelScope.launch {
            searchDao.getRecentSearches().collect {
                _recentSearches.value = it
            }
        }

        // Initialize RuTracker sample catalog
        searchRutracker("", 0)
    }

    private fun setupRpcClient(server: ServerEntity) {
        activeRpcClient = TransmissionRpcClient(server)
        startPolling()
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (isActive) {
                refreshData(silent = true)
                delay(3000)
            }
        }
    }

    fun refreshData(silent: Boolean = false) {
        val client = activeRpcClient ?: return
        viewModelScope.launch {
            if (!silent) _isRefreshing.value = true

            val torrentsResult = client.fetchTorrents()
            if (torrentsResult.isSuccess) {
                _connectionError.value = null
                val list = torrentsResult.getOrDefault(emptyList())

                // Check for download completions and trigger notifications
                if (_notificationsEnabled.value) {
                    checkDownloadCompletions(list)
                }

                _torrents.value = list

                // If a torrent is currently inspected in details sheet, refresh its state
                _selectedTorrent.value?.let { sel ->
                    val updated = list.find { it.id == sel.id }
                    if (updated != null) _selectedTorrent.value = updated
                }
            } else {
                val errorMsg = torrentsResult.exceptionOrNull()?.message ?: "Не удалось подключиться к серверу"
                _connectionError.value = errorMsg
            }

            val statsResult = client.fetchSessionStats()
            if (statsResult.isSuccess) {
                _sessionStats.value = statsResult.getOrDefault(SessionStats())
            }

            val infoResult = client.fetchSessionInfo()
            if (infoResult.isSuccess) {
                _sessionInfo.value = infoResult.getOrDefault(SessionInfo())
            }

            if (!silent) _isRefreshing.value = false
        }
    }

    private fun checkDownloadCompletions(newList: List<TorrentItem>) {
        for (torrent in newList) {
            val isDoneNow = torrent.percentDone >= 1.0f || torrent.status == TorrentStatus.SEED
            val wasDoneBefore = previousCompletionState[torrent.id]

            if (wasDoneBefore == false && isDoneNow) {
                // Newly finished!
                notificationManager.notifyDownloadCompleted(
                    torrentName = torrent.name,
                    totalSize = torrent.formattedTotalSize,
                    torrentId = torrent.id
                )
            }
            previousCompletionState[torrent.id] = isDoneNow
        }
    }

    fun setFilter(filter: TorrentFilter) {
        _activeFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSort(sort: TorrentSort) {
        _activeSort.value = sort
    }

    fun selectTorrent(torrent: TorrentItem?) {
        _selectedTorrent.value = torrent
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        _notificationsEnabled.value = enabled
    }

    fun testNotification() {
        notificationManager.sendTestNotification()
        showUserMessage("Тестовое уведомление отправлено!")
    }

    // Torrent actions
    fun startTorrent(id: Long) {
        val client = activeRpcClient ?: return
        viewModelScope.launch {
            val res = client.startTorrent(id)
            if (res.isSuccess) {
                refreshData(silent = true)
                showUserMessage("Торрент запущен")
            } else {
                showUserMessage("Ошибка запуска: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun stopTorrent(id: Long) {
        val client = activeRpcClient ?: return
        viewModelScope.launch {
            val res = client.stopTorrent(id)
            if (res.isSuccess) {
                refreshData(silent = true)
                showUserMessage("Торрент приостановлен")
            } else {
                showUserMessage("Ошибка остановки: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun startAllTorrents() {
        val client = activeRpcClient ?: return
        viewModelScope.launch {
            val res = client.startAllTorrents()
            if (res.isSuccess) {
                refreshData(silent = true)
                showUserMessage("Все торренты запущены")
            }
        }
    }

    fun stopAllTorrents() {
        val client = activeRpcClient ?: return
        viewModelScope.launch {
            val res = client.stopAllTorrents()
            if (res.isSuccess) {
                refreshData(silent = true)
                showUserMessage("Все торренты приостановлены")
            }
        }
    }

    fun removeTorrent(id: Long, deleteLocalData: Boolean) {
        val client = activeRpcClient ?: return
        viewModelScope.launch {
            val res = client.removeTorrent(id, deleteLocalData)
            if (res.isSuccess) {
                if (_selectedTorrent.value?.id == id) {
                    _selectedTorrent.value = null
                }
                refreshData(silent = true)
                showUserMessage(if (deleteLocalData) "Торрент и файлы удалены" else "Торрент удален")
            } else {
                showUserMessage("Ошибка удаления: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun addTorrent(
        filenameOrMagnet: String? = null,
        metainfoBase64: String? = null,
        downloadDir: String? = null,
        paused: Boolean = false
    ) {
        val client = activeRpcClient ?: return
        viewModelScope.launch {
            val res = client.addTorrent(filenameOrMagnet, metainfoBase64, downloadDir, paused)
            if (res.isSuccess) {
                refreshData(silent = true)
                showUserMessage("Торрент успешно добавлен в Transmission! 🚀")
            } else {
                showUserMessage("Не удалось добавить: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun addTorrentFromUri(uri: Uri, downloadDir: String? = null, paused: Boolean = false) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()

                if (bytes != null && bytes.isNotEmpty()) {
                    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    addTorrent(
                        filenameOrMagnet = null,
                        metainfoBase64 = base64,
                        downloadDir = downloadDir,
                        paused = paused
                    )
                } else {
                    showUserMessage("Не удалось прочитать выбранный файл торрента")
                }
            } catch (e: Exception) {
                showUserMessage("Ошибка чтения файла: ${e.message}")
            }
        }
    }

    fun addTorrentFromRutracker(url: String, referer: String? = null, cookieHeader: String? = null) {
        if (url.startsWith("magnet:")) {
            addTorrent(filenameOrMagnet = url)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                showUserMessage("Загрузка торрент-файла с RuTracker...")
            }
            try {
                val cookies = cookieHeader ?: try {
                    CookieManager.getInstance().getCookie(url)
                } catch (e: Exception) {
                    null
                }

                val conn = URL(url).openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
                )
                if (!cookies.isNullOrEmpty()) {
                    conn.setRequestProperty("Cookie", cookies)
                }
                if (!referer.isNullOrEmpty()) {
                    conn.setRequestProperty("Referer", referer)
                } else {
                    val fallbackReferer = if (url.contains("t=")) {
                        val topicId = url.substringAfter("t=").substringBefore("&")
                        url.substringBefore("/dl.php") + "/viewtopic.php?t=" + topicId
                    } else {
                        url.substringBefore("/dl.php") + "/forum/"
                    }
                    conn.setRequestProperty("Referer", fallbackReferer)
                }
                conn.setRequestProperty("Accept", "application/x-bittorrent, */*")
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                conn.instanceFollowRedirects = true

                val code = conn.responseCode
                if (code in 200..299) {
                    val bytes = conn.inputStream.use { it.readBytes() }
                    // Bencoded torrent files start with 'd' (ASCII 100)
                    if (bytes.isNotEmpty() && bytes[0] == 'd'.code.toByte()) {
                        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                        withContext(Dispatchers.Main) {
                            addTorrent(metainfoBase64 = base64)
                        }
                    } else {
                        // Might be HTML redirect or login required
                        val contentStr = String(bytes.take(400).toByteArray(), Charsets.UTF_8)
                        withContext(Dispatchers.Main) {
                            if (contentStr.contains("<html", ignoreCase = true) || contentStr.contains("login", ignoreCase = true)) {
                                showUserMessage("Для скачивания файла .torrent требуется авторизация на RuTracker. Используйте Magnet-ссылку 🧲 — она работает без входа!")
                            } else {
                                showUserMessage("RuTracker вернул ответ вместо торрента. Попробуйте Magnet-ссылку 🧲!")
                            }
                        }
                    }
                } else if (code == 403) {
                    withContext(Dispatchers.Main) {
                        showUserMessage("Для скачивания файла .torrent войдите в свой профиль на RuTracker или используйте Magnet-ссылку 🧲")
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        showUserMessage("Ошибка RuTracker (HTTP $code). Рекомендуем нажать Magnet-ссылку 🧲!")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showUserMessage("Ошибка загрузки: ${e.message}. Нажмите на Magnet-ссылку 🧲 на странице раздачи!")
                }
            }
        }
    }

    fun toggleAltSpeed() {
        val client = activeRpcClient ?: return
        viewModelScope.launch {
            val newEnabled = !_sessionInfo.value.altSpeedEnabled
            val res = client.toggleAltSpeed(newEnabled)
            if (res.isSuccess) {
                _sessionInfo.value = _sessionInfo.value.copy(altSpeedEnabled = newEnabled)
                showUserMessage(if (newEnabled) "Ограничение скорости (Turtle Mode) включено 🐢" else "Ограничение скорости отключено ⚡")
            }
        }
    }

    fun toggleFileWanted(torrentId: Long, fileIndex: Int, wanted: Boolean) {
        val client = activeRpcClient ?: return
        viewModelScope.launch {
            val res = client.setFileWanted(torrentId, fileIndex, wanted)
            if (res.isSuccess) {
                refreshData(silent = true)
            }
        }
    }

    // RuTracker methods
    fun searchRutracker(query: String, categoryId: Int = _rutrackerCategory.value) {
        _rutrackerQuery.value = query
        _rutrackerCategory.value = categoryId
        _rutrackerLoading.value = true

        viewModelScope.launch {
            if (query.isNotBlank()) {
                searchDao.insertQuery(SearchHistoryEntity(query = query))
            }
            val res = rutrackerClient.search(query, categoryId)
            _rutrackerTopics.value = res.getOrDefault(emptyList())
            _rutrackerLoading.value = false
        }
    }

    fun setRutrackerMirror(mirror: String) {
        _rutrackerMirror.value = mirror
        rutrackerClient.updateMirror(mirror)
        showUserMessage("Зеркало RuTracker обновлено: $mirror")
        searchRutracker(_rutrackerQuery.value, _rutrackerCategory.value)
    }

    fun downloadRutrackerTopic(topic: RutrackerTopic) {
        val magnet = topic.magnetUrl
        if (magnet.isNotEmpty()) {
            addTorrent(filenameOrMagnet = magnet)
            showUserMessage("Торрент \"${topic.title.take(30)}...\" отправлен в Transmission!")
        } else {
            showUserMessage("У этого релиза нет прямой ссылки Magnet")
        }
    }

    // Server profile management
    fun switchActiveServer(server: ServerEntity) {
        viewModelScope.launch {
            serverDao.deactivateAll()
            serverDao.setActive(server.id)
            _activeServer.value = server.copy(isActive = true)
            setupRpcClient(server)
            showUserMessage("Подключено к: ${server.name}")
        }
    }

    fun saveServer(server: ServerEntity) {
        viewModelScope.launch {
            if (server.id == 0L) {
                serverDao.insertServer(server)
            } else {
                serverDao.updateServer(server)
            }
            showUserMessage("Сервер \"${server.name}\" сохранен")
        }
    }

    fun deleteServer(server: ServerEntity) {
        viewModelScope.launch {
            serverDao.deleteServer(server)
            showUserMessage("Сервер \"${server.name}\" удален")
        }
    }

    suspend fun testConnection(server: ServerEntity): Result<String> {
        val tempClient = TransmissionRpcClient(server)
        return tempClient.testConnection()
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun showUserMessage(message: String) {
        _userMessage.value = message
    }

    fun clearConnectionError() {
        _connectionError.value = null
    }
}
