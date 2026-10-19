package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.notifications.DownloadNotificationManager
import com.example.ui.TransmifyViewModel
import com.example.ui.screens.MainAppScreen
import com.example.ui.theme.TransmifyTheme

class MainActivity : ComponentActivity() {

    private val viewModel: TransmifyViewModel by viewModels()
    private var incomingTorrentUriOrMagnet by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIncomingIntent(intent)

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()

            TransmifyTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainAppScreen(
                        viewModel = viewModel,
                        initialAddMagnetOrFile = incomingTorrentUriOrMagnet
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return

        // 1. Check if opened from download completed notification
        val targetTorrentId = intent.getLongExtra(DownloadNotificationManager.EXTRA_TORRENT_ID, -1L)
        if (targetTorrentId != -1L) {
            val torrents = viewModel.torrents.value
            val target = torrents.find { it.id == targetTorrentId }
            if (target != null) {
                viewModel.selectTorrent(target)
            }
            return
        }

        // 2. Check if opened with magnet link or .torrent file
        val data: Uri? = intent.data
        if (data != null) {
            val uriString = data.toString()
            if (uriString.startsWith("magnet:")) {
                incomingTorrentUriOrMagnet = uriString
                viewModel.addTorrent(filenameOrMagnet = uriString)
            } else if (intent.type == "application/x-bittorrent" || uriString.endsWith(".torrent")) {
                viewModel.addTorrentFromUri(uri = data)
            }
        }
    }
}
