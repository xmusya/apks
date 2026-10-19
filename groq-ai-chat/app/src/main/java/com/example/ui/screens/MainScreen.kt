package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.ConversationEntity
import com.example.ui.chat.ChatViewModel

enum class TabletLeftTab(val title: String) {
    HISTORY("Chats"),
    MODELS("Models"),
    STATS("Stats"),
    SETTINGS("Settings")
}

enum class PhoneScreen {
    CONVERSATION_LIST,
    CHAT_DETAIL,
    MODELS,
    STATS,
    SETTINGS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val conversations by viewModel.filteredConversations.collectAsStateWithLifecycle()
    val activeId by viewModel.activeConversationId.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isStreaming by viewModel.isStreaming.collectAsStateWithLifecycle()
    val streamingContent by viewModel.streamingContent.collectAsStateWithLifecycle()
    val liveResponseTime by viewModel.liveResponseTimeMs.collectAsStateWithLifecycle()
    val liveTokensPerSec by viewModel.liveTokensPerSec.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val globalStats by viewModel.globalStats.collectAsStateWithLifecycle()
    val sessionStats by viewModel.sessionStats.collectAsStateWithLifecycle()
    val modelStats by viewModel.modelStats.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val availableModels by viewModel.availableModels.collectAsStateWithLifecycle()
    val isLoadingModels by viewModel.isLoadingModels.collectAsStateWithLifecycle()
    val modelsFetchError by viewModel.modelsFetchError.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    // Tablet state
    var tabletTab by remember { mutableStateOf(TabletLeftTab.HISTORY) }

    // Phone navigation state
    var phoneScreen by remember { mutableStateOf(PhoneScreen.CONVERSATION_LIST) }

    // Dialog states
    var renamingConversation by remember { mutableStateOf<ConversationEntity?>(null) }
    var renameInput by remember { mutableStateOf("") }
    var deletingConversation by remember { mutableStateOf<ConversationEntity?>(null) }

    val rawConvs by viewModel.rawConversations.collectAsStateWithLifecycle()
    val activeConversation = conversations.find { it.id == activeId }
        ?: rawConvs.find { it.id == activeId }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                actionLabel = "Dismiss",
                duration = SnackbarDuration.Long
            )
            viewModel.dismissError()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { _ ->
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val isTabletExpanded = maxWidth >= 720.dp

            if (isTabletExpanded) {
                // Adaptive Two-Pane Layout for Tablets & Large Screens
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("tablet_two_pane_layout")
                ) {
                    // Left Pane Container (History, Model selection, Stats, or Settings)
                    Column(
                        modifier = Modifier
                            .width(380.dp)
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                    ) {
                        PrimaryTabRow(
                            selectedTabIndex = tabletTab.ordinal,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TabletLeftTab.values().forEach { tab ->
                                Tab(
                                    selected = tabletTab == tab,
                                    onClick = { tabletTab = tab },
                                    text = {
                                        Text(
                                            text = tab.title,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (tabletTab == tab) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    icon = {
                                        when (tab) {
                                            TabletLeftTab.HISTORY -> Icon(
                                                imageVector = Icons.Default.ChatBubbleOutline,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            TabletLeftTab.MODELS -> Icon(
                                                imageVector = Icons.Default.Bolt,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            TabletLeftTab.STATS -> Icon(
                                                imageVector = Icons.Default.Analytics,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            TabletLeftTab.SETTINGS -> Icon(
                                                imageVector = Icons.Default.Settings,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    },
                                    modifier = Modifier.testTag("tablet_tab_${tab.name.lowercase()}")
                                )
                            }
                        }

                        // Content of the selected Tab in the Left Pane
                        Box(modifier = Modifier.weight(1f)) {
                            when (tabletTab) {
                                TabletLeftTab.HISTORY -> {
                                    ChatListPane(
                                        conversations = conversations,
                                        activeConversationId = activeId,
                                        searchQuery = searchQuery,
                                        settings = settings,
                                        onSearchChange = viewModel::setSearchQuery,
                                        onSelectConversation = { id ->
                                            viewModel.selectConversation(id)
                                        },
                                        onNewConversation = {
                                            viewModel.createNewConversation()
                                        },
                                        onRenameConversation = { conv ->
                                            renamingConversation = conv
                                            renameInput = conv.title
                                        },
                                        onDeleteConversation = { conv ->
                                            deletingConversation = conv
                                        },
                                        onOpenModelSelector = { tabletTab = TabletLeftTab.MODELS },
                                        onOpenStats = { tabletTab = TabletLeftTab.STATS },
                                        onOpenSettings = { tabletTab = TabletLeftTab.SETTINGS },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                TabletLeftTab.MODELS -> {
                                    ModelSelectionPane(
                                        settings = settings,
                                        models = availableModels,
                                        isLoading = isLoadingModels,
                                        errorMessage = modelsFetchError,
                                        onRefreshModels = viewModel::refreshModels,
                                        onModelSelected = { modelId ->
                                            viewModel.setModel(modelId)
                                            activeId?.let { convId ->
                                                viewModel.updateConversationModel(convId, modelId)
                                            }
                                        },
                                        onCustomModelSaved = viewModel::setCustomModel,
                                        onTemperatureChanged = viewModel::setTemperature,
                                        onMaxTokensChanged = viewModel::setMaxTokens,
                                        onStreamChanged = viewModel::setStreamResponse,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                TabletLeftTab.STATS -> {
                                    StatsPane(
                                        sessionStats = sessionStats,
                                        overallStats = globalStats,
                                        modelStats = modelStats,
                                        activeConversation = activeConversation,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                TabletLeftTab.SETTINGS -> {
                                    SettingsPane(
                                        settings = settings,
                                        onSaveApiKey = viewModel::setApiKey,
                                        onSaveBaseUrl = viewModel::setBaseUrl,
                                        onSaveSystemPrompt = viewModel::setSystemPrompt,
                                        onSaveThemeMode = viewModel::setThemeMode,
                                        onSaveUseDynamicColors = viewModel::setUseDynamicColors,
                                        onClearAllHistory = {
                                            viewModel.clearAllConversations()
                                            tabletTab = TabletLeftTab.HISTORY
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }

                    VerticalDivider()

                    // Right Pane: Active Conversation always displayed
                    ChatDetailPane(
                        conversation = activeConversation,
                        messages = messages,
                        isStreaming = isStreaming,
                        streamingContent = streamingContent,
                        liveResponseTimeMs = liveResponseTime,
                        liveTokensPerSec = liveTokensPerSec,
                        settings = settings,
                        showNavigationIcon = false,
                        onNavigationClick = { },
                        onSendMessage = viewModel::sendMessage,
                        onCancelStream = viewModel::cancelStreaming,
                        onRenameChat = {
                            activeConversation?.let { conv ->
                                renamingConversation = conv
                                renameInput = conv.title
                            }
                        },
                        onDeleteChat = {
                            activeConversation?.let { conv ->
                                deletingConversation = conv
                            }
                        },
                        onOpenModelSelector = { tabletTab = TabletLeftTab.MODELS },
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                // Adaptive Phone Navigation for Compact Screens
                AnimatedContent(
                    targetState = phoneScreen,
                    transitionSpec = {
                        if (targetState != PhoneScreen.CONVERSATION_LIST) {
                            slideInHorizontally { width -> width } togetherWith slideOutHorizontally { width -> -width }
                        } else {
                            slideInHorizontally { width -> -width } togetherWith slideOutHorizontally { width -> width }
                        }
                    },
                    label = "phone_screen_navigation",
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("phone_single_pane_layout")
                ) { target ->
                    when (target) {
                        PhoneScreen.CONVERSATION_LIST -> {
                            Scaffold(
                                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                                bottomBar = {
                                    NavigationBar {
                                        NavigationBarItem(
                                            selected = true,
                                            onClick = { },
                                            icon = { Icon(Icons.Default.ChatBubbleOutline, contentDescription = null) },
                                            label = { Text("Chats") }
                                        )
                                        NavigationBarItem(
                                            selected = false,
                                            onClick = { phoneScreen = PhoneScreen.MODELS },
                                            icon = { Icon(Icons.Default.Bolt, contentDescription = null) },
                                            label = { Text("Models") }
                                        )
                                        NavigationBarItem(
                                            selected = false,
                                            onClick = { phoneScreen = PhoneScreen.STATS },
                                            icon = { Icon(Icons.Default.Analytics, contentDescription = null) },
                                            label = { Text("Stats") }
                                        )
                                        NavigationBarItem(
                                            selected = false,
                                            onClick = { phoneScreen = PhoneScreen.SETTINGS },
                                            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                            label = { Text("Settings") }
                                        )
                                    }
                                }
                            ) { innerPadding ->
                                ChatListPane(
                                    conversations = conversations,
                                    activeConversationId = activeId,
                                    searchQuery = searchQuery,
                                    settings = settings,
                                    onSearchChange = viewModel::setSearchQuery,
                                    onSelectConversation = { id ->
                                        viewModel.selectConversation(id)
                                        phoneScreen = PhoneScreen.CHAT_DETAIL
                                    },
                                    onNewConversation = {
                                        viewModel.createNewConversation()
                                        phoneScreen = PhoneScreen.CHAT_DETAIL
                                    },
                                    onRenameConversation = { conv ->
                                        renamingConversation = conv
                                        renameInput = conv.title
                                    },
                                    onDeleteConversation = { conv ->
                                        deletingConversation = conv
                                    },
                                    onOpenModelSelector = { phoneScreen = PhoneScreen.MODELS },
                                    onOpenStats = { phoneScreen = PhoneScreen.STATS },
                                    onOpenSettings = { phoneScreen = PhoneScreen.SETTINGS },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .statusBarsPadding()
                                        .padding(bottom = innerPadding.calculateBottomPadding())
                                )
                            }
                        }

                        PhoneScreen.CHAT_DETAIL -> {
                            ChatDetailPane(
                                conversation = activeConversation,
                                messages = messages,
                                isStreaming = isStreaming,
                                streamingContent = streamingContent,
                                liveResponseTimeMs = liveResponseTime,
                                liveTokensPerSec = liveTokensPerSec,
                                settings = settings,
                                showNavigationIcon = true,
                                onNavigationClick = {
                                    phoneScreen = PhoneScreen.CONVERSATION_LIST
                                },
                                onSendMessage = viewModel::sendMessage,
                                onCancelStream = viewModel::cancelStreaming,
                                onRenameChat = {
                                    activeConversation?.let { conv ->
                                        renamingConversation = conv
                                        renameInput = conv.title
                                    }
                                },
                                onDeleteChat = {
                                    activeConversation?.let { conv ->
                                        deletingConversation = conv
                                    }
                                },
                                onOpenModelSelector = { phoneScreen = PhoneScreen.MODELS },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        PhoneScreen.MODELS -> {
                            Scaffold(
                                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                                topBar = {
                                    TopAppBar(
                                        title = { Text("Models & Parameters") },
                                        navigationIcon = {
                                            IconButton(onClick = { phoneScreen = PhoneScreen.CONVERSATION_LIST }) {
                                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                            }
                                        },
                                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                                    )
                                }
                            ) { innerPadding ->
                                ModelSelectionPane(
                                    settings = settings,
                                    models = availableModels,
                                    isLoading = isLoadingModels,
                                    errorMessage = modelsFetchError,
                                    onRefreshModels = viewModel::refreshModels,
                                    onModelSelected = { modelId ->
                                        viewModel.setModel(modelId)
                                        activeId?.let { convId ->
                                            viewModel.updateConversationModel(convId, modelId)
                                        }
                                    },
                                    onCustomModelSaved = viewModel::setCustomModel,
                                    onTemperatureChanged = viewModel::setTemperature,
                                    onMaxTokensChanged = viewModel::setMaxTokens,
                                    onStreamChanged = viewModel::setStreamResponse,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(innerPadding)
                                        .navigationBarsPadding()
                                )
                            }
                        }

                        PhoneScreen.STATS -> {
                            Scaffold(
                                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                                topBar = {
                                    TopAppBar(
                                        title = { Text("Performance & Stats") },
                                        navigationIcon = {
                                            IconButton(onClick = { phoneScreen = PhoneScreen.CONVERSATION_LIST }) {
                                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                            }
                                        },
                                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                                    )
                                }
                            ) { innerPadding ->
                                StatsPane(
                                    sessionStats = sessionStats,
                                    overallStats = globalStats,
                                    modelStats = modelStats,
                                    activeConversation = activeConversation,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(innerPadding)
                                        .navigationBarsPadding()
                                )
                            }
                        }

                        PhoneScreen.SETTINGS -> {
                            Scaffold(
                                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                                topBar = {
                                    TopAppBar(
                                        title = { Text("Settings") },
                                        navigationIcon = {
                                            IconButton(onClick = { phoneScreen = PhoneScreen.CONVERSATION_LIST }) {
                                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                            }
                                        },
                                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                                    )
                                }
                            ) { innerPadding ->
                                SettingsPane(
                                    settings = settings,
                                    onSaveApiKey = viewModel::setApiKey,
                                    onSaveBaseUrl = viewModel::setBaseUrl,
                                    onSaveSystemPrompt = viewModel::setSystemPrompt,
                                    onSaveThemeMode = viewModel::setThemeMode,
                                    onSaveUseDynamicColors = viewModel::setUseDynamicColors,
                                    onClearAllHistory = {
                                        viewModel.clearAllConversations()
                                        phoneScreen = PhoneScreen.CONVERSATION_LIST
                                    },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(innerPadding)
                                        .navigationBarsPadding()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Rename Conversation Dialog
    renamingConversation?.let { conv ->
        AlertDialog(
            onDismissRequest = { renamingConversation = null },
            title = { Text("Rename Chat") },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    singleLine = true,
                    label = { Text("Chat Title") },
                    modifier = Modifier.testTag("rename_chat_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameInput.isNotBlank()) {
                            viewModel.renameConversation(conv.id, renameInput)
                        }
                        renamingConversation = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { renamingConversation = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Conversation Dialog
    deletingConversation?.let { conv ->
        AlertDialog(
            onDismissRequest = { deletingConversation = null },
            title = { Text("Delete Chat?") },
            text = { Text("Are you sure you want to delete '${conv.title}' and all its cached messages?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteConversation(conv.id)
                        deletingConversation = null
                        if (conv.id == activeId) {
                            phoneScreen = PhoneScreen.CONVERSATION_LIST
                        }
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingConversation = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
