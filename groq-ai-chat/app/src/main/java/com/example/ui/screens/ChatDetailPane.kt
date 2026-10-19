package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.ConversationEntity
import com.example.data.local.MessageEntity
import com.example.data.preferences.AppSettings
import com.example.ui.components.ChatInputField
import com.example.ui.components.MessageBubble
import com.example.ui.components.StreamingBubble
import com.example.ui.theme.StatSpeedGreen

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChatDetailPane(
    conversation: ConversationEntity?,
    messages: List<MessageEntity>,
    isStreaming: Boolean,
    streamingContent: String,
    liveResponseTimeMs: Long,
    liveTokensPerSec: Double,
    settings: AppSettings,
    showNavigationIcon: Boolean,
    onNavigationClick: () -> Unit,
    onSendMessage: (String) -> Unit,
    onCancelStream: () -> Unit,
    onRenameChat: () -> Unit,
    onDeleteChat: () -> Unit,
    onOpenModelSelector: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    var showTopMenu by remember { mutableStateOf(false) }

    // Auto-scroll on new messages or streaming chunks
    LaunchedEffect(messages.size, streamingContent.length) {
        if (messages.isNotEmpty() || streamingContent.isNotEmpty()) {
            val totalItems = messages.size + if (isStreaming) 1 else 0
            if (totalItems > 0) {
                listState.animateScrollToItem(totalItems - 1)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onRenameChat() }
                            .padding(vertical = 2.dp, horizontal = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = conversation?.title ?: "Groq AI Chat",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Rename chat",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }

                        // Model pill
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onOpenModelSelector() }
                        ) {
                            Text(
                                text = settings.effectiveModel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (showNavigationIcon) {
                        IconButton(
                            onClick = onNavigationClick,
                            modifier = Modifier.testTag("chat_navigation_icon")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to conversations"
                            )
                        }
                    }
                },
                actions = {
                    Box {
                        IconButton(
                            onClick = { showTopMenu = true },
                            modifier = Modifier.testTag("chat_detail_options_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options"
                            )
                        }

                        DropdownMenu(
                            expanded = showTopMenu,
                            onDismissRequest = { showTopMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Change Model") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showTopMenu = false
                                    onOpenModelSelector()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Rename Chat") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showTopMenu = false
                                    onRenameChat()
                                }
                            )
                            if (conversation != null) {
                                DropdownMenuItem(
                                    text = { Text("Delete Chat") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        showTopMenu = false
                                        onDeleteChat()
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            ChatInputField(
                isStreaming = isStreaming,
                onSendMessage = onSendMessage,
                onCancelStream = onCancelStream
            )
        }
    ) { innerPadding ->
        if (messages.isEmpty() && !isStreaming) {
            // Empty State with Hero Banner & Suggestions
            EmptyChatWelcome(
                onSuggestionClick = onSendMessage,
                onOpenModelSelector = onOpenModelSelector,
                currentModel = settings.effectiveModel,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            // Messages list
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .testTag("messages_lazy_column")
            ) {
                items(
                    items = messages,
                    key = { it.id }
                ) { msg ->
                    MessageBubble(message = msg)
                }

                if (isStreaming) {
                    item {
                        StreamingBubble(
                            content = streamingContent,
                            liveResponseTimeMs = liveResponseTimeMs,
                            liveTokensPerSec = liveTokensPerSec,
                            onCancel = onCancelStream
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmptyChatWelcome(
    onSuggestionClick: (String) -> Unit,
    onOpenModelSelector: () -> Unit,
    currentModel: String,
    modifier: Modifier = Modifier
) {
    val suggestions = listOf(
        "⚡ Explain how Groq LPUs achieve 500+ tok/s",
        "💻 Write a Kotlin coroutines channel pipeline",
        "🔬 Summarize recent AI breakthroughs in 3 points",
        "📝 Draft an executive summary for a tech proposal",
        "🚀 Optimize a slow PostgreSQL database query",
        "🌐 Create an async Python scraper using httpx"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Hero Banner Art
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(140.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.groq_hero_banner_1790240593244),
                contentDescription = "Groq LPU Acceleration",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Groq AI Speed Engine",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Experience instantaneous AI responses powered by Groq's Tensor Streaming Processors. Select a suggestion or start typing below.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(0.85f)
        )

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Quick Prompts",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            suggestions.forEach { suggestion ->
                SuggestionChip(
                    onClick = { onSuggestionClick(suggestion.substringAfter(" ")) },
                    label = {
                        Text(
                            text = suggestion,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}
