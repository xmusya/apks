package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.db.SearchHistoryEntity
import com.example.data.model.RutrackerTopic
import kotlinx.coroutines.launch

/**
 * RuTracker Screen with embedded browser and Material You Monet Dynamic CSS injection,
 * as well as direct search catalog mode.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RutrackerScreen(
    topics: List<RutrackerTopic>,
    query: String,
    category: Int,
    isLoading: Boolean,
    mirrorUrl: String,
    recentSearches: List<SearchHistoryEntity>,
    onSearch: (query: String, categoryId: Int) -> Unit,
    onDownloadTopic: (RutrackerTopic) -> Unit,
    onAddTorrentLink: (String) -> Unit,
    onUpdateMirror: (String) -> Unit
) {
    // Mode: 0 = WebView Web Page (Rutracker page directly with Monet CSS), 1 = Native Catalog
    var viewMode by remember { mutableIntStateOf(0) }

    if (viewMode == 0) {
        RutrackerWebViewPane(
            mirrorUrl = mirrorUrl,
            onAddTorrent = onAddTorrentLink,
            onSwitchToCatalog = { viewMode = 1 },
            onUpdateMirror = onUpdateMirror
        )
    } else {
        RutrackerCatalogPane(
            topics = topics,
            query = query,
            category = category,
            isLoading = isLoading,
            mirrorUrl = mirrorUrl,
            recentSearches = recentSearches,
            onSearch = onSearch,
            onDownloadTopic = onDownloadTopic,
            onSwitchToWeb = { viewMode = 0 },
            onUpdateMirror = onUpdateMirror
        )
    }
}

/**
 * Embedded RuTracker Web Page with real-time Monet Dynamic CSS injection,
 * magnet/torrent download interception, and navigation controls.
 */
@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RutrackerWebViewPane(
    mirrorUrl: String,
    onAddTorrent: (String) -> Unit,
    onSwitchToCatalog: () -> Unit,
    onUpdateMirror: (String) -> Unit
) {
    val context = LocalContext.current
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var currentUrl by remember { mutableStateOf(mirrorUrl) }
    var pageTitle by remember { mutableStateOf("RuTracker.org") }
    var webProgress by remember { mutableFloatStateOf(0f) }
    var isLoading by remember { mutableStateOf(true) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var applyMonetCss by remember { mutableStateOf(true) }
    var isMobileView by remember { mutableStateOf(true) }
    var showMirrorDialog by remember { mutableStateOf(false) }
    var customMirrorInput by remember(mirrorUrl) { mutableStateOf(mirrorUrl) }

    val mobileUserAgent = "Mozilla/5.0 (Linux; Android 14; Mobile; Pixel 8a) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
    val desktopUserAgent = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"

    // Intercepted magnet dialog
    var interceptedLink by remember { mutableStateOf<String?>(null) }
    var pageMagnetUrl by remember { mutableStateOf<String?>(null) }

    // JavaScript to toggle between optimized Mobile View and classic Desktop View
    val mobileModeJs = remember(isMobileView) {
        if (!isMobileView) {
            """
            (function() {
                var vp = document.querySelector('meta[name="viewport"]');
                if (vp) { vp.content = 'width=1024'; }
                var st = document.getElementById('transmify-mobile-style');
                if (st) { st.remove(); }
            })();
            """.trimIndent()
        } else {
            """
            (function() {
                var vp = document.querySelector('meta[name="viewport"]');
                if (!vp) {
                    vp = document.createElement('meta');
                    vp.name = 'viewport';
                    document.head.appendChild(vp);
                }
                vp.content = 'width=device-width, initial-scale=1.0, maximum-scale=3.0, user-scalable=yes';

                var cssId = 'transmify-mobile-style';
                var style = document.getElementById(cssId);
                if (!style) {
                    style = document.createElement('style');
                    style.id = cssId;
                    style.type = 'text/css';
                    document.head.appendChild(style);
                }
                style.innerHTML = `
                    html, body {
                        max-width: 100vw !important;
                        overflow-x: hidden !important;
                        -webkit-text-size-adjust: 100% !important;
                    }
                    #page_container, #page_content, #main_content_wrap, .wrap, table.w100, .body_container, #page_footer {
                        width: 100% !important;
                        min-width: 0 !important;
                        max-width: 100vw !important;
                        box-sizing: border-box !important;
                        margin: 0 auto !important;
                        padding-left: 4px !important;
                        padding-right: 4px !important;
                    }
                    table.forumline, table.forumline_hl, table.topic {
                        width: 100% !important;
                        display: block !important;
                        box-sizing: border-box !important;
                        overflow-x: auto !important;
                    }
                    .post_body, .post_wrap, .post_message {
                        font-size: 15px !important;
                        line-height: 1.6 !important;
                        word-break: break-word !important;
                    }
                    .tt-text, .topictitle, a.topictitle {
                        font-size: 15px !important;
                        line-height: 1.4 !important;
                        display: inline-block !important;
                        padding: 3px 0 !important;
                    }
                    .pg, .pg-jump, .nav, a.pg {
                        font-size: 13px !important;
                        padding: 6px 10px !important;
                        margin: 2px !important;
                        display: inline-block !important;
                        min-height: 28px !important;
                        line-height: 20px !important;
                        border-radius: 6px !important;
                    }
                    a[href^="magnet:"] {
                        display: inline-flex !important;
                        align-items: center !important;
                        background: #10B981 !important;
                        color: #FFFFFF !important;
                        padding: 8px 14px !important;
                        border-radius: 8px !important;
                        font-weight: bold !important;
                        font-size: 14px !important;
                        text-decoration: none !important;
                        margin: 6px 0 !important;
                        box-shadow: 0 2px 6px rgba(16, 185, 129, 0.4) !important;
                    }
                    #search-text, input[type="text"], input[type="search"] {
                        font-size: 15px !important;
                        padding: 8px 10px !important;
                        border-radius: 8px !important;
                        box-sizing: border-box !important;
                    }
                    #bn-top, .bn, .adv, #sidebar {
                        display: none !important;
                    }
                `;
            })();
            """.trimIndent()
        }
    }

    // Colors from MaterialTheme (Monet dynamic colors)
    val colorScheme = MaterialTheme.colorScheme
    val primaryHex = String.format("#%06X", 0xFFFFFF and colorScheme.primary.toArgb())
    val surfaceHex = String.format("#%06X", 0xFFFFFF and colorScheme.surface.toArgb())
    val surfaceVariantHex = String.format("#%06X", 0xFFFFFF and colorScheme.surfaceVariant.toArgb())
    val onSurfaceHex = String.format("#%06X", 0xFFFFFF and colorScheme.onSurface.toArgb())
    val onSurfaceVariantHex = String.format("#%06X", 0xFFFFFF and colorScheme.onSurfaceVariant.toArgb())
    val primaryContainerHex = String.format("#%06X", 0xFFFFFF and colorScheme.primaryContainer.toArgb())
    val onPrimaryContainerHex = String.format("#%06X", 0xFFFFFF and colorScheme.onPrimaryContainer.toArgb())
    val outlineHex = String.format("#%06X", 0xFFFFFF and colorScheme.outline.toArgb())

    // JavaScript to inject Monet Custom CSS into RuTracker
    val monetCssJs = remember(primaryHex, surfaceHex, onSurfaceHex, applyMonetCss) {
        if (!applyMonetCss) {
            "var el = document.getElementById('monet-custom-css'); if (el) el.remove();"
        } else {
            """
            (function() {
                var cssId = 'monet-custom-css';
                var style = document.getElementById(cssId);
                if (!style) {
                    style = document.createElement('style');
                    style.id = cssId;
                    style.type = 'text/css';
                    document.head.appendChild(style);
                }
                style.innerHTML = `
                    :root {
                        --monet-primary: $primaryHex !important;
                        --monet-surface: $surfaceHex !important;
                        --monet-surface-var: $surfaceVariantHex !important;
                        --monet-on-surface: $onSurfaceHex !important;
                        --monet-on-surface-var: $onSurfaceVariantHex !important;
                        --monet-primary-cont: $primaryContainerHex !important;
                        --monet-outline: $outlineHex !important;
                    }
                    html, body, #page_container, #page_content, .wrap {
                        background-color: var(--monet-surface) !important;
                        color: var(--monet-on-surface) !important;
                        font-family: Roboto, -apple-system, sans-serif !important;
                    }
                    /* Headers and forum bars */
                    th, .thHead, .catTitle, .catHead, .forumline th, #main_nav, .topmenu {
                        background: var(--monet-primary-cont) !important;
                        color: $onPrimaryContainerHex !important;
                        border-color: var(--monet-outline) !important;
                    }
                    /* Links */
                    a, a:visited, .genmed a, .topictitle {
                        color: var(--monet-primary) !important;
                        text-decoration: none !important;
                    }
                    a:hover {
                        text-decoration: underline !important;
                    }
                    /* Tables and forum rows */
                    table.forumline, table.topic, .row1, .row2, .row3, .post_body {
                        background-color: var(--monet-surface) !important;
                        color: var(--monet-on-surface) !important;
                        border-color: var(--monet-outline) !important;
                    }
                    tr.hl-tr:hover td, .row1:hover, .row2:hover {
                        background-color: var(--monet-surface-var) !important;
                    }
                    /* Input fields and buttons */
                    input[type="text"], input[type="password"], select, textarea {
                        background-color: var(--monet-surface-var) !important;
                        color: var(--monet-on-surface) !important;
                        border: 1px solid var(--monet-outline) !important;
                        border-radius: 8px !important;
                        padding: 6px 10px !important;
                    }
                    input[type="submit"], input[type="button"], .btn {
                        background-color: var(--monet-primary) !important;
                        color: $surfaceHex !important;
                        border: none !important;
                        border-radius: 8px !important;
                        padding: 6px 14px !important;
                        font-weight: bold !important;
                    }
                    /* Torrent badges & download buttons */
                    .attach_link, a[href^="magnet:"], a[href*="dl.php"] {
                        background-color: var(--monet-primary-cont) !important;
                        color: $onPrimaryContainerHex !important;
                        padding: 4px 8px !important;
                        border-radius: 6px !important;
                        display: inline-block !important;
                        font-weight: bold !important;
                    }
                    /* Seeders & Leechers */
                    .seed, .seedmed, b.seed {
                        color: #10B981 !important;
                    }
                    .leech, .leechmed, b.leech {
                        color: #EF4444 !important;
                    }
                    /* Remove bright banners */
                    #bn-top, .bn, .adv {
                        display: none !important;
                    }
                `;
            })();
            """.trimIndent()
        }
    }

    // Hardware back press navigation within WebView
    BackHandler(enabled = canGoBack) {
        webViewInstance?.goBack()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "RuTracker",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (applyMonetCss) "Monet" else "Web",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isMobileView) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        isMobileView = !isMobileView
                                        webViewInstance?.let { wv ->
                                            wv.settings.userAgentString = if (isMobileView) mobileUserAgent else desktopUserAgent
                                            wv.settings.useWideViewPort = !isMobileView
                                            wv.settings.loadWithOverviewMode = !isMobileView
                                            wv.evaluateJavascript(mobileModeJs, null)
                                            if (applyMonetCss) {
                                                wv.evaluateJavascript(monetCssJs, null)
                                            }
                                        }
                                    }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isMobileView) "📱 Мобильный" else "💻 ПК",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMobileView) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = currentUrl.take(45),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { webViewInstance?.goBack() },
                        enabled = canGoBack
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = if (canGoBack) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                },
                actions = {
                    // Forward button
                    IconButton(
                        onClick = { webViewInstance?.goForward() },
                        enabled = canGoForward
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Вперед",
                            tint = if (canGoForward) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                    // Toggle Mobile / Desktop version
                    IconButton(
                        onClick = {
                            isMobileView = !isMobileView
                            webViewInstance?.let { wv ->
                                wv.settings.userAgentString = if (isMobileView) mobileUserAgent else desktopUserAgent
                                wv.settings.useWideViewPort = !isMobileView
                                wv.settings.loadWithOverviewMode = !isMobileView
                                wv.evaluateJavascript(mobileModeJs, null)
                                if (applyMonetCss) {
                                    wv.evaluateJavascript(monetCssJs, null)
                                }
                            }
                        }
                    ) {
                        Icon(
                            if (isMobileView) Icons.Default.PhoneAndroid else Icons.Default.Computer,
                            contentDescription = if (isMobileView) "Переключить на версию для ПК" else "Переключить на мобильную версию",
                            tint = if (isMobileView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Toggle Monet Theme
                    IconButton(
                        onClick = {
                            applyMonetCss = !applyMonetCss
                            webViewInstance?.evaluateJavascript(monetCssJs, null)
                        }
                    ) {
                        Icon(
                            Icons.Default.Palette,
                            contentDescription = "Monet CSS",
                            tint = if (applyMonetCss) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Refresh or Home
                    IconButton(onClick = { webViewInstance?.reload() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Обновить")
                    }
                    // Switch to Catalog mode
                    IconButton(onClick = onSwitchToCatalog) {
                        Icon(Icons.Default.Search, contentDescription = "Каталог поиска")
                    }
                    // Mirror config
                    IconButton(onClick = { showMirrorDialog = true }) {
                        Icon(Icons.Default.Language, contentDescription = "Зеркало")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Web Loading Progress
            if (isLoading) {
                LinearProgressIndicator(
                    progress = { webProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            // Quick navigation bar (Forum categories & search shortcut)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilledTonalButton(
                        onClick = { webViewInstance?.loadUrl(mirrorUrl) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Главная", fontSize = 11.sp)
                    }
                    FilledTonalButton(
                        onClick = { webViewInstance?.loadUrl("$mirrorUrl/forum/tracker.php") },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Трекер", fontSize = 11.sp)
                    }
                    FilledTonalButton(
                        onClick = { webViewInstance?.loadUrl("$mirrorUrl/forum/index.php") },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Форум", fontSize = 11.sp)
                    }

                    // Mobile / PC switch button
                    FilledTonalButton(
                        onClick = {
                            isMobileView = !isMobileView
                            webViewInstance?.let { wv ->
                                wv.settings.userAgentString = if (isMobileView) mobileUserAgent else desktopUserAgent
                                wv.settings.useWideViewPort = !isMobileView
                                wv.settings.loadWithOverviewMode = !isMobileView
                                wv.evaluateJavascript(mobileModeJs, null)
                                if (applyMonetCss) {
                                    wv.evaluateJavascript(monetCssJs, null)
                                }
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (isMobileView) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Icon(
                            if (isMobileView) Icons.Default.PhoneAndroid else Icons.Default.Computer,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(if (isMobileView) "Моб." else "ПК", fontSize = 11.sp)
                    }
                }

                TextButton(
                    onClick = onSwitchToCatalog,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Быстрый поиск 🔍", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Android WebView
            Box(modifier = Modifier.weight(1f)) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )

                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.userAgentString = if (isMobileView) mobileUserAgent else desktopUserAgent
                            settings.loadWithOverviewMode = !isMobileView
                            settings.useWideViewPort = !isMobileView
                            settings.builtInZoomControls = true
                            settings.displayZoomControls = false
                            settings.databaseEnabled = true
                            settings.cacheMode = WebSettings.LOAD_DEFAULT

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    webProgress = newProgress / 100f
                                    isLoading = newProgress < 100
                                    if (newProgress > 60 && applyMonetCss) {
                                        view?.evaluateJavascript(monetCssJs, null)
                                    }
                                }

                                override fun onReceivedTitle(view: WebView?, title: String?) {
                                    title?.let { pageTitle = it }
                                }
                            }

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    isLoading = true
                                    url?.let { currentUrl = it }
                                    canGoBack = canGoBack()
                                    canGoForward = canGoForward()
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    isLoading = false
                                    url?.let { currentUrl = it }
                                    canGoBack = canGoBack()
                                    canGoForward = canGoForward()
                                    if (isMobileView) {
                                        view?.evaluateJavascript(mobileModeJs, null)
                                    }
                                    if (applyMonetCss) {
                                        view?.evaluateJavascript(monetCssJs, null)
                                    }
                                    // Search for magnet link on this page
                                    view?.evaluateJavascript(
                                        "(function() { var m = document.querySelector('a[href^=\"magnet:\"]'); return m ? m.href : ''; })()"
                                    ) { res ->
                                        val clean = res?.trim('"', '\'', ' ', '\\')
                                        pageMagnetUrl = if (!clean.isNullOrEmpty() && clean.startsWith("magnet:")) clean else null
                                    }
                                }

                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    val url = request?.url?.toString() ?: return false
                                    // Intercept magnet link or torrent file download
                                    if (url.startsWith("magnet:") || url.contains("dl.php") || url.endsWith(".torrent")) {
                                        interceptedLink = url
                                        if (pageMagnetUrl == null) {
                                            view?.evaluateJavascript(
                                                "(function() { var m = document.querySelector('a[href^=\"magnet:\"]'); return m ? m.href : ''; })()"
                                            ) { res ->
                                                val clean = res?.trim('"', '\'', ' ', '\\')
                                                if (!clean.isNullOrEmpty() && clean.startsWith("magnet:")) {
                                                    pageMagnetUrl = clean
                                                }
                                            }
                                        }
                                        return true
                                    }
                                    return false
                                }
                            }

                            setDownloadListener { url, _, _, _, _ ->
                                if (url != null) {
                                    interceptedLink = url
                                    if (pageMagnetUrl == null) {
                                        evaluateJavascript(
                                            "(function() { var m = document.querySelector('a[href^=\"magnet:\"]'); return m ? m.href : ''; })()"
                                        ) { res ->
                                            val clean = res?.trim('"', '\'', ' ', '\\')
                                            if (!clean.isNullOrEmpty() && clean.startsWith("magnet:")) {
                                                pageMagnetUrl = clean
                                            }
                                        }
                                    }
                                }
                            }

                            loadUrl(mirrorUrl)
                            webViewInstance = this
                        }
                    },
                    update = { webView ->
                        webView.settings.userAgentString = if (isMobileView) mobileUserAgent else desktopUserAgent
                        webView.settings.useWideViewPort = !isMobileView
                        webView.settings.loadWithOverviewMode = !isMobileView
                        if (isMobileView) {
                            webView.evaluateJavascript(mobileModeJs, null)
                        }
                        if (applyMonetCss) {
                            webView.evaluateJavascript(monetCssJs, null)
                        }
                    }
                )

                // Floating Action Button when Magnet is detected on current release page
                if (pageMagnetUrl != null) {
                    Button(
                        onClick = {
                            pageMagnetUrl?.let { magnet ->
                                onAddTorrent(magnet)
                            }
                        },
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Скачать в Transmission (Magnet) 🧲", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Intercepted magnet/torrent download confirmation dialog
    interceptedLink?.let { link ->
        val isMagnet = link.startsWith("magnet:")
        val hasMagnetAlternative = !isMagnet && pageMagnetUrl != null

        AlertDialog(
            onDismissRequest = { interceptedLink = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (isMagnet) "Добавить Magnet в Transmission?"
                        else if (hasMagnetAlternative) "Скачать раздачу через Magnet?"
                        else "Скачать торрент в Transmission?"
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isMagnet) {
                        Text(
                            text = "Обнаружена Magnet-ссылка. Transmission сразу начнет загрузку метаданных и раздачи:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else if (hasMagnetAlternative) {
                        Text(
                            text = "На странице обнаружена прямая Magnet-ссылка — она начнет скачивание сразу и не требует авторизации под аккаунтом:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        Text(
                            text = "Обнаружена ссылка на скачивание файла .torrent. Если вы не авторизованы, рекомендуется нажать на Magnet-ссылку 🧲 на странице раздачи:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (hasMagnetAlternative) pageMagnetUrl ?: link else link,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(10.dp),
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val targetLink = if (hasMagnetAlternative && pageMagnetUrl != null) pageMagnetUrl!! else link
                        onAddTorrent(targetLink)
                        interceptedLink = null
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (hasMagnetAlternative) "Скачать через Magnet 🚀" else "Добавить в Transmission 🚀")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (hasMagnetAlternative) {
                        OutlinedButton(
                            onClick = {
                                onAddTorrent(link)
                                interceptedLink = null
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Файлом .torrent", fontSize = 12.sp)
                        }
                    }
                    TextButton(onClick = { interceptedLink = null }) {
                        Text("Отмена")
                    }
                }
            }
        )
    }

    // Mirror URL setting dialog
    if (showMirrorDialog) {
        AlertDialog(
            onDismissRequest = { showMirrorDialog = false },
            title = { Text("Настройка адреса / зеркала RuTracker") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Укажите рабочий домен RuTracker. При необходимости используйте доступные зеркала:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = customMirrorInput,
                        onValueChange = { customMirrorInput = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { customMirrorInput = "https://rutracker.org" },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(".org", fontSize = 11.sp)
                        }
                        Button(
                            onClick = { customMirrorInput = "https://rutracker.net" },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(".net", fontSize = 11.sp)
                        }
                        Button(
                            onClick = { customMirrorInput = "https://rutracker.org.app" },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(".app", fontSize = 11.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = customMirrorInput.trim()
                        onUpdateMirror(clean)
                        webViewInstance?.loadUrl(clean)
                        showMirrorDialog = false
                    }
                ) {
                    Text("Применить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMirrorDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

/**
 * Catalog search pane with fast category filtering and direct release cards
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RutrackerCatalogPane(
    topics: List<RutrackerTopic>,
    query: String,
    category: Int,
    isLoading: Boolean,
    mirrorUrl: String,
    recentSearches: List<SearchHistoryEntity>,
    onSearch: (query: String, categoryId: Int) -> Unit,
    onDownloadTopic: (RutrackerTopic) -> Unit,
    onSwitchToWeb: () -> Unit,
    onUpdateMirror: (String) -> Unit
) {
    val context = LocalContext.current
    var searchInput by remember(query) { mutableStateOf(query) }
    var selectedCategory by remember(category) { mutableStateOf(category) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "RuTracker Каталог",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    FilledTonalButton(
                        onClick = onSwitchToWeb,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Веб-страница", fontSize = 12.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchInput,
                    onValueChange = { searchInput = it },
                    placeholder = { Text("Поиск раздач...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchInput.isNotEmpty()) {
                            IconButton(onClick = {
                                searchInput = ""
                                onSearch("", selectedCategory)
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Очистить")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { onSearch(searchInput, selectedCategory) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.height(56.dp)
                ) {
                    Text("Найти")
                }
            }

            // Fast category chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                com.example.data.model.RutrackerCategories.ALL.take(4).forEach { cat ->
                    val isSelected = selectedCategory == cat.id
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedCategory = cat.id
                            onSearch(searchInput, cat.id)
                        },
                        label = { Text(cat.nameRu, fontSize = 11.sp) },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // Results List
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(topics, key = { it.topicId }) { topic ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = topic.title,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${topic.category} • ${topic.sizeFormatted}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "▲ ${topic.seeds}  ▼ ${topic.leeches}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = { onDownloadTopic(topic) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Скачать в Transmission")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
